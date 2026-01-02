package com.kamran.template.user;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service layer for user-related business logic.
 * Handles user CRUD operations, validation, and data transformation between entities and DTOs.
 * All read operations are performed in read-only transactions for performance optimization.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users from the database and converts them to DTOs.
     *
     * @return List of UserDto objects representing all users in the system
     */
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserDto::formEntity).toList();
    }

    /**
     * Retrieves a single user by their unique identifier.
     *
     * @param id The unique identifier of the user to retrieve
     * @return UserDto if user is found, null otherwise
     */
    public UserDto getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(UserDto::formEntity).orElse(null);
    }

    /**
     * Creates a new user in the system with the provided information.
     * Validates that the email is unique before creating the user.
     * The password is stored as provided (consider adding hashing in production).
     *
     * @param request The creation request containing all required user information
     * @return UserDto representing the newly created user
     * @throws RuntimeException if a user with the same email already exists
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = userRepository.save(user);

        return UserDto.formEntity(savedUser);
    }

    /**
     * Updates an existing user with the provided information.
     * Only non-null fields from the request will be updated.
     * Validates email uniqueness if a new email is provided.
     *
     * @param id The unique identifier of the user to update
     * @param request The update request containing fields to be modified
     * @return UserDto representing the updated user
     * @throws RuntimeException if user is not found or if new email already exists
     */
    @Transactional
    public UserDto updateUser(Long id, @Valid UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("User not found") );
        updateIfPresent(request.getEmail(), user::setEmail, ()-> validateEmailNotTaken(request.getEmail(), user.getEmail()));
        updateIfPresent(request.getPassword(), user::setPassword);
        updateIfPresent(request.getFirstName(), user::setFirstName);
        updateIfPresent(request.getLastName(), user::setLastName);

        return UserDto.formEntity(user);

    }

    /**
     * Updates a field on the entity if the provided value is not null.
     * This is a utility method to support partial updates.
     *
     * @param <T> The type of the value being updated
     * @param value The new value to set (update only if non-null)
     * @param setter The setter method reference to update the field
     */
    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /**
     * Updates a field on the entity if the provided value is not null,
     * with an additional validation step before applying the update.
     *
     * @param <T> The type of the value being updated
     * @param value The new value to set (update only if non-null)
     * @param setter The setter method reference to update the field
     * @param validator The validation logic to run before updating
     */
    private <T> void updateIfPresent(T value, Consumer<T> setter, Runnable validator) {
        if (value != null) {
            validator.run();
            setter.accept(value);
        }
    }

    /**
     * Validates that a new email is not already taken by another user.
     * If the new email is the same as the current email, validation passes.
     *
     * @param newEmail The new email to validate
     * @param currentEmail The user's current email
     * @throws RuntimeException if the new email is already taken by another user
     */
    private void validateEmailNotTaken(String newEmail, String currentEmail) {
        if (!newEmail.equals(currentEmail) && userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email is already taken");
        }
    }

    /**
     * Deletes a user from the system by their unique identifier.
     * Note: This method does not verify if the user exists before deletion.
     *
     * @param id The unique identifier of the user to delete
     */
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
