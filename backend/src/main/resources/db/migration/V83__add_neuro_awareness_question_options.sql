CREATE TABLE question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    score_value INT NOT NULL,
    awareness_component VARCHAR(50),
    inferred_level VARCHAR(50),
    psychometric_weight DOUBLE,
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_question_options_question_id ON question_options(question_id);
