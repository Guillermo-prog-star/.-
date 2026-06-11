-- ══════════════════════════════════════════════════════════════════════════
-- V63 — Columnas de configuración generacional en family_lineages
--
-- Garantiza que family_lineages tenga los campos del modelo de evolución
-- en instalaciones limpias donde V62 solo agregó columnas de lineage_members.
-- Usa IGNORE para ser idempotente si ya existen (instalaciones existentes).
-- ══════════════════════════════════════════════════════════════════════════

-- MySQL no soporta ADD COLUMN IF NOT EXISTS directamente.
-- Usamos un procedimiento temporal para verificar antes de alterar.

DROP PROCEDURE IF EXISTS add_lineage_col_if_missing;

DELIMITER $$
CREATE PROCEDURE add_lineage_col_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'family_lineages'
      AND COLUMN_NAME  = 'anchor_generation'
  ) THEN
    ALTER TABLE family_lineages
      ADD COLUMN anchor_generation  INT          NOT NULL DEFAULT  0    COMMENT '0=Responsable, negativo=pasado, positivo=futuro',
      ADD COLUMN max_past_gen       INT          NOT NULL DEFAULT -2    COMMENT 'Generacion mas antigua registrada',
      ADD COLUMN max_future_gen     INT          NOT NULL DEFAULT  2    COMMENT 'Generacion mas futura proyectada',
      ADD COLUMN vision_statement   TEXT                               COMMENT 'Vision del legado familiar',
      ADD COLUMN founding_year      VARCHAR(10)                        COMMENT 'Año aprox. de la generacion mas antigua';
  END IF;
END$$
DELIMITER ;

CALL add_lineage_col_if_missing();
DROP PROCEDURE IF EXISTS add_lineage_col_if_missing;
