CREATE TABLE instrument
(
    id UUID PRIMARY KEY,

    symbol VARCHAR(30) NOT NULL UNIQUE,

    company_name VARCHAR(255) NOT NULL,

    exchange VARCHAR(30) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL
);