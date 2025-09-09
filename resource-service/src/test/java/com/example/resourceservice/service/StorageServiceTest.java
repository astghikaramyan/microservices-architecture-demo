package com.example.resourceservice.service;

import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.util.DataPreparerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static com.example.resourceservice.service.ResourceService.SERVICE_UNAVAILABLE_RESPONSE_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private DataPreparerService dataPreparerService;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use reflection to set private fields
        java.lang.reflect.Field bucketField = StorageService.class.getDeclaredField("BUCKET_NAME");
        bucketField.setAccessible(true);
        bucketField.set(storageService, "test-bucket");

        java.lang.reflect.Field endpointField = StorageService.class.getDeclaredField("S3_ENDPOINT");
        endpointField.setAccessible(true);
        endpointField.set(storageService, "http://localhost:4566");
    }

    @Test
    void testAddFileBytesToStorage_success() {
        byte[] bytes = new byte[]{1,2,3};
        assertDoesNotThrow(() -> storageService.addFileBytesToStorage("key123", bytes));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testAddFileBytesToStorage_exception() {
        byte[] bytes = new byte[]{1,2,3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(dataPreparerService.prepareErrorResponse(anyString(), eq(SERVICE_UNAVAILABLE_RESPONSE_CODE)))
                .thenReturn(errorResponse);

        doThrow(new RuntimeException("S3 failure")).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.addFileBytesToStorage("key123", bytes));
        assertNotNull(ex);
    }

    @Test
    void testRetrieveFileFromStorage_success() {
        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        ResponseBytes<GetObjectResponse> result = storageService.retrieveFileFromStorage("key123");
        assertNotNull(result);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void testRetrieveFileFromStorage_exception() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(dataPreparerService.prepareErrorResponse(anyString(), eq(SERVICE_UNAVAILABLE_RESPONSE_CODE)))
                .thenReturn(errorResponse);
        doThrow(new RuntimeException("S3 get error")).when(s3Client).getObjectAsBytes(any(GetObjectRequest.class));

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.retrieveFileFromStorage("key123"));
        assertNotNull(ex);
    }

    @Test
    void testRecoverDeletedFileToStorage_success() {
        byte[] bytes = new byte[]{1,2,3};
        assertDoesNotThrow(() -> storageService.recoverDeletedFileToStorage("key123", bytes));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testRecoverDeletedFileToStorage_exception() {
        byte[] bytes = new byte[]{1,2,3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(dataPreparerService.prepareErrorResponse(anyString(), eq(SERVICE_UNAVAILABLE_RESPONSE_CODE)))
                .thenReturn(errorResponse);
        doThrow(new RuntimeException("S3 failure")).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.recoverDeletedFileToStorage("key123", bytes));
        assertNotNull(ex);
    }

    @Test
    void testDeleteResourceFromStorage_success() {
        assertDoesNotThrow(() -> storageService.deleteResourceFromStorage("key123"));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteResourceFromStorage_exception() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(dataPreparerService.prepareErrorResponse(anyString(), eq(SERVICE_UNAVAILABLE_RESPONSE_CODE)))
                .thenReturn(errorResponse);
        doThrow(new RuntimeException("S3 delete error")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.deleteResourceFromStorage("key123"));
        assertNotNull(ex);
            }

    @Test
    void testPrepareFileUrl() {
        String url = storageService.prepareFileUrl("key123");
        assertEquals("http://localhost:4566/test-bucket/key123", url);
    }
}
