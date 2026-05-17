package com.parkease.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.parkease.payment.exception.ResourceNotFoundException;
import com.parkease.payment.dto.response.ApiResponse;
import com.parkease.payment.exception.PaymentException;
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
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment not found");
        ApiResponse resp = handler.handleNotFound(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Payment not found", resp.getMessage());
    }
    @Test
    void handlePayment_returnsFailResponse() {
        PaymentException ex = new PaymentException("Already paid");
        ApiResponse resp = handler.handlePayment(ex);
        assertFalse(resp.isSuccess());
        assertEquals("Already paid", resp.getMessage());
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
