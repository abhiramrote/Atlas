-- Universe expansion from twenty to fifty companies.
--
-- WHY THIS SPECIFIC EXPANSION
--
-- After V13 only two of nine sectors cleared the three peer minimum
-- for a meaningful baseline. Most sectors had a single company, which
-- means the median was that company and normalisation was impossible.
--
-- This migration is shaped by that gap. Rather than adding the next
-- thirty largest companies by market capitalisation, it deliberately
-- fills thin sectors so most reach four or more scoreable peers.
--
-- Target distribution after this migration:
--
--   Information Technology    5 scoreable
--   Consumer Goods            8 scoreable
--   Automobile                5 scoreable
--   Healthcare                5 scoreable
--   Metals and Mining         4 scoreable
--   Energy                    4 scoreable
--   Power                     4 scoreable
--   Construction Materials    3 scoreable
--   Telecommunications        2 scoreable
--   Financial Services       10 unscoreable
--
-- Telecommunications stays thin because the listed Indian telecom
-- universe genuinely is thin. That is a real constraint, not an
-- oversight, and the baseline will correctly report it as
-- insufficient.
--
-- BANKS AND FINANCIALS
--
-- Seven more financial companies are added even though none can be
-- scored. They belong in the instrument universe because Atlas will
-- eventually need bank metrics, and having them present makes the
-- gap visible rather than hidden. They are classified as unscoreable
-- so they are excluded from rankings automatically.

