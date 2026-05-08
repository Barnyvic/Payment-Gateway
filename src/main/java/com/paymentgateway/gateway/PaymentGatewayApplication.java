package com.paymentgateway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.paymentgateway")
@EntityScan(basePackages = "com.paymentgateway.payments.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.paymentgateway.payments.infrastructure.persistence")
public class PaymentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
