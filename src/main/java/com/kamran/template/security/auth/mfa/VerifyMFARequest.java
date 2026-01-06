package com.kamran.template.security.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyMFARequest {
    @NotBlank
    private String email;
    @NotBlank
    private String code;
}
