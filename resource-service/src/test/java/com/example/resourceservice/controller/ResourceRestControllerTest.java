package com.example.resourceservice.controller;

import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.DatabaseException;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.service.ResourceService;
import com.example.resourceservice.util.DataPreparerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ResourceRestControllerTest {

    @Mock
    private ResourceService resourceService;

    @Mock
    private DataPreparerService dataPreparerService;

    @InjectMocks
    private ResourceRestController controller;

    @Test
    void uploadResource_success() {
        byte[] audioData = new byte[]{1, 2, 3};
        ResourceEntity entity = new ResourceEntity();
        entity.setId(42);

        when(resourceService.uploadResource(audioData, null)).thenReturn(entity);

        ResponseEntity<Map<String, Integer>> response = controller.uploadResource(audioData, "test");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(42, response.getBody().get("id"));
    }

    @Test
    void uploadResource_invalidDataException() {
        byte[] audioData = new byte[]{1, 2, 3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(resourceService.uploadResource(audioData, null)).thenThrow(DatabaseException.class);

        assertThrows(DatabaseException.class, () -> controller.uploadResource(audioData, "test"));
    }

    @Test
    void uploadResource_nullEntity() {
        byte[] audioData = new byte[]{1, 2, 3};
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("error");
        when(resourceService.uploadResource(audioData, null)).thenReturn(null);
        assertThrows(InvalidDataException.class, () -> controller.uploadResource(audioData, "test"));
    }

    @Test
    void getResource_success() {
        byte[] expected = new byte[]{10, 20, 30};
        when(resourceService.getFileAsBytes(5)).thenReturn(expected);

        ResponseEntity<byte[]> response = controller.getResource(5);

        assertEquals(200, response.getStatusCodeValue());
        assertArrayEquals(expected, response.getBody());
    }

    @Test
    void deleteResource_success() {
        Map<String, List<Integer>> result = new HashMap<>();
        result.put("ids", Arrays.asList(1, 2));
        when(resourceService.deleteResourceByIds("1,2")).thenReturn(result);

        ResponseEntity<Map<String, List<Integer>>> response = controller.deleteResource("1,2");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Arrays.asList(1, 2), response.getBody().get("ids"));
    }
}