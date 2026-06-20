-- Progreso de cada familia a través de los 75 capítulos de transformación
CREATE TABLE IF NOT EXISTS family_chapter_progress (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id      BIGINT    NOT NULL,
    chapter_number INT       NOT NULL,
    completed      TINYINT(1) NOT NULL DEFAULT 0,
    completed_at   BIGINT,
    started_at     BIGINT    NOT NULL,
    UNIQUE KEY uk_family_chapter (family_id, chapter_number),
    INDEX idx_fcp_family (family_id)
);
