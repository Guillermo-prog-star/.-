-- Family Action Engine (IFRM-D Hito 5): registro de ejecuciones de comandos semánticos
-- del Hogar Digital, usado para idempotencia (evita reejecutar la misma acción si el
-- cliente reintenta con el mismo Idempotency-Key) y como rastro auditable mínimo.
CREATE TABLE IF NOT EXISTS family_action_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id CHAR(36) NOT NULL,
    action VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    result_sprint_ref CHAR(36) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_family_action_idem UNIQUE (family_id, action, idempotency_key)
);
