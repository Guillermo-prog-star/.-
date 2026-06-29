-- V80: Correcciones a tablas del módulo EcosistemaDeApoyo (V79)
--
-- Problema 1: network_type ENUM en V79 no incluía 'PROFESSIONAL'.
--   El enum Java NetworkType tiene FAMILIAR·PROFESSIONAL·INSTITUTIONAL·COMMUNITY·TERRITORIAL.
--   Sin este valor MySQL rechaza insertar participantes o links de tipo PROFESSIONAL.
--
-- Problema 2: las 4 tablas de V79 no especificaban ENGINE ni CHARSET.
--   MySQL 8.4 usa utf8mb4 por defecto, pero lo hacemos explícito para consistencia.
--
-- ALTER TABLE ... MODIFY COLUMN para agregar un valor a ENUM es safe en MySQL 8.x
-- (operación in-place, no reconstruye la tabla).

ALTER TABLE ecosystem_participants
    MODIFY COLUMN network_type
        ENUM('FAMILIAR','PROFESSIONAL','INSTITUTIONAL','COMMUNITY','TERRITORIAL') NOT NULL;

ALTER TABLE family_ecosystem_links
    MODIFY COLUMN network_type
        ENUM('FAMILIAR','PROFESSIONAL','INSTITUTIONAL','COMMUNITY','TERRITORIAL') NOT NULL;

-- Charset explícito (idempotente en MySQL 8.4, no modifica datos existentes)
ALTER TABLE ecosystem_participants         CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE family_ecosystem_links         CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ecosystem_participant_contacts CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ecosystem_access_log           CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
