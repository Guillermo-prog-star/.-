-- V113: Diario guiado biográfico (módulo `biography`, ADR-017).
--
-- Catálogo global y fijo de preguntas (12 capítulos de vida) + respuestas
-- en primera persona de cada miembro. Solo DDL: el contenido del catálogo
-- se siembra por BiographyQuestionDataInitializer (Java, idempotente), NO
-- aquí por SQL crudo -- ver ADR-017 (misma lección aprendida que V75/V100-V101).

CREATE TABLE biography_questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_number  INT NOT NULL,
    chapter_title   VARCHAR(150) NOT NULL,
    question_key    VARCHAR(80) NOT NULL,
    prompt_text     TEXT NOT NULL,
    deepen_text     TEXT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_biography_questions_key (question_key),
    INDEX idx_biography_questions_chapter (chapter_number, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biography_answers (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id         BIGINT NOT NULL,
    member_id         BIGINT NOT NULL,
    question_id       BIGINT NOT NULL,
    answer_text          TEXT NULL,
    epistemic_source     VARCHAR(20) NULL,
    occurred_period      VARCHAR(100) NULL,
    meaning_then         TEXT NULL,
    current_meaning      TEXT NULL,
    related_source_note  TEXT NULL,
    perceived_event_key  VARCHAR(64) NULL,
    confidence_level     INT NULL,
    visibility           VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    correction_note      TEXT NULL,
    completed_at         DATETIME NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NULL,

    CONSTRAINT fk_biography_answers_family   FOREIGN KEY (family_id)   REFERENCES families(id)            ON DELETE CASCADE,
    CONSTRAINT fk_biography_answers_member    FOREIGN KEY (member_id)   REFERENCES family_members(id)      ON DELETE CASCADE,
    CONSTRAINT fk_biography_answers_question  FOREIGN KEY (question_id) REFERENCES biography_questions(id) ON DELETE CASCADE,

    UNIQUE KEY uq_biography_answer_member_question (member_id, question_id),
    INDEX idx_biography_answers_family (family_id),
    INDEX idx_biography_answers_perceived_event (family_id, perceived_event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
