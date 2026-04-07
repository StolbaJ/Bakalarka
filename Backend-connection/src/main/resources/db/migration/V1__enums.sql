-- OrderPriority
CREATE TYPE order_priority AS ENUM (
    'NIZKA',
    'STREDNI',
    'VYSOKA',
    'KRITICKA'
);

-- OrderSource
CREATE TYPE order_source AS ENUM (
    'SHOPTET',
    'FRONTEND'
);

-- OrderStatus
CREATE TYPE order_status AS ENUM (
    'QUEUED',
    'SYNCED',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED'
);

-- SkiCondition
CREATE TYPE ski_condition AS ENUM (
    'VYORNY',
    'DOBRY',
    'STREDNI',
    'SPATNY'
);

-- SkiStatus
CREATE TYPE ski_status AS ENUM (
    'DOSTUPNY',
    'V_SERVISU',
    'REZERVOVANO',
    'NEDOSTUPNY',
    'PROVIZORNI'
);