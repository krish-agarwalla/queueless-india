package com.queueless.service;

import com.queueless.model.*;
import com.queueless.model.enums.Role;
import com.queueless.model.enums.TokenStatus;
import com.queueless.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;
    private final OrganisationRepository orgRepository;
    private final UserRepository userRepository;

    // ================= USER =================
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // ================= ORG =================
    public Organisation createOrganisation(Organisation org) {
        return orgRepository.save(org);
    }

    public Organisation getOrganisation(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organisation not found"));
    }

    // ================= JOIN QUEUE =================
    public Token joinQueue(Long orgId, User userRequest) {

        if (userRequest.getName() == null || userRequest.getName().isEmpty()) {
            throw new RuntimeException("Name is required");
        }

        Organisation org = getOrganisation(orgId);

        // ✅ Prevent duplicate users
        User customer = userRepository.findByEmail(userRequest.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(userRequest.getName());
                    newUser.setEmail(userRequest.getEmail());
                    newUser.setRole(Role.CUSTOMER);
                    return userRepository.save(newUser);
                });

        customer.setName(userRequest.getName());

        // ✅ Safer token generation
        long totalTokens = tokenRepository.countByOrganisationId(orgId);
        String tokenNumber = org.getPrefix() + (totalTokens + 101);

        Token token = new Token();
        token.setTokenNumber(tokenNumber);
        token.setStatus(TokenStatus.WAITING);
        token.setOrganisation(org);
        token.setUser(customer);
        token.setCreatedAt(LocalDateTime.now());

        return tokenRepository.save(token);
    }

    // ================= CALL NEXT =================
    public Token callNextToken(Long orgId) {

        getOrganisation(orgId); // validation

        Token nextToken = tokenRepository
                .findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING)
                .orElseThrow(() -> new RuntimeException("Queue is empty"));

        nextToken.setStatus(TokenStatus.SERVED);
        return tokenRepository.save(nextToken);
    }

    // ================= SKIP NEXT =================
    public Token skipNextToken(Long orgId) {

        getOrganisation(orgId);

        Token nextToken = tokenRepository
                .findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING)
                .orElseThrow(() -> new RuntimeException("Queue is empty"));

        nextToken.setStatus(TokenStatus.SKIPPED);
        return tokenRepository.save(nextToken);
    }

    // ================= STATUS =================
    public Map<String, Object> getQueueStatus(Long orgId) {

        getOrganisation(orgId);

        long waitingCount = tokenRepository.countByOrganisationIdAndStatus(orgId, TokenStatus.WAITING);

        Token currentlyServing = tokenRepository
                .findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING)
                .orElse(null);

        Map<String, Object> status = new HashMap<>();
        status.put("peopleAhead", waitingCount > 0 ? waitingCount - 1 : 0);
        status.put("nowServing", currentlyServing != null ? currentlyServing.getTokenNumber() : "None");
        status.put("estimatedWaitTimeMins", waitingCount * 5);

        return status;
    }
}