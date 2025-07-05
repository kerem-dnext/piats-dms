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

/**
 * A central handler for exceptions thrown across the entire application.
 * <p>
 * This {@link RestControllerAdvice} catches specific exceptions and transforms
 * them into a standardized {@link ErrorResponseDTO}, providing consistent error
 * handling and responses for all API endpoints.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * A private helper method to build a standardized error response entity.
     *
     * @param status  The HTTP status for the response.
     * @param message The error message to include in the response.
     * @param path    The request path where the error occurred.
     * @return A {@link ResponseEntity} containing the structured error DTO.
     */
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

    /**
     * Handles {@link DocumentNotFoundException}.
     * <p>
     * This is triggered when a client requests a document that does not exist.
     * It returns a 404 Not Found response.
     * </p>
     *
     * @param ex      The caught {@code DocumentNotFoundException}.
     * @param request The current web request.
     * @return A 404 Not Found response with error details.
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleDocumentNotFound(DocumentNotFoundException ex, WebRequest request) {
        log.warn("Document not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false));
    }

    /**
     * Handles {@link IllegalArgumentException}.
     * <p>
     * This typically occurs when method arguments are invalid, such as an empty file
     * or a missing required parameter. It returns a 400 Bad Request response.
     * </p>
     *
     * @param ex      The caught {@code IllegalArgumentException}.
     * @param request The current web request.
     * @return A 400 Bad Request response with error details.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false));
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException}.
     * <p>
     * This occurs when a controller method argument cannot be converted from a
     * request parameter (e.g., passing "text" for a UUID). It returns a 400 Bad Request.
     * </p>
     *
     * @param ex      The caught {@code MethodArgumentTypeMismatchException}.
     * @param request The current web request.
     * @return A 400 Bad Request response with a specific type mismatch message.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("The parameter '%s' with value '%s' could not be converted to type '%s'.",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        log.warn("Parameter type mismatch: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getDescription(false));
    }

    /**
     * Handles {@link MaxUploadSizeExceededException}.
     * <p>
     * This is triggered when an uploaded file exceeds the configured maximum size.
     * It returns a 413 Payload Too Large response.
     * </p>
     *
     * @param ex      The caught {@code MaxUploadSizeExceededException}.
     * @param request The current web request.
     * @return A 413 Payload Too Large response.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum upload file size exceeded.", request.getDescription(false));
    }

    /**
     * Handles {@link S3Exception}.
     * <p>
     * This catches exceptions from the AWS S3 client, indicating issues with the
     * storage service. It returns a 503 Service Unavailable response to signal
     * that the issue is with a downstream dependency.
     * </p>
     *
     * @param ex      The caught {@code S3Exception}.
     * @param request The current web request.
     * @return A 503 Service Unavailable response with a generic message.
     */
    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleS3Exception(S3Exception ex, WebRequest request) {
        log.error("An error occurred with the S3 service. Status Code: {}, AWS Request ID: {}", ex.statusCode(), ex.requestId(), ex);
        String message = "The storage service is currently unavailable or encountered an error. Please try again later.";
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, message, request.getDescription(false));
    }

    /**
     * A generic handler for any other unhandled {@link Exception}.
     * <p>
     * This serves as a catch-all for unexpected errors. It logs the full exception
     * and returns a 500 Internal Server Error, hiding implementation details from the client.
     * </p>
     *
     * @param ex      The caught {@code Exception}.
     * @param request The current web request.
     * @return A 500 Internal Server Error with a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        String message = "An unexpected internal error occurred. The technical team has been notified.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getDescription(false));
    }
} 