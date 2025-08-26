package com.example.resourceprocessor.service;

import com.example.resourceprocessor.dto.SongDTO;
import com.example.resourceprocessor.exception.InvalidDataException;
import com.example.resourceprocessor.model.SongMetadata;
import com.example.resourceprocessor.s3config.S3Config;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ResourceProcessorService {

    @Value("${song.service.url}")
    private String songServiceUrl;
    @Value("${s3.bucket}")
    private String BUCKET_NAME = "resource-files";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private S3Config s3Config;

    public SongMetadata retrieveFileMetadata(byte[] fileBytes) {
        try {
            return getFileMetadata(fileBytes);
        } catch (IOException | TikaException | SAXException e) {
            throw new InvalidDataException("Invalid file format: . Only MP3 files are allowed");
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
}

