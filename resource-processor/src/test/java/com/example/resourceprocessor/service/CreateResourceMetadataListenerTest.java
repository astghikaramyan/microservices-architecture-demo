package com.example.resourceprocessor.service;

import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.messaging.consumer.CreateResourceMetadataListener;
import com.example.resourceprocessor.messaging.publisher.ProcessSongMetadataPublisher;
import com.example.resourceprocessor.model.SongMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateResourceMetadataListenerTest {

    @Mock
    private ResourceServiceClient resourceClient;

    @Mock
    private SongServiceClient songClient;
    @Mock
    private ProcessSongMetadataPublisher processSongMetadataPublisher;


    @InjectMocks
    private CreateResourceMetadataListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateResourceMetadata_success() {
        String resourceId = "123";
        byte[] fakeMp3Bytes = "fake-mp3".getBytes(StandardCharsets.UTF_8);

        when(resourceClient.getResourceBinary(resourceId)).thenReturn(fakeMp3Bytes);
        when(songClient.saveResourceMetadata(any(SongMetadata.class)))
                .thenReturn(ResponseEntity.ok("saved"));

        Consumer<Message<String>> consumer = listener.createResourceMetadata();
        consumer.accept(MessageBuilder.withPayload(resourceId).build());

        verify(resourceClient, times(1)).getResourceBinary(resourceId);
        verify(songClient, times(1)).saveResourceMetadata(any(SongMetadata.class));
    }

    @Test
    void testCreateResourceMetadata_failure_wrapsInRuntimeException() {
        String resourceId = "456";

        when(resourceClient.getResourceBinary(resourceId))
                .thenThrow(new RuntimeException("Service unavailable"));

        Consumer<Message<String>> consumer = listener.createResourceMetadata();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.accept(MessageBuilder.withPayload(resourceId).build()));

        assertEquals("Service unavailable", ex.getCause().getMessage());
    }

    @Test
    void testRetrieveFileMetadata_throwsRuntimeException() {
        String resourceId = "789";
        byte[] invalidData = "not-an-mp3".getBytes(StandardCharsets.UTF_8);

        when(resourceClient.getResourceBinary(resourceId)).thenThrow(RuntimeException.class);

        Consumer<Message<String>> consumer = listener.createResourceMetadata();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.accept(MessageBuilder.withPayload(resourceId).build()));

        assertTrue(ex.getCause() instanceof RuntimeException);
    }

    @Test
    void testFormatDuration_validMillis() {
        String formatted = invokeFormatDuration("120000"); // 2 minutes
        assertEquals("02:00", formatted);
    }

    @Test
    void testFormatDuration_invalidMillis() {
        String formatted = invokeFormatDuration("not-a-number");
        assertEquals("Unknown", formatted);
    }

    @Test
    void testResolveEmptyField_nullReturnsUnknown() {
        assertEquals("Unknown", invokeResolveEmptyField(null));
        assertEquals("Hello", invokeResolveEmptyField("Hello"));
    }

    @Test
    void testResolveEmptyLength_variousFormats() {
        assertEquals("01:23", invokeResolveEmptyLength("01:23"));
        assertEquals("00:22", invokeResolveEmptyLength(null)); // fallback default
        assertEquals("00:22", invokeResolveEmptyLength("5"));
        assertEquals("00:22", invokeResolveEmptyLength("invalid"));
    }

    @Test
    void testResolveEmptyYear_validYear() {
        assertEquals("1999", invokeResolveEmptyYear("1999"));
        assertEquals("1987", invokeResolveEmptyYear("abcd")); // fallback default
    }

    // ---- Reflection helpers ----
    private String invokeFormatDuration(String input) {
        try {
            var method = CreateResourceMetadataListener.class.getDeclaredMethod("formatDuration", String.class);
            method.setAccessible(true);
            return (String) method.invoke(listener, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeResolveEmptyField(String input) {
        try {
            var method = CreateResourceMetadataListener.class.getDeclaredMethod("resolveEmptyField", String.class);
            method.setAccessible(true);
            return (String) method.invoke(listener, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeResolveEmptyLength(String input) {
        try {
            var method = CreateResourceMetadataListener.class.getDeclaredMethod("resolveEmptyLength", String.class);
            method.setAccessible(true);
            return (String) method.invoke(listener, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeResolveEmptyYear(String input) {
        try {
            var method = CreateResourceMetadataListener.class.getDeclaredMethod("resolveEmptyYear", String.class);
            method.setAccessible(true);
            return (String) method.invoke(listener, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
