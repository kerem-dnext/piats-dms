package com.piats.dms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configures and provides beans for interacting with Amazon S3.
 * <p>
 * This class sets up the necessary S3 clients using credentials and region
 * information retrieved from the application's configuration properties.
 * </p>
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;
    
    @Value("${aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    /**
     * Creates and configures an {@link S3Client} bean.
     * <p>
     * The S3 client is used for direct interactions with the S3 service,
     * such as uploading and deleting objects.
     * </p>
     *
     * @return A configured {@code S3Client} instance.
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * Creates and configures an {@link S3Presigner} bean.
     * <p>
     * The S3 presigner is used to generate temporary, presigned URLs for
     * accessing S3 objects securely.
     * </p>
     *
     * @return A configured {@code S3Presigner} instance.
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
} 