-- V10: Schema for Family Behavioral Events and IVR (Índice de Velocidad de Reparación)
CREATE TABLE family_behavioral_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    severity INT NOT NULL, -- Rango 1 a 5 (Leve a Crítico)
    occurred_at TIMESTAMP NOT NULL,
    repaired_at TIMESTAMP NULL,
    repair_description VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_behavioral_family
        FOREIGN KEY (family_id)
        REFERENCES families(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_behavioral_family_id ON family_behavioral_events(family_id);
