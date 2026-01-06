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
import com.kamran.template.user.User;
import com.kamran.template.user.UserDto;
import com.kamran.template.user.UserRepository;
import com.kamran.template.user.Role;
import com.kamran.template.user.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private AuthMessages authMessages;

    @Mock
    private MFAService mfaService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Setup test data
        testUser = User.builder()
            .email("test@example.com")
            .password("hashedPassword")
            .firstName("John")
            .lastName("Doe")
            .emailVerified(true)
            .mfaEnabled(false)
            .role(Role.USER)
            .subscriptionTier(SubscriptionTier.FREE)
            .build();
        testUser.setId(1L);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Set the @Value field using reflection
        var field = AuthService.class.getDeclaredField("verificationBaseUrl");
        field.setAccessible(true);
        field.set(authService, "http://localhost:8080/verify-email");
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    void register_ShouldCreateUserAndSendVerificationEmail_WhenEmailIsUnique() {
        // Arrange
        User savedUser = User.builder()
            .email(registerRequest.getEmail())
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .emailVerified(false)
            .enabled(true)
            .accountLocked(false)
            .role(Role.USER)
            .subscriptionTier(SubscriptionTier.FREE)
            .build();
        savedUser.setId(1L);

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(verificationTokenService.createVerificationToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION)))
            .thenReturn("verification-token-123");
        when(verificationTokenService.getTokenValidityHours()).thenReturn(24);

        // Act
        RegisterResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getMessage()).isEqualTo("Registration successful. Please check your email to verify your account.");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(verificationTokenService).createVerificationToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION));
        verify(emailService).sendVerificationEmail(
            eq(registerRequest.getEmail()),
            eq(registerRequest.getFirstName()),
            anyString(),
            eq(24)
        );
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining(registerRequest.getEmail());

        verify(userRepository, never()).save(any(User.class));
        verify(verificationTokenService, never()).createVerificationToken(any(), any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any(), anyInt());
    }

    // ==================== EMAIL VERIFICATION TESTS ====================

    @Test
    void verifyEmail_ShouldVerifyUserAndSendWelcomeEmail_WhenTokenIsValid() {
        // Arrange
        VerificationToken mockToken = VerificationToken.builder()
            .user(testUser)
            .token("valid-token")
            .type(TokenType.EMAIL_VERIFICATION)
            .build();

        when(verificationTokenService.validateToken("valid-token")).thenReturn(mockToken);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        String result = authService.verifyEmail("valid-token");

        // Assert
        assertThat(result).isEqualTo("Email verified successfully! You can now log in.");
        assertThat(testUser.getEmailVerified()).isTrue();
        verify(verificationTokenService).validateToken("valid-token");
        verify(userRepository).save(testUser);
        verify(verificationTokenService).markTokenAsVerified(mockToken);
        verify(emailService).sendWelcomeEmail(testUser.getEmail(), testUser.getFirstName());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsValidAndNoMFA() {
        // Arrange
        Authentication mockAuth = mock(Authentication.class);
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(mockUserDetails.getUsername()).thenReturn(testUser.getEmail());
        when(jwtUtil.generateToken(testUser.getEmail())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationTime("jwt-token")).thenReturn(new Date());

        // Act
        Object result = authService.login(loginRequest);

        // Assert
        assertThat(result).isInstanceOf(AuthResponse.class);
        AuthResponse authResponse = (AuthResponse) result;
        assertThat(authResponse.getAccessToken()).isEqualTo("jwt-token");
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(testUser.getEmail());
    }

    @Test
    void login_ShouldReturnMFARequiredResponse_WhenMFAIsEnabled() {
        // Arrange
        User userWithMFA = User.builder()
            .email("test@example.com")
            .password("hashedPassword")
            .firstName("John")
            .lastName("Doe")
            .emailVerified(true)
            .mfaEnabled(true)  // MFA enabled
            .build();
        userWithMFA.setId(1L);

        Authentication mockAuth = mock(Authentication.class);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(userWithMFA));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(mfaService.generateAndSendCode(userWithMFA)).thenReturn("123456");

        // Act
        Object result = authService.login(loginRequest);

        // Assert
        assertThat(result).isInstanceOf(MFARequiredResponse.class);
        MFARequiredResponse mfaResponse = (MFARequiredResponse) result;
        assertThat(mfaResponse.isMfaRequired()).isTrue();
        assertThat(mfaResponse.getMessage()).isEqualTo("MFA code sent to your email");

        verify(mfaService).generateAndSendCode(userWithMFA);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void login_ShouldThrowBadCredentialsException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid credentials");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldThrowEmailNotVerifiedException_WhenEmailNotVerified() {
        // Arrange
        User unverifiedUser = User.builder()
            .email("test@example.com")
            .password("hashedPassword")
            .firstName("John")
            .lastName("Doe")
            .emailVerified(false)  // Not verified
            .build();
        unverifiedUser.setId(1L);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(unverifiedUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(EmailNotVerifiedException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldPropagateBadCredentialsException_WhenAuthenticationFails() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid credentials");
    }

    // ==================== MFA VERIFICATION TESTS ====================

    @Test
    void verifyMFA_ShouldReturnAuthResponse_WhenMFAVerificationSucceeds() {
        // Arrange
        String mfaCode = "123456";

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mfaService.verifyCode(testUser, mfaCode)).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getEmail())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationTime("jwt-token")).thenReturn(new Date());

        // Act
        AuthResponse result = authService.verifyMFA(testUser.getEmail(), mfaCode);

        // Assert
        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(mfaService).verifyCode(testUser, mfaCode);
        verify(jwtUtil).generateToken(testUser.getEmail());
    }

    @Test
    void verifyMFA_ShouldThrowBadCredentialsException_WhenMFAVerificationFails() {
        // Arrange
        String invalidCode = "000000";

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mfaService.verifyCode(testUser, invalidCode)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyMFA(testUser.getEmail(), invalidCode))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid or expired MFA code");

        verify(mfaService).verifyCode(testUser, invalidCode);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    // ==================== CURRENT USER TESTS ====================

    @Test
    void getCurrentUser_ShouldReturnUserDto_WhenUserExists() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = authService.getCurrentUser(testUser.getEmail());

        // Assert
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(result.getLastName()).isEqualTo(testUser.getLastName());

        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void getCurrentUser_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getCurrentUser(nonExistentEmail))
            .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
            .hasMessage("User not found: " + nonExistentEmail);
    }

    // ==================== PASSWORD RESET TESTS ====================

    @Test
    void forgotPassword_ShouldSendResetEmail_WhenUserExists() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(verificationTokenService.createVerificationToken(testUser, TokenType.PASSWORD_RESET))
            .thenReturn("reset-token-123");
        when(authMessages.passwordResetLinkSent()).thenReturn("Password reset link sent");

        // Act
        String result = authService.forgotPassword(testUser.getEmail());

        // Assert
        assertThat(result).isEqualTo("Password reset link sent");

        verify(verificationTokenService).createVerificationToken(testUser, TokenType.PASSWORD_RESET);
        verify(emailService).sendPasswordResetEmail(
            eq(testUser.getEmail()),
            eq(testUser.getFirstName()),
            anyString(),
            eq(1)
        );
    }

    @Test
    void forgotPassword_ShouldReturnSuccessMessage_WhenUserDoesNotExist() {
        // Arrange - User doesn't exist
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
        when(authMessages.passwordResetLinkSent()).thenReturn("Password reset link sent");

        // Act
        String result = authService.forgotPassword(nonExistentEmail);

        // Assert - Should still return success message (security by obscurity)
        assertThat(result).isEqualTo("Password reset link sent");

        verify(verificationTokenService, never()).createVerificationToken(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    @Test
    void resetPassword_ShouldUpdatePasswordAndSendConfirmation_WhenTokenIsValid() {
        // Arrange
        String newPassword = "newSecurePassword123";
        String hashedPassword = "hashedNewPassword";

        VerificationToken mockToken = VerificationToken.builder()
            .user(testUser)
            .token("valid-reset-token")
            .type(TokenType.PASSWORD_RESET)
            .build();

        when(verificationTokenService.validateToken("valid-reset-token")).thenReturn(mockToken);
        when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authMessages.passwordResetSuccess()).thenReturn("Password reset successful");

        // Act
        String result = authService.resetPassword("valid-reset-token", newPassword);

        // Assert
        assertThat(result).isEqualTo("Password reset successful");
        assertThat(testUser.getPassword()).isEqualTo(hashedPassword);

        verify(verificationTokenService).validateToken("valid-reset-token");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        verify(verificationTokenService).markTokenAsVerified(mockToken);
        verify(emailService).sendPasswordChangedEmail(
            eq(testUser.getEmail()),
            eq(testUser.getFirstName()),
            any(LocalDateTime.class)
        );
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenTypeIsNotPasswordReset() {
        // Arrange
        VerificationToken emailToken = VerificationToken.builder()
            .user(testUser)
            .token("email-token")
            .type(TokenType.EMAIL_VERIFICATION)  // Wrong type
            .build();

        when(verificationTokenService.validateToken("email-token")).thenReturn(emailToken);

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword("email-token", "newPassword"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid token type for password reset");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
