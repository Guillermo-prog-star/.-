-- =====================================================================
-- V18 — Tracking temporal del hito actual por familia
-- =====================================================================
-- Añade milestone_started_at para medir cuánto tiempo lleva la familia
-- en su hito actual. Necesario para el avance automático por tiempo.

DROP PROCEDURE IF EXISTS AddMilestoneTrackingV18;

DELIMITER //

CREATE PROCEDURE AddMilestoneTrackingV18()
BEGIN
    -- Columna: cuándo empezó la familia el hito actual
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'families'
          AND COLUMN_NAME  = 'milestone_started_at'
    ) THEN
        ALTER TABLE families ADD COLUMN milestone_started_at DATETIME NULL;
        -- Familias existentes: inicializar con created_at como proxy
        UPDATE families
        SET milestone_started_at = COALESCE(created_at, NOW())
        WHERE milestone_started_at IS NULL;
    END IF;

    -- Columna: ICF mínimo acumulado para avanzar (calculado y cacheado)
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'families'
          AND COLUMN_NAME  = 'milestone_icf_avg'
    ) THEN
        ALTER TABLE families ADD COLUMN milestone_icf_avg DOUBLE NULL;
    END IF;
END //

DELIMITER ;

CALL AddMilestoneTrackingV18();
DROP PROCEDURE IF EXISTS AddMilestoneTrackingV18;
