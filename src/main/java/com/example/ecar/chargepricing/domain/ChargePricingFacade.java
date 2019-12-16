package com.example.ecar.chargepricing.domain;

import com.example.ecar.chargepricing.dto.*;
import com.example.ecar.shared.lang.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.ecar.chargepricing.domain.DailyPriceDefinition.TimeInterval;

@RequiredArgsConstructor
public class ChargePricingFacade {
    private final DailyPriceDefinitionRepository priceDefinitionRepository;
    private final ChargingCostsCalculator chargingCostsCalculator;
    private final CustomerClient customerClient;

    public void addPriceDefinition(@NonNull NewPriceDefinitionDto dto) {
        DailyPriceDefinition priceDefinition = Optional.ofNullable(priceDefinitionRepository.findByAsOf(dto.getAsOf()))
                .orElse(DailyPriceDefinition.defaultFullDay(dto.getAsOf(), dto.getPricePerMinute()));

        if (dto.getTimeInterval() == null) {
            priceDefinition.changeDefaultPricePerMinute(dto.getPricePerMinute());
        } else {
            priceDefinition.addInterval(TimeInterval.of(dto.getPricePerMinute(), dto.getTimeInterval().getStart(), dto.getTimeInterval().getEnd()));
        }
        priceDefinitionRepository.save(priceDefinition);
    }

    // complex event handling with possible side effects (simple for now)
    public ChargingCostsDto calculateChargingCosts(@NonNull ChargingSessionFinished event) {
        ChargingCosts chargingCosts = calculateChargingCosts(event.getCustomerId(), event.getStart(), event.getEnd());
        // probably persistent the result
        // publish ChargingCostsCalculated event
        // maybe some check for idempotency
        // maybe do some reporting in case we obtained an invalid event
        return chargingCosts.toDto();
    }

    // simple query without side effects
    public ChargingCostsDto calculateChargingCosts(@NonNull CalculateChargingCostsQuery query) {
        return calculateChargingCosts(query.getCustomerId(), query.getStart(), query.getEnd()).toDto();
    }

    private ChargingCosts calculateChargingCosts(String customerId, OffsetDateTime start, OffsetDateTime end) {
        LocalDateTime sessionStartUtc = start.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime sessionEndUtc = end.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();

        PriceSlice priceSlice = priceDefinitionRepository.findForPeriod(sessionStartUtc.toLocalDate(), sessionEndUtc.toLocalDate());
        ChargingCosts chargingCosts = chargingCostsCalculator.calculate(sessionStartUtc, sessionEndUtc, priceSlice);
        chargingCosts = applyDiscount(chargingCosts, customerId);
        return chargingCosts;
    }

    // simple method is enough, there is no need for abstraction for now until we get more requirements
    private ChargingCosts applyDiscount(ChargingCosts chargingCosts, String customerId) {
        CustomerClient.CustomerDto customer = customerClient.getCustomerDetails(customerId);
        double discountPercentage = determineDiscountPercentage(customer);
        return chargingCosts.discountedBy(discountPercentage);
    }

    private double determineDiscountPercentage(@Nullable CustomerClient.CustomerDto customer) {
        if (customer == null) return 0.0;

        switch (customer.getCustomerStatus()) {
            case VIP:
                return 0.10;
            case REGULAR:
            default:
                return 0.0;
        }
    }
}
