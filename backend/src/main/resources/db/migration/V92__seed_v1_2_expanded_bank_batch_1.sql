-- V92__seed_v1_2_expanded_bank_batch_1.sql
-- Batch 1 of expanded scenarios based on Gold Standard Methodological Contract
-- Adds Scenarios 2, 3, 4 (comunicacion/habitos/tiempo), 21 (emocion, intrapersonal)
-- y 22 (emocion, interpersonal).
-- Escenarios 21/22 se agregan para completar cobertura de las 4 dimensiones ICF
-- (emociones/comunicacion/habitos/tiempos) antes de la auditoría editorial de Batch 1,
-- cubriendo ambas variantes del disparador emocional (sin causa externa / en vínculo).
-- active = 1: Batch 1 aprobado formalmente por el equipo editorial. Visible para familias reales.

-- ==========================================
-- Escenario 2 (Intensidad: 3, Dominio: comunicacion)
-- ==========================================
UPDATE questions SET
  text = 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'comunicacion',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Crítica percibida / Desencuentro","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La pausa física inicial (NOTICE 4-5) previene la escalada de conflicto y favorece la reparación rápida (AFTERMATH 4-5)."}'
WHERE question_key = 'M-POC-S2-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S2-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una ola de calor y rigidez muscular, preparándome para atacar.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí tensión en el pecho y un impulso fuerte por interrumpir de inmediato.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi respiración agitada, pero intenté no moverme impulsivamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad física, tomé aire y solté la tensión de las manos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré el impacto en mi cuerpo pero mantuve una postura corporal estable.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q1';

UPDATE questions SET
  text = 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'comunicacion',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Crítica percibida / Desencuentro","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La pausa física inicial (NOTICE 4-5) previene la escalada de conflicto y favorece la reparación rápida (AFTERMATH 4-5)."}'
WHERE question_key = 'M-POC-S2-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S2-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Siempre están atacándome, nunca valoran nada de lo que hago".', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es injusto que me digan eso después del esfuerzo que hice".', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me duele lo que dicen, pero intentaré no tomármelo personal".', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Están frustrados; escucharé antes de asumir que es un ataque".', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Escucharé su perspectiva completa antes de defender mi posición".', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q2';

UPDATE questions SET
  text = 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'comunicacion',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Crítica percibida / Desencuentro","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La pausa física inicial (NOTICE 4-5) previene la escalada de conflicto y favorece la reparación rápida (AFTERMATH 4-5)."}'
WHERE question_key = 'M-POC-S2-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S2-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Respondí inmediatamente con otra crítica o elevando la voz.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interrumpí rápidamente para justificarme y negar lo que decían.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Guardé silencio conteniéndome, pero mostrando evidente molestia facial.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuché hasta el final y expresé cómo me hizo sentir su comentario.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuché su punto de vista y luego planteé mi propia perspectiva con firmeza.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q3';

UPDATE questions SET
  text = 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'comunicacion',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Crítica percibida / Desencuentro","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La pausa física inicial (NOTICE 4-5) previene la escalada de conflicto y favorece la reparación rápida (AFTERMATH 4-5)."}'
WHERE question_key = 'M-POC-S2-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S2-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Terminamos en una fuerte discusión y nos alejamos profundamente ofendidos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hubo un silencio tenso e incómodo en la casa durante varias horas.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hablamos de otra cosa rápido para evitar profundizar en la tensión generada.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Logramos bajar la guardia y cada uno explicó su punto de vista con calma.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aclaramos el malentendido específico sin recurrir a ataques personales.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q4';

UPDATE questions SET
  text = 'Alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'comunicacion',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Crítica percibida / Desencuentro","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La pausa física inicial (NOTICE 4-5) previene la escalada de conflicto y favorece la reparación rápida (AFTERMATH 4-5)."}'
