-- V64: Agrega campo photo_url a lineage_members
-- Permite mostrar foto de perfil real en el nodo SVG del árbol generacional

ALTER TABLE lineage_members
    ADD COLUMN photo_url VARCHAR(500) NULL COMMENT 'URL de foto de perfil del miembro (opcional)';
