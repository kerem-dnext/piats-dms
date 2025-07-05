# Document Management System (DMS)

A Spring Boot microservice for handling the upload, storage, and retrieval of documents. This system is designed to integrate with a larger application, such as an Applicant Tracking System (ATS), to manage files associated with specific entities (e.g., job applications). It uses Amazon S3 for distributed object storage and a PostgreSQL database for metadata management.

## Features

-   **File Upload**: Upload documents and associate them with an application ID.
-   **Secure Downloads**: Generate temporary, presigned URLs for secure document access.
-   **Metadata Management**: Create, retrieve, update, and delete document metadata stored in a relational database.
-   **List by Application**: Retrieve all documents associated with a specific application.
-   **API Documentation**: Interactive API documentation provided via Swagger (OpenAPI).
-   **Health Check**: An endpoint to monitor the service status.

## Getting Started

Follow these instructions to get a local development environment up and running.

### Prerequisites

-   **Java 17**: Ensure you have JDK 17 installed.
-   **Apache Maven**: The project uses Maven for dependency management and building.
-   **PostgreSQL**: A running instance of PostgreSQL is required for storing document metadata.
-   **AWS Account**: An AWS account with an S3 bucket is needed for file storage.

### Local Setup

1.  **Clone the Repository**
    ```sh
    git clone <your-repository-url>
    cd dms
    ```

2.  **Configure the Application**
    The main configuration is in `src/main/resources/application.yml`. You will need to set up your environment-specific properties for the database and AWS.

    **Database Setup**:
    -   Create a PostgreSQL database. The default configuration expects a database named `dms_db`.
    -   Update the datasource properties in `application.yml` with your local database URL, username, and password.

    **AWS Setup**:
    -   Create an AWS S3 bucket.
    -   Update the `aws` properties in `application.yml` with your S3 bucket name, region, and AWS credentials.

3.  **Install Dependencies**
    Run the following Maven command to download dependencies and build the project:
    ```sh
    mvn clean install
    ```

4.  **Run the Application**
    Use the Spring Boot Maven plugin to start the application:
    ```sh
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8081` by default.

## Configuration

All configuration is managed in `src/main/resources/application.yml`.

| Key                             | Description                                            |
| ------------------------------- | ------------------------------------------------------ |
| `server.port`                   | The port the application runs on (default: `8081`).    |
| `spring.datasource.url`         | The JDBC URL for the PostgreSQL database.              |
| `spring.datasource.username`    | The username for the database connection.              |
| `spring.datasource.password`    | The password for the database connection.              |
| `aws.s3.bucket-name`            | The name of the AWS S3 bucket for storing files.       |
| `aws.s3.region`                 | The AWS region where the bucket is located.            |
| `aws.credentials.access-key`    | Your AWS access key.                                   |
| `aws.credentials.secret-key`    | Your AWS secret key.                                   |

> **Security Warning**: The `application.yml` file in the repository contains placeholder credentials. It is strongly recommended to use environment variables or a secrets management tool for sensitive data like database and AWS credentials in a production environment. Avoid committing real credentials to version control.

## API Documentation

The API is documented using OpenAPI (Swagger). Once the application is running, the full, interactive API documentation can be accessed at:

**[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)**

### API Endpoints

This section details the available endpoints, including example requests and responses.

---

#### 1. Upload a Document

Uploads a document and associates it with a specific application.

-   **Method**: `POST`
-   **Path**: `/api/v1/applications/{applicationId}/documents`
-   **Request**: `multipart/form-data`
    -   `file`: The document file to upload.
    -   `applicationId`: The UUID of the application.

-   **Success Response (200 OK)**
    ```json
    {
      "documentId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "fileName": "cv_johndoe.pdf",
      "message": "File uploaded successfully."
    }
    ```

-   **Error Response (400 Bad Request)**
    ```json
    {
      "timestamp": "2023-10-27T10:30:00Z",
      "status": 400,
      "error": "Bad Request",
      "message": "Invalid file or parameters."
    }
    ```

---

#### 2. Get Document Download URL

