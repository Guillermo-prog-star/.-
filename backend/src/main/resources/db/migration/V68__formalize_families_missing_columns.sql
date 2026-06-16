-- V68: Formalizar columnas de 'families' añadidas via ddl-auto=update
-- sin registro en Flyway. ADD COLUMN IF NOT EXISTS es MariaDB-only;
-- en MySQL 8.x se usa un procedimiento con information_schema.

DROP PROCEDURE IF EXISTS AddFamilyColumnsV68;

DELIMITER //
CREATE PROCEDURE AddFamilyColumnsV68()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'pin') THEN
        ALTER TABLE families ADD COLUMN pin VARCHAR(255) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'pin_hash') THEN
        ALTER TABLE families ADD COLUMN pin_hash VARCHAR(255) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'icf_score') THEN
        ALTER TABLE families ADD COLUMN icf_score INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'total_tasks') THEN
        ALTER TABLE families ADD COLUMN total_tasks INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'completed_tasks') THEN
        ALTER TABLE families ADD COLUMN completed_tasks INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'participation_score') THEN
        ALTER TABLE families ADD COLUMN participation_score INT NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'rotation_enabled') THEN
        ALTER TABLE families ADD COLUMN rotation_enabled TINYINT(1) NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'guardian_member_id') THEN
        ALTER TABLE families ADD COLUMN guardian_member_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'guardian_since') THEN
        ALTER TABLE families ADD COLUMN guardian_since DATETIME NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'milestone_icf_avg') THEN
        ALTER TABLE families ADD COLUMN milestone_icf_avg DOUBLE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'families' AND COLUMN_NAME = 'milestone_started_at') THEN
        ALTER TABLE families ADD COLUMN milestone_started_at DATETIME NULL;
    END IF;
END //
DELIMITER ;

CALL AddFamilyColumnsV68();
DROP PROCEDURE IF EXISTS AddFamilyColumnsV68;
