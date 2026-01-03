package com.kamran.template.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // extract authorization header
        final String authHeader = request.getHeader("Authorization");

        // check if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // extract JWT token (removing the "Bearer " prefix
        final String jwt = authHeader.substring(7);
        final String userEmail = jwtUtil.extractUsername(jwt);

        // if email extracted and user not already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // load user details from db
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // validate token
            if (jwtUtil.validateToken(jwt, userDetails)){
                // create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // set additional details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // continue filter chain
        filterChain.doFilter(request, response);
    }
}
