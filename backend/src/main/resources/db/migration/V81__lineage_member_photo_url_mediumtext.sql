-- V81: Amplía photo_url de VARCHAR(500) a MEDIUMTEXT para soportar imágenes en base64
ALTER TABLE lineage_members MODIFY COLUMN photo_url MEDIUMTEXT;
