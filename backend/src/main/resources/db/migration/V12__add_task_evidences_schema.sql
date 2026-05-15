-- V12: Add task_evidences table for Evidence-Driven Transformation Engine

DROP PROCEDURE IF EXISTS CreateTaskEvidencesTable;

DELIMITER //

CREATE PROCEDURE CreateTaskEvidencesTable()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'task_evidences'
    ) THEN
        CREATE TABLE task_evidences (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            task_id BIGINT NOT NULL,
            family_id BIGINT NOT NULL,
            evidence_type VARCHAR(50) NOT NULL,
            status VARCHAR(30) NOT NULL,
            title VARCHAR(255) NULL,
            description TEXT NULL,
            file_url TEXT NULL,
            text_content LONGTEXT NULL,
            submitted_by VARCHAR(120) NOT NULL,
            ai_score DECIMAL(5,2) NULL,
            human_score DECIMAL(5,2) NULL,
            validated BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            validated_at TIMESTAMP NULL,
            CONSTRAINT fk_evidence_task 
                FOREIGN KEY (task_id) 
                REFERENCES plan_tasks(id) 
                ON DELETE CASCADE,
            CONSTRAINT fk_evidence_family 
                FOREIGN KEY (family_id) 
                REFERENCES families(id) 
                ON DELETE CASCADE
        );
    END IF;

    -- Add indexes if they don't exist
    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'task_evidences' 
        AND INDEX_NAME = 'idx_evidence_task_id'
    ) THEN
        CREATE INDEX idx_evidence_task_id ON task_evidences(task_id);
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'task_evidences' 
        AND INDEX_NAME = 'idx_evidence_family_status'
    ) THEN
        CREATE INDEX idx_evidence_family_status ON task_evidences(family_id, status);
    END IF;
END //

DELIMITER ;

CALL CreateTaskEvidencesTable();
DROP PROCEDURE IF EXISTS CreateTaskEvidencesTable;
