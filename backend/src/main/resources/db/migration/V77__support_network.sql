-- V77: Red de Apoyo Humano
-- Principio: la familia decide quién accede, cuándo y a qué.

CREATE TABLE IF NOT EXISTS support_network_members (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    phone            VARCHAR(30),
    specialty        ENUM('THERAPIST','ORIENTADOR','SOCIAL_WORKER','DOCTOR','TEACHER','COMMUNITY_LEADER','COACH','INSTITUTION') NOT NULL,
    license_number   VARCHAR(80),
    institution_name VARCHAR(150),
    bio              TEXT,
    active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snm_email (email),
    INDEX idx_snm_specialty (specialty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS family_support_assignments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    support_member_id BIGINT NOT NULL,
    specialty        ENUM('THERAPIST','ORIENTADOR','SOCIAL_WORKER','DOCTOR','TEACHER','COMMUNITY_LEADER','COACH','INSTITUTION') NOT NULL,
    status           ENUM('INVITED','ACTIVE','SUSPENDED','REVOKED') NOT NULL DEFAULT 'INVITED',

    -- Quién invitó (miembro de la familia)
    invited_by_email VARCHAR(150) NOT NULL,
    invited_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consentimiento explícito de la familia
    consented_by_email VARCHAR(150),
    consented_at     DATETIME,

    -- Revocación
    revoked_by_email VARCHAR(150),
    revoked_at       DATETIME,
    revocation_reason VARCHAR(500),

    -- Alcance de acceso (qué puede ver)
    can_view_icf_score     TINYINT(1) NOT NULL DEFAULT 1,
    can_view_risk_level    TINYINT(1) NOT NULL DEFAULT 1,
    can_view_plan_summary  TINYINT(1) NOT NULL DEFAULT 0,
    can_view_sprint_progress TINYINT(1) NOT NULL DEFAULT 0,
    can_view_crisis_history TINYINT(1) NOT NULL DEFAULT 0,
    can_leave_notes        TINYINT(1) NOT NULL DEFAULT 1,

    notes            TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fsa_family   FOREIGN KEY (family_id)         REFERENCES families(id)                  ON DELETE CASCADE,
    CONSTRAINT fk_fsa_support  FOREIGN KEY (support_member_id) REFERENCES support_network_members(id)   ON DELETE CASCADE,
    UNIQUE KEY uq_fsa_family_member (family_id, support_member_id),
    INDEX idx_fsa_family  (family_id),
    INDEX idx_fsa_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS support_professional_notes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id    BIGINT NOT NULL,
    family_id        BIGINT NOT NULL,
    support_member_id BIGINT NOT NULL,
    content          TEXT NOT NULL,
    is_visible_to_family TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_spn_assignment FOREIGN KEY (assignment_id) REFERENCES family_support_assignments(id) ON DELETE CASCADE,
    INDEX idx_spn_assignment (assignment_id),
    INDEX idx_spn_family     (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS support_access_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id    BIGINT NOT NULL,
    family_id        BIGINT NOT NULL,
    support_member_id BIGINT NOT NULL,
    action           VARCHAR(60) NOT NULL,   -- INVITED, CONSENT_GIVEN, ACCESS_REVOKED, NOTE_ADDED, DATA_VIEWED
    performed_by     VARCHAR(150),
    detail           VARCHAR(500),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_sal_assignment (assignment_id),
    INDEX idx_sal_family     (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
