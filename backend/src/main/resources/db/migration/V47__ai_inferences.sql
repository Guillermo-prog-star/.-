CREATE TABLE ai_inferences
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    family_id        BIGINT       NOT NULL,
    context_hash     VARCHAR(100) NULL,
    input_summary    TEXT         NULL,
    inference_result TEXT         NULL,
    priority         VARCHAR(50)  NULL,
    prompt_used      TEXT         NULL,
    model_version    VARCHAR(100) NULL,
    created_at       DATETIME     NULL,

    CONSTRAINT pk_ai_inferences
        PRIMARY KEY (id)
);

CREATE INDEX idx_ai_inferences_family
    ON ai_inferences (family_id);

CREATE INDEX idx_ai_inferences_created_at
    ON ai_inferences (created_at);

CREATE INDEX idx_ai_inferences_family_created_at
    ON ai_inferences (family_id, created_at);