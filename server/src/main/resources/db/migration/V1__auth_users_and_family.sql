-- =============================================================
-- V1: users + family_members (auth module foundation)
-- =============================================================

CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(40)   NOT NULL,
    phone           VARCHAR(20)   NOT NULL,
    name            VARCHAR(64)   NOT NULL,
    is_verified     TINYINT(1)    NOT NULL DEFAULT 0,
    ehs_card_no     VARCHAR(64)   NULL,
    ehs_bound       TINYINT(1)    NOT NULL DEFAULT 0,
    create_time     BIGINT        NOT NULL,
    update_time     BIGINT        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS family_members (
    id          VARCHAR(40) NOT NULL,
    owner_id    VARCHAR(40) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    id_card     VARCHAR(32) NOT NULL,
    relation    VARCHAR(16) NOT NULL,
    avatar      VARCHAR(255) NULL,
    is_default  TINYINT(1)  NOT NULL DEFAULT 0,
    create_time BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_family_owner (owner_id),
    CONSTRAINT fk_family_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;