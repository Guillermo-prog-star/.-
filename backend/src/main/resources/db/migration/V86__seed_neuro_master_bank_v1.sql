-- V86__seed_neuro_master_bank_v1.sql
-- Inyección del Banco Maestro Neurofenomenológico V1 (20 Reactivos)

INSERT IGNORE INTO questions 
(question_key, dimension, phase, type, text, milestone_code, severity_weight, active) 
VALUES
('N-EMO-1', 'emociones', 'ENTRY', 'NEURO_AWARENESS', '¿Dónde notas primero que estás a punto de perder la paciencia con tu hijo/a?', 'M24', 1.0, true),
('N-EMO-2', 'emociones', 'ENTRY', 'NEURO_AWARENESS', 'Cuando sientes una emoción intensa, ¿qué te sucede internamente?', 'M24', 1.0, true),
('N-EMO-3', 'emociones', 'TIMING', 'NEURO_AWARENESS', '¿Cuánto tardas en darte cuenta de que tu tono de voz ha cambiado por el enojo?', 'M24', 1.0, true),
('N-EMO-4', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Cuando estás muy frustrado/a, ¿qué sueles hacer primero?', 'M24', 1.0, true),
('N-EMO-5', 'emociones', 'ACTION', 'NEURO_AWARENESS', 'Si logras notar tu enojo antes de explotar, ¿cómo procedes?', 'M24', 1.0, true),
('N-COM-1', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'Cuando tu hijo/a te contradice, ¿qué pensamiento surge primero?', 'M24', 1.0, true),
('N-COM-2', 'comunicacion', 'ENTRY', 'NEURO_AWARENESS', 'En medio de una discusión acalorada, ¿dónde sientes la tensión?', 'M24', 1.0, true),
('N-COM-3', 'comunicacion', 'TIMING', 'NEURO_AWARENESS', '¿En qué momento de una conversación difícil notas que ya no estás escuchando?', 'M24', 1.0, true),
('N-COM-4', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Cuando sientes que la comunicación se rompió, ¿cómo reaccionas?', 'M24', 1.0, true),
('N-COM-5', 'comunicacion', 'ACTION', 'NEURO_AWARENESS', 'Si identificas que están discutiendo sin llegar a nada, ¿qué haces?', 'M24', 1.0, true),
('N-HAB-1', 'habitos', 'ENTRY', 'NEURO_AWARENESS', '¿Qué sientes cuando es hora de dormir y tu hijo/a no hace caso?', 'M24', 1.0, true),
('N-HAB-2', 'habitos', 'ENTRY', 'NEURO_AWARENESS', 'Cuando rompes un límite que tú mismo pusiste, ¿qué te dices internamente?', 'M24', 1.0, true),
('N-HAB-3', 'habitos', 'TIMING', 'NEURO_AWARENESS', '¿Cuánto tiempo pasa desde que das una orden hasta que decides intervenir físicamente?', 'M24', 1.0, true),
('N-HAB-4', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Al ver que las rutinas no se cumplen, ¿cuál es tu primera respuesta?', 'M24', 1.0, true),
('N-HAB-5', 'habitos', 'ACTION', 'NEURO_AWARENESS', 'Si notas que estás a punto de ceder ante un berrinche, ¿cómo decides actuar?', 'M24', 1.0, true),
('N-TIM-1', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', '¿Dónde sientes el estrés cuando no tienes tiempo suficiente para la familia?', 'M24', 1.0, true),
('N-TIM-2', 'tiempos', 'ENTRY', 'NEURO_AWARENESS', 'Cuando por fin estás jugando con tu hijo/a, ¿qué sueles pensar?', 'M24', 1.0, true),
('N-TIM-3', 'tiempos', 'TIMING', 'NEURO_AWARENESS', '¿Cuánto tiempo pasa hasta que logras desconectarte del trabajo estando en casa?', 'M24', 1.0, true),
('N-TIM-4', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Si tu hijo/a interrumpe tu descanso, ¿cómo respondes inmediatamente?', 'M24', 1.0, true),
('N-TIM-5', 'tiempos', 'ACTION', 'NEURO_AWARENESS', 'Cuando te das cuenta de que has estado ausente mentalmente, ¿qué ajuste haces?', 'M24', 1.0, true);

-- Inserción de Opciones por Reactivo

-- Opciones para N-EMO-1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-1';

-- Opciones para N-EMO-2 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-2';

-- Opciones para N-EMO-3 (TIMING - PAUSE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'PAUSE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'PAUSE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'PAUSE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'PAUSE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'PAUSE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-3';

-- Opciones para N-EMO-4 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-4';

-- Opciones para N-EMO-5 (ACTION - AGENCY)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'AGENCY' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'AGENCY' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'AGENCY' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'AGENCY' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'AGENCY' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-EMO-5';

-- Opciones para N-COM-1 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-1';

-- Opciones para N-COM-2 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-2';

-- Opciones para N-COM-3 (TIMING - PAUSE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'PAUSE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'PAUSE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'PAUSE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'PAUSE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'PAUSE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-3';

-- Opciones para N-COM-4 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-4';

-- Opciones para N-COM-5 (ACTION - AGENCY)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'AGENCY' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'AGENCY' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'AGENCY' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'AGENCY' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'AGENCY' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-COM-5';

-- Opciones para N-HAB-1 (ENTRY - EMOTIONAL)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'EMOTIONAL' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-1';

-- Opciones para N-HAB-2 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-2';

-- Opciones para N-HAB-3 (TIMING - PAUSE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'PAUSE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'PAUSE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'PAUSE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'PAUSE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'PAUSE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-3';

-- Opciones para N-HAB-4 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-4';

-- Opciones para N-HAB-5 (ACTION - AGENCY)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'AGENCY' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'AGENCY' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'AGENCY' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'AGENCY' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'AGENCY' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-HAB-5';

-- Opciones para N-TIM-1 (ENTRY - SOMATIC)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'SOMATIC' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIM-1';

-- Opciones para N-TIM-2 (ENTRY - COGNITIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L1' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L2' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L3' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L4' as comp UNION ALL
  SELECT 0 as score, 'COGNITIVE' as vector, 'ENTRY_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIM-2';

-- Opciones para N-TIM-3 (TIMING - PAUSE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'PAUSE' as vector, 'TIMING_L1' as comp UNION ALL
  SELECT 2 as score, 'PAUSE' as vector, 'TIMING_L2' as comp UNION ALL
  SELECT 3 as score, 'PAUSE' as vector, 'TIMING_L3' as comp UNION ALL
  SELECT 4 as score, 'PAUSE' as vector, 'TIMING_L4' as comp UNION ALL
  SELECT 5 as score, 'PAUSE' as vector, 'TIMING_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIM-3';

-- Opciones para N-TIM-4 (ACTION - IMPULSIVE)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'IMPULSIVE' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'IMPULSIVE' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'IMPULSIVE' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'IMPULSIVE' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'IMPULSIVE' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIM-4';

-- Opciones para N-TIM-5 (ACTION - INTEGRATION)
INSERT IGNORE INTO question_options (question_id, score_value, vector_tag, awareness_component)
SELECT rq.id, tmp.score, tmp.vector, tmp.comp
FROM (
  SELECT 1 as score, 'INTEGRATION' as vector, 'ACTION_L1' as comp UNION ALL
  SELECT 2 as score, 'INTEGRATION' as vector, 'ACTION_L2' as comp UNION ALL
  SELECT 3 as score, 'INTEGRATION' as vector, 'ACTION_L3' as comp UNION ALL
  SELECT 4 as score, 'INTEGRATION' as vector, 'ACTION_L4' as comp UNION ALL
  SELECT 5 as score, 'INTEGRATION' as vector, 'ACTION_L5' as comp
) tmp
JOIN questions rq ON rq.question_key = 'N-TIM-5';
