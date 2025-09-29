package com.example.resourceservice.service;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.NotFoundException;
import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.util.DataPreparerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.cloud.stream.function.StreamBridge;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ResourceServiceTest {
    @Mock
    private ResourceRepository repository;
    @Mock
    private S3Client s3Client;
    @Mock
    private StreamBridge streamBridge;
    @Mock
    private StorageService storageService;
    @Mock
    private DataPreparerService dataPreparerService;
    @Mock
    private SongServiceClient songServiceClient;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void uploadResource_success() {
        byte[] fileBytes = new byte[]{1, 2, 3};
        Mockito.when(storageService.prepareFileUrl(Mockito.anyString(), Mockito.any(StorageMetadataResponse.class))).thenReturn("fileUrl");
        org.mockito.Mockito.doNothing().when(storageService).addFileBytesToStorage(org.mockito.Mockito.anyString(), org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
        ResourceEntity entity = new ResourceEntity();
        entity.setId(1);
        org.mockito.Mockito.when(repository.save(org.mockito.Mockito.any())).thenReturn(entity);
        ResourceEntity result = resourceService.uploadResource(fileBytes);
        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void uploadResource_storageException() {
        byte[] fileBytes = new byte[]{1, 2, 3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        org.mockito.Mockito.doThrow(new RuntimeException()).when(storageService).addFileBytesToStorage(org.mockito.Mockito.anyString(), org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
        org.mockito.Mockito.when(dataPreparerService.prepareErrorResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(errorResponse);
        assertThrows(StorageException.class, () -> resourceService.uploadResource(fileBytes));
    }

    @Test
    void getFileAsBytes_success() {
        ResourceEntity entity = new ResourceEntity();
        entity.setId(1);
        entity.setS3Key("key");
        org.mockito.Mockito.when(repository.existsById(1)).thenReturn(true);
        org.mockito.Mockito.when(repository.getById(1)).thenReturn(entity);
        ResponseBytes<GetObjectResponse> responseBytes = org.mockito.Mockito.mock(ResponseBytes.class);
        org.mockito.Mockito.when(storageService.retrieveFileFromStorage("key", "test-bucket")).thenReturn(responseBytes);
        org.mockito.Mockito.when(responseBytes.asByteArray()).thenReturn(new byte[]{1, 2, 3});
        byte[] result = resourceService.getFileAsBytes(1);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
    }

    @Test
    void getFileAsBytes_notFound() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        org.mockito.Mockito.when(repository.existsById(1)).thenReturn(true);
        org.mockito.Mockito.when(repository.getById(1)).thenReturn(null);
        org.mockito.Mockito.when(dataPreparerService.prepareErrorResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(errorResponse);
        assertThrows(NotFoundException.class, () -> resourceService.getFileAsBytes(1));
    }

    @Test
    void existById_true() {
        org.mockito.Mockito.when(repository.existsById(1)).thenReturn(true);
        assertTrue(resourceService.existById(1));
    }

    @Test
    void existById_false() {
        org.mockito.Mockito.when(repository.existsById(2)).thenReturn(false);
        assertFalse(resourceService.existById(2));
    }
}