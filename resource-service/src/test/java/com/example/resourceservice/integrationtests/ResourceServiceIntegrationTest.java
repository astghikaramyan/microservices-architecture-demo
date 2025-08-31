package com.example.resourceservice.integrationtests;

import com.example.resourceservice.ResourceServiceApplication;
import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.controller.ResourceRestController;
import com.example.resourceservice.entity.ResourceEntity;
import com.example.resourceservice.exception.StorageException;
import com.example.resourceservice.integrationtests.config.*;
import com.example.resourceservice.repository.ResourceRepository;
import com.example.resourceservice.service.ResourceService;
import com.example.resourceservice.service.StorageService;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
        LocalStackS3TestConfig.class,
        RabbitMQTestConfig.class,
        SongServiceWireMockConfig.class,
        PostgresTestConfig.class,
        S3BucketInitializer.class,
        DisableEurekaTestConfig.class,
        ResourceServiceApplication.class,
        TestRestTemplateConfig.class
})
@ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class})
@Testcontainers
@AutoConfigureMockMvc
class ResourceServiceIntegrationTest {

    @Autowired
    private ResourceRestController resourceRestController;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private SongServiceClient songServiceClient;

    @Autowired
    private S3Client s3Client;
    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        PostgresTestConfig.registerProperties(registry);
        LocalStackS3TestConfig.registerLocalstackProperties(registry);
        SongServiceWireMockConfig.configureWireMockProperties(registry);
        RabbitMQTestConfig.rabbitProperties(registry);
        DisableEurekaTestConfig.disableEureka(registry);
    }

    @BeforeEach
    void setup() {
        resourceRepository.deleteAll();
        int port = SongServiceWireMockConfig.getPort();
        WireMock.configureFor(SongServiceWireMockConfig.getHost(), port);
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/songs/resource-identifiers/1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"title\":\"Stub Song\"}")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/songs/resource-identifiers/2"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":2,\"title\":\"Stub Song\"}")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/songs/resource-identifiers/3"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":3,\"title\":\"Stub Song\"}")));

        // Stub POST /songs
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/songs"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"status\":\"success\"}")));

        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/songs"))
                .withQueryParam("id", equalTo("1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"status\":\"deleted\"}")));
        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/songs"))
                .withQueryParam("id", equalTo("2"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"status\":\"deleted\"}")));
        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/songs"))
                .withQueryParam("id", equalTo("3"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"status\":\"deleted\"}")));
    }

    @AfterAll
    void cleanup() {
        try {
            deleteBucket(s3Client, "test-bucket");
        } catch (Exception ignored) {
            // ignore if already deleted
        }
    }
    public void deleteBucket(S3Client s3Client, String bucketName) {
        // 1. List all objects
        ListObjectsV2Response listObjects = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucketName).build()
        );

        // 2. Delete all objects
        for (S3Object s3Object : listObjects.contents()) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Object.key())
                    .build());
        }

        // 3. Delete bucket
        s3Client.deleteBucket(DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build());
    }


    // -----------------------------
    // POST /resources -> uses SongServiceWireMockConfig POST stub
    // -----------------------------
    @Test
    void testUploadResource_savesResourceInResourcesTableAndFileBytesToS3() throws Exception {
        byte[] audioData = "dummy audio data".getBytes();

        // Write file to S3 (LocalStack)
        storageService.addFileBytesToStorage("test-key", audioData);

        mockMvc.perform(post("/resources")
                        .content(audioData)
                        .contentType("audio/mpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        List<ResourceEntity> resources = resourceRepository.findAll();
        assertThat(storageService.retrieveFileFromStorage("test-key").asByteArray()).isEqualTo(audioData);
        assertThat(resources).hasSize(1);
    }

    // -----------------------------
    // GET /resources/{id} -> file from S3
    // -----------------------------
    @Test
    void testGetResource_retrieveSavedResourceFromDBAndFileBytesFromS3() throws Exception {
        // Arrange: create and save a resource entity
        ResourceEntity entity = new ResourceEntity();
        entity.setFileName("dummy audio");
        entity.setS3Key("s3Key");
        entity.setUploadedAt(LocalDateTime.now());
        resourceRepository.save(entity);

        // Arrange: upload dummy file to S3
        byte[] audioData = "dummy bytes".getBytes(StandardCharsets.UTF_8);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket("test-bucket")
                        .key(entity.getS3Key())
                        .build(),
                RequestBody.fromBytes(audioData)
        );

        // Act & Assert: perform GET request and verify response
        mockMvc.perform(get("/resources/{id}", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(audioData)) // verify returned bytes
                .andExpect(content().contentType("audio/mpeg"));

        // Additional verification: entity still exists in repository
        ResourceEntity found = resourceRepository.findById(entity.getId())
                .orElseThrow(() -> new AssertionError("Resource not found"));
        assertThat(found.getFileName()).isEqualTo("dummy audio");
        assertThat(found.getS3Key()).isEqualTo("s3Key");
        assertThat(storageService.retrieveFileFromStorage("s3Key").asByteArray()).isEqualTo(audioData);
    }

    // -----------------------------
    // DELETE /resources -> uses SongServiceWireMockConfig DELETE stub
    // -----------------------------
    @Test
    void testDeleteResource_deleteSavedResourceFromDBAndFileBytesFromS3() throws Exception {
        ResourceEntity entity = new ResourceEntity();
        entity.setFileName("audio1");
        entity.setS3Key("s3Key");
        entity.setUploadedAt(LocalDateTime.now());
        resourceRepository.save(entity);

        // Upload file to S3
        byte[] audioData = "dummy bytes".getBytes();
        s3Client.putObject(
                PutObjectRequest.builder().bucket("test-bucket").key("s3Key").build(),
                RequestBody.fromBytes(audioData)
        );

        mockMvc.perform(delete("/resources")
                        .param("id", entity.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());

        assertThat(resourceRepository.findById(entity.getId())).isEmpty();
        assertThrows(
                StorageException.class,
                () -> storageService.retrieveFileFromStorage("s3Key")
        );
    }

    // -----------------------------
    // Test SongServiceWireMock GET stub directly
    // -----------------------------
    @Test
    void testGetSongMetadata() {
        var response = songServiceClient.saveResourceMetadata(null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    // -----------------------------
    // Test upload and retrieve from S3
    // -----------------------------
    @Test
    void testS3UploadAndRetrieve() {
        byte[] content = "hello-test".getBytes();
        String key = "file1.mp3";

        s3Client.putObject(
                b -> b.bucket("test-bucket").key(key),
                RequestBody.fromBytes(content)
        );

        var result = s3Client.getObjectAsBytes(b -> b.bucket("test-bucket").key(key));
        assertThat(result.asUtf8String()).isEqualTo("hello-test");
    }
}
