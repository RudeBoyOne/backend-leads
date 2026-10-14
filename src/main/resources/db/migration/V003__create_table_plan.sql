CREATE TABLE plan (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL,
    max_users               INTEGER      NOT NULL DEFAULT 0,
    max_leads_month         INTEGER      NOT NULL DEFAULT 0,
    max_real_estate_agents  INTEGER      NOT NULL DEFAULT 0,
    monthly_value           NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);