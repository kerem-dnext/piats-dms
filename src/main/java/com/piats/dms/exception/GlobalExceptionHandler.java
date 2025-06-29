// package com.piats.dms.exception;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.multipart.MaxUploadSizeExceededException;

// import java.time.Instant;
// import java.util.HashMap;
// import java.util.Map;

// @RestControllerAdvice
// @Slf4j
// public class GlobalExceptionHandler {

//     @ExceptionHandler(DocumentNotFoundException.class)
//     public ResponseEntity<Map<String, Object>> handleDocumentNotFound(DocumentNotFoundException ex) {
//         Map<String, Object> error = new HashMap<>();
//         error.put("timestamp", Instant.now());
//         error.put("status", HttpStatus.NOT_FOUND.value());
//         error.put("error", "Document Not Found");
//         error.put("message", ex.getMessage());
        
//         log.error("Document not found: {}", ex.getMessage());
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//     }

//     @ExceptionHandler(MaxUploadSizeExceededException.class)
//     public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
//         Map<String, Object> error = new HashMap<>();
//         error.put("timestamp", Instant.now());
//         error.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
//         error.put("error", "File Too Large");
//         error.put("message", "Maximum upload size exceeded");
        
//         log.error("File upload size exceeded: {}", ex.getMessage());
//         return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
//     }

//     @ExceptionHandler(Exception.class)
//     public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
//         Map<String, Object> error = new HashMap<>();
//         error.put("timestamp", Instant.now());
//         error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//         error.put("error", "Internal Server Error");
//         error.put("message", "An unexpected error occurred");
        
//         log.error("Unexpected error: {}", ex.getMessage(), ex);
//         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//     }
// } 