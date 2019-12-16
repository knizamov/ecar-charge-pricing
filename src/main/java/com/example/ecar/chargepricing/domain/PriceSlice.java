package com.example.ecar.chargepricing.domain;

import com.example.ecar.shared.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

class PriceSlice {
    private final Map<LocalDate, DailyPriceDefinition> dailyPriceDefinitionMap;

    PriceSlice(List<DailyPriceDefinition> dailyPriceDefinitions) {
        dailyPriceDefinitionMap = toMap(dailyPriceDefinitions);
    }

    private Map<LocalDate, DailyPriceDefinition> toMap(List<DailyPriceDefinition> dailyPriceDefinitions) {
        Map<LocalDate, DailyPriceDefinition> map = new TreeMap<>(Comparator.<LocalDate>naturalOrder().reversed());
        for (DailyPriceDefinition dailyPriceDefinition : dailyPriceDefinitions) {
            map.put(dailyPriceDefinition.getAsOf(), dailyPriceDefinition);
        }
        return map;
    }

    BigDecimal totalCostForDay(LocalDate date, LocalTime start, LocalTime end) {
        DailyPriceDefinition priceDefinition = getPriceDefinitionAtOrBefore(date);
        if (priceDefinition == null) return BigDecimal.ZERO;
        return priceDefinition.totalCostForDayPeriod(start, end);
    }

    private @Nullable DailyPriceDefinition getPriceDefinitionAtOrBefore(LocalDate localDate) {
        return Optional.ofNullable(dailyPriceDefinitionMap.get(localDate))
                .orElseGet(() -> getPriceDefinitionBefore(localDate));
    }

    private @Nullable DailyPriceDefinition getPriceDefinitionBefore(LocalDate localDate) {
        return dailyPriceDefinitionMap.entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(localDate))
                .map(entry -> entry.getValue())
                .findFirst().orElse(null);
    }
}
