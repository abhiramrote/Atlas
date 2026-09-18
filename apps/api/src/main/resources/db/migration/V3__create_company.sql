CREATE TABLE company
(
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL UNIQUE,
    sector VARCHAR(100),
    industry VARCHAR(255),
    website VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_company_instrument
        FOREIGN KEY (instrument_id)
        REFERENCES instrument(id)
);
