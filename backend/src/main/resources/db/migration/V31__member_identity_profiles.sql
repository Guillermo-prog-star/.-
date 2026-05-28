-- =========================================================
-- V31: Perfil de Identidad Individual por Miembro
-- =========================================================

CREATE TABLE member_identity_profiles (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id             BIGINT       NOT NULL UNIQUE,

    -- Estilo conversacional inferido de interacciones anteriores
    communication_style   VARCHAR(20)  NULL
                              COMMENT 'DIRECT | REFLECTIVE | AVOIDANT | ASSERTIVE',

    -- Escalas de 1 a 5 derivadas de patrones de respuesta
    reflexivity_level     TINYINT      NOT NULL DEFAULT 3
                              COMMENT '1=impulsivo, 5=muy reflexivo',
    emotional_sensitivity TINYINT      NOT NULL DEFAULT 3
                              COMMENT '1=muy contenido, 5=muy sensible',

    -- Arrays JSON de strings inferidos de conversaciones
    evasion_patterns      TEXT         NULL
                              COMMENT 'JSON array — temas o dinámicas que el miembro evita',
    motivators            TEXT         NULL
                              COMMENT 'JSON array — qué activa la participación del miembro',

    -- Resistencia al cambio conductual
    change_resistance     VARCHAR(10)  NOT NULL DEFAULT 'MED'
                              COMMENT 'LOW | MED | HIGH',

    last_updated          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_mip_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE CASCADE,

    INDEX idx_mip_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
