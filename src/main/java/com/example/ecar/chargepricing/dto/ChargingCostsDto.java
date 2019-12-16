package com.example.ecar.chargepricing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@AllArgsConstructor(onConstructor = @__(@JsonCreator)) // needed due to single parameter
@Value
@Builder
public class ChargingCostsDto {
    BigDecimal total;
}
