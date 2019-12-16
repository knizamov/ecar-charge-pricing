package com.example.ecar.chargepricing.domain


import com.example.ecar.chargepricing.domain.CustomerClient.CustomerDto
import com.example.ecar.chargepricing.dto.ChargingCostsDto
import com.example.ecar.chargepricing.dto.ChargingSessionFinished
import com.example.ecar.chargepricing.dto.NewPriceDefinitionDto
import spock.lang.Specification
import spock.lang.Unroll

class ChargePricingSpec extends Specification implements SamplePriceDefinitions, SampleChargeSessions {
    CustomerClient customers = Stub() {
        getCustomerDetails(VIP_CUSTOMER_ID) >> new CustomerDto(CustomerDto.CustomerStatus.VIP)
        getCustomerDetails(REGULAR_CUSTOMER_ID) >> new CustomerDto(CustomerDto.CustomerStatus.REGULAR)
        getCustomerDetails(UNKNOWN_CUSTOMER_ID) >> null
        getCustomerDetails(null) >> null
    }

    ChargePricingFacade facade = new ChargePricingConfiguration().chargePricingFacade(customers)

    def "acceptance test scenario"() {
        given: "there is a price definition of 1.00\$ per minute for 00:00-7:00 " +
                "and 2.00\$ for 7:00-14:00 and 3.00\$ for 14:00-24:00 time period as of 31.01.2019 " +
                "and another price definition of 0.50\$ as of 01.02.2019"
            add priceDefinitions(asOf: "2019-01-31",
                    [pricePerMinute: 1.00, timeInterval: "00:00-07:00"],
                    [pricePerMinute: 2.00, timeInterval: "07:00-14:00"],
                    [pricePerMinute: 3.00, timeInterval: "14:00-24:00"])
            add priceDefinition(asOf: "2019-02-01", pricePerMinute: 0.50)

        and: "I’m a VIP customer"

        when: "I calculate possible charging costs for 31.01.2019 12:00 - 02.02.2019 1:30 period"
            ChargingCostsDto possibleCosts = facade.calculateChargingCosts(
                    calculateChargingCostsQuery(customerId: VIP_CUSTOMER_ID, start: "2019-01-31T12:00:00Z", end: "2019-02-02T01:30:00Z"))

        then: "it estimates 2524.5\$ for the specified period " +
                "(12:00-14:00 2h * 60m * 2\$) + (14:00-24:00 10h * 60m * 3\$) = 2040\$ for 31.01.2019 " +
                "(00:00-24:00 24h * 60m * 0.50\$) = 720\$ for 01.02.2019 " +
                "(00:00-1:30 1.5h * 60m * 0.50\$) = 45\$ for 02.02.2019 " +
                "= 2805\$ discounted by 10% = 2524.5"
            possibleCosts.total == 2524.5

        when: "I really start my charging session at 31.01.2019 12:00 and finish at 02.02.2019 01:30"
            ChargingSessionFinished realChargingSessionFinished =
                    chargingSessionFinished(customerId: VIP_CUSTOMER_ID, start: "2019-01-31T12:00:00Z", end: "2019-02-02T01:30:00Z")

        and: "in the meantime there is a new price definition of 10.00\$ per minute added as of 02.02.2019"
            add priceDefinition(asOf: '2019-02-02', pricePerMinute: 10.00)

        and: "when my charging costs are calculated"
            ChargingCostsDto costs = facade.calculateChargingCosts(realChargingSessionFinished)

        then: "they are equal to 3294\$ (2040+720+900 discounted by 10%)"
            costs.total == 3294
    }

    def "a price definition without specific hour intervals defaults to price being defined for the whole 24-hour interval"() {
        given: "a price definition without interval"
            add priceDefinition(pricePerMinute: 1.0, timeInterval: null)

        when: "charging costs are calculated for one day session"
            ChargingCostsDto chargingCosts = facade.calculateChargingCosts(oneDayChargingSessionFinished())

        then: "the result is calculated for the 24-hour interval"
            chargingCosts.total == 1440.0
    }

    def "adding a price definition without time interval for already existing price definition at the given date changes the default price per minute"() {
        given: "there a price definition for the whole day with default price of 2.0\$ and another price of 1.0\$ for 00:00-12:00"
            add priceDefinition(pricePerMinute: 2.0)
            add priceDefinition(pricePerMinute: 1.0, timeInterval: '00:00-01:00')

        when: "a price manager creates another price definition of 0.0\$ without interval for the same day"
            add priceDefinition(pricePerMinute: 0.0)

        then: "it changes the default price per minute to 0.0\$"
            ChargingCostsDto chargingCosts = facade.calculateChargingCosts(oneDayChargingSessionFinished())
            chargingCosts.total == 60
    }