WHERE question_key = 'M-POC-S2-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S2-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que se generó un resentimiento que nos alejó emocionalmente.', 1, 1 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó la sensación instalada de que es mejor no hablar de esos temas.', 2, 2 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí cautela mutua, midiendo mis palabras durante el día siguiente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque el momento fue incómodo, logramos ser honestos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S2-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que pudimos procesar una fricción manteniendo el respeto mutuo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S2-Q5';

-- ==========================================
-- Escenario 3 (Intensidad: 2, Dominio: habitos)
-- ==========================================
UPDATE questions SET
  text = 'Llegas a casa o terminas el día exhausto/a y encuentras desorden en un área común.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'habitos',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Sobrecarga logística","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Reconocer el cansancio (THINK 4-5) modula la acción punitiva, protegiendo el entorno emocional (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S3-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S3-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un colapso de energía y una presión inmediata en la cabeza.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el cuerpo pesado y un impulso por suspirar ruidosamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi cansancio físico mezclado con frustración repentina.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la sobrecarga corporal y reduje mi ritmo físico a propósito.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré mi agotamiento y permití que mi cuerpo se relajara un momento antes de actuar.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q1';

UPDATE questions SET
  text = 'Llegas a casa o terminas el día exhausto/a y encuentras desorden en un área común.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'habitos',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Sobrecarga logística","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Reconocer el cansancio (THINK 4-5) modula la acción punitiva, protegiendo el entorno emocional (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S3-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S3-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Nadie me ayuda en esta casa, soy el/la único/a que hace algo".', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Son unos desconsiderados al dejar esto así sabiendo que estoy cansado/a".', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me da mucha rabia, pero también sé que hoy no tengo energía para esto".', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "El desorden me estresa, pero mi necesidad de descanso es prioridad ahora".', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Dejaré esto para mañana; la funcionalidad de la casa puede esperar".', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q2';

UPDATE questions SET
  text = 'Llegas a casa o terminas el día exhausto/a y encuentras desorden en un área común.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'habitos',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Sobrecarga logística","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Reconocer el cansancio (THINK 4-5) modula la acción punitiva, protegiendo el entorno emocional (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S3-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S3-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Comencé a limpiar agresivamente haciendo ruido para que todos notaran mi enojo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice reclamos en voz alta a quien estuviera cerca sobre el estado de la casa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Limpié lo mínimo con evidente mala actitud y desgano.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Comuniqué que estaba demasiado cansado/a para ordenar y me retiré a descansar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Dejé el desorden sin culpa y comuniqué que lo resolveríamos juntos al día siguiente.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q3';

UPDATE questions SET
  text = 'Llegas a casa o terminas el día exhausto/a y encuentras desorden en un área común.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'habitos',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Sobrecarga logística","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Reconocer el cansancio (THINK 4-5) modula la acción punitiva, protegiendo el entorno emocional (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S3-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S3-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Generé un clima de culpa y tensión en toda la familia el resto de la noche.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Los demás se alejaron de mí para evitar ser el blanco de mis quejas.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hubo una interacción fría mientras cada quien terminaba sus cosas.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Descansé, aunque con algo de incomodidad por no haber dejado todo perfecto.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pude descansar sin afectar el clima emocional del resto de la familia.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q4';

UPDATE questions SET
  text = 'Llegas a casa o terminas el día exhausto/a y encuentras desorden en un área común.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'habitos',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Sobrecarga logística","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Reconocer el cansancio (THINK 4-5) modula la acción punitiva, protegiendo el entorno emocional (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S3-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S3-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que mi agotamiento se tradujo en una distancia emocional con la familia.', 1, 1 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una atmósfera de reproche no resuelto al día siguiente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Retomamos la rutina con cierta frialdad, sin hablar de lo ocurrido.', 3, 3 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que priorizar mi descanso evitó un conflicto innecesario.', 4, 4 FROM questions WHERE question_key = 'M-POC-S3-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que modelé un límite sano sobre el autocuidado frente a mi familia.', 5, 5 FROM questions WHERE question_key = 'M-POC-S3-Q5';

