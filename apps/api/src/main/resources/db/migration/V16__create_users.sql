-- User accounts and role based access control.
--
-- WHY THIS MATTERS FOR ATLAS SPECIFICALLY
--
-- Atlas is currently single user by assumption. Every thesis belongs
-- to nobody, every admin endpoint is open, and anyone with the URL
-- can trigger a provider refresh that consumes rate limited API
-- quota.
--
-- The thesis engine makes ownership genuinely necessary rather than
-- cosmetic. A research record only means something if it belongs to
-- a specific person. "Someone thought this in September" is useless;
-- "I thought this in September and was wrong" is the entire point.
--
-- IDENTITY MODEL
--
-- Authentication is delegated to an OAuth2 provider. Atlas never
-- stores a password, which removes an entire class of security
-- obligations: hashing, rotation, reset flows, breach exposure.
--
-- provider plus provider_user_id is the natural key. Email alone is
-- unreliable because users can change it at the provider, and two
-- providers can report the same address for different accounts.

CREATE TABLE app_user
(
    id UUID PRIMARY KEY,

    -- OAuth2 identity
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),

    role VARCHAR(30) NOT NULL DEFAULT 'USER',

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,

    CONSTRAINT uk_app_user_provider_identity
        UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_app_user_email ON app_user (email);
CREATE INDEX idx_app_user_role ON app_user (role);


-- Thesis ownership.
--
-- Nullable because existing theses predate authentication. Making it
-- mandatory would require inventing an owner for records that
-- genuinely have none, which would corrupt the research history the
-- thesis engine exists to protect.
--
-- New theses always carry an owner. Legacy rows stay honest about
-- not having one.

ALTER TABLE thesis
    ADD COLUMN owner_id UUID;

ALTER TABLE thesis
    ADD CONSTRAINT fk_thesis_owner
        FOREIGN KEY (owner_id)
        REFERENCES app_user(id);

CREATE INDEX idx_thesis_owner ON thesis (owner_id);


COMMENT ON COLUMN app_user.provider_user_id IS
    'Stable identifier from the OAuth2 provider. Used with provider '
    'as the natural key because email can change and is not unique '
    'across providers.';

COMMENT ON COLUMN thesis.owner_id IS
    'Nullable because theses created before authentication have no '
    'genuine owner. Inventing one would corrupt the research record.';
