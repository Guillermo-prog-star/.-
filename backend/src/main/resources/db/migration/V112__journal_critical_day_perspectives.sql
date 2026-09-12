-- V112: Perspectivas multiples sobre un mismo evento familiar (H2, ADR-012).
--
-- journal_entries no tenia columna de autor (solo family_id) -- bloqueaba
-- incluso H1 (de quien es cada relato). critical_days ya tenia member_id
-- EN PRODUCCION, pero solo via ddl-auto=update: ninguna migracion lo
-- formalizo (V54/V69 lo declaran dentro de CREATE TABLE IF NOT EXISTS, que
-- es no-op sobre la tabla ya creada por V3). Sobre un schema puramente
-- Flyway la columna no existe, asi que este script la formaliza de forma
-- idempotente (mismo patron que V68) antes de referenciarla.
--
-- perceived_event_key es una cadena opaca asignada por el cliente (no
-- inferida por IA ni por coincidencia de fecha/categoria) -- se descarta a
-- proposito una entidad FamilyEvent nueva mientras no exista un segundo
-- consumidor real (mismo criterio que ADR-004 aplico al bounded context
-- research descartado).
--
-- visibility es PRIVATE por defecto: ninguna fila se expone a otro miembro
-- salvo que su propio autor la comparta explicitamente (nunca automatico).

-- ── journal_entries ──────────────────────────────────────────────────────
-- member_id es columna nueva (JournalEntry.member se agrega en el mismo
-- cambio de ADR-012): en prod tampoco existia aun, se crea limpia con su FK.
ALTER TABLE journal_entries
    ADD COLUMN member_id BIGINT NULL AFTER family_id,
    ADD COLUMN perceived_event_key VARCHAR(64) NULL AFTER member_id,
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' AFTER perceived_event_key,
    ADD CONSTRAINT fk_journal_entries_member FOREIGN KEY (member_id) REFERENCES family_members(id) ON DELETE SET NULL,
    ADD INDEX idx_journal_entries_perceived_event_key (perceived_event_key);

-- ── critical_days.member_id: formalizacion idempotente ───────────────────
-- Sin FK: en prod la columna ya existe (ddl-auto) como BIGINT desnudo
-- -- CriticalDay.memberId es un Long plano, no @ManyToOne -- y puede tener
-- valores huerfanos de miembros borrados; anadir una FK aqui podria fallar
-- sobre datos reales. El filtro de visibilidad (findVisibleToMember) compara
-- c.memberId = :viewerMemberId sin join, no necesita FK.
SET @cd_member_id_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'critical_days'
      AND COLUMN_NAME = 'member_id'
);
SET @ddl := IF(@cd_member_id_exists = 0,
    'ALTER TABLE critical_days ADD COLUMN member_id BIGINT NULL AFTER family_id',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── critical_days: clave de evento + visibilidad ────────────────────────
ALTER TABLE critical_days
    ADD COLUMN perceived_event_key VARCHAR(64) NULL,
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD INDEX idx_critical_days_perceived_event_key (perceived_event_key);

-- Backfill: las filas ya existentes de critical_days que tienen member_id
-- (reportadas por un miembro real, ver CrisisServiceImpl.registerCrisis)
-- se dejan en PRIVATE por defecto -- es el valor mas conservador y no
-- cambia comportamiento observable hasta este ADR (antes no habia
-- filtrado alguno). Las que no tienen member_id (ej. SENTINEL_ALERT
-- generadas por AiInferenceService) tambien quedan PRIVATE pero el filtro
-- de lectura (JournalEntryRepository/CriticalDayRepository.findVisibleToMember)
-- las trata como visibles a todos por no tener autor -- no requieren backfill.
