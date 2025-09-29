package com.example.storageservice.util;

import org.springframework.stereotype.Service;

import com.example.storageservice.model.error.ErrorResponse;

@Service
public class DataPreparerService {
  public ErrorResponse prepareErrorResponse(final String message, final String code) {
    final ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setErrorMessage(message);
    errorResponse.setErrorCode(code);
    return errorResponse;
  }
}