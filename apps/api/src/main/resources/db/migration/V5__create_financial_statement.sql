CREATE TABLE financial_statement
(
    id UUID PRIMARY KEY,

    company_id UUID NOT NULL,

    fiscal_year INTEGER NOT NULL,

    fiscal_quarter INTEGER,

    revenue NUMERIC(20,2),

    net_income NUMERIC(20,2),

    operating_profit NUMERIC(20,2),

    operating_cash_flow NUMERIC(20,2),

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_financial_company
        FOREIGN KEY (company_id)
        REFERENCES company(id)
);