-- ==========================================
-- Escenario 4 (Intensidad: 4, Dominio: tiempo)
-- ==========================================
UPDATE questions SET
  text = 'Estás en una reunión o tarea importante y un familiar interrumpe repetidas veces con demandas urgentes.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'tiempo',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Disrupción de agenda","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva (ACT 4-5) previene el daño vincular (EFFECT 4-5) frente a interrupciones."}'
WHERE question_key = 'M-POC-S4-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S4-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un salto de sobresalto y una corriente de ansiedad aguda.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí mi cuello tensarse y la respiración cortarse por la interrupción.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi irritación corporal, pero no llegué a moverme bruscamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la molestia de la distracción, cerré los ojos un segundo y me centré.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la interrupción corporalmente pero mantuve mi estado base de calma.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q1';

UPDATE questions SET
  text = 'Estás en una reunión o tarea importante y un familiar interrumpe repetidas veces con demandas urgentes.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'tiempo',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Disrupción de agenda","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva (ACT 4-5) previene el daño vincular (EFFECT 4-5) frente a interrupciones."}'
WHERE question_key = 'M-POC-S4-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S4-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "No respetan mi tiempo ni mi espacio, es imposible hacer algo aquí".', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Si me interrumpen una vez más, voy a perder el control".', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Tengo que cortar esto rápido para volver a lo mío".', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Necesitan algo, pero mi límite de tiempo también es crucial ahora".', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Pausaré para evaluar si es una emergencia real o si puede esperar".', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q2';

UPDATE questions SET
  text = 'Estás en una reunión o tarea importante y un familiar interrumpe repetidas veces con demandas urgentes.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'tiempo',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Disrupción de agenda","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva (ACT 4-5) previene el daño vincular (EFFECT 4-5) frente a interrupciones."}'
WHERE question_key = 'M-POC-S4-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S4-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Grité que me dejaran en paz y cerré la puerta con fuerza.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Respondí de mala gana, resolviendo el problema rápidamente con tono áspero.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Les hice un gesto impaciente para que se callaran mientras terminaba mi tarea.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Les dije firmemente que estaba ocupado/a y les pedí que volvieran en 20 minutos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pausé mi tarea, los miré, atendí brevemente y acordamos cuándo estaría disponible.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q3';

UPDATE questions SET
  text = 'Estás en una reunión o tarea importante y un familiar interrumpe repetidas veces con demandas urgentes.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'tiempo',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Disrupción de agenda","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva (ACT 4-5) previene el daño vincular (EFFECT 4-5) frente a interrupciones."}'
WHERE question_key = 'M-POC-S4-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S4-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El familiar se fue ofendido o asustado, y yo no pude volver a concentrarme.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se retiraron rápidamente, dejando un ambiente tenso alrededor mío.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Terminé mi tarea apresuradamente con sentimiento de culpa y molestia cruzados.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pude terminar mi actividad concentrado/a sabiendo que el límite estaba claro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Retomé mi tarea sin estrés residual, y ellos respetaron el tiempo acordado.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q4';

UPDATE questions SET
  text = 'Estás en una reunión o tarea importante y un familiar interrumpe repetidas veces con demandas urgentes.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 1,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'tiempo',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Disrupción de agenda","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva (ACT 4-5) previene el daño vincular (EFFECT 4-5) frente a interrupciones."}'
WHERE question_key = 'M-POC-S4-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S4-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que causé un daño innecesario en la relación por mi estrés laboral.', 1, 1 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una sensación de desconexión; evitaron acercarse a mí más tarde.', 2, 2 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tuvimos una interacción precavida después, sin hablar de la interrupción.', 3, 3 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que respetaron mi espacio sin que nuestra dinámica se viera afectada.', 4, 4 FROM questions WHERE question_key = 'M-POC-S4-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí claridad en que los límites de tiempo pueden sostenerse con respeto mutuo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S4-Q5';

