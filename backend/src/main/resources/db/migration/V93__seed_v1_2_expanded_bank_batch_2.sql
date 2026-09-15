-- V93__seed_v1_2_expanded_bank_batch_2.sql
-- Batch 2 of expanded scenarios based on Gold Standard Methodological Contract
-- Adds Scenarios 5, 6, 7, 8, 9 with active = 0 (REVIEW)

-- ==========================================
-- Escenario 5 (Intensidad: 3, Dominio: normas)
-- ==========================================
UPDATE questions SET
  text = 'Se acaba el tiempo límite de pantallas acordado y tu hijo/a se niega rotundamente a apagar el dispositivo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'normas',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Uso excesivo de pantallas","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva en la regulación del límite (ACT 4-5) previene rupturas del vínculo a largo plazo (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S5-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S5-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la sangre subir a mi cabeza y una tensión agresiva en mis manos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí frustración física inmediata y un impulso por arrancar el dispositivo de sus manos.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi enojo corporal, pero me contuve de hacer un movimiento brusco.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la molestia, respiré profundo y me acerqué físicamente con calma.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la tensión, mantuve mis hombros relajados y mi postura no amenazante.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q1';

UPDATE questions SET
  text = 'Se acaba el tiempo límite de pantallas acordado y tu hijo/a se niega rotundamente a apagar el dispositivo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'normas',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Uso excesivo de pantallas","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva en la regulación del límite (ACT 4-5) previene rupturas del vínculo a largo plazo (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S5-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S5-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me está tomando el pelo, le voy a quitar esto para siempre".', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un adicto/a a esa pantalla, no respeta ninguna regla".', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Esto me enfurece, pero sé que quitarlo a la fuerza empeorará todo".', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es difícil para él/ella desconectarse; necesito reafirmar el límite con firmeza".', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Validaré su frustración por terminar su actividad, pero el límite es innegociable".', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q2';

UPDATE questions SET
  text = 'Se acaba el tiempo límite de pantallas acordado y tu hijo/a se niega rotundamente a apagar el dispositivo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'normas',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Uso excesivo de pantallas","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva en la regulación del límite (ACT 4-5) previene rupturas del vínculo a largo plazo (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S5-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S5-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité y le arrebaté el dispositivo de las manos violentamente.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le amenacé con quitarle el dispositivo por una semana si no lo apagaba ya.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le exigí que lo apagara con tono duro y me quedé de pie mirándolo/a fijamente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me senté a su lado, reconocí que era difícil y le di un minuto final para apagarlo.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Reafirmé el acuerdo original con calma y le ofrecí ayuda para guardar el progreso de su actividad.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q3';

UPDATE questions SET
  text = 'Se acaba el tiempo límite de pantallas acordado y tu hijo/a se niega rotundamente a apagar el dispositivo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'normas',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Uso excesivo de pantallas","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva en la regulación del límite (ACT 4-5) previene rupturas del vínculo a largo plazo (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S5-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S5-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tuvimos una pelea a gritos, portazos y no nos hablamos el resto de la tarde.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me fui enojado/a y quedó un clima de hostilidad muy pesado.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hubo frialdad; logré que apagara la pantalla pero quedó mucho resentimiento.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Apagó el dispositivo con algo de protesta, pero luego el ambiente se fue relajando.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Cerramos el momento de transición sin que escalara a una crisis emocional.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q4';

UPDATE questions SET
  text = 'Se acaba el tiempo límite de pantallas acordado y tu hijo/a se niega rotundamente a apagar el dispositivo.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'normas',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Uso excesivo de pantallas","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La asertividad no reactiva en la regulación del límite (ACT 4-5) previene rupturas del vínculo a largo plazo (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S5-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S5-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la regla de pantallas se convirtió en una guerra constante que nos destruye.', 1, 1 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó el temor de que cada vez que toque el tema habrá una confrontación severa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque se cumplió la regla, perdimos conexión emocional.', 3, 3 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que mantuvimos el límite de manera firme sin destruir nuestra comunicación.', 4, 4 FROM questions WHERE question_key = 'M-POC-S5-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logramos navegar una transición difícil de forma colaborativa y respetuosa.', 5, 5 FROM questions WHERE question_key = 'M-POC-S5-Q5';

