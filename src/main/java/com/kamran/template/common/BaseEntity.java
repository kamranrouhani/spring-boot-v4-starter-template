package com.kamran.template.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Abstract base entity providing common fields for all JPA entities.
 * Provides automatic management of:
 * - Primary key (id)
 * - Creation timestamp (createdAt)
 * - Last update timestamp (updatedAt)
 *
 * The timestamps are automatically set using JPA lifecycle callbacks:
 * - onCreate: Sets both createdAt and updatedAt when entity is first persisted
 * - onUpdate: Updates updatedAt whenever entity is modified
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@MappedSuperclass
@Getter
@Setter
@Schema(description = "Base entity providing common fields for all entities")
public abstract class BaseEntity {

    /**
     * Unique identifier for the entity.
     * Auto-generated using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1")
    private Long Id;

    /**
     * Timestamp when the entity was created.
     * Automatically set when the entity is first persisted.
     * Cannot be updated after creation.
     */
    @Column(nullable = false, updatable = false)
    @Schema(description = "Timestamp when entity was created", example = "2026-01-01T12:00:00")
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last updated.
     * Automatically set on creation and updated on every modification.
     */
    @Column(nullable = false)
    @Schema(description = "Timestamp when entity was last updated", example = "2026-01-02T15:30:00")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback executed before entity is persisted.
     * Sets the createdAt and updatedAt timestamps to the current time.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback executed before entity is updated.
     * Updates the updatedAt timestamp to the current time.
     */
    @PreUpdate
    protected void onUpdate () {
        updatedAt = LocalDateTime.now();
    }
}
