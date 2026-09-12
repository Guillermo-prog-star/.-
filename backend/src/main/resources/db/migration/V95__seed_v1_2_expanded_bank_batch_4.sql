-- V95__seed_v1_2_expanded_bank_batch_4.sql
-- Batch 4 (Final Batch) of expanded scenarios based on Gold Standard Methodological Contract
-- Adds Scenarios 15, 16, 17, 18, 19, 20 with active = 0 (REVIEW)

-- ==========================================
-- Escenario 15 (Intensidad: 4, Dominio: normas)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S15-Q1', 'Tu hijo/a llega a casa una hora y media más tarde de lo acordado un fin de semana y entra tratando de no hacer ruido para no ser visto/a.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S15', 'normas', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":4,"trigger_type":"Incumplimiento de horario de llegada","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar la transgresión desde la calma inicial (NOTICE 4-5) previene la hostilidad y promueve el respeto a los acuerdos (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un golpe de indignación física y una tensión inmediata en todo el cuerpo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el impulso de confrontarle ruidosamente apenas pisara la casa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi enfado, pero decidí esperarle sentado/a controlando mi respiración.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la frustración, exhalé lentamente y me tomé un segundo antes de hablar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la molestia física, manteniendo una postura neutra y mi voz en tono bajo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S15-Q2', 'Tu hijo/a llega a casa una hora y media más tarde de lo acordado un fin de semana y entra tratando de no hacer ruido para no ser visto/a.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S15', 'normas', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":4,"trigger_type":"Incumplimiento de horario de llegada","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar la transgresión desde la calma inicial (NOTICE 4-5) previene la hostilidad y promueve el respeto a los acuerdos (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le importan poco las reglas de esta casa, se cree que esto es un hotel".', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le voy a prohibir salir por un mes para que aprenda a respetar los horarios".', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Sé que está mal lo que hizo, pero no quiero desatar una discusión en la madrugada".', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Incumplió el acuerdo; mañana abordaremos las consecuencias lógicas con la cabeza fría".', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Mi prioridad hoy es que llegó a salvo; mañana revisaremos el acuerdo y su responsabilidad".', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S15-Q3', 'Tu hijo/a llega a casa una hora y media más tarde de lo acordado un fin de semana y entra tratando de no hacer ruido para no ser visto/a.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S15', 'normas', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":4,"trigger_type":"Incumplimiento de horario de llegada","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar la transgresión desde la calma inicial (NOTICE 4-5) previene la hostilidad y promueve el respeto a los acuerdos (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confronté a gritos de inmediato en la entrada, exigiéndole explicaciones.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije con enojo que estaba castigado/a a partir de ese instante y le mandé a dormir.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le hablé con frialdad y sarcasmo sobre su hora de llegada antes de irme a la cama.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le indiqué que viera la hora, le pedí que fuera a descansar y le cité para hablar al día siguiente.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Verifiqué que estuviera bien, le recordé brevemente el horario y agendé una charla para la mañana.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S15-Q4', 'Tu hijo/a llega a casa una hora y media más tarde de lo acordado un fin de semana y entra tratando de no hacer ruido para no ser visto/a.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S15', 'normas', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":4,"trigger_type":"Incumplimiento de horario de llegada","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar la transgresión desde la calma inicial (NOTICE 4-5) previene la hostilidad y promueve el respeto a los acuerdos (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Discutimos a gritos en la entrada, despertando a los demás miembros del hogar.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se encerró molesto/a en su cuarto azotando la puerta y dejando el ambiente tenso.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fue a su habitación en completo silencio, con una atmósfera de reproche mutuo.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se retiró a descansar reconociendo el retraso, posponiendo la tensión para el día siguiente.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fuimos a descansar tranquilos, habiendo pactado revisar el límite con calma por la mañana.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S15-Q5', 'Tu hijo/a llega a casa una hora y media más tarde de lo acordado un fin de semana y entra tratando de no hacer ruido para no ser visto/a.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S15', 'normas', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":4,"trigger_type":"Incumplimiento de horario de llegada","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar la transgresión desde la calma inicial (NOTICE 4-5) previene la hostilidad y promueve el respeto a los acuerdos (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la llegada tarde dañó la confianza y generó un ambiente defensivo constante.', 1, 1 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una barrera de desconfianza y un control estricto que desgasta la relación.', 2, 2 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque no peleamos en el momento, el malestar afectó el trato familiar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos sostener la norma sin necesidad de generar una crisis relacional.', 4, 4 FROM questions WHERE question_key = 'M-POC-S15-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que manejar el impase con calma fortaleció el valor del acuerdo y el respeto mutuo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S15-Q5';

-- ==========================================
-- Escenario 16 (Intensidad: 2, Dominio: responsabilidad)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S16-Q1', 'Le pides a tu hijo/a que realice una tarea sencilla del hogar (como sacar la basura o lavar su plato) y se niega rotundamente argumentando pereza.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S16', 'responsabilidad', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":2,"trigger_type":"Incumplimiento de tareas del hogar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La firmeza no violenta ante las responsabilidades (ACT 4-5) fomenta el sentido de colaboración y pertenencia familiar (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una molestia rápida y un impulso físico de obligarlo/a autoritariamente.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí tensión en mi mandíbula y el deseo de gritarle por su flojera.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi frustración corporal, pero evité reaccionar bruscamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad, respiré hondo y mantuve un tono de voz suave pero firme.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la negativa con tranquilidad física, manteniendo mi cuerpo relajado.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S16-Q2', 'Le pides a tu hijo/a que realice una tarea sencilla del hogar (como sacar la basura o lavar su plato) y se niega rotundamente argumentando pereza.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S16', 'responsabilidad', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":2,"trigger_type":"Incumplimiento de tareas del hogar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La firmeza no violenta ante las responsabilidades (ACT 4-5) fomenta el sentido de colaboración y pertenencia familiar (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un consentido/a flojo/a, no quiere aportar nada a la casa".', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Si no lo hace ahora, le voy a quitar los privilegios inmediatamente".', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Qué flojera ponerme a discutir por algo tan pequeño, mejor lo hago yo".', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "El deber en casa es parte de la convivencia; debo sostener el límite con calma".', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "La colaboración no es opcional, pero le daré la opción de elegir el momento dentro de un rango".', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S16-Q3', 'Le pides a tu hijo/a que realice una tarea sencilla del hogar (como sacar la basura o lavar su plato) y se niega rotundamente argumentando pereza.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S16', 'responsabilidad', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":2,"trigger_type":"Incumplimiento de tareas del hogar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La firmeza no violenta ante las responsabilidades (ACT 4-5) fomenta el sentido de colaboración y pertenencia familiar (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité que era un/a desconsiderado/a y le obligué a hacerlo de inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le amenacé con apagarle el internet si no cumplía la tarea en ese mismo segundo.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hice la tarea yo mismo/a con obvia molestia y murmurando quejas en voz alta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le repetí la indicación con firmeza, explicándole que todos aportamos a la casa.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Establecí el límite con claridad: la tarea debía completarse antes de pasar a su tiempo de ocio.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S16-Q4', 'Le pides a tu hijo/a que realice una tarea sencilla del hogar (como sacar la basura o lavar su plato) y se niega rotundamente argumentando pereza.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S16', 'responsabilidad', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":2,"trigger_type":"Incumplimiento de tareas del hogar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La firmeza no violenta ante las responsabilidades (ACT 4-5) fomenta el sentido de colaboración y pertenencia familiar (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hizo la tarea de muy mala gana, tirando cosas y generando una disputa familiar.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se rehusó del todo y nos distanciamos con enojo por el resto de la tarde.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Cumplió la tarea refunfuñando, dejando un clima incómodo entre los dos.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Realizó la tarea tras mi insistencia, disolviendo la resistencia inicial.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Completó su responsabilidad sin mayores protestas al ver que el límite era firme y claro.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S16-Q5', 'Le pides a tu hijo/a que realice una tarea sencilla del hogar (como sacar la basura o lavar su plato) y se niega rotundamente argumentando pereza.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S16', 'responsabilidad', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":2,"trigger_type":"Incumplimiento de tareas del hogar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La firmeza no violenta ante las responsabilidades (ACT 4-5) fomenta el sentido de colaboración y pertenencia familiar (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que las tareas domésticas se convirtieron en un campo de batalla constante.', 1, 1 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una atmósfera de reproche mutuo por la falta de colaboración en el hogar.', 2, 2 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que cumplió la tarea solo por obligación, sin valorar la convivencia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos sostener las responsabilidades del hogar con orden.', 4, 4 FROM questions WHERE question_key = 'M-POC-S16-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que reforcé la estructura de colaboración mutua sin dañar la relación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S16-Q5';

-- ==========================================
-- Escenario 17 (Intensidad: 4, Dominio: respeto)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S17-Q1', 'En un centro comercial o lugar público, tu hijo/a te grita o te hace un reclamo irrespetuoso frente a otras personas por no cumplirle un capricho.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S17', 'respeto', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":4,"trigger_type":"Escena / Confrontación en público","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Contener la reactividad social ante la vergüenza pública (THINK y ACT 4-5) desescala el conflicto y preserva la dignidad mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una oleada intensa de vergüenza física, taquicardia y un impulso por callarle a gritos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la cara arder de humillación y deseos de imponer mi autoridad con dureza.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi incomodidad corporal por la mirada de la gente, pero me obligué a mantenerme firme.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el impacto del reclamo, tomé aire despacio y busqué un espacio con menos público.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la vergüenza social sin perder mi centro físico, respirando con regularidad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S17-Q2', 'En un centro comercial o lugar público, tu hijo/a te grita o te hace un reclamo irrespetuoso frente a otras personas por no cumplirle un capricho.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S17', 'respeto', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":4,"trigger_type":"Escena / Confrontación en público","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Contener la reactividad social ante la vergüenza pública (THINK y ACT 4-5) desescala el conflicto y preserva la dignidad mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me está avergonzando frente a todos, esto es el colmo de la insolencia".', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le voy a dar un escarmiento público para que aprenda a respetarme".', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Qué situación tan incómoda, haré lo que sea para que se calle rápido".', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Está buscando atención o desregularme; mantendré la calma para no alimentar la escena".', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Abordaré el desborde emocional en privado; no resolveré un conflicto relacional con público".', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S17-Q3', 'En un centro comercial o lugar público, tu hijo/a te grita o te hace un reclamo irrespetuoso frente a otras personas por no cumplirle un capricho.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S17', 'respeto', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":4,"trigger_type":"Escena / Confrontación en público","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Contener la reactividad social ante la vergüenza pública (THINK y ACT 4-5) desescala el conflicto y preserva la dignidad mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité de vuelta con ira, amenazándole públicamente y avergonzándole también.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le compré lo que quería de mala gana solo para terminar con la escena pública.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le ordené callarse bruscamente, tomándolo/a del brazo de manera tosca.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le retiré del lugar con firmeza y le dije que hablaríamos de su tono en casa.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le comuniqué con voz baja pero firme que no respondería a gritos, y nos retiramos del sitio.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S17-Q4', 'En un centro comercial o lugar público, tu hijo/a te grita o te hace un reclamo irrespetuoso frente a otras personas por no cumplirle un capricho.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S17', 'respeto', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":4,"trigger_type":"Escena / Confrontación en público","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Contener la reactividad social ante la vergüenza pública (THINK y ACT 4-5) desescala el conflicto y preserva la dignidad mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La escena escaló con más gritos y regresamos a casa en medio de un conflicto mayor.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Compramos el objeto, pero el camino a casa fue frío, tenso y lleno de desprecio.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Caminamos en silencio absoluto, cargando una profunda incomodidad y molestia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Logramos salir de la situación pública y la tensión disminuyó gradualmente al enfriarse los ánimos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pudimos conversar en un espacio privado con calma; se disculpó por el tono y aclaramos el límite.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S17-Q5', 'En un centro comercial o lugar público, tu hijo/a te grita o te hace un reclamo irrespetuoso frente a otras personas por no cumplirle un capricho.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S17', 'respeto', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":4,"trigger_type":"Escena / Confrontación en público","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Contener la reactividad social ante la vergüenza pública (THINK y ACT 4-5) desescala el conflicto y preserva la dignidad mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que perdimos el respeto en público y que la autoridad familiar quedó fracturada.', 1, 1 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó un patrón donde el berrinche o el grito se perciben como herramientas efectivas.', 2, 2 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la humillación mutua dejó una herida relacional que nos distanció.', 3, 3 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logré manejar el desborde con dignidad sin sumarme al conflicto.', 4, 4 FROM questions WHERE question_key = 'M-POC-S17-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que modelé autocontrol en una crisis y reforcé la confianza en que podemos dialogar sin gritos.', 5, 5 FROM questions WHERE question_key = 'M-POC-S17-Q5';

-- ==========================================
-- Escenario 18 (Intensidad: 3, Dominio: responsabilidad)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S18-Q1', 'Descubres un cargo inesperado en tu tarjeta de crédito por compras de juegos o suscripciones digitales realizadas por tu hijo/a sin tu autorización.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S18', 'responsabilidad', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":3,"trigger_type":"Gasto digital no autorizado","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asunción de consecuencias reparadoras (ACT 4-5) promueve la responsabilidad financiera y la honestidad en el uso de recursos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una fuerte opresión en el pecho y un impulso de ir a confrontarle de inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí enojo físico y ganas de confiscarle todos sus dispositivos electrónicos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi frustración financiera corporal, pero logré pausar unos minutos.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la molestia del gasto, respiré profundo y revisé detalladamente el reporte antes de hablar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la sorpresa y la molestia económica, manteniendo una postura física calmada y asertiva.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S18-Q2', 'Descubres un cargo inesperado en tu tarjeta de crédito por compras de juegos o suscripciones digitales realizadas por tu hijo/a sin tu autorización.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S18', 'responsabilidad', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":3,"trigger_type":"Gasto digital no autorizado","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asunción de consecuencias reparadoras (ACT 4-5) promueve la responsabilidad financiera y la honestidad en el uso de recursos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un ladrón/a, no tiene ningún respeto por el dinero que tanto me cuesta ganar".', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le voy a quitar el celular por un año entero, esto es una falta gravísima".', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Tengo mucha rabia, pero necesito entender cómo obtuvo el acceso a la cuenta".', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Hubo un fallo de honestidad y control de impulsos; buscaremos cómo va a devolver ese dinero".', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Esto requiere una lección práctica sobre el valor del dinero y la reparación de la confianza".', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S18-Q3', 'Descubres un cargo inesperado en tu tarjeta de crédito por compras de juegos o suscripciones digitales realizadas por tu hijo/a sin tu autorización.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S18', 'responsabilidad', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":3,"trigger_type":"Gasto digital no autorizado","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asunción de consecuencias reparadoras (ACT 4-5) promueve la responsabilidad financiera y la honestidad en el uso de recursos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité acusándolo/a de robarme y le descalifiqué severamente.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confisqué la consola o celular de forma indefinida sin mediar palabra.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le exigí explicaciones con tono hostil y amenacé con denunciarle.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le mostré el estado de cuenta con firmeza, explicándole el impacto de su acción.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le presenté el gasto realizado sin enojo, y acordamos un plan de trabajo en casa para reponer la suma.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S18-Q4', 'Descubres un cargo inesperado en tu tarjeta de crédito por compras de juegos o suscripciones digitales realizadas por tu hijo/a sin tu autorización.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S18', 'responsabilidad', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":3,"trigger_type":"Gasto digital no autorizado","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asunción de consecuencias reparadoras (ACT 4-5) promueve la responsabilidad financiera y la honestidad en el uso de recursos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La conversación derivó en gritos, negaciones mentirosas y un clima hostil.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se encerró molesto/a por el castigo y no nos dirigimos la palabra el resto del día.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aceptó el regaño en silencio, pero con una actitud apática y sin mostrar arrepentimiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pautamos la devolución del dinero bloqueando sus compras digitales.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Establecimos un acuerdo constructivo de devolución del gasto, lo que redujo la tensión relacional.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S18-Q5', 'Descubres un cargo inesperado en tu tarjeta de crédito por compras de juegos o suscripciones digitales realizadas por tu hijo/a sin tu autorización.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S18', 'responsabilidad', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":3,"trigger_type":"Gasto digital no autorizado","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asunción de consecuencias reparadoras (ACT 4-5) promueve la responsabilidad financiera y la honestidad en el uso de recursos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la confianza en temas económicos se quebró del todo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una dinámica de sospecha y control estricto de contraseñas muy incómoda.', 2, 2 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que el problema financiero erosionó la calidad del trato en el hogar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos dejar clara la importancia de la honestidad con los recursos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S18-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la situación sirvió para educar financieramente sin deteriorar el vínculo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S18-Q5';

-- ==========================================
-- Escenario 19 (Intensidad: 5, Dominio: convivencia)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S19-Q1', 'Recibes una llamada de la escuela informándote que tu hijo/a ha estado involucrado/a como instigador/a en una situación de acoso o agresión física a un compañero.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S19', 'convivencia', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":5,"trigger_type":"Reporte de bullying / Agresión escolar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La contención emocional y la indagación de la raíz del problema (THINK y ACT 4-5) facilitan la asunción del error y la empatía real."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una taquicardia violenta, dolor de estómago y un impulso inmediato de repudio físico.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí mi cara arder de vergüenza y rabia, queriendo reprenderle de forma violenta.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi desilusión y agitación interna, pero decidí sentarme a respirar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la gravedad de la noticia, forcé mi respiración a estabilizarse y me preparé para escuchar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré el dolor y la alarma corporal, manteniendo una actitud física sólida y receptiva.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S19-Q2', 'Recibes una llamada de la escuela informándote que tu hijo/a ha estado involucrado/a como instigador/a en una situación de acoso o agresión física a un compañero.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S19', 'convivencia', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":5,"trigger_type":"Reporte de bullying / Agresión escolar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La contención emocional y la indagación de la raíz del problema (THINK y ACT 4-5) facilitan la asunción del error y la empatía real."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Crié a un monstruo, qué clase de persona es para hacerle daño a otros".', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me arruinó la reputación, esto amerita el castigo más severo de su vida".', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me duele profundamente, pero necesito saber qué le pasa internamente para actuar así".', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un comportamiento inaceptable; abordaremos la raíz del problema sin destruir su autoestima".', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "La agresión es un síntoma de desregulación o dolor; mi rol es corregir el daño y guiarle".', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S19-Q3', 'Recibes una llamada de la escuela informándote que tu hijo/a ha estado involucrado/a como instigador/a en una situación de acoso o agresión física a un compañero.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S19', 'convivencia', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":5,"trigger_type":"Reporte de bullying / Agresión escolar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La contención emocional y la indagación de la raíz del problema (THINK y ACT 4-5) facilitan la asunción del error y la empatía real."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le descalifiqué duramente a gritos, diciéndole que me daba vergüenza y castigándolo/a de por vida.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le prohibí salir y le incomuniqué de inmediato sin escuchar su versión ni sus sentimientos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confronté con severidad extrema y frialdad, exigiéndole disculpas inmediatas sin dialogar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le expuse el reporte de la escuela con firmeza, exigiéndole que explicara su conducta.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hablé seriamente con él/ella, indagué la raíz de su agresión y acordamos medidas de disculpa y reparación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S19-Q4', 'Recibes una llamada de la escuela informándote que tu hijo/a ha estado involucrado/a como instigador/a en una situación de acoso o agresión física a un compañero.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S19', 'convivencia', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":5,"trigger_type":"Reporte de bullying / Agresión escolar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La contención emocional y la indagación de la raíz del problema (THINK y ACT 4-5) facilitan la asunción del error y la empatía real."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se desató un conflicto masivo en el hogar, con llanto hostil y encierro defensivo.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Negó todo con altanería y la brecha de comunicación se hizo más profunda.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aceptó las medidas de la escuela de mala gana, mostrando apatía ante el sufrimiento ajeno.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conversamos sobre la gravedad del acoso y aceptó las sanciones escolares correspondientes.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Logramos un diálogo honesto sobre el origen de su conducta y pactamos consecuencias lógicas y reparadoras.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S19-Q5', 'Recibes una llamada de la escuela informándote que tu hijo/a ha estado involucrado/a como instigador/a en una situación de acoso o agresión física a un compañero.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S19', 'convivencia', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":5,"trigger_type":"Reporte de bullying / Agresión escolar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La contención emocional y la indagación de la raíz del problema (THINK y ACT 4-5) facilitan la asunción del error y la empatía real."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que perdí toda conexión con mi hijo/a y le percibo con desconfianza.', 1, 1 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una atmósfera de sospecha constante y distanciamiento emocional insalvable.', 2, 2 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que acató las normas solo para evitar el castigo, sin desarrollar empatía real.', 3, 3 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos marcar un límite ético claro y firme frente al daño a terceros.', 4, 4 FROM questions WHERE question_key = 'M-POC-S19-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, a pesar de la gravedad, logramos usar la crisis para educar en empatía y responsabilidad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S19-Q5';

-- ==========================================
-- Escenario 20 (Intensidad: 3, Dominio: conexion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S20-Q1', 'Tras una pequeña discusión por una regla en el hogar, tu hijo/a se muestra completamente frío/a y te aplica la ley del hielo durante dos días.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S20', 'conexion', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":3,"trigger_type":"Retraimiento / Ley del hielo","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No personalizar el alejamiento (THINK 4-5) permite sostener los canales de conexión abiertos sin claudicar en las reglas necesarias."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un nudo de rechazo en el estómago y un impulso fuerte de pagarle con la misma moneda.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una frustración constante al pasar cerca de él/ella y deseos de gritarle para que me hablara.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi molestia ante su silencio, pero me esforcé por mantener mi rutina normal.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad de la distancia relacional, suspiré y relajé mi postura corporal.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la distancia emocional sin reactividad física, manteniendo mi cuerpo relajado y disponible.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S20-Q2', 'Tras una pequeña discusión por una regla en el hogar, tu hijo/a se muestra completamente frío/a y te aplica la ley del hielo durante dos días.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S20', 'conexion', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":3,"trigger_type":"Retraimiento / Ley del hielo","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No personalizar el alejamiento (THINK 4-5) permite sostener los canales de conexión abiertos sin claudicar en las reglas necesarias."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un/a caprichoso/a infantil, si no me habla, yo tampoco le hablaré".', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Siempre hace esto para manipularme y que yo ceda en las reglas".', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me duele su indiferencia, pero sé que es su forma de lidiar con el enojo".', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Está regulando su frustración con el silencio; yo mantendré el canal de diálogo abierto".', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Su distancia es una estrategia de defensa; sostendré el límite pero sin retirarle mi afecto".', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S20-Q3', 'Tras una pequeña discusión por una regla en el hogar, tu hijo/a se muestra completamente frío/a y te aplica la ley del hielo durante dos días.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S20', 'conexion', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":3,"trigger_type":"Retraimiento / Ley del hielo","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No personalizar el alejamiento (THINK 4-5) permite sostener los canales de conexión abiertos sin claudicar en las reglas necesarias."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le apliqué la misma ley del hielo, ignorándole por completo y mostrándome hostil.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité que dejara su actitud ridícula y le obligué a hablarme de inmediato.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le hice reclamos constantes y sarcásticos sobre su silencio mientras pasaba a su lado.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije con tono neutro que cuando estuviera listo/a para conversar con respeto, yo estaría disponible.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Saludé normalmente cada día respetando su espacio, y propuse realizar una actividad juntos sin presiones.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S20-Q4', 'Tras una pequeña discusión por una regla en el hogar, tu hijo/a se muestra completamente frío/a y te aplica la ley del hielo durante dos días.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S20', 'conexion', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":3,"trigger_type":"Retraimiento / Ley del hielo","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No personalizar el alejamiento (THINK 4-5) permite sostener los canales de conexión abiertos sin claudicar en las reglas necesarias."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La distancia se convirtió en una guerra fría de semanas, dañando el clima del hogar.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Estalló otra discusión violenta cuando intenté forzarle a hablar a la fuerza.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Continuamos interactuando de manera tensa y con monosílabos durante varios días.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Rompió el silencio al cabo de un tiempo y pudimos conversar sobre la regla en disputa.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El ambiente se distendió gradualmente al ver que mi afecto no dependía del cumplimiento de su capricho.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S20-Q5', 'Tras una pequeña discusión por una regla en el hogar, tu hijo/a se muestra completamente frío/a y te aplica la ley del hielo durante dos días.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S20', 'conexion', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":3,"trigger_type":"Retraimiento / Ley del hielo","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No personalizar el alejamiento (THINK 4-5) permite sostener los canales de conexión abiertos sin claudicar en las reglas necesarias."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la desconexión se volvió una barrera difícil de romper en nuestra relación diaria.', 1, 1 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una dinámica de manipulación relacional instalada para futuras discusiones.', 2, 2 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que el problema no se resolvió y que el distanciamiento sigue siendo una opción fácil.', 3, 3 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que sostuve el límite de la regla sin generar una ruptura relacional permanente.', 4, 4 FROM questions WHERE question_key = 'M-POC-S20-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que modelé un apego seguro al mantener la conexión disponible a pesar del desacuerdo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S20-Q5';

