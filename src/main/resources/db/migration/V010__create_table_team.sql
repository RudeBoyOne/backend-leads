CREATE TABLE team (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id       UUID         NOT NULL REFERENCES manager(id) ON DELETE CASCADE,
    branch_office_id UUID         REFERENCES branch_office(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_team_manager      ON team(manager_id);
CREATE INDEX idx_team_branch_office ON team(branch_office_id);
