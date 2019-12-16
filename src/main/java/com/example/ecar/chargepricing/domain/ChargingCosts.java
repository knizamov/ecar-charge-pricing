package com.example.ecar.chargepricing.domain;

import com.example.ecar.chargepricing.dto.ChargingCostsDto;
import lombok.Value;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;

@Value
class ChargingCosts {
    BigDecimal total;

    ChargingCosts discountedBy(double discountPercentage) {
        BigDecimal discountedTotal = total.multiply(ONE.subtract(valueOf(discountPercentage)));
        return new ChargingCosts(discountedTotal);
    }

    ChargingCostsDto toDto() {
        return ChargingCostsDto.builder()
                .total(total)
                .build();
    }
}
