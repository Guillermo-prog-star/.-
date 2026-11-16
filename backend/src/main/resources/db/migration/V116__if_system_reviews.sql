-- V116: Sistemas IF — salida escrita de las revisiones (ADR-020).
--
-- No duplica diario, sprint, tareas ni documentación. Guarda la salida mínima
-- que cada revisión debe dejar por escrito, y el enlace opcional al check-in
-- o a la retrospectiva cuando ya existen.
-- No altera ICF, riesgo ni Ruta de Conciencia. Sin backfill.

CREATE TABLE if_system_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    author_member_id BIGINT NOT NULL,
    review_type VARCHAR(16) NOT NULL,
    review_date DATE NOT NULL,
    period_label VARCHAR(120) NULL,
    sprint_id BIGINT NULL,
    sprint_daily_id BIGINT NULL,
    retrospective_id BIGINT NULL,
    capture_emptied BOOLEAN NULL,
    agenda_checked BOOLEAN NULL,
    calendar_reviewed BOOLEAN NULL,
    tasks_pruned BOOLEAN NULL,
    commitments_transferred BOOLEAN NULL,
    stage_change_unnoticed BOOLEAN NULL,
    change_description VARCHAR(240) NULL,
    what_no_longer_fits TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_isr_family FOREIGN KEY (family_id) REFERENCES families (id),
    CONSTRAINT fk_isr_author FOREIGN KEY (author_member_id) REFERENCES family_members (id),
    CONSTRAINT fk_isr_sprint FOREIGN KEY (sprint_id) REFERENCES family_sprints (id) ON DELETE SET NULL,
    CONSTRAINT fk_isr_daily FOREIGN KEY (sprint_daily_id) REFERENCES sprint_dailies (id) ON DELETE SET NULL,
    CONSTRAINT fk_isr_retro FOREIGN KEY (retrospective_id) REFERENCES sprint_retrospectives (id) ON DELETE SET NULL
);

CREATE INDEX idx_isr_family_date ON if_system_reviews (family_id, review_date);
CREATE INDEX idx_isr_family_type ON if_system_reviews (family_id, review_type);

CREATE TABLE if_system_review_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    item_kind VARCHAR(24) NOT NULL,
    text_value TEXT NULL,
    decision VARCHAR(16) NULL,
    achievement_criterion TEXT NULL,
    review_on DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_isri_review FOREIGN KEY (review_id) REFERENCES if_system_reviews (id) ON DELETE CASCADE
);

CREATE INDEX idx_isri_review ON if_system_review_items (review_id, sort_order);

CREATE TABLE if_system_checks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    author_member_id BIGINT NOT NULL,
    window_start DATE NOT NULL,
    window_end DATE NOT NULL,
    weekly_reviews_done INT NOT NULL,
    capture_emptied_every_week BOOLEAN NOT NULL,
    unregistered_commitment BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_isc_family FOREIGN KEY (family_id) REFERENCES families (id),
    CONSTRAINT fk_isc_author FOREIGN KEY (author_member_id) REFERENCES family_members (id)
);

CREATE INDEX idx_isc_family_window ON if_system_checks (family_id, window_end);
