-- V70: Índice de Capital Familiar (ICaF)
-- Extiende family_longitudinal_state con columnas longitudinales del ICaF.
-- Crea tablas nuevas: family_capital_snapshots, family_critical_events,
-- observatory_snapshots.
-- Compatible con MySQL 8.4 (ADD COLUMN IF NOT EXISTS es MariaDB-only;
-- se usa procedimiento con information_schema, igual que V68).

-- ─── 1. EXTENSIÓN DE family_longitudinal_state ───────────────────────────────
-- Agrega trayectoria longitudinal ICaF (6m / 12m / 36m) y nivel de madurez.

DROP PROCEDURE IF EXISTS AddIcafLongitudinalColumnsV70;

DELIMITER //
CREATE PROCEDURE AddIcafLongitudinalColumnsV70()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_current') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_current DOUBLE NULL COMMENT 'ICaF último calculado (0-100)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_6m_ago') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_6m_ago DOUBLE NULL COMMENT 'ICaF hace 6 meses';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_12m_ago') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_12m_ago DOUBLE NULL COMMENT 'ICaF hace 12 meses';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_36m_ago') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_36m_ago DOUBLE NULL COMMENT 'ICaF hace 36 meses';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_madurez') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_madurez TINYINT NULL COMMENT 'Nivel de madurez familiar 1-5';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_trend') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_trend VARCHAR(20) NULL COMMENT 'IMPROVING | STABLE | DECLINING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME  = 'family_longitudinal_state'
                     AND COLUMN_NAME = 'icaf_last_calculated') THEN
        ALTER TABLE family_longitudinal_state
          ADD COLUMN icaf_last_calculated DATETIME NULL COMMENT 'Timestamp del último cálculo ICaF';
    END IF;
END //
DELIMITER ;

CALL AddIcafLongitudinalColumnsV70();
DROP PROCEDURE IF EXISTS AddIcafLongitudinalColumnsV70;

-- ─── 2. family_capital_snapshots ─────────────────────────────────────────────
-- Un registro por cada recálculo del ICaF.
-- Permite reconstruir la trayectoria completa de Capital Familiar por familia.
-- trigger_type: ASSESSMENT | SPRINT_CLOSE | CRITICAL_EVENT | SCHEDULED

