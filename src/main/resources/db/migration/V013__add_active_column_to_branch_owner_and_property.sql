ALTER TABLE branch_owner ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE property ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_branch_owner_active ON branch_owner(active);
CREATE INDEX idx_property_active ON property(active);