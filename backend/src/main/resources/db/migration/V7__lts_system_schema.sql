-- V7: Learning Trace System (LTS) - Bitácora Familiar
-- Modelo cognitivo: Intento -> Error -> Hipótesis -> Corrección -> Insight

CREATE TABLE IF NOT EXISTS lts_sessions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id    BIGINT NOT NULL,
    member_id    BIGINT NOT NULL,
    topic        VARCHAR(200) NOT NULL,
    objective    TEXT NOT NULL,
    status       VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, ARCHIVED
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_lts_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_attempts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT NOT NULL,
    version      INT NOT NULL DEFAULT 1,
    content      TEXT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_att_session FOREIGN KEY (session_id) REFERENCES lts_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_errors (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id   BIGINT NOT NULL,
    error_type   VARCHAR(100) NOT NULL, -- CONCEPTUAL, LOGICAL, PROCEDURAL, ATTENTION
    description  TEXT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_err_attempt FOREIGN KEY (attempt_id) REFERENCES lts_attempts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_hypotheses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_id     BIGINT NOT NULL,
    content      TEXT NOT NULL, -- ¿Por qué ocurrió?
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_hyp_error FOREIGN KEY (error_id) REFERENCES lts_errors(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_corrections (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    hypothesis_id BIGINT NOT NULL,
    new_solution TEXT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_corr_hyp FOREIGN KEY (hypothesis_id) REFERENCES lts_hypotheses(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_insights (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT NOT NULL,
    what_learned TEXT NOT NULL, -- ¿Por qué / Cómo?
    transfer     TEXT,         -- ¿Dónde más aplica?
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_ins_session FOREIGN KEY (session_id) REFERENCES lts_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lts_comparisons (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT NOT NULL,
    v_old_id     BIGINT NOT NULL,
    v_new_id     BIGINT NOT NULL,
    diff_analysis TEXT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lts_comp_session FOREIGN KEY (session_id) REFERENCES lts_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_lts_comp_old FOREIGN KEY (v_old_id) REFERENCES lts_attempts(id),
    CONSTRAINT fk_lts_comp_new FOREIGN KEY (v_new_id) REFERENCES lts_attempts(id)
) ENGINE=InnoDB;
