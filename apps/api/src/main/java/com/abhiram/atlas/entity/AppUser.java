package com.abhiram.atlas.entity;

import com.abhiram.atlas.domain.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An authenticated Atlas user.
 *
 * No password field exists anywhere in Atlas. Authentication is
 * delegated entirely to an OAuth2 provider, which removes hashing,
 * rotation, reset flows and breach exposure from the threat model.
 *
 * Identity is provider plus providerUserId rather than email,
 * because email can change at the provider and is not unique across
 * providers.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(
            name = "provider_user_id",
            nullable = false,
            length = 255
    )
    private String providerUserId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    protected AppUser() {
    }

    public static AppUser register(
            UUID id,
            String provider,
            String providerUserId,
            String email,
            String displayName,
            String avatarUrl,
            UserRole role
    ) {
        AppUser user = new AppUser();

        user.id = id;
        user.provider = provider;
        user.providerUserId = providerUserId;
        user.email = email;
        user.displayName = displayName;
        user.avatarUrl = avatarUrl;
        user.role = role;
        user.enabled = Boolean.TRUE;
        user.createdAt = LocalDateTime.now();
        user.lastLoginAt = LocalDateTime.now();

        return user;
    }

    /**
     * Refreshes profile fields from the provider on each login.
     *
     * Role is deliberately excluded. It is an Atlas concern, not a
     * provider concern, and refreshing it would silently reset any
     * elevation an administrator had granted.
     */
    public void recordLogin(
            String email,
            String displayName,
            String avatarUrl
    ) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }

        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }

        if (avatarUrl != null && !avatarUrl.isBlank()) {
            this.avatarUrl = avatarUrl;
        }

        this.lastLoginAt = LocalDateTime.now();
    }

    public void promoteTo(UserRole newRole) {
        this.role = newRole;
    }

    public void disable() {
        this.enabled = Boolean.FALSE;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
