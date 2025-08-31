package com.example.songservice.controller;

import com.example.songservice.exception.*;
import com.example.songservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<Object> invokePrivateMethod(String methodName, Class<?> paramType, Object param) throws Exception {
        Method method = GlobalExceptionHandler.class.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true); // allow access to private methods
        return (ResponseEntity<Object>) method.invoke(handler, param);
    }

    @Test
    void testHandleNotFoundException_returnsNotFound() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Not found");
        errorResponse.setErrorCode("404");
        NotFoundException ex = new NotFoundException(errorResponse);

        ResponseEntity<Object> response = invokePrivateMethod("handleNotFoundException", NotFoundException.class, ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleNumberFormatException_returnsBadRequest() throws Exception {
        NumberFormatException ex = new NumberFormatException("Invalid number");
        ResponseEntity<Object> response = invokePrivateMethod("handleNumberFormatException", NumberFormatException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid number", response.getBody());
    }

    @Test
    void testHandleIllegalArgumentException_returnsBadRequest() throws Exception {
        IllegalArgumentException ex = new IllegalArgumentException("Illegal argument");
        ResponseEntity<Object> response = invokePrivateMethod("handleIllegalArgumentException", IllegalArgumentException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Illegal argument", response.getBody());
    }

    @Test
    void testHandleInvalidDataException_returnsBadRequest() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Invalid data");
        errorResponse.setErrorCode("400");
        InvalidDataException ex = new InvalidDataException(errorResponse);

        ResponseEntity<Object> response = invokePrivateMethod("handleInvalidDataException", InvalidDataException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleAssertionError_returnsBadRequest() throws Exception {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<Object> response = invokePrivateMethod("handleAssertionError", MethodArgumentTypeMismatchException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertTrue(error.getErrorMessage().contains("abc"));
        assertEquals("400", error.getErrorCode());
    }

    @Test
    void testHandleConflictDataException_returnsConflict() throws Exception {
        ConflictDataException ex = mock(ConflictDataException.class);
        ErrorResponse errorResponse = new ErrorResponse();
        when(ex.getSimpleErrorResponse()).thenReturn(errorResponse);

        ResponseEntity<Object> response = invokePrivateMethod("handleConflictDataException", ConflictDataException.class, ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleException_returnsInternalServerError() throws Exception {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<Object> response = invokePrivateMethod("handleException", Exception.class, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Unexpected error", body.getErrorMessage());
        assertEquals("500", body.getErrorCode());
    }
}
