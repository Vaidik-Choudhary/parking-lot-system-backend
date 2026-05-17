package com.parkease.parkingspot.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkease.parkingspot.dto.response.ApiResponse;
import com.parkease.parkingspot.exception.ResourceNotFoundException;
import com.parkease.parkingspot.exception.SpotNotAvailableException;
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
        ResourceNotFoundException ex = new ResourceNotFoundException("Spot not found");
        ApiResponse resp = handler.handleNotFound(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Spot not found", resp.getMessage());
    }
    @Test
    void handleSpotNotAvailable_returnsFailResponse() {
        SpotNotAvailableException ex = new SpotNotAvailableException("Spot is occupied");
        ApiResponse resp = handler.handleSpotNotAvailable(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Spot is occupied", resp.getMessage());
    }
    @Test
    void handleBadArg_returnsFailResponse() {
        ApiResponse resp = handler.handleBadArg(new IllegalArgumentException("Duplicate spot"));
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
