package com.example.storageservice.controller;

import static com.example.storageservice.service.StorageMetadataService.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({InvalidDataException.class})
  private ResponseEntity<Object> handleInvalidDataException(final InvalidDataException invalidDataException) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        Objects.nonNull(invalidDataException.getErrorResponse()) ? invalidDataException.getErrorResponse()
            : invalidDataException.getSimpleErrorResponse());
  }

  // Handle @Valid / JSR-303 validation errors
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, Object> errors = new HashMap<>();
    errors.put("status", HttpStatus.BAD_REQUEST.value());
    errors.put("error", "Validation failed");

    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    errors.put("fieldErrors", fieldErrors);

    return ResponseEntity.badRequest().body(errors);
  }

  // Handle invalid JSON / type mismatches (Jackson errors)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleJsonParseError(HttpMessageNotReadableException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("error", "Malformed JSON request");
    error.put("message", ex.getMostSpecificCause().getMessage()); // shows Jackson cause
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler({DatabaseException.class})
  private ResponseEntity<Object> handleDatabaseException(final DatabaseException databaseException) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        Objects.nonNull(databaseException.getErrorResponse()) ? databaseException.getErrorResponse()
            : databaseException.getSimpleErrorResponse());
  }

  @ExceptionHandler({NumberFormatException.class})
  private ResponseEntity<Object> handleNumberFormatException(final NumberFormatException numberFormatException) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(numberFormatException.getMessage());
  }

  @ExceptionHandler({NotFoundException.class})
  private ResponseEntity<Object> handleNotFoundException(final NotFoundException notFoundException) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        Objects.nonNull(notFoundException.getErrorResponse()) ? notFoundException.getErrorResponse()
            : notFoundException.getSimpleErrorResponse());
  }

  @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
  private ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
      final HttpMediaTypeNotSupportedException ex) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setErrorMessage("Invalid file format: " + ex.getContentType() + ". Only MP3 files are allowed");
    errorResponse.setErrorCode("400");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler({MethodArgumentTypeMismatchException.class})
  private ResponseEntity<Object> handleAssertionError(
      final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setErrorMessage(
        String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, methodArgumentTypeMismatchException.getValue()));
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
