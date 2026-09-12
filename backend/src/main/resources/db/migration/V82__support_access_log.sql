CREATE TABLE IF NOT EXISTS support_access_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id  BIGINT       NOT NULL,
    family_id      BIGINT       NOT NULL,
    actor_email    VARCHAR(255) NOT NULL,
    action         VARCHAR(100) NOT NULL,
    detail         VARCHAR(500) NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_sal_assignment (assignment_id),
    INDEX idx_sal_family     (family_id),
    CONSTRAINT fk_sal_assignment FOREIGN KEY (assignment_id)
        REFERENCES family_support_assignments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
