-- V58: Corregir campos boolean primitivos NULL en tabla questions
-- Los campos boolean primitivos en Java no pueden ser NULL en BD (NullPointerException)
UPDATE questions SET detects_relapse  = FALSE WHERE detects_relapse  IS NULL;
UPDATE questions SET requires_evidence = FALSE WHERE requires_evidence IS NULL;
UPDATE questions SET reverse_question  = FALSE WHERE reverse_question  IS NULL;
UPDATE questions SET active = TRUE WHERE active IS NULL;
UPDATE questions SET vertice = 0 WHERE vertice IS NULL;

ALTER TABLE questions MODIFY COLUMN detects_relapse   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE questions MODIFY COLUMN requires_evidence  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE questions MODIFY COLUMN reverse_question   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE questions MODIFY COLUMN active             BOOLEAN NOT NULL DEFAULT TRUE;
