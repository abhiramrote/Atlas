package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.AppUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository
        extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByProviderAndProviderUserId(
            String provider,
            String providerUserId
    );

    Optional<AppUser> findByEmail(String email);

    long count();
}