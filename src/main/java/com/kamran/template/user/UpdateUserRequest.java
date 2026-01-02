package com.kamran.template.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing user.
 * All fields are OPTIONAL - clients can update any combination of fields.
 * Only the provided (non-null) fields will be updated on the user entity.
 * If updating email, the new email must not already exist for another user.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "Request payload for updating an existing user (all fields optional)")
public class UpdateUserRequest {

    @Schema(
            description = "New email address for the user (must be unique if provided)",
            example = "newemail@example.com",
            required = false
    )
    @Email(message = "Must be a valid Email")
    private String email;

    @Schema(
            description = "New password for the user (minimum 8 characters if provided)",
            example = "NewSecurePass456",
            required = false,
            minLength = 8
    )
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Schema(
            description = "New first name for the user",
            example = "Jane",
            required = false,
            maxLength = 50
    )
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(
            description = "New last name for the user",
            example = "Smith",
            required = false,
            maxLength = 50
    )
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
}
