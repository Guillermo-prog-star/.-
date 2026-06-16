-- V66__family_documentaries_schema.sql
-- Crea la tabla nativa para los Mini Documentales Familiares (diferenciando de la evidencia cruda)

CREATE TABLE IF NOT EXISTS family_documentaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    sprint_id BIGINT NULL,
    pillar_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    source_type VARCHAR(50) NOT NULL, -- MISSION, SPONTANEOUS, MEMORY, SPRINT_CLOSURE, PILLAR_CLOSURE
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documentary_family FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_documentary_task FOREIGN KEY (task_id) REFERENCES plan_tasks(id) ON DELETE SET NULL,
    CONSTRAINT fk_documentary_sprint FOREIGN KEY (sprint_id) REFERENCES family_sprints(id) ON DELETE SET NULL
);
