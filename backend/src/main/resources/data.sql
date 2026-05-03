-- [SDD] Script de Sembrado Maestro - HASH VERIFICADO
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Roles removidos por SDD (Migracion a String role)

-- 2. Usuario Maestro
-- Password: admin123
REPLACE INTO users (id, full_name, email, password_hash, role, enabled, created_at) 
VALUES (1, 'William Lopez', 'william@integrity.family', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', 'ADMIN', true, NOW());

-- 3. Familia Lopez Rivera
REPLACE INTO families (id, name, family_code, current_milestone, icf_score, total_tasks, completed_tasks, sentinel_active, created_by_id, created_at)
VALUES (1, 'Familia Lopez Rivera', 'IF-CO-QUI-2026-0001', 'MES_00_DIAGNOSTICO_BASE', 75, 12, 8, false, 1, NOW());

-- 4. Miembros de Familia
REPLACE INTO family_members (id, full_name, first_name, password, role, email, family_id, user_id)
VALUES (1, 'William Lopez', 'William', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', 'PADRE', 'william@integrity.family', 1, 1);

-- 5. Items de Checklist
REPLACE INTO checklist_items (id, description, dimension, source, completed, family_id, created_at)
VALUES (1, 'Establecer rutina de dialogo familiar diaria', 'COMUNICACION', 'MES_00_DIAGNOSTICO_BASE', true, 1, NOW());

SET FOREIGN_KEY_CHECKS = 1;