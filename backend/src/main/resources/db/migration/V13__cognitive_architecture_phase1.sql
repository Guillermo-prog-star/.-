-- =============================================================================
-- V13: Arquitectura Cognitiva Familiar — Fase 1
-- Memoria multicapa + habilidades aprendidas + perfil de identidad familiar
-- SDD Sprint 7: Sistema Operativo Cognitivo Familiar
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Memoria cognitiva unificada (episódica, semántica, procedural, identitaria)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS family_memory (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    family_id       BIGINT          NOT NULL,
    memory_type     ENUM('EPISODIC','SEMANTIC','PROCEDURAL','IDENTITY') NOT NULL,
    semantic_key    VARCHAR(100)    NULL,
    content         TEXT            NOT NULL,
    importance_score DOUBLE         NOT NULL DEFAULT 0.5,
    source_type     VARCHAR(50)     NULL     COMMENT 'EVALUATION|REFLECTION|LEARNING_ENTRY|AI_INFERENCE|MANUAL',
    source_id       BIGINT          NULL,
    expires_at      DATETIME        NULL     COMMENT 'NULL = no expira; se usa para compresión cognitiva',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_family_memory_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    INDEX idx_family_memory_family_type (family_id, memory_type),
    INDEX idx_family_memory_importance  (family_id, importance_score DESC),
    INDEX idx_family_memory_semantic    (family_id, semantic_key),
    INDEX idx_family_memory_expires     (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Habilidades aprendidas — patrones de intervención validados empíricamente
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learned_skills (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    skill_name           VARCHAR(100)    NOT NULL,
    description          TEXT            NOT NULL,
    conditions           TEXT            NOT NULL COMMENT 'JSON array de condiciones activadoras',
    recommended_strategy TEXT            NOT NULL COMMENT 'JSON array de acciones recomendadas',
    dimension            VARCHAR(50)     NULL,
    success_rate         DOUBLE          NOT NULL DEFAULT 0.0,
    reuse_count          INT             NOT NULL DEFAULT 0,
    success_count        INT             NOT NULL DEFAULT 0,
    created_by_ai        BOOLEAN         NOT NULL DEFAULT TRUE,
    confidence           DOUBLE          NOT NULL DEFAULT 0.5,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_applied_at      DATETIME        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_skill_name (skill_name),
    INDEX idx_skill_success (success_rate DESC),
    INDEX idx_skill_confidence (confidence DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Perfil de identidad cognitiva familiar (uno por familia)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS family_identity_profile (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    family_id            BIGINT          NOT NULL,
    communication_style  VARCHAR(30)     NOT NULL DEFAULT 'UNKNOWN'
                                         COMMENT 'RESERVED|EXPRESSIVE|CONFLICTIVE|AVOIDANT|COLLABORATIVE',
    conflict_style       VARCHAR(30)     NOT NULL DEFAULT 'UNKNOWN'
                                         COMMENT 'CONFRONTATIONAL|AVOIDANT|NEGOTIATING|EXPLOSIVE|PASSIVE',
    ritual_patterns      TEXT            NULL     COMMENT 'JSON array de rituales familiares',
    stress_triggers      TEXT            NULL     COMMENT 'JSON array de factores de estrés',
    emotional_expression VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM'
                                         COMMENT 'LOW|MEDIUM|HIGH',
    adaptability_index   DOUBLE          NOT NULL DEFAULT 0.5,
    identity_narrative   TEXT            NULL     COMMENT 'Narrativa generada por la IA sobre la familia',
    evolution_stage      VARCHAR(20)     NOT NULL DEFAULT 'INITIAL'
                                         COMMENT 'INITIAL|RECOGNITION|ADJUSTMENT|CONSOLIDATION|AUTONOMOUS',
    completed_cycles     INT             NOT NULL DEFAULT 0,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_family_identity (family_id),
    CONSTRAINT fk_identity_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Habilidades iniciales (seed) — conocimiento de arranque del sistema cognitivo
-- -----------------------------------------------------------------------------
INSERT INTO learned_skills (skill_name, description, conditions, recommended_strategy, dimension, success_rate, confidence, created_by_ai) VALUES
(
    'micro_missions_high_stress',
    'Familias con alto estrés laboral abandonan misiones largas. Fragmentar en microacciones de 5-10 minutos maximiza adherencia.',
    '["high_stress_triggers", "low_adherence", "tasks_too_long"]',
    '["assign_micro_tasks_5min", "reduce_weekly_task_count", "add_positive_reinforcement"]',
    'HABITOS',
    0.72,
    0.75,
    FALSE
),
(
    'short_dialogues_low_communication',
    'Familias con puntaje de comunicación bajo responden mejor a diálogos guiados de 3 preguntas que a conversaciones abiertas.',
    '["communication_score_lt_40", "conflict_style_avoidant"]',
    '["guided_3question_dialogue", "written_reflection_before_talk", "avoid_open_confrontation"]',
    'COMUNICACION',
    0.68,
    0.70,
    FALSE
),
(
    'celebration_rituals_consolidation',
    'Reconocer pequeños logros con rituales familiares breves incrementa adherencia en etapas de consolidación.',
    '["evolution_stage_consolidation", "adherence_gt_60", "completed_cycles_gte_3"]',
    '["weekly_celebration_ritual", "visual_progress_board", "family_gratitude_moment"]',
    'EMOCIONES',
    0.81,
    0.85,
    FALSE
);
