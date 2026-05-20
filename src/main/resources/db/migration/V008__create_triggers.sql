CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_plan_updated_at
    BEFORE UPDATE ON plan
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_agency_updated_at
    BEFORE UPDATE ON real_estate_agency
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_subscription_updated_at
    BEFORE UPDATE ON subscription
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON "user"
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_manager_updated_at
    BEFORE UPDATE ON manager
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_team_updated_at
    BEFORE UPDATE ON team
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_agent_updated_at
    BEFORE UPDATE ON real_estate_agent
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_portal_updated_at
    BEFORE UPDATE ON portal
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_funnel_stage_updated_at
    BEFORE UPDATE ON funnel_stage
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_lead_updated_at
    BEFORE UPDATE ON lead
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
