# ADR-012: Perspectivas múltiples sobre un mismo evento familiar (H2) — anclaje ligero sin nueva raíz de agregado

**Status:** Accepted
**Date:** 2026-08-09
**Deciders:** William Lopez

## Context

**Motivación:** en esta misma sesión se evaluó un documento externo que aplica el principio de cambio de altura de observación (Chi Po-lin, *Beyond Beauty: Taiwan from Above*) a Integrity Family, proponiendo un "Principio de Altura de Observación" en 5 niveles (H1 Experiencia → H2 Perspectivas → H3 Patrón → H4 Trayectoria → H5 Legado). La verificación contra el código real mostró que H1, H3 (parcial), H4 y H5 ya tienen mecanismo operacional construido (`LongitudinalStateService`, `FamilyCausalEngine`, `hypothesis_evidence`, los módulos `legado`/`documentary`/`movie`/`lineage`/`dna`) — no ameritan infraestructura nueva. **H2 es el único nivel sin ningún mecanismo real hoy.**

El fundamento conceptual de H2 ya existe en el proyecto, aunque solo como prosa — `docs/vision.md:31-35` (Principio de Construcción Compartida de Significado Familiar, CCSF):

> *"La transformación familiar ocurre cuando las experiencias individuales dejan de ser interpretaciones aisladas y se convierten, mediante procesos deliberados de comunicación, reflexión y memoria compartida, en una comprensión colectiva..."*

CCSF describe la transformación de "interpretaciones aisladas" a "comprensión colectiva", pero el sistema no tiene forma de representar que existen varias interpretaciones aisladas *de un mismo evento real* antes de esa síntesis. Este ADR cierra ese hueco puntual, no reescribe CCSF.

### Verificado en este repo, no asumido

- **`JournalEntry`** (`backend/src/main/java/com/integrityfamily/domain/JournalEntry.java`) no tiene columna de autor/miembro — solo `family_id` (línea 29). Cualquier entrada de bitácora es anónima respecto a quién la escribió. Esto bloquea H2 antes incluso de llegar a "perspectivas": sin autoría, no hay forma de saber de quién es cada relato.
- **`CriticalDay`** (`backend/src/main/java/com/integrityfamily/domain/CriticalDay.java:25`) sí tiene `member_id` nullable, pero no existe ninguna clave que vincule dos filas como "el mismo incidente". Si dos miembros reportan el mismo evento real el mismo `day_date`, hoy son dos filas indistinguibles de dos eventos no relacionados. `CriticalDay` es además la entidad más conectada del dominio para esto: consumida activamente por `CrisisServiceImpl`, `FamilyContextEngine`, `AiInferenceService`, `DigitalTwinService`, `FamilyMovieService` y `FamilyTimelineService` (8 usos reales confirmados) — cualquier clave de agrupación agregada ahí se propaga a H4/H5 sin tocar esos consumidores.
- El único precedente real de "varias filas, una por miembro, sobre el mismo sujeto" es **`GuardianVote`** (`backend/src/main/java/com/integrityfamily/guardian/domain/GuardianVote.java`, `uk_one_vote_per_member` sobre `(family_id, voter_member_id)`) — pero es un voto de valor único (nominación de guardián), no una narrativa, y no tiene dimensión de privacidad: se asume visible al agregado.
- **`FamilyCouncilService`** (`backend/src/main/java/com/integrityfamily/council/service/FamilyCouncilService.java`) sintetiza con IA desde ADN/legado/árbol/contexto **agregado** de la familia — no captura ni distingue narrativas individuales de un mismo evento. No es H2, es un consumidor potencial de H2.
- **`ConsentPurpose`** (`backend/src/main/java/com/integrityfamily/consent/domain/ConsentPurpose.java`) solo tiene `ECOSYSTEM_SHARING`, `SUPPORT_NETWORK_SHARING`, `HEALTH_INTEROPERABILITY`, `RESEARCH` — los cuatro son de alcance familia→externo. **Corrección respecto a lo dicho antes en esta misma sesión:** se afirmó que el módulo `consent` ya resolvía la salvaguarda ética de "no exponer automáticamente la narrativa de un miembro a otro"; verificado ahora contra el enum real, eso es falso — `consent` no cubre visibilidad miembro→miembro dentro de la misma familia, solo familia→tercero.

