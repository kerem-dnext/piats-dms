package com.piats.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) for sending standardized error responses.
 * <p>
 * This DTO provides a consistent structure for error messages across the API,
 * including a timestamp, HTTP status, error type, a descriptive message, and
 * the path where the error occurred.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    /** The timestamp when the error occurred. */
    private Instant timestamp;
    /** The HTTP status code. */
    private int status;
    /** The HTTP reason phrase for the status code (e.g., "Not Found"). */
    private String error;
    /** A developer-friendly message describing the error. */
    private String message;
    /** The API path that was requested. */
    private String path;
} 