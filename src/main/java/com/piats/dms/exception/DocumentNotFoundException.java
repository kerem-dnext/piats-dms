package com.piats.dms.exception;

/**
 * An exception thrown when a requested document cannot be found in the system.
 * <p>
 * This is a {@link RuntimeException} that indicates a specific business-level
 * error: a client requested a document by an identifier that does not exist.
 * </p>
 */
public class DocumentNotFoundException extends RuntimeException {
    /**
     * Constructs a new {@code DocumentNotFoundException} with the specified detail message.
     *
     * @param message The detail message, which is saved for later retrieval by the {@link #getMessage()} method.
     */
    public DocumentNotFoundException(String message) {
        super(message);
    }
} 