Generates a temporary, presigned URL to securely download a document from S3.

-   **Method**: `GET`
-   **Path**: `/api/v1/documents/{documentId}/download-url`

-   **Success Response (200 OK)**
    ```
    "https://piats-dms-dev.s3.us-east-1.amazonaws.com/cv_johndoe.pdf?AWSAccessKeyId=...&Expires=...&Signature=..."
    ```

-   **Error Response (404 Not Found)**
    ```json
    {
      "timestamp": "2023-10-27T10:30:00Z",
      "status": 404,
      "error": "Not Found",
      "message": "Document not found with ID: a1b2c3d4-e5f6-7890-1234-567890abcdef"
    }
    ```

---

#### 3. Get Document Metadata

Retrieves metadata for a specific document.

-   **Method**: `GET`
-   **Path**: `/api/v1/documents/{documentId}`

-   **Success Response (200 OK)**
    ```json
    {
      "documentId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "applicationId": "f0e9d8c7-b6a5-4321-fedc-ba9876543210",
      "fileName": "cv_johndoe.pdf",
      "fileType": "application/pdf",
      "fileSize": 123456,
      "createdAt": "2023-10-27T10:00:00Z"
    }
    ```

---

#### 4. Get All Documents for an Application

Retrieves metadata for all documents associated with a specific application.

-   **Method**: `GET`
-   **Path**: `/api/v1/applications/{applicationId}/documents`

-   **Success Response (200 OK)**
    ```json
    [
      {
        "documentId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
        "applicationId": "f0e9d8c7-b6a5-4321-fedc-ba9876543210",
        "fileName": "cv_johndoe.pdf",
        "fileType": "application/pdf",
        "fileSize": 123456,
        "createdAt": "2023-10-27T10:00:00Z"
      },
      {
        "documentId": "b2c3d4e5-f6a7-8901-2345-67890abcdef1",
        "applicationId": "f0e9d8c7-b6a5-4321-fedc-ba9876543210",
        "fileName": "cover_letter.pdf",
        "fileType": "application/pdf",
        "fileSize": 78901,
        "createdAt": "2023-10-27T10:05:00Z"
      }
    ]
    ```

---

#### 5. Update Document Metadata

Updates a document's metadata, such as re-associating it with a new application ID.

-   **Method**: `PUT`
-   **Path**: `/api/v1/documents/{documentId}?applicationId={newApplicationId}`

-   **Success Response (200 OK)**
    ```json
    {
      "documentId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "applicationId": "a new-application-id-uuid-goes-here",
      "fileName": "cv_johndoe.pdf",
      "fileType": "application/pdf",
      "fileSize": 123456,
      "createdAt": "2023-10-27T10:00:00Z"
    }
    ```

---

#### 6. Delete a Document

Deletes a document's metadata from the database and the corresponding file from S3.

-   **Method**: `DELETE`
-   **Path**: `/api/v1/documents/{documentId}`

-   **Success Response**: `204 No Content`

---

#### 7. Health Check

Provides a simple health check endpoint to confirm the service is running.

-   **Method**: `GET`
-   **Path**: `/api/v1/health`

-   **Success Response (200 OK)**
    ```
    Document Management Service is running
    ```

## Running Tests

To run the automated test suite, use the following Maven command:

```sh
mvn test
```

## Architectural Overview

The application follows a standard layered architecture common in Spring Boot applications.

-   **`controller`**: Exposes the REST API endpoints. It receives HTTP requests, validates them, and passes them to the `service` layer. It uses OpenAPI annotations for self-documentation.
-   **`service`**: Contains the core business logic. The `DocumentService` orchestrates operations between the database and S3, while the `S3Service` provides an abstraction for interacting with the AWS S3 API.
-   **`repository`**: Defines Spring Data JPA interfaces for all database interactions with the `Document` entity.
-   **`entity`**: Contains the `Document` JPA entity, which maps to a table in the PostgreSQL database and stores file metadata.
-   **`exception`**: Provides a `GlobalExceptionHandler` to catch exceptions and return standardized JSON error responses, ensuring consistent error handling across the API.
