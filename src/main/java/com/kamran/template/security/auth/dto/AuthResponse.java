package com.kamran.template.security.auth.dto;

import com.kamran.template.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private String tokenType;  // "Bearer"
    private Date expiresIn;    // Seconds until expiration
}
