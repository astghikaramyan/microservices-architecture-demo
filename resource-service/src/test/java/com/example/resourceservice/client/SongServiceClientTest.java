package com.example.resourceservice.client;

import com.example.resourceservice.dto.SongDTO;
import com.example.resourceservice.exception.SongClientException;
import com.example.resourceservice.model.SongMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SongServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SongServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        java.lang.reflect.Field urlField = SongServiceClient.class.getDeclaredField("songServiceUrl");
        urlField.setAccessible(true);
        urlField.set(client, "http://localhost:8080");
    }

    @Test
    void testSaveResourceMetadata_success() {
        SongMetadata metadata = new SongMetadata();
        metadata.setResourceId(123);

        when(restTemplate.postForEntity(anyString(), eq(metadata), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        ResponseEntity<String> response = client.saveResourceMetadata(metadata);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ok", response.getBody());
        verify(restTemplate).postForEntity("http://localhost:8080/songs", metadata, String.class);
    }

    @Test
    void testSaveResourceMetadata_exceptionTriggersRecover() {
        SongMetadata metadata = new SongMetadata();
        metadata.setResourceId(456);

        when(restTemplate.postForEntity(anyString(), eq(metadata), eq(String.class)))
                .thenThrow(new RuntimeException("downstream error"));

        SongClientException ex = assertThrows(SongClientException.class,
                () -> client.recover(new RuntimeException("downstream error"), metadata));

        assertEquals("503", ex.getErrorResponse().getErrorCode());
        assertTrue(ex.getErrorResponse().getErrorMessage().contains("456"));
    }

    @Test
    void testDeleteResourceMetadataByResourceId_success() {
        SongDTO songDTO = new SongDTO();
        songDTO.setId(999);

        when(restTemplate.getForEntity(anyString(), eq(SongDTO.class)))
                .thenReturn(new ResponseEntity<>(songDTO, HttpStatus.OK));

        doNothing().when(restTemplate).delete(anyString());

        client.deleteResourceMetadataByResourceId(100);

        verify(restTemplate).getForEntity("http://localhost:8080/songs/resource-identifiers/100", SongDTO.class);
        verify(restTemplate).delete("http://localhost:8080/songs?id=999");
    }

    @Test
    void testDeleteResourceMetadataByResourceId_notFound_noDeleteCalled() {
        when(restTemplate.getForEntity(anyString(), eq(SongDTO.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        client.deleteResourceMetadataByResourceId(200);

        verify(restTemplate).getForEntity("http://localhost:8080/songs/resource-identifiers/200", SongDTO.class);
        verify(restTemplate, never()).delete(anyString());
    }

    @Test
    void testDeleteResourceMetadata_exceptionTriggersRecover() {
        RuntimeException cause = new RuntimeException("network failure");

        SongClientException ex = assertThrows(SongClientException.class,
                () -> client.recover(cause, 789));

        assertEquals("503", ex.getErrorResponse().getErrorCode());
        assertTrue(ex.getErrorResponse().getErrorMessage().contains("789"));
    }
}
