-- Scoring profiles and sector classification.
--
-- PROBLEM THIS SOLVES
--
-- Two distinct failures showed up once the universe reached twenty
-- companies:
--
-- 1. BANKS SCORE AS NOISE
--    HDFCBANK, ICICIBANK and SBIN all scored exactly 10 out of 100.
--    An identical score across three very different lenders is not a
--    signal, it is a metric that does not apply. Banks have no
--    revenue or operating profit in the manufacturing sense, and
--    their operating cash flow is dominated by deposit and lending
--    movements rather than profitability.
--
--    Ranking them last looks like an answer. It is worse than
--    refusing to rank them, because a wrong answer gets acted on.
--
-- 2. CROSS SECTOR MARGINS ARE NOT COMPARABLE
--    Software structurally earns higher margins than cement. Scoring
--    them on the same absolute thresholds rewards the industry, not
--    the company. The useful question is whether a company is strong
--    for its own sector.
--
-- APPROACH
--
-- Each company carries a scoring profile that determines whether and
-- how it can be scored. Profiles are explicit rather than inferred,
-- because guessing from an industry string would silently mis-handle
-- companies at the boundary.

CREATE TABLE scoring_profile
(
    code VARCHAR(40) PRIMARY KEY,

    display_name VARCHAR(100) NOT NULL,

    scoreable BOOLEAN NOT NULL,

    -- Explains to a user why a profile cannot be scored. Shown in
    -- the API so the limitation is visible rather than implied by a
    -- low number.
    unscoreable_reason VARCHAR(500),

    description VARCHAR(1000) NOT NULL,

    created_at TIMESTAMP NOT NULL
);


INSERT INTO scoring_profile
(
    code,
    display_name,
    scoreable,
    unscoreable_reason,
    description,
    created_at
)
VALUES
    (
        'OPERATING_COMPANY',
        'Operating Company',
        TRUE,
        NULL,
        'Standard non-financial business. Revenue, operating profit '
        || 'and operating cash flow carry their conventional '
        || 'meanings, so margin and growth metrics apply directly.',
        NOW()
    ),
    (
        'BANK',
        'Bank or Lender',
        FALSE,
        'Atlas cannot yet score banks. Interest income is not '
        || 'revenue, and operating cash flow reflects deposit and '
        || 'lending flows rather than profitability. Scoring a bank '
        || 'on margins designed for operating companies produces a '
        || 'number that looks meaningful and is not. Bank scoring '
        || 'requires net interest margin, cost to income ratio and '
        || 'asset quality metrics that Atlas does not yet ingest.',
        'Banks and lenders. Deliberately excluded from ranking until '
        || 'sector appropriate metrics exist.',
        NOW()
    ),
    (
        'FINANCIAL_SERVICES',
        'Non-Bank Financial',
        FALSE,
        'Atlas cannot yet score non-bank financial companies. Their '
        || 'income and cash flow statements follow financial sector '
        || 'conventions that the current metrics do not model.',
        'Insurers, NBFCs and asset managers. Excluded for the same '
        || 'reason as banks.',
        NOW()
    ),
    (
        'UNKNOWN',
        'Unclassified',
        FALSE,
        'This company has not been assigned a scoring profile, so '
        || 'Atlas cannot determine whether its financial metrics are '
        || 'interpretable.',
        'Default for companies awaiting classification. Refusing to '
        || 'score is safer than assuming the standard profile.',
        NOW()
    );


-- Attach profiles to companies.
--
-- Defaulting to UNKNOWN rather than OPERATING_COMPANY is deliberate.
-- A new company added later should be explicitly classified, not
-- silently assumed to be scoreable.

ALTER TABLE company
    ADD COLUMN scoring_profile VARCHAR(40)
        NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE company
    ADD CONSTRAINT fk_company_scoring_profile
        FOREIGN KEY (scoring_profile)
        REFERENCES scoring_profile(code);

CREATE INDEX idx_company_scoring_profile
    ON company (scoring_profile);


-- Classify the current universe.

UPDATE company c
SET scoring_profile = 'BANK'
FROM instrument i
WHERE i.id = c.instrument_id
  AND i.symbol IN ('HDFCBANK', 'ICICIBANK', 'SBIN');

UPDATE company c
SET scoring_profile = 'OPERATING_COMPANY'
FROM instrument i
WHERE i.id = c.instrument_id
  AND i.symbol IN (
      'RELIANCE', 'TCS', 'INFY', 'HCLTECH',
      'HINDUNILVR', 'ITC', 'NESTLEIND', 'ASIANPAINT', 'TITAN',
      'BHARTIARTL', 'LT', 'MARUTI', 'TATAMOTORS',
      'SUNPHARMA', 'ULTRACEMCO', 'TATASTEEL', 'POWERGRID'
  );


COMMENT ON COLUMN company.scoring_profile IS
    'Determines whether standard margin and growth metrics are '
    'interpretable for this company. Banks and financial companies '
    'are excluded until sector specific metrics exist. Defaults to '
    'UNKNOWN so new companies must be classified explicitly.';
