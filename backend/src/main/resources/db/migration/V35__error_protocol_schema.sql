-- V35: Protocolo de gestión del error familiar
-- Ciclo: Detectar → Sentir → Comprender → Accionar → Acordar → Seguimiento → Aprender
-- Dialecto: MySQL 8.4

CREATE TABLE IF NOT EXISTS family_error_protocols (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT       NOT NULL,
    mission_failed   TEXT,
    feelings         TEXT,
    what_happened    TEXT,
    corrective_action TEXT,
    who_helps        VARCHAR(200),
    agreement        TEXT,
    followup_date    DATE,
    learning         TEXT,
    closed           TINYINT(1)   NOT NULL DEFAULT 0,
    current_step     VARCHAR(20)  NOT NULL DEFAULT 'DETECT',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at        TIMESTAMP    NULL,
    CONSTRAINT fk_error_protocol_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE INDEX idx_error_protocol_family ON family_error_protocols(family_id);
