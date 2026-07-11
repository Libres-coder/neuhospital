-- One-shot init for the MySQL container. Runs on first boot only.
-- 1) Make sure the schema exists with utf8mb4.
-- 2) Make sure the application user has full rights on it (Flyway DDL + JPA).

CREATE DATABASE IF NOT EXISTS neusoft_hospital
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- The MYSQL_USER / MYSQL_PASSWORD env vars already provision this user on
-- first boot, but in case someone overrides MYSQL_USER we re-grant here.
-- We can't reference ${MYSQL_USER} from SQL, so we resolve at runtime via a
-- CREATE USER IF NOT EXISTS guarded by dynamic SQL would require mysql shell,
-- so keep it simple: grant on the canonical dev user. Operators editing
-- production secrets should swap these credentials.

GRANT ALL PRIVILEGES ON neusoft_hospital.* TO 'hospital'@'%';
GRANT ALL PRIVILEGES ON neusoft_hospital.* TO 'root'@'%';
FLUSH PRIVILEGES;