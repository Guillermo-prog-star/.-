-- V6: Relax Member Constraints
-- Permite registrar miembros sin necesidad inmediata de email/password (ej: niños o registros rápidos)
ALTER TABLE family_members MODIFY first_name VARCHAR(120) NULL;
ALTER TABLE family_members MODIFY email VARCHAR(120) NULL;
ALTER TABLE family_members MODIFY password VARCHAR(255) NULL;

-- Asegurar que los niveles tengan valores por defecto si no se especifican
ALTER TABLE family_members ALTER autonomy_level SET DEFAULT 50;
ALTER TABLE family_members ALTER responsibility_level SET DEFAULT 50;
