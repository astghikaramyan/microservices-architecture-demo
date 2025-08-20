package com.example.resourceservice.service;

import com.example.resourceservice.dto.SongDTO;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.InvalidDataException;
import com.example.resourceservice.exception.NotFoundException;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ResourceService {
    public static final String BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE = "Invalid value \'%s\' for ID. Must be a positive integer";
    public static final String BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE = "Invalid ID format: \'%s\' for ID. Only positive integers are allowed";
    public static final String BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE = "CSV string is too long: received %s characters, maximum allowed is 200";
    public static final String BAD_REQUEST_RESPONSE_CODE = "400";
    public static final String NOT_FOUND_REQUEST_RESPONSE_CODE = "404";
    @Value("${song.service.url}")
    private String songServiceUrl;
    @Autowired
    private ResourceRepository repository;
    @Autowired
    private RestTemplate restTemplate;

    public ResourceEntity createResource(final byte[] audioData) {
        ResourceEntity resource = new ResourceEntity();
        resource.setData(audioData);
        resource = this.repository.save(resource);
        SongMetadata songMetadata = null;
        try {
            songMetadata = getFileMetadata(audioData);
        } catch (IOException e) {
            throw new InvalidDataException("Invalid file format: . Only MP3 files are allowed");
        } catch (TikaException e) {
            throw new InvalidDataException("Invalid file format: . Only MP3 files are allowed");
        } catch (SAXException e) {
            throw new InvalidDataException("Invalid file format: . Only MP3 files are allowed");
        }
        songMetadata.setResourceId(resource.getId());
        ResponseEntity<String> songMetadataResourceEntity = createSongMetadata(songMetadata);
        if (!songMetadataResourceEntity.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            deleteResource(resource.getId());
            return null;
        }
        return resource;
    }

    public ResourceEntity getResource(final Integer id) {
        if (!isNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }

        if (!isValidNumeric(String.valueOf(id))) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, id), BAD_REQUEST_RESPONSE_CODE));
        }

        if (!this.existById(Integer.valueOf(id))) {
            throw new NotFoundException(prepareErrorResponse(String.format("Resource with ID=%s not found", id), NOT_FOUND_REQUEST_RESPONSE_CODE));
        }
        return this.repository.getById(id);
    }

    public Map<String, List<Integer>> deleteResourceByIds(final String id) {
        if (Objects.nonNull(id) && id.length() > 200) {
            throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_CSV_TOO_LONG_ERROR_MESSAGE, id.length()), BAD_REQUEST_RESPONSE_CODE));
        }
        String[] ids = Optional.ofNullable(id).map(param -> param.split(",")).orElse(new String[]{});
        Arrays.stream(ids).forEach(param -> {
            if (!isNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
            if (!isValidNumeric(param)) {
                throw new InvalidDataException(prepareErrorResponse(String.format(BAD_REQUEST_INCORRECT_NUMBER_ERROR_MESSAGE, param), BAD_REQUEST_RESPONSE_CODE));
            }
        });
        List<Integer> removedIds = new LinkedList<>();
        Arrays.stream(ids).forEach(param -> {
            Integer resourceId = Integer.valueOf(param);
            if (this.existById(resourceId)) {
                this.deleteResource(resourceId);
                removedIds.add(resourceId);
            }
        });
        final Map<String, List<Integer>> responseObject = new HashMap<>();
        responseObject.put("ids", removedIds);
        return responseObject;
    }

    public void deleteResource(final Integer id) {
        ResponseEntity<SongDTO> songDTO = getSongBasedOnResourceId(id);
        if (songDTO.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            deleteSong(songDTO.getBody().getId());
        }
        repository.deleteById(id);
    }


    public boolean existById(final Integer id) {
        return this.repository.existsById(id);
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

    private ResponseEntity<String> createSongMetadata(final SongMetadata songMetadata) {
        String url = songServiceUrl + "/songs";
        return restTemplate.postForEntity(url, songMetadata, String.class);
    }

    private ResponseEntity<SongDTO> getSongBasedOnResourceId(final Integer id) {
        String url = songServiceUrl + "/songs/resource-identifiers/" + id;
        return restTemplate.getForEntity(url, SongDTO.class);
    }

    private void deleteSong(final Integer id) {
        String url = songServiceUrl + "/songs?id=" + id;
        restTemplate.delete(url);
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
}

