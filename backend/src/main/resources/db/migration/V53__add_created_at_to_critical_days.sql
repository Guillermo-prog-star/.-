-- V53__add_created_at_to_critical_days.sql
-- Añade la columna `created_at` a la tabla `critical_days` para registrar la fecha de creación.
-- La columna se inicializa con la marca de tiempo actual del servidor.

ALTER TABLE critical_days
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
