package com.kamran.template.security.auth.email;

import com.kamran.template.email.EmailService;
import com.kamran.template.email.EmailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthEmailServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthEmailService authEmailService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "John Doe";

    @BeforeEach
    void setUp() {
        // No setup needed for this test - we're testing the facade layer
    }

    // ==================== VERIFICATION EMAIL TESTS ====================

    @Test
    void sendVerificationEmail_ShouldCallCoreEmailServiceWithCorrectParameters() {
        // Arrange
        String verificationUrl = "http://localhost:8080/verify-email?token=abc123";
        int expiryHours = 24;

        // Act
        authEmailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, verificationUrl, expiryHours);

        // Assert
        verify(emailService).sendTemplatedEmail(
            eq(TEST_EMAIL),
            eq(EmailTemplate.EMAIL_VERIFICATION),
            argThat(variables -> {
                return variables.get("username").equals(TEST_USERNAME) &&
                       variables.get("verificationUrl").equals(verificationUrl) &&
                       variables.get("expiryHours").equals(expiryHours) &&
                       variables.get("subject").equals(EmailTemplate.EMAIL_VERIFICATION.getSubject());
            })
        );
    }

    // ==================== WELCOME EMAIL TESTS ====================

    @Test
    void sendWelcomeEmail_ShouldCallCoreEmailServiceWithCorrectParameters() {
        // Act
        authEmailService.sendWelcomeEmail(TEST_EMAIL, TEST_USERNAME);

        // Assert
        verify(emailService).sendTemplatedEmail(
            eq(TEST_EMAIL),
            eq(EmailTemplate.WELCOME),
            argThat(variables -> {
                return variables.get("username").equals(TEST_USERNAME) &&
                       variables.get("subject").equals(EmailTemplate.WELCOME.getSubject());
            })
        );
    }

    // ==================== PASSWORD RESET EMAIL TESTS ====================

    @Test
    void sendPasswordResetEmail_ShouldCallCoreEmailServiceWithCorrectParameters() {
        // Arrange
        String resetUrl = "http://localhost:8080/reset-password?token=reset123";
        int expiryHours = 1;

        // Act
        authEmailService.sendPasswordResetEmail(TEST_EMAIL, TEST_USERNAME, resetUrl, expiryHours);

        // Assert
        verify(emailService).sendTemplatedEmail(
            eq(TEST_EMAIL),
            eq(EmailTemplate.PASSWORD_RESET),
            argThat(variables -> {
                return variables.get("userName").equals(TEST_USERNAME) &&
                       variables.get("resetUrl").equals(resetUrl) &&
                       variables.get("expiryHours").equals(expiryHours) &&
                       variables.get("subject").equals(EmailTemplate.PASSWORD_RESET.getSubject());
            })
        );
    }

    // ==================== PASSWORD CHANGED EMAIL TESTS ====================

    @Test
    void sendPasswordChangedEmail_ShouldCallCoreEmailServiceWithCorrectParameters() {
        // Arrange
        LocalDateTime changeDate = LocalDateTime.now();

        // Act
        authEmailService.sendPasswordChangedEmail(TEST_EMAIL, TEST_USERNAME, changeDate);

        // Assert
        verify(emailService).sendTemplatedEmail(
            eq(TEST_EMAIL),
            eq(EmailTemplate.PASSWORD_CHANGED),
            argThat(variables -> {
                return variables.get("userName").equals(TEST_USERNAME) &&
                       variables.get("changeDate").equals(changeDate) &&
                       variables.get("subject").equals(EmailTemplate.PASSWORD_CHANGED.getSubject());
            })
        );
    }

    // ==================== MFA CODE EMAIL TESTS ====================

    @Test
    void sendMFACodeEmail_ShouldCallCoreEmailServiceWithCorrectParameters() {
        // Arrange
        String mfaCode = "123456";

        // Act
        authEmailService.sendMFACodeEmail(TEST_EMAIL, TEST_USERNAME, mfaCode);

        // Assert
        verify(emailService).sendTemplatedEmail(
            eq(TEST_EMAIL),
            eq(EmailTemplate.MFA_CODE),
            argThat(variables -> {
                return variables.get("userName").equals(TEST_USERNAME) &&
                       variables.get("code").equals(mfaCode) &&
                       variables.get("expiryMinutes").equals(10) &&
                       variables.get("subject").equals("Your MFA Code");
            })
        );
    }



    // ==================== HELPER METHODS ====================

}