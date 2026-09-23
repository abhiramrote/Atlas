package com.abhiram.atlas.security;

import com.abhiram.atlas.domain.UserRole;
import com.abhiram.atlas.entity.AppUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for token issuing and validation.
 *
 * The rejection tests matter more than the happy path. A token
 * system that accepts a forged or tampered token is worse than no
 * authentication, because it creates false confidence.
 */
class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-that-is-long-enough-for-hmac-sha";

    private final JwtService service =
            new JwtService(SECRET, 3600, "atlas");

    @Test
    @DisplayName("Issues a token carrying the user identity")
    void issuesTokenWithIdentity() {

        AppUser user = user(UserRole.USER);

        String token = service.issueToken(user);

        assertThat(token).isNotBlank();
        assertThat(service.isValid(token)).isTrue();

        assertThat(service.extractUserId(token))
                .isEqualTo(user.getId());

        assertThat(service.extractRole(token))
                .isEqualTo("USER");
    }

    @Test
    @DisplayName("Carries the admin role when present")
    void carriesAdminRole() {

        String token = service.issueToken(user(UserRole.ADMIN));

        assertThat(service.extractRole(token))
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Rejects a token signed with a different key")
    void rejectsForeignSignature() {

        JwtService other = new JwtService(
                "a-completely-different-secret-key-for-testing",
                3600,
                "atlas"
        );

        String foreignToken = other.issueToken(user(UserRole.ADMIN));

        // Accepting this would let anyone who knows the payload
        // format mint admin tokens.
        assertThat(service.isValid(foreignToken)).isFalse();
        assertThat(service.extractUserId(foreignToken)).isNull();
    }

    @Test
    @DisplayName("Rejects a tampered token")
    void rejectsTamperedToken() {

        String token = service.issueToken(user(UserRole.USER));

        // Flip a character in the payload segment.
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x."
                + parts[2];

        assertThat(service.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("Rejects an expired token")
    void rejectsExpiredToken() throws Exception {

        JwtService shortLived =
                new JwtService(SECRET, 1, "atlas");

        String token =
                shortLived.issueToken(user(UserRole.USER));

        assertThat(shortLived.isValid(token)).isTrue();

        Thread.sleep(1100);

        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Rejects a token from a different issuer")
    void rejectsForeignIssuer() {

        JwtService other =
                new JwtService(SECRET, 3600, "not-atlas");

        String token = other.issueToken(user(UserRole.USER));

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Returns null rather than throwing on junk")
    void handlesMalformedInput() {

        // Anonymous requests are ordinary, not exceptional.
        // Throwing would turn every one into a stack trace.
        assertThat(service.extractUserId(null)).isNull();
        assertThat(service.extractUserId("")).isNull();
        assertThat(service.extractUserId("   ")).isNull();
        assertThat(service.extractUserId("not-a-token")).isNull();
        assertThat(service.extractUserId("a.b.c")).isNull();

        assertThat(service.isValid("garbage")).isFalse();
    }

    @Test
    @DisplayName("Refuses to start with a weak secret")
    void refusesWeakSecret() {

        // A short key makes HMAC signatures brute forcible.
        assertThatThrownBy(() ->
                new JwtService("short", 3600, "atlas"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    private AppUser user(UserRole role) {
        return AppUser.register(
                UUID.randomUUID(),
                "GOOGLE",
                "google-user-123",
                "test@example.com",
                "Test User",
                "https://example.com/avatar.png",
                role
        );
    }
}
