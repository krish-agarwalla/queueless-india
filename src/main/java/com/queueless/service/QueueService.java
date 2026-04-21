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

    public Organisation createOrganisation(Organisation org) {
        return orgRepository.save(org);
    }
    public Organisation getOrganisation(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organisation not found"));
    }
    public Token joinQueue(Long orgId, User userRequest) {
        if (userRequest.getName() == null || userRequest.getName().isEmpty()) {
            throw new RuntimeException("Name is required");
        }
        Organisation org = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organisation not found"));

        // Ensure the public user is saved strictly as a CUSTOMER with no admin privileges
        User customer = new User();
        customer.setName(userRequest.getName());
        customer.setEmail(userRequest.getEmail());
        customer.setRole(Role.CUSTOMER);

        customer = userRepository.save(customer);

        long currentQueueSize = tokenRepository.countByOrganisationIdAndStatus(orgId, TokenStatus.WAITING);
        String tokenNumber = org.getPrefix() + (currentQueueSize + 101);

        Token token = new Token();
        token.setTokenNumber(tokenNumber);
        token.setStatus(TokenStatus.WAITING);
        token.setOrganisation(org);
        token.setUser(customer);
        token.setCreatedAt(LocalDateTime.now());

        return tokenRepository.save(token);
    }

    public Token callNextToken(Long orgId) {
        Token nextToken = tokenRepository.findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING)
                .orElseThrow(() -> new RuntimeException("Queue is empty"));

        nextToken.setStatus(TokenStatus.SERVED);
        return tokenRepository.save(nextToken);
    }
    public Token skipNextToken(Long orgId) {
        Token nextToken = tokenRepository.findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING)
                .orElseThrow(() -> new RuntimeException("Queue is empty"));

        nextToken.setStatus(TokenStatus.SKIPPED);
        return tokenRepository.save(nextToken);
    }
    public Map<String, Object> getQueueStatus(Long orgId) {
        long waitingCount = tokenRepository.countByOrganisationIdAndStatus(orgId, TokenStatus.WAITING);
        Token currentlyServing = tokenRepository.findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(orgId, TokenStatus.WAITING).orElse(null);

        Map<String, Object> status = new HashMap<>();
        status.put("peopleAhead", waitingCount > 0 ? waitingCount - 1 : 0);
        status.put("nowServing", currentlyServing != null ? currentlyServing.getTokenNumber() : "None");
        status.put("estimatedWaitTimeMins", waitingCount * 5); // Mock 5 min avg service time
        return status;
    }
}