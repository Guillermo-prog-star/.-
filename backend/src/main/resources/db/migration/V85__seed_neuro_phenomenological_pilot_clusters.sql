-- V85: Banco de Escenarios Piloto Neurofenomenológicos
-- Estos no son los reactivos definitivos del banco maestro, sino escenarios piloto (inc_legacy_summary aplicable).

INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-EMO-001-Q1', 'Q-EMO-001', 'Cuando alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo... ¿Qué apareció primero en ti?', 'NEURO_AWARENESS', 'emociones', 'DIAGNOSTIC', 'ENTRY', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Siento una punzada en el pecho o tensión muscular', 0, 'ENTRY_SOMATIC', 'SOMATIC' FROM questions WHERE question_key = 'Q-EMO-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Siento una punzada en el pecho o tensión muscular');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me siento avergonzado/a o atacado/a', 0, 'ENTRY_EMOTIONAL', 'EMOTIONAL' FROM questions WHERE question_key = 'Q-EMO-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me siento avergonzado/a o atacado/a');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Pienso "No valoran lo que hago"', 0, 'ENTRY_COGNITIVE', 'COGNITIVE' FROM questions WHERE question_key = 'Q-EMO-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Pienso "No valoran lo que hago"');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Siento el impulso inmediato de defenderme', 0, 'ENTRY_IMPULSIVE', 'IMPULSIVE' FROM questions WHERE question_key = 'Q-EMO-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Siento el impulso inmediato de defenderme');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'No logré notar nada internamente', 0, 'ENTRY_NONE', 'NONE' FROM questions WHERE question_key = 'Q-EMO-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'No logré notar nada internamente');
INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-EMO-001-Q2', 'Q-EMO-001', 'Cuando alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo... ¿Cuándo te diste cuenta de esa reacción?', 'NEURO_AWARENESS', 'emociones', 'DIAGNOSTIC', 'TIMING', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'No me di cuenta hasta que alguien me lo hizo notar', 1, 'TIMING_BLIND', 'BLIND' FROM questions WHERE question_key = 'Q-EMO-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'No me di cuenta hasta que alguien me lo hizo notar');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta tiempo después del incidente', 2, 'TIMING_RETROSPECTIVE', 'RETROSPECTIVE' FROM questions WHERE question_key = 'Q-EMO-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta tiempo después del incidente');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta mientras estaba reaccionando', 3, 'TIMING_SYNCHRONOUS', 'SYNCHRONOUS' FROM questions WHERE question_key = 'Q-EMO-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta mientras estaba reaccionando');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta antes de decir o hacer algo', 4, 'TIMING_ANTICIPATORY', 'ANTICIPATORY' FROM questions WHERE question_key = 'Q-EMO-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta antes de decir o hacer algo');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Permanecí consciente de mí mismo/a durante toda la situación', 5, 'TIMING_SUSTAINED', 'SUSTAINED' FROM questions WHERE question_key = 'Q-EMO-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Permanecí consciente de mí mismo/a durante toda la situación');
INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-EMO-001-Q3', 'Q-EMO-001', 'Cuando alguien en casa hace un comentario que percibes como una crítica injusta a tu esfuerzo... ¿Qué hiciste con eso que notaste?', 'NEURO_AWARENESS', 'emociones', 'DIAGNOSTIC', 'ACTION', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Reaccioné automáticamente a la defensiva', 1, 'ACTION_AUTOMATIC', 'AUTOMATIC' FROM questions WHERE question_key = 'Q-EMO-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Reaccioné automáticamente a la defensiva');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Intenté suprimir mi enojo y quedarme callado/a forzosamente', 2, 'ACTION_SUPPRESSION', 'SUPPRESSION' FROM questions WHERE question_key = 'Q-EMO-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Intenté suprimir mi enojo y quedarme callado/a forzosamente');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Hice una pausa para no empeorar las cosas, aunque seguía molesto/a', 3, 'ACTION_PAUSE', 'PAUSE' FROM questions WHERE question_key = 'Q-EMO-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Hice una pausa para no empeorar las cosas, aunque seguía molesto/a');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Elegí conscientemente pedir un momento antes de hablar', 4, 'ACTION_AGENCY', 'AGENCY' FROM questions WHERE question_key = 'Q-EMO-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Elegí conscientemente pedir un momento antes de hablar');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Respondí desde la calma, preguntando para comprender su punto de vista', 5, 'ACTION_INTEGRATION', 'INTEGRATION' FROM questions WHERE question_key = 'Q-EMO-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Respondí desde la calma, preguntando para comprender su punto de vista');

INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-COM-001-Q1', 'Q-COM-001', 'Cuando hay una discusión acalorada y sientes que el otro no te está escuchando... ¿Qué apareció primero en ti?', 'NEURO_AWARENESS', 'comunicacion', 'DIAGNOSTIC', 'ENTRY', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Siento calor en la cara y aceleración cardíaca', 0, 'ENTRY_SOMATIC', 'SOMATIC' FROM questions WHERE question_key = 'Q-COM-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Siento calor en la cara y aceleración cardíaca');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Siento frustración o impotencia profunda', 0, 'ENTRY_EMOTIONAL', 'EMOTIONAL' FROM questions WHERE question_key = 'Q-COM-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Siento frustración o impotencia profunda');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Pienso "Es imposible hablar con esta persona"', 0, 'ENTRY_COGNITIVE', 'COGNITIVE' FROM questions WHERE question_key = 'Q-COM-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Pienso "Es imposible hablar con esta persona"');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Tengo el impulso de gritar o irme del lugar', 0, 'ENTRY_IMPULSIVE', 'IMPULSIVE' FROM questions WHERE question_key = 'Q-COM-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Tengo el impulso de gritar o irme del lugar');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'No logré notar nada, me desconecté', 0, 'ENTRY_NONE', 'NONE' FROM questions WHERE question_key = 'Q-COM-001-Q1' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'No logré notar nada, me desconecté');
INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-COM-001-Q2', 'Q-COM-001', 'Cuando hay una discusión acalorada y sientes que el otro no te está escuchando... ¿Cuándo te diste cuenta de esa reacción?', 'NEURO_AWARENESS', 'comunicacion', 'DIAGNOSTIC', 'TIMING', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'No me di cuenta, me perdí en la discusión', 1, 'TIMING_BLIND', 'BLIND' FROM questions WHERE question_key = 'Q-COM-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'No me di cuenta, me perdí en la discusión');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta después de que terminamos de pelear', 2, 'TIMING_RETROSPECTIVE', 'RETROSPECTIVE' FROM questions WHERE question_key = 'Q-COM-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta después de que terminamos de pelear');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta mientras estaba levantando la voz', 3, 'TIMING_SYNCHRONOUS', 'SYNCHRONOUS' FROM questions WHERE question_key = 'Q-COM-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta mientras estaba levantando la voz');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me di cuenta justo antes de reaccionar agresivamente', 4, 'TIMING_ANTICIPATORY', 'ANTICIPATORY' FROM questions WHERE question_key = 'Q-COM-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me di cuenta justo antes de reaccionar agresivamente');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Mantuve la presencia y atención durante todo el intercambio', 5, 'TIMING_SUSTAINED', 'SUSTAINED' FROM questions WHERE question_key = 'Q-COM-001-Q2' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Mantuve la presencia y atención durante todo el intercambio');
INSERT IGNORE INTO questions (question_key, parent_key, text, type, dimension, area, phase, active, severity_weight)
VALUES ('Q-COM-001-Q3', 'Q-COM-001', 'Cuando hay una discusión acalorada y sientes que el otro no te está escuchando... ¿Qué hiciste con eso que notaste?', 'NEURO_AWARENESS', 'comunicacion', 'DIAGNOSTIC', 'ACTION', true, 1.0);
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Elevé la voz e interrumpí para imponerme', 1, 'ACTION_AUTOMATIC', 'AUTOMATIC' FROM questions WHERE question_key = 'Q-COM-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Elevé la voz e interrumpí para imponerme');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Me rendí y dejé la conversación a medias reprimiendo mi frustración', 2, 'ACTION_SUPPRESSION', 'SUPPRESSION' FROM questions WHERE question_key = 'Q-COM-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Me rendí y dejé la conversación a medias reprimiendo mi frustración');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Hice silencio para detener la escalada', 3, 'ACTION_PAUSE', 'PAUSE' FROM questions WHERE question_key = 'Q-COM-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Hice silencio para detener la escalada');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Propuse explícitamente retomar la charla más tarde', 4, 'ACTION_AGENCY', 'AGENCY' FROM questions WHERE question_key = 'Q-COM-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Propuse explícitamente retomar la charla más tarde');
INSERT IGNORE INTO question_options (question_id, text, score_value, awareness_component, vector_tag)
SELECT id, 'Validé lo que el otro decía primero y luego expresé mi punto con calma', 5, 'ACTION_INTEGRATION', 'INTEGRATION' FROM questions WHERE question_key = 'Q-COM-001-Q3' AND NOT EXISTS (SELECT 1 FROM question_options WHERE question_id = questions.id AND text = 'Validé lo que el otro decía primero y luego expresé mi punto con calma');

