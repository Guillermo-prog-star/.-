-- V71: Cuestionarios propios del ICaF — confianza y bienestar_emocional
-- Agrega campos de clasificación ICaF en tabla questions.
-- Crea tabla family_icaf_answers para respuestas per-familia.
-- Compatible con MySQL 8.4 (procedimiento con information_schema).

-- ─── 1. Nuevos campos en questions ───────────────────────────────────────────
-- question_type: ICF | ICAF  (NULL para preguntas legacy sin clasificar)
-- icaf_domain:   confianza | bienestar_emocional | ... (NULL para preguntas ICF)

DROP PROCEDURE IF EXISTS AddIcafQuestionFieldsV71;

DELIMITER //
CREATE PROCEDURE AddIcafQuestionFieldsV71()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'questions'
                     AND COLUMN_NAME = 'question_type') THEN
        ALTER TABLE questions
          ADD COLUMN question_type VARCHAR(20) NULL
            COMMENT 'ICF | ICAF — tipo de cuestionario al que pertenece';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'questions'
                     AND COLUMN_NAME = 'icaf_domain') THEN
        ALTER TABLE questions
          ADD COLUMN icaf_domain VARCHAR(30) NULL
            COMMENT 'Dominio ICaF: confianza | bienestar_emocional | autonomia | ...';
    END IF;
END //
DELIMITER ;

CALL AddIcafQuestionFieldsV71();
DROP PROCEDURE IF EXISTS AddIcafQuestionFieldsV71;

-- ─── 2. family_icaf_answers ───────────────────────────────────────────────────
-- Respuestas per-familia a los cuestionarios ICaF (confianza, bienestar, ...).
-- Una fila por (family_id, question_key): se actualiza en cada nueva respuesta.
-- answered_by: email del miembro que responde (nullable → respuesta familiar conjunta).

CREATE TABLE IF NOT EXISTS family_icaf_answers (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  family_id       BIGINT        NOT NULL,
  question_key    VARCHAR(100)  NOT NULL COMMENT 'Clave única de la pregunta (ej. ICAF_CONF_001)',
  icaf_domain     VARCHAR(30)   NOT NULL COMMENT 'Dominio al que pertenece la respuesta',
  score           TINYINT       NOT NULL COMMENT 'Respuesta en escala 1-5',
  answered_by     VARCHAR(120)  NULL     COMMENT 'Email del miembro respondiente (NULL = familia)',
  answered_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_family_question (family_id, question_key),
  KEY idx_fia_family        (family_id),
  KEY idx_fia_domain        (family_id, icaf_domain),
  CONSTRAINT fk_fia_family FOREIGN KEY (family_id)
    REFERENCES families (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
