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
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QueueController {

    private final QueueService queueService;
    private final SimpMessagingTemplate messagingTemplate;

    public QueueController(QueueService queueService, SimpMessagingTemplate messagingTemplate) {
        this.queueService = queueService;
        this.messagingTemplate = messagingTemplate;
    }

    // 🔥 Broadcast queue update
    private void broadcastQueueUpdate(Long orgId) {
        Map<String, Object> status = queueService.getQueueStatus(orgId);
        messagingTemplate.convertAndSend("/topic/queue/" + orgId, Optional.ofNullable(status));
    }

    // ================= ORG =================

    @PostMapping("/org")
    public ResponseEntity<Organisation> createOrg(@RequestBody Organisation org) {
        return ResponseEntity.ok(queueService.createOrganisation(org));
    }

    // 🔐 GET ORG (FIXED - with proper security)
    @GetMapping("/org/{orgId}")
    public ResponseEntity<?> getOrg(@PathVariable Long orgId, Authentication authentication) {

        String email = authentication.getName(); // from JWT
        User user = queueService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        // ✅ SUPER ADMIN → access any org
        if (user.getRole().name().equals("SUPER_ADMIN")) {
            return ResponseEntity.ok(queueService.getOrganisation(orgId));
        }

        // ✅ ORG ADMIN → only own org
        if (user.getRole().name().equals("ORG_ADMIN") &&
                user.getOrganisation() != null &&
                user.getOrganisation().getId().equals(orgId)) {

            return ResponseEntity.ok(user.getOrganisation());
        }

        return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
    }

    // ================= JOIN QUEUE =================

    @PostMapping("/token/{orgId}")
    public ResponseEntity<?> joinQueue(@PathVariable Long orgId, @RequestBody User user) {
        Token token = queueService.joinQueue(orgId, user);
        broadcastQueueUpdate(orgId);
        return ResponseEntity.ok(token);
    }

    // ================= CALL NEXT =================

    @PutMapping("/token/next/{orgId}")
    public ResponseEntity<?> callNext(@PathVariable Long orgId, Authentication authentication) {

        String email = authentication.getName();
        User user = queueService.getUserByEmail(email);

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

    // ================= SKIP NEXT =================

    @PutMapping("/token/skip/{orgId}")
    public ResponseEntity<?> skipNext(@PathVariable Long orgId, Authentication authentication) {

        String email = authentication.getName();
        User user = queueService.getUserByEmail(email);

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

    // ================= GET STATUS =================

    @GetMapping("/queue/{orgId}")
    public ResponseEntity<?> getStatus(@PathVariable Long orgId, Authentication authentication) {

        String email = authentication.getName();
        User user = queueService.getUserByEmail(email);

        if (!isAuthorizedForOrg(user, orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access Denied"));
        }

        return ResponseEntity.ok(queueService.getQueueStatus(orgId));
    }

    // ================= COMMON AUTH CHECK =================

    private boolean isAuthorizedForOrg(User user, Long orgId) {
        if (user == null) return false;

        // SUPER ADMIN → always allowed
        if (user.getRole().name().equals("SUPER_ADMIN")) return true;

        // ORG ADMIN → only own org
        return user.getOrganisation() != null &&
                user.getOrganisation().getId().equals(orgId);
    }
}