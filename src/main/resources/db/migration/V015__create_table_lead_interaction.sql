CREATE TABLE lead_interaction (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id              UUID         NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    real_estate_agent_id UUID         REFERENCES real_estate_agent(id) ON DELETE SET NULL,
    type                 VARCHAR(50)  NOT NULL,
    description          TEXT,
    date_time            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interaction_lead      ON lead_interaction(lead_id);
CREATE INDEX idx_interaction_agent     ON lead_interaction(real_estate_agent_id);
CREATE INDEX idx_interaction_datetime  ON lead_interaction(date_time DESC);
