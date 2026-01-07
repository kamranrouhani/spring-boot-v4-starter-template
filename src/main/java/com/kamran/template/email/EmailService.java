package com.kamran.template.email;

import com.kamran.template.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    /**
     * Generic method to send templated email
     *
     * @param to        Recipient email
     * @param template  Email template enum
     * @param variables Template variables (userName, links, etc.)
     */
    public void sendTemplatedEmail(String to, EmailTemplate template, Map<String, Object> variables) {
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
    public String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        return templateEngine.process("email/" + templateName, context);
    }

    /**
     * Send HTML email using JavaMailSender
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailConfig.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
