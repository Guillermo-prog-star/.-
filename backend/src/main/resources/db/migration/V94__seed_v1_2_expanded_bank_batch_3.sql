-- V94__seed_v1_2_expanded_bank_batch_3.sql
-- Batch 3 of expanded scenarios based on Gold Standard Methodological Contract
-- Adds Scenarios 10, 11, 12, 13, 14 with active = 0 (REVIEW)

-- ==========================================
-- Escenario 10 (Intensidad: 3, Dominio: responsabilidad)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S10-Q1', 'Te enteras por la escuela de que tu hijo/a ha reprobado múltiples asignaturas y te lo había ocultado.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S10', 'responsabilidad', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":3,"trigger_type":"Bajo rendimiento escolar / Ocultamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La atribución de responsabilidad constructiva (ACT 4-5) fomenta la autoeficacia académica (EFFECT 4-5) en lugar del abandono o evasión."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un vuelco en el estómago y una tensión caliente en los hombros.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí decepción física inmediata y un impulso por gritar y exigir explicaciones.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi desilusión corporal, pero forcé una respiración lenta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad de la mala noticia, bajé los hombros y me senté.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la sorpresa y la frustración física, manteniendo mi respiración regulada.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S10-Q2', 'Te enteras por la escuela de que tu hijo/a ha reprobado múltiples asignaturas y te lo había ocultado.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S10', 'responsabilidad', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":3,"trigger_type":"Bajo rendimiento escolar / Ocultamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La atribución de responsabilidad constructiva (ACT 4-5) fomenta la autoeficacia académica (EFFECT 4-5) en lugar del abandono o evasión."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un/a vago/a, no le interesa su futuro ni valora lo que pago".', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me mintió en la cara todo este tiempo, esto no se lo voy a perdonar".', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me preocupa mucho su rendimiento, pero primero debo entender por qué no me lo dijo".', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "El ocultamiento es por miedo; necesito abrir un espacio seguro antes de hablar de las notas".', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "El fracaso escolar es un síntoma; nos enfocaremos en la solución y el apoyo mutuo".', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S10-Q3', 'Te enteras por la escuela de que tu hijo/a ha reprobado múltiples asignaturas y te lo había ocultado.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S10', 'responsabilidad', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":3,"trigger_type":"Bajo rendimiento escolar / Ocultamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La atribución de responsabilidad constructiva (ACT 4-5) fomenta la autoeficacia académica (EFFECT 4-5) en lugar del abandono o evasión."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité fuertemente, le llamé irresponsable y le quité todo derecho a salir.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le impuse un castigo estricto enfocado en privaciones sin escuchar sus razones.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le interrogué con tono acusatorio y distante, mostrándome extremadamente decepcionado/a.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le expuse la información con firmeza y le pedí que me explicara qué estaba pasando.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Conversé con tranquilidad, escuché sus dificultades académicas y le invité a diseñar juntos un plan de estudio.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S10-Q4', 'Te enteras por la escuela de que tu hijo/a ha reprobado múltiples asignaturas y te lo había ocultado.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S10', 'responsabilidad', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":3,"trigger_type":"Bajo rendimiento escolar / Ocultamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La atribución de responsabilidad constructiva (ACT 4-5) fomenta la autoeficacia académica (EFFECT 4-5) en lugar del abandono o evasión."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tuvimos una discusión circular cargada de llanto, gritos y actitud defensiva.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se encerró en su habitación enfadado/a y la comunicación se cortó por completo.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aceptó el castigo con resignación resentida y no volvimos a tocar el tema ese día.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tuvimos una conversación seria y establecimos horarios de estudio obligatorios.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Cerramos la charla con un compromiso de apoyo escolar mutuo sin dañar el clima familiar.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S10-Q5', 'Te enteras por la escuela de que tu hijo/a ha reprobado múltiples asignaturas y te lo había ocultado.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S10', 'responsabilidad', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":3,"trigger_type":"Bajo rendimiento escolar / Ocultamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La atribución de responsabilidad constructiva (ACT 4-5) fomenta la autoeficacia académica (EFFECT 4-5) en lugar del abandono o evasión."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que este problema nos distanció y que ahora me ocultará aún más cosas.', 1, 1 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una atmósfera de desconfianza mutua y tensión constante respecto al estudio.', 2, 2 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque acató la norma, la motivación de mi hijo/a por aprender disminuyó.', 3, 3 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos poner orden y claridad respecto a las responsabilidades escolares.', 4, 4 FROM questions WHERE question_key = 'M-POC-S10-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la dificultad nos unió para resolver problemas de manera constructiva y honesta.', 5, 5 FROM questions WHERE question_key = 'M-POC-S10-Q5';

