package com.example.resourceservice.integrationtests.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
@Testcontainers
public class SongServiceWireMockConfig {

    // Start WireMock in a Testcontainer
    @Container
    public static GenericContainer<?> wiremockContainer =
            new GenericContainer<>("wiremock/wiremock:3.5.2")
                    .withExposedPorts(8080);

    static {
        wiremockContainer.start();
    }

    public static int getPort() {
        return wiremockContainer.getMappedPort(8080);
    }

    public static String getHost() {
        return wiremockContainer.getHost();
    }

    // Expose WireMock URL as Spring property for tests
    public static void configureWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("song.service.url", () ->
                "http://" + wiremockContainer.getHost() + ":" + wiremockContainer.getMappedPort(8080));
    }
}
