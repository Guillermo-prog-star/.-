-- V98: Activación de Protocolo de Seguridad
-- Respuesta estructurada para trayectorias con requires_safety_protocol=true (V97).
-- Deliberadamente separado de family_error_protocols (ritual Detectar->Sentir->Comprender
-- para misiones fallidas): un riesgo vital/legal necesita responsable real, acción
-- inmediata y fecha de seguimiento obligatoria, no un ritual emocional de 7 pasos.
-- Activación manual (requiere confirmación humana), nunca automática.

CREATE TABLE IF NOT EXISTS safety_protocol_activations (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_risk_trajectory_id  BIGINT       NOT NULL,
    responsible_id              BIGINT       NOT NULL,
    initial_action               TEXT         NOT NULL,
    follow_up_date               DATE         NOT NULL,
    support_assignment_id       BIGINT       NULL,
    closed                       BOOLEAN      NOT NULL DEFAULT FALSE,
    resolution_notes             TEXT         NULL,
    activated_by                 VARCHAR(255) NULL,
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at                    DATETIME     NULL,
    CONSTRAINT fk_spa_family_trajectory FOREIGN KEY (family_risk_trajectory_id) REFERENCES family_risk_trajectories(id)   ON DELETE CASCADE,
    CONSTRAINT fk_spa_responsible       FOREIGN KEY (responsible_id)             REFERENCES family_members(id)             ON DELETE RESTRICT,
    CONSTRAINT fk_spa_support           FOREIGN KEY (support_assignment_id)      REFERENCES family_support_assignments(id) ON DELETE SET NULL,
    INDEX idx_spa_family_trajectory (family_risk_trajectory_id),
    INDEX idx_spa_closed (closed),
    INDEX idx_spa_follow_up (follow_up_date)
);
