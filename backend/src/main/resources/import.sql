SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_roles; TRUNCATE TABLE users; TRUNCATE TABLE roles;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO roles (id, name) VALUES (1, 'ROLE_FAMILY'), (2, 'ROLE_USER');

-- William Lopez: Hash real de 'Admin123*'
INSERT INTO users (id, active, email, full_name, password) 
VALUES (1, 1, 'willibla1957@gmail.com', 'William Lopez', '$2a$10$8.UnVuG9HHgffUDAIkq8fOuVGkQrzgVymGe07xd0Dmn5M.C4/41/N2');

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1), (1, 2);