package com.kamran.template.security.config;

import com.kamran.template.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * Configures JWT-based stateless authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Main security filter chain
     * Defines which endpoints are public vs protected
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // disabled CSRF (Not needed for stateless JWT)
                .csrf(AbstractHttpConfigurer::disable)
                // configurable endpoint access
                .authorizeHttpRequests(auth -> auth
                        // public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/auth/**", // login, register
                                "/h2-console/**", // H2 DB console (Dev only)
                                "/error", // Error page
                                "/swagger-ui/**", // Swagger UI
                                "/swagger-ui.html", // Swagger UI HTML page
                                "/v3/api-docs/**", // OpenAPI docs
                                "/api-docs/**", // Alternative OpenAPI path
                                "/api/auth/verify-email" // for email verification
                        ).permitAll()
                        // ALL other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // stateless session (no server-side sessions, use JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // use our custom authentication provider
                .authenticationProvider(authenticationProvider())
                // add JWT filter before spring security's user/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // allow H2 console to work (frames needed for H2 UI)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        return http.build();
    }

    /**
     * Password encoder - uses BCrypt (industry standard)
     * Automatically salts and hashes passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider - connects UserDetailsService with password encoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager - used by login endpoint
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
