
### 1. High-Level Architecture

First, let's visualize the components and their interactions.

```
+----------------+      +---------------------------+      +--------------------+
|                |      |                           |      |                    |
|   API Gateway  |----->|   Document Microservice   |----->|  PostgreSQL / MySQL|
|  (or other     |      |      (Spring Boot)        |      |      (Metadata)    |
|   services)    |      |                           |      |                    |
+----------------+      +-------------+-------------+      +--------------------+
                                      |
                                      | S3 API Calls
                                      |
                                      v
                               +--------------+
                               |              |
                               |   AWS S3     |
                               |   (Files)    |
                               |              |
                               +--------------+
```

**Workflow for a CV Upload:**

1.  A client (e.g., a frontend application or another microservice) sends a `POST` request with the user's CV file to the `Document Microservice`.
2.  The microservice receives the file.
3.  It generates a unique identifier (UUID) for the document.
4.  It uploads the file to a designated S3 bucket with a unique key (e.g., `user_id/document_uuid/original_filename.pdf`).
5.  After a successful S3 upload, it saves the document's metadata (original filename, S3 key, user ID, MIME type, etc.) into its dedicated database.
6.  It returns the unique document ID and other metadata to the client.

---

### 2. Database Design (Metadata Storage)

This is the core of your service's state. You need to store pointers to the files in S3 and any other relevant information that you might want to query without having to download the file. A relational database like PostgreSQL or MySQL is a great choice.

Let's define a `documents` table.

**Table: `documents`**

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | The unique identifier for this document record. Using UUID is best practice in microservices. |
| `user_id` | `UUID` | `NOT NULL` | The ID of the user who owns this document. This is a foreign key to your `users` table (which might live in another service). |
| `application_id` | `UUID` | `NULLABLE` | The ID of the job application this CV is associated with. Can be null if it's just stored in a user's profile. |
| `s3_bucket` | `VARCHAR(255)` | `NOT NULL` | The name of the S3 bucket where the file is stored. (e.g., `my-ats-cvs-prod`) |
| `s3_key` | `VARCHAR(1024)`| `NOT NULL, UNIQUE` | The full path/key of the object within the S3 bucket. (e.g., `documents/u-123/doc-456.pdf`) |
| `original_filename`| `VARCHAR(255)` | `NOT NULL` | The original name of the file as uploaded by the user. (e.g., `JohnDoe_Resume_2023.pdf`) |
| `file_type` | `VARCHAR(100)` | `NOT NULL` | The MIME type of the file (e.g., `application/pdf`, `application/msword`). |
| `file_size` | `BIGINT` | `NOT NULL` | The size of the file in bytes. |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Timestamp of when the record was created. |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Timestamp of when the record was last updated. |

**SQL Schema (PostgreSQL example):**

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    application_id UUID,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

---


#### 3.2. Application Configuration (`application.yml`)

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ats_documents_db
    username: your_db_user
    password: your_db_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # Use 'update' for dev, 'validate' or 'none' for prod
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Custom Application Properties for AWS
aws:
  s3:
    bucket-name: "my-ats-cvs-prod"
    region: "us-east-1"
    # Credentials should be configured via environment variables, IAM roles, or config files
    # access-key-id: YOUR_ACCESS_KEY
    # secret-access-key: YOUR_SECRET_KEY
```

#### 3.3. Code Structure (Packages)

```
com.yourapp.ats.document
├── config/
│   └── S3Config.java              // Configures the S3Client bean
├── controller/
│   └── DocumentController.java    // REST API endpoints
├── dto/
│   ├── DocumentResponseDTO.java   // DTO for sending document data to client
│   └── UploadResponseDTO.java     // DTO for upload success response
├── entity/
│   └── Document.java              // JPA entity for the 'documents' table
├── repository/
│   └── DocumentRepository.java    // Spring Data JPA repository interface
├── service/
│   ├── DocumentService.java       // Core business logic
│   └── S3Service.java             // Encapsulates all S3 interactions
└── exception/
    ├── DocumentNotFoundException.java
    └── GlobalExceptionHandler.java  // Handles exceptions globally
```

#### 3.4. Code Implementation Snippets

**Entity (`Document.java`)**
```java
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID applicationId;

    @Column(nullable = false)
    private String s3Bucket;

    @Column(nullable = false, unique = true)
    private String s3Key;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

**Repository (`DocumentRepository.java`)**
```java
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserId(UUID userId);
}
```

**S3 Service (`S3Service.java`)**
This abstracts S3 logic, making it testable and reusable.
```java
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

import java.io.InputStream;
import java.time.Duration;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public void uploadFile(String key, long contentLength, InputStream inputStream) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    public String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // Link expires in 10 minutes
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public void deleteFile(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
```

**Main Business Logic (`DocumentService.java`)**
```java
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
// ... other imports

@Service
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final S3Service s3Service;

    // Constructor injection
    public DocumentService(DocumentRepository documentRepository, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    public Document uploadDocument(MultipartFile file, UUID userId) throws IOException {
        // 1. Generate a unique key for S3 (improved structure)
        String documentId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = String.format("documents/%s/%s%s", 
            userId, documentId, fileExtension);

        // 2. Upload to S3
        s3Service.uploadFile(s3Key, file.getSize(), file.getInputStream());

        // 3. Create metadata entity
        Document doc = Document.builder()
            .userId(userId)
            .originalFilename(file.getOriginalFilename())
            .s3Bucket(s3Service.getBucketName()) // Get bucket from S3Service
            .s3Key(s3Key)
            .fileSize(file.getSize())
            .fileType(file.getContentType())
            .build();
        
        // 4. Save metadata to the database
        return documentRepository.save(doc);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public String getDownloadUrl(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
        return s3Service.generatePresignedUrl(doc.getS3Key());
    }
    
    // other methods like getDocumentMetadata, deleteDocument, etc.
}
```

**Controller (`DocumentController.java`)**
```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
// ... DTOs and other imports

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponseDTO> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("userId") UUID userId) {
        try {
            Document savedDoc = documentService.uploadDocument(file, userId);
            UploadResponseDTO response = new UploadResponseDTO(savedDoc.getId(), "File uploaded successfully.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            // Use a proper exception handler
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{documentId}/download-url")
    public ResponseEntity<String> getDownloadUrl(@PathVariable UUID documentId) {
        String url = documentService.getDownloadUrl(documentId);
        return ResponseEntity.ok(url);
    }
}
```

### 4. Important Considerations

*   **Security**:
    *   **Pre-signed URLs**: The design uses S3 pre-signed URLs for downloads. This is highly secure because it grants temporary, limited access to a private S3 object without exposing your credentials. The link expires automatically.
    *   **API Security**: Your controller endpoints should be secured, likely using Spring Security with JWT tokens or OAuth2 to ensure only authenticated and authorized users can upload or access documents.
*   **Synchronous Processing**: The design is synchronous and simplified. The client waits for the file to be uploaded to S3 and the DB record to be created before receiving a response.
*   **S3 Key Structure**: The improved S3 key structure (`documents/user_id/document_uuid.extension`) provides better organization, avoids filename encoding issues, and maintains uniqueness without exposing original filenames in the S3 path.
*   **Scalability**: Without indexes, queries filtering by user_id or application_id may become slower as the dataset grows. Consider adding them back selectively based on actual query patterns in production.