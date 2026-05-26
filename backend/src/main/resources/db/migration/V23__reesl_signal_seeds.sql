-- V23: IF-REE — Señales faltantes para reglas EEDSL sembradas en V22.
-- Las reglas relapse_detection, simulation_suspected y dimension_collapse
-- no tenían entradas en emotional_rule_signals, impidiendo su ejecución.
-- Este script cierra el gap para que IF-REE pueda evaluarlas correctamente.

-- relapse_detection: dispara cuando el engine detecta respuestas de recaída
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'relapse_detected'
  FROM emotional_rules WHERE rule_key = 'relapse_detection' AND version = 1;

-- simulation_suspected: dispara cuando >60% de preguntas MIRROR son perfectas
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'simulation_suspected'
  FROM emotional_rules WHERE rule_key = 'simulation_suspected' AND version = 1;

-- dimension_collapse: dispara cuando alguna dimensión cae por debajo de 2.0/5.0
INSERT IGNORE INTO emotional_rule_signals (rule_id, signal_name)
SELECT id, 'dimension_collapse'
  FROM emotional_rules WHERE rule_key = 'dimension_collapse' AND version = 1;
