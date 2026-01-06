package com.kamran.template.security.auth;

import com.kamran.template.common.exception.EmailAlreadyExistsException;
import com.kamran.template.common.exception.EmailNotVerifiedException;
import com.kamran.template.common.exception.InvalidTokenException;
import com.kamran.template.security.auth.dto.AuthResponse;
import com.kamran.template.security.auth.dto.LoginRequest;
import com.kamran.template.security.auth.dto.RegisterRequest;
import com.kamran.template.security.auth.dto.RegisterResponse;
import com.kamran.template.security.auth.email.EmailService;
import com.kamran.template.security.auth.mfa.MFARequiredResponse;
import com.kamran.template.security.auth.mfa.MFAService;
import com.kamran.template.security.auth.verification_token.TokenType;
import com.kamran.template.security.auth.verification_token.VerificationToken;
import com.kamran.template.security.auth.verification_token.VerificationTokenService;
import com.kamran.template.security.jwt.JwtUtil;
import com.kamran.template.user.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;
    private final AuthMessages authMessages;
    private final MFAService mfaService;

    @Value("${app.email.verification-url}")
    private String verificationBaseUrl;

    /**
     * Register a new user and send verification email
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email " + request.getEmail() + " is already registered");
        }

        // Create new user with hashed password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .subscriptionTier(SubscriptionTier.FREE)
                .emailVerified(false)  // Not verified yet
                .enabled(true)
                .accountLocked(false)
                .build();

        // Save user to DB
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate verification token
        String token = verificationTokenService.createVerificationToken(
                savedUser,
                TokenType.EMAIL_VERIFICATION
        );

        // Build verification URL
        String verificationUrl = verificationBaseUrl + "?token=" + token;

        // Send verification email (async)
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                verificationUrl,
                verificationTokenService.getTokenValidityHours()
        );

        log.info("Verification email sent to: {}", savedUser.getEmail());

        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    /**
     * Verify email with token
     */
    @Transactional
    public String verifyEmail(String tokenString) {
        log.info("Attempting to verify email with token");

        // Validate token (throws InvalidTokenException if invalid)
        VerificationToken token = verificationTokenService.validateToken(tokenString);

        // Get user from token
        User user = token.getUser();

        // Mark user as verified
        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationTokenService.markTokenAsVerified(token);

        log.info("Email verified successfully for user: {}", user.getEmail());

        // Send welcome email (async)
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        return "Email verified successfully! You can now log in.";
    }

    /**
     * Login and generate JWT token
     */
    @Transactional
    public Object login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // First, check if user exists and email is verified
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if email is verified
        if (!user.getEmailVerified()) {
            log.warn("Login failed - email not verified: {}", request.getEmail());
            throw new EmailNotVerifiedException();
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            if (user.getMfaEnabled()) {
                // Generate and send MFA code
                String code = mfaService.generateAndSendCode(user);

                return MFARequiredResponse.builder()
                        .message("MFA code sent to your email")
                        .mfaRequired(true)
                        .build();
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            if (userDetails != null) {
                log.info("User authenticated successfully: {}",
                        Optional.of(userDetails.getUsername()).orElse("unknown"));
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail());

            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getExpirationTime(token))
                    .user(UserDto.fromEntity(user))
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Login failed for email: {} - {}", request.getEmail(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Get current user information by email
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        log.debug("Fetching current user info for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found: {}", email);
                    return new org.springframework.security.core.userdetails.UsernameNotFoundException(
                            "User not found: " + email
                    );
                });

        return UserDto.fromEntity(user);
    }

    /**
     * Initiate password reset - send email with reset link
     */
    @Transactional
    public String forgotPassword(String email) {
        log.info("Password reset requested for: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.warn("Password reset request for non-existing email: {}", email);
            return authMessages.passwordResetLinkSent();
        }

        String token = verificationTokenService.createVerificationToken(
                user,
                TokenType.PASSWORD_RESET
        );

        String resetUrl = verificationBaseUrl.replace("verify-email", "reset-password") + "?token=" + token;

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFirstName(),
                resetUrl,
                1
        );

        log.info("Password reset email sent to: {}", email);

        return authMessages.passwordResetLinkSent();

    }

    /**
     * Reset password with token
     */
    @Transactional
    public String resetPassword(String tokenString, String newPassword) {
        log.info("Attempting password reset with token");

        // Validate token
        VerificationToken token = verificationTokenService.validateToken(tokenString);

        // Check token type
        if (token.getType() != TokenType.PASSWORD_RESET) {
            throw new InvalidTokenException("Invalid token type for password reset");
        }

        // Get user
        User user = token.getUser();

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        verificationTokenService.markTokenAsVerified(token);

        log.info("Password reset successfully for user: {}", user.getEmail());

        // Send confirmation email
        emailService.sendPasswordChangedEmail(
                user.getEmail(),
                user.getFirstName(),
                LocalDateTime.now()
        );

        return authMessages.passwordResetSuccess();
    }

    @Transactional
    public AuthResponse verifyMFA(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!mfaService.verifyCode(user, code)) {
            throw new BadCredentialsException("Invalid or expired MFA code");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime(token))
                .user(UserDto.fromEntity(user))
                .build();
    }
}