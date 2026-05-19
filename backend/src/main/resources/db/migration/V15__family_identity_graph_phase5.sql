-- =====================================================================
-- V15 — Family Identity Graph (SDD Fase 5)
-- Grafo de dinámicas relacionales entre miembros de la familia
-- =====================================================================

CREATE TABLE member_relation_edges (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    family_id            BIGINT       NOT NULL,
    member_a_id          BIGINT       NOT NULL,
    member_b_id          BIGINT       NOT NULL,
    relationship_type    VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    dynamic_type         VARCHAR(20)  NOT NULL DEFAULT 'BALANCED',
    cohesion_score       DOUBLE       NOT NULL DEFAULT 50.0,
    tension_score        DOUBLE       NOT NULL DEFAULT 30.0,
    communication_score  DOUBLE       NOT NULL DEFAULT 50.0,
    evolution_trend      VARCHAR(15)  NOT NULL DEFAULT 'STABLE',
    role_a               VARCHAR(20)  NOT NULL DEFAULT 'NEUTRAL',
    role_b               VARCHAR(20)  NOT NULL DEFAULT 'NEUTRAL',
    from_evaluation_id   BIGINT,
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_edge_pair (family_id, member_a_id, member_b_id),
    INDEX idx_edge_family   (family_id),
    INDEX idx_edge_member_a (member_a_id),
    INDEX idx_edge_member_b (member_b_id),
    CONSTRAINT fk_edge_family   FOREIGN KEY (family_id)   REFERENCES families (id),
    CONSTRAINT fk_edge_member_a FOREIGN KEY (member_a_id) REFERENCES family_members (id),
    CONSTRAINT fk_edge_member_b FOREIGN KEY (member_b_id) REFERENCES family_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
