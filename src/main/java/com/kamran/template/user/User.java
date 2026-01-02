package com.kamran.template.user;

import com.kamran.template.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity representing a user in the system.
 * Stores user authentication credentials and basic profile information.
 * Inherits timestamp fields (id, createdAt, updatedAt) from BaseEntity.
 * Email is unique and serves as the primary identifier for authentication.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 * @see BaseEntity
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "User entity representing a system user")
public class User extends BaseEntity {

    /**
     * User's email address.
     * Must be unique across all users in the system.
     * Used as the primary identifier for authentication.
     */
    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    @Schema(description = "Unique email address of the user", example = "user@example.com")
    private String email;

    /**
     * User's password.
     * Note: In a production system, this should be hashed using BCrypt or similar.
     */
    @Column(nullable = false)
    @Schema(description = "User's password (should be hashed in production)", example = "hashedPassword123")
    private String password;

    /**
     * User's first name.
     */
    @Column(nullable = false)
    @Schema(description = "First name of the user", example = "John")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(nullable = false)
    @Schema(description = "Last name of the user", example = "Doe")
    private String lastName;

}
