package com.kamran.template.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User entity.
 * Used to expose user information to external clients without revealing sensitive data
 * or internal entity details. Password is excluded from this DTO for security.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "User information returned to clients")
public class UserDto {

    @Schema(description = "Unique identifier of the user", example = "1")
    private Long Id;

    @Schema(description = "Timestamp when the user was created", example = "2026-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the user was last updated", example = "2026-01-02T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    private String email;

    @Schema(description = "First name of the user", example = "John")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Doe")
    private String lastName;

    private Role role;  // USER or ADMIN
    private SubscriptionTier subscriptionTier;  // FREE, PRO, or PREMIUM



    /**
     * Converts a User entity to a UserDto.
     * This method maps all fields from the entity to the DTO except for the password,
     * which is excluded for security reasons.
     *
     * @param user The User entity to convert
     * @return A new UserDto instance populated with data from the entity
     */
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRole(user.getRole());
        dto.setSubscriptionTier(user.getSubscriptionTier());
        return dto;
    }
}
