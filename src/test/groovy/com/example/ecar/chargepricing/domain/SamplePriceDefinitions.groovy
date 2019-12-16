package com.example.ecar.chargepricing.domain

import com.example.ecar.chargepricing.dto.LocalTimeIntervalDto
import com.example.ecar.chargepricing.dto.NewPriceDefinitionDto
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalTime

import static com.example.ecar.chargepricing.domain.Mapper.mapNonNull

@CompileStatic
trait SamplePriceDefinitions {
    static final Map SAMPLE_NEW_PRICE_DEFINITION = [
            "asOf"          : "2019-01-01",
            "pricePerMinute": 1.0,
            "timeInterval"  : null
    ]

    NewPriceDefinitionDto priceDefinition(Map properties = [:]) {
        properties = SAMPLE_NEW_PRICE_DEFINITION + properties

        return NewPriceDefinitionDto.builder()
                .pricePerMinute(properties.pricePerMinute as BigDecimal)
                .asOf(LocalDate.parse(properties.asOf as String))
                .timeInterval(mapNonNull(properties.timeInterval, { interval(it) }))
                .build()
    }

    List<NewPriceDefinitionDto> priceDefinitions(Map asOfAndDefaultPrice = [:], Map... priceAndInterval = []) {
        priceAndInterval.collect { asOfAndDefaultPrice + it }
                .collect { priceDefinition(it) }

    }

    LocalTimeIntervalDto interval(Object object) {
        if (object instanceof LocalTimeIntervalDto) return object
        if (object instanceof String) return interval([start: object.split("-")[0],
                                                       end  : object.split("-")[1]])
        Map properties = object as Map

        Map fullDayInterval = [start: LocalTime.MIN.toString(), end: LocalTime.MAX.toString()]

        properties = fullDayInterval + properties


        return LocalTimeIntervalDto.builder()
                .start(mapNonNull(properties.start, { LocalTime.parse(it as String) }))
                .end(mapNonNull(properties.end, {
                    LocalTime.parse(it as String == "24:00"
                            ? LocalTime.MAX.toString()
                            : it)
                }))
                .build()
    }
}