package com.paymentgateway.payments.infrastructure.persistence;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceConfig {

    @Bean
    public Clock systemUtcClock() {
        return Clock.systemUTC();
    }
}
