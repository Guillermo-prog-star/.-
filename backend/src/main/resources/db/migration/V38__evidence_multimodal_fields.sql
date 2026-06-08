-- V38: Campos multimodales para evidencias familiares
-- emotion: emoción reportada al subir la evidencia
-- latitude / longitude: coordenadas si la familia comparte su ubicación
-- member_name: quién capturó la evidencia (nombre libre, sin FK dura)
-- media_data: datos base64 para foto/audio cuando no hay cloud storage
-- media_mime: tipo MIME (image/jpeg, audio/webm, etc.)

ALTER TABLE task_evidences
    ADD COLUMN emotion      VARCHAR(80)  NULL AFTER description,
    ADD COLUMN latitude     DOUBLE       NULL AFTER emotion,
    ADD COLUMN longitude    DOUBLE       NULL AFTER latitude,
    ADD COLUMN member_name  VARCHAR(150) NULL AFTER longitude,
    ADD COLUMN media_data   LONGTEXT     NULL AFTER member_name,
    ADD COLUMN media_mime   VARCHAR(80)  NULL AFTER media_data;
