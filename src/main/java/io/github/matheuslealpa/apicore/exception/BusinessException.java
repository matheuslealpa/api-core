package io.github.matheuslealpa.apicore.exception;

/**
 * Business rule violation. Responded as 422.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
