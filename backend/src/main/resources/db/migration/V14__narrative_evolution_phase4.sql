-- =====================================================================
-- V14 — Narrative Evolution Engine (SDD Fase 4)
-- Motor de capítulos narrativos: historia evolutiva de cada familia
-- =====================================================================

CREATE TABLE narrative_chapters (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    family_id         BIGINT          NOT NULL,
    chapter_number    INT             NOT NULL,
    title             VARCHAR(200)    NOT NULL,
    body              TEXT,
    phase             VARCHAR(20)     NOT NULL,
    icf_at_open       DOUBLE,
    icf_at_close      DOUBLE,
    key_event         VARCHAR(400),
    turning_point     TINYINT(1)      NOT NULL DEFAULT 0,
    opened_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at         DATETIME(6),
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_narrative_family  (family_id),
    INDEX idx_narrative_open    (family_id, closed_at),
    CONSTRAINT fk_narrative_family FOREIGN KEY (family_id) REFERENCES families (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
