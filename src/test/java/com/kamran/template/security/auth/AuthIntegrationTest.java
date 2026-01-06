package com.kamran.template.security.auth;

import com.kamran.template.security.auth.dto.AuthResponse;
import com.kamran.template.security.auth.dto.LoginRequest;
import com.kamran.template.security.auth.dto.RegisterRequest;
import com.kamran.template.security.auth.mfa.MFACode;
import com.kamran.template.security.auth.mfa.MFACodeRepository;
import com.kamran.template.security.auth.mfa.MFARequiredResponse;
import com.kamran.template.security.auth.mfa.VerifyMFARequest;
import com.kamran.template.security.auth.verification_token.TokenType;
import com.kamran.template.security.auth.verification_token.VerificationToken;
import com.kamran.template.security.auth.verification_token.VerificationTokenRepository;
import com.kamran.template.user.User;
import com.kamran.template.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration test for authentication workflows.
 * Tests the complete user journey: registration → email verification → login → MFA → password reset
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private MFACodeRepository mfaCodeRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final String baseUrl = "/api/auth";

    @BeforeEach
    void setUp() {
        // Clean up test data
        mfaCodeRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Initialize MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void basicAuthWorkflow_shouldWork() throws Exception {
        // Test data
        String email = "testuser@example.com";
        String password = "TestPassword123!";
        String firstName = "Test";
        String lastName = "User";

        // === STEP 1: USER REGISTRATION ===
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setFirstName(firstName);
        registerRequest.setLastName(lastName);

        mockMvc.perform(post(baseUrl + "/register")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "%s",
                        "lastName": "%s"
                    }
                    """.formatted(email, password, firstName, lastName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));

        // Verify user was created but not verified
        User registeredUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(registeredUser.getEmail()).isEqualTo(email);
        assertThat(registeredUser.getEmailVerified()).isFalse();

        // Manually create verification token for testing
        String testToken = "test-verification-token-123";
        VerificationToken verificationToken = VerificationToken.builder()
                .token(testToken)
                .user(registeredUser)
                .type(TokenType.EMAIL_VERIFICATION)
                .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(verificationToken);

        // === STEP 2: EMAIL VERIFICATION ===
        mockMvc.perform(get(baseUrl + "/verify-email")
                .param("token", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully! You can now log in."));

        // Verify user is now verified
        User verifiedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(verifiedUser.getEmailVerified()).isTrue();

        // === STEP 3: LOGIN ===
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value(firstName))
                .andExpect(jsonPath("$.user.lastName").value(lastName));
    }

    @Test
    void authWorkflowEdgeCases_shouldHandleProperly() throws Exception {
        // Test duplicate email registration
        String duplicateEmail = "duplicate@example.com";
        String password = "Password123!";
        String firstName = "Test";
        String lastName = "User";

        // First registration should succeed
        mockMvc.perform(post(baseUrl + "/register")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "%s",
                        "lastName": "%s"
                    }
                    """.formatted(duplicateEmail, password, firstName, lastName)))
                .andExpect(status().isCreated());

        // Second registration with same email should fail
        mockMvc.perform(post(baseUrl + "/register")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "%s",
                        "lastName": "%s"
                    }
                    """.formatted(duplicateEmail, password, firstName, lastName)))
                .andExpect(status().is(409));

        // Test login with unverified email
        mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(duplicateEmail, password)))
                .andExpect(status().is(417)); // EmailNotVerifiedException returns EXPECTATION_FAILED

        // Test invalid token verification
        mockMvc.perform(get(baseUrl + "/verify-email")
                .param("token", "invalid-token-123"))
                .andExpect(status().isBadRequest());

        // Test forgot password for non-existent email
        mockMvc.perform(post(baseUrl + "/forgot-password")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk()); // Should still return 200 for security

        // Test invalid MFA code
        mockMvc.perform(post(baseUrl + "/verify-mfa")
                .contentType("application/json")
                .content("""
                    {
                        "email": "test@example.com",
                        "code": "999999"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordAndResetPasswordWorkflow_shouldWork() throws Exception {
        // Test data
        String email = "forgotuser@example.com";
        String oldPassword = "OldPassword123!";
        String newPassword = "NewPassword456!";
        String firstName = "Forgot";
        String lastName = "User";

        // === STEP 1: REGISTER AND VERIFY USER ===
        mockMvc.perform(post(baseUrl + "/register")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "%s",
                        "lastName": "%s"
                    }
                    """.formatted(email, oldPassword, firstName, lastName)))
                .andExpect(status().isCreated());

        // Get user and verify email
        User user = userRepository.findByEmail(email).orElseThrow();
        String verificationToken = "email-verify-token-123";
        VerificationToken emailToken = VerificationToken.builder()
                .token(verificationToken)
                .user(user)
                .type(TokenType.EMAIL_VERIFICATION)
                .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(emailToken);

        mockMvc.perform(get(baseUrl + "/verify-email")
                .param("token", verificationToken))
                .andExpect(status().isOk());

        // Verify user is verified
        user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();

        // === STEP 2: FORGOT PASSWORD ===
        mockMvc.perform(post(baseUrl + "/forgot-password")
                .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the entered email exists, a password reset link has been sent. Please check your email address."));

        // Verify password reset token was created
        VerificationToken resetToken = verificationTokenRepository
                .findFirstByUserAndTypeOrderByCreatedAtDesc(user, TokenType.PASSWORD_RESET)
                .orElseThrow();
        assertThat(resetToken.getToken()).isNotNull();

        // === STEP 3: RESET PASSWORD ===
        mockMvc.perform(post(baseUrl + "/reset-password")
                .param("token", resetToken.getToken())
                .param("newPassword", newPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully. You can now log in with your new password."));

        // Verify password was actually changed in database
        user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPassword()).isNotNull();
        // Note: We can't easily verify the exact hashed password without the password encoder,
        // but we can verify login works with new password

        // === STEP 4: LOGIN WITH NEW PASSWORD ===
        mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email));

        // === STEP 5: VERIFY OLD PASSWORD NO LONGER WORKS ===
        mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullMFAWorkflow_shouldWork() throws Exception {
        // Test data
        String email = "mfauser@example.com";
        String password = "MFAPassword123!";
        String firstName = "MFA";
        String lastName = "User";

        // === STEP 1: REGISTER AND VERIFY USER ===
        mockMvc.perform(post(baseUrl + "/register")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "%s",
                        "lastName": "%s"
                    }
                    """.formatted(email, password, firstName, lastName)))
                .andExpect(status().isCreated());

        // Get user and verify email
        User user = userRepository.findByEmail(email).orElseThrow();
        String verificationToken = "mfa-email-verify-token-123";
        VerificationToken emailToken = VerificationToken.builder()
                .token(verificationToken)
                .user(user)
                .type(TokenType.EMAIL_VERIFICATION)
                .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(emailToken);

        mockMvc.perform(get(baseUrl + "/verify-email")
                .param("token", verificationToken))
                .andExpect(status().isOk());

        // Refresh user from database
        user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();

        // === STEP 2: ENABLE MFA ON USER ===
        user.setMfaEnabled(true);
        userRepository.save(user);

        // === STEP 3: LOGIN - SHOULD REQUIRE MFA ===
        String mfaLoginResponse = mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.message").value("MFA code sent to your email"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // === STEP 4: GET MFA CODE FROM DATABASE ===
        final Long userId = user.getId();
        MFACode mfaCode = mfaCodeRepository.findAll().stream()
                .filter(code -> code.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow();
        assertThat(mfaCode.getCode()).isNotNull();
        assertThat(mfaCode.getCode().length()).isEqualTo(6);

        // === STEP 5: VERIFY MFA CODE ===
        String finalLoginResponse = mockMvc.perform(post(baseUrl + "/verify-mfa")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "code": "%s"
                    }
                    """.formatted(email, mfaCode.getCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // === STEP 6: VERIFY MFA CODE WAS MARKED AS VERIFIED ===
        MFACode verifiedCode = mfaCodeRepository.findById(mfaCode.getId()).orElseThrow();
        assertThat(verifiedCode.isVerified()).isTrue();
        assertThat(verifiedCode.getVerifiedAt()).isNotNull();

        // === STEP 7: TEST SUBSEQUENT LOGINS STILL REQUIRE MFA ===
        // Clear any existing MFA codes
        mfaCodeRepository.deleteAll();

        // Login again - should still require MFA
        mockMvc.perform(post(baseUrl + "/login")
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true));
    }
}