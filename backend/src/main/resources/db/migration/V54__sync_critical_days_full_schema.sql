-- =========================================================================
-- Ecosistema: Integrity Family
-- Componente: Módulo de Contención IA / Días Críticos
-- Descripción: Creación formal de la tabla para el registro de alertas
-- =========================================================================

CREATE TABLE IF NOT EXISTS critical_days (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT NULL,
    category    VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_critical_days_member FOREIGN KEY (member_id) 
        REFERENCES members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
