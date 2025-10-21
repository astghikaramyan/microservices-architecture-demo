package com.example.resourceservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.cloud.stream.function.StreamBridge;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.client.StorageMetadataServiceClient;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.NotFoundException;
import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.model.storagemetadata.StorageMetadataResponse;
import com.example.resourceservice.model.storagemetadata.StorageType;
import com.example.resourceservice.repository.OutboxEventRepository;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.util.DataPreparerService;

import software.amazon.awssdk.services.s3.S3Client;

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
    @Mock
    private StorageMetadataServiceClient storageMetadataServiceClient;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void uploadResource_success() {
        byte[] fileBytes = new byte[]{1, 2, 3};
        when(storageService.prepareFileUrl(Mockito.anyString(), any(StorageMetadataResponse.class))).thenReturn("fileUrl");
        org.mockito.Mockito.doNothing().when(storageService).addFileBytesToStorage(org.mockito.Mockito.anyString(), any(), org.mockito.Mockito.anyString());
        when(storageMetadataServiceClient.getStoragesWithStorageServiceCB(any())).thenReturn(prepareStorageMetadata());
        when(outboxEventRepository.save(any())).thenReturn(null);
        ResourceEntity entity = new ResourceEntity();
        entity.setId(1);
        when(repository.save(any())).thenReturn(entity);
        ResourceEntity result = resourceService.uploadResource(fileBytes, null);
        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void uploadResource_storageException() {
        byte[] fileBytes = new byte[]{1, 2, 3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        org.mockito.Mockito.doThrow(new RuntimeException()).when(storageService).addFileBytesToStorage(org.mockito.Mockito.anyString(), any(), org.mockito.Mockito.anyString());
        when(dataPreparerService.prepareErrorResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(errorResponse);
        when(storageMetadataServiceClient.getStoragesWithStorageServiceCB(any())).thenReturn(prepareStorageMetadata());
        assertThrows(StorageException.class, () -> resourceService.uploadResource(fileBytes, null));
    }

    @Test
    void getFileAsBytes_notFound() {
        ResourceEntity resource = new ResourceEntity();
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(repository.existsById(1)).thenReturn(true);
        when(repository.findById(1)).thenReturn(Optional.of(resource));
        when(dataPreparerService.prepareErrorResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(errorResponse);
        assertThrows(NotFoundException.class, () -> resourceService.getFileAsBytes(1));
    }

    @Test
    void existById_true() {
        when(repository.existsById(1)).thenReturn(true);
        assertTrue(resourceService.existById(1));
    }

    @Test
    void existById_false() {
        when(repository.existsById(2)).thenReturn(false);
        assertFalse(resourceService.existById(2));
    }

    private List<StorageMetadataResponse> prepareStorageMetadata() {
        StorageMetadataResponse stub1 = new StorageMetadataResponse();
        stub1.setId(1L);
        stub1.setStorageType(StorageType.PERMANENT);
        stub1.setBucket("permanent-resource-files");
        stub1.setPath("/permanent-resource-files");
        StorageMetadataResponse stub2 = new StorageMetadataResponse();
        stub2.setId(2L);
        stub2.setStorageType(StorageType.STAGING);
        stub2.setBucket("staging-resource-files");
        stub2.setPath("/staging-resource-files");
        return List.of(stub1, stub2);
    }
}