-- V65__make_evidence_task_optional.sql
-- Hace que la columna task_id sea opcional para permitir documentales espontáneos

ALTER TABLE task_evidences MODIFY COLUMN task_id BIGINT NULL;
