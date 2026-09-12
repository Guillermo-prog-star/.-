-- V88__poc_v1_2_parallel.sql

-- 1. Alterar la estructura de la tabla questions para separar la situación (text) del prompt clínico (phase_prompt)
ALTER TABLE questions ADD COLUMN phase_prompt VARCHAR(500);

-- 2. Alterar la estructura de la tabla question_options para añadir la separación metodológica de rubric_level
ALTER TABLE question_options ADD COLUMN rubric_level INT;

-- 3. Inserción del Escenario de Prueba de Concepto (5 Fases) - V1.2 Phenomenological
-- Escenario Base (text): "Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono."
-- parent_key: M-POC-S1
-- NOTA: NO HACEMOS UPDATE/DELETE de los escenarios V1.1. Inyectamos este para evaluarlo explícitamente en UI.

INSERT IGNORE INTO questions (question_key, parent_key, dimension, phase, type, text, phase_prompt, milestone_code, severity_weight, active) 
VALUES
('M-POC-S1-Q1', 'M-POC-S1', 'comunicacion', 'NOTICE', 'NEURO_AWARENESS', 'Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono.', '¿Qué fue lo primero que notaste en tu cuerpo?', 'M24', 1.0, true),
('M-POC-S1-Q2', 'M-POC-S1', 'comunicacion', 'THINK', 'NEURO_AWARENESS', 'Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono.', '¿Qué cruzó por tu mente?', 'M24', 1.0, true),
('M-POC-S1-Q3', 'M-POC-S1', 'comunicacion', 'ACT', 'NEURO_AWARENESS', 'Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono.', '¿Qué hiciste apenas lo/la viste?', 'M24', 1.0, true),
('M-POC-S1-Q4', 'M-POC-S1', 'comunicacion', 'AFTERMATH', 'NEURO_AWARENESS', 'Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono.', '¿Qué ocurrió en los siguientes minutos?', 'M24', 1.0, true),
('M-POC-S1-Q5', 'M-POC-S1', 'comunicacion', 'EFFECT', 'NEURO_AWARENESS', 'Tu hijo/a llega dos horas más tarde de lo acordado y no responde el teléfono.', '¿Cómo sentiste la relación al día siguiente?', 'M24', 1.0, true);

-- 4. Insertar opciones para Q1 (Notice) asignando score_value Y rubric_level
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Un aumento súbito de temperatura, tensión en la mandíbula y aceleración.', 1, 1 FROM questions WHERE question_key = 'M-POC-S1-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Una sensación de peso, rechazo y molestia física inmediata.', 2, 2 FROM questions WHERE question_key = 'M-POC-S1-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El estómago apretado y los hombros tensos de anticipación.', 3, 3 FROM questions WHERE question_key = 'M-POC-S1-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté la respiración agitada, y tomé aire un par de veces.', 4, 4 FROM questions WHERE question_key = 'M-POC-S1-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí alerta en mi cuerpo, pero mi respiración se mantuvo estable.', 5, 5 FROM questions WHERE question_key = 'M-POC-S1-Q1';

-- 5. Insertar opciones para Q2 (Think)
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Lo está haciendo a propósito para medirme."', 1, 1 FROM questions WHERE question_key = 'M-POC-S1-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Seguro se quedó haciendo algo que le importaba más."', 2, 2 FROM questions WHERE question_key = 'M-POC-S1-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"No sé si estar preocupado/a o enojado/a en este momento."', 3, 3 FROM questions WHERE question_key = 'M-POC-S1-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Esto rompe el acuerdo, tendré que preguntar qué pasó."', 4, 4 FROM questions WHERE question_key = 'M-POC-S1-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Me enfocaré en verificar que esté bien, luego revisamos la hora."', 5, 5 FROM questions WHERE question_key = 'M-POC-S1-Q2';

-- 6. Insertar opciones para Q3 (Act)
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Elevé la voz de inmediato y dije lo que pensaba.', 1, 1 FROM questions WHERE question_key = 'M-POC-S1-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije con firmeza que estaba castigado/a.', 2, 2 FROM questions WHERE question_key = 'M-POC-S1-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le pregunté dónde estaba con un tono seco y rápido.', 3, 3 FROM questions WHERE question_key = 'M-POC-S1-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me quedé en silencio un momento y le pregunté qué había pasado.', 4, 4 FROM questions WHERE question_key = 'M-POC-S1-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuché su explicación completa antes de mencionar el horario.', 5, 5 FROM questions WHERE question_key = 'M-POC-S1-Q3';

-- 7. Insertar opciones para Q4 (Aftermath)
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Nos separamos físicamente; uno de los dos salió de la habitación.', 1, 1 FROM questions WHERE question_key = 'M-POC-S1-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dejamos de hablarnos el resto del día.', 2, 2 FROM questions WHERE question_key = 'M-POC-S1-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conversamos rápido, pero quedó una sensación de incomodidad en el aire.', 3, 3 FROM questions WHERE question_key = 'M-POC-S1-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuchamos ambas partes; la interacción fue tensa pero se sostuvo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S1-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Establecimos claridad sobre qué haremos distinto la próxima vez.', 5, 5 FROM questions WHERE question_key = 'M-POC-S1-Q4';

-- 8. Insertar opciones para Q5 (Effect)
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Menos disposición de ambas partes para compartir cosas.', 1, 1 FROM questions WHERE question_key = 'M-POC-S1-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Distancia evidente; no se tocó el tema del horario.', 2, 2 FROM questions WHERE question_key = 'M-POC-S1-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Algo de cautela mutua, pero interactuando de nuevo.', 3, 3 FROM questions WHERE question_key = 'M-POC-S1-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sensación de haber superado un momento incómodo juntos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S1-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Claridad de que el límite existe y nos cuidamos mutuamente.', 5, 5 FROM questions WHERE question_key = 'M-POC-S1-Q5';
