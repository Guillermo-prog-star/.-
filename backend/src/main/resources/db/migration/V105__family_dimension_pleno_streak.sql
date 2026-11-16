-- V105: Streaks de sostenimiento en PLENO por dimension ICF (ADR-003).
-- Identidad Familiar / Patrimonio Automatico Familiar (PAF) no se autoevalua
-- (ver ADR-003 para las alternativas descartadas: sexto nivel de escala,
-- texto "funciona para mi familia", medicion por trayectoria individual) --
-- se infiere de sostener dimScore >= 90 durante >= 3 ciclos consecutivos de
-- evaluacion. Mismo patron que consecutive_improvements/consecutive_deteriorations
-- (contador que se incrementa o resetea por evento, sin tabla de historial nueva).
ALTER TABLE family_longitudinal_state
    ADD COLUMN emociones_pleno_streak     INT NOT NULL DEFAULT 0 AFTER dim_tiempos,
    ADD COLUMN comunicacion_pleno_streak  INT NOT NULL DEFAULT 0 AFTER emociones_pleno_streak,
    ADD COLUMN habitos_pleno_streak       INT NOT NULL DEFAULT 0 AFTER comunicacion_pleno_streak,
    ADD COLUMN tiempos_pleno_streak       INT NOT NULL DEFAULT 0 AFTER habitos_pleno_streak;
