package com.example.resourceservice.controller;

import static com.example.resourceservice.constants.Constants.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;

import com.example.resourceservice.exception.*;
import com.example.resourceservice.model.ErrorResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

@RestControllerAdvice(basePackages = "com.example.resourceservice.controller")
public class GlobalExceptionHandler {
    @ExceptionHandler({InvalidDataException.class})
    private ResponseEntity<Object> handleInvalidDataException(final InvalidDataException invalidDataException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Objects.nonNull(invalidDataException.getErrorResponse()) ? invalidDataException.getErrorResponse() : invalidDataException.getSimpleErrorResponse(), headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({StorageException.class})
    private ResponseEntity<Object> handleStorageException(final StorageException storageException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Objects.nonNull(storageException.getErrorResponse()) ? storageException.getErrorResponse() : storageException.getSimpleErrorResponse(), headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({SongClientException.class})
    private ResponseEntity<Object> handleSongClientException(final SongClientException SongClientException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Objects.nonNull(SongClientException.getErrorResponse()) ? SongClientException.getErrorResponse() : SongClientException.getSimpleErrorResponse(), headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({DatabaseException.class})
    private ResponseEntity<Object> handleDatabaseException(final DatabaseException databaseException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Objects.nonNull(databaseException.getErrorResponse()) ? databaseException.getErrorResponse() : databaseException.getSimpleErrorResponse(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({NumberFormatException.class})
    private ResponseEntity<Object> handleNumberFormatException(final NumberFormatException numberFormatException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(numberFormatException.getMessage(), headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NotFoundException.class})
    private ResponseEntity<Object> handleNotFoundException(final NotFoundException notFoundException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Objects.nonNull(notFoundException.getErrorResponse()) ? notFoundException.getErrorResponse() : notFoundException.getSimpleErrorResponse(), headers, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    private ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(final HttpMediaTypeNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Invalid file format: " + ex.getContentType() + ". Only MP3 files are allowed");
        errorResponse.setErrorCode("400");
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    private ResponseEntity<Object> handleAssertionError(final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, methodArgumentTypeMismatchException.getValue()));
        errorResponse.setErrorCode("400");
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({Exception.class})
    private ResponseEntity<ErrorResponse> handleException(final Exception ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(ex.getMessage());
        errorResponse.setErrorCode("500");
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
