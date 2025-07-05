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

@Service
@Slf4j
@Transactional
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    
    // Allowed file types for CV documents
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public DocumentService(DocumentRepository documentRepository, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    public UploadResponseDTO uploadDocument(MultipartFile file, UUID applicationId) throws IOException {
        log.info("Starting document upload for application: {}", applicationId);
        
        if (applicationId == null) {
            throw new IllegalArgumentException("An application ID must be provided.");
        }

        // Validate file
        validateFile(file);
        
        // Generate unique S3 key
        String documentId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = String.format("applications/%s/%s%s", applicationId, documentId, fileExtension);
        
        try {
            // Upload to S3
            s3Service.uploadFile(s3Key, file.getSize(), file.getInputStream(), file.getContentType());
            
            // Create metadata entity
            Document doc = Document.builder()
                .applicationId(applicationId)
                .originalFilename(file.getOriginalFilename())
                .s3Bucket(s3Service.getBucketName())
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
            
            // Save metadata to database
            Document savedDoc = documentRepository.save(doc);
            log.info("Document saved successfully with ID: {}", savedDoc.getId());
            
            // Generate initial download URL
            String downloadUrl = s3Service.generatePresignedUrl(s3Key, Duration.ofMinutes(10));
            
            return new UploadResponseDTO(savedDoc.getId(), "File uploaded successfully", downloadUrl);
            
        } catch (Exception e) {
            log.error("Failed to upload document for application: {}", applicationId, e);
            // If database save fails, clean up S3 file
            try {
                s3Service.deleteFile(s3Key);
            } catch (Exception cleanupException) {
                log.error("Failed to cleanup S3 file after failed upload: {}", s3Key, cleanupException);
            }
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID documentId) {
        log.info("Generating download URL for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        return s3Service.generatePresignedUrl(doc.getS3Key(), Duration.ofMinutes(10));
    }
    
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentMetadata(UUID documentId) {
        log.info("Retrieving metadata for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        return mapToResponseDTO(doc);
    }
    
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getApplicationDocuments(UUID applicationId) {
        log.info("Retrieving documents for application: {}", applicationId);
        
        List<Document> documents = documentRepository.findByApplicationId(applicationId);
        return documents.stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }
    
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        try {
            // Delete from S3 first
            s3Service.deleteFile(doc.getS3Key());
            
            // Then delete metadata from database
            documentRepository.delete(doc);
            
            log.info("Document deleted successfully: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document: {}", documentId, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    public DocumentResponseDTO updateDocumentMetadata(UUID documentId, UUID applicationId) {
        log.info("Updating document metadata for document: {}", documentId);
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        
        doc.setApplicationId(applicationId);
        Document updatedDoc = documentRepository.save(doc);
        
        log.info("Document metadata updated successfully: {}", documentId);
        return mapToResponseDTO(updatedDoc);
    }
    
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
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
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