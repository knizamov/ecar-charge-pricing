package com.example.ecar.chargepricing.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

class ChargingCostsCalculator {
    ChargingCosts calculate(LocalDateTime chargeSessionStart, LocalDateTime chargeSessionEnd, PriceSlice priceSlice) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDateTime date = chargeSessionStart;
             date.isBefore(chargeSessionEnd);
             date = startOfNextDay(date)) {

            total = total.add(
                    priceSlice.totalCostForDay(date.toLocalDate(), date.toLocalTime(), endOfDayOrChargingEndTime(date, chargeSessionEnd))
            );
        }

        return new ChargingCosts(total);
    }

    private LocalDateTime startOfNextDay(LocalDateTime date) {
        return date.plusDays(1).toLocalDate().atStartOfDay();
    }

    private LocalTime endOfDayOrChargingEndTime(LocalDateTime date, LocalDateTime chargeSessionEnd) {
        return min(startOfNextDay(date).minusNanos(1), chargeSessionEnd).toLocalTime();
    }

    private LocalDateTime min(LocalDateTime date, LocalDateTime other) {
        return date.isBefore(other) ? date : other;
    }
}


