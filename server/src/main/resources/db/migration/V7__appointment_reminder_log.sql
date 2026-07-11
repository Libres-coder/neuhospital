-- =============================================================
-- V7: appointment reminder state
-- =============================================================
-- Track when an SMS reminder was actually sent so the scheduled job
-- can skip already-notified patients.

ALTER TABLE appointments
    ADD COLUMN reminder_sent_at BIGINT NULL AFTER reminder_set;

-- The scheduler filters on (reminder_set=1, status IN ('payed','confirmed'),
-- reminder_sent_at IS NULL). A composite index keeps that query cheap.
CREATE INDEX idx_appt_reminder_due
    ON appointments (reminder_set, status, reminder_sent_at);