-- ==========================================
-- Escenario 11 (Intensidad: 2, Dominio: convivencia)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S11-Q1', 'Dos de tus hijos se gritan e insultan fuertemente en la sala por el uso de un espacio u objeto común.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S11', 'convivencia', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":2,"trigger_type":"Conflicto entre hermanos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La mediación neutral sin tomar partido (ACT 4-5) reduce la hostilidad a largo plazo (EFFECT 4-5) y modela la resolución de conflictos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una punzada de dolor de cabeza y un impulso de gritar más fuerte que ellos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí mi cuerpo tenso y una desesperación física por silenciarlos de inmediato.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi irritación ante el ruido, pero me detuve antes de entrar a la sala.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad del conflicto, respiré hondo y relajé mis manos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré el ruido y el estrés corporal, manteniendo mi tono físico y voz neutros.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S11-Q2', 'Dos de tus hijos se gritan e insultan fuertemente en la sala por el uso de un espacio u objeto común.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S11', 'convivencia', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":2,"trigger_type":"Conflicto entre hermanos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La mediación neutral sin tomar partido (ACT 4-5) reduce la hostilidad a largo plazo (EFFECT 4-5) y modela la resolución de conflictos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Son insoportables, no pueden convivir un minuto en paz".', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Sé perfectamente quién empezó esto, siempre es el/la mismo/a".', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Tengo que intervenir rápido porque este nivel de ruido me altera".', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Están desregulados; necesito separarlos físicamente antes de resolver nada".', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Esta es una oportunidad para que aprendan a negociar bajo mi mediación neutral".', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S11-Q3', 'Dos de tus hijos se gritan e insultan fuertemente en la sala por el uso de un espacio u objeto común.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S11', 'convivencia', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":2,"trigger_type":"Conflicto entre hermanos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La mediación neutral sin tomar partido (ACT 4-5) reduce la hostilidad a largo plazo (EFFECT 4-5) y modela la resolución de conflictos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Entré gritando, los castigué a ambos y confisqué el objeto del conflicto.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tomé partido inmediatamente por el que consideré más débil y regañé al otro.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Les ordené callarse con tono seco y amenazante, ignorando el problema real.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Los separé físicamente y les pedí a cada uno que explicara su versión por turnos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Intervine para calmarlos, guié una conversación donde se escucharan y propuse buscar una solución justa.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S11-Q4', 'Dos de tus hijos se gritan e insultan fuertemente en la sala por el uso de un espacio u objeto común.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S11', 'convivencia', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":2,"trigger_type":"Conflicto entre hermanos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La mediación neutral sin tomar partido (ACT 4-5) reduce la hostilidad a largo plazo (EFFECT 4-5) y modela la resolución de conflictos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Ambos terminaron llorando, enojados conmigo y peleados entre sí.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se separaron molestos, dejándose de hablar y lanzándose miradas de odio.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hubo un silencio tenso; obedecieron pero el rencor se mantuvo latente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se resolvió el turno del objeto, aunque quedó cierta queja residual de insatisfacción.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Lograron compartir o turnarse el espacio/objeto en paz, disminuyendo la hostilidad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S11-Q5', 'Dos de tus hijos se gritan e insultan fuertemente en la sala por el uso de un espacio u objeto común.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S11', 'convivencia', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":2,"trigger_type":"Conflicto entre hermanos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La mediación neutral sin tomar partido (ACT 4-5) reduce la hostilidad a largo plazo (EFFECT 4-5) y modela la resolución de conflictos."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la rivalidad entre ellos se agravó por mi intervención punitiva.', 1, 1 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó la sensación de que siempre debo ser el juez que imponga castigos severos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la convivencia diaria se mantiene frágil y dependiente de mis órdenes.', 3, 3 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos establecer una regla de convivencia funcional para esa tarde.', 4, 4 FROM questions WHERE question_key = 'M-POC-S11-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que promoví el diálogo autónomo y fortalecí el respeto básico entre hermanos.', 5, 5 FROM questions WHERE question_key = 'M-POC-S11-Q5';

