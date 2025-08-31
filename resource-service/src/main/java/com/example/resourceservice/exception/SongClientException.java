package com.example.resourceservice.exception;

import com.example.resourceservice.model.ErrorResponse;

public class SongClientException extends RuntimeException {
    private ErrorResponse errorResponse;

    public SongClientException(String message, ErrorResponse errorResponse) {
        super(message);
        this.errorResponse = errorResponse;
    }

    public SongClientException(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getSimpleErrorResponse() {
        return errorResponse;
    }

    public void setSimpleErrorResponse(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
