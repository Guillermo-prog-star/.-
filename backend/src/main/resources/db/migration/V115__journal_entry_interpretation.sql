-- V115: separar lo ocurrido de lo interpretado en el diario (ADR-018).
--
-- journal_entries ya tiene observations ("¿qué ocurrió?"), emotion,
-- reflection y learning. Se agregan:
--   interpretation          "¿qué interpreté / qué estoy concluyendo?"
--   alternative_explanation "¿qué otra explicación considero / qué me falta saber?"
--   body_signal             "¿qué noto en el cuerpo?" (Amendment 1; separado de emotion)
--   explicit_responses      mapa JSON paso -> respuesta explícita (Amendment 3):
--                           NONE_NOTICED | UNKNOWN | DECLINED | NOT_HELPFUL.
--                           Distingue "no sé" o "prefiero no compartir" de un
--                           campo no respondido.
--
-- Todas NULL (NULL = no respondido). Sin backfill. No alteran ICF, riesgo
-- ni Ruta de Conciencia.

ALTER TABLE journal_entries
    ADD COLUMN interpretation TEXT NULL,
    ADD COLUMN alternative_explanation TEXT NULL,
    ADD COLUMN body_signal TEXT NULL,
    ADD COLUMN explicit_responses JSON NULL;
