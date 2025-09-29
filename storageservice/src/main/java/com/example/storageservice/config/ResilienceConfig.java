package com.example.storageservice.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ResilienceConfig {

  @Value("${retry.waitDuration:1000}")
  private Integer waitDuration;
  @Value("${retry.reconnection.attempt:2}")
  private Integer noOfReconnectionAttempt;

  @Bean(name = "retryDBOperationConfig")
  public Retry retryDBOperationConfig() {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(noOfReconnectionAttempt)
        .waitDuration(Duration.ofMillis(waitDuration))
        .retryOnException(e -> e instanceof Exception)
        .retryExceptions(Exception.class)
        .ignoreExceptions(IOException.class)
        .build();
    return RetryRegistry.of(config).retry("Retry-DBOperation");
  }
}