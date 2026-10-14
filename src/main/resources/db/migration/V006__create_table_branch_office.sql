CREATE TABLE branch_office (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    name                    VARCHAR(150) NOT NULL,
    address                 VARCHAR(255),
    phone                   VARCHAR(20),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    UNIQUE (real_estate_agency_id, name)
);

CREATE INDEX idx_branch_office_agency ON branch_office(real_estate_agency_id);
