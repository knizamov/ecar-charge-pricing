package com.example.ecar.chargepricing.domain.infrastructure.mvc;

import com.example.ecar.chargepricing.domain.ChargePricingFacade;
import com.example.ecar.chargepricing.dto.CalculateChargingCostsQuery;
import com.example.ecar.chargepricing.dto.ChargingCostsDto;
import com.example.ecar.chargepricing.dto.ChargingSessionFinished;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/charging-costs")
@RequiredArgsConstructor
class ChargingCostsController {

    private final ChargePricingFacade facade;
    private final ObjectMapper objectMapper;

    // should be authorized as Charging Station or maybe it should not be here and the event should come from different source
    @PostMapping("/calculate")
    ChargingCostsDto calculateChargingCosts(@NonNull @RequestBody ChargingSessionFinished event) {
        return facade.calculateChargingCosts(event);
    }

    // should be authorized as Customer and customerId should match
    @GetMapping("/calculate")
    ChargingCostsDto calculateChargingCosts(@RequestParam Map<String, String> query) {
        // spring can't parse some OffsetDateTime valid values if I used this class directly in the method
        return facade.calculateChargingCosts(objectMapper.convertValue(query, CalculateChargingCostsQuery.class));
    }
}
