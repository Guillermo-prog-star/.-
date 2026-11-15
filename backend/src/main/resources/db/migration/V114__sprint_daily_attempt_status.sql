-- V114: estado del intento en el check-in diario del sprint.
--
-- Distingue cuatro situaciones que antes quedaban mezcladas en texto libre:
-- NO_OPPORTUNITY (no hubo oportunidad), NOT_ATTEMPTED (hubo oportunidad y no
-- se intentó), ATTEMPTED_UNFAVORABLE (se intentó sin resultado favorable) y
-- ATTEMPTED_FAVORABLE (se intentó con resultado favorable). Un día sin
-- oportunidad no es un fracaso de la práctica.
--
-- NULL = no respondido (incluye "prefiero no indicarlo" y todos los
-- check-ins anteriores a esta migración). Sin backfill.

ALTER TABLE sprint_dailies
    ADD COLUMN attempt_status VARCHAR(30) NULL;
