package com.example.ecar.chargepricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Value
@Builder
public class ChargingSessionFinished {
    String customerId;
    OffsetDateTime start;
    OffsetDateTime end;
}
