package com.kamran.template.security.auth.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {

    EMAIL_VERIFICATION(
            "Verify Your Email Address",
            "email-verification.html",
            "Verify your email to activate your account"
    ),

    PASSWORD_RESET(
            "Reset Your Password",
            "password-reset.html",
            "Reset your password to regain access"
    ),

    WELCOME(
            "Welcome to Template App!",
            "welcome.html",
            "Get started with your new account"
    ),

    PASSWORD_CHANGED(
            "Password Changed Successfully",
            "password-changed.html",
            "Your password has been updated"
    ),

    MFA_CODE(
            "Your Security Code",
            "mfa-code.html",
            "Verify your identity."
    );

    /**
     * Email subject line
     */
    private final String subject;

    /**
     * HTML template filename (in resources/templates/email/)
     */
    private final String templateName;

    /**
     * Email preview text (shows in inbox)
     */
    private final String previewText;
}
