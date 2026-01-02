package com.kamran.template.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new user.
 * All fields are required and will be validated before processing.
 * Password must be at least 8 characters long.
 * Email must be unique in the system and follow valid email format.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "Request payload for creating a new user")
public class CreateUserRequest {

    @Schema(
            description = "Email address of the user (must be unique)",
            example = "john.doe@example.com",
            required = true
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid Email")
    private String email;

    @Schema(
            description = "User's password (minimum 8 characters)",
            example = "SecurePass123",
            required = true,
            minLength = 8
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Schema(
            description = "First name of the user",
            example = "John",
            required = true
    )
    @NotBlank(message = "First name is required")
    private String firstName;

    @Schema(
            description = "Last name of the user",
            example = "Doe",
            required = true
    )
    @NotBlank(message = "Last name is required")
    private String lastName;
}
