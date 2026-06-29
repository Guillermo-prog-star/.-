-- Módulo EcosistemaDeApoyo
-- 5 niveles de red: FAMILIAR · PROFESSIONAL (ya existe en support) · INSTITUTIONAL · COMMUNITY · TERRITORIAL
-- Principio rector: la familia autoriza cada conexión. Nivel 3 solo recibe datos agregados y anónimos.

-- Registro de participantes del ecosistema (organizaciones, actores comunitarios, miembros de red natural)
CREATE TABLE IF NOT EXISTS ecosystem_participants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    network_type    ENUM('FAMILIAR','INSTITUTIONAL','COMMUNITY','TERRITORIAL') NOT NULL,
    description     TEXT,
    contact_email   VARCHAR(180),
    contact_phone   VARCHAR(30),
    website         VARCHAR(300),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Vínculo entre una familia y un participante del ecosistema
CREATE TABLE IF NOT EXISTS family_ecosystem_links (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT NOT NULL,
    participant_id   BIGINT NOT NULL,
    network_type     ENUM('FAMILIAR','INSTITUTIONAL','COMMUNITY','TERRITORIAL') NOT NULL,
    access_level     TINYINT NOT NULL DEFAULT 1 COMMENT '1=Familiar 2=Interdisciplinario 3=Intersectorial',
    objective        TEXT,
    responsibilities TEXT,
    valid_from       DATE,
    valid_until      DATE,
    status           ENUM('INVITED','ACTIVE','SUSPENDED','REVOKED') NOT NULL DEFAULT 'INVITED',
    -- alcance de acceso (mínimo privilegio)
    can_view_icf_score      BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_risk_level     BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_plan_summary   BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_sprint_progress BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_crisis_history BOOLEAN NOT NULL DEFAULT FALSE,
    can_receive_alerts      BOOLEAN NOT NULL DEFAULT FALSE,
    -- trazabilidad de consentimiento
    invited_by_email  VARCHAR(180) NOT NULL,
    invited_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consented_by_email VARCHAR(180),
    consented_at       DATETIME,
    revoked_by_email   VARCHAR(180),
    revoked_at         DATETIME,
    revocation_reason  TEXT,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eco_link_family      FOREIGN KEY (family_id)      REFERENCES families(id)                ON DELETE CASCADE,
    CONSTRAINT fk_eco_link_participant FOREIGN KEY (participant_id)  REFERENCES ecosystem_participants(id)  ON DELETE RESTRICT
);

-- Contactos dentro de una organización participante (persona física de contacto)
CREATE TABLE IF NOT EXISTS ecosystem_participant_contacts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    participant_id BIGINT NOT NULL,
    full_name      VARCHAR(200) NOT NULL,
    email          VARCHAR(180),
    role_title     VARCHAR(120),
    phone          VARCHAR(30),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eco_contact_participant FOREIGN KEY (participant_id) REFERENCES ecosystem_participants(id) ON DELETE CASCADE
);

-- Log de acceso del ecosistema (auditoría por nivel)
CREATE TABLE IF NOT EXISTS ecosystem_access_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    link_id        BIGINT NOT NULL,
    family_id      BIGINT NOT NULL,
    actor_email    VARCHAR(180),
    action         VARCHAR(80) NOT NULL,
    detail         TEXT,
    access_level   TINYINT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eco_log_link FOREIGN KEY (link_id) REFERENCES family_ecosystem_links(id) ON DELETE CASCADE
);
