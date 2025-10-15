package com.example.resourceservice.client;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.resourceservice.model.requestMetadata.RequestMetadata;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.model.storagemetadata.StorageType;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

@Component
public class StorageMetadataServiceClient {

  private static final Logger LOGGER = LogManager.getLogger(StorageMetadataServiceClient.class);
  @Value("${s3.permanent-bucket-name}")
  private String permanentBucketName;
  @Value("${s3.staging-bucket-name}")
  private String stagingBucketName;
  @Value("${s3.permanent-files-path}")
  private String permanentFilesPath;
  @Value("${s3.staging-files-path}")
  private String stagingFilesPath;
  @Value("${storage-metadata.service.url}")
  private String storageMetadataServiceUrl;
  private final RestTemplate storageRestTemplate;
  private final CircuitBreaker storageServiceCB;
  private final Retry storageServiceRetry;
  public StorageMetadataServiceClient(@Qualifier("storageRestTemplate") RestTemplate storageRestTemplate, CircuitBreaker storageServiceCB,
      Retry storageServiceRetry) {
    this.storageRestTemplate = storageRestTemplate;
    this.storageServiceCB = storageServiceCB;
    this.storageServiceRetry = storageServiceRetry;
  }

  public List<StorageMetadataResponse> getStoragesWithStorageServiceCB(RequestMetadata requestMetadata) {
    try {
      return storageServiceCB.executeSupplier(() -> {
        // Prepare headers and URI
        HttpEntity<?> httpEntity = new HttpEntity<>(prepareHeaders(requestMetadata));
        URI uri = prepareURI();
        if (uri == null) {
          LOGGER.error("Storage Metadata Service URL is not configured");
          return getStoragesFallback();
        }
        LOGGER.debug("Calling Storage Service, URI {}", uri);
        // Call with circuit breaker using executeSupplier
        ResponseEntity<List<StorageMetadataResponse>> responseEntity = storageRestTemplate.exchange(
            uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<>() {
            }
        );
        LOGGER.info("Received response from Storage Service for protected resource.");
        return responseEntity != null && responseEntity.getBody() != null
            ? responseEntity.getBody()
            : getStoragesFallback();
      });
    } catch (CallNotPermittedException ex) {
      // Circuit breaker is OPEN: default fallback
      LOGGER.warn("Circuit breaker is OPEN, returning stub storage data");
      return getStoragesFallback();
    } catch (Exception e) {
      LOGGER.error("Error occurred while calling Storage Service, returning stub", e);
      return getStoragesFallback();
    }
  }

  public List<StorageMetadataResponse> getStoragesWithCBAndRetry() {
    HttpEntity<?> httpEntity = new HttpEntity<>(prepareHeaders(null));
    URI uri = prepareURI();
    if (uri == null) {
      LOGGER.error("Storage Metadata Service URL is not configured");
      return getStoragesFallback();
    }

    LOGGER.debug("Calling Storage Service, URI {}", uri);

    Supplier<List<StorageMetadataResponse>> supplier = () -> {
      ResponseEntity<List<StorageMetadataResponse>> responseEntity = storageRestTemplate.exchange(
          uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<>() {}
      );
      return responseEntity != null && responseEntity.getBody() != null
          ? responseEntity.getBody()
          : getStoragesFallback();
    };

    // Decorate with Circuit Breaker first, then Retry
    Supplier<List<StorageMetadataResponse>> decorated =
        Retry.decorateSupplier(storageServiceRetry,
            CircuitBreaker.decorateSupplier(storageServiceCB, supplier)
        );

    try {
      return decorated.get();
    } catch (CallNotPermittedException ex) {
      // Circuit breaker is OPEN
      LOGGER.warn("Circuit breaker is open, using fallback", ex);
      return getStoragesFallback();
    } catch (Exception e) {
      // Any other exception
      LOGGER.error("Error occurred while calling Storage Service, returning stub", e);
      return getStoragesFallback();
    }
  }

  private URI prepareURI() {
    if (storageMetadataServiceUrl == null || storageMetadataServiceUrl.isEmpty()) {
      return null;
    }
    return UriComponentsBuilder.fromHttpUrl(storageMetadataServiceUrl)
        .path("/storages")
        .build()
        .toUri();
  }

  private HttpHeaders prepareHeaders(RequestMetadata requestMetadata) {
    HttpHeaders headers = new HttpHeaders();
    String traceId = ThreadContext.get("traceId");
    headers.add("X-Trace-Id", (Objects.nonNull(requestMetadata) && Objects.nonNull(requestMetadata.getTraceId())) ? requestMetadata.getTraceId() : traceId);
    headers.add("Content-Type", "application/json");
    return headers;
  }

  private List<StorageMetadataResponse> getStoragesFallback() {
    LOGGER.debug("Calling Storages Fallback");
    StorageMetadataResponse stub1 = new StorageMetadataResponse();
    stub1.setId(1L);
    stub1.setStorageType(StorageType.PERMANENT);
    stub1.setBucket(permanentBucketName);
    stub1.setPath(permanentFilesPath);
    StorageMetadataResponse stub2 = new StorageMetadataResponse();
    stub2.setId(2L);
    stub2.setStorageType(StorageType.STAGING);
    stub2.setBucket(stagingBucketName);
    stub2.setPath(stagingFilesPath);
    return List.of(stub1, stub2);
  }
}
