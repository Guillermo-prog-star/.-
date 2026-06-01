-- V33: Vincula sprint con el PlanTask que lo originó
-- Permite trazar qué misión del plan de transformación generó cada sprint.
-- Dialecto: MySQL 8.4
-- Nota: MySQL no soporta ADD COLUMN IF NOT EXISTS ni COMMENT ON COLUMN.
--       El COMMENT se declara inline en la definición de columna.

ALTER TABLE family_sprints
    ADD COLUMN mission_task_id BIGINT DEFAULT NULL
        COMMENT 'ID del plan_task (misión del plan de transformación) que auto-generó este sprint. NULL si fue creado manualmente.';
