CREATE TABLE lead (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    real_estate_agency_id   UUID         NOT NULL REFERENCES real_estate_agency(id) ON DELETE CASCADE,
    real_estate_agent_id    UUID         REFERENCES real_estate_agent(id) ON DELETE SET NULL,
    portal_id               UUID         REFERENCES portal(id) ON DELETE SET NULL,
    funnel_stage_id         UUID         REFERENCES funnel_stage(id) ON DELETE SET NULL,
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
    insight_id              VARCHAR(255),
    status                  VARCHAR(30)  NOT NULL DEFAULT 'new'
                            CHECK (status IN ('new', 'in_progress', 'converted', 'lost', 'archived')),
    timestamp               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lead_agency   ON lead(real_estate_agency_id);
CREATE INDEX idx_lead_agent    ON lead(real_estate_agent_id);
CREATE INDEX idx_lead_portal   ON lead(portal_id);
CREATE INDEX idx_lead_funnel   ON lead(funnel_stage_id);
CREATE INDEX idx_lead_status   ON lead(status);
CREATE INDEX idx_lead_created  ON lead(created_at DESC);
