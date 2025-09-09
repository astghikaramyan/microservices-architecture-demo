package com.example.resourceservice.controller;

import com.example.resourceservice.exception.*;
import com.example.resourceservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<Object> invokePrivate(String methodName, Class<?> paramType, Object param) throws Exception {
        Method method = GlobalExceptionHandler.class.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        return (ResponseEntity<Object>) method.invoke(handler, param);
    }

    @Test
    void testHandleInvalidDataException() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Invalid data");
        errorResponse.setErrorCode("400");
        InvalidDataException ex = new InvalidDataException(errorResponse);

        ResponseEntity<Object> response = invokePrivate("handleInvalidDataException", InvalidDataException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleStorageException() throws Exception {
        StorageException ex = mock(StorageException.class);
        ErrorResponse errorResponse = new ErrorResponse();
        when(ex.getSimpleErrorResponse()).thenReturn(errorResponse);

        ResponseEntity<Object> response = invokePrivate("handleStorageException", StorageException.class, ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleSongClientException() throws Exception {
        SongClientException ex = mock(SongClientException.class);
        ErrorResponse errorResponse = new ErrorResponse();
        when(ex.getSimpleErrorResponse()).thenReturn(errorResponse);

        ResponseEntity<Object> response = invokePrivate("handleSongClientException", SongClientException.class, ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleDatabaseException() throws Exception {
        DatabaseException ex = mock(DatabaseException.class);
        ErrorResponse errorResponse = new ErrorResponse();
        when(ex.getSimpleErrorResponse()).thenReturn(errorResponse);

        ResponseEntity<Object> response = invokePrivate("handleDatabaseException", DatabaseException.class, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleNumberFormatException() throws Exception {
        NumberFormatException ex = new NumberFormatException("Invalid number");
        ResponseEntity<Object> response = invokePrivate("handleNumberFormatException", NumberFormatException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid number", response.getBody());
    }

    @Test
    void testHandleNotFoundException() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Not found");
        errorResponse.setErrorCode("404");
        NotFoundException ex = new NotFoundException(errorResponse);

        ResponseEntity<Object> response = invokePrivate("handleNotFoundException", NotFoundException.class, ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorResponse, response.getBody());
    }

    @Test
    void testHandleHttpMediaTypeNotSupportedException() throws Exception {
        HttpMediaTypeNotSupportedException ex1 = mock(HttpMediaTypeNotSupportedException.class);
        when(ex1.getContentType()).thenReturn(MediaType.valueOf("text/plain"));

        ResponseEntity<Object> response = invokePrivate("handleHttpMediaTypeNotSupportedException", HttpMediaTypeNotSupportedException.class, ex1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getErrorMessage().contains("text/plain"));
        assertEquals("400", body.getErrorCode());
    }

    @Test
    void testHandleAssertionError() throws Exception {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<Object> response = invokePrivate("handleAssertionError", MethodArgumentTypeMismatchException.class, ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getErrorMessage().contains("abc"));
        assertEquals("400", body.getErrorCode());
    }

    @Test
    void testHandleException() throws Exception {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<Object> response = invokePrivate("handleException", Exception.class, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Unexpected error", body.getErrorMessage());
        assertEquals("500", body.getErrorCode());
    }
}
