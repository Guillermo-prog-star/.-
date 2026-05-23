-- V20: Sincronización y persistencia de contexto diagnóstico adaptativo
-- Agrega columnas para registrar la dimensión específica y el nivel de conciencia
-- en el que se guardó cada respuesta en la tabla evaluation_answers, con migración segura de datos.

DROP PROCEDURE IF EXISTS MigrateEvaluationAnswersV20;

DELIMITER //

CREATE PROCEDURE MigrateEvaluationAnswersV20()
BEGIN
    -- 1. Agregar columna diagnostic_dimension si no existe
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND COLUMN_NAME  = 'diagnostic_dimension'
    ) THEN
        ALTER TABLE evaluation_answers 
        ADD COLUMN diagnostic_dimension VARCHAR(50) NULL DEFAULT NULL COMMENT 'Dimensión específica del diagnóstico adaptativo v2';
    END IF;

    -- 2. Agregar columna consciousness_level si no existe
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'evaluation_answers'
          AND COLUMN_NAME  = 'consciousness_level'
    ) THEN
        ALTER TABLE evaluation_answers 
        ADD COLUMN consciousness_level VARCHAR(30) NULL DEFAULT NULL COMMENT 'Etiqueta del nivel de conciencia en primera persona';
    END IF;

    -- 3. Actualización retroactiva de diagnostic_dimension cruzando con la tabla de preguntas
    UPDATE evaluation_answers ea
    INNER JOIN questions q ON (ea.question_id = q.id OR ea.question_key = q.question_key)
    SET ea.diagnostic_dimension = q.dimension
    WHERE ea.diagnostic_dimension IS NULL;

    -- 4. Actualización retroactiva de consciousness_level basada en el score 1-5
    UPDATE evaluation_answers
    SET consciousness_level = CASE
        WHEN score = 1 THEN 'Inconsciente'
        WHEN score = 2 THEN 'Reactivo'
        WHEN score = 3 THEN 'Consciente'
        WHEN score = 4 THEN 'Intencional'
        WHEN score = 5 THEN 'Pleno'
        ELSE 'Consciente'
    END
    WHERE consciousness_level IS NULL;

END //

DELIMITER ;

CALL MigrateEvaluationAnswersV20();
DROP PROCEDURE IF EXISTS MigrateEvaluationAnswersV20;
