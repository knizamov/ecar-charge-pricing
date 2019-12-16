package com.example.ecar.chargepricing.domain.infrastructure.mvc;

import com.example.ecar.chargepricing.domain.ChargePricingFacade;
import com.example.ecar.chargepricing.dto.NewPriceDefinitionDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class PriceDefinitionController {

    private final ChargePricingFacade facade;

    // should be authorized as Price Manager
    @PostMapping("/price-definitions")
    void addPriceDefinition(@NonNull @RequestBody NewPriceDefinitionDto dto) {
        facade.addPriceDefinition(dto);
    }
}
