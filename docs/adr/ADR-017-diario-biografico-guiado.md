# ADR-017: Diario guiado biográfico (módulo `biography`)

## Estado

Aceptado.

## Contexto

Se propuso integrar como feature del producto un diario guiado biográfico:
un documento con 12 capítulos de vida (Los comienzos, Infancia,
Adolescencia, Trabajo y trayectoria, Vida adulta y hogar, Familia y legado,
Mi árbol familiar, Mis padres y mis abuelos, Mis hermanos, Convertirme en
papá y serlo, Amor compromiso y celebraciones, Favoritos creencias y
consejos) con preguntas estructuradas + "para profundizar" + cierre de
capítulo, pensado para que un miembro narre su propia historia de vida en
primera persona, separando explícitamente qué vio/vivió, qué le contaron, y
qué significa hoy.

Se auditó el backend antes de diseñar (Explore agents sobre `legado`,
`lineage`, `dna`, `documentary`, `timeline`, `tree`, `myspace`) y se
confirmó que no existe nada parecido:

- `legado` (`FamilyLegacy`): un documento único por familia (constitución,
  misión, carta al futuro), no por persona, no por capítulos.
- `lineage` (`LineageMember`): tiene campos narrativos por ancestro
  (`story`, `valores`, `aprendizajes`, `erroresSuperados`, `tradiciones`,
  `misionesCumplidas`, `legadoPersonal`) y un `dataSource` (string libre) +
  `confidenceLevel` — el más cercano en espíritu, pero esos campos los
  llena **un tercero** construyendo el árbol sobre un ancestro, como
  bloques únicos de texto, sin preguntas ni capítulos ni autoría propia.
- `myspace` (`PrivateJournalEntry`): diario libre de reflexión emocional
  del presente, sin preguntas ni estructura.
- `documentary`/`dna`/`timeline`/`tree`: generados por IA o agregadores de
  solo lectura; no aplican.

## Decisión

**No se extiende `lineage`.** Su modelo de datos es sobre generación/
posición en el árbol, no sobre capítulos de vida, y sus campos narrativos
son un resumen de tercero, no una entrevista estructurada en primera
persona. El patrón real más parecido en el repo es `JournalEntry`
(ADR-012): autoría explícita resuelta del principal autenticado, nunca
confiada del cliente; visibilidad `PRIVATE` por defecto, nunca automática.

Se crea un **módulo nuevo** `biography` (`V113__biography_guided_diary.sql`):

1. **`BiographyQuestion`** (tabla `biography_questions`): catálogo fijo y
   global, igual para cualquier familia de la plataforma (12 capítulos, ~75
   preguntas). Sembrado por `BiographyQuestionDataInitializer` (Java,
   idempotente, `@Profile("!test")`, mismo patrón que
   `LineageDataInitializer`), **no** por SQL crudo en la migración.

   *Por qué no por SQL*: V75 sembró el Banco de Trayectorias directo por
   SQL y eso produjo el defecto corregido en V100-V101 (valores de
   `category` que no coincidían con el enum Java real, silenciado meses por
   un try/catch amplio). El catálogo del diario son ~75 filas de texto
   largo en español con acentos y comillas — mucho más propenso a errores
   de escape que un `INSERT` de pocas columnas cortas. Un inicializador
   Java se puede corregir sin una nueva migración mientras el contenido se
   sigue afinando.

2. **`BiographyAnswer`** (tabla `biography_answers`): una fila por
   `(member, question)`, editable (upsert, no append-only). Autor siempre
   un `FamilyMember` humano (`member_id` no nulo, a diferencia de
   `JournalEntry.member` que es nullable para contenido autogenerado).
   Campos: `answerText` (narrativa libre — cubre lo que vio/vivió + lo que
   le contaron + lo que pensó entonces, en un solo bloque, tal como invita
   el diario a escribir), `epistemicSource` (enum nuevo
   `com.integrityfamily.domain.EpistemicSource`: `OBSERVED`,
   `TOLD_BY_OTHER`, `DOCUMENT`, `UNCERTAIN` — etiqueta opcional, no
   forzada), `currentMeaning` ("lo que pienso hoy", separado explícitamente
   del recuerdo crudo), `confidenceLevel` (0-100, mismo concepto que
   `LineageMember.confidenceLevel`), `visibility` (se reutiliza el enum
   existente `EntryVisibility` — `PRIVATE`/`SHARED_WITH_FAMILY` — sin
   duplicarlo, mismo gobierno que `JournalEntry`/`CriticalDay`), y
   `completedAt` (null = borrador, no-null = marcado completo por el
   autor).

