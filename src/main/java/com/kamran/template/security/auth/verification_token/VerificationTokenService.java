package com.kamran.template.security.auth.verification_token;

import com.kamran.template.common.exception.InvalidTokenException;
import com.kamran.template.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;

    @Value("${app.verification.token-validity-hours:24}")
    private int tokenValidityHours;

    /**
     * Create a new verification token for a user
     *
     * @param user User to create token for
     * @param type Type of token (EMAIL_VERIFICATION or PASSWORD_RESET)
     * @return Generated token string
     */
    @Transactional
    public String createVerificationToken(User user, TokenType type){
        log.debug("Creating {} token for user: {}", type, user.getEmail());

        verificationTokenRepository.deleteByUserAndType(user, type);

        String tokenString = UUID.randomUUID().toString();

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenValidityHours);

        VerificationToken token = VerificationToken.builder()
                .token(tokenString)
                .user(user)
                .type(type)
                .expiresAt(expiresAt)
                .build();

        verificationTokenRepository.save(token);

        log.info("Created {} token for user: {} which expires at: {}", type, user.getEmail(), expiresAt);

        return tokenString;
    }


    /**
     * Validate and retrieve a verification token
     *
     * @param tokenString Token string to validate
     * @return VerificationToken if valid
     * @throws InvalidTokenException if token is invalid, expired, or already used
     */
    @Transactional(readOnly = true)
    public VerificationToken validateToken(String tokenString) {
        log.debug("Validating token: {}", tokenString);

        VerificationToken token = verificationTokenRepository.findByToken(tokenString)
                .orElseThrow(()-> {
                    log.warn("Token not found: {}", tokenString);
                    return new InvalidTokenException("invalid verification token");
                });

        if (token.isVerified()) {
            log.warn("Token already used: {}", tokenString);
            throw new InvalidTokenException("This verification link has already been used");
        }

        if (token.isExpired()) {
            log.warn("Token expired: {} (expired at: {})", tokenString, token.getExpiresAt());
            throw new InvalidTokenException("This verification link has expired. Please request a new one");
        }

        log.debug("Token validated successfully: {}", tokenString);
        return token;
    }


    /**
     * Mark token as verified
     *
     * @param token Token to mark as verified
     */
    @Transactional
    public void markTokenAsVerified(VerificationToken token) {
        token.setVerifiedAt(LocalDateTime.now());
        verificationTokenRepository.save(token);

        log.info("Token marked as verified for user: {}", token.getUser().getEmail());
    }

    /**
     * Get token validity hours for email template
     */
    public int getTokenValidityHours() {
        return tokenValidityHours;
    }

    /**
     * Clean up expired tokens (scheduled job can call this)
     */
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Cleaning up expired verification tokens");
        verificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
