package com.kamran.template.security.auth.mfa;

import com.kamran.template.security.auth.email.EmailService;
import com.kamran.template.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MFAService {
    private final MFACodeRepository mfaCodeRepository;
    private final EmailService emailService;

    public String generateAndSendCode(User user) {
        // Delete old codes
        mfaCodeRepository.deleteByUser(user);

        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Save with 10-min expiry
        MFACode mfaCode = MFACode.builder()
                .code(code)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        mfaCodeRepository.save(mfaCode);

        // Send email
        emailService.sendMFACodeEmail(user.getEmail(), user.getFirstName(), code);

        return code;
    }

    public boolean verifyCode(User user, String code) {
        return mfaCodeRepository.findByCodeAndUser(code, user)
                .filter(c -> !c.isExpired() && !c.isVerified())
                .map(c -> {
                    c.setVerifiedAt(LocalDateTime.now());
                    mfaCodeRepository.save(c);
                    return true;
                })
                .orElse(false);
    }
}