package com.parkease.auth.dto.response;

import com.parkease.auth.entity.Role;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;      
    private String refreshToken;     
    private String tokenType = "Bearer";

    private Long userId;
    private String fullName;
    private String email;
    private Role role;
}
