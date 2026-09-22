-- Point-in-time financial statement versioning.
--
-- PROBLEM THIS SOLVES
--
-- Atlas currently replaces financial statements on every ingest. That
-- makes honest backtesting impossible, because you cannot reconstruct
-- what was actually known on a past date. Scoring a company "as of
-- 2022" using today's restated figures leaks future information into
-- the past and produces backtest results that cannot be reproduced in
-- live trading. This is the single most common way backtests lie.
--
-- APPROACH
--
-- Two distinct dates are tracked per observation:
--
--   period_end_date   when the reporting period ended
--   knowledge_date    when Atlas first learned this figure
--
-- A backtest as of date D must filter on knowledge_date <= D, never
-- on period_end_date alone. A company's FY2024 results are not usable
-- on 1 April 2024 because they had not been published yet.
--
-- Restatements create a NEW row rather than updating an existing one.
-- The superseded row keeps its original knowledge_date so the earlier
-- view of the world remains queryable.

CREATE TABLE financial_observation
(
    id UUID PRIMARY KEY,

    company_id UUID NOT NULL,

    -- Reporting period
    fiscal_year INTEGER NOT NULL,
    fiscal_quarter INTEGER,
    period_end_date DATE,

    -- When Atlas learned this. The backtest filter column.
    knowledge_date DATE NOT NULL,

    -- Figures as reported at knowledge_date
    revenue NUMERIC(20, 2),
    net_income NUMERIC(20, 2),
    operating_profit NUMERIC(20, 2),
    operating_cash_flow NUMERIC(20, 2),

    -- Provenance
    source_provider VARCHAR(40) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'CRORE',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',

    -- Restatement chain
    revision_number INTEGER NOT NULL DEFAULT 1,
    superseded_at TIMESTAMP,
    superseded_by UUID,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_financial_observation_company
        FOREIGN KEY (company_id)
        REFERENCES company(id),

    CONSTRAINT fk_financial_observation_superseded_by
        FOREIGN KEY (superseded_by)
        REFERENCES financial_observation(id)
);

-- The primary backtest access path. A point-in-time query filters by
-- company and knowledge_date, then takes the latest period per
-- fiscal year.
CREATE INDEX idx_financial_observation_pit
    ON financial_observation (
        company_id,
        knowledge_date,
        fiscal_year DESC
    );

CREATE INDEX idx_financial_observation_period
    ON financial_observation (company_id, fiscal_year);

-- Prevents the same provider reporting the same period twice on the
-- same day. A genuine restatement arrives on a later date and is
-- therefore permitted.
CREATE UNIQUE INDEX uk_financial_observation_unique_report
    ON financial_observation (
        company_id,
        fiscal_year,
        fiscal_quarter,
        knowledge_date,
        source_provider
    );


-- Backfill from the existing financial_statement table.
--
-- knowledge_date is set to the row's created_at, which is the best
-- available approximation of when Atlas learned the figure. This is
-- NOT the true publication date and must not be treated as reliable
-- for periods ingested in bulk. Rows backfilled here all share
-- roughly the same knowledge_date, which means a backtest before that
-- date will correctly find nothing rather than silently using data
-- that did not exist yet.

INSERT INTO financial_observation
(
    id,
    company_id,
    fiscal_year,
    fiscal_quarter,
    period_end_date,
    knowledge_date,
    revenue,
    net_income,
    operating_profit,
    operating_cash_flow,
    source_provider,
    unit,
    currency,
    revision_number,
    created_at
)
SELECT
    gen_random_uuid(),
    fs.company_id,
    fs.fiscal_year,
    fs.fiscal_quarter,
    MAKE_DATE(fs.fiscal_year, 3, 31),
    CAST(fs.created_at AS DATE),
    fs.revenue,
    fs.net_income,
    fs.operating_profit,
    fs.operating_cash_flow,
    'BACKFILL',
    'CRORE',
    'INR',
    1,
    fs.created_at
FROM financial_statement fs;


-- Data quality note recorded in the schema itself.
COMMENT ON COLUMN financial_observation.knowledge_date IS
    'Date Atlas learned this figure. Backtests MUST filter on this '
    'column, never on period_end_date alone. Rows with '
    'source_provider = BACKFILL carry an approximated knowledge_date '
    'and are not reliable for point-in-time evaluation.';

COMMENT ON COLUMN financial_observation.unit IS
    'Reporting unit. IndianAPI returns CRORE. Mixing units across '
    'providers without conversion would corrupt absolute comparisons.';
