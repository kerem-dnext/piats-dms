package com.piats.dms.service;

import com.piats.dms.dto.DocumentResponseDTO;
import com.piats.dms.dto.UploadResponseDTO;
import com.piats.dms.entity.Document;
import com.piats.dms.exception.DocumentNotFoundException;
import com.piats.dms.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for handling business logic related to documents.
 * <p>
 * This class orchestrates operations between the document repository and the S3 service.
 * It is responsible for tasks like validating, uploading, retrieving, and deleting documents,
 * and ensuring that the database metadata and S3 storage are consistent. All operations
 * are transactional.
 * </p>
 */
@Service
@Slf4j
@Transactional
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    
    /**
     * A list of allowed MIME types for document uploads. This provides a basic
     * level of security and validation, ensuring that only expected file types
     * (e.g., resumes) are processed.
     */
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    /**
     * The maximum allowed file size for uploads, set to 10MB. This prevents
     * excessively large files from consuming too much storage or bandwidth.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Constructs a {@code DocumentService} with its dependencies.
     *
     * @param documentRepository The repository for document metadata.
     * @param s3Service          The service for S3 file operations.
     */
    public DocumentService(DocumentRepository documentRepository, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    /**
     * Uploads a document, stores it in S3, and saves its metadata to the database.
     * <p>
     * This method performs validation, generates a unique key for the S3 object,
     * uploads the file, and then creates a corresponding record in the documents table.
     * If any part of this process fails, it attempts to clean up by deleting the
     * file from S3 to prevent orphaned data.
     * </p>
     *
     * @param file          The multipart file to upload.
     * @param applicationId The UUID of the application to associate the document with.
     * @return An {@link UploadResponseDTO} containing details of the successful upload.
     * @throws IOException if an error occurs during file stream processing.
     */
    public UploadResponseDTO uploadDocument(MultipartFile file, UUID applicationId) throws IOException {
        log.info("Starting document upload for application: {}", applicationId);
        
        if (applicationId == null) {
            throw new IllegalArgumentException("An application ID must be provided.");
        }

        validateFile(file);
        
        String documentId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = String.format("applications/%s/%s%s", applicationId, documentId, fileExtension);
        
        try {
            s3Service.uploadFile(s3Key, file.getSize(), file.getInputStream(), file.getContentType());
            
            Document doc = Document.builder()
                .applicationId(applicationId)
                .originalFilename(file.getOriginalFilename())
                .s3Bucket(s3Service.getBucketName())
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
            
            Document savedDoc = documentRepository.save(doc);
            log.info("Document saved successfully with ID: {}", savedDoc.getId());
            
            String downloadUrl = s3Service.generatePresignedUrl(s3Key, Duration.ofMinutes(10));
            
            return new UploadResponseDTO(savedDoc.getId(), "File uploaded successfully", downloadUrl);
            
        } catch (Exception e) {
            log.error("Failed to upload document for application: {}", applicationId, e);
            try {
                s3Service.deleteFile(s3Key);
            } catch (Exception cleanupException) {
                log.error("Failed to cleanup S3 file after failed upload: {}", s3Key, cleanupException);
            }
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    /**
     * Generates a temporary, presigned download URL for a document.
     * The transaction is read-only as this operation does not modify data.
     *
     * @param documentId The UUID of the document.
     * @return A string containing the presigned URL.
     * @throws DocumentNotFoundException if no document with the given ID is found.
     */
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID documentId) {
        log.info("Generating download URL for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        return s3Service.generatePresignedUrl(doc.getS3Key(), Duration.ofMinutes(10));
    }

    /**
     * Generates a temporary, presigned download URL for an application's document.
     * The transaction is read-only as this operation does not modify data.
     *
     * @param applicationId The UUID of the application.
     * @return A string containing the presigned URL.
     * @throws DocumentNotFoundException if no document for the given application is found.
     */
    @Transactional(readOnly = true)
    public String getDownloadUrlForApplication(UUID applicationId) {
        log.info("Generating download URL for application: {}", applicationId);

        Document doc = documentRepository.findByApplicationId(applicationId).stream().findFirst()
                .orElseThrow(() -> new DocumentNotFoundException("Document not found for application ID: " + applicationId));

        return s3Service.generatePresignedUrl(doc.getS3Key(), Duration.ofMinutes(10));
    }
    
    /**
     * Retrieves the metadata for a single document.
     * The transaction is read-only as this operation does not modify data.
     *
     * @param documentId The UUID of the document.
     * @return A {@link DocumentResponseDTO} with the document's metadata.
     * @throws DocumentNotFoundException if no document with the given ID is found.
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentMetadata(UUID documentId) {
        log.info("Retrieving metadata for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        return mapToResponseDTO(doc);
    }
    
    /**
     * Retrieves all document metadata for a specific application.
     * The transaction is read-only as this operation does not modify data.
     *
     * @param applicationId The UUID of the application.
     * @return A list of {@link DocumentResponseDTO}s.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getApplicationDocuments(UUID applicationId) {
        log.info("Retrieving documents for application: {}", applicationId);
        
        List<Document> documents = documentRepository.findByApplicationId(applicationId);
        return documents.stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Deletes a document from S3 and its metadata from the database.
     * <p>
     * The file is deleted from S3 first. If that succeeds, the database record
     * is deleted. This order minimizes the risk of having an orphaned database
     * record pointing to a non-existent S3 object.
     * </p>
     *
     * @param documentId The UUID of the document to delete.
     * @throws DocumentNotFoundException if no document with the given ID is found.
     * @throws RuntimeException if the deletion fails at either the S3 or database step.
     */
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        try {
            s3Service.deleteFile(doc.getS3Key());
            
            documentRepository.delete(doc);
            
            log.info("Document deleted successfully: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document: {}", documentId, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    /**
     * Updates the metadata of a document, such as its application association.
     *
     * @param documentId    The UUID of the document to update.
     * @param applicationId The new application UUID to associate with the document.
     * @return A {@link DocumentResponseDTO} with the updated metadata.
     * @throws DocumentNotFoundException if no document with the given ID is found.
     */
    public DocumentResponseDTO updateDocumentMetadata(UUID documentId, UUID applicationId) {
        log.info("Updating document metadata for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        doc.setApplicationId(applicationId);
        Document updatedDoc = documentRepository.save(doc);
        
        log.info("Document metadata updated successfully: {}", documentId);
        return mapToResponseDTO(updatedDoc);
    }
    
    /**
     * Validates an uploaded file based on size, type, and name.
     *
     * @param file The {@link MultipartFile} to validate.
     * @throws IllegalArgumentException if the file is invalid.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }
        
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Supported types: PDF, DOC, DOCX, TXT");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }
    }
    
    /**
     * Extracts the file extension from a filename.
     *
     * @param filename The full name of the file.
     * @return The file extension including the dot (e.g., ".pdf"), or an empty string if none is found.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Maps a {@link Document} entity to a {@link DocumentResponseDTO}.
     *
     * @param doc The entity to map.
     * @return The resulting DTO.
     */
    private DocumentResponseDTO mapToResponseDTO(Document doc) {
        return DocumentResponseDTO.builder()
            .id(doc.getId())
            .applicationId(doc.getApplicationId())
            .originalFilename(doc.getOriginalFilename())
            .fileType(doc.getFileType())
            .fileSize(doc.getFileSize())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
    }
} 