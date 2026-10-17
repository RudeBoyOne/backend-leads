CREATE TABLE property (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    branch_office_id        UUID         NOT NULL REFERENCES branch_office(id) ON DELETE CASCADE,
    title                   VARCHAR(150) NOT NULL,
    description             VARCHAR(200),
    price                   NUMERIC(12, 2),
    property_type           VARCHAR(50),
    bedrooms                INT,
    bathrooms               INT,
    area_sqm                NUMERIC(10, 2),
    address                 VARCHAR(200),
    city                    VARCHAR(100),
    state                   VARCHAR(50),
    zip_code                VARCHAR(10),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'available'
                            CHECK (status IN ('available', 'reserved', 'sold', 'rented', 'inactive')),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
);

CREATE INDEX idx_property_agency        ON property(real_estate_agency_id);
CREATE INDEX idx_property_branch_office ON property(branch_office_id);
CREATE INDEX idx_property_status        ON property(status);
