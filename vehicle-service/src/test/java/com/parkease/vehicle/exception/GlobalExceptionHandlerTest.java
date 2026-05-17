package com.parkease.vehicle.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.parkease.vehicle.dto.response.ApiResponse;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() { handler = new GlobalExceptionHandler(); }

    @Test
    void handleNotFound_returnsFailResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Vehicle not found");
        ApiResponse resp = handler.handleNotFound(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Vehicle not found", resp.getMessage());
    }
    @Test
    void handleConflict_returnsFailResponse() {
        ApiResponse resp = handler.handleConflict(new IllegalArgumentException("Duplicate plate"));
        assertFalse(resp.isSuccess());
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
