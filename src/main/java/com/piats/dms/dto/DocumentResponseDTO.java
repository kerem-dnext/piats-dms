package com.piats.dms.dto;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for representing document metadata.
 * <p>
 * This DTO is used to send detailed information about a document to clients,
 * excluding sensitive details like its physical storage location.
 * </p>
 */
@Data
@Builder
public class DocumentResponseDTO {
    /** The unique identifier of the document. */
    private UUID id;
    /** The identifier of the application this document is associated with. */
    private UUID applicationId;
    /** The original filename of the document as it was uploaded. */
    private String originalFilename;
    /** The MIME type of the document file. */
    private String fileType;
    /** The size of the document file in bytes. */
    private Long fileSize;
    /** The timestamp when the document was first created. */
    private Instant createdAt;
    /** The timestamp when the document was last updated. */
    private Instant updatedAt;
} 