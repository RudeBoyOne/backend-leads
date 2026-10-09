ALTER TABLE activity
    ALTER COLUMN activity_date TYPE TIMESTAMP,
    ALTER COLUMN created_at   TYPE TIMESTAMP;

ALTER TABLE activity DROP CONSTRAINT IF EXISTS activity_branch_owner_id_fkey;
DROP INDEX IF EXISTS idx_activity_branch_owner_id;
ALTER TABLE activity DROP COLUMN IF EXISTS branch_owner_id;