CREATE TABLE IF NOT EXISTS family_capital_snapshots (
  id                    BIGINT        NOT NULL AUTO_INCREMENT,
  family_id             BIGINT        NOT NULL,

  -- Índice global
  icaf                  DOUBLE        NOT NULL COMMENT 'ICaF global 0-100',
  madurez_nivel         TINYINT       NOT NULL COMMENT 'Nivel de madurez 1-5',

  -- 11 dominios (nullable: se calculan de forma incremental según sprint activo)
  dom_cohesion          DOUBLE        NULL COMMENT 'Dominio 1 — ICF actual (peso 20%)',
  dom_confianza         DOUBLE        NULL COMMENT 'Dominio 2 — Confianza mutua (peso 12%)',
  dom_resiliencia       DOUBLE        NULL COMMENT 'Dominio 3 — Resolución de crisis (peso 12%)',
  dom_comunicacion      DOUBLE        NULL COMMENT 'Dominio 4 — Calidad comunicación (peso 10%)',
  dom_autonomia         DOUBLE        NULL COMMENT 'Dominio 5 — Autonomía responsable (peso 8%)',
  dom_bienestar         DOUBLE        NULL COMMENT 'Dominio 6 — Bienestar emocional (peso 8%)',
  dom_proposito         DOUBLE        NULL COMMENT 'Dominio 7 — Propósito individual/familiar (peso 8%)',
  dom_integracion       DOUBLE        NULL COMMENT 'Dominio 8 — Participación activa (peso 7%)',
  dom_emprendimiento    DOUBLE        NULL COMMENT 'Dominio 9 — Proyectos y economía (peso 5%)',
  dom_legado            DOUBLE        NULL COMMENT 'Dominio 10 — Valores e identidad (peso 5%)',
  dom_madurez           DOUBLE        NULL COMMENT 'Dominio 11 — Nivel evolutivo calculado (peso 5%)',

  -- Metadatos
  trigger_type          VARCHAR(50)   NOT NULL COMMENT 'ASSESSMENT | SPRINT_CLOSE | CRITICAL_EVENT | SCHEDULED',
  algorithm_version     VARCHAR(30)   NOT NULL DEFAULT 'ICAF_V1',
  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_fcs_family            (family_id),
  KEY idx_fcs_family_created    (family_id, created_at),
  KEY idx_fcs_madurez           (madurez_nivel),
  CONSTRAINT fk_fcs_family FOREIGN KEY (family_id)
    REFERENCES families (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── 3. family_critical_events ───────────────────────────────────────────────
-- Ciclo de vida completo de un evento crítico familiar.
-- Permite medir: tiempo hasta resolución, recaídas, velocidad de detección.
-- category: ALCOHOL | VIOLENCIA | COMUNICACION_ROTA | ADOLESCENTE_AISLADO |
--           PADRE_PERMISIVO | RUPTURA_RIESGO | ANSIEDAD | DUELO | OTRO

CREATE TABLE IF NOT EXISTS family_critical_events (
  id                    BIGINT        NOT NULL AUTO_INCREMENT,
  family_id             BIGINT        NOT NULL,

  category              VARCHAR(60)   NOT NULL COMMENT 'Tipo de evento crítico',
  status                VARCHAR(30)   NOT NULL DEFAULT 'DETECTED'
                          COMMENT 'DETECTED | IN_PROGRESS | RESOLVED | RELAPSED | CLOSED',
  severity              VARCHAR(20)   NOT NULL DEFAULT 'MODERATE'
                          COMMENT 'LOW | MODERATE | HIGH | CRITICAL',

  -- Trayectoria temporal
  detected_at           DATE          NOT NULL,
  intervention_start_at DATE          NULL COMMENT 'Inicio de ruta de intervención',
  resolved_at           DATE          NULL,
  closed_at             DATE          NULL,

  -- Indicadores calculados al resolver
  days_to_resolution    INT           NULL COMMENT 'detected_at → resolved_at',
  relapse_count         INT           NOT NULL DEFAULT 0,
  last_relapse_at       DATE          NULL,

  -- Contexto
  notes                 TEXT          NULL,
  resolution_summary    TEXT          NULL COMMENT 'Síntesis de resolución (generada por IA)',
  icaf_at_detection     DOUBLE        NULL COMMENT 'ICaF en el momento de detección',
  icaf_at_resolution    DOUBLE        NULL COMMENT 'ICaF al resolver',

  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_fce_family            (family_id),
  KEY idx_fce_status            (status),
  KEY idx_fce_category          (category),
  KEY idx_fce_family_status     (family_id, status),
  KEY idx_fce_detected          (detected_at),
  CONSTRAINT fk_fce_family FOREIGN KEY (family_id)
    REFERENCES families (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── 4. observatory_snapshots ────────────────────────────────────────────────
-- Agregados mensuales anonimizados para el Observatorio del Desarrollo Familiar.
-- Generados por job @Scheduled el primer día de cada mes.
-- No contienen family_id: son estadísticas poblacionales puras.

CREATE TABLE IF NOT EXISTS observatory_snapshots (
  id                    BIGINT        NOT NULL AUTO_INCREMENT,
  snapshot_month        DATE          NOT NULL COMMENT 'Primer día del mes (ej. 2026-07-01)',

  -- Distribución ICaF
  icaf_avg              DOUBLE        NULL COMMENT 'Promedio ICaF del mes',
  icaf_p25              DOUBLE        NULL COMMENT 'Percentil 25',
  icaf_median           DOUBLE        NULL COMMENT 'Percentil 50 (mediana)',
  icaf_p75              DOUBLE        NULL COMMENT 'Percentil 75',
  families_count        INT           NULL COMMENT 'N familias activas ese mes',

  -- Distribución por nivel de madurez (% sobre families_count)
  nivel_1_pct           DOUBLE        NULL COMMENT '% familias Nivel 1 Supervivencia',
  nivel_2_pct           DOUBLE        NULL COMMENT '% familias Nivel 2 Reactividad',
  nivel_3_pct           DOUBLE        NULL COMMENT '% familias Nivel 3 Organización',
  nivel_4_pct           DOUBLE        NULL COMMENT '% familias Nivel 4 Propósito',
  nivel_5_pct           DOUBLE        NULL COMMENT '% familias Nivel 5 Legado',

  -- Eventos críticos del mes
  events_detected       INT           NULL COMMENT 'Nuevos eventos críticos detectados',
  events_resolved       INT           NULL COMMENT 'Eventos críticos resueltos',
  avg_days_resolution   DOUBLE        NULL COMMENT 'Días promedio hasta resolución',
  resolution_rate_pct   DOUBLE        NULL COMMENT 'events_resolved / events_detected × 100',

  -- Tendencia de capital
  families_improving    INT           NULL COMMENT 'Familias con icaf_trend = IMPROVING',
  families_declining    INT           NULL COMMENT 'Familias con icaf_trend = DECLINING',
  families_stable       INT           NULL COMMENT 'Familias con icaf_trend = STABLE',

  -- Promedios por dominio (para análisis comparativo)
  avg_dom_cohesion      DOUBLE        NULL,
  avg_dom_confianza     DOUBLE        NULL,
  avg_dom_resiliencia   DOUBLE        NULL,
  avg_dom_comunicacion  DOUBLE        NULL,
  avg_dom_autonomia     DOUBLE        NULL,
  avg_dom_bienestar     DOUBLE        NULL,
  avg_dom_proposito     DOUBLE        NULL,
  avg_dom_integracion   DOUBLE        NULL,
  avg_dom_emprendimiento DOUBLE       NULL,
  avg_dom_legado        DOUBLE        NULL,

  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_obs_month (snapshot_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
