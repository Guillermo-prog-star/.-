-- V4: Hacer opcional la vinculación de familia para permitir el flujo de registro inicial
ALTER TABLE users MODIFY family_id BIGINT NULL;
