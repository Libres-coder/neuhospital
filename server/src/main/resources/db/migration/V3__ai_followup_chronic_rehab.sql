-- =============================================================
-- V3: ai_chat + preconsult + followup + chronic + rehab
-- =============================================================

CREATE TABLE IF NOT EXISTS chat_sessions (
    id           VARCHAR(40) NOT NULL,
    patient_id   VARCHAR(40) NOT NULL,
    title        VARCHAR(128) NOT NULL,
    last_message VARCHAR(255) NULL,
    create_time  BIGINT      NOT NULL,
    last_time    BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_patient (patient_id),
    CONSTRAINT fk_chat_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    session_id  VARCHAR(40) NOT NULL,
    role        VARCHAR(16) NOT NULL,  -- user / assistant
    content     TEXT        NOT NULL,
    create_time BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_msg_session (session_id),
    CONSTRAINT fk_msg_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS follow_up_plans (
    id           VARCHAR(40) NOT NULL,
    patient_id   VARCHAR(40) NOT NULL,
    disease      VARCHAR(64) NOT NULL,
    surgery_date VARCHAR(10) NOT NULL,
    total_days   INT         NOT NULL,
    create_time  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_plan_patient (patient_id),
    CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS follow_up_tasks (
    id             VARCHAR(40) NOT NULL,
    plan_id        VARCHAR(40) NOT NULL,
    patient_id     VARCHAR(40) NOT NULL,
    day_index      INT         NOT NULL,
    target_date    VARCHAR(10) NOT NULL,
    questions_json TEXT        NOT NULL,
    answers_json   TEXT        NULL,
    completed      TINYINT(1)  NOT NULL DEFAULT 0,
    completed_time BIGINT      NULL,
    doctor_reply   TEXT        NULL,
    PRIMARY KEY (id),
    KEY idx_task_plan (plan_id),
    KEY idx_task_patient_date (patient_id, target_date),
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES follow_up_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chronic_records (
    id                  VARCHAR(40) NOT NULL,
    patient_id          VARCHAR(40) NOT NULL,
    type                VARCHAR(16) NOT NULL,  -- HYPERTENSION / DIABETES
    record_date         VARCHAR(10) NOT NULL,
    systolic            INT         NULL,
    diastolic           INT         NULL,
    heart_rate          INT         NULL,
    fasting_glucose     DECIMAL(6,2) NULL,
    postprandial_glucose DECIMAL(6,2) NULL,
    hba1c               DECIMAL(4,2) NULL,
    note                VARCHAR(255) NULL,
    alert_level         INT         NOT NULL DEFAULT 0,
    create_time         BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chr_patient_type_date (patient_id, type, record_date),
    CONSTRAINT fk_chr_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chronic_alerts (
    id            VARCHAR(40) NOT NULL,
    patient_id    VARCHAR(40) NOT NULL,
    record_id     VARCHAR(40) NOT NULL,
    type          VARCHAR(16) NOT NULL,
    level         INT         NOT NULL,
    message       VARCHAR(255) NOT NULL,
    create_time   BIGINT      NOT NULL,
    acknowledged  TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_alert_patient (patient_id),
    CONSTRAINT fk_alert_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rehab_logs (
    id           VARCHAR(40) NOT NULL,
    patient_id   VARCHAR(40) NOT NULL,
    disease      VARCHAR(64) NOT NULL,
    log_date     VARCHAR(10) NOT NULL,
    items_json   TEXT        NOT NULL,
    completed    TINYINT(1)  NOT NULL DEFAULT 0,
    create_time  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rehab_patient_date (patient_id, log_date),
    CONSTRAINT fk_rehab_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;