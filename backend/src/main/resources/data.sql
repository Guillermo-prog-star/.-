-- Crear usuario inicial William para evitar Error 500
INSERT INTO users (full_name, email, password, active) 
SELECT 'William Lopez', 'william@integrity.family', '$2a$10$8.UnVuG9HHgffUDAlk8qnO6CkS.87L7G4pS6D2L2x2x2x2x2x2x2x', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'william@integrity.family');