package com.parkease.auth.controller;

import com.parkease.auth.dto.request.*;
import com.parkease.auth.dto.response.*;
import com.parkease.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body: { "fullName": "John", "email": "john@example.com", "password": "abc123" }
     * Returns: JWT access token + refresh token + user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Body: { "email": "john@example.com", "password": "abc123" }
     * Returns: JWT access token + refresh token + user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh
     * Body: { "refreshToken": "uuid-string-here" }
     * Returns: new JWT access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Header: Authorization: Bearer <access_token>
     * Returns: success message
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/auth/logout - user: {}", userDetails.getUsername());
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully."));
    }

    /**
     * POST /api/auth/forgot-password
     * Body: { "email": "john@example.com" }
     * Returns: success message (always, even if email doesn't exist — for security)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - email: {}", request.getEmail());
        authService.forgotPassword(request);
        // Always return success to prevent email enumeration attacks
        return ResponseEntity.ok(ApiResponse.ok("If that email is registered, a reset link has been sent."));
    }

    /**
     * POST /api/auth/reset-password
     * Body: { "token": "uuid-from-email", "newPassword": "newPass123" }
     * Returns: success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password");
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. You can now login."));
    }

    /**
     * GET /api/auth/me
     * Header: Authorization: Bearer <access_token>
     * Returns: user profile
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/auth/me - user: {}", userDetails.getUsername());
        UserResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }
}
