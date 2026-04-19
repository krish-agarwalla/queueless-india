package com.queueless.controller;

import com.queueless.model.Organisation;
import com.queueless.model.Token;
import com.queueless.model.User;
import com.queueless.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        messagingTemplate.convertAndSend("/topic/queue/" + orgId, (Object) status);
    }

    // ================= ORG =================
    @PostMapping("/org")
    public ResponseEntity<Organisation> createOrg(@RequestBody Organisation org) {
        return ResponseEntity.ok(queueService.createOrganisation(org));
    }

    // ================= JOIN QUEUE =================
    @PostMapping("/token/{orgId}")
    public ResponseEntity<Token> joinQueue(@PathVariable Long orgId, @RequestBody User user) {
        Token token = queueService.joinQueue(orgId, user);

        // 🔥 Push update to frontend
        broadcastQueueUpdate(orgId);

        return ResponseEntity.ok(token);
    }

    // ================= CALL NEXT =================
    @PutMapping("/token/next/{orgId}")
    public ResponseEntity<?> callNext(@PathVariable Long orgId) {
        try {
            Token token = queueService.callNextToken(orgId);
            broadcastQueueUpdate(orgId); // Shout the update!
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            // Return a clean 400 Bad Request with the error message
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================= GET STATUS =================
    @GetMapping("/queue/{orgId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long orgId) {
        return ResponseEntity.ok(queueService.getQueueStatus(orgId));
    }

    @GetMapping("/org/{orgId}")
    public ResponseEntity<Organisation> getOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(queueService.getOrganisation(orgId));
    }
    @PutMapping("/token/skip/{orgId}")
    public ResponseEntity<?> skipNext(@PathVariable Long orgId) {
        try {
            Token token = queueService.skipNextToken(orgId);
            broadcastQueueUpdate(orgId); // Instantly update all screens!
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}