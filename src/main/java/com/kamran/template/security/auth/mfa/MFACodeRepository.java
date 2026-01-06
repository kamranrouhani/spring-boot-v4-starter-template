package com.kamran.template.security.auth.mfa;

import com.kamran.template.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MFACodeRepository extends JpaRepository<MFACode, Long> {
    Optional<MFACode> findByCodeAndUser(String code, User user);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(LocalDateTime now);
}