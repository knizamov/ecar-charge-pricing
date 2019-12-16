package com.example.ecar.chargepricing.domain;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

class DailyPriceDefinition {
    private LocalDate asOf;
    private BigDecimal defaultPricePerMinute;
    private List<TimeInterval> timeIntervals = new ArrayList<>();

    private DailyPriceDefinition(@NonNull LocalDate asOf, @NonNull BigDecimal defaultPricePerMinute, @NonNull List<TimeInterval> timeIntervals) {
        assertNonNegativePrice(defaultPricePerMinute);

        this.asOf = asOf;
        this.defaultPricePerMinute = defaultPricePerMinute;
        this.timeIntervals.addAll(timeIntervals);
    }

    static DailyPriceDefinition defaultFullDay(LocalDate asOf, BigDecimal pricePerMinute) {
        return new DailyPriceDefinition(asOf, pricePerMinute, Collections.emptyList());
    }

    void changeDefaultPricePerMinute(BigDecimal pricePerMinute) {
        assertNonNegativePrice(pricePerMinute);
        defaultPricePerMinute = pricePerMinute;
    }

    void addInterval(TimeInterval timeInterval) {
        assertDoesNotOverlapWithExisting(timeInterval);

        this.timeIntervals.add(timeInterval);
    }

    private void assertDoesNotOverlapWithExisting(TimeInterval timeInterval) {
        boolean doesNotOverlap = timeIntervals.stream()
                .noneMatch(existingTimeInterval -> existingTimeInterval.overlaps(timeInterval));
        Preconditions.checkState(doesNotOverlap, "%s overlaps with existing", timeInterval);
    }


    BigDecimal totalCostForDayPeriod(LocalTime start, LocalTime end) {
        if (timeIntervals.isEmpty())
            return TimeInterval.fullDay(defaultPricePerMinute).totalCost(start, end);

        List<TimeInterval> definedTimeIntervals = timeIntervals;
        List<TimeInterval> undefinedTimeIntervals = undefinedTimeIntervalsBetween(start, end);

        return totalCostWithin(definedTimeIntervals, start, end)
                .add(totalCostWithin(undefinedTimeIntervals, start, end));
    }

    private List<TimeInterval> undefinedTimeIntervalsBetween(LocalTime start, LocalTime end) {
        TreeRangeSet<LocalTime> definedTimeIntervals = TreeRangeSet.create(
                timeIntervals.stream()
                        .map(TimeInterval::getRange)
                        .collect(toList())
        );
        Set<Range<LocalTime>> gaps = definedTimeIntervals.complement().subRangeSet(Range.closedOpen(start, end)).asRanges();

        return gaps.stream()
                .map(gap -> TimeInterval.of(defaultPricePerMinute, gap.lowerEndpoint(), gap.upperEndpoint()))
                .collect(toList());
    }

    private static BigDecimal totalCostWithin(final List<TimeInterval> timeIntervals, LocalTime start, LocalTime end) {
        return timeIntervals.stream()
                .map(timeInterval -> timeInterval.totalCost(start, end))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }


    LocalDate getAsOf() {
        return asOf;
    }

    @Value
    static class TimeInterval {
        private BigDecimal pricePerMinute;
        private Range<LocalTime> range;

        private TimeInterval(@NonNull BigDecimal pricePerMinute, @NonNull LocalTime start, @NonNull LocalTime end) {
            assertNonNegativePrice(pricePerMinute);
            assertPositiveDuration(start, end);
            assertMinutePrecession(start, end);

            this.pricePerMinute = pricePerMinute;
            this.range = Range.closedOpen(start, end);
        }

        private void assertPositiveDuration(LocalTime start, LocalTime end) {
            Duration duration = Duration.between(start, end);
            Preconditions.checkArgument(duration.getSeconds() > 0, "Time should be positive");
        }

        private void assertMinutePrecession(LocalTime start, LocalTime end) {
            Preconditions.checkArgument(start.getSecond() == 0);
            Preconditions.checkArgument(start.getNano() == 0);

            if (!end.equals(LocalTime.MAX)) {
                Preconditions.checkArgument(end.getSecond() == 0);
                Preconditions.checkArgument(end.getNano() == 0);
            }

        }

        static TimeInterval of(BigDecimal pricePerMinute, LocalTime start, LocalTime end) {
            return new TimeInterval(pricePerMinute, start, end);
        }

        static TimeInterval fullDay(BigDecimal pricePerMinute) {
            return new TimeInterval(pricePerMinute, LocalTime.MIN, LocalTime.MAX);
        }


        BigDecimal totalCost(LocalTime start, LocalTime end) {
            Range<LocalTime> startEndRange = Range.closedOpen(start, end);
            if (!rangeOverlaps(this.range, startEndRange))
                return BigDecimal.ZERO;

            Range<LocalTime> intersection = range.intersection(startEndRange);
            LocalTime startTime = intersection.lowerEndpoint();
            LocalTime endTime = intersection.upperEndpoint();

            Duration duration = adjustedDurationBetween(startTime, endTime);

            double durationInMinutes = duration.getSeconds() / 60d;
            return pricePerMinute.multiply(new BigDecimal(durationInMinutes));
        }

        private Duration adjustedDurationBetween(LocalTime startTime, LocalTime endTime) {
            Duration duration = Duration.between(startTime, endTime);
            if (isEndOfDay(endTime)) {
                duration = duration.plusNanos(1);
            }
            return duration;
        }

        private boolean isEndOfDay(LocalTime localTime) {
            return localTime.equals(LocalTime.MAX);
        }

        boolean overlaps(TimeInterval other) {
            return rangeOverlaps(this.range, other.range);
        }

        private boolean rangeOverlaps(Range<LocalTime> rangeA, Range<LocalTime> rangeB) {
            return rangeA.isConnected(rangeB) // otherwise intersection throws for non connected, e.g 12-14 and 20-22
                    && !rangeA.intersection(rangeB).isEmpty();
        }

    }

    private static void assertNonNegativePrice(BigDecimal price) {
        Preconditions.checkArgument(price.compareTo(BigDecimal.ZERO) >= 0);
    }
}
