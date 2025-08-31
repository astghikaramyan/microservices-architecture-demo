package com.example.resourceservice.client;

import com.example.resourceservice.dto.SongDTO;
import com.example.resourceservice.exception.SongClientException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.model.SongMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
        throw new SongClientException(e.getMessage(), errorResponse);
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void deleteResourceMetadataByResourceId(Integer resourceId) {
        ResponseEntity<SongDTO> songDTO = getMetadataByResourceId(resourceId);
        if (songDTO.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            deleteResourceMetadata(songDTO.getBody().getId());
        }
    }

    @Recover
    public void recover(Exception e, Integer resourceId) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode("503");
        errorResponse.setErrorMessage("Metadata Service can not perform removal of metadata for resource with ID: " + resourceId + " after multiple attempts. ");
        throw new SongClientException(e.getMessage(), errorResponse);
    }

    private ResponseEntity<SongDTO> getMetadataByResourceId(final Integer id) {
        String url = songServiceUrl + "/songs/resource-identifiers/" + id;
        return restTemplate.getForEntity(url, SongDTO.class);
    }

    private void deleteResourceMetadata(final Integer id) {
        String url = songServiceUrl + "/songs?id=" + id;
        restTemplate.delete(url);
    }
}