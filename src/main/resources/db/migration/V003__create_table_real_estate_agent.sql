CREATE TABLE portal (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    name                  VARCHAR(100) NOT NULL,
    url                   VARCHAR(255),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE funnel_stage (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID        NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    name                  VARCHAR(100) NOT NULL,
    "order"               SMALLINT    NOT NULL DEFAULT 0,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (real_estate_agency_id, "order")
);