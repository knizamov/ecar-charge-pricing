package com.example.ecar.chargepricing.domain.infrastructure.customer;

import com.example.ecar.chargepricing.domain.CustomerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CustomerClientConfiguration {

    @Bean
    CustomerClient fakeCustomerClient() {
        return new CustomerClient() {
            @Override
            public CustomerDto getCustomerDetails(String customerId) {
                if ("vip".equalsIgnoreCase(customerId)) {
                    return new CustomerDto(CustomerDto.CustomerStatus.VIP);
                } else if ("regular".equalsIgnoreCase(customerId)) {
                    return new CustomerDto(CustomerDto.CustomerStatus.REGULAR);
                } else {
                    return null;
                }
            }
        };
    }
}
