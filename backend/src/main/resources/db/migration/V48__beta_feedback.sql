CREATE TABLE beta_feedback
(
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    family_id            BIGINT NULL,
    reporter_id          BIGINT NULL,
    score                INT NOT NULL,
    comment              TEXT NULL,
    type                 VARCHAR(255) NULL,
    milestone_at_moment  VARCHAR(255) NULL,
    created_at           DATETIME NULL,

    CONSTRAINT pk_beta_feedback
        PRIMARY KEY (id)
);

CREATE INDEX idx_beta_feedback_family
    ON beta_feedback (family_id);

CREATE INDEX idx_beta_feedback_reporter
    ON beta_feedback (reporter_id);

CREATE INDEX idx_beta_feedback_created_at
    ON beta_feedback (created_at);