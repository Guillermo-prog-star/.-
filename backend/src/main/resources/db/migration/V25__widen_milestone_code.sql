-- Flyway Migration: Widen milestone_code length to 50
-- Prevents data truncation for longer milestone codes such as 'MES_00_DIAGNOSTICO_BASE'

CREATE TABLE IF NOT EXISTS progress_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    current_evaluation_id BIGINT NOT NULL,
    previous_evaluation_id BIGINT,
    milestone_code VARCHAR(50),
    previous_icf DOUBLE,
    current_icf DOUBLE,
    delta_icf DOUBLE,
    classification VARCHAR(30),
    interpretation TEXT,
    recommended_action TEXT,
    created_at DATETIME NOT NULL
);

ALTER TABLE progress_snapshots MODIFY COLUMN milestone_code VARCHAR(50);
ALTER TABLE plan_tasks MODIFY COLUMN milestone_code VARCHAR(50);
ALTER TABLE questions MODIFY COLUMN milestone_code VARCHAR(50);
