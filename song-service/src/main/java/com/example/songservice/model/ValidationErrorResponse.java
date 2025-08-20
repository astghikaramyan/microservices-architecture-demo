package com.example.songservice.model;

import java.util.Map;

public class ValidationErrorResponse {
    private String errorMessage;
    private Map<String, String> errorDetails;
    private String errorCode;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, String> getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(final Map<String, String> errorDetails) {
        this.errorDetails = errorDetails;
    }
}
