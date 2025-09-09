package com.example.resourceservice.integrationtests.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
@Testcontainers
public class RabbitMQTestConfig {

    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.12-management-alpine");

    static {
        rabbitMQ.start();
    }

    public static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);

        // For Spring Cloud Stream binding (exchange)
        registry.add("APP_RABBITMQ_EXCHANGE", () -> "resource.exchange");

        // Force Cloud Stream binder to Rabbit
        registry.add("spring.cloud.stream.binders.rabbit.type", () -> "rabbit");
        registry.add("spring.cloud.stream.defaultBinder", () -> "rabbit");
    }
}
