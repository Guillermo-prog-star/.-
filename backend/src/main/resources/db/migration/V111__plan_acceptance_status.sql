-- V111: PlanAcceptanceStatus (ADR-010) -- declaracion de intencion, Fase 0b.
--
-- Hoy `ImprovementPlan` se activa sin ningun paso de confirmacion por parte
-- de la familia (ver ADR-010, Context: PlanGenerationService.java:214 persiste
-- el plan generado por IA sin ninguna aceptacion explicita). Se agregan
-- columnas de aceptacion directamente sobre `plans` -- no se crea tabla nueva
-- porque la invariante "solo 1 plan activo por familia"
-- (PlanGenerationService.java:150-156) ya garantiza que no hay historial de
-- multiples planes que preservar (ver ADR-010, Decision 2).
ALTER TABLE plans
    ADD COLUMN acceptance_status   VARCHAR(20) NOT NULL DEFAULT 'PROPOSED' AFTER ai_generated_at,
    ADD COLUMN accepted_at         DATETIME NULL AFTER acceptance_status,
    ADD COLUMN accepted_by         VARCHAR(120) NULL AFTER accepted_at,
    ADD COLUMN intention_statement TEXT NULL AFTER accepted_by;

-- Backfill: los planes ya existentes se marcan ACCEPTED, no PROPOSED -- ya
-- estan operando activamente para familias reales; dejarlos en PROPOSED
-- falsearia el historial retroactivamente (ver ADR-010, Decision 5).
-- accepted_by queda NULL: quien acepto un plan antes de este ADR es
-- desconocido, no se inventa un valor.
UPDATE plans
SET acceptance_status = 'ACCEPTED',
    accepted_at = COALESCE(ai_generated_at, NOW())
WHERE acceptance_status = 'PROPOSED';