3. **Seguridad**: `@PreAuthorize("@familySecurity.check(#familyId)")` en
   cada endpoint (patrón `LineageController`/`ConsentController`); autoría
   siempre resuelta del `Principal` autenticado vía
   `MemberRepository.findByEmailIgnoreCase(...)`, nunca aceptada del body
   (patrón `JournalController.createJournal`). Visibilidad: una respuesta
   `PRIVATE` solo la ve su autor — **sin** el bypass de
   `ROLE_ADMIN`/creador de familia que usa `JournalService.getTimeline`
   vía `SecurityValidator.resolveViewerMemberId`. A diferencia del diario
   familiar, aquí cada miembro narra su propia vida en primera persona;
   que el creador de la familia (típicamente un padre) leyera lo privado
   de un hijo rompería la promesa de "privado". Un viewer sin perfil de
   miembro solo ve lo `SHARED_WITH_FAMILY`.

4. **Auditoría solo de eventos discretos**: no se audita cada edición de
   texto (~75 preguntas por miembro, ruido masivo), pero sí los dos eventos
   que afectan privacidad o son irreversibles: cambio de visibilidad
   (`BIOGRAPHY_VISIBILITY_CHANGED`) y borrado (`BIOGRAPHY_ANSWER_DELETED`).

## Amendment 1 — campos híbridos y siembra de datos reales

Tras una revisión del diseño (propuesta externa "Nuestra historia": escenas
libres con procedencia, perspectivas múltiples, correcciones y fuente
relacionada), se decidió **no** reemplazar el catálogo fijo por escenas
libres — el catálogo de 12 capítulos sigue siendo la fuente de verdad y el
rework de entidades/migración/servicio ya escritos no se justifica sin un
segundo consumidor real que lo exija (mismo principio ADR-004). En cambio,
se adoptó un híbrido: se agregaron a `BiographyAnswer` los campos concretos
que esa revisión señaló como huecos reales, sin tocar la forma del catálogo:

- `occurredPeriod` (texto libre: año, edad aproximada, etapa de vida) — "¿cuándo cree que ocurrió?".
- `meaningThen` — "lo que pensé entonces", complementa `currentMeaning` ("lo que pienso hoy").
- `relatedSourceNote` — descripción libre de una foto/documento/objeto/testigo relacionado (no es un adjunto de archivo).
- `perceivedEventKey` — mismo patrón exacto de `JournalEntry.perceivedEventKey` (ADR-012): permite que otro miembro etiquete su propia respuesta como relato de la misma escena ("otra perspectiva"). Nuevo endpoint `GET /api/families/{familyId}/biography/perspectives/{perceivedEventKey}` agrega esas respuestas respetando la misma visibilidad que `getAnswers`.
- `correctionNote` — nota ligera de qué cambió el autor al releer o escuchar su propia respuesta. Deliberadamente **no** un historial de versiones completo — eso sería construir para una necesidad hipotética sin un segundo consumidor real.

**Sin siembra de respuestas reales en el código.** Se evaluó sembrar,
vía un `CommandLineRunner`, respuestas reales de una entrevista biográfica
previa de un miembro, y se descartó: (a) dejaría datos personales de una
persona real en el historial de git y en cada imagen Docker para siempre;
(b) correría en producción como si fueran datos de ejemplo; (c) un seed
"idempotente por ausencia de respuestas" resucita lo que el autor borró a
propósito. Las respuestas reales se cargan por la propia UI/API, como las
de cualquier otro miembro. El catálogo de preguntas (contenido global, no
personal) sí se sigue sembrando por `BiographyQuestionDataInitializer`.

## Alcance explícitamente excluido

- Sin síntesis o puntuación por IA de las respuestas.
- Sin empuje automático de respuestas del diario hacia los campos
  narrativos de `LineageMember` — si algún día se quiere esa destilación,
  es una feature separada con su propio consumidor real (mismo principio
  de ADR-004/ADR-012: no construir para una necesidad hipotética).
- Sin frontend en esta iteración — el Angular de `if-frontend` es el
  siguiente paso natural, a implementar aparte.

## Consecuencias

- Catálogo reutilizable por cualquier familia sin trabajo adicional: una
  familia nueva ve los 12 capítulos de inmediato, sin seed por familia.
- `biography_answers` crece linealmente con miembros × preguntas
  respondidas (máximo ~75 filas por miembro) — volumen trivial, sin
  necesidad de particionamiento ni archivado.
- Si en el futuro se decide destilar respuestas del diario hacia
  `LineageMember.story`/`valores`/etc., esa destilación deberá ser una
  acción explícita y curada (no automática), para no mezclar narrativa
  cruda de ~75 preguntas con el resumen editorial que hoy tiene cada nodo
  del árbol.

## Verificación

- 10 tests unitarios en `BiographyServiceTest` (JUnit 5 + Mockito, estilo
  `ConsentServiceTest`/`LineageServiceTest`): catálogo agrupado por
  capítulo, upsert crea vs. actualiza, rechazo cuando el miembro no
  pertenece a la familia o no se resolvió autor, filtrado de visibilidad al
  leer el diario de otro miembro, borrado idempotente. `mvn test
  -Dtest=BiographyServiceTest` → 10/10 verde.
- Migración V113 compila y aplica sobre el esquema Flyway existente
  (verificar con `docker compose up -d db` + arranque del backend antes de
  desplegar).
