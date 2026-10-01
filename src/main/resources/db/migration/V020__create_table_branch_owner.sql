CREATE TABLE branch_owner (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID          NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    name                  VARCHAR(150)  NOT NULL,
    email                 VARCHAR(150),
    phone                 VARCHAR(20),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_branch_owner_real_estate_agency_id ON branch_owner(real_estate_agency_id);