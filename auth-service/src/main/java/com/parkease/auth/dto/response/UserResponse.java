package com.parkease.auth.dto.response;

import com.parkease.auth.entity.AuthProvider;
import com.parkease.auth.entity.Role;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private AuthProvider provider;
    private boolean active;
    private String profilePicUrl;
    private LocalDateTime createdAt;
}
