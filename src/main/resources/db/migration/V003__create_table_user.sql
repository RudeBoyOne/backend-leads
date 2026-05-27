CREATE TABLE users (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID         REFERENCES real_estate_agency(id) ON DELETE SET NULL,
    role_id               UUID         NOT NULL REFERENCES role(id),
    name                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);