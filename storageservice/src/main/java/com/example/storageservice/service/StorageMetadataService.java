package com.example.storageservice.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.storageservice.entity.StorageEntity;
import com.example.storageservice.exception.DatabaseException;
import com.example.storageservice.exception.InvalidDataException;
import com.example.storageservice.repository.StorageMetadataRepository;
import com.example.storageservice.util.DataPreparerService;

import io.github.resilience4j.retry.Retry;

@Service
public class StorageMetadataService {

  private static final Logger LOGGER = LogManager.getLogger(StorageMetadataService.class);
  private static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
  private static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
  public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
  public static final String BAD_REQUEST_RESPONSE_CODE = "400";
  public static final String INTERNAL_SERVER_ERROR_RESPONSE_CODE = "500";
  private static final String DATABASE_ERROR_MESSAGE = "Resource operation could not be completed";
  private final StorageMetadataRepository repository;
  private final DataPreparerService dataPreparerService;
  private final Retry retryDBOperationConfig;

  public StorageMetadataService(StorageMetadataRepository repository, DataPreparerService dataPreparerService,
      @Qualifier("retryDBOperationConfig") Retry retryDBOperationConfig) {
    this.repository = repository;
    this.dataPreparerService = dataPreparerService;
    this.retryDBOperationConfig = retryDBOperationConfig;
  }

  public StorageEntity addStorageMetadata(StorageEntity storageEntity) {
    try {
      return retryDBOperationConfig.executeCallable(() -> {
        return repository.save(storageEntity);
      });
    } catch (Exception e) {
      LOGGER.error("All retries failed while saving storage", e);
      throw new DatabaseException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  public List<StorageEntity> findStoragesMetadata() {
    try {
      return retryDBOperationConfig.executeCallable(() -> {
        return repository.findAll();
      });
    } catch (Exception e) {
      LOGGER.error("All retries failed while saving storage", e);
      throw new DatabaseException(
          dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
    }
  }

  public Map<String, List<Integer>> deleteStorageMetadataByIds(final String id) {
    validateResourceIds(id);
    String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
    List<Integer> removedIds = new LinkedList<>();
    for (String param : ids) {
      Integer resourceId = Integer.valueOf(param);
      Optional<StorageEntity> storage = repository.findById(resourceId);
      if (storage.isPresent()) {
        try {
          repository.delete(storage.get());
          removedIds.add(resourceId);
        } catch (Exception e) {
          LOGGER.error("Unexpected error occurred while deleting resource for resource ID={}. Error: {}", resourceId,
              e.getMessage(), e);
          throw new DatabaseException(
              dataPreparerService.prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
      }
    }
    final Map<String, List<Integer>> responseObject = new HashMap<>();
    responseObject.put("ids", removedIds);
    return responseObject;
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

  private boolean isNumeric(final String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidNumeric(String id) {
    final boolean isWholeNumber = Optional.ofNullable(id)
        .map(s -> s.chars().allMatch(Character::isDigit))
        .orElse(false);
    return isWholeNumber && Integer.parseInt(id) > 0;
  }
}
