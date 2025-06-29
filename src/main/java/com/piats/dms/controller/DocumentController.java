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

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management", description = "APIs for managing CV and document uploads")
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Upload a document", description = "Upload a CV or document file for a specific user and optionally associate it with a job application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
        @ApiResponse(responseCode = "413", description = "File too large"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadDocument(
            @Parameter(description = "The file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "The ID of the user uploading the document", required = true)
            @RequestParam("userId") UUID userId,
            
            @Parameter(description = "The ID of the job application (optional)")
            @RequestParam(value = "applicationId", required = false) UUID applicationId) {
        
        try {
            log.info("Received upload request for user: {}, application: {}, filename: {}", 
                    userId, applicationId, file.getOriginalFilename());
            
            UploadResponseDTO response = documentService.uploadDocument(file, userId, applicationId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get document download URL", description = "Generate a temporary download URL for a specific document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Download URL generated successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/{documentId}/download-url")
    public ResponseEntity<String> getDownloadUrl(
            @Parameter(description = "The ID of the document", required = true)
            @PathVariable UUID documentId) {
        
        log.info("Generating download URL for document: {}", documentId);
        String url = documentService.getDownloadUrl(documentId);
        return ResponseEntity.ok(url);
    }

    @Operation(summary = "Get document metadata", description = "Retrieve metadata information for a specific document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document metadata retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponseDTO> getDocumentMetadata(
            @Parameter(description = "The ID of the document", required = true)
            @PathVariable UUID documentId) {
        
        log.info("Retrieving metadata for document: {}", documentId);
        DocumentResponseDTO document = documentService.getDocumentMetadata(documentId);
        return ResponseEntity.ok(document);
    }

    @Operation(summary = "Get user documents", description = "Retrieve all documents for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User documents retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponseDTO>> getUserDocuments(
            @Parameter(description = "The ID of the user", required = true)
            @PathVariable UUID userId) {
        
        log.info("Retrieving documents for user: {}", userId);
        List<DocumentResponseDTO> documents = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(documents);
    }

    @Operation(summary = "Get application documents", description = "Retrieve all documents for a specific job application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application documents retrieved successfully")
    })
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<DocumentResponseDTO>> getApplicationDocuments(
            @Parameter(description = "The ID of the job application", required = true)
            @PathVariable UUID applicationId) {
        
        log.info("Retrieving documents for application: {}", applicationId);
        List<DocumentResponseDTO> documents = documentService.getApplicationDocuments(applicationId);
        return ResponseEntity.ok(documents);
    }

    @Operation(summary = "Update document metadata", description = "Update the application association for a document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document metadata updated successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @PutMapping("/{documentId}")
    public ResponseEntity<DocumentResponseDTO> updateDocumentMetadata(
            @Parameter(description = "The ID of the document", required = true)
            @PathVariable UUID documentId,
            
            @Parameter(description = "The new application ID to associate with the document")
            @RequestParam(value = "applicationId", required = false) UUID applicationId) {
        
        log.info("Updating metadata for document: {}, new application: {}", documentId, applicationId);
        DocumentResponseDTO updatedDocument = documentService.updateDocumentMetadata(documentId, applicationId);
        return ResponseEntity.ok(updatedDocument);
    }

    @Operation(summary = "Delete document", description = "Delete a document and its associated file from S3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "The ID of the document to delete", required = true)
            @PathVariable UUID documentId) {
        
        log.info("Deleting document: {}", documentId);
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Health check", description = "Check if the document service is running")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Document Management Service is running");
    }
} 