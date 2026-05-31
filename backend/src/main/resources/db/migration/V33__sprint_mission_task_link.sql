-- V33: Vincula sprint con el PlanTask que lo originó
-- Permite trazar qué misión del plan de transformación generó cada sprint.

ALTER TABLE family_sprints
    ADD COLUMN IF NOT EXISTS mission_task_id BIGINT DEFAULT NULL;

COMMENT ON COLUMN family_sprints.mission_task_id
    IS 'ID del plan_task (misión del plan de transformación) que auto-generó este sprint. NULL si fue creado manualmente.';
