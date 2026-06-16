-- V67__add_documentary_id_to_evidences.sql
-- Relaciona las evidencias sueltas (fotos, audios, notas) con un documental consolidador

ALTER TABLE task_evidences ADD COLUMN documentary_id BIGINT NULL;
ALTER TABLE task_evidences ADD CONSTRAINT fk_evidence_documentary FOREIGN KEY (documentary_id) REFERENCES family_documentaries(id) ON DELETE SET NULL;
