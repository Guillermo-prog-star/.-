-- V91__add_scenario_metadata.sql
ALTER TABLE questions ADD COLUMN metadata JSON;
UPDATE questions SET metadata = '{"scenario_intensity": 4, "trigger_type": "Incumplimiento de acuerdo / Incertidumbre", "expected_age_range": "12-18", "family_role": "Progenitor / Cuidador principal", "validation_status": "PILOT_V1", "pilot_version": "1.2.0", "clinical_hypothesis": "Una mayor regulación corporal inicial (NOTICE) se asociará probabilísticamente con una menor disonancia relacional al día siguiente (EFFECT)."}' WHERE parent_key = 'M-POC-S1';
