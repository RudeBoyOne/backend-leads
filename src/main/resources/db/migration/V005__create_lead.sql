CREATE TABLE lead (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    real_estate_agent_id    UUID REFERENCES real_estate_agent(id) ON DELETE SET NULL,
    portal_id               UUID REFERENCES portal(id) ON DELETE SET NULL,
    funnel_stage_id         UUID REFERENCES funnel_stage(id) ON DELETE SET NULL,
    origin_lead_id          VARCHAR(100),
    origin_listing_id       VARCHAR(100),
    lead_origin             VARCHAR(255),
    client_listing_id       VARCHAR(100),
    name                    VARCHAR(150) NOT NULL,
    email                   VARCHAR(150),
    phone_number            VARCHAR(20),
    message                 TEXT,
    temperature             VARCHAR(20),
    transaction_type        VARCHAR(50),
    source                  VARCHAR(100),
    status                  VARCHAR(30) NOT NULL DEFAULT 'new'
                            CHECK (status IN ('new', 'in_progress', 'converted', 'lost', 'archived')),
    timestamp               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
