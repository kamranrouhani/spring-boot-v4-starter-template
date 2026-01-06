package com.kamran.template.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMessages {

    private final MessageSource messageSource;

    private String get(String key) {
        return messageSource.getMessage(
                key,
                null,
                "??" + key + "??",
                LocaleContextHolder.getLocale()
        );
    }

    public String passwordResetLinkSent() {
        return get("auth.password.reset.sent");
    }

    public String passwordResetSuccess() {
        return get("auth.password.reset.success");
    }
}
