package com.example.resourceservice.messaging.consumer;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.example.resourceservice.client.StorageMetadataServiceClient;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.model.storagemetadata.StorageType;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.service.StorageService;
import com.example.resourceservice.util.DataPreparerService;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Configuration
public class ProcessSongMetadataListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessSongMetadataListener.class);
  private final StorageService storageService;
  private final ResourceRepository resourceRepository;
  private final StorageMetadataServiceClient storageMetadataServiceClient;
  private final DataPreparerService dataPreparerService;

  public ProcessSongMetadataListener(StorageService storageService,
      ResourceRepository resourceRepository,
      StorageMetadataServiceClient storageMetadataServiceClient,
      DataPreparerService dataPreparerService) {
    this.storageService = storageService;
    this.resourceRepository = resourceRepository;
    this.storageMetadataServiceClient = storageMetadataServiceClient;
    this.dataPreparerService = dataPreparerService;
  }

  @Bean
  public Consumer<Message<String>> processSongMetadata() {
    return message -> {
      try {
        String resourceId = message.getPayload();
        List<StorageMetadataResponse> storageMetadataResponses = retrieveStoragesMetadata();
        StorageMetadataResponse permanentStorageMetadata = filterStoragesMetadataByType(storageMetadataResponses, StorageType.PERMANENT);
        StorageMetadataResponse stagingStorageMetadata = filterStoragesMetadataByType(storageMetadataResponses, StorageType.STAGING);
        ResourceEntity resource = resourceRepository.findById(Integer.parseInt(resourceId))
            .orElseThrow(() -> new IllegalStateException("Resource not found"));
        if (resource.getFileName().contains(permanentStorageMetadata.getBucket())) {
          return;
        }
        ResponseBytes<GetObjectResponse> objectBytes = storageService.retrieveFileFromStorage(resource.getS3Key(), stagingStorageMetadata.getBucket());
        if (Objects.nonNull(objectBytes)) {
          storageService.addFileBytesToStorage(resource.getS3Key(), objectBytes.asByteArray(), permanentStorageMetadata.getBucket());
          resource.setFileName(storageService.prepareFileUrl(resource.getS3Key(), permanentStorageMetadata));
          resourceRepository.save(resource);
        }
        LOGGER.info("Processed resource ID: {} in permanent storage", resourceId);
      } catch (Exception e) {
        throw new RuntimeException(e); // triggers retry & DLQ
      }
    };
  }

  private List<StorageMetadataResponse> retrieveStoragesMetadata() {
    return storageMetadataServiceClient.getStoragesWithStorageServiceCB();
  }

  private StorageMetadataResponse filterStoragesMetadataByType(List<StorageMetadataResponse> storageMetadataResponses,
      StorageType storageType) {
    return storageMetadataResponses.stream()
        .filter(storageMetadataResponse -> storageMetadataResponse.getStorageType() == storageType)
        .findFirst()
        .orElse(null);
  }
}

