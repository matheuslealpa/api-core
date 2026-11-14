package io.github.matheuslealpa.apicore.exception;

/**
 * A request precondition, such as {@code If-Match}, does not hold for the current state of the resource.
 * Responded as 412.
 */
public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String message) {
        super(message);
    }
}
