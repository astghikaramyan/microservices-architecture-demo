package com.example.resourceservice.integrationtests.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3BucketInitializer {

    private final S3Client s3Client;

    public S3BucketInitializer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void initBucket() {
        String bucketName = "staging-resource-files";
        if (!bucketExists(bucketName)) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            return true; // bucket exists
        } catch (NoSuchBucketException e) {
            return false; // does not exist
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e; // some other error
        }
    }
}
