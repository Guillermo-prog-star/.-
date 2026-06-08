DROP TABLE IF EXISTS adaptive_adjustments;

CREATE TABLE adaptive_adjustments
(
    id            BINARY(16)   NOT NULL,
    family_id     BIGINT       NOT NULL,
    rule_type     VARCHAR(50)  NOT NULL,
    reason        TEXT         NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    approved_at   DATETIME     NULL,
    applied_at    DATETIME     NULL,
    approved_by   VARCHAR(120) NULL,

    CONSTRAINT pk_adaptive_adjustments
        PRIMARY KEY (id)
);

CREATE INDEX idx_adaptive_adjustments_family
    ON adaptive_adjustments (family_id);

CREATE INDEX idx_adaptive_adjustments_status
    ON adaptive_adjustments (status);

CREATE INDEX idx_adaptive_adjustments_family_status
    ON adaptive_adjustments (family_id, status);