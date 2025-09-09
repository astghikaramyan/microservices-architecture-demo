package com.example.resourceprocessor.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ResourceServiceClient resourceServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Field urlField = ResourceServiceClient.class.getDeclaredField("resourceServiceUrl");
        urlField.setAccessible(true);
        urlField.set(resourceServiceClient, "http://localhost:8080");
    }

    @Test
    void testGetResourceBinary_success() {
        String resourceId = "123";
        byte[] expectedData = "audio-bytes".getBytes();

        when(restTemplate.getForObject("http://localhost:8080/resources/" + resourceId, byte[].class))
                .thenReturn(expectedData);

        byte[] result = resourceServiceClient.getResourceBinary(resourceId);

        assertNotNull(result);
        assertArrayEquals(expectedData, result);

        verify(restTemplate, times(1))
                .getForObject("http://localhost:8080/resources/" + resourceId, byte[].class);
    }

    @Test
    void testGetResourceBinary_failure_recoverThrows() {
        String resourceId = "456";

        when(restTemplate.getForObject("http://localhost:8080/resources/" + resourceId, byte[].class))
                .thenThrow(new RuntimeException("service down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resourceServiceClient.getResourceBinary(resourceId));

        // Directly check the message, no need for ex.getCause()
        assertEquals("service down", ex.getMessage());

        verify(restTemplate, times(1))
                .getForObject("http://localhost:8080/resources/" + resourceId, byte[].class);
    }

    @Test
    void testRecoverMethodThrowsSongClientException() {
        String resourceId = "789";
        Exception original = new Exception("retry failed");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resourceServiceClient.recover(original, resourceId));

        assertEquals("retry failed", ex.getCause().getMessage());
    }
}
