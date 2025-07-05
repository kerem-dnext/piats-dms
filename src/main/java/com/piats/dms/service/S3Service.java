package com.piats.dms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.Duration;

/**
 * A service for interacting with Amazon S3.
 * <p>
 * This class abstracts the low-level details of the AWS S3 SDK and provides
 * simple methods for uploading, deleting, and generating presigned URLs for files.
 * It is configured with the bucket name from application properties.
 * </p>
 */
@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Constructs an {@code S3Service} with the necessary S3 clients.
     *
     * @param s3Client    The client for direct S3 operations.
     * @param s3Presigner The client for generating presigned URLs.
     */
    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Uploads a file to the S3 bucket.
     *
     * @param key           The unique key (path) to store the file under in S3.
     * @param contentLength The length of the file content in bytes.
     * @param inputStream   The input stream of the file content.
     * @param contentType   The MIME type of the file.
     * @throws RuntimeException if the upload fails.
     */
    public void uploadFile(String key, long contentLength, InputStream inputStream, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Successfully uploaded file to S3 with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to upload file to S3 with key: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Generates a temporary, presigned URL for downloading a file from S3.
     *
     * @param key      The S3 key of the file.
     * @param duration The duration for which the URL should be valid.
     * @return A string representation of the presigned URL.
     * @throws RuntimeException if URL generation fails.
     */
    public String generatePresignedUrl(String key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            log.info("Generated presigned URL for key: {}", key);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Deletes a file from the S3 bucket.
     *
     * @param key The S3 key of the file to delete.
     * @throws RuntimeException if the deletion fails.
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted file from S3 with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3 with key: {}", key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Returns the name of the S3 bucket used by this service.
     *
     * @return The S3 bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }
} 