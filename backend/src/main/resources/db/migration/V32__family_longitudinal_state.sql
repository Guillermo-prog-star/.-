-- ============================================================
-- V32: Estado Longitudinal Familiar
--
-- Tabla central del sistema vivo de Integrity Family.
-- Mantiene la memoria estructural de la familia a lo largo del tiempo.
--
-- Sin esta tabla: el Consultor IA responde sin historial (FALLA 2).
-- Con esta tabla: cada inferencia tiene contexto longitudinal real.
--
-- Se actualiza automáticamente con cada evento del bus familiar:
--   family.crisis.triggered      → incrementa crisis_count_30d
--   family.icf.recalculated      → sincroniza icf_current + dimensiones
--   family.journal.entry.added   → ajusta consecutive_deteriorations/improvements
--   family.plan.adjusted         → actualiza plan_adherence_percent
--   family.assessment.completed  → resetea last_assessment_at
-- ============================================================

CREATE TABLE IF NOT EXISTS family_longitudinal_state (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id                   BIGINT NOT NULL UNIQUE,

    -- ICF y Riesgo
    icf_current                 DOUBLE,
    icf_30d_ago                 DOUBLE,
    icf_90d_ago                 DOUBLE,
    current_risk_level          VARCHAR(20),
    risk_trend                  VARCHAR(20),  -- IMPROVING | STABLE | DETERIORATING | CRITICAL

    -- Dimensiones ICF (0-100)
    dim_emociones               DOUBLE,
    dim_comunicacion            DOUBLE,
    dim_habitos                 DOUBLE,
    dim_tiempos                 DOUBLE,
    critical_dimension          VARCHAR(30),

    -- Crisis y señales emocionales
    crisis_count_30d            INT DEFAULT 0,
    crisis_count_total          INT DEFAULT 0,
    last_crisis_at              DATETIME(6),
    consecutive_deteriorations  INT DEFAULT 0,
    consecutive_improvements    INT DEFAULT 0,
    communication_collapse_active BOOLEAN DEFAULT FALSE,

    -- Evolución longitudinal (RECONOCIMIENTO → AMOR → ENTREGA)
    evolution_phase             VARCHAR(30),  -- inconsciente | reactivo | consciente | pleno
    narrative_stage             VARCHAR(30),  -- RECONOCIMIENTO | AMOR | ENTREGA
    consciousness_level         INT,          -- 1=Plena … 5=Inconsciente
    consciousness_label         VARCHAR(30),

    -- Adherencia y actividad
    plan_adherence_percent      DOUBLE,
    inactivity_days             INT DEFAULT 0,

    -- Control temporal
    last_assessment_at          DATETIME(6),
    last_journal_at             DATETIME(6),
    updated_at                  DATETIME(6),
    created_at                  DATETIME(6),

    CONSTRAINT fk_longitudinal_family
        FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

-- Índices para consultas sentinel y priorización IA
-- Nota: IF NOT EXISTS no soportado en CREATE INDEX en MySQL 8 via Flyway
CREATE INDEX idx_longitudinal_risk_trend
    ON family_longitudinal_state(risk_trend);

CREATE INDEX idx_longitudinal_crisis_count
    ON family_longitudinal_state(crisis_count_30d);

CREATE INDEX idx_longitudinal_deterioration
    ON family_longitudinal_state(consecutive_deteriorations);

CREATE INDEX idx_longitudinal_last_crisis
    ON family_longitudinal_state(last_crisis_at);