-- ==========================================
-- Escenario 21 (Intensidad: 3, Dominio: emocion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S21-Q1', 'Sientes una ola de ansiedad o angustia intensa que aparece de forma repentina, sin que puedas identificar una causa externa clara en ese momento.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S21', 'emocion', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":3,"trigger_type":"Activación emocional sin causa externa identificable","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La capacidad de nombrar y contener la activación emocional inicial (NOTICE/THINK 4-5) sin necesitar una causa externa reduce el impacto relacional posterior (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el pecho oprimirse y el corazón acelerarse de golpe, sin poder ubicar por qué.', 1, 1 FROM questions WHERE question_key = 'M-POC-S21-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un nudo en el estómago y la respiración volverse corta y superficial.', 2, 2 FROM questions WHERE question_key = 'M-POC-S21-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté la tensión general en el cuerpo, tratando de identificar de dónde venía.', 3, 3 FROM questions WHERE question_key = 'M-POC-S21-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la activación corporal y me detuve un momento a notar dónde la sentía exactamente.', 4, 4 FROM questions WHERE question_key = 'M-POC-S21-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí la sensación como ansiedad pasajera, sin resistirme a que estuviera ahí.', 5, 5 FROM questions WHERE question_key = 'M-POC-S21-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S21-Q2', 'Sientes una ola de ansiedad o angustia intensa que aparece de forma repentina, sin que puedas identificar una causa externa clara en ese momento.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S21', 'emocion', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":3,"trigger_type":"Activación emocional sin causa externa identificable","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La capacidad de nombrar y contener la activación emocional inicial (NOTICE/THINK 4-5) sin necesitar una causa externa reduce el impacto relacional posterior (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Algo malo está por pasar, tengo que anticiparlo ya".', 1, 1 FROM questions WHERE question_key = 'M-POC-S21-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"No puedo con esto, se me va a salir de las manos".', 2, 2 FROM questions WHERE question_key = 'M-POC-S21-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"No sé bien qué es esto, pero me está costando pensar con claridad".', 3, 3 FROM questions WHERE question_key = 'M-POC-S21-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Esto es incómodo, pero sé que las sensaciones así pasan si les doy espacio".', 4, 4 FROM questions WHERE question_key = 'M-POC-S21-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Es ansiedad, no peligro real; puedo seguir funcionando mientras la acompaño".', 5, 5 FROM questions WHERE question_key = 'M-POC-S21-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S21-Q3', 'Sientes una ola de ansiedad o angustia intensa que aparece de forma repentina, sin que puedas identificar una causa externa clara en ese momento.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S21', 'emocion', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":3,"trigger_type":"Activación emocional sin causa externa identificable","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La capacidad de nombrar y contener la activación emocional inicial (NOTICE/THINK 4-5) sin necesitar una causa externa reduce el impacto relacional posterior (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Empecé a buscar de forma frenética a qué o quién culpar de lo que sentía.', 1, 1 FROM questions WHERE question_key = 'M-POC-S21-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Interrumpí lo que estaba haciendo para resolver algo, aunque no supiera bien qué.', 2, 2 FROM questions WHERE question_key = 'M-POC-S21-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me quedé paralizado/a, sin poder continuar con lo que tenía que hacer.', 3, 3 FROM questions WHERE question_key = 'M-POC-S21-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice una pausa breve, respiré un par de veces y seguí con la tarea a medio ritmo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S21-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Nombré en silencio lo que sentía y continué con lo que estaba haciendo sin exigirme calma inmediata.', 5, 5 FROM questions WHERE question_key = 'M-POC-S21-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S21-Q4', 'Sientes una ola de ansiedad o angustia intensa que aparece de forma repentina, sin que puedas identificar una causa externa clara en ese momento.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S21', 'emocion', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":3,"trigger_type":"Activación emocional sin causa externa identificable","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La capacidad de nombrar y contener la activación emocional inicial (NOTICE/THINK 4-5) sin necesitar una causa externa reduce el impacto relacional posterior (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La ansiedad escaló hasta un pico que me impidió seguir con cualquier actividad.', 1, 1 FROM questions WHERE question_key = 'M-POC-S21-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedé disperso/a y de mal humor con quienes estaban cerca, sin razón aparente para ellos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S21-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La sensación bajó lentamente, pero me costó volver a concentrarme el resto del rato.', 3, 3 FROM questions WHERE question_key = 'M-POC-S21-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La intensidad bajó en unos minutos y pude retomar mis actividades con normalidad.', 4, 4 FROM questions WHERE question_key = 'M-POC-S21-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté cómo la sensación iba bajando mientras seguía funcionando con quienes me rodeaban.', 5, 5 FROM questions WHERE question_key = 'M-POC-S21-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S21-Q5', 'Sientes una ola de ansiedad o angustia intensa que aparece de forma repentina, sin que puedas identificar una causa externa clara en ese momento.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S21', 'emocion', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":3,"trigger_type":"Activación emocional sin causa externa identificable","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"La capacidad de nombrar y contener la activación emocional inicial (NOTICE/THINK 4-5) sin necesitar una causa externa reduce el impacto relacional posterior (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que quedé más alerta y desconfiado/a del resto del día, sin saber por qué.', 1, 1 FROM questions WHERE question_key = 'M-POC-S21-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedé agotado/a y distante con mi familia el resto de la jornada.', 2, 2 FROM questions WHERE question_key = 'M-POC-S21-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí cierto desgaste, pero pude estar presente igual con los demás.', 3, 3 FROM questions WHERE question_key = 'M-POC-S21-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que pude sostener mis vínculos aunque la ansiedad hubiera aparecido.', 4, 4 FROM questions WHERE question_key = 'M-POC-S21-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que procesar la emoción sin dramatizarla me dejó más disponible para mi familia.', 5, 5 FROM questions WHERE question_key = 'M-POC-S21-Q5';

-- ==========================================
-- Escenario 22 (Intensidad: 3, Dominio: emocion — variante interpersonal)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S22-Q1', 'Compartes con un familiar algo que te emociona profundamente (alegría, orgullo o dolor) y notas que su reacción es indiferente o distraída.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S22', 'emocion', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":3,"trigger_type":"Desborde emocional no correspondido / vulnerabilidad expuesta","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Sostener la propia emoción sin necesitar validación inmediata del otro (ACT/AFTERMATH 4-5) protege el vínculo de un ciclo de reproche-distancia (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un vacío repentino en el pecho y las mejillas calientes de vergüenza.', 1, 1 FROM questions WHERE question_key = 'M-POC-S22-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que se me cerraba la garganta y quería replegarme de inmediato.', 2, 2 FROM questions WHERE question_key = 'M-POC-S22-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté la decepción física, pero intenté seguir sosteniendo lo que estaba contando.', 3, 3 FROM questions WHERE question_key = 'M-POC-S22-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la desilusión en el cuerpo y me tomé un segundo antes de reaccionar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S22-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reconocí la emoción de no sentirme visto/a, sin que se apoderara de mí.', 5, 5 FROM questions WHERE question_key = 'M-POC-S22-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S22-Q2', 'Compartes con un familiar algo que te emociona profundamente (alegría, orgullo o dolor) y notas que su reacción es indiferente o distraída.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S22', 'emocion', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":3,"trigger_type":"Desborde emocional no correspondido / vulnerabilidad expuesta","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Sostener la propia emoción sin necesitar validación inmediata del otro (ACT/AFTERMATH 4-5) protege el vínculo de un ciclo de reproche-distancia (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"No le importo, nunca le importa lo que me pasa".', 1, 1 FROM questions WHERE question_key = 'M-POC-S22-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Mejor no comparto nada más, para qué si no escucha".', 2, 2 FROM questions WHERE question_key = 'M-POC-S22-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Me dolió, aunque quizás no fue intencional de su parte".', 3, 3 FROM questions WHERE question_key = 'M-POC-S22-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Su distracción no necesariamente habla de lo que valgo yo o lo que sentí".', 4, 4 FROM questions WHERE question_key = 'M-POC-S22-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, '"Puedo seguir sintiendo lo mío como válido aunque él/ella no lo haya notado".', 5, 5 FROM questions WHERE question_key = 'M-POC-S22-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S22-Q3', 'Compartes con un familiar algo que te emociona profundamente (alegría, orgullo o dolor) y notas que su reacción es indiferente o distraída.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S22', 'emocion', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":3,"trigger_type":"Desborde emocional no correspondido / vulnerabilidad expuesta","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Sostener la propia emoción sin necesitar validación inmediata del otro (ACT/AFTERMATH 4-5) protege el vínculo de un ciclo de reproche-distancia (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le reclamé con dureza que nunca me presta atención a nada.', 1, 1 FROM questions WHERE question_key = 'M-POC-S22-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Cerré la conversación de golpe y me fui visiblemente molesto/a.', 2, 2 FROM questions WHERE question_key = 'M-POC-S22-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Bajé la voz y dejé el tema a medias, sin decir lo que sentía.', 3, 3 FROM questions WHERE question_key = 'M-POC-S22-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le nombré con calma que sentí que no me estaba escuchando.', 4, 4 FROM questions WHERE question_key = 'M-POC-S22-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Terminé de compartir lo que sentía y luego le pregunté si podíamos retomarlo con más atención.', 5, 5 FROM questions WHERE question_key = 'M-POC-S22-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S22-Q4', 'Compartes con un familiar algo que te emociona profundamente (alegría, orgullo o dolor) y notas que su reacción es indiferente o distraída.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S22', 'emocion', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":3,"trigger_type":"Desborde emocional no correspondido / vulnerabilidad expuesta","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Sostener la propia emoción sin necesitar validación inmediata del otro (ACT/AFTERMATH 4-5) protege el vínculo de un ciclo de reproche-distancia (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Terminamos discutiendo por algo que ya no tenía que ver con el tema original.', 1, 1 FROM questions WHERE question_key = 'M-POC-S22-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó un silencio incómodo y distancia física entre los dos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S22-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Seguimos hablando de otra cosa, pero con cierta tensión no resuelta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S22-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Retomamos el tema con más calma minutos después, con mejor disposición de ambos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S22-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Logramos volver atrás y compartir lo que había quedado pendiente con más presencia.', 5, 5 FROM questions WHERE question_key = 'M-POC-S22-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata)
VALUES ('M-POC-S22-Q5', 'Compartes con un familiar algo que te emociona profundamente (alegría, orgullo o dolor) y notas que su reacción es indiferente o distraída.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 1, 1, 0, 'M-POC-S22', 'emocion', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":3,"trigger_type":"Desborde emocional no correspondido / vulnerabilidad expuesta","expected_age_range":"General","family_role":"Cualquiera","validation_status":"EXPANSION_BATCH_1","pilot_version":"1.2.1","clinical_hypothesis":"Sostener la propia emoción sin necesitar validación inmediata del otro (ACT/AFTERMATH 4-5) protege el vínculo de un ciclo de reproche-distancia (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que aprendí a no compartir cosas importantes con esa persona.', 1, 1 FROM questions WHERE question_key = 'M-POC-S22-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una sensación de lejanía que no se habló abiertamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S22-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí cierta cautela para la próxima vez que quisiera compartir algo así.', 3, 3 FROM questions WHERE question_key = 'M-POC-S22-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que el vínculo se sostuvo, aunque el momento inicial doliera.', 4, 4 FROM questions WHERE question_key = 'M-POC-S22-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que pudimos reparar la desconexión y el vínculo se sintió más seguro.', 5, 5 FROM questions WHERE question_key = 'M-POC-S22-Q5';

