CREATE TABLE checklist_logs
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    family_id      BIGINT NOT NULL,
    member_id      BIGINT NULL,
    milestone_key  VARCHAR(50) NULL,
    event_type     VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    comment        VARCHAR(500) NULL,
    evidence_url   VARCHAR(255) NULL,
    source         VARCHAR(20) NULL,
    created_at     DATETIME NULL,

    CONSTRAINT pk_checklist_logs
        PRIMARY KEY (id)
);

CREATE INDEX idx_checklist_logs_family
    ON checklist_logs (family_id);

CREATE INDEX idx_checklist_logs_member
    ON checklist_logs (member_id);

CREATE INDEX idx_checklist_logs_created_at
    ON checklist_logs (created_at);