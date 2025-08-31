package com.example.resourceservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataPreparerServiceTest {

    @Test
    void prepareErrorResponse() {
        DataPreparerService service = new DataPreparerService();
        String message = "Not Found";
        String code = "404";
        var result = service.prepareErrorResponse(message, code);

        assertNotNull(result);
        assertEquals(message, result.getErrorMessage());
        assertEquals(code, result.getErrorCode());
    }
}