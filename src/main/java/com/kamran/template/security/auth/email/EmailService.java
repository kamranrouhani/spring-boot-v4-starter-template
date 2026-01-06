package com.kamran.template.security.auth.email;

import com.kamran.template.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
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
public class EmailService {

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

        sendTemplatedEmail(to, EmailTemplate.EMAIL_VERIFICATION, variables);
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

        sendTemplatedEmail(to, EmailTemplate.WELCOME, variables);
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

        sendTemplatedEmail(to, EmailTemplate.PASSWORD_RESET, variables);
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

        sendTemplatedEmail(to, EmailTemplate.PASSWORD_CHANGED, variables);
    }

    /**
     * Generic method to send templated email
     *
     * @param to        Recipient email
     * @param template  Email template enum
     * @param variables Template variables (userName, links, etc.)
     */
    private void sendTemplatedEmail(String to, EmailTemplate template, Map<String, Object> variables) {
        try {
            String htmlContent = processTemplate(template.getTemplateName(), variables);

            sendHtmlEmail(to, template.getSubject(), htmlContent);

            log.info("Email '{}' sent successfull to : {}", template.name(), to);
        } catch (MessagingException exception) {
            log.error("Failed to send email '{}' to: {}", template.name(), to, exception);
        }
    }

    /**
     * Process Thymeleaf template with variables
     */
    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        return templateEngine.process("email/" + templateName, context);
    }

    /**
     * Send HTML email using JavaMailSender
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailConfig.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    @Async("emailTaskExecutor")
    public void sendMFACodeEmail(String to, String userName, String code) {
        Map<String, Object> variables = Map.of(
                "userName", userName,
                "code", code,
                "expiryMinutes", 10,
                "subject", "Your MFA Code"
        );
        sendTemplatedEmail(to, EmailTemplate.MFA_CODE, variables);
    }
}
