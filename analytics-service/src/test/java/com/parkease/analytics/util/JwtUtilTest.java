package com.parkease.analytics.util;

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
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String token(String email, String role, long ms) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder().setSubject(email).claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ms))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    @Test void getEmail_returnsCorrectEmail() {
        assertEquals("u@t.com", jwtUtil.getEmail(token("u@t.com", "DRIVER", 3600000)));
    }
    @Test void getRole_returnsCorrectRole() {
        assertEquals("DRIVER", jwtUtil.getRole(token("u@t.com", "DRIVER", 3600000)));
    }
    @Test void validate_validToken_returnsTrue() {
        assertTrue(jwtUtil.validate(token("u@t.com", "DRIVER", 3600000)));
    }
    @Test void validate_expiredToken_returnsFalse() {
        assertFalse(jwtUtil.validate(token("u@t.com", "DRIVER", -1000)));
    }
    @Test void validate_malformedToken_returnsFalse() {
        assertFalse(jwtUtil.validate("not.a.token"));
    }
    @Test void validate_emptyToken_returnsFalse() {
        assertFalse(jwtUtil.validate(""));
    }
}
