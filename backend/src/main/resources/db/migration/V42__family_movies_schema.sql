-- V42: Película Familiar — resumen narrativo generado por IA
-- Similar a Spotify Wrapped pero familiar y multigeneracional.
-- Se genera trimestralmente de forma automática o bajo demanda.

CREATE TABLE IF NOT EXISTS family_movies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    period_label    VARCHAR(100) NOT NULL,    -- "Enero – Marzo 2026"
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,

    -- Estadísticas del período
    evidences_count     INT NOT NULL DEFAULT 0,
    gratitudes_count    INT NOT NULL DEFAULT 0,
    missions_completed  INT NOT NULL DEFAULT 0,
    crises_count        INT NOT NULL DEFAULT 0,
    rituals_completed   INT NOT NULL DEFAULT 0,
    days_active         INT NOT NULL DEFAULT 0,
    best_streak         INT NOT NULL DEFAULT 0,
    icf_start           DOUBLE,
    icf_end             DOUBLE,
    icf_delta           DOUBLE,

    -- Narrativa generada por IA
    opening_line    TEXT,          -- frase de apertura cinematográfica
    chapter_1       TEXT,          -- Los momentos que los conectaron
    chapter_2       TEXT,          -- Los desafíos que enfrentaron
    chapter_3       TEXT,          -- Lo que construyeron juntos
    mentor_letter   TEXT,          -- carta personal del Mentor a la familia
    highlight_quote TEXT,          -- la frase más memorable del período

    -- Metadata
    generated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generation_model VARCHAR(50),

    CONSTRAINT fk_movie_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    INDEX idx_movie_family_period (family_id, period_start)
);
