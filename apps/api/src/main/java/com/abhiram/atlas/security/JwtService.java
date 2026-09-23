package com.abhiram.atlas.security;

import com.abhiram.atlas.entity.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates Atlas access tokens.
 *
 * WHY JWT RATHER THAN SESSIONS
 *
 * Atlas has a React frontend and a Spring Boot API deployed
 * separately. Server side sessions would require sticky routing or
 * shared session storage, both of which add infrastructure for a
 * problem a signed token solves directly.
 *
 * The tradeoff is real and worth stating: a JWT cannot be revoked
 * before it expires. Atlas accepts that by keeping lifetimes short
 * and checking the enabled flag on every request, so a disabled
 * account loses access at the next call rather than at token expiry.
 *
 * SECRET HANDLING
 *
 * The signing secret comes from the environment and has no default.
 * A default would inevitably reach production and make every token
 * forgeable by anyone who read the source.
 */
@Service
public class JwtService {

    private static final Logger log =
            LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";

    private final SecretKey signingKey;
    private final long expirySeconds;
    private final String issuer;

    public JwtService(
            @Value("${atlas.jwt.secret}") String secret,
            @Value("${atlas.jwt.expiry-seconds:86400}")
            long expirySeconds,
            @Value("${atlas.jwt.issuer:atlas}") String issuer
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "atlas.jwt.secret must be at least 32 "
                            + "characters. A short key weakens "
                            + "HMAC signing enough to make tokens "
                            + "forgeable."
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirySeconds = expirySeconds;
        this.issuer = issuer;
    }

    public String issueToken(AppUser user) {

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirySeconds);

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getDisplayName())
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the user id from a token.
     *
     * Returns null rather than throwing on any failure. A malformed
     * or expired token is an ordinary condition for a public
     * endpoint, not an exceptional one, and throwing would turn
     * every anonymous request into a stack trace.
     */
    public UUID extractUserId(String token) {

        Claims claims = parse(token);

        if (claims == null) {
            return null;
        }

        try {
            return UUID.fromString(claims.getSubject());

        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String extractRole(String token) {

        Claims claims = parse(token);

        return claims == null
                ? null
                : claims.get(CLAIM_ROLE, String.class);
    }

    public boolean isValid(String token) {
        return parse(token) != null;
    }

    private Claims parse(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rejected: {}", ex.getMessage());
            return null;
        }
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}
