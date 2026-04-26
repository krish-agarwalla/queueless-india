package com.queueless.repository;

import com.queueless.model.Token;
import com.queueless.model.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    long countByOrganisationIdAndStatus(Long orgId, TokenStatus status);

    long countByOrganisationId(Long orgId); // ✅ added

    Optional<Token> findFirstByOrganisationIdAndStatusOrderByCreatedAtAsc(
            Long orgId, TokenStatus status
    );
}