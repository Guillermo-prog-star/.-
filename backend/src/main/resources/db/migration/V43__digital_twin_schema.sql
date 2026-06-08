-- V43: Gemelo Digital Familiar
-- Sintetiza patrones históricos en un perfil predictivo único.
-- Se actualiza semanalmente y on-demand.

CREATE TABLE IF NOT EXISTS family_twin_profiles (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id             BIGINT NOT NULL UNIQUE,

    -- Huella conductual única
    behavioral_signature  TEXT,           -- descripción IA de la personalidad operativa
    communication_pattern VARCHAR(60),    -- EXPRESSIVE | AVOIDANT | COLLABORATIVE | RESERVED
    resilience_index      DOUBLE,         -- 0-100: qué tan rápido recupera tras crisis
    bonding_rhythm        VARCHAR(60),    -- DAILY | WEEKLY | SPORADIC | INTENSIVE
    dominant_strength     VARCHAR(100),   -- su fortaleza más consistente
    dominant_vulnerability VARCHAR(100),  -- su punto de mayor fragilidad

    -- Patrones detectados (JSON array)
    detected_patterns     JSON,           -- [{pattern, frequency, confidence, description}]

    -- Correlaciones descubiertas (JSON array)
    correlations          JSON,           -- [{trigger, effect, lag_days, confidence}]

    -- Predicciones activas (JSON array de FamilyPrediction desnormalizado)
    active_predictions    JSON,

    -- Ciclo familiar
    avg_days_between_crises  INT,         -- promedio de días entre crisis
    avg_recovery_days        INT,         -- días promedio para recuperarse de una crisis
    peak_activity_day        VARCHAR(20), -- LUNES | MARTES | etc.

    -- Control
    computed_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_richness         VARCHAR(20) DEFAULT 'LOW', -- LOW | MEDIUM | HIGH | EXPERT

    CONSTRAINT fk_twin_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS family_predictions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    prediction_type  VARCHAR(50) NOT NULL,  -- TENSION_RISK | GROWTH | COMMUNICATION_ALERT | etc.
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    confidence       INT NOT NULL,           -- 0-100
    time_horizon     VARCHAR(30),            -- "próximos 7 días" | "próximas 2 semanas"
    recommended_action TEXT,
    status           VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE | CONFIRMED | DISMISSED | EXPIRED
    predicted_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       DATETIME,
    CONSTRAINT fk_pred_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    INDEX idx_pred_family_status (family_id, status)
);
