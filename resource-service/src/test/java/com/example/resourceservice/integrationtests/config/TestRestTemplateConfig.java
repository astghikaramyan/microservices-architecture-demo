package com.example.resourceservice.integrationtests.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class TestRestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplateBean(RestTemplateBuilder builder) {
        return builder.build(); // no load balancing
    }
}
