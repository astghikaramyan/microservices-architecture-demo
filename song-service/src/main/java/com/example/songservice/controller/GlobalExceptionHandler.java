package com.example.songservice.controller;

import com.example.songservice.exception.ConflictDataException;
import com.example.songservice.exception.InvalidDataException;
import com.example.songservice.exception.NotFoundException;
import com.example.songservice.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

import static com.example.songservice.service.SongService.BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({NotFoundException.class})
    private ResponseEntity<Object> handleNotFoundException(final NotFoundException notFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Objects.nonNull(notFoundException.getErrorResponse()) ? notFoundException.getErrorResponse() : notFoundException.getSimpleErrorResponse());
    }

    @ExceptionHandler({NumberFormatException.class})
    private ResponseEntity<Object> handleNumberFormatException(final NumberFormatException numberFormatException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(numberFormatException.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    private ResponseEntity<Object> handleIllegalArgumentException(final IllegalArgumentException illegalArgumentException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(illegalArgumentException.getMessage());
    }

    @ExceptionHandler({InvalidDataException.class})
    private ResponseEntity<Object> handleInvalidDataException(final InvalidDataException invalidDataException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Objects.nonNull(invalidDataException.getErrorResponse()) ? invalidDataException.getErrorResponse() : invalidDataException.getSimpleErrorResponse());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    private ResponseEntity<Object> handleAssertionError(final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(String.format(BAD_REQUEST_NOT_NUMBER_ERROR_MESSAGE, methodArgumentTypeMismatchException.getValue()));
        errorResponse.setErrorCode("400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({ConflictDataException.class})
    private ResponseEntity<Object> handleConflictDataException(final ConflictDataException conflictDataException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictDataException.getSimpleErrorResponse());
    }

    @ExceptionHandler({Exception.class})
    private ResponseEntity<ErrorResponse> handleException(final Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage(ex.getMessage());
        errorResponse.setErrorCode("500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
