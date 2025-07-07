package com.piats.dms.controller;

import com.piats.dms.dto.DocumentResponseDTO;
import com.piats.dms.dto.UploadResponseDTO;
import com.piats.dms.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing documents.
 * <p>
 * This controller provides endpoints for uploading, downloading, retrieving,
 * updating, and deleting documents associated with job applications.
 * All endpoints are under the {@code /api/v1} path.
 * </p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Document Management", description = "APIs for managing document uploads for applications")
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Constructs a {@code DocumentController} with the required {@link DocumentService}.
     *
     * @param documentService The service for handling document-related business logic.
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Handles the file upload for a specific application.
     * <p>
     * This method contains basic exception handling for the web layer. More specific
     * validation and business logic exceptions are handled by the {@link DocumentService}
     * and translated into appropriate HTTP responses by the {@link com.piats.dms.exception.GlobalExceptionHandler}.
     * </p>
     *
     * @param file          The multipart file to upload.
     * @param applicationId The UUID of the application to associate the document with.
     * @return A {@link ResponseEntity} containing the upload response DTO.
     */
    @Operation(summary = "Upload a document for an application", description = "Upload a CV or other document and associate it with a job application ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
        @ApiResponse(responseCode = "413", description = "File too large"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/applications/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadForApplication(
            @Parameter(description = "The file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "The ID of the job application", required = true)
            @PathVariable UUID applicationId) {
        
        try {
            log.info("Received upload request for application: {}, filename: {}", 
                    applicationId, file.getOriginalFilename());
            
            UploadResponseDTO response = documentService.uploadDocument(file, applicationId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during file upload for application: {}", applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates a presigned download URL for a document.
     *
     * @param applicationId The UUID of the application.
     * @return A {@link ResponseEntity} containing the temporary download URL as a string.
     */
    @Operation(summary = "Get document download URL", description = "Generate a temporary download URL for a specific document for an application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Download URL generated successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/applications/{applicationId}/download-url")
    public ResponseEntity<String> getDownloadUrl(
            @Parameter(description = "The ID of the application", required = true)
            @PathVariable UUID applicationId) {
        
        log.info("Generating download URL for application: {}", applicationId);
        String url = documentService.getDownloadUrlForApplication(applicationId);
        return ResponseEntity.ok(url);
    }

    /**
     * Retrieves the metadata for a specific document.
     *
     * @param documentId The UUID of the document.
     * @return A {@link ResponseEntity} containing the document metadata DTO.
     */
    @Operation(summary = "Get document metadata", description = "Retrieve metadata information for a specific document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document metadata retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponseDTO> getDocumentMetadata(
            @Parameter(description = "The ID of the document", required = true)
            @PathVariable UUID documentId) {
        
        log.info("Retrieving metadata for document: {}", documentId);
        DocumentResponseDTO document = documentService.getDocumentMetadata(documentId);
        return ResponseEntity.ok(document);
    }

    // /**
    //  * Retrieves a list of all documents associated with an application.
    //  *
    //  * @param applicationId The UUID of the application.
    //  * @return A {@link ResponseEntity} containing a list of document metadata DTOs.
    //  */
    // @Operation(summary = "Get all documents for an application", description = "Retrieve all documents for a specific job application")
    // @ApiResponses(value = {
    //     @ApiResponse(responseCode = "200", description = "Application documents retrieved successfully")
    // })
    // @GetMapping("/applications/{applicationId}/documents")
    // public ResponseEntity<List<DocumentResponseDTO>> getApplicationDocuments(
    //         @Parameter(description = "The ID of the job application", required = true)
    //         @PathVariable UUID applicationId) {
        
    //     log.info("Retrieving documents for application: {}", applicationId);
    //     List<DocumentResponseDTO> documents = documentService.getApplicationDocuments(applicationId);
    //     return ResponseEntity.ok(documents);
    // }

    // /**
    //  * Updates the metadata of a document, such as its application association.
    //  *
    //  * @param documentId    The UUID of the document to update.
    //  * @param applicationId The new application UUID to associate with the document.
    //  * @return A {@link ResponseEntity} containing the updated document metadata DTO.
    //  */
    // @Operation(summary = "Update document metadata", description = "Update the application association for a document")
    // @ApiResponses(value = {
    //     @ApiResponse(responseCode = "200", description = "Document metadata updated successfully"),
    //     @ApiResponse(responseCode = "404", description = "Document not found")
    // })
    // @PutMapping("/documents/{documentId}")
    // public ResponseEntity<DocumentResponseDTO> updateDocumentMetadata(
    //         @Parameter(description = "The ID of the document", required = true)
    //         @PathVariable UUID documentId,
            
    //         @Parameter(description = "The new application ID to associate with the document")
    //         @RequestParam(value = "applicationId", required = false) UUID applicationId) {
        
    //     log.info("Updating metadata for document: {}, new application: {}", documentId, applicationId);
    //     DocumentResponseDTO updatedDocument = documentService.updateDocumentMetadata(documentId, applicationId);
    //     return ResponseEntity.ok(updatedDocument);
    // }

    // /**
    //  * Deletes a document.
    //  *
    //  * @param documentId The UUID of the document to delete.
    //  * @return A {@link ResponseEntity} with no content.
    //  */
    // @Operation(summary = "Delete document", description = "Delete a document and its associated file from S3")
    // @ApiResponses(value = {
    //     @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
    //     @ApiResponse(responseCode = "404", description = "Document not found")
    // })
    // @DeleteMapping("/documents/{documentId}")
    // public ResponseEntity<Void> deleteDocument(
    //         @Parameter(description = "The ID of the document to delete", required = true)
    //         @PathVariable UUID documentId) {
        
    //     log.info("Deleting document: {}", documentId);
    //     documentService.deleteDocument(documentId);
    //     return ResponseEntity.noContent().build();
    // }

    /**
     * Provides a simple health check endpoint.
     *
     * @return A {@link ResponseEntity} with a success message.
     */
    @Operation(summary = "Health check", description = "Check if the document service is running")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Document Management Service is running");
    }
} 