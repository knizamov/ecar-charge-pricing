package com.example.ecar.chargepricing.dto;

import com.example.ecar.shared.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Value
@Builder
public class NewPriceDefinitionDto {
    LocalDate asOf;
    BigDecimal pricePerMinute;
    @Nullable LocalTimeIntervalDto timeInterval;
}
