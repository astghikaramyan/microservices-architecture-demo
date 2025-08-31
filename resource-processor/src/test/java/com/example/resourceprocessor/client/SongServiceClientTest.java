package com.example.resourceprocessor.client;

import com.example.resourceprocessor.model.SongMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SongServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SongServiceClient songServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Field urlField = SongServiceClient.class.getDeclaredField("songServiceUrl");
        urlField.setAccessible(true);
        urlField.set(songServiceClient, "http://localhost:8080");
    }

    @Test
    void testSaveResourceMetadata_success() {
        SongMetadata metadata = new SongMetadata();
        metadata.setResourceId(123);

        ResponseEntity<String> mockResponse = ResponseEntity.ok("success");

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/songs"),
                eq(metadata),
                eq(String.class)))
                .thenReturn(mockResponse);

        ResponseEntity<String> response = songServiceClient.saveResourceMetadata(metadata);

        assertNotNull(response);
        assertEquals("success", response.getBody());

        verify(restTemplate, times(1))
                .postForEntity("http://localhost:8080/songs", metadata, String.class);
    }

    @Test
    void testSaveResourceMetadata_failureThrowsException() {
        SongMetadata metadata = new SongMetadata();
        metadata.setResourceId(456);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("service unavailable"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> songServiceClient.saveResourceMetadata(metadata));

        assertEquals("service unavailable", ex.getMessage());

        verify(restTemplate, times(1))
                .postForEntity("http://localhost:8080/songs", metadata, String.class);
    }

}
