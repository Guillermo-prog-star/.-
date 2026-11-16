-- V110: DailyVitalityLog (ADR-009) -- Fase 4, base biologica.
--
-- Unica fase del metodo externo de 8 fases sin cobertura digital existente
-- (ver ADR-009, Context). Todos los campos numericos son NULL-ables -- un
-- miembro puede registrar solo sueno un dia y solo ejercicio otro, sin
-- forzar el panel completo (misma logica de "simplicidad progresiva"
-- documentada en el ADR).
--
-- No se reutiliza sprint_dailies: su sprint_id es NOT NULL (requiere sprint
-- activo) y sus campos son texto libre, no numericos -- ver ADR-009,
-- seccion "Por que no se reutiliza SprintDaily".
CREATE TABLE IF NOT EXISTS daily_vitality_logs (
    id                                BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_member_id                  BIGINT NOT NULL,
    log_date                          DATE NOT NULL,

    sleep_hours                       DOUBLE NULL,
    sleep_quality                     INT NULL,
    exercise_minutes                  INT NULL,
    nutrition_quality                 INT NULL,
    screen_time_before_bed_minutes    INT NULL,
    fatigue_level                     INT NULL,

    source                            VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at                        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dvl_family_member FOREIGN KEY (family_member_id) REFERENCES family_members(id) ON DELETE CASCADE,
    CONSTRAINT uq_dvl_member_date UNIQUE (family_member_id, log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_dvl_member_date ON daily_vitality_logs (family_member_id, log_date);
