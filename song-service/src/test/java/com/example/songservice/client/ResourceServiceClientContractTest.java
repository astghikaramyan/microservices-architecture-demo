package com.example.songservice.client;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "resource-service")
public class ResourceServiceClientContractTest {

    private final String RESOURCE_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Pact(consumer = "song-service")
    public V4Pact resourceExists(PactBuilder builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder
                .usingLegacyDsl()
                .given("Resource exists with ID " + RESOURCE_ID)
                .uponReceiving("A request to get resource as bytes")
                .path("/resources/" + RESOURCE_ID)
                .method("GET")
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .headers(headers)
                .body("true")
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "song-service")
    public V4Pact resourceDoesNotExist(PactBuilder builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder
                .usingLegacyDsl()
                .given("Resource does not exist with ID " + RESOURCE_ID)
                .uponReceiving("A request to retrieve non-existent resource")
                .path("/resources/" + RESOURCE_ID)
                .method("GET")
                .willRespondWith()
                .status(HttpStatus.BAD_REQUEST.value())
                .headers(headers)
                .body("{\"errorMessage\":\"Resource with ID=" + RESOURCE_ID + " not found\",\"errorCode\":400}")
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "resourceExists")
    void testResourceExists(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/resources/" + RESOURCE_ID;

        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
    }

    @Test
    @PactTestFor(pactMethod = "resourceDoesNotExist")
    void testResourceDoesNotExist(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/resources/" + RESOURCE_ID;

        try {
            // Make the request expecting a JSON response as String
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Should not reach here because 400 throws exception
        } catch (HttpClientErrorException ex) {
            // Verify status code
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

            String body = ex.getResponseBodyAsString();
            assertTrue(body.contains("\"errorMessage\":\"Resource with ID=" + RESOURCE_ID + " not found\""));
            assertTrue(body.contains("\"errorCode\":400"));
        }
    }
}