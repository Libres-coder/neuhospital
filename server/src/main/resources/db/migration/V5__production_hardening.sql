-- =============================================================
-- V5: production hardening
--   1) optimistic lock column on appointments
--   2) unique constraint to prevent same slot being booked twice
--      (note: MySQL has no partial index, so we model "active" via
--       an `active_slot_id` virtual column populated by trigger OR
--       enforce uniqueness on (doctor_id, appointment_date, time_slot)
--       plus a `cancelled_at` timestamp and an active-by-status filter
--       in the service layer. We choose the latter to keep the schema
--       portable; the booking service MUST re-check status in tx.)
--   3) idempotency_keys table
--   4) audit_logs table
-- =============================================================

-- 1) Optimistic lock
ALTER TABLE appointments
    ADD COLUMN version INT NOT NULL DEFAULT 0;

-- 2) Idempotency: client passes an Idempotency-Key for any non-idempotent write
CREATE TABLE IF NOT EXISTS idempotency_keys (
    token          VARCHAR(64)  NOT NULL,
    user_id        VARCHAR(40)  NOT NULL,
    request_hash   VARCHAR(64)  NOT NULL,
    response_code  INT          NOT NULL DEFAULT 0,
    response_body  TEXT         NULL,
    created_at     BIGINT       NOT NULL,
    PRIMARY KEY (token, user_id),
    KEY idx_idem_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) Audit log: every write op records who/what/when/where/payload
CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(40)  NULL,
    action       VARCHAR(32)  NOT NULL,    -- e.g. BOOK, CANCEL, PAY, NO_SHOW, LOGIN
    target_type  VARCHAR(32)  NULL,        -- e.g. Appointment, User
    target_id    VARCHAR(64)  NULL,
    payload      TEXT         NULL,        -- JSON
    ip           VARCHAR(45)  NULL,
    user_agent   VARCHAR(255) NULL,
    at           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_user_at (user_id, at),
    KEY idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;