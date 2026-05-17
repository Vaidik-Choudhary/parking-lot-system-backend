package com.parkease.auth.exception;

import com.parkease.auth.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEmailExists_shouldReturnFailResponse() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email exists");
        ApiResponse response = handler.handleEmailExists(ex);
        assertFalse(response.isSuccess());
        assertEquals("Email exists", response.getMessage());
    }

    @Test
    void handleNotFound_shouldReturnFailResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ApiResponse response = handler.handleNotFound(ex);
        assertFalse(response.isSuccess());
        assertEquals("Not found", response.getMessage());
    }

    @Test
    void handleInvalidToken_shouldReturnFailResponse() {
        InvalidTokenException ex = new InvalidTokenException("Invalid token");
        ApiResponse response = handler.handleInvalidToken(ex);
        assertFalse(response.isSuccess());
        assertEquals("Invalid token", response.getMessage());
    }

    @Test
    void handleBadCredentials_shouldReturnFailResponse() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ApiResponse response = handler.handleBadCredentials(ex);
        assertFalse(response.isSuccess());
        assertEquals("Bad credentials", response.getMessage());
    }

    @Test
    void handleValidationErrors_shouldReturnFieldErrorMap() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "email", "Email is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        Map<String, String> errors = handler.handleValidationErrors(ex);

        assertTrue(errors.containsKey("email"));
        assertEquals("Email is required", errors.get("email"));
    }


    @Test
    void handleGeneral_withNormalPath_shouldReturnFailResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        Exception ex = new Exception("Unexpected error");

        ApiResponse response = handler.handleGeneral(request, ex);
        assertFalse(response.isSuccess());
    }
}
