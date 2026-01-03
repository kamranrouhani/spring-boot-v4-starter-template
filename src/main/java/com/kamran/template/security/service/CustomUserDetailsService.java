package com.kamran.template.security.service;

import com.kamran.template.common.exception.UserNotFoundException;
import com.kamran.template.user.User;
import com.kamran.template.user.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Custom UserDetailsService - loads user from database
 * Used by Spring Security for authentication
 */
@Service
@RequiredArgsConstructor
@NullMarked
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (email in our case)
     * Spring Security calls this during authentication
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new UserNotFoundException("User not found with Email: " + email));

        // convert our User to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(new ArrayList<>()) // TODO: we will add roles later
                .build();
    }
}
