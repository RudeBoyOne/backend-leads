CREATE TABLE role (
    id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO role (name) VALUES
    ('FULL_ADMIN'),
    ('ADMIN'),
    ('MANAGER'),
    ('REAL_ESTATE_AGENT'),
    ('TEAM_AGENT'),
    ('USER');

CREATE TABLE plan (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL,
    max_users              INTEGER      NOT NULL DEFAULT 0,
    max_leads_month        INTEGER      NOT NULL DEFAULT 0,
    max_real_estate_agents INTEGER      NOT NULL DEFAULT 0,
    monthly_value          NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE real_estate_agency (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    cnpj       VARCHAR(18)  NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,    
    phone      VARCHAR(20),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID        NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    plan_id               UUID        NOT NULL REFERENCES plan(id),
    start_date            DATE        NOT NULL,
    end_date              DATE,
    status                VARCHAR(20) NOT NULL DEFAULT 'active'
                          CHECK (status IN ('active', 'inactive', 'suspended', 'cancelled')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);