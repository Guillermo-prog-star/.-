-- V27: Espacio privado de reflexión personal (Mi Espacio).
-- Cada usuario tiene su propio diario emocional, completamente privado e independiente del núcleo familiar.
CREATE TABLE private_journal_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    emotional_state VARCHAR(20)  NOT NULL DEFAULT 'NEUTRAL',
    category        VARCHAR(30)  NOT NULL DEFAULT 'REFLEXION',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_journal_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_journal_user_created (user_id, created_at DESC)
);
