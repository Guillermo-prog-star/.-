-- V76: Journey Progress Snapshots
-- Persiste el nivel alcanzado por cada familia en el viaje de 13 niveles.
-- Permite detectar level-ups para enviar celebraciones y analizar tendencias.

CREATE TABLE IF NOT EXISTS family_journey_snapshots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    journey_level   INT          NOT NULL COMMENT '0-13: nivel alcanzado',
    journey_progress INT         NOT NULL COMMENT '0-100: porcentaje de completitud',
    level_up        BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'TRUE si este snapshot registra un nuevo nivel',
    previous_level  INT          NULL     COMMENT 'Nivel anterior, solo cuando level_up = TRUE',
    celebration_sent BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'TRUE si ya se envió la celebración WhatsApp',
    snapshot_date   DATE         NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_fjs_family  (family_id),
    INDEX idx_fjs_date    (snapshot_date),
    INDEX idx_fjs_levelup (family_id, level_up),
    CONSTRAINT fk_fjs_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