    def "charging costs are calculated according to each time interval within the same day"() {
        given: "there is a price definition of 1.00\$ for 00:00-08:00 and 2.00\$ for 08:00-16:00 and 3.00\$ for 16:00-24:00"
            add priceDefinition(pricePerMinute: 1.00, timeInterval: "00:00-08:00")
            add priceDefinition(pricePerMinute: 2.00, timeInterval: "08:00-16:00")
            add priceDefinition(pricePerMinute: 3.00, timeInterval: "16:00-24:00")

        when: "charging costs are calculated for full day session"
            ChargingCostsDto costs = facade.calculateChargingCosts(oneDayChargingSessionFinished())

        then:
            costs.total == 480 + 480 * 2 + 480 * 3
    }

    def "charging costs are calculated according to each daily price definition"() {
        given: "there is a price definition of 1.00\$ as of 2019-01-01 and 10.00\$ as of 2019-01-02"
            add priceDefinition(asOf: "2019-01-01", pricePerMinute: 1.00)
            add priceDefinition(asOf: "2019-01-02", pricePerMinute: 10.00)

        when: "charging costs are calculated for 2019-01-01 12:00 - 2019-01-02 12:00"
            ChargingCostsDto costs = facade.calculateChargingCosts(
                    oneDayChargingSessionFinished(start: "2019-01-01T12:00:00Z"))

        then:
            costs.total == 720 + 720 * 10
    }

    def "a price definition for undefined day defaults to the previously known price definition"() {
        given: "there is a price of 1.00\$ per minute for 00:00-12:00 and price of 2.00\$ per minute for 12:00-24:00 as of 2019-01-01 and another price of 10.00\$ as of 2019-01-01"
            add priceDefinitions(asOf: "2019-01-01",
                    [pricePerMinute: 1.00, timeInterval: "00:00-12:00"],
                    [pricePerMinute: 2.00, timeInterval: "12:00-24:00"])
            add priceDefinition(asOf: "2019-01-25", pricePerMinute: 10.00)

        when: "charging costs are calculated for 2019-01-10 00:00 - 2019-01-11 00:00"
            ChargingCostsDto costs = facade.calculateChargingCosts(
                    oneDayChargingSessionFinished(start: "2019-01-10T00:00:00Z"))

        then:
            costs.total == 720 + 1440
    }

    def "a price definition with gaps in unspecified hour intervals defaults to a price of the first created price definition for the given date"() {
        given: "there is a price of 1.00\$ per minute for 12:00-14:00 and another price of 2.00\$ for 22:00-24:00 as of 01.01.2019"
            add priceDefinition(pricePerMinute: 1.00, timeInterval: "12:00-14:00")
            add priceDefinition(pricePerMinute: 2.00, timeInterval: "20:00-22:00")

        when: "charging costs are calculated for full day session"
            ChargingCostsDto costs = facade.calculateChargingCosts(oneDayChargingSessionFinished())

        then:
            costs.total == 120 + 240 + 1200
    }

    @Unroll
    def "a price definition with overlapping interval is rejected (#interval #reason, 03:00-12:00 already exists)"() {
        given: "there is a price definition for 03:00-12:00"
            add priceDefinition(timeInterval: "03:00-12:00")

        when: "a price manager tries to add a price definition with overlapping time interval"
            add priceDefinition(timeInterval: interval)

        then: "it is rejected"
            IllegalStateException ex = thrown()

        where:
            interval      || reason
            "05:00-06:00" || "already defined interval encloses a new one"
            "05:00-14:00" || "partially overlapping start"
            "00:00-05:00" || "partially overlapping end"
            "00:00-23:00" || "new interval encloses already defined one"
    }

    @Unroll
    def "#reason is not allowed (basic new price definition parameters validation)"() {
        when: "a price manager tries to add a new price definition with invalid parameters"
            add priceDefinition(timeInterval: interval, pricePerMinute: pricePerMinute)

        then: "it is rejected and costs are still calculated according to the first valid price definition"
            Exception ex = thrown()

        where:
            interval                    | pricePerMinute || reason
            "00:00-12:00"               | -1.00          || "negative price"
            "00:00-12:00"               | null           || "null price"
            "12:00-12:00"               | 2.00           || "zero duration interval"
            "12:00-08:00"               | 2.00           || "negative duration interval"
            [start: null, end: "12:00"] | 2.00           || "null interval start"
            [start: "00:00", end: null] | 2.00           || "null interval end"
    }

