package com.parkease.parkinglot.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkease.parkinglot.dto.response.ApiResponse;
import com.parkease.parkinglot.exception.ResourceNotFoundException;
import com.parkease.parkinglot.exception.UnauthorizedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() { handler = new GlobalExceptionHandler(); }

    @Test
    void handleNotFound_returnsFailResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Lot not found");
        ApiResponse resp = handler.handleNotFound(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Lot not found", resp.getMessage());
    }
    @Test
    void handleUnauthorized_returnsFailResponse() {
        UnauthorizedException ex = new UnauthorizedException("Not your lot");
        ApiResponse resp = handler.handleUnauthorized(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Not your lot", resp.getMessage());
    }
    @Test
    void handleGeneral_returnsFailResponse() {
        ApiResponse resp = handler.handleGeneral(new RuntimeException("Unexpected"));
        assertFalse(resp.isSuccess());
    }
    @Test
    void handleValidation_returnsFieldErrors() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "obj");
        result.addError(new FieldError("obj", "name", "Name is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, result);
        Map<String, String> errors = handler.handleValidation(ex);
        assertTrue(errors.containsKey("name"));
        assertEquals("Name is required", errors.get("name"));
    }
}
