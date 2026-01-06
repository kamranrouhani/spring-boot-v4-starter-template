package com.kamran.template.security.auth.email;

import com.kamran.template.config.EmailConfig;
import com.kamran.template.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailService {

    private final EmailService emailService;
    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    /**
     * Send verification email asynchronously
     *
     * @param to              Recipient email address
     * @param userName        User's name for personalization
     * @param verificationUrl Full verification URL with token
     * @param expiryHours     How many hours until token expires
     */
    @Async("emailTaskExecutor")
    public void sendVerificationEmail(String to, String userName, String verificationUrl, int expiryHours) {
        log.info("Preparing verification email for: {}", to);

        Map<String, Object> variables = Map.of(
                "username", userName,
                "verificationUrl", verificationUrl,
                "expiryHours", expiryHours,
                "subject", EmailTemplate.EMAIL_VERIFICATION.getSubject()
        );

        emailService.sendTemplatedEmail(to, EmailTemplate.EMAIL_VERIFICATION, variables);
    }

    /**
     * Send welcome email after successful verification
     */
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String to, String userName) {

        log.info("Preparing welcome email for: {}", to);

        Map<String, Object> variables = Map.of(
                "username", userName,
                "subject", EmailTemplate.WELCOME.getSubject()
        );

        emailService.sendTemplatedEmail(to, EmailTemplate.WELCOME, variables);
    }

    /**
     * Send password reset email
     */
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String userName, String resetUrl, int expiryHours) {
        log.info("Preparing password reset email for: {}", to);

        Map<String, Object> variables = Map.of(
                "userName", userName,
                "resetUrl", resetUrl,
                "expiryHours", expiryHours,
                "subject", EmailTemplate.PASSWORD_RESET.getSubject()
        );

        emailService.sendTemplatedEmail(to, EmailTemplate.PASSWORD_RESET, variables);
    }


    /**
     * @param to
     * @param userName
     * @param changeDate Send Password successfully changed email
     */
    @Async("emailTaskExecutor")
    public void sendPasswordChangedEmail(String to, String userName, LocalDateTime changeDate) {
        log.info("Preparing password changed notification for: {}", to);

        Map<String, Object> variables = Map.of(
                "userName", userName,
                "changeDate", changeDate,
                "subject", EmailTemplate.PASSWORD_CHANGED.getSubject()
        );

        emailService.sendTemplatedEmail(to, EmailTemplate.PASSWORD_CHANGED, variables);
    }



    @Async("emailTaskExecutor")
    public void sendMFACodeEmail(String to, String userName, String code) {
        Map<String, Object> variables = Map.of(
                "userName", userName,
                "code", code,
                "expiryMinutes", 10,
                "subject", "Your MFA Code"
        );
        emailService.sendTemplatedEmail(to, EmailTemplate.MFA_CODE, variables);
    }
}
