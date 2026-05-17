package com.parkease.parkinglot.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "testSecretKeyThatIsLongEnoughForHS256Algorithm12345";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
    }

    private String token(String email, String role, long ms) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder().setSubject(email).claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ms))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    @Test void getEmailFromToken_returnsEmail() {
        assertEquals("u@t.com", jwtUtil.getEmailFromToken(token("u@t.com", "DRIVER", 3600000)));
    }
    @Test void getRoleFromToken_returnsRole() {
        assertEquals("LOT_MANAGER", jwtUtil.getRoleFromToken(token("u@t.com", "LOT_MANAGER", 3600000)));
    }
    @Test void validateToken_valid_returnsTrue() {
        assertTrue(jwtUtil.validateToken(token("u@t.com", "DRIVER", 3600000)));
    }
    @Test void validateToken_expired_returnsFalse() {
        assertFalse(jwtUtil.validateToken(token("u@t.com", "DRIVER", -1000)));
    }
    @Test void validateToken_malformed_returnsFalse() {
        assertFalse(jwtUtil.validateToken("bad.token.here"));
    }
    @Test void validateToken_empty_returnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }
}