-- ==========================================
-- Escenario 6 (Intensidad: 2, Dominio: conexion)
-- ==========================================
UPDATE questions SET
  text = 'Has planeado una actividad familiar y tu hijo/a decide encerrarse en su habitación sin querer participar.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'conexion',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Aislamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Evitar la personalización (THINK 4-5) facilita una respuesta empática que protege la relación estructural (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S6-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S6-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un rechazo físico profundo y un nudo de tristeza e ira en el estómago.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la cara caliente de decepción y un impulso por ir a sacarlo/a de ahí.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi dolor corporal ante el rechazo, e intenté disimularlo.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad, solté un suspiro y reduje la tensión de mi mandíbula.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré mi propia decepción, permitiendo que mi cuerpo se mantuviera relajado.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q1';

UPDATE questions SET
  text = 'Has planeado una actividad familiar y tu hijo/a decide encerrarse en su habitación sin querer participar.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'conexion',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Aislamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Evitar la personalización (THINK 4-5) facilita una respuesta empática que protege la relación estructural (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S6-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S6-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un malagradecido/a, todo el esfuerzo que hago no le importa nada".', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Ya no quiere ser parte de esta familia, solo le importa su mundo".', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me duele que no quiera estar con nosotros, no sé qué estoy haciendo mal".', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Probablemente necesita espacio, aunque yo quería que estuviéramos juntos".', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Respetaré su necesidad de aislamiento temporal sin tomarlo como un ataque personal".', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q2';

UPDATE questions SET
  text = 'Has planeado una actividad familiar y tu hijo/a decide encerrarse en su habitación sin querer participar.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'conexion',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Aislamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Evitar la personalización (THINK 4-5) facilita una respuesta empática que protege la relación estructural (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S6-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S6-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Abrí la puerta de golpe y le obligué a salir usando chantaje emocional.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le hice reclamos desde afuera de su cuarto sobre lo egoísta que era.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije con tono de dolor fingido que hiciera lo que quisiera y me alejé.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Toqué su puerta, le dije que lo extrañaríamos y dejé la invitación abierta.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le hice saber que respetaba su espacio y que disfrutaríamos la actividad pensando en él/ella.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q3';

UPDATE questions SET
  text = 'Has planeado una actividad familiar y tu hijo/a decide encerrarse en su habitación sin querer participar.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'conexion',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Aislamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Evitar la personalización (THINK 4-5) facilita una respuesta empática que protege la relación estructural (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S6-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S6-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El resto de la familia terminó peleando por el ambiente de tensión que generé.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hicimos la actividad con evidente mal humor y quejándonos de su ausencia.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La actividad fue algo desganada y hubo comentarios pasivo-agresivos.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Disfrutamos la actividad familiar respetando el espacio del que no participó.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'El ambiente familiar se mantuvo positivo y el que se quedó en su cuarto eventualmente se acercó.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q4';

UPDATE questions SET
  text = 'Has planeado una actividad familiar y tu hijo/a decide encerrarse en su habitación sin querer participar.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'conexion',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":2,"trigger_type":"Aislamiento","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Evitar la personalización (THINK 4-5) facilita una respuesta empática que protege la relación estructural (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S6-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S6-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la distancia entre nosotros se hizo más grande e irreparable.', 1, 1 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una barrera de incomprensión y dolor que dificultó el trato al día siguiente.', 2, 2 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí cierta resignación sobre su etapa, manteniendo una distancia prudente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que al no forzar la conexión, evitamos un conflicto mayor.', 4, 4 FROM questions WHERE question_key = 'M-POC-S6-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que respetar sus ritmos afianzó la confianza básica entre nosotros.', 5, 5 FROM questions WHERE question_key = 'M-POC-S6-Q5';

-- ==========================================
-- Escenario 7 (Intensidad: 5, Dominio: confianza)
-- ==========================================
UPDATE questions SET
  text = 'Descubres evidencia irrefutable de que te ha mentido sobre algo importante que afecta su seguridad.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'confianza',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":5,"trigger_type":"Descubrimiento de mentira/engaño","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La regulación emocional alta (NOTICE y THINK 4-5) ante una ruptura grave previene la reactividad destructiva (ACT 1-2)."}'
WHERE question_key = 'M-POC-S7-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S7-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una taquicardia violenta, mareo y un impulso ciego de confrontación inmediata.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un frío repentino en el cuerpo seguido de un calor intenso de traición.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi respiración entrecortada e intenté alejarme físicamente de la evidencia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un golpe de adrenalina, cerré los ojos y forcé mi respiración a estabilizarse.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré el impacto del miedo y la traición, anclando mis pies al suelo para no desbordarme.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q1';

