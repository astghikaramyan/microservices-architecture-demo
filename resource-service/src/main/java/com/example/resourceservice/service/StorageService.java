package com.example.resourceservice.service;

import static com.example.resourceservice.constants.Constants.SERVICE_UNAVAILABLE_RESPONSE_CODE;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.util.DataPreparerService;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
    @Value("${s3.permanent-bucket-name}")
    private String permanentBucketName;
    @Value("${s3.staging-bucket-name}")
    private String stagingBucketName;
    @Value("${s3.endpoint}")
    private String s3Endpoint;

    @Retryable(
            retryFor = {AwsServiceException.class, SdkClientException.class, S3Exception.class},
            noRetryFor = {
                    HttpClientErrorException.BadRequest.class,
                    HttpClientErrorException.Conflict.class,
                    HttpClientErrorException.NotFound.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void addFileBytesToStorage(String s3Key, byte[] fileBytes, String bucketName) {
        try{
            s3Client.putObject(preparePutRequestData(s3Key, bucketName), RequestBody.fromBytes(fileBytes));
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
            retryFor = {AwsServiceException.class, SdkClientException.class, S3Exception.class},
            noRetryFor = {
                    HttpClientErrorException.BadRequest.class,
                    HttpClientErrorException.Conflict.class,
                    HttpClientErrorException.NotFound.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ResponseBytes<GetObjectResponse> retrieveFileFromStorage(String s3Key, String bucketName) {
        try{
            return s3Client.getObjectAsBytes(prepareGetRequestData(s3Key, bucketName));
        }catch (Exception e){
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RETRIEVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public ResponseBytes<GetObjectResponse> retrieveFileFromStorageFallback(Exception e, String s3Key, String bucketName) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RETRIEVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void recoverDeletedFileToStorage(String s3Key, byte[] fileBytes, String bucketName) {
        try{
            s3Client.putObject(preparePutRequestData(s3Key, bucketName), RequestBody.fromBytes(fileBytes));
        }catch (Exception e){
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RECOVERY_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public void recoverDeletedFileToStorageFallback(Exception e, String s3Key, byte[] fileBytes, String bucketName) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_RECOVERY_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteResourceFromStorage(String s3Key, String bucketName) {
        try {
            s3Client.deleteObject(prepareDeleteRequestData(s3Key, bucketName));
        } catch (Exception e) {
            throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_REMOVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Recover
    public void deleteResourceFromStorageFallback(Exception e, String s3Key, String bucketName) {
        throw new StorageException(dataPreparerService.prepareErrorResponse(String.format(STORAGE_REMOVAL_ERROR_MESSAGE, s3Key), SERVICE_UNAVAILABLE_RESPONSE_CODE));
    }

    public String prepareFileUrl(String s3Key, StorageMetadataResponse storageMetadata) {
        return prepareFileUrl(s3Key, prepareBucketPath(storageMetadata));
    }

    private String prepareFileUrl(String s3Key, String bucketPath) {
        return s3Endpoint + "/" + bucketPath + "/" + s3Key;
    }

    private String prepareBucketPath(StorageMetadataResponse storageMetadata) {
        return storageMetadata.getBucket() + storageMetadata.getPath();
    }

    private GetObjectRequest prepareGetRequestData(String s3Key, String bucketName) {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
    }

    private PutObjectRequest preparePutRequestData(String s3Key, String bucketName) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
    }

    private DeleteObjectRequest prepareDeleteRequestData(String s3Key, String bucketName) {
        return DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
    }
}

