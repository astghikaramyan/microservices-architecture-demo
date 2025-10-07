package com.example.storageservice.controller;

import static com.example.storageservice.service.StorageMetadataService.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.storageservice.exception.DatabaseException;
import com.example.storageservice.exception.InvalidDataException;
import com.example.storageservice.exception.NotFoundException;
import com.example.storageservice.model.error.ErrorResponse;

@RestControllerAdvice(basePackages = "com.example.storageservice.controller")
public class GlobalExceptionHandler {

  @ExceptionHandler({InvalidDataException.class})
  private ResponseEntity<Object> handleInvalidDataException(final InvalidDataException invalidDataException) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(Objects.nonNull(invalidDataException.getErrorResponse()) ? invalidDataException.getErrorResponse() : invalidDataException.getSimpleErrorResponse(), headers, HttpStatus.BAD_REQUEST);
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
