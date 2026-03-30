-- JWT auth users table setup
-- Note: Flyway dependency is not currently enabled in build.gradle.
-- Run this script manually if Flyway is not used.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id BIGINT UNIQUE,
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'enabled'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE users RENAME COLUMN enabled TO is_active;
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_employee' AND table_name = 'users'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'users_email_key' AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'users_employee_id_key' AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_employee_id_key UNIQUE (employee_id);
    END IF;
END $$;

UPDATE users SET role = 'SELLER' WHERE role = 'EMPLOYEE';

INSERT INTO users (username, password, role, email, full_name, created_at, is_active)
SELECT
    'admin',
    crypt('admin123', gen_salt('bf')),
    'ADMIN',
    'admin@liriosbeauty.local',
    'System Admin',
    NOW(),
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
