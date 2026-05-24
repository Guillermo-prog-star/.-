-- V19: Respuestas incrementales de evaluación
-- Soporte para guardar respuestas una a una durante el cuestionario (mobile-first)
-- y retomar donde se dejó si el usuario cierra la app.

DROP PROCEDURE IF EXISTS MigrateEvaluationAnswersV19;

DELIMITER //

CREATE PROCEDURE MigrateEvaluationAnswersV19()
BEGIN
    -- 1. Eliminar filas duplicadas (evaluation_id + question_key) que pudieran existir
    --    Conserva sólo la fila con el id más alto (última escrita) por par.
    DELETE ea1
    FROM evaluation_answers ea1
    INNER JOIN evaluation_answers ea2
      ON  ea1.evaluation_id = ea2.evaluation_id
      AND ea1.question_key  = ea2.question_key
      AND ea1.id            < ea2.id;

    -- 2. Agregar columnas si no existen
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND COLUMN_NAME  = 'answered_at'
    ) THEN
        ALTER TABLE evaluation_answers ADD COLUMN answered_at DATETIME NULL DEFAULT NULL COMMENT 'Momento en que se guardó esta respuesta';
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND COLUMN_NAME  = 'question_id'
    ) THEN
        ALTER TABLE evaluation_answers ADD COLUMN question_id BIGINT NULL DEFAULT NULL COMMENT 'FK a questions.id (resolución de question_key)';
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND COLUMN_NAME  = 'boolean_answer'
    ) THEN
        ALTER TABLE evaluation_answers ADD COLUMN boolean_answer TINYINT(1) NULL DEFAULT NULL COMMENT 'Respuesta Sí/No (1=Sí, 0=No); NULL si no aplica';
    END IF;

    -- 3. FK a questions (opcional — no bloquea si la pregunta fue borrada)
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND CONSTRAINT_NAME = 'fk_ans_question'
    ) THEN
        ALTER TABLE evaluation_answers
        ADD CONSTRAINT fk_ans_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL;
    END IF;

    -- 4. Constraint de unicidad: una respuesta por pregunta por evaluación
    --    Garantiza que el upsert incremental no cree duplicados.
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND CONSTRAINT_NAME = 'uq_ans_eval_question'
    ) THEN
        ALTER TABLE evaluation_answers
        ADD CONSTRAINT uq_ans_eval_question
        UNIQUE (evaluation_id, question_key);
    END IF;

END //

DELIMITER ;

CALL MigrateEvaluationAnswersV19();
DROP PROCEDURE IF EXISTS MigrateEvaluationAnswersV19;
