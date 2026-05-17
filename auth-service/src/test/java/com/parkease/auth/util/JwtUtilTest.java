package com.parkease.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "testSecretKeyThatIsLongEnoughForHS256Algorithm12345");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("test@test.com", "DRIVER");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken("test@test.com", "DRIVER");
        assertEquals("test@test.com", jwtUtil.getEmailFromToken(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("test@test.com", "DRIVER");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);
        String token = jwtUtil.generateToken("test@test.com", "DRIVER");
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtUtil.validateToken("this.is.not.a.valid.token"));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void generateToken_differentRoles_shouldProduceDifferentTokens() {
        String driverToken  = jwtUtil.generateToken("test@test.com", "DRIVER");
        String managerToken = jwtUtil.generateToken("test@test.com", "LOT_MANAGER");
        assertNotEquals(driverToken, managerToken);
    }
}
