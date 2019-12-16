package com.example.ecar.chargepricing.domain;

import com.example.ecar.shared.lang.Nullable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

interface DailyPriceDefinitionRepository {
    void save(DailyPriceDefinition priceDefinition);

    @Nullable DailyPriceDefinition findByAsOf(LocalDate date);

    PriceSlice findForPeriod(LocalDate start, LocalDate end);
}

class InMemoryDailyPriceDefinitionRepository implements DailyPriceDefinitionRepository {
    private final Map<String, DailyPriceDefinition> store = new ConcurrentHashMap<>();

    @Override
    public void save(DailyPriceDefinition priceDefinition) {
        store.put(priceDefinition.getAsOf().toString(), priceDefinition);
    }

    @Override
    public DailyPriceDefinition findByAsOf(LocalDate date) {
        return store.get(date.toString());
    }

    @Override
    public PriceSlice findForPeriod(LocalDate start, LocalDate end) {
        DailyPriceDefinition priceDefinitionForStart = store.values().stream()
                .sorted(Comparator.comparing(DailyPriceDefinition::getAsOf).reversed())
                .filter(priceDefinition -> start.isEqual(priceDefinition.getAsOf()) || start.isAfter(priceDefinition.getAsOf()))
                .findFirst().orElse(null);

        if (priceDefinitionForStart == null)
            return new PriceSlice(Collections.emptyList());

        List<DailyPriceDefinition> priceDefinitions = store.values().stream()
                .filter(priceDefinition -> priceDefinition.getAsOf().isEqual(priceDefinitionForStart.getAsOf())
                        || priceDefinition.getAsOf().isAfter(priceDefinitionForStart.getAsOf()))
                .filter(priceDefinition -> priceDefinition.getAsOf().isEqual(end) || priceDefinition.getAsOf().isBefore(end))
                .collect(toList());

        return new PriceSlice(priceDefinitions);
    }
}
