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
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // 1. Verify credentials with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 2. Fetch the user to get their role and org details
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Generate the JWT Token
            String jwtToken = jwtService.generateToken(user);

            // 4. Send back everything the frontend needs
            Long orgId = (user.getOrganisation() != null) ? user.getOrganisation().getId() : null;
            return ResponseEntity.ok(new AuthResponse(jwtToken, user.getRole().name(), orgId, user.getName()));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }
}