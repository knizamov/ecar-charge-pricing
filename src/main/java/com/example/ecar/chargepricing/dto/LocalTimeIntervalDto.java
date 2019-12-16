package com.example.ecar.chargepricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@AllArgsConstructor
@Value
@Builder
public class LocalTimeIntervalDto {
    LocalTime start;
    LocalTime end;
}
