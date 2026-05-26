-- =====================================================================
-- V16 — Taxonomía Longitudinal v2 en Preguntas y Tareas Clínicas
-- =====================================================================

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_key VARCHAR(255) UNIQUE,
    text VARCHAR(500) NOT NULL,
    dimension VARCHAR(50),
    area VARCHAR(50),
    direction VARCHAR(20) DEFAULT 'POSITIVE',
    version VARCHAR(20) DEFAULT '1.0',
    active BOOLEAN DEFAULT TRUE,
    vertice INT,
    weight INT DEFAULT 1,
    sort_order INT,
    pillar VARCHAR(50),
    phase VARCHAR(50),
    type VARCHAR(50),
    severity_weight DOUBLE,
    detects_relapse BOOLEAN,
    requires_evidence BOOLEAN,
    reverse_question BOOLEAN,
    category VARCHAR(100),
    adaptive_triggers VARCHAR(255),
    evidence_type VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
