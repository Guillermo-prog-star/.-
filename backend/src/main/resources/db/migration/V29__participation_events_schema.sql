CREATE TABLE participation_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    member_id       BIGINT,
    event_type      VARCHAR(30)  NOT NULL COMMENT 'CHAT_MESSAGE | MISSION_COMPLETED | EVIDENCE_SUBMITTED | LOGBOOK_ENTRY',
    occurred_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pe_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE SET NULL,

    INDEX idx_pe_family_occurred (family_id, occurred_at),
    INDEX idx_pe_member_occurred (member_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
