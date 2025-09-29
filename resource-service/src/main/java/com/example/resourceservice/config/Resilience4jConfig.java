package com.example.resourceservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

  // Circuit Breaker Bean
  @Bean
  public CircuitBreaker storageServiceCircuitBreaker() {
    CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50) // % of failures before opening the breaker
        .waitDurationInOpenState(Duration.ofSeconds(10)) // how long to stay OPEN before trying half-open
        .slidingWindowType(SlidingWindowType.COUNT_BASED) // count-based sliding window
        .slidingWindowSize(5) // keep last 5 calls in the window
        .minimumNumberOfCalls(3) // require at least 3 calls before evaluating failure rate
        .permittedNumberOfCallsInHalfOpenState(2) // trial calls when half-open
        .automaticTransitionFromOpenToHalfOpenEnabled(true) // auto move to half-open after wait
        .recordExceptions(Exception.class) // what to treat as failure
        .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);

    return registry.circuitBreaker("storageServiceCB", circuitBreakerConfig);
  }

  // Retry Bean
  @Bean
  public Retry storageServiceRetry() {
    RetryConfig retryConfig = RetryConfig.custom()
        .maxAttempts(2) // Number of retry attempts
        .waitDuration(Duration.ofSeconds(2)) // Delay between retries
        .build();
    return Retry.of("storageServiceRetry", retryConfig);
  }
}
