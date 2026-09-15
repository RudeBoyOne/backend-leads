CREATE TABLE property (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id UUID          NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    title                 VARCHAR(150)  NOT NULL,
    description           TEXT,
    price                 NUMERIC(12,2),
    property_type         VARCHAR(50),
    bedrooms              INT,
    bathrooms             INT,
    area_sqm              NUMERIC(10,2),
    address               VARCHAR(255),
    city                  VARCHAR(100),
    state                 VARCHAR(2),
    zip_code              VARCHAR(10),
    status                VARCHAR(30)   NOT NULL DEFAULT 'available'
                          CHECK (status IN ('available', 'reserved', 'sold', 'rented', 'inactive')),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_property_real_estate_agency_id ON property(real_estate_agency_id);
CREATE INDEX idx_property_status ON property(status);