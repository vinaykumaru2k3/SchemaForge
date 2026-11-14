package com.schemaforge.exception;

/** Maps to HTTP CONFLICT in {@link GlobalExceptionHandler}. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
