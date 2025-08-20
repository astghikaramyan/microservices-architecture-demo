package com.example.resourceservice.exception;

import com.example.resourceservice.model.ValidationErrorResponse;
import com.example.resourceservice.model.ErrorResponse;

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
