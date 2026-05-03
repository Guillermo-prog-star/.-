-- V5: Eliminar columna de rol legacy en users para usar la tabla user_roles
-- SDD: Centralización de identidad en la tabla 'roles' y 'user_roles'
ALTER TABLE users DROP COLUMN role;
