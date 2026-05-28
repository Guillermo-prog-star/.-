-- =========================================================
-- V30: Sesiones Conversacionales + memberId en chat_messages
-- =========================================================

-- 1. Tabla de sesiones conversacionales
CREATE TABLE conversation_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    member_id       BIGINT,
    goal            VARCHAR(20)  NOT NULL DEFAULT 'GENERAL'
                        COMMENT 'GENERAL | SUPPORT | REFLECTION | PLANNING | GUARDIAN_SYNC',
    emotional_state VARCHAR(20)  NULL
                        COMMENT 'CALM | ANXIOUS | FRUSTRATED | HOPEFUL | CONFUSED | ENGAGED',
    turn_count      INT          NOT NULL DEFAULT 0,
    outcome         VARCHAR(30)  NULL
                        COMMENT 'COMPLETED | ABANDONED | ESCALATED',
    started_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at        DATETIME     NULL,

    CONSTRAINT fk_cs_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE SET NULL,

    INDEX idx_cs_family_member  (family_id, member_id),
    INDEX idx_cs_family_started (family_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Enriquecer chat_messages con identidad del hablante y sesión
ALTER TABLE chat_messages
    ADD COLUMN member_id          BIGINT       NULL AFTER is_ai,
    ADD COLUMN session_id         BIGINT       NULL AFTER member_id,
    ADD COLUMN emotional_snapshot VARCHAR(20)  NULL AFTER session_id,
    ADD CONSTRAINT fk_cm_member  FOREIGN KEY (member_id)  REFERENCES family_members(id)      ON DELETE SET NULL,
    ADD CONSTRAINT fk_cm_session FOREIGN KEY (session_id) REFERENCES conversation_sessions(id) ON DELETE SET NULL;
