-- V17: Add family_sprints, sprint_missions, sprint_dailies, and sprint_retrospectives tables

DROP PROCEDURE IF EXISTS CreateAgileSprintsSchema;

DELIMITER //

CREATE PROCEDURE CreateAgileSprintsSchema()
BEGIN
    -- 1. Create family_sprints
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'family_sprints'
    ) THEN
        CREATE TABLE family_sprints (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            family_id BIGINT NOT NULL,
            objective VARCHAR(255) NOT NULL,
            risk_dimension VARCHAR(50) NOT NULL,
            duration_days INT NOT NULL,
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            status VARCHAR(30) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_sprint_family 
                FOREIGN KEY (family_id) 
                REFERENCES families(id) 
                ON DELETE CASCADE
        );
    END IF;

    -- 2. Create sprint_missions
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'sprint_missions'
    ) THEN
        CREATE TABLE sprint_missions (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            sprint_id BIGINT NOT NULL,
            description VARCHAR(500) NOT NULL,
            status VARCHAR(30) NOT NULL,
            completed_at TIMESTAMP NULL,
            CONSTRAINT fk_mission_sprint 
                FOREIGN KEY (sprint_id) 
                REFERENCES family_sprints(id) 
                ON DELETE CASCADE
        );
    END IF;

    -- 3. Create sprint_dailies
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'sprint_dailies'
    ) THEN
        CREATE TABLE sprint_dailies (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            sprint_id BIGINT NOT NULL,
            member_name VARCHAR(120) NOT NULL,
            checkin_date DATE NOT NULL,
            yesterday_text TEXT NOT NULL,
            today_text TEXT NOT NULL,
            blockages_text TEXT NOT NULL,
            resolution_text TEXT NOT NULL,
            emotional_indicator VARCHAR(50) NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_daily_sprint 
                FOREIGN KEY (sprint_id) 
                REFERENCES family_sprints(id) 
                ON DELETE CASCADE
        );
    END IF;

    -- 4. Create sprint_retrospectives
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'sprint_retrospectives'
    ) THEN
        CREATE TABLE sprint_retrospectives (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            sprint_id BIGINT NOT NULL,
            what_went_well TEXT NULL,
            what_was_difficult TEXT NULL,
            what_learned TEXT NULL,
            what_to_adjust TEXT NULL,
            tension_level INT NULL,
            mindful_compliance INT NULL,
            shared_time INT NULL,
            positive_interactions INT NULL,
            emotional_persistence INT NULL,
            consistency_score INT NULL,
            ai_feedback TEXT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_retro_sprint 
                FOREIGN KEY (sprint_id) 
                REFERENCES family_sprints(id) 
                ON DELETE CASCADE
        );
    END IF;

    -- 5. Indexes
    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'family_sprints' 
        AND INDEX_NAME = 'idx_sprint_family_status'
    ) THEN
        CREATE INDEX idx_sprint_family_status ON family_sprints(family_id, status);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'sprint_missions' 
        AND INDEX_NAME = 'idx_mission_sprint_id'
    ) THEN
        CREATE INDEX idx_mission_sprint_id ON sprint_missions(sprint_id);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'sprint_dailies' 
        AND INDEX_NAME = 'idx_daily_sprint_id'
    ) THEN
        CREATE INDEX idx_daily_sprint_id ON sprint_dailies(sprint_id);
    END IF;

END //

DELIMITER ;

CALL CreateAgileSprintsSchema();
DROP PROCEDURE IF EXISTS CreateAgileSprintsSchema;
