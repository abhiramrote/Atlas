CREATE TABLE price_bar
(
    id UUID PRIMARY KEY,

    instrument_id UUID NOT NULL,

    trade_date DATE NOT NULL,

    open_price NUMERIC(20,2),
    high_price NUMERIC(20,2),
    low_price NUMERIC(20,2),
    close_price NUMERIC(20,2),

    volume BIGINT,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_price_bar_instrument
        FOREIGN KEY (instrument_id)
        REFERENCES instrument(id)
);