-- V24: IF-ALT — Tabla de alertas clínicas del motor de detección de patrones.
-- Las alertas se generan por el AlertEngine al detectar patrones críticos:
-- riesgo ALTO/CRITICO repetido, estado CRITICAL sostenido, simulación reiterada,
-- recaída confirmada, o múltiples reglas EEDSL activadas simultáneamente.

CREATE TABLE family_alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    alert_type      VARCHAR(50)  NOT NULL,   -- CONSECUTIVE_HIGH_RISK | CRITICAL_STATE_SUSTAINED | SIMULATION_REPEAT | RELAPSE_CONFIRMED | MULTI_RULE_ACTIVATION
    severity        VARCHAR(20)  NOT NULL,   -- LOW | MEDIUM | HIGH | CRITICAL
    title           VARCHAR(100) NOT NULL,
    detail          VARCHAR(500),
    inference_key   VARCHAR(60),             -- ruleKey o "ICF_CALC" que originó la alerta
    evaluation_id   BIGINT,
    resolved        BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at     DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    INDEX idx_fa_family    (family_id),
    INDEX idx_fa_resolved  (family_id, resolved),
    INDEX idx_fa_severity  (severity),
    CONSTRAINT fk_fa_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);
