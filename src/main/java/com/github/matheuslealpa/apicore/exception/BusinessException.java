package com.github.matheuslealpa.apicore.exception;

/**
 * Violação de regra de negócio. Respondida como 422.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
