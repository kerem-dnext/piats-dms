package com.piats.dms.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for responses after a successful file upload.
 * <p>
 * This DTO confirms a successful upload by providing the new document's ID,
 * a confirmation message, and a temporary URL for immediate access.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponseDTO {
    /** The unique identifier assigned to the newly uploaded document. */
    private UUID documentId;
    /** A confirmation message indicating the result of the upload. */
    private String message;
    /** A temporary, presigned URL to download the uploaded file. */
    private String downloadUrl;
} 