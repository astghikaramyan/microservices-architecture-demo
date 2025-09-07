package com.example.resourceservice.controller;

import com.example.resourceservice.exception.*;
import com.example.resourceservice.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

import static com.example.resourceservice.service.ResourceService.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({InvalidDataException.class})
    private ResponseEntity<Object> handleInvalidDataException(final InvalidDataException invalidDataException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Objects.nonNull(invalidDataException.getErrorResponse()) ? invalidDataException.getErrorResponse() : invalidDataException.getSimpleErrorResponse());
    }

    @ExceptionHandler({StorageException.class})
    private ResponseEntity<Object> handleStorageException(final StorageException storageException) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Objects.nonNull(storageException.getErrorResponse()) ? storageException.getErrorResponse() : storageException.getSimpleErrorResponse());
    }

    @ExceptionHandler({SongClientException.class})
    private ResponseEntity<Object> handleSongClientException(final SongClientException SongClientException) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Objects.nonNull(SongClientException.getErrorResponse()) ? SongClientException.getErrorResponse() : SongClientException.getSimpleErrorResponse());
    }

    @ExceptionHandler({DatabaseException.class})
    private ResponseEntity<Object> handleDatabaseException(final DatabaseException databaseException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Objects.nonNull(databaseException.getErrorResponse()) ? databaseException.getErrorResponse() : databaseException.getSimpleErrorResponse());
    }

    @ExceptionHandler({NumberFormatException.class})
    private ResponseEntity<Object> handleNumberFormatException(final NumberFormatException numberFormatException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(numberFormatException.getMessage());
    }

    @ExceptionHandler({NotFoundException.class})
    private ResponseEntity<Object> handleNotFoundException(final NotFoundException notFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Objects.nonNull(notFoundException.getErrorResponse()) ? notFoundException.getErrorResponse() : notFoundException.getSimpleErrorResponse());
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    private ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(final HttpMediaTypeNotSupportedException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Invalid file format: " + ex.getContentType() + ". Only MP3 files are allowed");
        errorResponse.setErrorCode("400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    private ResponseEntity<Object> handleAssertionError(final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, methodArgumentTypeMismatchException.getValue()));
        errorResponse.setErrorCode("400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({Exception.class})
    private ResponseEntity<ErrorResponse> handleException(final Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(ex.getMessage());
        errorResponse.setErrorCode("500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
