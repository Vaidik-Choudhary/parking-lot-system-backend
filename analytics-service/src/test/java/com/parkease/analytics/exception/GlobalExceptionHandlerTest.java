package com.parkease.analytics.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkease.analytics.dto.response.ApiResponse;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() { handler = new GlobalExceptionHandler(); }

    @Test
    void handleBadArg_returnsFailResponse() {
        ApiResponse resp = handler.handleBadArg(new IllegalArgumentException("Invalid lot"));
        assertFalse(resp.isSuccess());
    }
    @Test
    void handleGeneral_returnsFailResponse() {
        ApiResponse resp = handler.handleGeneral(new RuntimeException("Unexpected"));
        assertFalse(resp.isSuccess());
    }
}
