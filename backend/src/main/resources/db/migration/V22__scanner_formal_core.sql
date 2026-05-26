-- V22: IF-Scanner Core — Registros de Inferencia Formal y DSL Emocional
-- Implementa: IF-CIS (estabilidad inferencial), IF-EEDSL (reglas ejecutables)
-- Garantía: evidenceHash UNIQUE previene duplicados y habilita replay histórico.

-- ── Tabla: inference_records (IF-CIS) ────────────────────────────────────────
-- Captura inmutablemente cada inferencia del RISK_ALGO_V1.
-- evidenceHash: SHA-256 de los inputs — misma evidencia → mismo registro.
CREATE TABLE IF NOT EXISTS inference_records (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    family_id           BIGINT          NOT NULL,
    evaluation_id       BIGINT          NOT NULL,
    inference_key       VARCHAR(50)     NULL,
    algo_version        INT             NOT NULL DEFAULT 1,
    -- Estados epistemológicos: OBSERVED/CORRELATED/INFERRED/STABILIZED/REVISED/ARCHIVED
    epistemic_state     VARCHAR(20)     NOT NULL DEFAULT 'INFERRED',
    -- Hash deterministico: misma entrada → mismo hash → sin duplicados
    evidence_hash       VARCHAR(64)     NOT NULL,
    -- Resultado de la inferencia
    icf_value           DOUBLE          NULL,
    risk_level          VARCHAR(20)     NULL,
    critical_dimension  VARCHAR(50)     NULL,
    -- IF-TOS: estado operacional calculado sobre el historial familiar
    operational_state   VARCHAR(20)     NULL,
    simulation_suspected BOOLEAN        NULL,
    -- IF-SUM: incertidumbre total (0.0–1.0)
    uncertainty_total   DOUBLE          NULL,
    -- Versionado epistemológico
    previous_version    BIGINT          NULL,
    revision_reason     VARCHAR(200)    NULL,
    stabilized_at       DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ir_evidence_hash (evidence_hash),
    KEY idx_ir_family    (family_id),
    KEY idx_ir_evaluation (evaluation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Tabla: emotional_rules (IF-EEDSL) ────────────────────────────────────────
-- Almacena reglas emocionales ejecutables y versionadas del DSL.
-- Versionables: versiones antiguas se conservan para replay histórico.
CREATE TABLE IF NOT EXISTS emotional_rules (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    rule_key            VARCHAR(60)     NOT NULL,
    version             INT             NOT NULL DEFAULT 1,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Alcance temporal y contextual
    milestone_scope     VARCHAR(20)     NULL DEFAULT '*',
    member_role         VARCHAR(20)     NULL DEFAULT '*',
    temporal_window_days INT            NOT NULL DEFAULT 14,
    -- Producción de la regla
    projection_label    VARCHAR(60)     NULL,
    confidence_base     DOUBLE          NOT NULL DEFAULT 0.70,
    risk_output         VARCHAR(20)     NULL,
    -- Trazabilidad
    created_by          VARCHAR(50)     NULL DEFAULT 'RISK_ALGO_V1',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_er_key_version (rule_key, version),
    KEY idx_er_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Tabla: emotional_rule_signals (señales requeridas por regla) ──────────────
-- Almacena la lista de señales que deben activarse para disparar la regla.
CREATE TABLE IF NOT EXISTS emotional_rule_signals (
    rule_id             BIGINT          NOT NULL,
    signal_name         VARCHAR(60)     NOT NULL,
    KEY idx_ers_rule (rule_id),
    CONSTRAINT fk_ers_rule FOREIGN KEY (rule_id)
        REFERENCES emotional_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Semilla inicial: reglas del RISK_ALGO_V1 formalizadas ────────────────────
-- Las reglas de recaída y simulación ya existían implícitas en el código.
-- Ahora se persisten como entidades formales del DSL.
INSERT IGNORE INTO emotional_rules
    (rule_key, version, active, milestone_scope, member_role, temporal_window_days,
     projection_label, confidence_base, risk_output, created_by)
VALUES
    ('relapse_detection',   1, TRUE, '*', '*', 1,  'RECAIDA',             0.85, 'ALTO',     'RISK_ALGO_V1'),
    ('simulation_suspected',1, TRUE, '*', '*', 1,  'RESPUESTA_SIMULADA',  0.80, 'MODERADO', 'RISK_ALGO_V1'),
    ('dimension_collapse',  1, TRUE, '*', '*', 1,  'COLAPSO_DIMENSIONAL', 0.95, 'CRITICO',  'RISK_ALGO_V1'),
    ('relational_stress',   1, TRUE, '*', 'PADRE', 14, 'ESTRES_RELACIONAL', 0.74, 'MODERADO','RISK_ALGO_V1'),
    ('emotional_exhaustion',1, TRUE, '*', 'MADRE', 14, 'AGOTAMIENTO_EMOCIONAL', 0.72, 'MODERADO','RISK_ALGO_V1');

-- Señales para relational_stress
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'voice_tension'  FROM emotional_rules WHERE rule_key='relational_stress' AND version=1;
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'interruptions'  FROM emotional_rules WHERE rule_key='relational_stress' AND version=1;
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'avoidance'      FROM emotional_rules WHERE rule_key='relational_stress' AND version=1;

-- Señales para emotional_exhaustion
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'reduced_participation' FROM emotional_rules WHERE rule_key='emotional_exhaustion' AND version=1;
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'chronic_fatigue_signals' FROM emotional_rules WHERE rule_key='emotional_exhaustion' AND version=1;
