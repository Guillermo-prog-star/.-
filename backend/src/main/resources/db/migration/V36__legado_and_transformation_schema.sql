-- V36: Legado familiar + Estado de transformación de 36 meses
-- Dialecto: MySQL 8.4

CREATE TABLE IF NOT EXISTS family_legacies (
    id                       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_id                BIGINT       NOT NULL,
    history_lessons          TEXT,
    history_conserve         TEXT,
    history_avoid_errors     TEXT,
    history_to_leave         TEXT,
    history_recognition      TEXT,
    constitution_family_name VARCHAR(200),
    constitution_year        INT,
    founding_principle       TEXT,
    commitments              TEXT,
    never_do                 TEXT,
    conflict_resolution      TEXT,
    family_mission           TEXT,
    family_vision            TEXT,
    family_tagline           VARCHAR(300),
    letter_from              VARCHAR(200),
    letter_to                VARCHAR(200),
    letter_open_in_year      INT,
    letter_content           TEXT,
    letter_sealed            TINYINT(1)   NOT NULL DEFAULT 0,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_legacy_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS family_values (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_id   BIGINT       NOT NULL,
    icon        VARCHAR(10),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_value_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transformation_states (
    id                    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_id             BIGINT      NOT NULL UNIQUE,
    onboarding_step       VARCHAR(30) NOT NULL DEFAULT 'CREATE_FAMILY',
    current_pillar        VARCHAR(30) NOT NULL DEFAULT 'RECONOCIMIENTO',
    current_month         INT         NOT NULL DEFAULT 1,
    current_sprint_number INT         NOT NULL DEFAULT 1,
    active_mission_id     BIGINT,
    progress_percent      INT         NOT NULL DEFAULT 0,
    milestone_label       VARCHAR(10) NOT NULL DEFAULT 'M1',
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transformation_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE INDEX idx_family_legacies_family   ON family_legacies(family_id);
CREATE INDEX idx_family_values_family     ON family_values(family_id);
