package com.abhiram.atlas.domain;

/**
 * Access levels within Atlas.
 *
 * Kept deliberately small. Two roles cover every distinction Atlas
 * actually needs today, and inventing speculative roles produces
 * authorisation rules nobody can reason about.
 *
 * The meaningful boundary is between reading research and consuming
 * provider quota. A refresh call costs rate limited API requests and
 * mutates data every user depends on, so it belongs to an operator
 * rather than a reader.
 */
public enum UserRole {

    /**
     * Reads research, writes their own theses.
     */
    USER,

    /**
     * Everything a USER can do, plus operations that consume
     * provider quota or mutate shared data.
     */
    ADMIN;

    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * Spring Security expects authorities prefixed with ROLE_ when
     * using hasRole. Storing the bare name and converting here keeps
     * the database readable.
     */
    public String authority() {
        return ROLE_PREFIX + name();
    }

    public static UserRole parse(String value) {

        if (value == null || value.isBlank()) {
            return USER;
        }

        try {
            return UserRole.valueOf(value.trim().toUpperCase());

        } catch (IllegalArgumentException ex) {
            // An unrecognised role must not grant elevated access.
            return USER;
        }
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
