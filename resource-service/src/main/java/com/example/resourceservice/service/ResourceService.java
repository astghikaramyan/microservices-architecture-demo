package com.example.resourceservice.service;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.*;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.util.DataPreparerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
    public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
    private static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
    public static final String BAD_REQUEST_RESPONSE_CODE = "400";
    private static final String NOT_FOUND_REQUEST_RESPONSE_CODE = "404";
    private static final String STORAGE_ERROR_MESSAGE = "Failed to upload file to S3";
    public static final String INTERNAL_SERVER_ERROR_RESPONSE_CODE = "500";
    public static final String SERVICE_UNAVAILABLE_RESPONSE_CODE = "503";
    private static final String DATABASE_ERROR_MESSAGE = "Resource operation could not be completed";
    private static final String NOT_FOUNT_RESOURCE_ERROR_MESSAGE = "Resource with ID=%s not found";
    public static final String CREATE_RESOURCE_METADATA_OUT = "createResourceMetadata-out-0";
    public static final String DELETE_RESOURCE_METADATA_OUT = "deleteResourceMetadata-out-0";

    @Autowired
    private ResourceRepository repository;
    @Autowired
    private S3Client s3Client;
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private StorageService storageService;
    @Autowired
    private DataPreparerService dataPreparerService;
    @Autowired
    private SongServiceClient songServiceClient;

    public ResourceEntity uploadResource(byte[] fileBytes) {
        String s3Key = "resources/" + UUID.randomUUID() + ".mp3";
        String filePath = storageService.prepareFileUrl(s3Key);
        try {
            storageService.addFileBytesToStorage(s3Key, fileBytes);
        } catch (Exception e) {
            throw new StorageException(dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
        Integer resourceId = null;
        try {
            ResourceEntity resource = saveResource(prepareResource(s3Key, filePath));
            resourceId = resource.getId();
            sendMessageThroughStreamBridge(CREATE_RESOURCE_METADATA_OUT, MessageBuilder.withPayload(resourceId).build());
            return resource;
        } catch (DatabaseException e) {
            try {
                storageService.deleteResourceFromStorage(s3Key);
            } catch (StorageException e1) {
                LOGGER.error("Failed to delete file bytes from storage for not saved resource for s3key with name={}", s3Key, e1);
            }
            throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        } catch (StreamBridgeException e) {
            LOGGER.error("Failed to send message through message broker for resource ID={} after retries. Error: {}", resourceId, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred while uploading resource. Error: {}", e.getMessage(), e);
        }
        return null;
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private ResourceEntity saveResource(ResourceEntity resourceEntity) {
        try {
            return this.repository.save(resourceEntity);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
    }

    @Recover
    public ResourceEntity recoverSaveResource(Exception e, ResourceEntity resourceEntity) {
        LOGGER.error("Failed to save resource after retries. Error: {}", e.getMessage(), e);
        throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }

    public byte[] getFileAsBytes(final Integer id) {
        validateResourceId(id);
        ResourceEntity resource;
        try {
            resource = this.repository.getById(id);
        } catch (Exception e) {
            throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
        if (Objects.isNull(resource) || Objects.isNull(resource.getS3Key())) {
            throw new NotFoundException(dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
        try {
            ResponseBytes<GetObjectResponse> objectBytes = storageService.retrieveFileFromStorage(resource.getS3Key());
            if (Objects.isNull(objectBytes)) {
                throw new NotFoundException(dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
            }
            return objectBytes.asByteArray();
        } catch (Exception e) {
            throw new StorageException(dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

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
                try {
                    this.storageService.deleteResourceFromStorage(s3Key);
                    this.deleteResourceWithMetadata(resourceId);
                    removedIds.add(resourceId);
                    sendMessageThroughStreamBridge(DELETE_RESOURCE_METADATA_OUT, MessageBuilder.withPayload(resourceId).build());
                } catch (StorageException e) {
                    throw new StorageException(dataPreparerService.prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
                } catch (SongClientException songClientException) {
                    try {
                        storageService.recoverDeletedFileToStorage(s3Key, fileBytes);
                    } catch (StorageException e1) {
                        LOGGER.error("Failed to recover deleted file bytes in storage for resource ID={}", resourceId, e1);
                    }
                    throw new SongClientException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
                } catch (DatabaseException e) {
                    try {
                        storageService.recoverDeletedFileToStorage(s3Key, fileBytes);
                    } catch (StorageException e1) {
                        LOGGER.error("Failed to recover deleted file bytes in storage for resource ID={}", resourceId, e1);
                    }
                    throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
                } catch (StreamBridgeException e) {
                    LOGGER.error("Failed to send message through message broker for deleted resource with ID={} after retries. Error: {}", resourceId, e.getMessage(), e);
                } catch (Exception e) {
                    LOGGER.error("Unexpected error occurred while deleting resource for resource ID={}. Error: {}", resourceId, e.getMessage(), e);
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
    private void sendMessageThroughStreamBridge(String outBindingName, Message<Integer> message) {
        try {
            streamBridge.send(outBindingName, message);
        } catch (Exception e){
            throw  new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
    }

    @Recover
    public void recoverSendMessageThroughStreamBridge(Exception e, String outBindingName, Message<Integer> message) {
        LOGGER.error("Failed to send message through StreamBridge after retries. Error: {}", e.getMessage(), e);
        throw new StreamBridgeException(dataPreparerService.prepareErrorResponse("Failed to send message through StreamBridge", INTERNAL_SERVER_ERROR_RESPONSE_CODE));
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
            throw new SongClientException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
    }

    @Recover
    public void recoverDeleteResource(Exception e, Integer resourceId) {
        LOGGER.error("Failed to delete resource with ID={} after retries. Error: {}", resourceId, e.getMessage(), e);
        throw new DatabaseException(dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }

    public boolean existById(final Integer id) {
        return this.repository.existsById(id);
    }

    private void validateResourceIds(String id) {
        if (Objects.nonNull(id) && id.length() > 200) {
            throw new InvalidDataException(dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE, id.length()), BAD_REQUEST_RESPONSE_CODE));
        }
        String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
        for (String param : ids) {
            if (!isNumeric(param)) {
                throw new InvalidDataException(dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
            if (!isValidNumeric(param)) {
                throw new InvalidDataException(dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
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
            throw new InvalidDataException(dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!isValidNumeric(String.valueOf(id))) {
            throw new InvalidDataException(dataPreparerService.prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!this.existById(Integer.valueOf(id))) {
            throw new NotFoundException(dataPreparerService.prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
    }

    private ResourceEntity prepareResource(String s3Key, String fileName) {
        ResourceEntity resource = new ResourceEntity();
        resource.setS3Key(s3Key);
        resource.setFileName(fileName);
        resource.setUploadedAt(LocalDateTime.now());
        return resource;
    }
}

