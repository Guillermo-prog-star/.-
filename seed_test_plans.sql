-- SCRIPT DE SEMBRADO PARA PRUEBAS DEL MOTOR DE EVIDENCIAS Y PLANES DE ACCIÓN (UNIFICADO)
USE integrity_family;

-- Desactivar temporalmente restricciones de clave foránea para la reconstrucción limpia
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Asegurar la existencia de hitos de evolución temporal (milestone_definitions)
INSERT INTO milestone_definitions (code, label, title, duration_days, order_index, description)
VALUES 
('W1', 'Semana 1', 'Despegue de la Alianza Familiar', 7, 1, 'Fase inicial de aclimatación familiar, sintonía de rutinas básicas y pactos iniciales sin pantallas.'),
('M1', 'Mes 1', 'Cimiento y Contención Emocional', 30, 2, 'Primeros 30 días enfocados en la asimilación del respeto mutuo, reconocimiento activo y hábitos de mesa.'),
('M3', 'Mes 3', 'Consolidación del Hábito Colaborativo', 90, 3, 'Evaluación trimestral de acuerdos, distribución equitativa de roles y consolidación de la bitácora.')
ON DUPLICATE KEY UPDATE label=VALUES(label), title=VALUES(title), duration_days=VALUES(duration_days), order_index=VALUES(order_index);

-- 2. Asegurar que existe una única familia base: Familia Lopez Rivera con código oficial
-- Se usa el ID 1 para unificación absoluta del ecosistema de base de datos
INSERT INTO families (id, name, family_code, pin, current_milestone, icf_score, total_tasks, completed_tasks, sentinel_active, created_at, municipio)
VALUES 
(1, 'Familia Lopez Rivera', 'IF-CO-QUI-2026-0004', '1234', 'M1', 75, 12, 8, true, NOW(), 'Armenia')
ON DUPLICATE KEY UPDATE name=VALUES(name), family_code=VALUES(family_code), pin=VALUES(pin), current_milestone=VALUES(current_milestone), sentinel_active=VALUES(sentinel_active), municipio=VALUES(municipio);

-- 3. Asegurar evaluaciones base finalizadas para la familia
INSERT INTO evaluations (id, family_id, status, started_at, finalized_at, has_crisis, icf, milestone_key, spiritual_synthesis)
VALUES
(1, 1, 'FINALIZED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR), false, 75.0, 'M1', 'Síntesis espiritual inicial de la Familia Lopez Rivera.')
ON DUPLICATE KEY UPDATE status=VALUES(status), finalized_at=VALUES(finalized_at);

-- 4. Crear plan de mejora para la familia
INSERT INTO plans (id, family_id, evaluation_id, title, description, created_at, ai_report)
VALUES
(1, 1, 1, 'PLAN DE TRANSFORMACIÓN: Familia López Rivera', 'Ruta clínica personalizada de 36 meses enfocada en el fortalecimiento de la alianza familiar, asimilación de virtudes y límites conductuales.', NOW(), '{"sintesis": "Ok"}')
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description);

-- 5. Limpiar tareas previas del plan
DELETE FROM plan_tasks WHERE plan_id = 1;

-- 6. Inyectar tareas interactivas para el Plan 1 (Familia Lopez Rivera ID 1)
INSERT INTO plan_tasks (id, plan_id, title, description, completed, created_at, due_date, dimension, periodicity_months, milestone_id, fase, riesgo_asociado, objetivo, accion_concreta, indicador_cumplimiento, evidencia_requerida, impacto_icf)
SELECT 
1, 
1, 
'Fomentar diálogo semanal sin dispositivos', 
'Establecer una cena familiar semanal donde el uso de teléfonos y pantallas esté prohibido por mutuo acuerdo para favorecer la escucha activa.', 
false, 
NOW(), 
DATE_ADD(NOW(), INTERVAL 7 DAY), 
'COMUNICACION', 
1, 
md.id, 
'EJECUCION', 
'BAJO', 
'Desconectar la tecnología para reconectar emocionalmente.', 
'Implementar una caja decorada en la mesa donde todos los miembros depositen sus dispositivos móviles antes de cenar.', 
'Realizar 4 cenas consecutivas sin interrupciones digitales.', 
'Reflexión profunda en el portal detallando la asimilación de la dinámica o una foto de la caja familiar de dispositivos.', 
15
FROM milestone_definitions md WHERE md.code = 'M1';

