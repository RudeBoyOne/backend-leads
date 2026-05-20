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