package com.kamran.template.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user operations.
 * Provides endpoints for CRUD operations on user entities.
 * All endpoints are prefixed with /api/users.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users in the system")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves all users from the system.
     *
     * @return List of all users as DTOs
     */
    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users registered in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of users",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            )
    })
    @GetMapping
    public List<UserDto> getUsers() {
        return userService.getAllUsers();
    }

    /**
     * Retrieves a specific user by their unique identifier.
     *
     * @param id The unique identifier of the user
     * @return The user DTO if found, null otherwise
     */
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by their unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public UserDto getUserById(
            @Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable Long id
    ) {
        return userService.getUserById(id);
    }

    /**
     * Creates a new user in the system.
     * All fields in the request are validated before creation.
     *
     * @param request The user creation request containing required user information
     * @return ResponseEntity with the created user DTO and HTTP 201 status
     * @throws RuntimeException if email already exists in the system
     */
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user with the provided information. Email must be unique."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or email already exists",
                    content = @Content
            )
    })
    @PostMapping()
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "User creation request payload", required = true)
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserDto createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Updates an existing user with the provided information.
     * All fields in the request are optional and only non-null fields will be updated.
     *
     * @param id The unique identifier of the user to update
     * @param request The update request containing fields to be modified
     * @return ResponseEntity with the updated user DTO and HTTP 200 status
     * @throws RuntimeException if user is not found or new email already exists
     */
    @Operation(
            summary = "Update an existing user",
            description = "Updates an existing user's information. All fields are optional."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or email already exists",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "ID of the user to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "User update request payload", required = true)
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserDto userDto = userService.updateUser(id, request);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Deletes a user from the system.
     *
     * @param id The unique identifier of the user to delete
     * @return ResponseEntity with HTTP 204 No Content status
     */
    @Operation(
            summary = "Delete a user",
            description = "Permanently deletes a user from the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete", required = true)
            @PathVariable Long id
    ) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