INSERT INTO plan_tasks (id, plan_id, title, description, completed, created_at, due_date, dimension, periodicity_months, milestone_id, fase, riesgo_asociado, objetivo, accion_concreta, indicador_cumplimiento, evidencia_requerida, impacto_icf)
SELECT 
2, 
1, 
'Práctica diaria de gratitud y reconocimiento', 
'Espacio diario nocturno para que cada integrante reconozca el valor y agradezca una acción específica realizada por otro miembro familiar.', 
false, 
NOW(), 
DATE_ADD(NOW(), INTERVAL 14 DAY), 
'EMOCIONES', 
1, 
md.id, 
'ESTABILIZACION', 
'MEDIO', 
'Fomentar un clima de validación y afecto sincero en el hogar.', 
'Dedicar 10 minutos al finalizar el día para la dinámica grupal de agradecimiento verbal.', 
'Sostener la práctica al menos 5 días en la semana escolar.', 
'Escribir una bitácora detallando las emociones identificadas y cómo reaccionaron los hijos ante el reconocimiento.', 
20
FROM milestone_definitions md WHERE md.code = 'M1';

INSERT INTO plan_tasks (id, plan_id, title, description, completed, created_at, due_date, dimension, periodicity_months, milestone_id, fase, riesgo_asociado, objetivo, accion_concreta, indicador_cumplimiento, evidencia_requerida, impacto_icf)
SELECT 
3, 
1, 
'Acuerdo de distribución equitativa de responsabilidades', 
'Discutir, consensuar y diagramar la asignación de las tareas domésticas y cuidado colaborativo dentro del hogar.', 
false, 
NOW(), 
DATE_ADD(NOW(), INTERVAL 21 DAY), 
'HABITOS', 
3, 
md.id, 
'EVALUACION', 
'BAJO', 
'Disminuir el estrés parental mediante corresponsabilidad equitativa.', 
'Elaborar un cartel o rotativo visual pegado en un área común con los roles firmados por todos.', 
'Distribución firmada y ejecución de tareas durante la primera semana piloto.', 
'Adjuntar foto del cartel y el acta redactada del compromiso familiar.', 
10
FROM milestone_definitions md WHERE md.code = 'M3';

-- 7. Vincular los pasos de ejecución (plan_task_steps) para habilitar el pipeline visual
DELETE FROM plan_task_steps WHERE task_id IN (1, 2, 3);

INSERT INTO plan_task_steps (task_id, type, detail, completed) VALUES
(1, 'PLANIFICAR', 'Decorar en familia la caja recolectora de celulares y pactar el día del diálogo.', true),
(1, 'EJECUTAR', 'Depositar los teléfonos antes de cenar y dialogar de forma ininterrumpida.', false),
(1, 'EVALUAR', 'Aportar la reflexión escrita del diálogo familiar para validar el hito.', false),

(2, 'PLANIFICAR', 'Acordar la hora fija nocturna (ej: 8:00 PM) para reunir a la familia.', true),
(2, 'EJECUTAR', 'Completar las dinámicas verbales de gratitud diaria de lunes a viernes.', false),
(2, 'EVALUAR', 'Someter la bitácora conductual a Sentinel AI para calificar la asimilación emocional.', false),

