-- V37: ADN Familiar — Objeto persistente de identidad evolutiva de la familia
-- Almacena valores, fortalezas, sombras, patrones y potencial por miembro.
-- Se actualiza cada vez que la IA sintetiza un nuevo ciclo de análisis.

CREATE TABLE IF NOT EXISTS family_dna (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id           BIGINT NOT NULL UNIQUE,
    -- Listas JSON: valores, fortalezas, sombras, patrones
    valores             JSON,
    fortalezas          JSON,
    sombras             JSON,
    patrones            JSON,
    -- Texto libre generado por IA
    estilo_comunicacion TEXT,
    ritmo_familiar      TEXT,
    -- Potencial oculto por miembro: [{miembro, talento, descripcion}]
    potencial_oculto    JSON,
    -- Narrativa generada por la IA (párrafo de síntesis)
    narrativa_ia        TEXT,
    -- Control de versiones
    version             INT NOT NULL DEFAULT 1,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_dna_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);
