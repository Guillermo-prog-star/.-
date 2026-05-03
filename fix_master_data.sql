USE integrity_family;
-- 1. Crear el usuario William si no existe
INSERT IGNORE INTO users (id, active, email, full_name, password) 
VALUES (1, 1, 'william@integrity.com', 'William Lopez', 'raw_password_will_be_encoded');

-- 2. Crear la familia IF-CO-QUI-2026-0004
INSERT IGNORE INTO families (id, name, family_code, sentinel_active, created_by_id, current_milestone) 
VALUES (2, 'Familia Lopez Rivera', 'IF-CO-QUI-2026-0004', 1, 1, 'M1');

-- 3. Asegurar que existan los snapshots (Doble validación)
DELETE FROM risk_snapshots WHERE family_id = 2;
INSERT INTO risk_snapshots (family_id, icf, risk_level, consciousness_level, has_crisis, created_at) 
VALUES 
(2, 25.5, 'ALTO', 2, 1, NOW()), 
(2, 20.1, 'CRITICO', 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)), 
(2, 15.0, 'EMERGENCIA', 1, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR));