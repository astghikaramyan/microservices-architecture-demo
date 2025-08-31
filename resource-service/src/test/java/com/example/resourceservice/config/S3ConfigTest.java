package com.example.resourceservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class S3ConfigTest {

    private S3Config s3Config;

    @BeforeEach
    void setUp() throws Exception {
        s3Config = new S3Config();

        injectPrivateField("endpoint", "http://localhost:4566");
        injectPrivateField("accessKey", "test-access-key");
        injectPrivateField("secretKey", "test-secret-key");
        injectPrivateField("region", "us-east-1");
    }

    private void injectPrivateField(String fieldName, String value) throws Exception {
        Field field = S3Config.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(s3Config, value);
    }

    @Test
    void testCreateS3Client_notNull() {
        S3Client client = s3Config.createS3Client();
        assertNotNull(client);
    }

    @Test
    void testCreateS3Client_endpointAndRegionConfigured() {
        S3Client client = s3Config.createS3Client();

        // Verify endpoint override
        assertEquals(URI.create("http://localhost:4566"), client.serviceClientConfiguration().endpointOverride().get());

        // Verify region
        assertEquals("us-east-1", client.serviceClientConfiguration().region().id());
    }
}
