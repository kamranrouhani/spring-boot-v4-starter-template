package com.kamran.template.security.auth.verification_token;

import com.kamran.template.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find token by token string
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Find latest verification token for a user by type
     */
    Optional<VerificationToken> findFirstByUserAndTypeOrderByCreatedAtDesc(User user, TokenType type);

    /**
     * Delete all expired tokens (cleanup job)
     */
    void deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * Delete all tokens for a user by type
     */
    void deleteByUserAndType(User user, TokenType type);
}
