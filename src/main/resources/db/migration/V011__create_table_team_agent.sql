CREATE TABLE team_agent (
    team_id              UUID        NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    real_estate_agent_id UUID        NOT NULL REFERENCES real_estate_agent(id) ON DELETE CASCADE,
    joined_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (team_id, real_estate_agent_id)
);

CREATE INDEX idx_team_agent_team  ON team_agent(team_id);
CREATE INDEX idx_team_agent_agent ON team_agent(real_estate_agent_id);
