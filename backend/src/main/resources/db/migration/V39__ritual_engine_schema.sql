-- V39: Motor de Rituales Familiares
-- Agrega birth_date a family_members para detectar cumpleaños.
-- Crea family_rituals para almacenar rituales activos y su historial.

ALTER TABLE family_members
    ADD COLUMN birth_date DATE NULL AFTER age;

CREATE TABLE IF NOT EXISTS family_rituals (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    ritual_type      VARCHAR(40) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    guided_steps     JSON,            -- pasos guiados generados por IA
    trigger_context  VARCHAR(500),    -- qué lo disparó (ej: "Cumpleaños de María")
    triggered_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at     DATETIME,
    completed_at     DATETIME,
    dismissed_at     DATETIME,
    CONSTRAINT fk_ritual_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    INDEX idx_ritual_family_status (family_id, status),
    INDEX idx_ritual_type_date (family_id, ritual_type, triggered_at)
);
