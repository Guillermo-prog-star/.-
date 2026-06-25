-- Centro de Documentación — repositorio de documentos del proyecto
CREATE TABLE IF NOT EXISTS project_documents (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)   NOT NULL UNIQUE,
    title            VARCHAR(255)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    content          LONGTEXT      NOT NULL,
    summary          TEXT          NULL,
    version          VARCHAR(20)   NOT NULL DEFAULT '1.0',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    tags             VARCHAR(500)  NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_project_documents_category ON project_documents(category);
CREATE INDEX idx_project_documents_status   ON project_documents(status);