-- ==========================================
-- Escenario 12 (Intensidad: 4, Dominio: seguridad)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S12-Q1', 'Descubres que tu hijo/a adolescente asistió a una reunión peligrosa no autorizada o participó en un reto de riesgo por presión social.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S12', 'seguridad', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":4,"trigger_type":"Conducta de riesgo / Presión social","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar las conductas de riesgo desde el cuidado y la conexión (THINK 4-5) reduce la evasión y fomenta la protección mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una oleada de pánico frío, opresión en el pecho e impulsividad de castigo inmediato.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí mi corazón acelerado y un calor intenso de enfado mezclado con terror.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi angustia física severa, pero logré sentarme para estabilizarme.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el miedo, tomé aire profundamente y exhalé despacio para calmar el cuerpo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré el impacto del miedo por su seguridad, manteniendo mi postura física receptiva y firme.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S12-Q2', 'Descubres que tu hijo/a adolescente asistió a una reunión peligrosa no autorizada o participó en un reto de riesgo por presión social.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S12', 'seguridad', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":4,"trigger_type":"Conducta de riesgo / Presión social","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar las conductas de riesgo desde el cuidado y la conexión (THINK 4-5) reduce la evasión y fomenta la protección mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "No tiene cerebro, se quiere matar o arruinar la vida".', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un/a irresponsable desobediente; no saldrá de casa en meses".', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me aterra lo que pudo pasarle; necesito asegurarme de que entienda el peligro".', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "La presión social nubló su juicio; necesito hablar del autocuidado, no solo del castigo".', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Mi prioridad es su seguridad y que confíe en mí para pedir ayuda cuando esté en riesgo".', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S12-Q3', 'Descubres que tu hijo/a adolescente asistió a una reunión peligrosa no autorizada o participó en un reto de riesgo por presión social.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S12', 'seguridad', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":4,"trigger_type":"Conducta de riesgo / Presión social","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar las conductas de riesgo desde el cuidado y la conexión (THINK 4-5) reduce la evasión y fomenta la protección mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité con furia, le descalifiqué por su falta de juicio y le impuse un castigo indefinido.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le prohibí toda red social y contacto con sus amigos como sanción inmediata.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le di un sermón atemorizante sobre las consecuencias fatales de sus actos.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confronté seriamente con los hechos y le pedí que me explicara cómo evaluó el peligro.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Expresé mi alivio de que estuviera a salvo, hablé con firmeza del peligro real y acordamos pautas de autocuidado.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S12-Q4', 'Descubres que tu hijo/a adolescente asistió a una reunión peligrosa no autorizada o participó en un reto de riesgo por presión social.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S12', 'seguridad', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":4,"trigger_type":"Conducta de riesgo / Presión social","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar las conductas de riesgo desde el cuidado y la conexión (THINK 4-5) reduce la evasión y fomenta la protección mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se desató una crisis de llanto, gritos y distanciamiento defensivo absoluto.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Negó los hechos, se mostró desafiante y la tensión en la casa escaló severamente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Escuchó mi sermón con la cabeza baja y actitud completamente apática y ausente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Aceptó las reglas de seguridad acordadas, reconociendo parte de su error.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pudimos conversar sobre la presión social con honestidad, acordando medidas de seguridad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S12-Q5', 'Descubres que tu hijo/a adolescente asistió a una reunión peligrosa no autorizada o participó en un reto de riesgo por presión social.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S12', 'seguridad', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":4,"trigger_type":"Conducta de riesgo / Presión social","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Abordar las conductas de riesgo desde el cuidado y la conexión (THINK 4-5) reduce la evasión y fomenta la protección mutua (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que si vuelve a estar en peligro, me lo ocultará por miedo a mi reacción.', 1, 1 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una barrera de desconfianza severa que limita nuestra comunicación diaria.', 2, 2 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que el límite quedó impuesto a la fuerza, pero sin un aprendizaje real de autocuidado.', 3, 3 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos reafirmar las reglas de seguridad sin que se cortara el diálogo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S12-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que fortalecí el canal de confianza para que acuda a mí ante situaciones de riesgo.', 5, 5 FROM questions WHERE question_key = 'M-POC-S12-Q5';

-- ==========================================
-- Escenario 13 (Intensidad: 2, Dominio: conexion)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S13-Q1', 'Durante una cena familiar, tu hijo/a ignora repetidamente la conversación activa por estar inmerso/a en su teléfono.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S13', 'conexion', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":2,"trigger_type":"Presencia digital invasiva","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Establecer límites con conexión (ACT 4-5) modela la presencia consciente y la vinculación real en el hogar."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un pinchazo de irritación y un impulso brusco de arrebatarle el teléfono.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí fastidio en el cuello y ganas de hacer un comentario hiriente sobre su actitud.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi molestia ante la falta de conexión, pero mantuve mi mirada neutra.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la desconexión física, suspiré suavemente y me enfoqué en calmar mi tono.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la molestia y mantuve mi respiración pausada, preparándome para intervenir con respeto.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S13-Q2', 'Durante una cena familiar, tu hijo/a ignora repetidamente la conversación activa por estar inmerso/a en su teléfono.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S13', 'conexion', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":2,"trigger_type":"Presencia digital invasiva","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Establecer límites con conexión (ACT 4-5) modela la presencia consciente y la vinculación real en el hogar."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es una falta de respeto total, para eso mejor que coma solo/a".', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Está enganchado/a a esa basura, no le interesa convivir con nosotros".', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me molesta estar comiendo con una pantalla, pero no quiero empezar una pelea en la mesa".', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "El espacio familiar requiere presencia; recordaré el acuerdo común de forma tranquila".', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Utilizaré una invitación a la conversación antes de imponer la regla rígidamente".', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S13-Q3', 'Durante una cena familiar, tu hijo/a ignora repetidamente la conversación activa por estar inmerso/a en su teléfono.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S13', 'conexion', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":2,"trigger_type":"Presencia digital invasiva","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Establecer límites con conexión (ACT 4-5) modela la presencia consciente y la vinculación real en el hogar."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité, le quité el teléfono de la mesa con enojo y lo guardé bajo llave.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le hice una burla o comentario sarcástico frente a todos sobre su dependencia.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le exigí secamente que guardara el teléfono o se retirara de la mesa.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le recordé con calma el acuerdo familiar de "cero pantallas durante las comidas".', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le toqué suavemente el brazo, le pedí guardar el dispositivo y le integré activamente en la charla.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S13-Q4', 'Durante una cena familiar, tu hijo/a ignora repetidamente la conversación activa por estar inmerso/a en su teléfono.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S13', 'conexion', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":2,"trigger_type":"Presencia digital invasiva","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Establecer límites con conexión (ACT 4-5) modela la presencia consciente y la vinculación real en el hogar."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se levantó enojado/a de la mesa y la cena familiar se arruinó para todos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Guardó el teléfono con un gesto de desprecio y la cena continuó en completo silencio.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Guardó el aparato de mala gana, mostrando aburrimiento e ignorando la conversación.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Guardó el teléfono sin protestar y la dinámica de la cena continuó de forma regular.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se integró al diálogo de la mesa con naturalidad, bajando la tensión de manera fluida.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S13-Q5', 'Durante una cena familiar, tu hijo/a ignora repetidamente la conversación activa por estar inmerso/a en su teléfono.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S13', 'conexion', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":2,"trigger_type":"Presencia digital invasiva","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Establecer límites con conexión (ACT 4-5) modela la presencia consciente y la vinculación real en el hogar."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la mesa familiar se convirtió en un espacio de disputa y alejamiento.', 1, 1 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una tensión latente en la rutina de las comidas familiares.', 2, 2 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque guardó el teléfono, no logramos una verdadera conexión familiar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que mantuvimos un límite necesario para la convivencia del hogar.', 4, 4 FROM questions WHERE question_key = 'M-POC-S13-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que modelamos un espacio de respeto mutuo y presencia consciente sin fricciones.', 5, 5 FROM questions WHERE question_key = 'M-POC-S13-Q5';

-- ==========================================
-- Escenario 14 (Intensidad: 4, Dominio: respeto)
-- ==========================================
INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S14-Q1', 'Tu hijo/a le contesta con sarcasmo extremo o desprecio a otro miembro de la familia (como un abuelo/a o tío/a) en tu presencia.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S14', 'respeto', 'NOTICE', 'Observa qué ocurrió primero en tu cuerpo antes de actuar.', '{"scenario_intensity":4,"trigger_type":"Falta de respeto a terceros / Autoridad familiar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Modelar la regulación y la firmeza moral sin humillación (ACT 4-5) enseña empatía estructural hacia terceros (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una oleada instantánea de vergüenza y rabia que me tensó la mandíbula.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí calor en la cara y unas ganas inmensas de reprenderle duramente en público.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi enojo físico ante su desprecio, pero me obligué a pausar antes de hablar.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad de la falta de respeto, tomé aire y estabilicé mi tono de voz.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la tensión de la situación social, manteniendo mi postura erguida y mi respiración calmada.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q1';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S14-Q2', 'Tu hijo/a le contesta con sarcasmo extremo o desprecio a otro miembro de la familia (como un abuelo/a o tío/a) en tu presencia.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S14', 'respeto', 'THINK', 'Recuerda cuál fue el pensamiento predominante en ese momento.', '{"scenario_intensity":4,"trigger_type":"Falta de respeto a terceros / Autoridad familiar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Modelar la regulación y la firmeza moral sin humillación (ACT 4-5) enseña empatía estructural hacia terceros (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Qué vergüenza de hijo/a, me hace quedar en ridículo frente a todos".', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le voy a dar una lección ahora mismo para que aprenda a respetar".', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Esto es inaceptable, pero no quiero hacer un espectáculo público".', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "La agresión hacia un familiar requiere un límite claro y una reparación posterior".', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Modelaré la firmeza moral defendiendo al tercero sin caer en la misma agresión".', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q2';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S14-Q3', 'Tu hijo/a le contesta con sarcasmo extremo o desprecio a otro miembro de la familia (como un abuelo/a o tío/a) en tu presencia.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S14', 'respeto', 'ACT', 'Elige la conducta que más se pareció a tu reacción inicial.', '{"scenario_intensity":4,"trigger_type":"Falta de respeto a terceros / Autoridad familiar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Modelar la regulación y la firmeza moral sin humillación (ACT 4-5) enseña empatía estructural hacia terceros (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité enfurecido/a, exigiéndole que se disculpara de rodillas ante el familiar.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le humillé públicamente mencionando sus propios defectos frente a la familia.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le mandé a callar con tono autoritario y le ordené retirarse inmediatamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Intervine con firmeza diciendo: "Ese tono no es aceptable en esta casa", pidiendo respeto.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pausé la interacción, señalé con calma la falta de respeto y solicité reparar el comentario.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q3';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S14-Q4', 'Tu hijo/a le contesta con sarcasmo extremo o desprecio a otro miembro de la familia (como un abuelo/a o tío/a) en tu presencia.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S14', 'respeto', 'AFTERMATH', 'Describe cómo evolucionó la interacción durante los minutos siguientes.', '{"scenario_intensity":4,"trigger_type":"Falta de respeto a terceros / Autoridad familiar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Modelar la regulación y la firmeza moral sin humillación (ACT 4-5) enseña empatía estructural hacia terceros (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se generó una gran discusión familiar, con llantos y reproches de todos los presentes.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se encerró furioso/a, dejando una atmósfera de incomodidad social intolerable.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pidió una disculpa forzada y falsa, y el resto de la reunión transcurrió de forma tensa.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El ambiente social se estabilizó y pudimos continuar la reunión de forma más calmada.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se aclaró la situación en el momento, el familiar aceptó la reparación y el clima social se distendió.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q4';

INSERT IGNORE INTO questions (question_key, text, type, dimension, area, active, severity_weight, detects_relapse, parent_key, pillar, phase, phase_prompt, metadata) 
VALUES ('M-POC-S14-Q5', 'Tu hijo/a le contesta con sarcasmo extremo o desprecio a otro miembro de la familia (como un abuelo/a o tío/a) en tu presencia.', 'SCENARIO_V1_2', 'Comportamiento', 'Familia', 0, 1, 0, 'M-POC-S14', 'respeto', 'EFFECT', 'Piensa cómo quedó la relación después de que terminó la situación.', '{"scenario_intensity":4,"trigger_type":"Falta de respeto a terceros / Autoridad familiar","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Modelar la regulación y la firmeza moral sin humillación (ACT 4-5) enseña empatía estructural hacia terceros (EFFECT 4-5)."}');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la relación familiar extensa se dañó y que reina la hostilidad social.', 1, 1 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una profunda incomodidad y resentimiento en la familia extensa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que se impuso una disculpa cosmética que no generó empatía real.', 3, 3 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos trazar una línea clara sobre el trato a los adultos de la familia.', 4, 4 FROM questions WHERE question_key = 'M-POC-S14-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que modelé con éxito el cuidado relacional y el respeto mutuo en el hogar.', 5, 5 FROM questions WHERE question_key = 'M-POC-S14-Q5';

