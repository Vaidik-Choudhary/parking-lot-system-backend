package com.parkease.payment.dto.response;

import lombok.*;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    public static ApiResponse ok(String msg)   { return new ApiResponse(true, msg); }
    public static ApiResponse fail(String msg) { return new ApiResponse(false, msg); }
}
