package com.kamran.template.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 * Provides standard CRUD operations inherited from JpaRepository
 * and custom query methods for email-based lookups.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Email lookups are case-sensitive.
     *
     * @param email The email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given email exists in the database.
     * More efficient than findByEmail when only checking existence.
     *
     * @param email The email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
