-- V9: Schema for Family Gratitude (Gratitud Familiar)
CREATE TABLE family_gratitude_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,

    from_member VARCHAR(150) NOT NULL,
    to_member VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_gratitude_family
        FOREIGN KEY (family_id)
        REFERENCES families(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_gratitude_family_id ON family_gratitude_entries(family_id);
