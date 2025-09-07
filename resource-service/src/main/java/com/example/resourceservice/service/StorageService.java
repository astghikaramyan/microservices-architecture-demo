package com.example.resourceservice.service;

import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.util.DataPreparerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

import static com.example.resourceservice.service.ResourceService.SERVICE_UNAVAILABLE_RESPONSE_CODE;

@Component
public class StorageService {
    private static final String STORAGE_PERSISTENCE_ERROR_MESSAGE = "Failed to persist file to S3 for s3Key: %s";
    private static final String STORAGE_RETRIEVAL_ERROR_MESSAGE = "Failed to retrieve file from S3 for s3Key: %s";
    private static final String STORAGE_RECOVERY_ERROR_MESSAGE = "Failed to recover file to S3 for s3Key: %s";
    private static final String STORAGE_REMOVAL_ERROR_MESSAGE = "Failed to delete file from S3 for s3Key: %s";

    @Autowired
    private S3Client s3Client;
    @Autowired
    private DataPreparerService dataPreparerService;
    @Value("${s3.bucket}")
    private String BUCKET_NAME;
    @Value("${s3.endpoint}")
    private String S3_ENDPOINT;

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void addFileBytesToStorage(String s3Key, byte[] fileBytes) {
        try{
            s3Client.putObject(preparePutRequestData(s3Key), RequestBody.fromBytes(fileBytes));
        }catch (Exception e){
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_PERSISTENCE_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public void addFileBytesToStorageFallback(Exception e, String s3Key, InputStream fileStream) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_PERSISTENCE_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Recover
    public void addFileBytesToStorageFallback(Exception e, String s3Key, byte[] fileBytes) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RETRIEVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseBytes<GetObjectResponse> retrieveFileFromStorage(String s3Key) {
        try{
            return s3Client.getObjectAsBytes(prepareGetRequestData(s3Key));
        }catch (Exception e){
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RETRIEVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public ResponseBytes<GetObjectResponse> retrieveFileFromStorageFallback(Exception e, String s3Key) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RETRIEVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void recoverDeletedFileToStorage(String s3Key, byte[] fileBytes) {
        try{
            s3Client.putObject(preparePutRequestData(s3Key), RequestBody.fromBytes(fileBytes));
        }catch (Exception e){
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RECOVERY_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public void recoverDeletedFileToStorageFallback(Exception e, String s3Key, byte[] fileBytes) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RECOVERY_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteResourceFromStorage(String s3Key) {
        try {
            s3Client.deleteObject(prepareDeleteRequestData(s3Key));
        } catch (Exception e) {
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_REMOVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public void deleteResourceFromStorageFallback(Exception e, String s3Key) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_REMOVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    public String prepareFileUrl(String s3Key) {
        return S3_ENDPOINT + "/" + BUCKET_NAME + "/" + s3Key;
    }

    private GetObjectRequest prepareGetRequestData(String s3Key) {
        return GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }

    private PutObjectRequest preparePutRequestData(String s3Key) {
        return PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }

    private DeleteObjectRequest prepareDeleteRequestData(String s3Key) {
        return DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }
}

