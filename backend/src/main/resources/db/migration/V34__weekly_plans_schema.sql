-- V34: Planeación semanal familiar (4 fases: PREPARE, EXECUTE, EVALUATE, CONSOLIDATE)
-- Dialecto: MySQL 8.4

CREATE TABLE IF NOT EXISTS weekly_plans (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT       NOT NULL,
    sprint_number   INT          NOT NULL DEFAULT 1,
    phase           VARCHAR(20)  NOT NULL,
    daily_answers   JSON,
    week_start_date DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_weekly_plan (family_id, sprint_number, phase),
    CONSTRAINT fk_weekly_plan_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS weekly_tasks (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    weekly_plan_id  BIGINT       NOT NULL,
    description     TEXT         NOT NULL,
    responsible     VARCHAR(100),
    `when`          VARCHAR(100),
    indicator       VARCHAR(200),
    done            TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order      INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_weekly_task_plan FOREIGN KEY (weekly_plan_id) REFERENCES weekly_plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_weekly_plans_family ON weekly_plans(family_id);
