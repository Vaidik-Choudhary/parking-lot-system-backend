package com.parkease.auth.dto.request;

import com.parkease.auth.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
	
	@Pattern(
		    regexp = "^[A-Za-z\\s]{2,50}$",
		    message = "Full name must contain only letters and spaces"
		)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

//    @Pattern(
//            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
//            message = "Password must be at least 8 characters and include uppercase, lowercase, and a number"
//        )
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Role role = Role.DRIVER;
}
