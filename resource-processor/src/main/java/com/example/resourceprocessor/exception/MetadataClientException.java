package com.example.resourceprocessor.exception;

import com.example.resourceprocessor.model.ErrorResponse;

public class MetadataClientException extends RuntimeException {
    private ErrorResponse errorResponse;

    public MetadataClientException(String message, ErrorResponse errorResponse) {
        super(message);
        this.errorResponse = errorResponse;
    }

    public MetadataClientException(ErrorResponse errorResponse) {
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
