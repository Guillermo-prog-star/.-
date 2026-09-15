-- V106: Hypothesis Evidence Pattern (ADR-004). Registro append-only de
-- observaciones primarias que respaldan una hipotesis del sistema (PAF,
-- Determinantes Transformacionales, futuras), independiente del estado
-- operacional (family_longitudinal_state y equivalentes), que puede
-- sobrescribirse y por tanto nunca es evidencia (ver ADR-004, Decision 1).
--
-- Deliberadamente sin FK a una entidad especifica: subject_type/subject_id
-- es generico y polimorfico (familia, miembro, dimension, mision, etc.),
-- y el catalogo de subject_type queda abierto sin reabrir este ADR (ver
-- ADR-004, Decision 2). measurement_value es DOUBLE porque los casos de
-- uso actuales (ICF, streak) son numericos; no se agrega un tipo mas
-- flexible sin un consumidor real que lo necesite.
CREATE TABLE IF NOT EXISTS hypothesis_evidence (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,

    hypothesis            VARCHAR(80)  NOT NULL,
    hypothesis_version    VARCHAR(20)  NOT NULL,

    subject_type          VARCHAR(40)  NOT NULL,
    subject_id            BIGINT       NOT NULL,

    measurement_type      VARCHAR(80)  NOT NULL,
    measurement_value     DOUBLE       NOT NULL,

    instrument            VARCHAR(80)  NOT NULL,
    instrument_version    VARCHAR(20)  NOT NULL,

    source                ENUM('MANUAL','AUTOMATIC','DERIVED','IMPORT','SIMULATION') NOT NULL,

    observed_at           DATETIME     NOT NULL,
    recorded_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_hypothesis_evidence_hypothesis (hypothesis, hypothesis_version),
    INDEX idx_hypothesis_evidence_subject    (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
