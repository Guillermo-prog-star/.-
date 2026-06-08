-- V55__fix_critical_days_schema.sql
-- Adds missing columns to critical_days table to satisfy entity mapping.

ALTER TABLE critical_days
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS description TEXT NULL,
    ADD COLUMN IF NOT EXISTS main_emotion VARCHAR(100) NULL;
