package com.parkease.auth.controller;

import com.parkease.auth.dto.response.*;
import com.parkease.auth.entity.*;
import com.parkease.auth.exception.ResourceNotFoundException;
import com.parkease.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.parkease.auth.annotation.TrackExecutionTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") 
public class AdminController {

    private final UserRepository userRepository;
    
    private static final String USER_NOT_FOUND = "User not found with id: ";

    @TrackExecutionTime
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/admin/users");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @TrackExecutionTime
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /api/admin/users/{}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        return ResponseEntity.ok(toResponse(user));
    }

    @TrackExecutionTime
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse> suspendUser(@PathVariable Long id) {
        log.info("PUT /api/admin/users/{}/suspend", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Cannot suspend an admin user."));
        }
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User suspended successfully."));
    }

    @TrackExecutionTime
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse> activateUser(@PathVariable Long id) {
        log.info("PUT /api/admin/users/{}/activate", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        user.setActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User activated successfully."));
    }

    @TrackExecutionTime
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/admin/users/{}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Cannot delete an admin user."));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully."));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable Role role) {
        log.info("GET /api/admin/users/role/{}", role);
        List<UserResponse> users = userRepository.findAllByRole(role)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .active(user.isActive())
                .profilePicUrl(user.getProfilePicUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
