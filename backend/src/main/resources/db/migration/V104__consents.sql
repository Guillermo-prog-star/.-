-- V104: Consentimiento real para interoperabilidad (Fase 2 del programa de
-- interoperabilidad con el ecosistema de salud). Distinto del flag simple
-- consented_by_email/consented_at usado hoy en ecosystem/support (que vive
-- como columnas de otra tabla, atado 1:1 a un participante o profesional):
-- aqui el consentimiento es la entidad principal, y cubre explicitamente el
-- caso de compartir con una institucion externa (Ministerio, IPS) que no es
-- un participante del ecosistema ni un miembro de la red de apoyo.
CREATE TABLE IF NOT EXISTS consents (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id          BIGINT NOT NULL,
    member_id          BIGINT NULL,
    purpose            ENUM('ECOSYSTEM_SHARING','SUPPORT_NETWORK_SHARING','HEALTH_INTEROPERABILITY','RESEARCH') NOT NULL,
    scope              TEXT NOT NULL,
    grantee_reference  VARCHAR(255) NOT NULL,
    status             ENUM('GRANTED','REVOKED','PENDING','NOT_REQUIRED') NOT NULL,
    granted_by_email   VARCHAR(180) NOT NULL,
    granted_at         DATETIME NOT NULL,
    expires_at         DATETIME NULL,
    revoked_by_email   VARCHAR(180) NULL,
    revoked_at         DATETIME NULL,
    revocation_reason  TEXT NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consents_family FOREIGN KEY (family_id)  REFERENCES families(id)        ON DELETE CASCADE,
    CONSTRAINT fk_consents_member FOREIGN KEY (member_id)  REFERENCES family_members(id)  ON DELETE CASCADE,
    INDEX idx_consents_family (family_id),
    INDEX idx_consents_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
