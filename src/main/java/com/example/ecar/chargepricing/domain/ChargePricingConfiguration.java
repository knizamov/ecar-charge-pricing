package com.example.ecar.chargepricing.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ChargePricingConfiguration {

    @Bean
    ChargePricingFacade chargePricingFacade(CustomerClient customerClient) {
        InMemoryDailyPriceDefinitionRepository inMemoryDailyPriceDefinitionRepository = new InMemoryDailyPriceDefinitionRepository();
        return chargePricingFacade(inMemoryDailyPriceDefinitionRepository, customerClient);
    }

    ChargePricingFacade chargePricingFacade(DailyPriceDefinitionRepository dailyPriceDefinitionRepository, CustomerClient customerClient) {
        ChargingCostsCalculator chargingCostsCalculator = new ChargingCostsCalculator();

        return new ChargePricingFacade(dailyPriceDefinitionRepository, chargingCostsCalculator, customerClient);
    }
}
