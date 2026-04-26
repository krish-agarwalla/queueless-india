package com.queueless.controller;

import com.queueless.repository.OrganisationRepository;
import com.queueless.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final OrganisationRepository orgRepo;
    private final TokenRepository tokenRepo;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        long totalClients      = orgRepo.count();
        long totalTokensEver   = tokenRepo.count();

        return ResponseEntity.ok(Map.of(
                "totalClients",      totalClients,
                "totalTokensIssued", totalTokensEver,
                "systemStatus",      "Operational"
        ));
    }
}