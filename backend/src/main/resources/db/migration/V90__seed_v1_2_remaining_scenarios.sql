-- V90__seed_v1_2_remaining_scenarios.sql
-- Inserción de los 19 escenarios restantes bajo la arquitectura dinámica V1.2.
-- Todos utilizan lenguaje fenomenológico y neutral.

-- ==========================================
-- Escenario 2 (emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S2-Q1', 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S2', 'emocion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S2-Q2', 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S2', 'emocion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S2-Q3', 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S2', 'emocion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S2-Q4', 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S2', 'emocion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S2-Q5', 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S2', 'emocion', 'ACT', '¿Qué hiciste frente a ese comentario?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Respondí de inmediato con otra crítica o me retiré cerrando la puerta.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me defendí verbalmente alzando la voz para justificarme.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Contesté de forma cortante para que notaran mi molestia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice una pausa silenciosa antes de responder para no escalar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pregunté serenamente qué querían decir exactamente para entender su perspectiva.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q5';

-- ==========================================
-- Escenario 3 (emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S3-Q1', 'Los planes familiares cambian inesperadamente por factores externos (clima, retrasos).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S3', 'emocion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S3-Q2', 'Los planes familiares cambian inesperadamente por factores externos (clima, retrasos).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S3', 'emocion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S3-Q3', 'Los planes familiares cambian inesperadamente por factores externos (clima, retrasos).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S3', 'emocion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S3-Q4', 'Los planes familiares cambian inesperadamente por factores externos (clima, retrasos).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S3', 'emocion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S3-Q5', 'Los planes familiares cambian inesperadamente por factores externos (clima, retrasos).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S3', 'emocion', 'ACT', '¿Qué hiciste frente al cambio?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mostré mi frustración visiblemente, quejándome el resto de la jornada.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Expresé mi desagrado repetidas veces antes de aceptar la nueva opción.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Permanecí en silencio, intentando adaptarme aunque mi lenguaje corporal era tenso.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Propuse buscar una alternativa, aunque aún me sentía desanimado/a.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reorienté al grupo hacia la nueva opción con una actitud práctica y cooperativa.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q5';

-- ==========================================
-- Escenario 4 (emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S4-Q1', 'Sientes que no estás recibiendo el reconocimiento que mereces por tus aportes familiares.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S4', 'emocion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S4-Q2', 'Sientes que no estás recibiendo el reconocimiento que mereces por tus aportes familiares.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S4', 'emocion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S4-Q3', 'Sientes que no estás recibiendo el reconocimiento que mereces por tus aportes familiares.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S4', 'emocion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S4-Q4', 'Sientes que no estás recibiendo el reconocimiento que mereces por tus aportes familiares.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S4', 'emocion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S4-Q5', 'Sientes que no estás recibiendo el reconocimiento que mereces por tus aportes familiares.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S4', 'emocion', 'ACT', '¿Cómo expresaste esta necesidad?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dejé de hacer cosas por la familia esperando que noten mi ausencia.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice comentarios pasivo-agresivos o reclamos indirectos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mencioné mi cansancio esperando que alguien me lo reconociera.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Expresé que me sentía poco valorado/a de forma directa pero tensa.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Comuniqué abiertamente mi necesidad de aprecio sin culpar a nadie.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q5';

-- ==========================================
-- Escenario 5 (emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S5-Q1', 'Experimentas una tristeza repentina o desánimo sin una causa evidente en casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S5', 'emocion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S5-Q2', 'Experimentas una tristeza repentina o desánimo sin una causa evidente en casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S5', 'emocion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S5-Q3', 'Experimentas una tristeza repentina o desánimo sin una causa evidente en casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S5', 'emocion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S5-Q4', 'Experimentas una tristeza repentina o desánimo sin una causa evidente en casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S5', 'emocion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S5-Q5', 'Experimentas una tristeza repentina o desánimo sin una causa evidente en casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S5', 'emocion', 'ACT', '¿Qué hiciste con esa emoción?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me irrité con los demás para desviar la atención de mi tristeza.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me distraje compulsivamente con pantallas o comida para adormecerla.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve un perfil bajo, intentando que nadie me preguntara nada.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pausé mis exigencias del día para descansar un momento a solas.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me permití sentir la emoción y la comuniqué tranquilamente si me preguntaron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q5';

-- ==========================================
-- Escenario 6 (emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S6-Q1', 'Un miembro de la familia expresa una emoción muy intensa (llanto o enojo fuerte).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S6', 'emocion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S6-Q2', 'Un miembro de la familia expresa una emoción muy intensa (llanto o enojo fuerte).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S6', 'emocion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S6-Q3', 'Un miembro de la familia expresa una emoción muy intensa (llanto o enojo fuerte).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S6', 'emocion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S6-Q4', 'Un miembro de la familia expresa una emoción muy intensa (llanto o enojo fuerte).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S6', 'emocion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S6-Q5', 'Un miembro de la familia expresa una emoción muy intensa (llanto o enojo fuerte).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S6', 'emocion', 'ACT', '¿Cuál fue tu intervención?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le exigí que se calmara de inmediato o salí del lugar por la incomodidad.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Intenté callar o resolver su problema rápidamente para frenar su emoción.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije que todo iba a estar bien con rapidez, buscando cerrar el tema.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me quedé a su lado en silencio, esperando a que pasara la intensidad.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Acompañé su emoción con presencia corporal serena, sin intentar apagarla.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q5';

-- ==========================================
-- Escenario 7 (comunicacion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S7-Q1', 'Necesitas pedir ayuda porque estás sobrepasado/a de tareas.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S7', 'comunicacion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S7-Q2', 'Necesitas pedir ayuda porque estás sobrepasado/a de tareas.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S7', 'comunicacion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S7-Q3', 'Necesitas pedir ayuda porque estás sobrepasado/a de tareas.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S7', 'comunicacion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S7-Q4', 'Necesitas pedir ayuda porque estás sobrepasado/a de tareas.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S7', 'comunicacion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S7-Q5', 'Necesitas pedir ayuda porque estás sobrepasado/a de tareas.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S7', 'comunicacion', 'ACT', '¿Cómo procediste a pedir apoyo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Exploté de cansancio y culpé a los demás por no darse cuenta.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pedí ayuda en un tono de reclamo o queja evidente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Insinué que necesitaba apoyo sin pedirlo directamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pedí ayuda directa, aunque me sentí incómodo/a al hacerlo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Solicité el apoyo con naturalidad, reconociendo mi límite de capacidad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q5';

-- ==========================================
-- Escenario 8 (comunicacion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S8-Q1', 'Hay una discusión acalorada y sientes que el otro no te está escuchando.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S8', 'comunicacion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S8-Q2', 'Hay una discusión acalorada y sientes que el otro no te está escuchando.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S8', 'comunicacion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S8-Q3', 'Hay una discusión acalorada y sientes que el otro no te está escuchando.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S8', 'comunicacion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S8-Q4', 'Hay una discusión acalorada y sientes que el otro no te está escuchando.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S8', 'comunicacion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S8-Q5', 'Hay una discusión acalorada y sientes que el otro no te está escuchando.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S8', 'comunicacion', 'ACT', '¿Qué acción tomaste en el pico de la tensión?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Elevé la voz e interrumpí para forzar a que me escucharan.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me rendí y me fui físicamente del lugar dejando la palabra en el aire.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Seguí repitiendo mi punto mecánicamente aunque nadie prestaba atención.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pedí un tiempo fuera de forma explícita para retomar más tarde.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Validé en voz alta el punto del otro para detener la escalada antes de dar el mío.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q5';

-- ==========================================
-- Escenario 9 (comunicacion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S9-Q1', 'Tienes que dar una retroalimentación difícil a tu pareja o hijos.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S9', 'comunicacion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S9-Q2', 'Tienes que dar una retroalimentación difícil a tu pareja o hijos.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S9', 'comunicacion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S9-Q3', 'Tienes que dar una retroalimentación difícil a tu pareja o hijos.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S9', 'comunicacion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S9-Q4', 'Tienes que dar una retroalimentación difícil a tu pareja o hijos.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S9', 'comunicacion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S9-Q5', 'Tienes que dar una retroalimentación difícil a tu pareja o hijos.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S9', 'comunicacion', 'ACT', '¿De qué manera entregaste el mensaje?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Solté la crítica de golpe en un momento de frustración.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Usé el sarcasmo o una broma para no confrontar directamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui muy suave y ambiguo/a, diluyendo el mensaje principal.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui directo/a pero preparé mis palabras y busqué el momento adecuado.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hablé desde mi experiencia ("yo siento"), enfocándome en el comportamiento.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q5';

-- ==========================================
-- Escenario 10 (comunicacion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S10-Q1', 'Tu familia te cuenta algo que le apasiona pero que a ti no te interesa tanto.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S10', 'comunicacion', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S10-Q2', 'Tu familia te cuenta algo que le apasiona pero que a ti no te interesa tanto.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S10', 'comunicacion', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S10-Q3', 'Tu familia te cuenta algo que le apasiona pero que a ti no te interesa tanto.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S10', 'comunicacion', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S10-Q4', 'Tu familia te cuenta algo que le apasiona pero que a ti no te interesa tanto.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S10', 'comunicacion', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S10-Q5', 'Tu familia te cuenta algo que le apasiona pero que a ti no te interesa tanto.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S10', 'comunicacion', 'ACT', '¿Cómo manejaste la atención en ese momento?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Lo ignoré visiblemente, mirando una pantalla o cambiando de tema.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fingí escuchar asintiendo, pero me ocupé de otra cosa a la vez.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuché un momento y luego corté la conversación educadamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice un esfuerzo consciente por re-enfocarme y guardar mis distracciones.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve contacto visual e hice preguntas por el valor que tiene para ellos.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q5';

-- ==========================================
-- Escenario 11 (habitos)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S11-Q1', 'Es tarde, estás cansado/a, pero aún hay desorden en la casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S11', 'habitos', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S11-Q2', 'Es tarde, estás cansado/a, pero aún hay desorden en la casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S11', 'habitos', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S11-Q3', 'Es tarde, estás cansado/a, pero aún hay desorden en la casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S11', 'habitos', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S11-Q4', 'Es tarde, estás cansado/a, pero aún hay desorden en la casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S11', 'habitos', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S11-Q5', 'Es tarde, estás cansado/a, pero aún hay desorden en la casa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S11', 'habitos', 'ACT', '¿Qué hiciste frente al desorden?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Limpié haciendo ruido intenso para que los demás notaran mi molestia.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dejé las cosas tiradas pero hice reclamos en voz alta a todos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Empecé a ordenar de forma mecánica, rumiando mi agotamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Decidí dejarlo para el día siguiente e irme a descansar conscientemente.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dejé el desorden sin culpa, priorizando la necesidad fisiológica de sueño.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q5';

-- ==========================================
-- Escenario 12 (habitos)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S12-Q1', 'Decides establecer un nuevo límite familiar y hay resistencia inicial.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S12', 'habitos', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S12-Q2', 'Decides establecer un nuevo límite familiar y hay resistencia inicial.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S12', 'habitos', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S12-Q3', 'Decides establecer un nuevo límite familiar y hay resistencia inicial.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S12', 'habitos', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S12-Q4', 'Decides establecer un nuevo límite familiar y hay resistencia inicial.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S12', 'habitos', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S12-Q5', 'Decides establecer un nuevo límite familiar y hay resistencia inicial.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S12', 'habitos', 'ACT', '¿Cómo gestionaste la resistencia?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me rendí al primer quejido porque no soporté el conflicto.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Impuse la regla con amenazas o subiendo el tono de voz.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sostuve el límite pero me mostré muy a la defensiva y tenso/a.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el límite firme repitiendo la instrucción de forma neutra.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sostuve el límite validando a la vez la frustración de mi familia.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q5';

-- ==========================================
-- Escenario 13 (habitos)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S13-Q1', 'Tienes un tiempo a solas planeado pero surgen demandas menores de la familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S13', 'habitos', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S13-Q2', 'Tienes un tiempo a solas planeado pero surgen demandas menores de la familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S13', 'habitos', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S13-Q3', 'Tienes un tiempo a solas planeado pero surgen demandas menores de la familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S13', 'habitos', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S13-Q4', 'Tienes un tiempo a solas planeado pero surgen demandas menores de la familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S13', 'habitos', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S13-Q5', 'Tienes un tiempo a solas planeado pero surgen demandas menores de la familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S13', 'habitos', 'ACT', '¿Qué hiciste con tu tiempo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Abandoné mi espacio personal inmediatamente, frustrado/a.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Atendí la demanda con evidente resentimiento y malos modos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Atendí la demanda rápido intentando salvar un poco de mi tiempo.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Comuniqué que estaba ocupado/a y pospuse la demanda para más tarde.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el espacio protectoramente con claridad y afecto.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q5';

-- ==========================================
-- Escenario 14 (habitos)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S14-Q1', 'Al momento de compartir una comida familiar en la mesa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S14', 'habitos', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S14-Q2', 'Al momento de compartir una comida familiar en la mesa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S14', 'habitos', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S14-Q3', 'Al momento de compartir una comida familiar en la mesa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S14', 'habitos', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S14-Q4', 'Al momento de compartir una comida familiar en la mesa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S14', 'habitos', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S14-Q5', 'Al momento de compartir una comida familiar en la mesa.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S14', 'habitos', 'ACT', '¿Cómo fue tu presencia física y mental?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Comí mirando una pantalla o atendiendo asuntos externos por completo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me levanté repetidas veces o mantuve distracciones cerca.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Estuve físicamente pero con poca interacción o aporte a la charla.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aparté las distracciones explícitamente para enfocarme en el momento.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me conecté con los sabores y la interacción, sosteniendo la presencia plena.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q5';

-- ==========================================
-- Escenario 15 (habitos)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S15-Q1', 'Un hábito familiar (como la rutina matutina o de dormir) se vuelve caótico.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S15', 'habitos', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S15-Q2', 'Un hábito familiar (como la rutina matutina o de dormir) se vuelve caótico.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S15', 'habitos', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S15-Q3', 'Un hábito familiar (como la rutina matutina o de dormir) se vuelve caótico.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S15', 'habitos', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S15-Q4', 'Un hábito familiar (como la rutina matutina o de dormir) se vuelve caótico.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S15', 'habitos', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S15-Q5', 'Un hábito familiar (como la rutina matutina o de dormir) se vuelve caótico.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S15', 'habitos', 'ACT', '¿Qué hiciste para manejar el momento?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Respondí con caos: grité y apresuré a todos frenéticamente.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Presioné constantemente con tensión verbal a cada paso.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Intenté forzar mi ritmo de forma mecánica y rígida.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Solté la perfección de la rutina y busqué solo cumplir lo esencial.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ajusté el ritmo al de la familia, guiando con fluidez sin forzar.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q5';

-- ==========================================
-- Escenario 16 (tiempo)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S16-Q1', 'Juegas o compartes tiempo de ocio no estructurado con tu familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S16', 'tiempo', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S16-Q2', 'Juegas o compartes tiempo de ocio no estructurado con tu familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S16', 'tiempo', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S16-Q3', 'Juegas o compartes tiempo de ocio no estructurado con tu familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S16', 'tiempo', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S16-Q4', 'Juegas o compartes tiempo de ocio no estructurado con tu familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S16', 'tiempo', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S16-Q5', 'Juegas o compartes tiempo de ocio no estructurado con tu familia.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S16', 'tiempo', 'ACT', '¿Cómo manejaste tu atención en el ocio?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dirigí la actividad para terminar rápido porque me aburría.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Jugué mecánicamente mientras resolvía otras cosas mentalmente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Estuve presente pero sintiendo urgencia por volver a ser productivo/a.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Cada vez que mi mente se iba, la traje de vuelta al juego.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me sumergí totalmente en la actividad perdiendo la noción del deber.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q5';

-- ==========================================
-- Escenario 17 (tiempo)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S17-Q1', 'Llegas a casa o cambias de rol después de un día exhaustivo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S17', 'tiempo', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S17-Q2', 'Llegas a casa o cambias de rol después de un día exhaustivo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S17', 'tiempo', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S17-Q3', 'Llegas a casa o cambias de rol después de un día exhaustivo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S17', 'tiempo', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S17-Q4', 'Llegas a casa o cambias de rol después de un día exhaustivo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S17', 'tiempo', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S17-Q5', 'Llegas a casa o cambias de rol después de un día exhaustivo.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S17', 'tiempo', 'ACT', '¿Cómo hiciste la transición al espacio familiar?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Descargué mi estrés y mal humor con la primera interacción.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me aislé completamente ignorando saludos para poder relajarme.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Entré físicamente pero seguía enviando mensajes y mentalmente fuera.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice una pausa en la puerta para respirar profundo antes de saludar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice un corte consciente y saludé con conexión plena y genuina.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q5';

-- ==========================================
-- Escenario 18 (tiempo)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S18-Q1', 'Tienes una conversación importante sobre decisiones o el futuro familiar.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S18', 'tiempo', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S18-Q2', 'Tienes una conversación importante sobre decisiones o el futuro familiar.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S18', 'tiempo', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S18-Q3', 'Tienes una conversación importante sobre decisiones o el futuro familiar.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S18', 'tiempo', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S18-Q4', 'Tienes una conversación importante sobre decisiones o el futuro familiar.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S18', 'tiempo', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S18-Q5', 'Tienes una conversación importante sobre decisiones o el futuro familiar.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S18', 'tiempo', 'ACT', '¿Cómo interactuaste con la incertidumbre del tema?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evité el tema completamente cambiando de conversación o huyendo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hablé con excesiva prisa, imponiendo soluciones para no sentir duda.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Participé con visible incomodidad, queriendo terminar pronto.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me esforcé por escuchar las opciones sin apresurar una conclusión.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me mantuve anclado/a en el presente explorando el tema con apertura.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q5';

-- ==========================================
-- Escenario 19 (tiempo)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S19-Q1', 'Durante las transiciones rápidas del día (ej. salir hacia la escuela/trabajo).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S19', 'tiempo', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S19-Q2', 'Durante las transiciones rápidas del día (ej. salir hacia la escuela/trabajo).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S19', 'tiempo', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S19-Q3', 'Durante las transiciones rápidas del día (ej. salir hacia la escuela/trabajo).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S19', 'tiempo', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S19-Q4', 'Durante las transiciones rápidas del día (ej. salir hacia la escuela/trabajo).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S19', 'tiempo', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S19-Q5', 'Durante las transiciones rápidas del día (ej. salir hacia la escuela/trabajo).', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S19', 'tiempo', 'ACT', '¿Cómo lideraste el flujo de la salida?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Generé pánico y un clima de alerta constante y reproches.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Transmití alta tensión a través de movimientos bruscos y prisa silenciosa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Apresuré a todos verbalmente de forma continua.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Al notar mi urgencia, bajé mi velocidad física a propósito.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Coordiné la salida con calma operativa, aceptando el ritmo real.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q5';

-- ==========================================
-- Escenario 20 (tiempo)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S20-Q1', 'Alguien te pide que observes algo que hicieron y estás en medio de una tarea.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S20', 'tiempo', 'THINK', '¿Qué pensaste en ese instante inicial?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensamientos automáticos de culpa, ataque o victimización sin filtro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Asumí intenciones negativas o consecuencias catastróficas rápidamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí que estaba especulando, pero me costaba soltar el pensamiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Recordé que hay múltiples perspectivas y elegí observar sin concluir.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Mantuve el enfoque en el hecho objetivo y concreto de la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S20-Q2', 'Alguien te pide que observes algo que hicieron y estás en medio de una tarea.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S20', 'tiempo', 'NOTICE', '¿Qué fue lo primero que notaste a nivel corporal o de impulso?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor o tensión repentina y abrumadora.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté agitación en mi respiración y rigidez en los músculos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me percaté de un impulso físico fuerte, reconociendo la incomodidad.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Observé mi propia tensión, logrando anclarme físicamente antes de actuar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui consciente de la incomodidad somática sin dejarme arrastrar por ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S20-Q3', 'Alguien te pide que observes algo que hicieron y estás en medio de una tarea.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S20', 'tiempo', 'EFFECT', '¿Qué efecto tuvo este episodio en la dinámica a largo plazo?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Degradación de la confianza y mayor distancia emocional estructural.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Evitación mutua del tema; se creó una barrera preventiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fricción residual, pero con certeza de que el vínculo lo soporta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de crecimiento incipiente tras superar la incomodidad juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Integración sistémica fortalecida; el límite y el cuidado coexistieron.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S20-Q4', 'Alguien te pide que observes algo que hicieron y estás en medio de una tarea.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S20', 'tiempo', 'AFTERMATH', '¿Qué ocurrió en el ambiente inmediatamente después?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ruptura de la conexión, con aislamiento físico o cierre verbal inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tensión evidente, silencios prolongados y distancia defensiva.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción rápida para salir del paso, dejando incomodidad latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interacción sostenida pese a la tensión, logrando escuchar al otro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conexión recuperada con claridad sobre el límite o la situación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt)
VALUES ('M-POC-S20-Q5', 'Alguien te pide que observes algo que hicieron y estás en medio de una tarea.', 'NEURO_AWARENESS', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S20', 'tiempo', 'ACT', '¿Cómo respondiste al llamado?');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Di una respuesta genérica ("qué bonito") sin mirarlos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Di un vistazo rápido de un segundo y regresé inmediatamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice un gesto de molestia pero lo miré brevemente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pausé mi tarea, los miré de frente y les dediqué un momento real.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Celebré su logro brindándoles atención plena con los ojos y postura.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q5';