## Decision

### Decisión 1 — autoría explícita en `journal_entries`

Agregar `member_id` (FK nullable a `family_members`) a `journal_entries`, mismo patrón que `CriticalDay.memberId`. Nullable porque entradas generadas por sistema/IA no tienen autor humano. Esto es un prerrequisito de H2, no una característica de H2 en sí — sin esto, "perspectivas múltiples" no tiene de quién distinguir la perspectiva.

### Decisión 2 — clave de agrupación ligera, no una raíz de agregado nueva

Agregar `perceived_event_key` (`VARCHAR(64)` nullable, indexado) a `journal_entries` y `critical_days`. Es una cadena opaca que el **cliente** asigna explícitamente (ej. UUID generado en frontend) cuando un miembro marca "esto nos pasó a varios" — nunca inferida automáticamente por IA ni por coincidencia de fecha/categoría.

Se descarta una entidad `FamilyEvent` nueva como raíz relacional del mismo evento: mismo principio que ADR-004 aplicó al descartar el bounded context `research` completo — no hay todavía un segundo consumidor que necesite integridad referencial real. Una clave opaca es reversible y barata; una tabla nueva con FKs y su propio ciclo de vida no lo es.

### Decisión 3 — visibilidad privada por defecto

Agregar `visibility` (enum string: `PRIVATE` default, `SHARED_WITH_FAMILY`) a ambas tablas. Ninguna fila se muestra a otro miembro salvo que su propio autor la marque `SHARED_WITH_FAMILY` explícitamente — nunca automático, nunca inferido por IA. Mismo principio de acción manual explícita que V98 ya fijó para la activación del protocolo de seguridad ("Activación siempre manual... nunca automática").

Se descarta reutilizar el módulo `consent` (V104) para esto: su semántica es compartir con un tercero externo a la familia, no controlar visibilidad entre miembros de la misma familia — forzarlo ahí mezclaría dos casos distintos bajo el mismo enum, el mismo tipo de error que V97 corrigió al renombrar `IDENTIDAD_GENERO` porque el objeto real de riesgo no era el que el nombre sugería.

### Decisión 4 — explícitamente NO se construye ahora

- Ninguna síntesis por IA que fusione perspectivas distintas del mismo `perceived_event_key` en una sola narrativa — es una capacidad de *consumidor* (candidata natural para `FamilyCouncilService`, o un H3 futuro), no de captura, y no tiene todavía un caso de uso real que la reclame.
- Ninguna validación server-side de que dos filas con la misma `perceived_event_key` realmente describen el mismo evento — el vínculo es voluntario y puede ser inconsistente. Se acepta como limitación conocida (ver Consequences) en vez de construir detección automática sin evidencia de que haga falta (Regla V1.1.1).

## Trade-off Analysis

Frente a una entidad `FamilyEvent` completa (con validación referencial, más "correcta" formalmente): se gana integridad, se pierde reversibilidad — mismo argumento que ADR-004 ya usó contra el bounded context `research`. Con cero consumidores reales de la agrupación todavía, el costo de construir de más no se justifica.

Frente a no construir nada y dejar el Principio de Altura de Observación solo como marco conceptual en `docs/vision.md` (como ADR-011 hizo con IF-CAM): se pierde la posibilidad real de que dos miembros narren el mismo evento sin forzarlo a través de `JournalOrigin`/`category`. Pero a diferencia de IF-CAM (que no tenía ningún hueco de captura, solo de interpretación), aquí sí hay un hueco de captura verificado (`journal_entries` sin autor) — la diferencia es real, no cosmética, y justifica la Decisión 1 incluso si las Decisiones 2/3 no se aprobaran.

