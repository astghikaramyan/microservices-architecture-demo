package com.example.storageservice.exception;

import com.example.storageservice.model.error.ErrorResponse;
import com.example.storageservice.model.error.ValidationErrorResponse;

public class NotFoundException extends RuntimeException {
    private ValidationErrorResponse validationErrorResponse;
    private ErrorResponse errorResponse;
    public NotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(final String message) {
        super(message);
    }
    public NotFoundException(final String message, final ValidationErrorResponse validationErrorResponse) {
        super(message);
        this.validationErrorResponse = validationErrorResponse;
    }
    public NotFoundException(ErrorResponse e){
        this.errorResponse = e;
    }

    public ValidationErrorResponse getErrorResponse() {
        return validationErrorResponse;
    }

    public void setErrorResponse(final ValidationErrorResponse validationErrorResponse) {
        this.validationErrorResponse = validationErrorResponse;
    }

    public ErrorResponse getSimpleErrorResponse() {
        return errorResponse;
    }

    public void setSimpleErrorResponse(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
