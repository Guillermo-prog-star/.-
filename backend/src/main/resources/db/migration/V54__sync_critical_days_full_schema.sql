-- V54__sync_critical_days_full_schema.sql
-- Sincroniza la tabla critical_days con la entidad CriticalDay.java
-- Agrega todas las columnas faltantes en una sola migración.

ALTER TABLE critical_days
    ADD COLUMN IF NOT EXISTS member_id    BIGINT       NULL,
    ADD COLUMN IF NOT EXISTS description  TEXT         NULL,
    ADD COLUMN IF NOT EXISTS main_emotion VARCHAR(100) NULL;
