package com.onlinedrive.staas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URL;
import java.time.Duration;


@Service
@Slf4j
public class
S3Service {

    private S3Presigner presigner;
    private S3Client s3Client;

    @Value("${aws.s3.region:ca-central-1}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presigned.duration.minutes:15}")
    private int presignedDurationMinutes;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        log.info("S3Service initialized - Region: {}, Bucket: {}", region, bucket);
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3Client closed");
        }
        if (presigner != null) {
            presigner.close();
            log.info("S3Presigner closed");
        }
    }

    public URL generatePresignedUploadUrl(String objectKey, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedDurationMinutes))
                .putObjectRequest(putReq)
                .build();

        URL url = presigner.presignPutObject(presignRequest).url();
        log.debug("Generated upload URL for: {} with Content-Type: {}", objectKey, contentType);
        return url;
    }


    public URL generatePresignedDownloadUrl(String objectKey) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedDurationMinutes))
                .getObjectRequest(getReq)
                .build();

        URL url = presigner.presignGetObject(presignRequest).url();
        log.debug("Generated download URL for: {}", objectKey);
        return url;
    }

    public URL generatePresignedDownloadUrl(String objectKey, String displayFileName) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .responseContentDisposition("attachment; filename=\"" + displayFileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedDurationMinutes))
                .getObjectRequest(getReq)
                .build();

        URL url = presigner.presignGetObject(presignRequest).url();
        log.debug("Generated download URL for: {} with display filename: {}", objectKey, displayFileName);
        return url;
    }

    public String generateObjectKey(String username, String folderName, String fileName) {
        long timestamp = System.currentTimeMillis();
        return String.format("uploads/%s/%s/%d-%s", username, folderName, timestamp, fileName);
    }


    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteReq);
            log.debug("Deleted S3 object: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", objectKey, e);
            throw new RuntimeException("Failed to delete S3 object: " + objectKey, e);
        }
    }
}
