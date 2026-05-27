CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON "user"
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_agent_updated_at
    BEFORE UPDATE ON real_estate_agent
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_manager_updated_at
    BEFORE UPDATE ON manager
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();