UPDATE questions SET
  text = 'Descubres evidencia irrefutable de que te ha mentido sobre algo importante que afecta su seguridad.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'confianza',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":5,"trigger_type":"Descubrimiento de mentira/engaño","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La regulación emocional alta (NOTICE y THINK 4-5) ante una ruptura grave previene la reactividad destructiva (ACT 1-2)."}'
WHERE question_key = 'M-POC-S7-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S7-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Es un mentiroso/a manipulador/a, no puedo creer en nada de lo que dice".', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Todo mi trabajo como padre/madre fue basura, esto es el límite".', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Tengo mucho miedo y rabia, necesito descubrir por qué lo hizo".', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Esto es grave; necesito calmarme totalmente antes de hablar con él/ella".', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "La mentira es una estrategia para evitar algo; debo abordar el problema raíz, no solo la mentira".', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q2';

UPDATE questions SET
  text = 'Descubres evidencia irrefutable de que te ha mentido sobre algo importante que afecta su seguridad.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'confianza',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":5,"trigger_type":"Descubrimiento de mentira/engaño","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La regulación emocional alta (NOTICE y THINK 4-5) ante una ruptura grave previene la reactividad destructiva (ACT 1-2)."}'
WHERE question_key = 'M-POC-S7-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S7-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Fui a buscarlo/a gritando, le mostré la evidencia y le dije que ya no confiaba en él/ella.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le apliqué un castigo severo inmediato sin darle oportunidad de hablar.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confronté con dureza, llorando de decepción e interrogándolo/a implacablemente.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le mostré la evidencia con firmeza y le pedí que me explicara la situación.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Inicié una conversación seria expresando mi preocupación por su seguridad y luego presenté lo que descubrí.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q3';

UPDATE questions SET
  text = 'Descubres evidencia irrefutable de que te ha mentido sobre algo importante que afecta su seguridad.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'confianza',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":5,"trigger_type":"Descubrimiento de mentira/engaño","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La regulación emocional alta (NOTICE y THINK 4-5) ante una ruptura grave previene la reactividad destructiva (ACT 1-2)."}'
WHERE question_key = 'M-POC-S7-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S7-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La relación se quebró; hubo insultos, llanto descontrolado y encierro.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Se instauró un silencio glacial y un régimen de hipervigilancia en la casa.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Terminamos en una conversación circular llena de reclamos mutuos.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Tuvimos una discusión difícil, pero establecimos consecuencias claras sobre la mentira.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Atravesamos una conversación dolorosa pero honesta, donde ambos asumimos nuestra parte.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q4';

UPDATE questions SET
  text = 'Descubres evidencia irrefutable de que te ha mentido sobre algo importante que afecta su seguridad.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'confianza',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":5,"trigger_type":"Descubrimiento de mentira/engaño","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"La regulación emocional alta (NOTICE y THINK 4-5) ante una ruptura grave previene la reactividad destructiva (ACT 1-2)."}'
WHERE question_key = 'M-POC-S7-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S7-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la confianza se destruyó para siempre y que somos enemigos viviendo juntos.', 1, 1 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó un ambiente de sospecha constante, revisando todo lo que hace.', 2, 2 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que perdimos mucha cercanía, interactuando de forma meramente transaccional.', 3, 3 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque la confianza tomará tiempo en sanar, las reglas quedaron claras.', 4, 4 FROM questions WHERE question_key = 'M-POC-S7-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la crisis, aunque grave, se manejó de forma que permitirá reconstruir la confianza paso a paso.', 5, 5 FROM questions WHERE question_key = 'M-POC-S7-Q5';

-- ==========================================
-- Escenario 8 (Intensidad: 3, Dominio: responsabilidad)
-- ==========================================
UPDATE questions SET
  text = 'Descubres que ha gastado una suma de dinero sin permiso o perdido un objeto valioso por descuido irresponsable.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'responsabilidad',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Manejo financiero/recursos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Enfocarse en la reparación en lugar del castigo punitivo (ACT 4-5) facilita la asimilación del aprendizaje (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S8-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S8-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un espasmo en el estómago y una furia inmediata que me hizo apretar los puños.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un calor que subía por mi pecho y un impulso urgente de recriminar.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi mandíbula tensa y me froté la cara tratando de procesar la molestia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí la incomodidad de la pérdida, tomé aire profundo y solté la tensión de los hombros.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré mi frustración financiera pero relajé conscientemente mi postura para no atacar.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q1';

