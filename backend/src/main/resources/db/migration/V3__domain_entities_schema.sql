-- V3: Completion of Domain Schema (SDD Centralization)
-- Sincronización con el modelo de dominio y data.sql

-- 1. Soporte para Roles (ManyToMany/OneToMany compatible con data.sql)
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 2. Evaluaciones y Resultados
CREATE TABLE IF NOT EXISTS evaluations (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id            BIGINT NOT NULL,
    member_id            BIGINT,
    status               VARCHAR(30) NOT NULL,
    started_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalized_at         DATETIME,
    has_crisis           BOOLEAN DEFAULT FALSE,
    icf                  DOUBLE,
    milestone_key        VARCHAR(50),
    spiritual_synthesis  TEXT,
    CONSTRAINT fk_eval_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_eval_member FOREIGN KEY (member_id) REFERENCES family_members(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluation_answers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    question_key  VARCHAR(255) NOT NULL,
    score         INT NOT NULL,
    dimension     VARCHAR(30),
    CONSTRAINT fk_ans_eval FOREIGN KEY (evaluation_id) REFERENCES evaluations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluation_dimension_scores (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    dimension_name VARCHAR(100) NOT NULL,
    score         DOUBLE NOT NULL,
    CONSTRAINT fk_eds_eval FOREIGN KEY (evaluation_id) REFERENCES evaluations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Planes de Acción e IA
CREATE TABLE IF NOT EXISTS plans (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    evaluation_id    BIGINT,
    title            VARCHAR(160) NOT NULL,
    description      TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ai_report        TEXT,
    ai_generated_at  DATETIME,
    CONSTRAINT fk_plan_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_plan_eval FOREIGN KEY (evaluation_id) REFERENCES evaluations(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS plan_tasks (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id             BIGINT NOT NULL,
    title               VARCHAR(160) NOT NULL,
    description         TEXT,
    completed           BOOLEAN DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        DATETIME,
    due_date            DATETIME,
    dimension           VARCHAR(50),
    periodicity_months  INT,
    responsible_id      BIGINT,
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_resp FOREIGN KEY (responsible_id) REFERENCES family_members(id)
) ENGINE=InnoDB;

-- 4. Operatividad Familiar (Checklist, Riesgos, etc.)
CREATE TABLE IF NOT EXISTS checklist_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id    BIGINT NOT NULL,
    description  TEXT NOT NULL,
    completed    BOOLEAN DEFAULT FALSE,
    completed_by VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    category     VARCHAR(50),
    source       VARCHAR(50),
    dimension    VARCHAR(50),
    CONSTRAINT fk_check_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS risk_snapshots (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id         BIGINT NOT NULL,
    risk_level        VARCHAR(30) NOT NULL,
    score             DOUBLE NOT NULL,
    observation       TEXT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dimension_impact  VARCHAR(255),
    CONSTRAINT fk_risk_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS critical_days (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id  BIGINT NOT NULL,
    day_date   DATE NOT NULL,
    reason     VARCHAR(255),
    severity   VARCHAR(30),
    CONSTRAINT fk_cd_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Comunicación y Alertas
CREATE TABLE IF NOT EXISTS chat_messages (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id  BIGINT NOT NULL,
    sender     VARCHAR(100),
    content    TEXT,
    timestamp  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_ai      BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_chat_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS admin_alerts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    message    TEXT,
    severity   VARCHAR(30),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read    BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;

-- 6. Feedback y Hitos
CREATE TABLE IF NOT EXISTS feedbacks (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id  BIGINT NOT NULL,
    member_id  BIGINT,
    rating     INT,
    comment    TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feed_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_feed_member FOREIGN KEY (member_id) REFERENCES family_members(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS milestones (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id    BIGINT NOT NULL,
    milestone_key VARCHAR(50) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    reached_at   DATETIME,
    CONSTRAINT fk_ms_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB;
