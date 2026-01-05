package com.kamran.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.email")
public class EmailConfig {
    /**
     * Email address to send from (e.g., noreply@template.com)
     */
    private String from;

    /**
     * Base URL for verification links (e.g., http://localhost:8080/api/auth/verify-email)
     */
    private String verificationUrl;
}
