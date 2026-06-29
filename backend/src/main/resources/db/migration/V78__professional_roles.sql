-- Roles para profesionales de la Red de Apoyo Humano
-- Se insertan solo si no existen para mantener idempotencia
INSERT IGNORE INTO roles (name) VALUES ('ROLE_THERAPIST');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ORIENTADOR');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_SOCIAL_WORKER');
