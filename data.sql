-- 1. Población de Roles (Garantiza que existan antes que el usuario)
INSERT INTO roles (name) SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');
INSERT INTO roles (name) SELECT 'USER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER');

-- 2. Usuario Administrador Maestro (William)
-- El password es 'admin123' encriptado con BCrypt
INSERT INTO users (full_name, email, password, active) 
SELECT 'William Lopez', 'william@integrityfamily.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xdqD1884W6G6G2DW', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'william@integrityfamily.com');

-- 3. Vinculación de Rol (Asignar ADMIN al primer usuario)
-- Nota: Esto asume que los IDs autoincrementales son 1. 
INSERT INTO user_roles (user_id, role_id) 
SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 1);s