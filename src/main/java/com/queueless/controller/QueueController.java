package com.queueless.controller;

import com.queueless.model.Organisation;
import com.queueless.model.Token;
import com.queueless.model.User;
import com.queueless.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueueController {

    private final QueueService queueService;
    private final SimpMessagingTemplate messagingTemplate;

    public QueueController(QueueService queueService, SimpMessagingTemplate messagingTemplate) {
        this.queueService = queueService;
        this.messagingTemplate = messagingTemplate;
    }

    // ============================================================
    // INTERNAL: Broadcast queue update over WebSocket
    // ============================================================
    private void broadcastQueueUpdate(Long orgId) {
        Map<String, Object> status = queueService.getQueueStatus(orgId);
        // FIX: Send the map directly — do NOT wrap in Optional (breaks JSON serialization)
        messagingTemplate.convertAndSend("/topic/queue/" + orgId, status);
    }

    // ============================================================
    // PUBLIC: Org info for the join-queue screen (no auth needed)
    // ============================================================
    @GetMapping("/queue/info/{orgId}")
    public ResponseEntity<?> getPublicOrgInfo(@PathVariable Long orgId) {
        Organisation org = queueService.getOrganisation(orgId);
        return ResponseEntity.ok(Map.of(
                "id",     org.getId(),
                "name",   org.getName(),
                "type",   org.getType(),
                "prefix", org.getPrefix()
        ));
    }

    // ============================================================
    // ORG: Create organisation (super-admin / org-admin guard via SecurityConfig)
    // ============================================================
    @PostMapping("/org")
    public ResponseEntity<Organisation> createOrg(@RequestBody Organisation org) {
        return ResponseEntity.ok(queueService.createOrganisation(org));
    }

    // ============================================================
    // ORG: Get organisation details (admin only)
    // ============================================================
    @GetMapping("/org/{orgId}")
    public ResponseEntity<?> getOrg(@PathVariable Long orgId, Authentication authentication) {
        String email = authentication.getName();
        User user = queueService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        // SUPER_ADMIN can access any org
        if (user.getRole().name().equals("SUPER_ADMIN")) {
            return ResponseEntity.ok(queueService.getOrganisation(orgId));
        }

        // ORG_ADMIN can only access their own org
        if (user.getRole().name().equals("ORG_ADMIN") &&
                user.getOrganisation() != null &&
                user.getOrganisation().getId().equals(orgId)) {
            return ResponseEntity.ok(user.getOrganisation());
        }

        return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
    }

    // ============================================================
    // QUEUE: Join (public, no auth needed)
    // ============================================================
    @PostMapping("/token/{orgId}")
    public ResponseEntity<?> joinQueue(@PathVariable Long orgId, @RequestBody User user) {
        Token token = queueService.joinQueue(orgId, user);
        broadcastQueueUpdate(orgId);
        return ResponseEntity.ok(token);
    }

    // ============================================================
    // QUEUE: Call next (authenticated + org check)
    // ============================================================
    @PutMapping("/token/next/{orgId}")
    public ResponseEntity<?> callNext(@PathVariable Long orgId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        if (!isAuthorizedForOrg(user, orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
        }

        try {
            Token token = queueService.callNextToken(orgId);
            broadcastQueueUpdate(orgId);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // QUEUE: Skip next (authenticated + org check)
    // ============================================================
    @PutMapping("/token/skip/{orgId}")
    public ResponseEntity<?> skipNext(@PathVariable Long orgId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        if (!isAuthorizedForOrg(user, orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
        }

        try {
            Token token = queueService.skipNextToken(orgId);
            broadcastQueueUpdate(orgId);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // QUEUE: Get live status (authenticated + org check)
    // ============================================================
    @GetMapping("/queue/{orgId}")
    public ResponseEntity<?> getStatus(@PathVariable Long orgId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        if (!isAuthorizedForOrg(user, orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
        }

        return ResponseEntity.ok(queueService.getQueueStatus(orgId));
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) return null;
        return queueService.getUserByEmail(authentication.getName());
    }

    private boolean isAuthorizedForOrg(User user, Long orgId) {
        if (user == null) return false;
        if (user.getRole().name().equals("SUPER_ADMIN")) return true;
        return user.getOrganisation() != null &&
                user.getOrganisation().getId().equals(orgId);
    }
}