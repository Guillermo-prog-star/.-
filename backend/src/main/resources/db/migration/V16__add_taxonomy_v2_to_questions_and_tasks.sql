-- =====================================================================
-- V16 — Taxonomía Longitudinal v2 en Preguntas y Tareas Clínicas
-- =====================================================================

DROP PROCEDURE IF EXISTS EvolveQuestionsAndTasksV16;

DELIMITER //

CREATE PROCEDURE EvolveQuestionsAndTasksV16()
BEGIN
    -- Questions table updates
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'questions' 
        AND COLUMN_NAME = 'pillar_name'
    ) THEN
        ALTER TABLE questions ADD COLUMN pillar_name VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'questions' 
        AND COLUMN_NAME = 'milestone_code'
    ) THEN
        ALTER TABLE questions ADD COLUMN milestone_code VARCHAR(20);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'questions' 
        AND COLUMN_NAME = 'member_type'
    ) THEN
        ALTER TABLE questions ADD COLUMN member_type VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'questions' 
        AND COLUMN_NAME = 'risk_type'
    ) THEN
        ALTER TABLE questions ADD COLUMN risk_type VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'questions' 
        AND COLUMN_NAME = 'mission_generator'
    ) THEN
        ALTER TABLE questions ADD COLUMN mission_generator VARCHAR(100);
    END IF;

    -- Plan Tasks table updates
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'pillar_name'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN pillar_name VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'milestone_code'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN milestone_code VARCHAR(20);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'member_type'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN member_type VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'risk_type'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN risk_type VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'plan_tasks' 
        AND COLUMN_NAME = 'mission_generator'
    ) THEN
        ALTER TABLE plan_tasks ADD COLUMN mission_generator VARCHAR(100);
    END IF;
END //

DELIMITER ;

CALL EvolveQuestionsAndTasksV16();
DROP PROCEDURE IF EXISTS EvolveQuestionsAndTasksV16;
