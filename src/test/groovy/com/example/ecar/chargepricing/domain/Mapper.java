package com.example.ecar.chargepricing.domain;

import java.util.function.Function;

public class Mapper {
    static <T, R> R mapNonNull(T object, Function<T, R> mapper) {
        if (object == null) {
            return null;
        }

        return mapper.apply(object);
    }
}


