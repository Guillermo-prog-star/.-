-- V40: Árbol Generacional — vinculación multigeneracional de familias
--
-- family_tree_links: El grafo dirigido que conecta familias.
--   parent_family_id → child_family_id
--   Relación: "Esta familia desciende de aquella"
--
-- generational_messages: Mensajes/cartas de una generación a otra,
--   independientes de family_legacies. Pueden tener fecha de apertura.

CREATE TABLE IF NOT EXISTS family_tree_links (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_family_id  BIGINT NOT NULL,
    child_family_id   BIGINT NOT NULL UNIQUE, -- cada familia tiene 1 sola familia origen
    relationship      VARCHAR(80) NOT NULL DEFAULT 'descendant',
    linked_by_member  VARCHAR(150),           -- nombre de quien creó el vínculo
    linked_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note              TEXT,                   -- descripción opcional del vínculo
    CONSTRAINT fk_tree_parent FOREIGN KEY (parent_family_id) REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT fk_tree_child  FOREIGN KEY (child_family_id)  REFERENCES families(id) ON DELETE CASCADE,
    CONSTRAINT uq_tree_link   UNIQUE (parent_family_id, child_family_id)
);

CREATE TABLE IF NOT EXISTS generational_messages (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_family_id    BIGINT NOT NULL,
    to_family_id      BIGINT,                -- null = para todas las generaciones futuras
    author_name       VARCHAR(150) NOT NULL,
    subject           VARCHAR(200),
    content           TEXT NOT NULL,
    message_type      VARCHAR(40) NOT NULL DEFAULT 'LETTER', -- LETTER | WISDOM | WARNING | BLESSING
    sealed            BOOLEAN NOT NULL DEFAULT TRUE,
    open_in_year      INT,                   -- null = abierto inmediatamente
    opened_at         DATETIME,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gm_from FOREIGN KEY (from_family_id) REFERENCES families(id) ON DELETE CASCADE
);
