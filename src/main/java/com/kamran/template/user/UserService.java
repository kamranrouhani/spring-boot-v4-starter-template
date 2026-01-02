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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserDto::formEntity).toList();
    }

    public UserDto getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(UserDto::formEntity).orElse(null);
    }

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

    @Transactional
    public UserDto updateUser(Long id, @Valid UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("User not found") );
        updateIfPresent(request.getEmail(), user::setEmail, ()-> validateEmailNotTaken(request.getEmail(), user.getEmail()));
        updateIfPresent(request.getPassword(), user::setPassword);
        updateIfPresent(request.getFirstName(), user::setFirstName);
        updateIfPresent(request.getLastName(), user::setLastName);

        return UserDto.formEntity(user);

    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter, Runnable validator) {
        if (value != null) {
            validator.run();
            setter.accept(value);
        }
    }


    private void validateEmailNotTaken(String newEmail, String currentEmail) {
        if (!newEmail.equals(currentEmail) && userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email is already taken");
        }
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
