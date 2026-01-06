package com.kamran.template.user;

import com.kamran.template.common.exception.EmailAlreadyExistsException;
import com.kamran.template.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = User.builder()
            .email("test@example.com")
            .password("password")
            .firstName("John")
            .lastName("Doe")
            .role(Role.USER)
            .subscriptionTier(SubscriptionTier.FREE)
            .build();
        testUser.setId(1L);

        createRequest = new CreateUserRequest();
        createRequest.setEmail("new@example.com");
        createRequest.setPassword("password123");
        createRequest.setFirstName("Jane");
        createRequest.setLastName("Smith");

        updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setFirstName("Updated");
    }

    @Test
    void createUser_ShouldCreateUser_WhenEmailIsUnique() {
        // Arrange
        User savedUser = User.builder()
            .email(createRequest.getEmail())
            .firstName(createRequest.getFirstName())
            .lastName(createRequest.getLastName())
            .build();
        savedUser.setId(1L);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserDto result = userService.createUser(createRequest);

        // Assert
        assertThat(result.getEmail()).isEqualTo(createRequest.getEmail());
        assertThat(result.getFirstName()).isEqualTo(createRequest.getFirstName());
        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(createRequest))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining(createRequest.getEmail());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUserDto_WhenUserExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.getUserById(1L);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserByEmail_ShouldReturnUserDto_WhenUserExists() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.getUserByEmail("test@example.com");

        // Assert
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsersAsDto() {
        // Arrange
        User anotherUser = User.builder()
            .email("another@example.com")
            .firstName("Another")
            .lastName("User")
            .build();
        anotherUser.setId(2L);

        when(userRepository.findAll()).thenReturn(List.of(testUser, anotherUser));

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(result.get(1).getEmail()).isEqualTo("another@example.com");
    }

    @Test
    void updateUser_ShouldUpdateOnlyProvidedFields_WhenPartialUpdate() {
        // Arrange
        UpdateUserRequest partialUpdate = new UpdateUserRequest();
        partialUpdate.setEmail("newemail@example.com");
        // Note: firstName, lastName, password are null - should not be updated

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);

        // Act
        UserDto result = userService.updateUser(1L, partialUpdate);

        // Assert
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        assertThat(result.getFirstName()).isEqualTo("John"); // Should remain unchanged
        assertThat(result.getLastName()).isEqualTo("Doe");   // Should remain unchanged
    }

    @Test
    void updateUser_ShouldUpdateAllFields_WhenAllFieldsProvided() {
        // Arrange
        UpdateUserRequest fullUpdate = new UpdateUserRequest();
        fullUpdate.setEmail("newemail@example.com");
        fullUpdate.setFirstName("UpdatedFirst");
        fullUpdate.setLastName("UpdatedLast");
        fullUpdate.setPassword("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);

        // Act
        UserDto result = userService.updateUser(1L, fullUpdate);

        // Assert
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        assertThat(result.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(result.getLastName()).isEqualTo("UpdatedLast");
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(999L, updateRequest))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUser_ShouldThrowException_WhenNewEmailAlreadyExists() {
        // Arrange
        UpdateUserRequest emailUpdate = new UpdateUserRequest();
        emailUpdate.setEmail("existing@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, emailUpdate))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Email is already taken");
    }

    @Test
    void updateUser_ShouldAllowSameEmail_WhenEmailUnchanged() {
        // Arrange - user wants to update other fields but keep same email
        UpdateUserRequest sameEmailUpdate = new UpdateUserRequest();
        sameEmailUpdate.setEmail("test@example.com"); // Same as current
        sameEmailUpdate.setFirstName("UpdatedName");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Note: No need to mock existsByEmail since email is the same as current

        // Act
        UserDto result = userService.updateUser(1L, sameEmailUpdate);

        // Assert
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("UpdatedName");
    }

    @Test
    void deleteById_ShouldDeleteUser_WhenUserExists() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteById(1L);

        // Assert
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteById_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteById(999L))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(999L);
    }
}
