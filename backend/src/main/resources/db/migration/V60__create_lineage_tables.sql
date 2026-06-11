-- ============================================================
-- V60: Módulo Linaje Generacional
-- Permite registrar ancestros históricos con datos incompletos,
-- conectar generaciones y construir el árbol genealógico familiar.
-- ============================================================

-- Tabla principal: un linaje por familia
CREATE TABLE family_lineages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    lineage_code    VARCHAR(50) NOT NULL UNIQUE,
    title           VARCHAR(255),
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lineage_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

-- Miembros del linaje a través de todas las generaciones
CREATE TABLE lineage_members (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    lineage_id              BIGINT NOT NULL,
    first_name              VARCHAR(100),
    last_name               VARCHAR(200),
    avatar_initials         VARCHAR(5),
    avatar_color            VARCHAR(150),
    generation              INT NOT NULL DEFAULT 0,
    -- 0=bisabuelos, 1=abuelos, 2=padres, 3=familia actual, 4=futura
    status                  VARCHAR(20) NOT NULL DEFAULT 'unknown',
    -- alive | deceased | unknown | future
    birth_year              INT,
    birth_year_approximate  BOOLEAN DEFAULT FALSE,
    birth_date              DATE,
    death_year              INT,
    death_date              DATE,
    origin                  VARCHAR(300),
    role_label              VARCHAR(255),
    confidence_level        INT DEFAULT 50,
    data_source             VARCHAR(500),
    story                   TEXT,
    position_x              FLOAT DEFAULT 0,
    position_y              FLOAT DEFAULT 0,
    family_member_id        BIGINT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lmember_lineage FOREIGN KEY (lineage_id) REFERENCES family_lineages(id) ON DELETE CASCADE,
    CONSTRAINT fk_lmember_fmember FOREIGN KEY (family_member_id) REFERENCES family_members(id) ON DELETE SET NULL
);

-- Relaciones entre miembros (padre→hijo, pareja)
CREATE TABLE lineage_relationships (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    lineage_id          BIGINT NOT NULL,
    from_member_id      BIGINT NOT NULL,
    to_member_id        BIGINT NOT NULL,
    relationship_type   VARCHAR(50) DEFAULT 'biological',
    -- biological | adoptive | step | couple
    is_couple           BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_lrel_lineage   FOREIGN KEY (lineage_id)      REFERENCES family_lineages(id)   ON DELETE CASCADE,
    CONSTRAINT fk_lrel_from      FOREIGN KEY (from_member_id)  REFERENCES lineage_members(id)   ON DELETE CASCADE,
    CONSTRAINT fk_lrel_to        FOREIGN KEY (to_member_id)    REFERENCES lineage_members(id)   ON DELETE CASCADE
);

-- Eventos históricos asociados a un miembro
CREATE TABLE lineage_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    event_year      VARCHAR(20),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    event_type      VARCHAR(50) DEFAULT 'milestone',
    -- birth | death | marriage | migration | trauma | achievement | milestone
    is_approximate  BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0,
    CONSTRAINT fk_levent_member FOREIGN KEY (member_id) REFERENCES lineage_members(id) ON DELETE CASCADE
);

-- Índices para consultas frecuentes
CREATE INDEX idx_lineage_family   ON family_lineages(family_id);
CREATE INDEX idx_lmember_lineage  ON lineage_members(lineage_id);
CREATE INDEX idx_lmember_gen      ON lineage_members(lineage_id, generation);
CREATE INDEX idx_lrel_lineage     ON lineage_relationships(lineage_id);
CREATE INDEX idx_levent_member    ON lineage_events(member_id);
