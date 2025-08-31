package com.example.resourceprocessor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ResourceServiceClient {
    @Value("${resource.service.url}")
    private String resourceServiceUrl;

    private final RestTemplate restTemplate;

    public ResourceServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public byte[] getResourceBinary(String resourceId) {
        String url = resourceServiceUrl + "/resources/" + resourceId;
        return restTemplate.getForObject(url, byte[].class);
    }

    @Recover
    public void recover(Exception e, String resourceId) {
        throw new RuntimeException(e);
    }
}