-- V107: Borrador de Nota de Seguimiento Profesional (ADR-006).
--
-- Traslada la generacion del "resumen clinico" del Panel Profesional
-- (antes una plantilla en Angular, generateAiSummaryText(), sin registro
-- ni auditoria) a una entidad versionada en backend. sourceSnapshot
-- congela los valores de FamilyDataView vistos al generar -- vienen de
-- estado operacional mutable (ICF/riesgo/sprint actuales), y sin
-- congelarlos el mismo borrador "cambiaria" al leerse despues (mismo
-- principio de ADR-004: el estado operacional mutable no es fuente de
-- verdad reproducible).
--
-- Ciclo de vida deliberadamente minimo (GENERATED/VOIDED): el flujo de
-- aprobacion/firma clinica completo (UNDER_REVIEW/APPROVED/SIGNED) queda
-- fuera de alcance hasta resolver preguntas de gobernanza no decididas
-- en este ADR (ver ADR-006, Context). generator_type incluye
-- 'AI_ASSISTED' en el catalogo aunque hoy solo se escribe
-- 'RULE_BASED_TEMPLATE' -- reservado para una Fase 5 futura (ADR-007,
-- no decidida), evita una migracion futura solo para ampliar el ENUM.
CREATE TABLE IF NOT EXISTS professional_follow_up_drafts (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,

    family_id                BIGINT NOT NULL,
    assignment_id            BIGINT NOT NULL,

    generated_by_user_email  VARCHAR(150) NOT NULL,
    generated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    generator_type           ENUM('RULE_BASED_TEMPLATE','AI_ASSISTED') NOT NULL DEFAULT 'RULE_BASED_TEMPLATE',
    template_version         VARCHAR(40) NOT NULL,

    source_snapshot          TEXT NOT NULL,
    narrative_text           TEXT NOT NULL,

    status                   ENUM('GENERATED','VOIDED') NOT NULL DEFAULT 'GENERATED',

    CONSTRAINT fk_pfud_assignment FOREIGN KEY (assignment_id) REFERENCES family_support_assignments(id) ON DELETE CASCADE,
    INDEX idx_pfud_assignment (assignment_id, status),
    INDEX idx_pfud_family     (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Vincula la nota final (si el profesional la guarda a partir de un
-- borrador) con el borrador que la origino, sin duplicar el flujo de
-- guardado ya existente en POST /support/notes (ADR-006, Decision 3).
ALTER TABLE support_professional_notes
    ADD COLUMN follow_up_draft_id BIGINT NULL AFTER support_member_id,
    ADD CONSTRAINT fk_spn_follow_up_draft FOREIGN KEY (follow_up_draft_id)
        REFERENCES professional_follow_up_drafts(id) ON DELETE SET NULL;
