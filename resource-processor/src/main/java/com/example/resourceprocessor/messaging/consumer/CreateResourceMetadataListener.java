package com.example.resourceprocessor.messaging.consumer;

import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.exception.InvalidDataException;
import com.example.resourceprocessor.messaging.publisher.ProcessSongMetadataPublisher;
import com.example.resourceprocessor.model.SongMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class CreateResourceMetadataListener {
    private static final Logger LOGGER = LogManager.getLogger(CreateResourceMetadataListener.class);
    public static final String PROCESS_SONG_METADATA_OUT = "processSongMetadata-out-0";
    private final ResourceServiceClient resourceClient;
    private final SongServiceClient songClient;
    private final ProcessSongMetadataPublisher processSongMetadataPublisher;
    public CreateResourceMetadataListener(ResourceServiceClient resourceClient, SongServiceClient songClient,
                                         ProcessSongMetadataPublisher processSongMetadataPublisher) {
        this.resourceClient = resourceClient;
        this.songClient = songClient;
        this.processSongMetadataPublisher = processSongMetadataPublisher;
    }

    @Bean
    public Consumer<Message<String>> createResourceMetadata() {
        return message -> {
            try {
                // Extract traceId from headers
                String traceId = null;
                if (message.getHeaders().containsKey("X-Trace-Id")) {
                    traceId = message.getHeaders().get("X-Trace-Id").toString();
                }
                if (traceId == null || traceId.isEmpty()) {
                    traceId = UUID.randomUUID().toString();
                }
                // Put traceId in MDC (ThreadContext) for logging
                ThreadContext.put("traceId", traceId);

                String resourceId = message.getPayload();
                byte[] resourceData = resourceClient.getResourceBinary(resourceId);
                SongMetadata metadata = retrieveFileMetadata(resourceData);
                metadata.setResourceId(Integer.valueOf(resourceId));
                songClient.saveResourceMetadata(metadata);
                processSongMetadataPublisher.sendProcessedSongMetadataEvent(PROCESS_SONG_METADATA_OUT,
                    MessageBuilder.withPayload(resourceId).build());
                LOGGER.info("Received message with resource ID={}", resourceId);
            } catch (Exception e) {
                throw new RuntimeException(e); // triggers retry & DLQ
            } finally {
                ThreadContext.clearAll();
            }
        };
    }

    private SongMetadata retrieveFileMetadata(byte[] fileBytes) {
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
}
