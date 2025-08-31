package com.example.resourceservice.util;

import com.example.resourceservice.model.ErrorResponse;
import org.springframework.stereotype.Service;

@Service
public class DataPreparerService {
    public ErrorResponse prepareErrorResponse(final String message, final String code) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(message);
        errorResponse.setErrorCode(code);
        return errorResponse;
    }
}
