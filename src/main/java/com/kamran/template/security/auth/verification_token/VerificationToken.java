package com.kamran.template.security.auth.verification_token;

import com.kamran.template.common.BaseEntity;
import com.kamran.template.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table(name = "verification_tokens")
@Entity
public class VerificationToken extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime verifiedAt;
}
