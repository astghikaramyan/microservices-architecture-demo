package com.example.resourceservice.service;

import static com.example.resourceservice.constants.Constants.BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE;
import static com.example.resourceservice.constants.Constants.BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE;
import static com.example.resourceservice.constants.Constants.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;
import static com.example.resourceservice.constants.Constants.BAD_REQUEST_RESPONSE_CODE;
import static com.example.resourceservice.constants.Constants.DATABASE_ERROR_MESSAGE;
import static com.example.resourceservice.constants.Constants.INTERNAL_SERVER_ERROR_RESPONSE_CODE;
import static com.example.resourceservice.constants.Constants.NOT_FOUND_REQUEST_RESPONSE_CODE;
import static com.example.resourceservice.constants.Constants.NOT_FOUNT_RESOURCE_ERROR_MESSAGE;
import static com.example.resourceservice.constants.Constants.SERVICE_UNAVAILABLE_RESPONSE_CODE;
import static com.example.resourceservice.constants.Constants.STORAGE_ERROR_MESSAGE;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.client.StorageMetadataServiceClient;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.DatabaseException;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.exception.NotFoundException;
import com.example.resourceservice.exception.SongClientException;
import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.exception.StreamBridgeException;
import com.example.resourceservice.messaging.producer.CreateResourceMetadataPublisher;
import com.example.resourceservice.model.requestMetadata.RequestMetadata;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.model.storagemetadata.StorageType;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.util.DataPreparerService;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class ResourceService {

  private static final Logger LOGGER = LogManager.getLogger(ResourceService.class);
  public static final String CREATE_RESOURCE_METADATA_OUT = "createResourceMetadata-out-0";
  private final ResourceRepository repository;
  private final StorageService storageService;
  private final DataPreparerService dataPreparerService;
  private final SongServiceClient songServiceClient;
  private final StorageMetadataServiceClient storageMetadataServiceClient;
  private final CreateResourceMetadataPublisher createResourceMetadataPublisher;
  private final String permanentBucketName;
  private final String stagingBucketName;

  public ResourceService(ResourceRepository repository,
      StreamBridge streamBridge,
      StorageService storageService,
      DataPreparerService dataPreparerService,
      SongServiceClient songServiceClient,
      StorageMetadataServiceClient storageMetadataServiceClient,
      CreateResourceMetadataPublisher createResourceMetadataPublisher,
      @Value("${s3.permanent-bucket-name}") String permanentBucketName,
      @Value("${s3.staging-bucket-name}") String stagingBucketName) {
    this.repository = repository;
    this.storageService = storageService;
    this.dataPreparerService = dataPreparerService;
    this.songServiceClient = songServiceClient;
    this.storageMetadataServiceClient = storageMetadataServiceClient;
    this.createResourceMetadataPublisher = createResourceMetadataPublisher;
    this.permanentBucketName = permanentBucketName;
    this.stagingBucketName = stagingBucketName;
  }

  public ResourceEntity uploadResource(byte[] fileBytes, RequestMetadata requestMetadata) {
    String s3Key = "resources/" + UUID.randomUUID() + ".mp3";
    StorageMetadataResponse storageMetadata = retrieveStorageMetadata(StorageType.STAGING, requestMetadata);
    if (Objects.nonNull(storageMetadata)) {
      try {
        storageService.addFileBytesToStorage(s3Key, fileBytes, storageMetadata.getBucket());
      } catch (Exception e) {
        throw new StorageException(
            dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
      }
      Integer resourceId = null;
      try {
        ResourceEntity resource = saveResource(
            prepareResource(s3Key, storageService.prepareFileUrl(s3Key, storageMetadata)));
        resourceId = resource.getId();
        createResourceMetadataPublisher.sendCreateResourceMetadataEvent(CREATE_RESOURCE_METADATA_OUT,
            prepareMessage(resourceId, requestMetadata));
        return resource;
      } catch (DatabaseException e) {
        try {
          storageService.deleteResourceFromStorage(s3Key, storageMetadata.getBucket());
        } catch (StorageException e1) {
          LOGGER.error("Failed to delete file bytes from storage for not saved resource for s3key with name={}", s3Key,
              e1);
        }
        throw new DatabaseException(
            dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
      } catch (StreamBridgeException e) {
        LOGGER.error("Failed to send message through message broker for resource ID={} after retries. Error: {}",
            resourceId, e.getMessage(), e);
      } catch (Exception e) {
        LOGGER.error("Unexpected error occurred while uploading resource. Error: {}", e.getMessage(), e);
      }
    }
    return null;
  }

  private Message<Integer> prepareMessage(Integer resourceId, RequestMetadata requestMetadata) {
    // Get traceId from MDC (ThreadContext)
    String traceId = (Objects.nonNull(requestMetadata) && Objects.nonNull(requestMetadata.getTraceId())) ? requestMetadata.getTraceId() : ThreadContext.get("traceId");
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
      ThreadContext.put("traceId", traceId);
    }
    LOGGER.info("Preparing message for resource ID={}", resourceId);

    // Build a new message with the traceId header
    return MessageBuilder
        .withPayload(resourceId)
        .setHeader("X-Trace-Id", traceId)
        .build();
  }

  @Retryable(
      value = Exception.class,
      maxAttempts = 2,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  @Transactional
  public ResourceEntity saveResource(ResourceEntity resourceEntity) {
    try {
      return this.repository.save(resourceEntity);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new DatabaseException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  @Recover
  public ResourceEntity recoverSaveResource(Exception e, ResourceEntity resourceEntity) {
    LOGGER.error("Failed to save resource after retries. Error: {}", e.getMessage(), e);
    throw new DatabaseException(
        dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
  }

  public byte[] getFileAsBytes(final Integer id) {
    validateResourceId(id);
    Optional<ResourceEntity> resourceOpt;
    try {
      resourceOpt = this.repository.findById(id);
    } catch (Exception e) {
      throw new DatabaseException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
    if (resourceOpt.isPresent() && Objects.isNull(resourceOpt.get()) || Objects.isNull(resourceOpt.get().getS3Key())) {
      throw new NotFoundException(
          dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id),
              NOT_FOUND_REQUEST_RESPONSE_CODE));
    }
    try {
      StorageMetadataResponse storageMetadata = retrieveStorageMetadata(
          resourceOpt.get().getFileName().contains(stagingBucketName) ? StorageType.STAGING : StorageType.PERMANENT, null);
      if (Objects.nonNull(storageMetadata)) {
        ResponseBytes<GetObjectResponse> objectBytes = storageService.retrieveFileFromStorage(
            resourceOpt.get().getS3Key(), storageMetadata.getBucket());
        if (Objects.isNull(objectBytes)) {
          throw new NotFoundException(
              dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id),
                  NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
        return objectBytes.asByteArray();
      }
      throw new NotFoundException(
          dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id),
              NOT_FOUND_REQUEST_RESPONSE_CODE));
    } catch (Exception e) {
      throw new StorageException(
          dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }
  }

  @Transactional
  public Map<String, List<Integer>> deleteResourceByIds(final String id) {
    validateResourceIds(id);
    String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
    List<Integer> removedIds = new LinkedList<>();
    for (String param : ids) {
      Integer resourceId = Integer.valueOf(param);
      Optional<ResourceEntity> resourceOpt = repository.findById(resourceId);
      if (resourceOpt.isPresent()) {
        ResourceEntity resource = resourceOpt.get();
        final String s3Key = resource.getS3Key();
        byte[] fileBytes = getFileAsBytes(resourceId);
        String bucketName = permanentBucketName;
        StorageMetadataResponse storageMetadata = retrieveStorageMetadata(
            resource.getFileName().contains(stagingBucketName) ? StorageType.STAGING : StorageType.PERMANENT, null);
        if (Objects.nonNull(storageMetadata)) {
          bucketName = storageMetadata.getBucket();
        }
        try {
          this.storageService.deleteResourceFromStorage(s3Key, bucketName);
          this.deleteResourceWithMetadata(resourceId);
          removedIds.add(resourceId);
        } catch (StorageException e) {
          throw new StorageException(
              dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        } catch (SongClientException songClientException) {
          try {
            storageService.recoverDeletedFileToStorage(s3Key, fileBytes, bucketName);
          } catch (StorageException e1) {
            LOGGER.error("Failed to recover deleted file bytes in storage for resource ID={}", resourceId, e1);
          }
          throw new SongClientException(
              dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        } catch (DatabaseException e) {
          try {
            storageService.recoverDeletedFileToStorage(s3Key, fileBytes, bucketName);
          } catch (StorageException e1) {
            LOGGER.error("Failed to recover deleted file bytes in storage for resource ID={}", resourceId, e1);
          }
          throw new DatabaseException(
              dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        } catch (Exception e) {
          LOGGER.error("Unexpected error occurred while deleting resource for resource ID={}. Error: {}", resourceId,
              e.getMessage(), e);
        }
      }
    }
    final Map<String, List<Integer>> responseObject = new HashMap<>();
    responseObject.put("ids", removedIds);
    return responseObject;
  }

  @Retryable(
      value = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public void deleteResourceWithMetadata(Integer resourceId) {
    try {
      songServiceClient.deleteResourceMetadataByResourceId(resourceId);
      repository.deleteById(resourceId);
    } catch (SongClientException songClientException) {
      LOGGER.error(songClientException.getMessage(), songClientException);
      throw new SongClientException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new DatabaseException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  @Recover
  public void recoverDeleteResource(Exception e, Integer resourceId) {
    LOGGER.error("Failed to delete resource with ID={} after retries. Error: {}", resourceId, e.getMessage(), e);
    throw new DatabaseException(
        dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
  }

  public boolean existById(final Integer id) {
    return this.repository.existsById(id);
  }

  private void validateResourceIds(String id) {
    if (Objects.nonNull(id) && id.length() > 200) {
      throw new InvalidDataException(
          dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE, id.length()),
              BAD_REQUEST_RESPONSE_CODE));
    }
    String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
    for (String param : ids) {
      if (!isNumeric(param)) {
        throw new InvalidDataException(
            dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, param),
                BAD_REQUEST_RESPONSE_CODE));
      }
      if (!isValidNumeric(param)) {
        throw new InvalidDataException(
            dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, param),
                BAD_REQUEST_RESPONSE_CODE));
      }
    }
  }

  private boolean isValidNumeric(String id) {
    final boolean isWholeNumber = Optional.ofNullable(id)
        .map(s -> s.chars().allMatch(Character::isDigit))
        .orElse(false);
    return isWholeNumber && Integer.parseInt(id) > 0;
  }

  private boolean isNumeric(final String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private void validateResourceId(Integer id) {
    if (!isNumeric(String.valueOf(id))) {
      throw new InvalidDataException(
          dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, id),
              BAD_REQUEST_RESPONSE_CODE));
    }
    if (!isValidNumeric(String.valueOf(id))) {
      throw new InvalidDataException(
          dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, id),
              BAD_REQUEST_RESPONSE_CODE));
    }
    if (!this.existById(Integer.valueOf(id))) {
      throw new NotFoundException(
          dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id),
              NOT_FOUND_REQUEST_RESPONSE_CODE));
    }
  }

  private ResourceEntity prepareResource(String s3Key, String fileName) {
    ResourceEntity resource = new ResourceEntity();
    resource.setS3Key(s3Key);
    resource.setFileName(fileName);
    resource.setUploadedAt(LocalDateTime.now());
    return resource;
  }

  private StorageMetadataResponse retrieveStorageMetadata(StorageType storageType, RequestMetadata requestMetadata) {
    return storageMetadataServiceClient.getStoragesWithStorageServiceCB(requestMetadata).stream()
        .filter(storageMetadataResponse -> storageMetadataResponse.getStorageType() == storageType)
        .findFirst()
        .orElse(null);
  }
}