Frente a extender `consent` para cubrir visibilidad intra-familia: técnicamente posible, pero mezcla dos semánticas distintas bajo el mismo concepto — descartado en la Decisión 3.

## Consequences

- **Más fácil:** un miembro puede registrar su propia bitácora (`JournalEntry`) o día crítico (`CriticalDay`) atribuido a sí mismo, y opcionalmente vincularlo a la misma clave que otro miembro use para narrar el mismo incidente — sin que se comparta automáticamente.
- **Más difícil:** nada estructural; el costo es semántico — `perceived_event_key` depende de que el frontend genere y coordine la misma clave entre los relatos de distintos miembros (ej. flujo de UI "invitar a alguien más a narrar este mismo evento"), lo cual es un flujo de producto que este ADR no diseña.
- **Habrá que revisitar:** si `perceived_event_key` acumula uso real con más de un miembro por clave, ahí sí hay evidencia para (a) una vista de agregación que junte las filas de una misma clave, (b) una síntesis IA opcional tipo Consejo Familiar, y (c) posiblemente promover la clave opaca a una entidad real con FK — no antes.

## Action Items

1. [x] Migración `V112` — a `journal_entries`: `member_id` (FK nullable a `family_members`) + `perceived_event_key` (`VARCHAR(64)` nullable, indexado) + `visibility` (string, default `PRIVATE`). A `critical_days`: `perceived_event_key` + `visibility` + formalización idempotente de `member_id` (existía solo vía `ddl-auto=update`, nunca en una migración — V3 crea la tabla sin él, V54/V69 lo declaran dentro de `CREATE TABLE IF NOT EXISTS` no-op). Sin FK sobre `critical_days.member_id`: dato preexistente en prod que puede tener huérfanos, y `CriticalDay.memberId` es un `Long` plano. Verificada contra MySQL 8.4 real (`FamilyLifecycleIntegrationTest` 6/6, Flyway V1→V112 sin errores) — la primera versión de V112 (`ADD COLUMN ... AFTER member_id`) fallaba sobre schema puramente Flyway y se corrigió antes de mergear.
2. [x] Entidades `JournalEntry`/`CriticalDay` — campos nuevos agregados (`member`/`visibility` en `JournalEntry`; `perceivedEventKey`/`visibility` en `CriticalDay`), más el enum `EntryVisibility`.
3. [x] Capa de lectura — `findVisibleToMember(familyId, viewerMemberId)` en `JournalEntryRepository`/`CriticalDayRepository`, resuelto vía `SecurityValidator.resolveViewerMemberId` (bypass admin/creador, filtro por autor para el resto). Aplicado en los 3 puntos de exposición real verificados: `CrisisController.getHistory`, `FamilyTimelineController.getTimeline`, `JournalController.getTimeline`. Autoría real conectada en creación (`CrisisController.reportCrisis`, `JournalController.createJournal`, antes pasaban autor `null` hardcodeado). Verificado: compilación limpia + 53 tests en verde (`CrisisServiceImplTest`, `JournalServiceTest`, `FamilyTimelineServiceTest`, `SecurityValidatorTest`). Consumidores `context`/`twin`/`movie` no exponen texto crudo de member a member — no requerían el filtro, ver Trade-off Analysis para el alcance verificado.
4. [x] `docs/vision.md` — agregado el Principio de Altura de Observación (H1-H5) como marco conceptual bajo CCSF, citando este ADR como el único punto donde H2 tiene mecanismo real; H1/H3/H4/H5 documentados como lectura de módulos ya existentes (mismo patrón que ADR-011 con IF-CAM), no como fases nuevas.
5. [ ] (Diferido, sin consumidor todavía) Endpoint de agregación por `perceived_event_key` y síntesis IA multi-perspectiva — no construir hasta que exista uso real de la clave con ≥2 miembros.
