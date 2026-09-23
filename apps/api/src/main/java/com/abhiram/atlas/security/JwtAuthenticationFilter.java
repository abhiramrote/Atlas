package com.abhiram.atlas.security;

import com.abhiram.atlas.domain.UserRole;
import com.abhiram.atlas.entity.AppUser;
import com.abhiram.atlas.repository.AppUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Authenticates requests carrying a bearer token.
 *
 * The user is loaded from the database on every request rather than
 * trusted from the token claims. This costs a query but means a
 * disabled account loses access immediately rather than when its
 * token expires, which is the main weakness of stateless auth.
 *
 * An absent or invalid token is not an error. The filter simply
 * leaves the context unauthenticated and lets the authorisation
 * rules decide, so public endpoints keep working for anonymous
 * callers.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            authenticate(token);
        }

        chain.doFilter(request, response);
    }

    private void authenticate(String token) {

        UUID userId = jwtService.extractUserId(token);

        if (userId == null) {
            return;
        }

        Optional<AppUser> found =
                userRepository.findById(userId);

        if (found.isEmpty()) {
            return;
        }

        AppUser user = found.get();

        // A disabled account is rejected even with a valid token.
        if (!user.isEnabled()) {
            return;
        }

        UserRole role = user.getRole();

        AtlasUserPrincipal principal = new AtlasUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                role
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                role.authority()))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {

        String header = request.getHeader(HEADER);

        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }

        String token = header.substring(PREFIX.length()).trim();

        return token.isEmpty() ? null : token;
    }
}