-- Limpieza de seguridad en orden jerárquico (H2 compatible)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE user_roles; TRUNCATE TABLE users; TRUNCATE TABLE roles;
SET REFERENTIAL_INTEGRITY TRUE;

-- Carga de Roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_FAMILY'), (2, 'ROLE_ADMIN');

-- Usuario Raíz: william@integrity.family / Admin123*
-- Se define active = 1 para evitar el bloqueo de Spring Security
INSERT INTO users (id, email, password, full_name, active) 
VALUES (1, 'william@integrity.family', '$2a$10$8.UnVuG9HHgffUDAIkq8fOuVGkQrzgVymGe07xd0Dmn5M.C4/41/N2', 'William Lopez', 1);

-- Asignación de Privilegios
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);