CREATE TABLE activity (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id       UUID        NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    title         VARCHAR(150) NOT NULL,
    description   TEXT        NOT NULL,
    activity_date TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_lead_id ON activity(lead_id);