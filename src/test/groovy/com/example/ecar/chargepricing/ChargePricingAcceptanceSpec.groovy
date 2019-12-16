package com.example.ecar.chargepricing

import com.example.ecar.base.IntegrationSpec
import com.example.ecar.chargepricing.domain.SampleChargeSessions
import com.example.ecar.chargepricing.domain.SamplePriceDefinitions
import com.example.ecar.chargepricing.dto.CalculateChargingCostsQuery
import com.example.ecar.chargepricing.dto.ChargingCostsDto
import com.example.ecar.chargepricing.dto.ChargingSessionFinished
import com.example.ecar.chargepricing.dto.NewPriceDefinitionDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.ResultActions

import static org.springframework.http.MediaType.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class ChargePricingAcceptanceSpec extends IntegrationSpec
        implements SamplePriceDefinitions, SampleChargeSessions {

    @Autowired
    ObjectMapper objectMapper

    def "acceptance test scenario"() {
        given: "there is a price definition of 1.00\$ per minute for 00:00-7:00 " +
                    "and 2.00\$ for 7:00-14:00 and 3.00\$ for 14:00-24:00 time period as of 31.01.2019 " +
                    "and another price definition of 0.50\$ as of 01.02.2019"
            postNew priceDefinitions(asOf: "2019-01-31",
                    [pricePerMinute: 1.00, timeInterval: "00:00-07:00"],
                    [pricePerMinute: 2.00, timeInterval: "07:00-14:00"],
                    [pricePerMinute: 3.00, timeInterval: "14:00-24:00"])
            postNew priceDefinition(asOf: "2019-02-01", pricePerMinute: 0.50)

        and: "I’m a VIP customer"

        when: "I calculate possible charging costs for 31.01.2019 12:00 - 02.02.2019 1:30 period"
            ResultActions calculationResult = calculate(
                    calculateChargingCostsQuery(customerId: VIP_CUSTOMER_ID, start: "2019-01-31T12:00:00Z", end: "2019-02-02T01:30:00Z"))

        then: "it estimates 2524.5\$ for the specified period " +
                    "(12:00-14:00 2h * 60m * 2\$) + (14:00-24:00 10h * 60m * 3\$) = 2040\$ for 31.01.2019 " +
                    "(00:00-24:00 24h * 60m * 0.50\$) = 720\$ for 01.02.2019 " +
                    "(00:00-1:30 1.5h * 60m * 0.50\$) = 45\$ for 02.02.2019 " +
                    "= 2805\$ discounted by 10% = 2524.5"
            ChargingCostsDto possibleCosts = parseJsonResponse(calculationResult, ChargingCostsDto)
            possibleCosts.total == 2524.5

        when: "I really start my charging session at 31.01.2019 12:00 and finish at 02.02.2019 01:30"
            ChargingSessionFinished realChargeSessionFinished =
                    chargingSessionFinished(customerId: VIP_CUSTOMER_ID, start: "2019-01-31T12:00:00Z", end: "2019-02-02T01:30:00Z")

        and: "in the meantime there is a new price definition of 10.00\$ per minute added as of 02.02.2019"
            postNew priceDefinition(asOf: '2019-02-02', pricePerMinute: 10.00)

        and: "when my charging costs are calculated"
            ResultActions realChargeSessionCalculationResult = calculate(realChargeSessionFinished)

        then: "they are equal to 3294\$ (2040+720+900 discounted by 10%)"
            ChargingCostsDto costs = parseJsonResponse(realChargeSessionCalculationResult, ChargingCostsDto)
            costs.total == 3294
    }

    void postNew(NewPriceDefinitionDto dto) {
        mockMvc.perform(post("/price-definitions")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
    }

    void postNew(List<NewPriceDefinitionDto> dtos) {
        dtos.each { postNew(it) }
    }

    ResultActions calculate(ChargingSessionFinished event) {
        return mockMvc.perform(post("/charging-costs/calculate")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event))
        )
    }

    ResultActions calculate(CalculateChargingCostsQuery query) {
        return mockMvc.perform(get("/charging-costs/calculate")
                .param("customerId", query.customerId)
                .param("start", query.start.toString())
                .param("end", query.end.toString())
        )
    }

    def <T> T parseJsonResponse(ResultActions resultActions, Class<T> clazz) {
        return objectMapper.readValue(resultActions.andReturn().response.contentAsString, clazz)
    }
}
