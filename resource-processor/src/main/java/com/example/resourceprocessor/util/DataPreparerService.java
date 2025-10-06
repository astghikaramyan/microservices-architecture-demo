package com.example.resourceprocessor.util;

import org.springframework.stereotype.Service;

import com.example.resourceprocessor.model.ErrorResponse;

@Service
public class DataPreparerService {
    public ErrorResponse prepareErrorResponse(final String message, final String code) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(message);
        errorResponse.setErrorCode(code);
        return errorResponse;
    }
}