UPDATE questions SET
  text = 'Descubres que ha gastado una suma de dinero sin permiso o perdido un objeto valioso por descuido irresponsable.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'responsabilidad',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Manejo financiero/recursos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Enfocarse en la reparación en lugar del castigo punitivo (ACT 4-5) facilita la asimilación del aprendizaje (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S8-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S8-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "No valora el esfuerzo que hacemos, es un/a irresponsable absoluto/a".', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Tengo que hacerle pagar por esto de inmediato para que aprenda".', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me duele el desperdicio, pero no quiero arruinar el día por cosas materiales".', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Hubo un fallo de criterio claro; tenemos que hablar sobre cómo va a reponer esto".', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Este es un momento formativo sobre el valor de las cosas y la reparación de daños".', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q2';

UPDATE questions SET
  text = 'Descubres que ha gastado una suma de dinero sin permiso o perdido un objeto valioso por descuido irresponsable.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'responsabilidad',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Manejo financiero/recursos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Enfocarse en la reparación en lugar del castigo punitivo (ACT 4-5) facilita la asimilación del aprendizaje (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S8-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S8-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité acusándolo/a de ladrón/a o descuidado/a, haciéndole sentir máxima culpa.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le confisqué sus cosas de valor inmediatamente como represalia desproporcionada.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le reclamé con sarcasmo sobre lo fácil que es gastar o perder lo que no cuesta.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le expliqué firmemente la gravedad del hecho y anuncié que habría consecuencias.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Abordé el hecho objetivamente y le pregunté cuál era su plan para compensar la pérdida.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q3';

UPDATE questions SET
  text = 'Descubres que ha gastado una suma de dinero sin permiso o perdido un objeto valioso por descuido irresponsable.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'responsabilidad',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Manejo financiero/recursos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Enfocarse en la reparación en lugar del castigo punitivo (ACT 4-5) facilita la asimilación del aprendizaje (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S8-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S8-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La situación terminó en una fuerte agresión verbal cruzada y aislamiento.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Me retiré molesto/a, dejándolo/a con una sensación de vergüenza tóxica.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Discutimos un rato y el tema quedó flotando con reproches intermitentes.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Establecimos un plan de reparación de los daños de forma seria pero sin gritos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Llegamos a un acuerdo claro sobre cómo resarcir la pérdida asumiendo consecuencias lógicas.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q4';

UPDATE questions SET
  text = 'Descubres que ha gastado una suma de dinero sin permiso o perdido un objeto valioso por descuido irresponsable.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'responsabilidad',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":3,"trigger_type":"Manejo financiero/recursos","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"Enfocarse en la reparación en lugar del castigo punitivo (ACT 4-5) facilita la asimilación del aprendizaje (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S8-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S8-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que este evento confirmó que no puedo confiarle absolutamente nada.', 1, 1 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una barrera de resentimiento sobre temas de dinero y pertenencias.', 2, 2 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que el problema material erosionó innecesariamente nuestro vínculo emocional.', 3, 3 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que, aunque el error existió, logramos establecer límites claros sobre los recursos.', 4, 4 FROM questions WHERE question_key = 'M-POC-S8-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la situación se convirtió en un aprendizaje valioso de responsabilidad sin dañar nuestra relación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S8-Q5';

-- ==========================================
-- Escenario 9 (Intensidad: 4, Dominio: respeto)
-- ==========================================
UPDATE questions SET
  text = 'En medio de un desacuerdo menor, te responde con una grosería, insulto directo o un portazo en la cara.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'respeto',
  phase = 'NOTICE',
  phase_prompt = 'Observa qué ocurrió primero en tu cuerpo antes de actuar.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Tono irrespetuoso/Insulto","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No escalar la agresión (NOTICE y THINK 4-5) permite establecer un límite efectivo sin dañar la seguridad vincular (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S9-Q1';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S9-Q1');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una explosión de ira en mi pecho, como si me hubieran dado una bofetada física.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí un bloqueo respiratorio y un impulso incontrolable de devolver la agresión.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Noté mi indignación física profunda y traté de contenerme mordiéndome el labio.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí el golpe de la falta de respeto, tomé distancia física y forcé una exhalación.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q1';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Registré la agresividad de sus palabras, manteniendo mis pies en la tierra y mi respiración estable.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q1';