    @Unroll
    def "smallest precision of an interval start or end is a minute (#interval #comment)"() {
        when: "a price manager tries to add a price with at non minute precision"
            add priceDefinition(timeInterval: interval)

        then: "it is rejected and costs are still calculated according to the first valid price definition"
            Exception ex = thrown();

        where:
            interval                   | comment
            "12:00:05-13:00"           | "start cannot contain milliseconds"
            "12:00:00.999999999-13:00" | "start cannot contain nanoseconds"
            "11:00-12:00:01"           | "end cannot contain nanoseconds if not end of the day"
            "11:00-12:00:00.999999999" | "end cannot contain milliseconds if not end of the day"
    }

    def "charge cost calculation has precession of seconds"() {
        given: "there is a price of 1.00\$ per minute for 00:00-12:00 and price of 2.00\$ per minute for 12:00-24:00"
            add priceDefinition(pricePerMinute: 1.00, timeInterval: "00:00-12:00")
            add priceDefinition(pricePerMinute: 2.00, timeInterval: "12:00-24:00")

        and: "my charging session started at 11:58:56 and 12:01:29"
            def chargingSessionFinished = chargingSessionFinished(start: "2019-01-01T11:58:56Z", end: "2019-01-01T12:01:29Z")

        when: "my charging costs are calculated"
            ChargingCostsDto costs = facade.calculateChargingCosts(chargingSessionFinished)

        then: "they are equal to 4.03333… = 4.03\$ (rounded to 2 decimal places for dollar) " +
                    "(11:58:56-12:00 64s -> (64/60)min * 1\$) + (12:00-12:01:29 89s -> (89/60)min * 2\$ )"
            costs.total.round(2) == 4.03
    }


    def "charging costs are calculated according to last changed prices"() {
        given: "there is a price of 1.00\$ as of 2019-01-01"
            add priceDefinition(asOf: "2019-01-01", pricePerMinute: 1.0)

        and: "my charging session started at 2019-01-01 12:00 and finished at 2019-01-02 12:00"
            def chargingSessionFinished = oneDayChargingSessionFinished(start: "2019-01-01T12:00:00Z")

        and: "then a price manager added a price of 2.00\$ as of 2019-01-02"
            add priceDefinition(asOf: "2019-01-02", pricePerMinute: 2.0)

        expect: "my charging costs should be calculated with this new price definition in mind"
            ChargingCostsDto costs = facade.calculateChargingCosts(chargingSessionFinished)
            costs.total == 720 + 1440
    }

    def "a VIP customer is granted 10% discount for charging"() {
        given: "there is a price of 1.00\$"
            add priceDefinition(pricePerMinute: 1.0)
        and: "we have a VIP customer and a regular one"

        when: "charging costs are calculated for the same one day session let's say for a regular customer and VIP"
            ChargingCostsDto regularCosts = facade.calculateChargingCosts(oneDayChargingSessionFinished(customerId: REGULAR_CUSTOMER_ID))
            ChargingCostsDto vipCosts = facade.calculateChargingCosts(oneDayChargingSessionFinished(customerId: VIP_CUSTOMER_ID))

        then: "the VIP charging costs are 10% lower than the regular customer costs"
            vipCosts.total == regularCosts.total * 0.9

    }

    def "customer is not charged if there are no prices defined"() {
        given: "there are no price definitions"
        and: "my charging session lasted for 1 day"
            ChargingSessionFinished chargingSessionFinished = oneDayChargingSessionFinished()

        when: "my charging costs are calculated"
            ChargingCostsDto costs = facade.calculateChargingCosts(chargingSessionFinished)

        then: "they are equal 0"
            costs.total == 0
    }

    @Unroll
    def "an unknown customer is charged as a regular one"() {
        given: "there is a price of 1.00\$"
            add priceDefinition(pricePerMinute: 1.0)

        and: "we are unable to identify a customer"

        when: "charging costs are calculated for the same one day session for a regular customer and an unknown one"
            ChargingCostsDto costsOfRegular = facade.calculateChargingCosts(oneDayChargingSessionFinished(customerId: REGULAR_CUSTOMER_ID))
            ChargingCostsDto costsOfUnknown = facade.calculateChargingCosts(oneDayChargingSessionFinished(customerId: unknownCustomerId))

        then: "charging costs of the unknown customer equal to charging costs of the regular customer"
            costsOfRegular.total == costsOfUnknown.total

        where:
            unknownCustomerId << [UNKNOWN_CUSTOMER_ID, null]
    }


    void add(List<NewPriceDefinitionDto> priceDefinitions) {
        priceDefinitions.each { add(it) }
    }

    void add(NewPriceDefinitionDto priceDefinition) {
        facade.addPriceDefinition(priceDefinition)
    }
}