ALTER TABLE activity
    ADD COLUMN branch_owner_id UUID REFERENCES branch_owner(id) ON DELETE SET NULL,
    ADD COLUMN property_id    UUID REFERENCES property(id)    ON DELETE SET NULL;

CREATE INDEX idx_activity_branch_owner_id ON activity(branch_owner_id);
CREATE INDEX idx_activity_property_id    ON activity(property_id);