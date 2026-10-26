package io.github.matheuslealpa.apicore.exception;

/**
 * Resource not found. Responded as 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
