-- Universe expansion to twenty NIFTY constituents.
--
-- WHY TWENTY AND NOT FIVE HUNDRED
--
-- Atlas scoring compares companies against each other. With three
-- companies a ranking is meaningless because there is no distribution
-- to sit within. Twenty is enough to see spread, spot outliers, and
-- notice when a score is high only because everything else is worse.
--
-- Going straight to hundreds would mean debugging partial provider
-- failures across a large surface before the pipeline is proven. The
-- existing refresh-all already showed what that looks like.
--
-- SELECTION
--
-- Large, liquid, well covered names across several sectors. Sector
-- diversity matters because Atlas will later normalise scores within
-- sector, and that is impossible to validate with a single industry.
--
-- SECTOR VALUES
--
-- Sectors here are assigned from public classification knowledge, not
-- from the provider, because IndianAPI exposes industry but not
-- sector. They are recorded as seed values that ingestion will not
-- overwrite, since blank provider fields never replace stored ones.

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
    ('550e8400-e29b-41d4-a716-446655440004', 'HDFCBANK',
     'HDFC Bank Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440005', 'ICICIBANK',
     'ICICI Bank Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440006', 'SBIN',
     'State Bank of India', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440007', 'HINDUNILVR',
     'Hindustan Unilever Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440008', 'ITC',
     'ITC Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440009', 'BHARTIARTL',
     'Bharti Airtel Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440010', 'LT',
     'Larsen & Toubro Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440011', 'MARUTI',
     'Maruti Suzuki India Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440012', 'SUNPHARMA',
     'Sun Pharmaceutical Industries Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440013', 'TITAN',
     'Titan Company Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440014', 'ASIANPAINT',
     'Asian Paints Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440015', 'NESTLEIND',
     'Nestle India Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440016', 'ULTRACEMCO',
     'UltraTech Cement Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440017', 'TATAMOTORS',
     'Tata Motors Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440018', 'TATASTEEL',
     'Tata Steel Ltd', 'NSE', TRUE, NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440019', 'POWERGRID',
     'Power Grid Corporation of India Ltd', 'NSE', TRUE,
     NOW(), NOW()),

    ('550e8400-e29b-41d4-a716-446655440020', 'HCLTECH',
     'HCL Technologies Ltd', 'NSE', TRUE, NOW(), NOW());


INSERT INTO company
(
    id,
    instrument_id,
    sector,
    industry,
    website,
    description,
    created_at,
    updated_at
)
VALUES
    ('650e8400-e29b-41d4-a716-446655440004',
     '550e8400-e29b-41d4-a716-446655440004',
     'Financial Services', 'Private Sector Bank',
     'https://www.hdfcbank.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440005',
     '550e8400-e29b-41d4-a716-446655440005',
     'Financial Services', 'Private Sector Bank',
     'https://www.icicibank.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440006',
     '550e8400-e29b-41d4-a716-446655440006',
     'Financial Services', 'Public Sector Bank',
     'https://www.sbi.co.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440007',
     '550e8400-e29b-41d4-a716-446655440007',
     'Consumer Goods', 'Personal Products',
     'https://www.hul.co.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440008',
     '550e8400-e29b-41d4-a716-446655440008',
     'Consumer Goods', 'Diversified FMCG',
     'https://www.itcportal.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440009',
     '550e8400-e29b-41d4-a716-446655440009',
     'Telecommunications', 'Telecom Services',
     'https://www.airtel.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440010',
     '550e8400-e29b-41d4-a716-446655440010',
     'Construction', 'Engineering and Construction',
     'https://www.larsentoubro.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440011',
     '550e8400-e29b-41d4-a716-446655440011',
     'Automobile', 'Passenger Vehicles',
     'https://www.marutisuzuki.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440012',
     '550e8400-e29b-41d4-a716-446655440012',
     'Healthcare', 'Pharmaceuticals',
     'https://www.sunpharma.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440013',
     '550e8400-e29b-41d4-a716-446655440013',
     'Consumer Goods', 'Gems and Jewellery',
     'https://www.titancompany.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440014',
     '550e8400-e29b-41d4-a716-446655440014',
     'Consumer Goods', 'Paints',
     'https://www.asianpaints.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440015',
     '550e8400-e29b-41d4-a716-446655440015',
     'Consumer Goods', 'Packaged Foods',
     'https://www.nestle.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440016',
     '550e8400-e29b-41d4-a716-446655440016',
     'Construction Materials', 'Cement',
     'https://www.ultratechcement.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440017',
     '550e8400-e29b-41d4-a716-446655440017',
     'Automobile', 'Commercial Vehicles',
     'https://www.tatamotors.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440018',
     '550e8400-e29b-41d4-a716-446655440018',
     'Metals and Mining', 'Iron and Steel',
     'https://www.tatasteel.com', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440019',
     '550e8400-e29b-41d4-a716-446655440019',
     'Power', 'Power Transmission',
     'https://www.powergrid.in', NULL, NOW(), NOW()),

    ('650e8400-e29b-41d4-a716-446655440020',
     '550e8400-e29b-41d4-a716-446655440020',
     'Information Technology', 'IT Services and Consulting',
     'https://www.hcltech.com', NULL, NOW(), NOW());


COMMENT ON TABLE instrument IS
    'Tradeable instruments. Sector values on the related company row '
    'are seeded from public classification because the fundamentals '
    'provider exposes industry but not sector.';
