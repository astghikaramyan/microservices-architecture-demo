package com.example.resourceservice.cucumber.hooks;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.integrationtests.config.S3BucketInitializer;
import com.example.resourceservice.service.StorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class MockExternalServicesConfig {

    @Bean
    @Primary
    public StorageService storageService() {
        StorageService mockStorage = mock(StorageService.class);

        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength(5L)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes =
                ResponseBytes.fromByteArray(response, "hello".getBytes(StandardCharsets.UTF_8));

        when(mockStorage.retrieveFileFromStorage(anyString())).thenReturn(objectBytes);

        return mockStorage;
    }

    @Bean
    @Primary
    public SongServiceClient songServiceClient() {
        SongServiceClient mockSongClient = mock(SongServiceClient.class);
        when(mockSongClient.saveResourceMetadata(any()))
                .thenReturn(ResponseEntity.ok("Success"));
        return mockSongClient;
    }

    @Bean
    @Primary
    public StreamBridge streamBridge() {
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        return streamBridge;
    }
    @Bean
    @Primary
    public S3BucketInitializer s3BucketInitializer() {
        // return a no-op mock so PostConstruct never calls AWS
        return org.mockito.Mockito.mock(S3BucketInitializer.class);
    }
}
