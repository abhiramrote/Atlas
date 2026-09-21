-- Thesis container.
--
-- One thesis per company per research idea. The container holds
-- mutable lifecycle state. The reasoning itself lives in immutable
-- versions so a published judgement can never be silently rewritten.

CREATE TABLE thesis
(
    id UUID PRIMARY KEY,

    company_id UUID NOT NULL,

    title VARCHAR(255) NOT NULL,

    state VARCHAR(30) NOT NULL,

    current_version INTEGER NOT NULL DEFAULT 0,

    opened_at TIMESTAMP NOT NULL,

    closed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_thesis_company
        FOREIGN KEY (company_id)
        REFERENCES company(id)
);

CREATE INDEX idx_thesis_company ON thesis (company_id);
CREATE INDEX idx_thesis_state ON thesis (state);


-- Immutable thesis version.
--
-- A published version is never updated. Revising a thesis creates a
-- new version that supersedes the previous one, so the original
-- reasoning remains inspectable.
--
-- The metric columns are a point-in-time snapshot captured at
-- publication. They are stored rather than recomputed because the
-- underlying financial data can be restated later, and the thesis
-- must remain judgeable against what was actually known at the time.

CREATE TABLE thesis_version
(
    id UUID PRIMARY KEY,

    thesis_id UUID NOT NULL,

    version_number INTEGER NOT NULL,

    horizon_months INTEGER NOT NULL,

    conviction VARCHAR(20) NOT NULL,

    rationale TEXT NOT NULL,

    key_risks TEXT,

    snapshot_fundamental_score INTEGER,

    snapshot_technical_score INTEGER,

    snapshot_final_score INTEGER,

    snapshot_rating VARCHAR(20),

    snapshot_policy_version VARCHAR(20),

    snapshot_close_price NUMERIC(20, 4),

    published_at TIMESTAMP NOT NULL,

    superseded_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_thesis_version_thesis
        FOREIGN KEY (thesis_id)
        REFERENCES thesis(id),

    CONSTRAINT uk_thesis_version_number
        UNIQUE (thesis_id, version_number)
);

CREATE INDEX idx_thesis_version_thesis
    ON thesis_version (thesis_id);


-- Invalidation condition.
--
-- Each condition belongs to a specific version, because revising a
-- thesis may legitimately change what would prove it wrong. Storing
-- them per version prevents retroactively loosening the bar after a
-- condition has been breached.

CREATE TABLE thesis_condition
(
    id UUID PRIMARY KEY,

    thesis_version_id UUID NOT NULL,

    metric VARCHAR(60) NOT NULL,

    comparison VARCHAR(20) NOT NULL,

    threshold NUMERIC(20, 4) NOT NULL,

    description VARCHAR(500),

    breached BOOLEAN NOT NULL DEFAULT FALSE,

    breached_at TIMESTAMP,

    breached_value NUMERIC(20, 4),

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_thesis_condition_version
        FOREIGN KEY (thesis_version_id)
        REFERENCES thesis_version(id)
);

CREATE INDEX idx_thesis_condition_version
    ON thesis_condition (thesis_version_id);


-- Append-only audit trail.
--
-- Every lifecycle transition is recorded. Rows are never updated or
-- deleted, so the full history of a thesis remains reconstructable.

CREATE TABLE thesis_event
(
    id UUID PRIMARY KEY,

    thesis_id UUID NOT NULL,

    event_type VARCHAR(40) NOT NULL,

    from_state VARCHAR(30),

    to_state VARCHAR(30),

    version_number INTEGER,

    detail VARCHAR(1000),

    occurred_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_thesis_event_thesis
        FOREIGN KEY (thesis_id)
        REFERENCES thesis(id)
);

CREATE INDEX idx_thesis_event_thesis
    ON thesis_event (thesis_id, occurred_at);
