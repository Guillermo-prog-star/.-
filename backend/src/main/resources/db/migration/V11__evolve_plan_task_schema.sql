-- V11: Evolve plan_tasks table to include additional longitudinal and structured transformation fields

DROP PROCEDURE IF EXISTS EvolvePlanTasks;

DELIMITER //

CREATE PROCEDURE EvolvePlanTasks()
BEGIN
    -- Check if milestone_id column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'milestone_id'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN milestone_id BIGINT;
    END IF;

    -- Check if fase column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'fase'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN fase VARCHAR(50);
    END IF;

    -- Check if riesgo_asociado column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'riesgo_asociado'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN riesgo_asociado VARCHAR(50);
    END IF;

    -- Check if objetivo column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'objetivo'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN objetivo TEXT;
    END IF;

    -- Check if accion_concreta column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'accion_concreta'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN accion_concreta TEXT;
    END IF;

    -- Check if indicador_cumplimiento column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'indicador_cumplimiento'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN indicador_cumplimiento TEXT;
    END IF;

    -- Check if evidencia_requerida column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'evidencia_requerida'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN evidencia_requerida TEXT;
    END IF;

    -- Check if impacto_icf column exists
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'impacto_icf'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN impacto_icf INT;
    END IF;

    -- Note: fk_task_milestone is managed automatically by Hibernate ddl-auto to point to milestone_definitions
    SELECT 'fk_task_milestone managed by hibernate' AS status;
END //

DELIMITER ;

CALL EvolvePlanTasks();
DROP PROCEDURE IF EXISTS EvolvePlanTasks;

