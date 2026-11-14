package com.schemaforge.exception;

/** Maps to HTTP FORBIDDEN in {@link GlobalExceptionHandler}. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
