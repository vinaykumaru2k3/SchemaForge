package com.schemaforge.exception;

/** Maps to HTTP NOT_FOUND in {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
