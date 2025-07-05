# Document Management System (DMS) API

This document provides a high-level overview of the Document Management System (DMS) for the PIATS Applicant Tracking System (ATS). The DMS is a Spring Boot microservice responsible for handling the upload, storage, and retrieval of documents, primarily CVs associated with job applications.

## 1. Project Structure

The project follows a standard Maven project structure. The core application code is located in `src/main/java/com/piats/dms`.

-   **`config`**: Contains Spring configuration classes for setting up beans related to security, Amazon S3, and OpenAPI documentation.
-   **`controller`**: Holds the REST controllers that expose the public API endpoints.
-   **`dto`**: (Data Transfer Objects) Defines the data structures used for API requests and responses.
-   **`entity`**: Contains the JPA entity classes that map to database tables.
-   **`exception`**: Includes custom exception classes and a global exception handler for standardized error responses.
-   **`repository`**: Defines Spring Data JPA repositories for database interactions.
-   **`service`**: Contains the business logic, orchestrating operations between controllers, repositories, and external services like S3.

## 2. Core Components

### `DocumentController`

The entry point for all API requests. It handles HTTP routing and delegates business logic to the `DocumentService`. It uses OpenAPI annotations to provide self-documenting API endpoints.

### `DocumentService`

The core of the application's business logic. It is responsible for:
-   Validating uploaded files (size, type).
-   Orchestrating the upload of files to Amazon S3 via the `S3Service`.
-   Creating and managing document metadata in the database via the `DocumentRepository`.
-   Generating secure, temporary download URLs.
-   Handling deletions and updates, ensuring consistency between the database and S3.

### `S3Service`

An abstraction layer for interacting with Amazon S3. It simplifies operations like `upload`, `delete`, and `generatePresignedUrl`, hiding the complexity of the AWS SDK.

### `Document` Entity

A JPA entity representing the metadata of a stored document. It includes fields for the application ID it belongs to, its location in S3 (bucket and key), and other file properties like name, size, and type.

### `GlobalExceptionHandler`

A centralized mechanism for handling exceptions. It catches specific exceptions (e.g., `DocumentNotFoundException`, `MaxUploadSizeExceededException`) and standard Spring exceptions, returning a consistent JSON error response (`ErrorResponseDTO`) for each.

## 3. API Endpoints

The API is documented using OpenAPI (Swagger). The full, interactive API documentation can be accessed at `/swagger-ui/index.html` when the service is running.

Key endpoints include:

-   `POST /api/v1/applications/{applicationId}/documents`: Upload a document for an application.
-   `GET /api/v1/documents/{documentId}/download-url`: Get a temporary URL to download a document.
-   `GET /api/v1/documents/{documentId}`: Retrieve a document's metadata.
-   `DELETE /api/v1/documents/{documentId}`: Delete a document.

## 4. Configuration

The application's behavior is configured in `src/main/resources/application.properties`. Key configuration values include:
-   AWS credentials and S3 bucket details.
-   Server port.
-   Database connection details (if applicable).
-   Maximum file upload size.
