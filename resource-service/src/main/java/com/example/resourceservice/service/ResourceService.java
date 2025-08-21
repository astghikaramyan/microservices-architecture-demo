package com.example.resourceservice.service;

import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.DatabaseException;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.exception.NotFoundException;
import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.model.ErrorResponse;
import com.example.resourceservice.model.SongMetadata;
import com.example.resourceservice.repository.ResourceRepository;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResourceService {
    private static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
    public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
    private static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
    public static final String BAD_REQUEST_RESPONSE_CODE = "400";
    private static final String NOT_FOUND_REQUEST_RESPONSE_CODE = "404";
    private static final String STORAGE_ERROR_MESSAGE = "Failed to upload file to S3";
    public static final String INTERNAL_SERVER_ERROR_RESPONSE_CODE = "500";
    public static final String SERVICE_UNAVAILABLE_RESPONSE_CODE = "503";
    private static final String DATABASE_ERROR_MESSAGE = "Failed to save resource identifier";
    private static final String NOT_FOUNT_RESOURCE_ERROR_MESSAGE = "Resource with ID=%s not found";

    @Value("${s3.bucket}")
    private String BUCKET_NAME = "resource-files";
    @Autowired
    private ResourceRepository repository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private S3Client s3Client;

    public ResourceEntity uploadResource(byte[] fileBytes) {
        SongMetadata songMetadata = retrieveFileMetadata(fileBytes);
        String fileName = songMetadata.getName();
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String s3Key = "resources/" + UUID.randomUUID() + "-" + safeFileName;
        try {
            s3Client.putObject(preparePutRequestData(s3Key), RequestBody.fromBytes(fileBytes));
        } catch (Exception e) {
            throw new StorageException(prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
        try {
            return this.repository.save(prepareResource(s3Key, fileName));
        } catch (Exception e) {
            s3Client.deleteObject(prepareDeleteRequestData(s3Key));
            throw new DatabaseException(prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
    }

    public byte[] getFileAsBytes(final Integer id) {
        validateResourceId(id);
        ResourceEntity resource;
        try {
            resource = this.repository.getById(id);
        } catch (Exception e) {
            throw new DatabaseException(prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
        if (Objects.isNull(resource) || Objects.isNull(resource.getS3Key())) {
            throw new NotFoundException(prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(prepareGetRequestData(resource.getS3Key()));
            if (Objects.isNull(objectBytes)) {
                throw new NotFoundException(prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
            }
            return objectBytes.asByteArray();
        } catch (Exception e) {
            throw new StorageException(prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    public Map<String, List<Integer>> deleteResourceByIds(final String id) {
        validateResourceIds(id);
        String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
        List<Integer> removedIds = new LinkedList<>();
        for (String param : ids) {
            Integer resourceId = Integer.valueOf(param);
            Optional<ResourceEntity> resourceOpt = repository.findById(resourceId);
            if (resourceOpt.isPresent()) {
                ResourceEntity resource = resourceOpt.get();
                try {
                    this.deleteResourceFromStorage(resource.getS3Key());
                    this.deleteResource(resourceId);
                    removedIds.add(resourceId);
                } catch (StorageException e) {
                    throw new StorageException(prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
                } catch (DatabaseException e) {
                    throw new DatabaseException(prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
                }
            }
        }
        final Map<String, List<Integer>> responseObject = new HashMap<>();
        responseObject.put("ids", removedIds);
        return responseObject;
    }

    @Retryable(
            value = StorageException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteResourceFromStorage(String s3Key) {
        try {
            s3Client.deleteObject(prepareDeleteRequestData(s3Key));
        } catch (Exception e) {
            throw new StorageException(prepareErrorResponse(STORAGE_ERROR_MESSAGE, SERVICE_UNAVAILABLE_RESPONSE_CODE));
        }
    }

    @Retryable(
            value = DatabaseException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteResource(Integer resourceId) {
        try {
            repository.deleteById(resourceId);
        } catch (Exception e) {
            throw new DatabaseException(prepareErrorResponse(DATABASE_ERROR_MESSAGE, INTERNAL_SERVER_ERROR_RESPONSE_CODE));
        }
    }

    public boolean existById(final Integer id) {
        return this.repository.existsById(id);
    }

    private void validateResourceIds(String id) {
        if (Objects.nonNull(id) && id.length() > 200) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE, id.length()), BAD_REQUEST_RESPONSE_CODE));
        }
        String[] ids = (id != null && !id.isBlank()) ? id.split(",") : new String[]{};
        for (String param : ids) {
            if (!isNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
            if (!isValidNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
        }
    }

    private SongMetadata getFileMetadata(final byte[] audioData) throws IOException, TikaException, SAXException {
        SongMetadata songMetadata = new SongMetadata();

        try (InputStream inputStream = new ByteArrayInputStream(audioData)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            Mp3Parser mp3Parser = new Mp3Parser();
            ParseContext parseContext = new ParseContext();

            mp3Parser.parse(inputStream, handler, metadata, parseContext);

            songMetadata.setName(resolveEmptyField(metadata.get("dc:title")));
            songMetadata.setArtist(resolveEmptyField(metadata.get("xmpDM:artist")));
            songMetadata.setAlbum(resolveEmptyField(metadata.get("xmpDM:album")));
            songMetadata.setDuration(resolveEmptyLength(formatDuration(metadata.get("xmpDM:duration"))));
            songMetadata.setYear(resolveEmptyYear(metadata.get("xmpDM:releaseDate")));
        }

        return songMetadata;
    }

    private String formatDuration(String durationMillis) {
        if (durationMillis == null) {
            return null;
        }
        try {
            double durationInSeconds = Double.parseDouble(durationMillis) / 1000;
            int minutes = (int) (durationInSeconds / 60);
            int seconds = (int) (durationInSeconds % 60);
            return String.format("%02d:%02d", minutes, seconds);
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }

    private String resolveEmptyField(final String value) {
        return Optional.ofNullable(value).orElse("Unknown");
    }

    private String resolveEmptyLength(final String value) {
        if (Objects.nonNull(value) && value.contains(":")) {
            String[] parts = value.split(":");
            if (parts.length == 3) {
                return resolveDurationParts(parts[1]) + ":" + resolveDurationParts(parts[2]);
            } else if (parts.length == 2) {
                return resolveDurationParts(parts[0]) + ":" + resolveDurationParts(parts[1]);
            } else if (parts.length == 1) {
                return "00:" + resolveDurationParts(parts[0]);
            }
        }
        return "00:22";
    }

    private String resolveDurationParts(final String part1) {
        if (part1.length() == 2) {
            return part1;
        } else if (part1.length() == 1) {
            return "0" + part1;
        } else {
            return "00";
        }
    }

    private String resolveEmptyYear(final String value) {
        final boolean isCorrectYear = Optional.ofNullable(value)
                                              .filter(v -> v.length() == 4)
                                              .map(s -> s.chars().allMatch(Character::isDigit))
                                              .orElse(false);
        return isCorrectYear ? value : "1987";
    }

    private boolean isValidNumeric(String id) {
        final boolean isWholeNumber = Optional.ofNullable(id)
                                              .map(s -> s.chars().allMatch(Character::isDigit))
                                              .orElse(false);
        return isWholeNumber && Integer.parseInt(id) > 0;
    }

    private boolean isNumeric(final String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public ErrorResponse prepareErrorResponse(final String message, final String code) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(message);
        errorResponse.setErrorCode(code);
        return errorResponse;
    }

    private GetObjectRequest prepareGetRequestData(String s3Key) {
        return GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }

    private PutObjectRequest preparePutRequestData(String s3Key) {
        return PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }

    private DeleteObjectRequest prepareDeleteRequestData(String s3Key) {
        return DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();
    }

    private SongMetadata retrieveFileMetadata(byte[] fileBytes) {
        try {
            return getFileMetadata(fileBytes);
        } catch (IOException | TikaException | SAXException e) {
            throw new InvalidDataException("Invalid file format: . Only MP3 files are allowed");
        }
    }

    private void validateResourceId(Integer id) {
        if (!isNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!isValidNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }
        if (!this.existById(Integer.valueOf(id))) {
            throw new NotFoundException(prepareErrorResponse(String.format(NOT_FOUNT_RESOURCE_ERROR_MESSAGE, id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
    }

    private ResourceEntity prepareResource(String s3Key, String fileName) {
        ResourceEntity resource = new ResourceEntity();
        resource.setS3Key(s3Key);
        resource.setFileName(fileName);
        resource.setUploadedAt(LocalDateTime.now());
        return resource;
    }
}

