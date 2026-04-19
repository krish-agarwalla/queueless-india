package com.queueless.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private Long orgId; // So the frontend knows which dashboard to load
    private String name;
}