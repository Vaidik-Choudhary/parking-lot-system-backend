package com.parkease.notification.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;

@Component 
@Slf4j
public class JwtUtil {
    @Value("${app.jwt.secret}") private String secret;
    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
    public String getRole(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().get("role", String.class);
    }
    public boolean validate(String token) {
        try { Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); return true; }
        catch (Exception e) { log.warn("JWT invalid: {}", e.getMessage()); return false; }
    }
}
