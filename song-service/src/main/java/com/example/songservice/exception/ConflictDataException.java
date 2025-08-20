package com.example.songservice.exception;

import com.example.songservice.model.ErrorResponse;

public class ConflictDataException extends RuntimeException {
    private ErrorResponse errorResponse;


    public ConflictDataException(final String message) {
        super(message);
    }

    public ConflictDataException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConflictDataException(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getSimpleErrorResponse() {
        return errorResponse;
    }

    public void setSimpleErrorResponse(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
