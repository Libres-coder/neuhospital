-- =============================================================
-- V6: family_member alignment + audit columns
-- =============================================================
-- The V1 table missed `update_time`. Backfill nullable so this migration
-- is safe to apply against any existing rows.

ALTER TABLE family_members
    ADD COLUMN update_time BIGINT NULL AFTER create_time;

-- Index so the "list my family members" endpoint stays snappy even at scale.
CREATE INDEX idx_family_owner_default ON family_members (owner_id, is_default);