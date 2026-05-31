-- V34: Planeación semanal familiar (4 fases: PREPARE, EXECUTE, EVALUATE, CONSOLIDATE)

CREATE TABLE IF NOT EXISTS weekly_plans (
    id              BIGSERIAL PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    sprint_number   INT          NOT NULL DEFAULT 1,
    phase           VARCHAR(20)  NOT NULL,
    week_start_date DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (family_id, sprint_number, phase)
);

CREATE TABLE IF NOT EXISTS weekly_tasks (
    id              BIGSERIAL PRIMARY KEY,
    weekly_plan_id  BIGINT       NOT NULL REFERENCES weekly_plans(id) ON DELETE CASCADE,
    description     TEXT         NOT NULL,
    responsible     VARCHAR(100),
    "when"          VARCHAR(100),
    indicator       VARCHAR(200),
    done            BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_weekly_plans_family ON weekly_plans(family_id);