(3, 'PLANIFICAR', 'Hacer una lluvia de ideas de todas las tareas del hogar necesarias.', true),
(3, 'EJECUTAR', 'Diagramar y consensuar la cartografía de responsabilidades domésticas.', false),
(3, 'EVALUAR', 'Subir la captura del cartel firmado y consolidar el progreso.', false);

-- 8. Inyectar ítems de hábitos/checklist activos para la familia 1 (Lopez Rivera)
DELETE FROM checklist_items WHERE family_id = 1;

INSERT INTO checklist_items (family_id, description, completed, completed_by, created_at, category, source, dimension)
VALUES
(1, 'Cena familiar del viernes sin teléfonos móviles en la mesa', false, NULL, NOW(), 'comunicacion', 'PLAN', 'comunicacion'),
(1, 'Compartir un agradecimiento sincero y verbal con un miembro antes de dormir', true, 'William Lopez', DATE_SUB(NOW(), INTERVAL 1 DAY), 'emociones', 'PLAN', 'emociones'),
(1, 'Actualizar la bitácora física de acuerdos y responsabilidades', false, NULL, NOW(), 'habitos', 'PLAN', 'habitos'),
(1, 'Dedicar 15 minutos de juego individual con cada hijo sin distractores', false, NULL, NOW(), 'tiempos', 'PLAN', 'tiempos');

-- 9. Inyectar registros inmutables de la bitácora familiar (RESOLVED) para la familia 1 (Lopez Rivera)
DELETE FROM family_logbook_entries WHERE family_id = 1;

INSERT INTO family_logbook_entries (id, family_id, situation, difficulty_detected, emotion_identified, understanding, correction_action, family_agreement, progress_evidence, status, created_by, resolved_by, created_at, resolved_at)
VALUES
(1, 1, 'Cena familiar de fin de semana con discusiones recurrentes por el uso de pantallas.', 'Desconexión emocional y respuestas defensivas al pedir dejar los teléfonos.', 'irritabilidad', 'Entendimos que el celular era un refugio individual que fracturaba el momento grupal.', 'Establecer la caja de celulares y un moderador rotativo para temas de conversación.', 'Depositar celulares al entrar al comedor y quien use el teléfono lava los platos.', 'La cena de ayer fluyó con risas y anécdotas de la infancia de los niños. Adjuntamos foto de la caja pintada por todos.', 'RESOLVED', 'William Lopez', 'SENTINEL_AUTO_AI', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 10. Inyectar victorias de misiones validadas por Sentinel AI (VALIDATED) para la familia 1 (Lopez Rivera)
DELETE FROM task_evidences WHERE family_id = 1;

INSERT INTO task_evidences (id, task_id, family_id, evidence_type, status, title, description, file_url, text_content, submitted_by, ai_score, human_score, validated, created_at, validated_at)
VALUES
(1, 1, 1, 'TEXT', 'VALIDATED', 'Cena del Silencio Digital Completada', 'El análisis cognitivo de Sentinel AI valida un alto índice de sintonía emocional y coherencia conductual en el núcleo familiar. Se observa una asimilación genuina del límite tecnológico sin reactividad. Sello de Integridad Familiar otorgado.', NULL, 'Ayer viernes hicimos la cena sin pantallas. Al principio los niños estaban un poco ansiosos, pero después sacamos un juego de preguntas familiares y fue muy especial conocer sus respuestas.', 'William Lopez', 95.0, 95.0, true, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 1, 'TEXT', 'VALIDATED', 'Bitácora del Reconocimiento Activo', 'Sentinel AI detecta una transición saludable de la fase de reactividad a la asimilación del afecto verbal. La práctica de gratitud nocturna actúa como un contenedor emocional de alto valor clínico.', NULL, 'Completamos la semana de gratitud nocturna. Los niños ahora esperan ansiosos el momento de darnos las gracias mutuamente antes de acostarse.', 'William Lopez', 88.0, 88.0, true, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Reactivar restricciones de clave foránea
SET FOREIGN_KEY_CHECKS = 1;
