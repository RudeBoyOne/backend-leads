CREATE INDEX idx_user_agency           ON "user"(real_estate_agency_id);
CREATE INDEX idx_subscription_agency   ON subscription(real_estate_agency_id);
CREATE INDEX idx_portal_agency         ON portal(real_estate_agency_id);
CREATE INDEX idx_funnel_stage_agency   ON funnel_stage(real_estate_agency_id);

CREATE INDEX idx_lead_agency           ON lead(real_estate_agency_id);
CREATE INDEX idx_lead_agent            ON lead(real_estate_agent_id);
CREATE INDEX idx_lead_portal           ON lead(portal_id);
CREATE INDEX idx_lead_funnel           ON lead(funnel_stage_id);
CREATE INDEX idx_lead_status           ON lead(status);
CREATE INDEX idx_lead_created          ON lead(created_at DESC);

CREATE INDEX idx_interaction_lead      ON lead_interaction(lead_id);
CREATE INDEX idx_interaction_agent     ON lead_interaction(real_estate_agent_id);
CREATE INDEX idx_interaction_datetime  ON lead_interaction(date_time DESC);

CREATE INDEX idx_team_agent_team       ON team_agent(team_id);
CREATE INDEX idx_team_agent_agent      ON team_agent(real_estate_agent_id);
CREATE INDEX idx_team_manager          ON team(manager_id);
