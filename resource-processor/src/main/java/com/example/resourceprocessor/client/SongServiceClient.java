package com.example.resourceprocessor.client;

import com.example.resourceprocessor.exception.MetadataClientException;
import com.example.resourceprocessor.model.ErrorResponse;
import com.example.resourceprocessor.model.SongMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SongServiceClient {
    @Value("${song.service.url}")
    private String songServiceUrl;
    private final RestTemplate restTemplate;

    public SongServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public ResponseEntity<String> saveResourceMetadata(SongMetadata songMetadata) {
        String url = songServiceUrl + "/songs";
        return restTemplate.postForEntity(url, songMetadata, String.class);
    }

    @Recover
    public void recover(Exception e, SongMetadata metadata) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode("503");
        errorResponse.setErrorMessage("Metadata Service can not perform database operation for metadata with resource ID: " + metadata.getResourceId() + " after multiple attempts. ");
        throw new MetadataClientException(e.getMessage(), errorResponse);
    }
}