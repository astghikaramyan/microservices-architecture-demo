package com.example.resourceprocessor.exception;

import com.example.resourceprocessor.model.ErrorResponse;
import com.example.resourceprocessor.model.ValidationErrorResponse;


public class InvalidDataException extends RuntimeException{
    private ValidationErrorResponse validationErrorResponse;
    private ErrorResponse errorResponse;
    public InvalidDataException(final String message) {
        super(message);
    }
    public InvalidDataException(final ValidationErrorResponse validationErrorResponse) {
        this.validationErrorResponse = validationErrorResponse;
    }

    public InvalidDataException(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
    public InvalidDataException(final String message, final ValidationErrorResponse validationErrorResponse) {
        super(message);
        this.validationErrorResponse = validationErrorResponse;
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
