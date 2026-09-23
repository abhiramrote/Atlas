package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.UserRole;
import com.abhiram.atlas.entity.AppUser;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.AppUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * User registration and lookup.
 *
 * Accounts are created on first login rather than through a separate
 * signup flow. The provider has already verified the person, so
 * asking them to register again adds friction without adding
 * assurance.
 */
@Service
public class UserService {

    private static final Logger log =
            LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository userRepository;
    private final String bootstrapAdminEmail;

    public UserService(
            AppUserRepository userRepository,
            @Value("${atlas.admin.email:}")
            String bootstrapAdminEmail
    ) {
        this.userRepository = userRepository;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    /**
     * Finds an existing user or creates one on first login.
     *
     * Lookup uses provider plus provider id rather than email,
     * because email can change at the provider and is not unique
     * across providers.
     */
    @Transactional
    public AppUser findOrCreate(
            String provider,
            String providerUserId,
            String email,
            String displayName,
            String avatarUrl
    ) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "OAuth2 provider did not supply a user id"
            );
        }

        Optional<AppUser> existing = userRepository
                .findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                );

        if (existing.isPresent()) {

            AppUser user = existing.get();
            user.recordLogin(email, displayName, avatarUrl);

            return userRepository.save(user);
        }

        UserRole role = determineInitialRole(email);

        AppUser user = AppUser.register(
                UUID.randomUUID(),
                provider,
                providerUserId,
                email,
                displayName,
                avatarUrl,
                role
        );

        log.info(
                "Registered new user {} with role {}",
                email,
                role
        );

        return userRepository.save(user);
    }

    /**
     * Decides the role for a brand new account.
     *
     * Two bootstrap paths exist because a system with no admin
     * cannot promote anyone, but an open first-user-wins rule on a
     * public deployment would hand control to whoever logs in first.
     *
     * The configured admin email is checked first and is the
     * intended path for public deployment. The first user rule only
     * applies when no admin email is configured, which suits local
     * development.
     */
    private UserRole determineInitialRole(String email) {

        if (bootstrapAdminEmail != null
                && !bootstrapAdminEmail.isBlank()
                && bootstrapAdminEmail.equalsIgnoreCase(email)) {

            return UserRole.ADMIN;
        }

        boolean noAdminConfigured =
                bootstrapAdminEmail == null
                        || bootstrapAdminEmail.isBlank();

        if (noAdminConfigured && userRepository.count() == 0) {
            log.warn(
                    "No admin email configured. Granting ADMIN to "
                            + "the first user. Set atlas.admin.email "
                            + "before deploying publicly."
            );

            return UserRole.ADMIN;
        }

        return UserRole.USER;
    }

    @Transactional(readOnly = true)
    public AppUser getById(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        )
                );
    }
}