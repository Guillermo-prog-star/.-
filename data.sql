-- [SDD] Script de Sembrado Maestro - Compatibilidad Total MySQL 8.4
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO roles (name) VALUES ('ROLE_USER') ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Usuario Maestro
-- Password: admin123
INSERT INTO users (full_name, email, password_hash, enabled, created_at, family_id) 
VALUES ('William Lopez', 'william@integrity.family', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', true, NOW(), 1)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- Vinculacion de Rol
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.email = 'william@integrity.family' AND r.name = 'ROLE_ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- 3. Familia Lopez Rivera
INSERT INTO families (id, name, family_code, current_milestone, icf_score, total_tasks, completed_tasks, sentinel_active, created_by_id, created_at)
VALUES (1, 'Familia Lopez Rivera', 'IF-CO-QUI-2026-0001', 'MES_00_DIAGNOSTICO_BASE', 75, 12, 8, false, 1, NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 4. Miembros de Familia
INSERT INTO family_members (full_name, first_name, password, role, email, family_id, user_id)
SELECT 'William Lopez', 'William', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', 'PADRE', 'william@integrity.family', 1, u.id
FROM users u WHERE u.email = 'william@integrity.family'
AND NOT EXISTS (SELECT 1 FROM family_members WHERE email = 'william@integrity.family');

INSERT INTO family_members (full_name, first_name, password, role, email, family_id)
SELECT 'Maria Rivera', 'Maria', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', 'MADRE', 'maria@example.com', f.id
FROM families f WHERE f.family_code = 'IF-CO-QUI-2026-0001'
AND NOT EXISTS (SELECT 1 FROM family_members WHERE email = 'maria@example.com');

-- 5. Items de Checklist
INSERT INTO checklist_items (description, dimension, source, completed, family_id, created_at)
SELECT 'Establecer rutina de dialogo familiar diaria', 'COMUNICACION', 'MES_00_DIAGNOSTICO_BASE', true, f.id, NOW()
FROM families f WHERE f.family_code = 'IF-CO-QUI-2026-0001';

INSERT INTO checklist_items (description, dimension, source, completed, family_id, created_at)
SELECT 'Definir presupuesto de ahorro colaborativo', 'FINANZAS', 'MES_00_DIAGNOSTICO_BASE', true, f.id, NOW()
FROM families f WHERE f.family_code = 'IF-CO-QUI-2026-0001';

INSERT INTO checklist_items (description, dimension, source, completed, family_id, created_at)
SELECT 'Implementar tablero de compromisos visibles', 'INTEGRIDAD', 'MES_00_DIAGNOSTICO_BASE', false, f.id, NOW()
FROM families f WHERE f.family_code = 'IF-CO-QUI-2026-0001';

SET FOREIGN_KEY_CHECKS = 1;