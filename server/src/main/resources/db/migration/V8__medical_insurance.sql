-- =============================================================
-- V8: medical insurance e-voucher (医保电子凭证)
-- =============================================================
-- Distinct from EHS (electronic health card). The 医保凭证 number is
-- typically the user's ID-card number or a separate 20-digit code;
-- we store whichever the client provides. `mi_bound` flips when the
-- user successfully completes the (mock) OAuth handshake with the
-- 国家医保局 app.

ALTER TABLE users
    ADD COLUMN medical_insurance_no VARCHAR(64) NULL AFTER ehs_bound,
    ADD COLUMN mi_bound              TINYINT(1) NOT NULL DEFAULT 0 AFTER medical_insurance_no,
    ADD COLUMN mi_bound_time         BIGINT      NULL AFTER mi_bound;