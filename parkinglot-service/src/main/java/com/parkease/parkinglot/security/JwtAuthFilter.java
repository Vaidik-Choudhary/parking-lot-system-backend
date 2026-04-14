package com.parkease.parkinglot.security;

import com.parkease.parkinglot.util.JwtUtil;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwt(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {

                String email = jwtUtil.getEmailFromToken(jwt);

                String role = jwtUtil.getRoleFromToken(jwt);
                String authority = (role != null && !role.startsWith("ROLE_"))
                        ? "ROLE_" + role
                        : (role != null ? role : "ROLE_DRIVER");

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,                                         
                                null,                                           
                                List.of(new SimpleGrantedAuthority(authority)) 
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with role: {}", email, authority);
            }

        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