UPDATE questions SET
  text = 'En medio de un desacuerdo menor, te responde con una grosería, insulto directo o un portazo en la cara.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'respeto',
  phase = 'THINK',
  phase_prompt = 'Recuerda cuál fue el pensamiento predominante en ese momento.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Tono irrespetuoso/Insulto","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No escalar la agresión (NOTICE y THINK 4-5) permite establecer un límite efectivo sin dañar la seguridad vincular (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S9-Q2';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S9-Q2');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "A mí nadie me falta al respeto así, y mucho menos en mi propia casa".', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Le voy a enseñar a respetarme a las malas si es necesario".', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Me ofendió terriblemente, ¿qué hice para criar a alguien así?".', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Está totalmente desregulado/a, no puedo tomarme su insulto como una verdad personal".', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q2';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pensé: "Su falta de respeto requiere un límite firme, pero debo modelar el autocontrol que le exijo".', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q2';

UPDATE questions SET
  text = 'En medio de un desacuerdo menor, te responde con una grosería, insulto directo o un portazo en la cara.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'respeto',
  phase = 'ACT',
  phase_prompt = 'Elige la conducta que más se pareció a tu reacción inicial.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Tono irrespetuoso/Insulto","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No escalar la agresión (NOTICE y THINK 4-5) permite establecer un límite efectivo sin dañar la seguridad vincular (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S9-Q3';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S9-Q3');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Respondí inmediatamente con un insulto mayor o una agresión física/verbal intensa.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le amenacé a gritos y exigí disculpas inmediatas invadiendo su espacio personal.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le grité que se callara y lo/a mandé a su habitación con visible furia.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Le dije con tono severo y controlado: "No permito que me hables de esa manera", y salí.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q3';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Pausé, lo/a miré a los ojos y con voz firme dije: "Esa forma de hablar lastima, hablaremos cuando te calmes".', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q3';

UPDATE questions SET
  text = 'En medio de un desacuerdo menor, te responde con una grosería, insulto directo o un portazo en la cara.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'respeto',
  phase = 'AFTERMATH',
  phase_prompt = 'Describe cómo evolucionó la interacción durante los minutos siguientes.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Tono irrespetuoso/Insulto","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No escalar la agresión (NOTICE y THINK 4-5) permite establecer un límite efectivo sin dañar la seguridad vincular (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S9-Q4';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S9-Q4');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'La escalada de gritos continuó hasta que la situación se salió totalmente de control.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Nos encerramos en espacios distintos, acumulando un rencor profundo y silencioso.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Hubo un periodo largo de frialdad y evitación mutua por el resto del día.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Retomamos la conversación horas más tarde; exigí respeto y se disculpó a regañadientes.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q4';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Una vez calmados, hablamos sobre el evento; reconoció su error y aclaramos los límites del respeto.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q4';

UPDATE questions SET
  text = 'En medio de un desacuerdo menor, te responde con una grosería, insulto directo o un portazo en la cara.',
  type = 'SCENARIO_V1_2',
  dimension = 'Comportamiento',
  area = 'Familia',
  active = 0,
  severity_weight = 1,
  detects_relapse = 0,
  pillar = 'respeto',
  phase = 'EFFECT',
  phase_prompt = 'Piensa cómo quedó la relación después de que terminó la situación.',
  metadata = '{"scenario_intensity":4,"trigger_type":"Tono irrespetuoso/Insulto","expected_age_range":"General","family_role":"Cualquiera","validation_status":"REVIEW","pilot_version":"1.2.1","clinical_hypothesis":"No escalar la agresión (NOTICE y THINK 4-5) permite establecer un límite efectivo sin dañar la seguridad vincular (EFFECT 4-5)."}'
WHERE question_key = 'M-POC-S9-Q5';
DELETE FROM question_options WHERE question_id = (SELECT id FROM questions WHERE question_key = 'M-POC-S9-Q5');
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que la línea de respeto se quebró irreversiblemente y la hostilidad es la norma.', 1, 1 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Quedó una atmósfera de miedo y desconexión muy difícil de cruzar.', 2, 2 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí una herida emocional latente que interfirió en nuestras interacciones posteriores.', 3, 3 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que logré sostener mi autoridad sin convertirme en un agresor/a yo también.', 4, 4 FROM questions WHERE question_key = 'M-POC-S9-Q5';
INSERT INTO question_options (question_id, text, score_value, rubric_level)
SELECT id, 'Sentí que proteger mi propio respeto fortaleció la relación estructural al modelar la regulación.', 5, 5 FROM questions WHERE question_key = 'M-POC-S9-Q5';

