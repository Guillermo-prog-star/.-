-- V59: Eliminar planes existentes para regeneración con el nuevo prompt (4 misiones por pilar)
-- Los planes actuales fueron generados con el prompt antiguo (6-12 misiones).
-- Al borrarlos, la familia podrá generar un plan nuevo con exactamente 4 misiones por pilar.
-- Se eliminan en orden correcto respetando las foreign keys.

SET FOREIGN_KEY_CHECKS = 0;

-- Borrar evidencias de tareas
DELETE FROM task_evidences WHERE task_id IN (
    SELECT pt.id FROM plan_tasks pt
    JOIN plans p ON pt.plan_id = p.id
);

-- Borrar tareas del plan
DELETE FROM plan_tasks WHERE plan_id IN (SELECT id FROM plans);

-- Borrar los planes
DELETE FROM plans;

SET FOREIGN_KEY_CHECKS = 1;
