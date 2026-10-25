package io.github.matheuslealpa.apicore.exception;

/**
 * Recurso inexistente. Respondida como 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
