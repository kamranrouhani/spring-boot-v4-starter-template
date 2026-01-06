package com.kamran.template.security.auth.mfa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MFARequiredResponse {
    private String message;
    private boolean mfaRequired;
}