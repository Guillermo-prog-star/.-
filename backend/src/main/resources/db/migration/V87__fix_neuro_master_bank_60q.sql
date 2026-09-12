-- V87__fix_neuro_master_bank_60q.sql
-- Inactivación de V85 y V86
UPDATE questions SET active = false WHERE question_key LIKE 'Q-EMO-001%' OR question_key LIKE 'Q-COM-001%';
UPDATE questions SET active = false WHERE question_key LIKE 'N-EMO-%' OR question_key LIKE 'N-COM-%' OR question_key LIKE 'N-HAB-%' OR question_key LIKE 'N-TIM-%';

-- Inserción de 20 Escenarios (60 preguntas)

INSERT IGNORE INTO questions 
(question_key, parent_key, dimension, phase, type, text, milestone_code, severity_weight, active) 
VALUES
('N-EMO-S1-Q1', 'N-EMO-S1', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Cuando sientes una emoción intensa (ej. enojo repentino) (Momento de entrada)', 'M24', 1.0, true),
('N-EMO-S1-Q2', 'N-EMO-S1', 'emociones', 'TIMING', 'NEURO_AWARENESS', 'Cuando sientes una emoción intensa (ej. enojo repentino) (Tiempo de reacción)', 'M24', 1.0, true),
('N-EMO-S1-Q3', 'N-EMO-S1', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Cuando sientes una emoción intensa (ej. enojo repentino) (Acción resultante)', 'M24', 1.0, true),
('N-EMO-S2-Q1', 'N-EMO-S2', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Ante una rabieta de tu hijo/a (Momento de entrada)', 'M24', 1.0, true),
('N-EMO-S2-Q2', 'N-EMO-S2', 'emociones', 'TIMING', 'NEURO_AWARENESS', 'Ante una rabieta de tu hijo/a (Tiempo de reacción)', 'M24', 1.0, true),
('N-EMO-S2-Q3', 'N-EMO-S2', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Ante una rabieta de tu hijo/a (Acción resultante)', 'M24', 1.0, true),
('N-EMO-S3-Q1', 'N-EMO-S3', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Si las cosas no salen como las planeaste (Momento de entrada)', 'M24', 1.0, true),
('N-EMO-S3-Q2', 'N-EMO-S3', 'emociones', 'TIMING', 'NEURO_AWARENESS', 'Si las cosas no salen como las planeaste (Tiempo de reacción)', 'M24', 1.0, true),
('N-EMO-S3-Q3', 'N-EMO-S3', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Si las cosas no salen como las planeaste (Acción resultante)', 'M24', 1.0, true),
('N-EMO-S4-Q1', 'N-EMO-S4', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Cuando estás exhausto/a y te piden atención (Momento de entrada)', 'M24', 1.0, true),
('N-EMO-S4-Q2', 'N-EMO-S4', 'emociones', 'TIMING', 'NEURO_AWARENESS', 'Cuando estás exhausto/a y te piden atención (Tiempo de reacción)', 'M24', 1.0, true),
('N-EMO-S4-Q3', 'N-EMO-S4', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Cuando estás exhausto/a y te piden atención (Acción resultante)', 'M24', 1.0, true),
('N-EMO-S5-Q1', 'N-EMO-S5', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Al recibir una mala noticia sobre tu hijo/a (Momento de entrada)', 'M24', 1.0, true),
('N-EMO-S5-Q2', 'N-EMO-S5', 'emociones', 'TIMING', 'NEURO_AWARENESS', 'Al recibir una mala noticia sobre tu hijo/a (Tiempo de reacción)', 'M24', 1.0, true),
('N-EMO-S5-Q3', 'N-EMO-S5', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Al recibir una mala noticia sobre tu hijo/a (Acción resultante)', 'M24', 1.0, true),
('N-COM-S1-Q1', 'N-COM-S1', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'Cuando tu hijo/a te contradice delante de otros (Momento de entrada)', 'M24', 1.0, true),
('N-COM-S1-Q2', 'N-COM-S1', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', 'Cuando tu hijo/a te contradice delante de otros (Tiempo de reacción)', 'M24', 1.0, true),
('N-COM-S1-Q3', 'N-COM-S1', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Cuando tu hijo/a te contradice delante de otros (Acción resultante)', 'M24', 1.0, true),
('N-COM-S2-Q1', 'N-COM-S2', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'En medio de una discusión acalorada con tu pareja (Momento de entrada)', 'M24', 1.0, true),
('N-COM-S2-Q2', 'N-COM-S2', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', 'En medio de una discusión acalorada con tu pareja (Tiempo de reacción)', 'M24', 1.0, true),
('N-COM-S2-Q3', 'N-COM-S2', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'En medio de una discusión acalorada con tu pareja (Acción resultante)', 'M24', 1.0, true),
('N-COM-S3-Q1', 'N-COM-S3', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'Si sientes que no te están escuchando (Momento de entrada)', 'M24', 1.0, true),
('N-COM-S3-Q2', 'N-COM-S3', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', 'Si sientes que no te están escuchando (Tiempo de reacción)', 'M24', 1.0, true),
('N-COM-S3-Q3', 'N-COM-S3', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Si sientes que no te están escuchando (Acción resultante)', 'M24', 1.0, true),
('N-COM-S4-Q1', 'N-COM-S4', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'Cuando alguien malinterpreta tus intenciones (Momento de entrada)', 'M24', 1.0, true),
('N-COM-S4-Q2', 'N-COM-S4', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', 'Cuando alguien malinterpreta tus intenciones (Tiempo de reacción)', 'M24', 1.0, true),
('N-COM-S4-Q3', 'N-COM-S4', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Cuando alguien malinterpreta tus intenciones (Acción resultante)', 'M24', 1.0, true),
('N-COM-S5-Q1', 'N-COM-S5', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'Al intentar explicar una regla importante (Momento de entrada)', 'M24', 1.0, true),
('N-COM-S5-Q2', 'N-COM-S5', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', 'Al intentar explicar una regla importante (Tiempo de reacción)', 'M24', 1.0, true),
('N-COM-S5-Q3', 'N-COM-S5', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Al intentar explicar una regla importante (Acción resultante)', 'M24', 1.0, true),
('N-HAB-S1-Q1', 'N-HAB-S1', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'A la hora de dormir, si tu hijo/a se resiste (Momento de entrada)', 'M24', 1.0, true),
('N-HAB-S1-Q2', 'N-HAB-S1', 'habitos', 'TIMING', 'NEURO_AWARENESS', 'A la hora de dormir, si tu hijo/a se resiste (Tiempo de reacción)', 'M24', 1.0, true),
('N-HAB-S1-Q3', 'N-HAB-S1', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'A la hora de dormir, si tu hijo/a se resiste (Acción resultante)', 'M24', 1.0, true),
('N-HAB-S2-Q1', 'N-HAB-S2', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'Al ver la habitación completamente desordenada (Momento de entrada)', 'M24', 1.0, true),
('N-HAB-S2-Q2', 'N-HAB-S2', 'habitos', 'TIMING', 'NEURO_AWARENESS', 'Al ver la habitación completamente desordenada (Tiempo de reacción)', 'M24', 1.0, true),
('N-HAB-S2-Q3', 'N-HAB-S2', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Al ver la habitación completamente desordenada (Acción resultante)', 'M24', 1.0, true),
('N-HAB-S3-Q1', 'N-HAB-S3', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'Cuando cedes en un límite que habías puesto (Momento de entrada)', 'M24', 1.0, true),
('N-HAB-S3-Q2', 'N-HAB-S3', 'habitos', 'TIMING', 'NEURO_AWARENESS', 'Cuando cedes en un límite que habías puesto (Tiempo de reacción)', 'M24', 1.0, true),
('N-HAB-S3-Q3', 'N-HAB-S3', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Cuando cedes en un límite que habías puesto (Acción resultante)', 'M24', 1.0, true),
('N-HAB-S4-Q1', 'N-HAB-S4', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'Si las rutinas de la mañana son caóticas (Momento de entrada)', 'M24', 1.0, true),
('N-HAB-S4-Q2', 'N-HAB-S4', 'habitos', 'TIMING', 'NEURO_AWARENESS', 'Si las rutinas de la mañana son caóticas (Tiempo de reacción)', 'M24', 1.0, true),
('N-HAB-S4-Q3', 'N-HAB-S4', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Si las rutinas de la mañana son caóticas (Acción resultante)', 'M24', 1.0, true),
('N-HAB-S5-Q1', 'N-HAB-S5', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'Al intentar establecer un nuevo hábito alimenticio (Momento de entrada)', 'M24', 1.0, true),
('N-HAB-S5-Q2', 'N-HAB-S5', 'habitos', 'TIMING', 'NEURO_AWARENESS', 'Al intentar establecer un nuevo hábito alimenticio (Tiempo de reacción)', 'M24', 1.0, true),
('N-HAB-S5-Q3', 'N-HAB-S5', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Al intentar establecer un nuevo hábito alimenticio (Acción resultante)', 'M24', 1.0, true),
('N-TIE-S1-Q1', 'N-TIE-S1', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Cuando tienes que trabajar pero tu hijo/a quiere jugar (Momento de entrada)', 'M24', 1.0, true),
('N-TIE-S1-Q2', 'N-TIE-S1', 'tiempos', 'TIMING', 'NEURO_AWARENESS', 'Cuando tienes que trabajar pero tu hijo/a quiere jugar (Tiempo de reacción)', 'M24', 1.0, true),
('N-TIE-S1-Q3', 'N-TIE-S1', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Cuando tienes que trabajar pero tu hijo/a quiere jugar (Acción resultante)', 'M24', 1.0, true),
('N-TIE-S2-Q1', 'N-TIE-S2', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Al darte cuenta de que pasaste todo el día sin conectar (Momento de entrada)', 'M24', 1.0, true),
('N-TIE-S2-Q2', 'N-TIE-S2', 'tiempos', 'TIMING', 'NEURO_AWARENESS', 'Al darte cuenta de que pasaste todo el día sin conectar (Tiempo de reacción)', 'M24', 1.0, true),
('N-TIE-S2-Q3', 'N-TIE-S2', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Al darte cuenta de que pasaste todo el día sin conectar (Acción resultante)', 'M24', 1.0, true),
('N-TIE-S3-Q1', 'N-TIE-S3', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Si el fin de semana se pasa en puras tareas de la casa (Momento de entrada)', 'M24', 1.0, true),
('N-TIE-S3-Q2', 'N-TIE-S3', 'tiempos', 'TIMING', 'NEURO_AWARENESS', 'Si el fin de semana se pasa en puras tareas de la casa (Tiempo de reacción)', 'M24', 1.0, true),
('N-TIE-S3-Q3', 'N-TIE-S3', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Si el fin de semana se pasa en puras tareas de la casa (Acción resultante)', 'M24', 1.0, true),
('N-TIE-S4-Q1', 'N-TIE-S4', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Al sentir que no tienes tiempo para ti mismo/a (Momento de entrada)', 'M24', 1.0, true),
('N-TIE-S4-Q2', 'N-TIE-S4', 'tiempos', 'TIMING', 'NEURO_AWARENESS', 'Al sentir que no tienes tiempo para ti mismo/a (Tiempo de reacción)', 'M24', 1.0, true),
('N-TIE-S4-Q3', 'N-TIE-S4', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Al sentir que no tienes tiempo para ti mismo/a (Acción resultante)', 'M24', 1.0, true),
('N-TIE-S5-Q1', 'N-TIE-S5', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Cuando interrumpen tu momento de descanso (Momento de entrada)', 'M24', 1.0, true),
('N-TIE-S5-Q2', 'N-TIE-S5', 'tiempos', 'TIMING', 'NEURO_AWARENESS', 'Cuando interrumpen tu momento de descanso (Tiempo de reacción)', 'M24', 1.0, true),
('N-TIE-S5-Q3', 'N-TIE-S5', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Cuando interrumpen tu momento de descanso (Acción resultante)', 'M24', 1.0, true);

-- Inserción de Opciones por Pregunta

-- Opciones para N-EMO-S1-Q1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S1-Q1';

-- Opciones para N-EMO-S1-Q2 (TIMING - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S1-Q2';

-- Opciones para N-EMO-S1-Q3 (ACTION - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S1-Q3';

-- Opciones para N-EMO-S2-Q1 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S2-Q1';

-- Opciones para N-EMO-S2-Q2 (TIMING - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S2-Q2';

-- Opciones para N-EMO-S2-Q3 (ACTION - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S2-Q3';

-- Opciones para N-EMO-S3-Q1 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S3-Q1';

-- Opciones para N-EMO-S3-Q2 (TIMING - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S3-Q2';

-- Opciones para N-EMO-S3-Q3 (ACTION - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S3-Q3';

-- Opciones para N-EMO-S4-Q1 (ENTRY - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S4-Q1';

-- Opciones para N-EMO-S4-Q2 (TIMING - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S4-Q2';

-- Opciones para N-EMO-S4-Q3 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S4-Q3';

-- Opciones para N-EMO-S5-Q1 (ENTRY - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S5-Q1';

-- Opciones para N-EMO-S5-Q2 (TIMING - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S5-Q2';

-- Opciones para N-EMO-S5-Q3 (ACTION - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-S5-Q3';

-- Opciones para N-COM-S1-Q1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S1-Q1';

-- Opciones para N-COM-S1-Q2 (TIMING - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S1-Q2';

-- Opciones para N-COM-S1-Q3 (ACTION - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S1-Q3';

-- Opciones para N-COM-S2-Q1 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S2-Q1';

-- Opciones para N-COM-S2-Q2 (TIMING - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S2-Q2';

-- Opciones para N-COM-S2-Q3 (ACTION - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S2-Q3';

-- Opciones para N-COM-S3-Q1 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S3-Q1';

-- Opciones para N-COM-S3-Q2 (TIMING - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S3-Q2';

-- Opciones para N-COM-S3-Q3 (ACTION - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S3-Q3';

-- Opciones para N-COM-S4-Q1 (ENTRY - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S4-Q1';

-- Opciones para N-COM-S4-Q2 (TIMING - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S4-Q2';

-- Opciones para N-COM-S4-Q3 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S4-Q3';

-- Opciones para N-COM-S5-Q1 (ENTRY - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S5-Q1';

-- Opciones para N-COM-S5-Q2 (TIMING - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S5-Q2';

-- Opciones para N-COM-S5-Q3 (ACTION - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-S5-Q3';

-- Opciones para N-HAB-S1-Q1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S1-Q1';

-- Opciones para N-HAB-S1-Q2 (TIMING - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S1-Q2';

-- Opciones para N-HAB-S1-Q3 (ACTION - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S1-Q3';

-- Opciones para N-HAB-S2-Q1 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S2-Q1';

-- Opciones para N-HAB-S2-Q2 (TIMING - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S2-Q2';

-- Opciones para N-HAB-S2-Q3 (ACTION - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S2-Q3';

-- Opciones para N-HAB-S3-Q1 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S3-Q1';

-- Opciones para N-HAB-S3-Q2 (TIMING - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S3-Q2';

-- Opciones para N-HAB-S3-Q3 (ACTION - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S3-Q3';

-- Opciones para N-HAB-S4-Q1 (ENTRY - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S4-Q1';

-- Opciones para N-HAB-S4-Q2 (TIMING - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S4-Q2';

-- Opciones para N-HAB-S4-Q3 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S4-Q3';

-- Opciones para N-HAB-S5-Q1 (ENTRY - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S5-Q1';

-- Opciones para N-HAB-S5-Q2 (TIMING - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S5-Q2';

-- Opciones para N-HAB-S5-Q3 (ACTION - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-S5-Q3';

-- Opciones para N-TIE-S1-Q1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S1-Q1';

-- Opciones para N-TIE-S1-Q2 (TIMING - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S1-Q2';

-- Opciones para N-TIE-S1-Q3 (ACTION - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'SOMATIC' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'SOMATIC' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'SOMATIC' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'SOMATIC' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'SOMATIC' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S1-Q3';

-- Opciones para N-TIE-S2-Q1 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S2-Q1';

-- Opciones para N-TIE-S2-Q2 (TIMING - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S2-Q2';

-- Opciones para N-TIE-S2-Q3 (ACTION - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'EMOTIONAL' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'EMOTIONAL' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'EMOTIONAL' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'EMOTIONAL' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'EMOTIONAL' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S2-Q3';

-- Opciones para N-TIE-S3-Q1 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S3-Q1';

-- Opciones para N-TIE-S3-Q2 (TIMING - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S3-Q2';

-- Opciones para N-TIE-S3-Q3 (ACTION - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'COGNITIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'COGNITIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'COGNITIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'COGNITIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'COGNITIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S3-Q3';

-- Opciones para N-TIE-S4-Q1 (ENTRY - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'IMPULSIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S4-Q1';

-- Opciones para N-TIE-S4-Q2 (TIMING - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S4-Q2';

-- Opciones para N-TIE-S4-Q3 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S4-Q3';

-- Opciones para N-TIE-S5-Q1 (ENTRY - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'NONE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S5-Q1';

-- Opciones para N-TIE-S5-Q2 (TIMING - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S5-Q2';

-- Opciones para N-TIE-S5-Q3 (ACTION - NONE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'NONE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'NONE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'NONE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'NONE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'NONE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIE-S5-Q3';
