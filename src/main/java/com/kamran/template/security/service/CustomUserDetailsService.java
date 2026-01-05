package com.kamran.template.security.service;

import com.kamran.template.common.exception.UserNotFoundException;
import com.kamran.template.user.User;
import com.kamran.template.user.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetailsService - loads user from database
 * Used by Spring Security for authentication
 */
@Service
@RequiredArgsConstructor
@NullMarked
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (email in our case)
     * Spring Security calls this during authentication
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("User not found with email: {}", email);
            return new UserNotFoundException("User not found with Email: " + email);
        });

        log.debug("User found: {} with role: {} and tier: {}",
                user.getEmail(), user.getRole(), user.getSubscriptionTier());

        // convert our User to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Convert user's role AND subscription tier to Spring Security authorities.
     * <p>
     * This allows us to check:
     * - @PreAuthorize("hasRole('ADMIN')") - System role
     * - @PreAuthorize("hasAuthority('TIER_PRO')") - Subscription tier
     * - @PreAuthorize("hasRole('ADMIN') or hasAuthority('TIER_PREMIUM')") - Combined
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add system role (ROLE_USER or ROLE_ADMIN)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Add subscription tier (TIER_FREE, TIER_PRO, or TIER_PREMIUM)
        authorities.add(new SimpleGrantedAuthority("TIER_" + user.getSubscriptionTier().name()));

        return authorities;
    }
}
