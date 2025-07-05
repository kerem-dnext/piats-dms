package com.piats.dms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents the metadata for a document stored in the system.
 * <p>
 * This JPA entity maps to the {@code documents} table in the database and holds
 * information about a file, including its link to an application, its storage
 * location in S3, and standard metadata like filename, size, and type.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {
    /**
     * The unique identifier for the document metadata record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The identifier of the application this document is associated with.
     * This can be used to link documents to other parts of the system.
     */
    private UUID applicationId;

    /**
     * The name of the S3 bucket where the file is stored.
     */
    @Column(nullable = false)
    private String s3Bucket;

    /**
     * The key (path) of the file within the S3 bucket. This should be unique
     * to prevent object overwrites. The path is structured to include the
     * application ID for better organization.
     */
    @Column(nullable = false, unique = true, length = 1024)
    private String s3Key;

    /**
     * The original name of the file as uploaded by the user.
     */
    @Column(nullable = false)
    private String originalFilename;

    /**
     * The MIME type of the file (e.g., "application/pdf").
     */
    @Column(nullable = false)
    private String fileType;

    /**
     * The size of the file in bytes.
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * The timestamp when the document record was created.
     * This is automatically managed by Hibernate.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * The timestamp when the document record was last updated.
     * This is automatically managed by Hibernate.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
} 