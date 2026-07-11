-- =============================================================
-- V2: appointment (departments, doctors, schedules, appointments)
-- =============================================================

CREATE TABLE IF NOT EXISTS departments (
    id           VARCHAR(40)  NOT NULL,
    parent_id    VARCHAR(40)  NULL,
    name         VARCHAR(64)  NOT NULL,
    name_py      VARCHAR(64)  NULL,
    description  VARCHAR(255) NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dept_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctors (
    id              VARCHAR(40)   NOT NULL,
    name            VARCHAR(64)   NOT NULL,
    department_id   VARCHAR(40)   NOT NULL,
    department_name VARCHAR(64)   NOT NULL,
    title           VARCHAR(32)   NOT NULL,
    expertise       VARCHAR(255)  NULL,
    profile         TEXT          NULL,
    rating          DECIMAL(2,1)  NOT NULL DEFAULT 4.5,
    avatar_url      VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    KEY idx_doctor_dept (department_id),
    CONSTRAINT fk_doctor_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- schedules: per-doctor per-day summary (for calendar grid)
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id            VARCHAR(40) NOT NULL,
    doctor_id     VARCHAR(40) NOT NULL,
    schedule_date VARCHAR(10) NOT NULL,  -- yyyy-MM-dd
    day_of_week   VARCHAR(8)  NOT NULL,
    morning_total INT         NOT NULL DEFAULT 0,
    morning_left  INT         NOT NULL DEFAULT 0,
    afternoon_total INT       NOT NULL DEFAULT 0,
    afternoon_left  INT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_date (doctor_id, schedule_date),
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- appointments
CREATE TABLE IF NOT EXISTS appointments (
    id              VARCHAR(40)   NOT NULL,
    patient_id      VARCHAR(40)   NOT NULL,
    patient_name    VARCHAR(64)   NOT NULL,
    doctor_id       VARCHAR(40)   NOT NULL,
    doctor_name     VARCHAR(64)   NOT NULL,
    department_id   VARCHAR(40)   NOT NULL,
    department_name VARCHAR(64)   NOT NULL,
    appointment_date VARCHAR(10)  NOT NULL,
    time_slot       VARCHAR(16)   NOT NULL,  -- e.g. "08:00-08:15"
    duration        INT           NOT NULL,
    status          VARCHAR(16)   NOT NULL,  -- pending/payed/confirmed/completed/cancelled/no_show
    reminder_set    TINYINT(1)    NOT NULL DEFAULT 0,
    create_time     BIGINT        NOT NULL,
    update_time     BIGINT        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_appt_patient (patient_id),
    KEY idx_appt_doctor_date (doctor_id, appointment_date),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctors(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;