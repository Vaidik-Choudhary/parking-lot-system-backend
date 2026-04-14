package com.parkease.notification.security;

import com.parkease.notification.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component 
@RequiredArgsConstructor 
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String jwt = header.substring(7);
                if (jwtUtil.validate(jwt)) {
                    String email = jwtUtil.getEmail(jwt);
                    String role  = jwtUtil.getRole(jwt);
                    String auth  = (role != null && !role.startsWith("ROLE_"))
                            ? "ROLE_" + role : (role != null ? role : "ROLE_DRIVER");
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(email, null,
                            List.of(new SimpleGrantedAuthority(auth))));
                    log.debug("Authenticated: {} [{}]", email, auth);
                }
            }
        } catch (Exception e) { log.error("JWT filter error: {}", e.getMessage()); }
        chain.doFilter(req, res);
    }
}
