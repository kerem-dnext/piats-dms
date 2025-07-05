package com.piats.dms.repository;

import com.piats.dms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A Spring Data JPA repository for {@link Document} entities.
 * <p>
 * This interface provides standard CRUD operations and custom query methods
 * for interacting with the document metadata stored in the database.
 * </p>
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    /**
     * Finds all documents associated with a specific application ID.
     *
     * @param applicationId The UUID of the application.
     * @return A list of {@link Document} entities.
     */
    List<Document> findByApplicationId(UUID applicationId);

    /**
     * Finds a document by its unique S3 key.
     * <p>
     * This is useful for ensuring that no duplicate file references are created
     * and for certain cleanup operations where only the S3 key is known.
     * </p>
     *
     * @param s3Key The unique key of the object in the S3 bucket.
     * @return An {@link Optional} containing the found {@link Document}, or empty if not found.
     */
    Optional<Document> findByS3Key(String s3Key);
} 