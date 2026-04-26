package com.queueless.controller;

import com.queueless.dto.AuthRequest;
import com.queueless.dto.AuthResponse;
import com.queueless.model.User;
import com.queueless.repository.UserRepository;
import com.queueless.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String jwtToken = jwtService.generateToken(user);

            return ResponseEntity.ok(new AuthResponse(
                    jwtToken,
                    user.getRole().name(),
                    user.getOrganisation() != null ? user.getOrganisation().getId() : null,
                    user.getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}