INSERT INTO instrument
(
    id,
    symbol,
    company_name,
    exchange,
    active,
    created_at,
    updated_at
)
VALUES
    -- Information Technology
    ('550e8400-e29b-41d4-a716-446655440021', 'WIPRO',
     'Wipro Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440022', 'TECHM',
     'Tech Mahindra Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Consumer Goods
    ('550e8400-e29b-41d4-a716-446655440023', 'BRITANNIA',
     'Britannia Industries Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440024', 'DABUR',
     'Dabur India Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440025', 'MARICO',
     'Marico Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Automobile
    ('550e8400-e29b-41d4-a716-446655440026', 'M&M',
     'Mahindra & Mahindra Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440027', 'BAJAJ-AUTO',
     'Bajaj Auto Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440028', 'EICHERMOT',
     'Eicher Motors Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Healthcare
    ('550e8400-e29b-41d4-a716-446655440029', 'DRREDDY',
     'Dr Reddys Laboratories Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440030', 'CIPLA',
     'Cipla Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440031', 'DIVISLAB',
     'Divis Laboratories Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440032', 'APOLLOHOSP',
     'Apollo Hospitals Enterprise Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Metals and Mining
    ('550e8400-e29b-41d4-a716-446655440033', 'HINDALCO',
     'Hindalco Industries Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440034', 'JSWSTEEL',
     'JSW Steel Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440035', 'COALINDIA',
     'Coal India Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Energy
    ('550e8400-e29b-41d4-a716-446655440036', 'ONGC',
     'Oil & Natural Gas Corporation Ltd', 'NSE', TRUE,
     NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440037', 'BPCL',
     'Bharat Petroleum Corporation Ltd', 'NSE', TRUE,
     NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440038', 'IOC',
     'Indian Oil Corporation Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Power
    ('550e8400-e29b-41d4-a716-446655440039', 'NTPC',
     'NTPC Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440040', 'TATAPOWER',
     'Tata Power Company Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440041', 'ADANIPOWER',
     'Adani Power Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Construction Materials
    ('550e8400-e29b-41d4-a716-446655440042', 'GRASIM',
     'Grasim Industries Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440043', 'SHREECEM',
     'Shree Cement Ltd', 'NSE', TRUE, NOW(), NOW()),

    -- Financial Services, deliberately unscoreable
    ('550e8400-e29b-41d4-a716-446655440044', 'KOTAKBANK',
     'Kotak Mahindra Bank Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440045', 'AXISBANK',
     'Axis Bank Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440046', 'INDUSINDBK',
     'IndusInd Bank Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440047', 'BAJFINANCE',
     'Bajaj Finance Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440048', 'HDFCLIFE',
     'HDFC Life Insurance Company Ltd', 'NSE', TRUE,
     NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440049', 'SBILIFE',
     'SBI Life Insurance Company Ltd', 'NSE', TRUE,
     NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440050', 'ADANIENT',
     'Adani Enterprises Ltd', 'NSE', TRUE, NOW(), NOW());


INSERT INTO company
(
    id,
    instrument_id,
    sector,
    industry,
    website,
    description,
    scoring_profile,
    created_at,
    updated_at
)
VALUES
    -- Information Technology
    ('650e8400-e29b-41d4-a716-446655440021',
     '550e8400-e29b-41d4-a716-446655440021',
     'Information Technology', 'IT Services and Consulting',
     'https://www.wipro.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440022',
     '550e8400-e29b-41d4-a716-446655440022',
     'Information Technology', 'IT Services and Consulting',
     'https://www.techmahindra.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Consumer Goods
    ('650e8400-e29b-41d4-a716-446655440023',
     '550e8400-e29b-41d4-a716-446655440023',
     'Consumer Goods', 'Packaged Foods',
     'https://www.britannia.co.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440024',
     '550e8400-e29b-41d4-a716-446655440024',
     'Consumer Goods', 'Personal Products',
     'https://www.dabur.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440025',
     '550e8400-e29b-41d4-a716-446655440025',
     'Consumer Goods', 'Personal Products',
     'https://marico.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Automobile
    ('650e8400-e29b-41d4-a716-446655440026',
     '550e8400-e29b-41d4-a716-446655440026',
     'Automobile', 'Passenger Vehicles',
     'https://www.mahindra.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440027',
     '550e8400-e29b-41d4-a716-446655440027',
     'Automobile', 'Two Wheelers',
     'https://www.bajajauto.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440028',
     '550e8400-e29b-41d4-a716-446655440028',
     'Automobile', 'Two Wheelers',
     'https://www.eicher.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Healthcare
    ('650e8400-e29b-41d4-a716-446655440029',
     '550e8400-e29b-41d4-a716-446655440029',
     'Healthcare', 'Pharmaceuticals',
     'https://www.drreddys.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440030',
     '550e8400-e29b-41d4-a716-446655440030',
     'Healthcare', 'Pharmaceuticals',
     'https://www.cipla.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440031',
     '550e8400-e29b-41d4-a716-446655440031',
     'Healthcare', 'Pharmaceuticals',
     'https://www.divislabs.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440032',
     '550e8400-e29b-41d4-a716-446655440032',
     'Healthcare', 'Hospitals and Diagnostic Centres',
     'https://www.apollohospitals.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Metals and Mining
    ('650e8400-e29b-41d4-a716-446655440033',
     '550e8400-e29b-41d4-a716-446655440033',
     'Metals and Mining', 'Aluminium',
     'https://www.hindalco.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440034',
     '550e8400-e29b-41d4-a716-446655440034',
     'Metals and Mining', 'Iron and Steel',
     'https://www.jsw.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440035',
     '550e8400-e29b-41d4-a716-446655440035',
     'Metals and Mining', 'Coal',
     'https://www.coalindia.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Energy
    ('650e8400-e29b-41d4-a716-446655440036',
     '550e8400-e29b-41d4-a716-446655440036',
     'Energy', 'Oil Exploration and Production',
     'https://www.ongcindia.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440037',
     '550e8400-e29b-41d4-a716-446655440037',
     'Energy', 'Refineries and Marketing',
     'https://www.bharatpetroleum.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440038',
     '550e8400-e29b-41d4-a716-446655440038',
     'Energy', 'Refineries and Marketing',
     'https://www.iocl.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Power
    ('650e8400-e29b-41d4-a716-446655440039',
     '550e8400-e29b-41d4-a716-446655440039',
     'Power', 'Power Generation',
     'https://www.ntpc.co.in', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440040',
     '550e8400-e29b-41d4-a716-446655440040',
     'Power', 'Integrated Power Utilities',
     'https://www.tatapower.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440041',
     '550e8400-e29b-41d4-a716-446655440041',
     'Power', 'Power Generation',
     'https://www.adanipower.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Construction Materials
    ('650e8400-e29b-41d4-a716-446655440042',
     '550e8400-e29b-41d4-a716-446655440042',
     'Construction Materials', 'Cement',
     'https://www.grasim.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440043',
     '550e8400-e29b-41d4-a716-446655440043',
     'Construction Materials', 'Cement',
     'https://www.shreecement.com', NULL,
     'OPERATING_COMPANY', NOW(), NOW()),

    -- Financial Services, unscoreable by design
    ('650e8400-e29b-41d4-a716-446655440044',
     '550e8400-e29b-41d4-a716-446655440044',
     'Financial Services', 'Private Sector Bank',
     'https://www.kotak.com', NULL,
     'BANK', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440045',
     '550e8400-e29b-41d4-a716-446655440045',
     'Financial Services', 'Private Sector Bank',
     'https://www.axisbank.com', NULL,
     'BANK', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440046',
     '550e8400-e29b-41d4-a716-446655440046',
     'Financial Services', 'Private Sector Bank',
     'https://www.indusind.com', NULL,
     'BANK', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440047',
     '550e8400-e29b-41d4-a716-446655440047',
     'Financial Services', 'Non Banking Financial Company',
     'https://www.bajajfinserv.in', NULL,
     'FINANCIAL_SERVICES', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440048',
     '550e8400-e29b-41d4-a716-446655440048',
     'Financial Services', 'Life Insurance',
     'https://www.hdfclife.com', NULL,
     'FINANCIAL_SERVICES', NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440049',
     '550e8400-e29b-41d4-a716-446655440049',
     'Financial Services', 'Life Insurance',
     'https://www.sbilife.co.in', NULL,
     'FINANCIAL_SERVICES', NOW(), NOW()),

    -- Diversified holding company.
    --
    -- Classified UNKNOWN rather than OPERATING_COMPANY because a
    -- conglomerate spanning ports, power, mining and airports has
    -- consolidated statements that do not describe a single business
    -- model. Assigning it a sector median would be meaningless.
    ('650e8400-e29b-41d4-a716-446655440050',
     '550e8400-e29b-41d4-a716-446655440050',
     'Diversified', 'Trading and Distribution',
     'https://www.adanienterprises.com', NULL,
     'UNKNOWN', NOW(), NOW());
