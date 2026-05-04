-- V8: Schema for Family Logbook (Bitácora Familiar) with advanced fields
CREATE TABLE family_logbook_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,

    situation VARCHAR(1000) NOT NULL,
    difficulty_detected VARCHAR(1000) NOT NULL,
    emotion_identified VARCHAR(255) NOT NULL,
    understanding VARCHAR(1000) NOT NULL,
    correction_action VARCHAR(1000) NOT NULL,
    family_agreement VARCHAR(1000) NOT NULL,
    progress_evidence VARCHAR(1000),

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',

    created_by VARCHAR(120),
    resolved_by VARCHAR(120),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,

    CONSTRAINT fk_logbook_family
        FOREIGN KEY (family_id)
        REFERENCES families(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_logbook_family_id ON family_logbook_entries(family_id);
CREATE INDEX idx_logbook_status ON family_logbook_entries(status);
