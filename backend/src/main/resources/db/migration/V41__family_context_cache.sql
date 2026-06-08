-- V41: Motor de Contexto Familiar
-- Almacena el último estado computado de cada familia.
-- Se recalcula on-demand y vía scheduler cada 4 horas.

CREATE TABLE IF NOT EXISTS family_context_snapshots (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id            BIGINT NOT NULL UNIQUE,
    -- Señales de estado
    connection_level     VARCHAR(20) NOT NULL DEFAULT 'MEDIA',   -- ALTA | MEDIA | BAJA
    stress_level         VARCHAR(20) NOT NULL DEFAULT 'BAJO',    -- BAJO | MODERADO | ALTO | CRITICO
    communication_trend  VARCHAR(20) NOT NULL DEFAULT 'ESTABLE', -- MEJORANDO | ESTABLE | DETERIORANDO
    participation_level  VARCHAR(20) NOT NULL DEFAULT 'MEDIA',   -- ALTA | MEDIA | BAJA
    overall_trend        VARCHAR(20) NOT NULL DEFAULT 'ESTABLE', -- ASCENDENTE | ESTABLE | DESCENDENTE | CRITICA
    overall_mood         VARCHAR(30) NOT NULL DEFAULT 'SERENO',  -- CELEBRANDO | CRECIENDO | SERENO | TENSO | EN_CRISIS
    -- Métricas clave
    icf_current          DOUBLE,
    risk_level           VARCHAR(20),
    days_without_activity INT NOT NULL DEFAULT 0,
    current_streak       INT NOT NULL DEFAULT 0,
    active_rituals_count INT NOT NULL DEFAULT 0,
    sprint_progress      DOUBLE,
    -- Listas JSON
    alerts               JSON,           -- señales de atención
    recommendations      JSON,           -- acciones sugeridas
    -- Control
    computed_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctx_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);
