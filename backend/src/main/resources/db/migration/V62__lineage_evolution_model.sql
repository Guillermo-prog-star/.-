-- ══════════════════════════════════════════════════════════════════════════
-- V62 — Árbol de Evolución y Legado Familiar (MySQL 8)
-- NOTA: family_lineages ya fue alterada en ejecución parcial anterior.
--       Solo se agregan campos de lineage_members y la nueva tabla.
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. LINEAGE_MEMBERS: campos de evolución y tipo generacional ───────────
ALTER TABLE lineage_members
  ADD COLUMN generation_type    VARCHAR(30)  COMMENT 'founding|builder|responsible|current|future|projected',
  ADD COLUMN is_anchor          TINYINT(1)   NOT NULL DEFAULT 0   COMMENT '1 si es el nodo ancla del árbol',
  ADD COLUMN valores            TEXT         COMMENT 'Valores que esta persona aportó o heredó',
  ADD COLUMN aprendizajes       TEXT         COMMENT 'Aprendizajes clave de vida',
  ADD COLUMN errores_superados  TEXT         COMMENT 'Errores o traumas superados',
  ADD COLUMN tradiciones        TEXT         COMMENT 'Tradiciones que inició o preservó',
  ADD COLUMN misiones_cumplidas TEXT         COMMENT 'Logros y misiones familiares cumplidas',
  ADD COLUMN legado_personal    TEXT         COMMENT 'Legado específico que dejó o dejará';

-- ── 2. LINEAGE_GENERATION_INFO: narrativa por nivel generacional ──────────
CREATE TABLE lineage_generation_info (
  id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  lineage_id       BIGINT       NOT NULL,
  generation_level INT          NOT NULL   COMMENT '-3=tatarabuelos ... 0=responsable ... +3=bisnietos',
  generation_type  VARCHAR(30)  NOT NULL DEFAULT 'responsible',
  title            VARCHAR(120),
  summary          TEXT,
  context          TEXT,
  key_challenge    TEXT,
  key_achievement  TEXT,
  period_start     VARCHAR(10),
  period_end       VARCHAR(10),
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_lgi_lineage FOREIGN KEY (lineage_id) REFERENCES family_lineages(id) ON DELETE CASCADE,
  CONSTRAINT uq_lineage_gen UNIQUE (lineage_id, generation_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. Índices ─────────────────────────────────────────────────────────────
CREATE INDEX idx_lgi_lineage ON lineage_generation_info(lineage_id);
CREATE INDEX idx_lm_gen_type ON lineage_members(generation_type);
CREATE INDEX idx_lm_anchor   ON lineage_members(is_anchor);
