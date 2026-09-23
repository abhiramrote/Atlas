package com.abhiram.atlas.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The signed in user, as the frontend needs them.
 *
 * Provider identifiers are deliberately excluded. The client has no
 * use for them and exposing internal identity keys widens the
 * surface for no benefit.
 */
public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String role,
        Boolean isAdmin,
        LocalDateTime lastLoginAt
) {
}