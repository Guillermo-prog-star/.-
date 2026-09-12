ALTER TABLE questions
ADD COLUMN parent_key VARCHAR(100);

ALTER TABLE question_options
ADD COLUMN vector_tag VARCHAR(50);

ALTER TABLE evaluations
ADD COLUMN somatic_awareness DOUBLE,
ADD COLUMN emotional_awareness DOUBLE,
ADD COLUMN cognitive_awareness DOUBLE,
ADD COLUMN impulsive_awareness DOUBLE,
ADD COLUMN pause_capacity DOUBLE,
ADD COLUMN integration_score DOUBLE;

