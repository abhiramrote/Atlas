package com.abhiram.atlas.security;

import com.abhiram.atlas.domain.UserRole;

import java.util.UUID;

/**
 * The authenticated caller, as Atlas needs it.
 *
 * Deliberately minimal. Carrying the full AppUser entity through the
 * security context would attach a detached JPA object to every
 * request and invite lazy loading failures far from where they
 * originate.
 */
public record AtlasUserPrincipal(
        UUID userId,
        String email,
        String displayName,
        UserRole role
) {

    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }
}
