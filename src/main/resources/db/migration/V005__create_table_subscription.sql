CREATE TABLE subscription (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID        NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    plan_id                 UUID        NOT NULL REFERENCES plan(id),
    start_date              DATE        NOT NULL,
    end_date                DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'active'
                            CHECK (status IN ('active', 'inactive', 'suspended', 'cancelled')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
