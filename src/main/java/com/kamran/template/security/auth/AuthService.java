package com.kamran.template.security.auth;

import com.kamran.template.common.exception.EmailAlreadyExistsException;
import com.kamran.template.common.exception.EmailNotVerifiedException;
import com.kamran.template.security.auth.dto.AuthResponse;
import com.kamran.template.security.auth.dto.LoginRequest;
import com.kamran.template.security.auth.dto.RegisterRequest;
import com.kamran.template.security.auth.dto.RegisterResponse;
import com.kamran.template.security.jwt.JwtUtil;
import com.kamran.template.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


    /**
     * Register a new user.
     * <p>
     * Flow:
     * 1. Check if email already exists
     * 2. Hash the password with BCrypt
     * 3. Save user to database
     * 4. Generate JWT token
     * 5. Return token + user info
     *
     * @param request Registration details (email, password, firstName, lastName)
     * @return AuthResponse with JWT token and user info
     * @throws EmailAlreadyExistsException if email is already registered
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // check if the user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());

            throw new EmailAlreadyExistsException("Email " + request.getEmail() + " is already registered");
        }

        // create new user with hashed password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .subscriptionTier(SubscriptionTier.FREE)
                .build();


        // save user to DB
        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getEmail());

        // TODO: Email verification workflow
//        // Generate verification token
//        String token = verificationTokenService.createVerificationToken(
//                savedUser,
//                TokenType.EMAIL_VERIFICATION
//        );
//
//        // Send verification email
//        emailService.sendVerificationEmail(savedUser.getEmail(), token);
//        log.info("Verification email sent to: {}", savedUser.getEmail());

        // generate jwt token
        String token = jwtUtil.generateToken(savedUser.getEmail());

        // return token and the user info
        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    /**
     * Login and generate JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // First, check if user exists and email is verified
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            log.info("User authenticated successfully: {}",
                    Optional.ofNullable(userDetails.getUsername()).orElse("unknown"));


            String token = jwtUtil.generateToken(user.getEmail());

            return AuthResponse.builder()
                    .accessToken("accessToken") // TODO: accessToken to be added
                    .refreshToken("refreshToken") // TODO: refreshToken to be added
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
     * Get current user information by email.
     * Fetches the latest user data from database.
     *
     * @param email User's email (extracted from JWT)
     * @return UserDto with current user information (excluding password)
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if user not found
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

}
