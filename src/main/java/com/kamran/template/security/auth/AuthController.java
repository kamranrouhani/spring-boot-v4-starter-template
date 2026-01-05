package com.kamran.template.security.auth;

import com.kamran.template.security.auth.dto.AuthResponse;
import com.kamran.template.security.auth.dto.LoginRequest;
import com.kamran.template.security.auth.dto.RegisterRequest;
import com.kamran.template.security.auth.dto.RegisterResponse;
import com.kamran.template.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * Example request:
     * POST /api/auth/register
     * {
     *   "email": "kamran@example.com",
     *   "password": "securePassword123",
     *   "firstName": "Kamran",
     *   "lastName": "Developer"
     * }
     *
     * Example response (201 Created):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "user": {
     *     "id": 1,
     *     "email": "kamran@example.com",
     *     "firstName": "Kamran",
     *     "lastName": "Developer"
     *   }
     * }
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns a JWT token for authentication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "type": "Bearer",
                                              "user": {
                                                "id": 1,
                                                "email": "kamran@example.com",
                                                "firstName": "Kamran",
                                                "lastName": "Developer",
                                                "role": "USER",
                                                "subscriptionTier": "FREE"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or email already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Registration request received for email: {}", request.getEmail());
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login and get JWT token.
     *
     * Example request:
     * POST /api/auth/login
     * {
     *   "email": "kamran@example.com",
     *   "password": "securePassword123"
     * }
     *
     * Example response (200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "user": {
     *     "id": 1,
     *     "email": "kamran@example.com",
     *     "firstName": "Kamran",
     *     "lastName": "Developer"
     *   }
     * }
     */
    @Operation(
            summary = "Login user",
            description = "Authenticates a user with email and password, returns JWT token on success"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "type": "Bearer",
                                              "user": {
                                                "id": 1,
                                                "email": "kamran@example.com",
                                                "firstName": "Kamran",
                                                "lastName": "Developer",
                                                "role": "USER",
                                                "subscriptionTier": "FREE"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user info.
     *
     * This endpoint demonstrates how to get the current user from JWT.
     * Flow:
     * 1. JwtAuthenticationFilter validates the JWT token (happens before this method)
     * 2. If valid, Spring Security populates SecurityContext with UserDetails
     * 3. @AuthenticationPrincipal extracts UserDetails from SecurityContext
     * 4. AuthService fetches fresh User entity from database using email
     * 5. Returns UserDto (excluding password)
     *
     * If JWT is invalid/expired, the request never reaches this method - Spring Security
     * returns 401 Unauthorized at the filter level.
     *
     * Example request:
     * GET /api/auth/me
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Example response (200 OK):
     * {
     *   "id": 1,
     *   "email": "kamran@example.com",
     *   "firstName": "Kamran",
     *   "lastName": "Developer",
     *   "role": "USER",
     *   "subscriptionTier": "FREE",
     *   "createdAt": "2025-01-01T12:00:00",
     *   "updatedAt": "2025-01-02T15:30:00"
     * }
     */
    @Operation(
            summary = "Get current user",
            description = "Returns the currently authenticated user's full information from the database. JWT must be valid.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved current user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 1,
                                              "email": "kamran@example.com",
                                              "firstName": "Kamran",
                                              "lastName": "Developer",
                                              "role": "USER",
                                              "subscriptionTier": "FREE",
                                              "createdAt": "2025-01-01T12:00:00",
                                              "updatedAt": "2025-01-02T15:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated - missing or invalid JWT token",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("Current user request for: {}", userDetails.getUsername());

        // Delegate to service layer to fetch fresh user data from database
        UserDto currentUser = authService.getCurrentUser(userDetails.getUsername());

        return ResponseEntity.ok(currentUser);
    }
}
