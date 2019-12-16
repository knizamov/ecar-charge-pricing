package com.example.ecar.chargepricing.domain

import com.example.ecar.chargepricing.dto.CalculateChargingCostsQuery
import com.example.ecar.chargepricing.dto.ChargingSessionFinished
import groovy.transform.CompileStatic

import java.time.OffsetDateTime

@CompileStatic
trait SampleChargeSessions {
    static String UNKNOWN_CUSTOMER_ID = "unknown"
    static String REGULAR_CUSTOMER_ID = "regular"
    static String VIP_CUSTOMER_ID = "vip"
    static String DEFAULT_CUSTOMER_ID = REGULAR_CUSTOMER_ID

    static final Map SAMPLE_CHARGE_SESSION_FINISHED = [
            "customerId": DEFAULT_CUSTOMER_ID,
            "start"     : "2019-12-03T11:30:00Z",
            "end"       : "2019-12-03T12:15:00Z"
    ]


    ChargingSessionFinished chargingSessionFinished(Map properties = [:]) {
        properties = SAMPLE_CHARGE_SESSION_FINISHED + properties

        return ChargingSessionFinished.builder()
                .customerId(properties.customerId as String)
                .start(OffsetDateTime.parse(properties.start as String))
                .end(OffsetDateTime.parse(properties.end as String))
                .build()
    }

    CalculateChargingCostsQuery calculateChargingCostsQuery(Map properties = [:]) {
        properties = SAMPLE_CHARGE_SESSION_FINISHED + properties

        return CalculateChargingCostsQuery.builder()
                .customerId(properties.customerId as String)
                .start(OffsetDateTime.parse(properties.start as String))
                .end(OffsetDateTime.parse(properties.end as String))
                .build()
    }

    ChargingSessionFinished oneDayChargingSessionFinished(Map properties = [:]) {
        if (properties["start"]) {
            properties["end"] = OffsetDateTime.parse(properties["start"] as String).plusDays(1).toString()
        } else if (properties["end"]) {
            properties["start"] = OffsetDateTime.parse(properties["end"] as String).minusDays(1).toString()
        } else {
            properties = [start: "2019-01-01T00:00:00Z", end: "2019-01-02T00:00:00Z"] + properties
        }
        return chargingSessionFinished(properties)
    }
}
