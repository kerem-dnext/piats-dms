package com.piats.dms.exception;

import com.piats.dms.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleDocumentNotFound(DocumentNotFoundException ex, WebRequest request) {
        log.warn("Document not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("The parameter '%s' with value '%s' could not be converted to type '%s'.",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        log.warn("Parameter type mismatch: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getDescription(false));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum upload file size exceeded.", request.getDescription(false));
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleS3Exception(S3Exception ex, WebRequest request) {
        log.error("An error occurred with the S3 service. Status Code: {}, AWS Request ID: {}", ex.statusCode(), ex.requestId(), ex);
        String message = "The storage service is currently unavailable or encountered an error. Please try again later.";
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, message, request.getDescription(false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        String message = "An unexpected internal error occurred. The technical team has been notified.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getDescription(false));
    }
} 