-- V21: Creación del Motor de Contenido Emocional e Inferencia de Historias Invisibles
CREATE TABLE IF NOT EXISTS emotional_stimuli (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    media_url   VARCHAR(1000) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    target_role VARCHAR(50) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reflective_sessions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    member_id        BIGINT NOT NULL,
    stimulus_id      BIGINT NOT NULL,
    reflection       TEXT NOT NULL,
    emotional_score  INT NOT NULL,
    inference_result TEXT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refs_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_refs_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE CASCADE,
    CONSTRAINT fk_refs_stimulus FOREIGN KEY (stimulus_id) REFERENCES emotional_stimuli(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insertar semilla disparadora inicial: Historias Invisibles
INSERT INTO emotional_stimuli (type, title, media_url, category, target_role)
VALUES (
    'VIDEO',
    'Historias Invisibles',
    'assets/videos/historias_invisibles.mp4',
    'PRESENCIA',
    'FAMILIA'
);
