package com.example.resourceservice.integrationtests.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@TestConfiguration
public class DisableEurekaTestConfig {

    public static void disableEureka(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> false);
        registry.add("spring.cloud.discovery.enabled", () -> false);
    }
}
