-- V1: Schema Base y Auth (Modulo 1) - SDD Redesign

CREATE TABLE IF NOT EXISTS families (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    family_code     VARCHAR(30) UNIQUE,
    current_milestone VARCHAR(50) DEFAULT 'inicio',
    pin_hash        VARCHAR(255),
    whatsapp        VARCHAR(30),
    municipio       VARCHAR(80),
    country_code    VARCHAR(10),
    department_code VARCHAR(10),
    created_by_id   BIGINT,
    sentinel_active BOOLEAN DEFAULT FALSE,
    icf_score       INT,
    total_tasks     INT,
    completed_tasks INT,
    last_report_sent_at DATETIME,
    next_evaluation_at DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id                BIGINT NULL,
    email                    VARCHAR(180) NOT NULL UNIQUE,
    password_hash            VARCHAR(120) NOT NULL,
    full_name                VARCHAR(150) NOT NULL,
    role                     VARCHAR(30)  NOT NULL,  -- FAMILY_ADMIN | FAMILY_MEMBER | ADMIN
    enabled                  TINYINT(1)   NOT NULL DEFAULT 1,
    account_locked_until     DATETIME     NULL,
    last_login_at            DATETIME     NULL,
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_family FOREIGN KEY (family_id) REFERENCES families(id),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS failed_login_attempts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(180) NOT NULL,
    attempted_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(45)  NULL,
    INDEX idx_fla_email_time (email, attempted_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    token_hash      VARCHAR(120) NOT NULL UNIQUE,
    expires_at      DATETIME     NOT NULL,
    used_at         DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(60)  NOT NULL,
    actor_email     VARCHAR(180) NULL,
    actor_user_id   BIGINT       NULL,
    ip_address      VARCHAR(45)  NULL,
    user_agent      VARCHAR(255) NULL,
    metadata_json   TEXT         NULL,
    occurred_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_type_time (event_type, occurred_at),
    INDEX idx_audit_actor (actor_user_id)
) ENGINE=InnoDB;

-- Tablas Legacy (Modulo 2 y 3) mantenidas temporalmente para evitar rotura de compilacion
CREATE TABLE IF NOT EXISTS family_members (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id            BIGINT NOT NULL,
    user_id              BIGINT NULL,
    full_name            VARCHAR(120) NOT NULL,
    first_name           VARCHAR(120) NOT NULL,
    email                VARCHAR(120) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    role                 VARCHAR(50) NOT NULL,
    phone                VARCHAR(30),
    joined_at            DATETIME NULL,
    age                  INT,
    autonomy_level       INT DEFAULT 50,
    responsibility_level INT DEFAULT 50,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_mem_f FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_mem_u FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Semilla de Datos Auth Básica
INSERT INTO users (full_name, email, password_hash, role, enabled) VALUES
('William Maestro', 'william@integrity.family', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', 'ADMIN', 1);
