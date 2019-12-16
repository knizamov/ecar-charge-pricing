package com.example.ecar.chargepricing.domain;

import com.example.ecar.shared.lang.Nullable;
import lombok.Value;

public interface CustomerClient {

    @Nullable CustomerDto getCustomerDetails(@Nullable String customerId);

    @Value
    public static class CustomerDto {
        CustomerStatus customerStatus;

        public enum CustomerStatus {VIP, REGULAR}
    }
}
