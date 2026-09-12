# ADR-006: Borrador de nota de seguimiento profesional — de plantilla en frontend a generación versionada en backend

**Status:** Accepted
**Date:** 2026-07-18
**Deciders:** William Lopez

## Context

**Motivación:** el Panel Profesional (`/professional`) mostraba una tarjeta rotulada "Resumen Clínico Asistido por IA" que, en realidad, no invoca ningún modelo de IA. `generateAiSummaryText()` (`if-frontend/src/app/features/professional/professional-dashboard.component.ts:284-343`) es una función TypeScript que concatena un template literal con datos ya cargados en el signal `dataView()` — sin llamada HTTP a Claude ni a ningún endpoint de generación. El botón "Usar como Nota de Acompañamiento" (renombrado en la corrección inmediata de este mismo día a "Revisar y editar borrador", ver más abajo) copiaba ese texto directo al textarea de notas.

El problema no es solo la etiqueta. Es una discrepancia entre lo que la interfaz afirma, lo que técnicamente ocurre, y el nivel de confianza clínica que el texto puede inducir en un profesional (médico/orientador) que lo use como base de su nota de seguimiento. Un texto con apariencia de análisis individualizado, generado en realidad por reglas fijas en el navegador, sin versión, sin snapshot del dato que lo originó y sin registro de auditoría, es una falla de trazabilidad y gobernanza en un módulo que produce documentación profesional sobre familias.

**Corrección inmediata ya aplicada (mismo día, previa a este ADR):** `professional-dashboard.component.html:218-231` — se renombró la tarjeta a "Borrador de Nota de Acompañamiento", se agregó una leyenda visible ("Generado automáticamente... Requiere revisión, edición y aprobación del profesional. No constituye diagnóstico clínico.") y se renombró el botón a "Revisar y editar borrador". Esto resuelve el etiquetado engañoso, no la arquitectura. Este ADR cubre lo segundo.

**Precedente directo a extender, no a descartar:** el mismo controller que sirve los datos de esta pantalla (`SupportNetworkController.java:115-121`, `GET /api/families/{familyId}/support/data-view`) ya expone `POST /api/families/{familyId}/support/notes` (`SupportNetworkController.java:124-132`, protegido con `@PreAuthorize("hasAnyRole('THERAPIST','ORIENTADOR','ADMIN')")`), que persiste `SupportProfessionalNote` (`support/domain/SupportProfessionalNote.java:10-37`: `id`, `assignmentId`, `familyId`, `supportMemberId`, `content`, `visibleToFamily`, `createdAt`). Es una entidad plana, sin ciclo de vida, sin versión, sin snapshot — el borrador de este ADR debe alimentar ese flujo, no duplicarlo.

**Por qué no se decide aquí el ciclo de firma clínico completo (UNDER_REVIEW → EDITED → APPROVED → SIGNED):** antes de construirlo hacen falta respuestas que no le corresponden a este ADR: ¿el borrador es documentación administrativa o queda en historia clínica? ¿qué perfiles pueden aprobar/firmar? ¿existe ya un concepto de firma digital en el sistema? Ninguna de esas preguntas tiene hoy una respuesta en el dominio — inventar los cinco estados sin ellas repetiría el error que ADR-004 ya identificó y descartó explícitamente para el bounded context `research`: *"Diseñar un Comité Científico y un pipeline PRISMA antes de que exista una sola fila real en una tabla de evidencia es la misma trampa"* (ADR-004, sección "Alcance descartado deliberadamente"). Aquí el equivalente sería diseñar un flujo de firma clínica antes de que exista un solo borrador generado por el sistema.

**Nota de alcance — esto no es una hipótesis bajo ADR-004:** el borrador de seguimiento no depende de evidencia longitudinal para ser válido (a diferencia de PAF o Determinantes Transformacionales) — es documentación operacional determinista. Por eso no se persiste en `hypothesis_evidence`; tiene su propia entidad. Se cita ADR-004 aquí únicamente por el principio que comparte (el estado operacional mutable no puede ser la fuente de verdad de algo que debe seguir siendo interpretable después), no porque el borrador sea una hipótesis.

## Decision

**La generación del borrador se traslada al backend, dentro del módulo `support` existente (no un módulo `professional` nuevo), como una plantilla determinista versionada — sin conectar IA generativa todavía.**

### Decisión 1 — nueva entidad `ProfessionalFollowUpDraft`, no un módulo nuevo

Vive en `com.integrityfamily.support.domain`, junto a `SupportProfessionalNote`, con la que comparte `assignmentId`/`familyId`. No se crea `com.integrityfamily.professional` — el precedente (`SupportNetworkController`) ya resuelve "acceso profesional a datos de una familia", y este borrador es una vista derivada de esos mismos datos, no un dominio nuevo.

Campos:

| Campo | Propósito |
|---|---|
| `id` (UUID) | Identificador del borrador |
| `familyId` / `assignmentId` | Igual que `SupportProfessionalNote` |
| `generatedByUserEmail` | Quién lo generó (siempre un profesional autenticado — nunca un proceso automático) |
| `generatedAt` | Timestamp de generación |
| `generatorType` | `RULE_BASED_TEMPLATE` hoy; `AI_ASSISTED` reservado para una Fase 5 futura, no implementada (ver Consequences) |
| `templateVersion` | `"professional-follow-up-v1.0"` — puerta a Decisión 2 |
| `sourceSnapshot` | JSON con los valores exactos de `dataView()` usados al generar (`icfScore`, `icfLabel`, `icfDirection`, `riskLevel`, `sentinelActive`, `activeSprintStatus`, `planSummaryAvailable`, `crisisHistoryAvailable`) |
| `narrativeText` | El texto ensamblado (mismo formato que produce hoy `generateAiSummaryText()`) |
| `status` | `GENERATED` / `VOIDED` (Decisión 3) |

### Decisión 2 — `templateVersion`, no reglas silenciosas

La lógica de `generateAiSummaryText()` (incluida la parte que ya es una regla real: líneas 305-315, `recomendacionesPlan` según `sentinelActive`/`riskLevel`/`icfDirection`) se traslada tal cual a `ProfessionalFollowUpDraftService`, versionada como `"professional-follow-up-v1.0"`. Cualquier cambio futuro al texto o a las reglas condicionales exige subir esta versión — mismo principio que `hypothesis_version` en ADR-004 (Decisión 2): la versión describe la *definición* de la plantilla, no la versión del software.

### Decisión 3 — `sourceSnapshot` congela el dato, no lo referencia

`dataView()` se construye a partir de estado operacional mutable (`family_longitudinal_state`, riesgo actual, sprint activo) — nada de eso tiene historial propio. Si el borrador solo guardara un ID de referencia a "la familia", el mismo borrador leído mañana mostraría datos distintos sin que el texto haya cambiado — rompe la trazabilidad que este ADR existe para resolver. Por eso `sourceSnapshot` es JSON embebido con los valores crudos vistos al momento de generar, no una FK a una tabla de snapshots (no existe una en el dominio hoy, y crear una tabla de snapshots genérica para un solo consumidor sería la misma sobre-construcción que ADR-004 rechazó para `research`).

Ciclo de vida deliberadamente mínimo: `GENERATED` al crear; `VOIDED` automáticamente cuando se genera un borrador más nuevo para el mismo `assignmentId` (nunca se borra — mismo Principio de no reconstrucción de ADR-004: el borrador anterior sigue siendo la fuente primaria de lo que el profesional vio en ese momento, aunque ya no sea el vigente).

**Deliberadamente fuera de alcance de este ADR** (no se implementan `UNDER_REVIEW`/`EDITED`/`APPROVED`/`SIGNED`): la aprobación real de una nota sigue ocurriendo, como hoy, al invocar `POST /support/notes`. Se agrega ahí una única columna nueva, nullable: `follow_up_draft_id` (FK a `ProfessionalFollowUpDraft`) — vincula la nota final con el borrador que la originó, sin inventar un flujo de aprobación separado. Una nota guardada sin borrador previo (redactada libremente) simplemente deja ese campo en `null`.

### Decisión 4 — endpoint y contrato de salida

`POST /api/families/{familyId}/support/follow-up-drafts?assignmentId=...` en `SupportNetworkController` (mismo controller, mismo `@PreAuthorize("hasAnyRole('THERAPIST','ORIENTADOR','ADMIN')")` que ya protege `/notes`). Responde:

```json
{
  "draftId": "uuid",
  "familyId": "uuid",
  "generatedAt": "2026-07-18T13:25:00-05:00",
  "generatorType": "RULE_BASED_TEMPLATE",
  "templateVersion": "professional-follow-up-v1.0",
  "narrativeText": "...",
  "warnings": ["REQUIRES_PROFESSIONAL_REVIEW", "NOT_A_CLINICAL_DIAGNOSIS"]
}
```

El frontend elimina `generateAiSummaryText()` y llama a este endpoint; la leyenda de disclaimer ya agregada en la corrección inmediata pasa a construirse desde `warnings`, no hardcodeada en el HTML.

### Decisión 5 — auditoría reutilizando `AuditService`, sin infraestructura nueva

Cada generación de borrador y cada guardado de nota que traiga `followUpDraftId` no nulo registran evento vía `AuditService.registerSystemEvent(String userEmail, AuditEventType eventType, String metadataJson)` (`auth/service/AuditService.java:33`) — mismo mecanismo que ya usa `ConsentService` para `CONSENT_GRANTED`/`CONSENT_REVOKED` (`consent/service/ConsentService.java:65,90`). Se agregan dos valores nuevos a `AuditEventType` (`domain/AuditEventType.java`): `PROFESSIONAL_DRAFT_GENERATED`, `PROFESSIONAL_DRAFT_USED_AS_NOTE`.

## Trade-off Analysis

Frente a dejarlo como está (plantilla en Angular): más simple hoy, pero cada regeneración es indistinguible de la anterior, no hay auditoría de quién generó qué ni cuándo, y el texto es manipulable desde el cliente sin dejar rastro — exactamente lo que este ADR corrige.

Frente a implementar ya el ciclo de firma completo (`UNDER_REVIEW`→`SIGNED`, sección 3.5 del análisis que originó este ADR): se ganaría un flujo "completo", pero sobre preguntas de gobernanza sin responder (¿quién firma?, ¿es historia clínica?). El costo de construirlo mal y tener que deshacerlo es mayor que el costo de esperar — mismo cálculo que ADR-004 hizo para el bounded context `research`.

Frente a crear un módulo `professional` nuevo: separaría conceptualmente "vista profesional" de "red de apoyo", pero el precedente (`SupportNetworkController`, `SupportProfessionalNote`) ya vive en `support` y las diferencia solo por rol (`THERAPIST`/`ORIENTADOR`) sobre el mismo `assignmentId` — dividir el módulo ahora sería reorganizar código funcionando sin un problema real que lo justifique.

## Consequences

- **Más fácil:** el borrador ahora es reproducible (mismo `sourceSnapshot` + `templateVersion` → mismo texto), auditable (`AuditEventType` nuevo, mismo mecanismo que consentimientos) y desacoplado del ciclo de release del frontend — cambiar una regla de redacción ya no requiere desplegar Angular.
- **Más difícil:** el frontend deja de poder generar el borrador offline/sin red — antes era una función pura en el cliente; ahora depende de un roundtrip al backend. Aceptable: la pantalla ya depende de `data-view` vía HTTP para los datos crudos.
- **Habrá que revisitar (no comprometido a implementarse):** el flujo de aprobación/firma clínica completo (`UNDER_REVIEW`/`EDITED`/`APPROVED`/`SIGNED`) queda pendiente de que producto/legal respondan las preguntas de gobernanza planteadas en Context — cuándo eso ocurra, decidir si esos estados viven en `ProfessionalFollowUpDraft`, en `SupportProfessionalNote`, o en una entidad nueva. También pendiente: la separación de procedencia por sección (dato vs. regla vs. observación del profesional, propuesta en el análisis original) — hoy `sourceSnapshot` da trazabilidad completa a nivel de backend/auditoría, pero no se expone desglosada en la UI; se difiere hasta que un caso de uso real la necesite. La Fase 5 (redacción asistida por Claude sobre hechos validados, con esquema JSON estricto, `allowedTerminology`/`prohibitedClaims`, temperatura baja y validación de salida) requiere su propio ADR-007 cuando exista necesidad concreta — no se decide aquí, siguiendo el mismo principio que este proyecto ya aplicó en interoperabilidad ("no construir el API Gateway/OAuth2 completo antes de tener un consumidor real", `CLAUDE.md`).

## Action Items

1. [x] Migración `V107` — tabla `professional_follow_up_drafts` (columnas de Decisión 1) + columna nullable `follow_up_draft_id` en `support_professional_notes`. Corregida por `V108` (ver Corrección más abajo).
2. [x] Entidad `ProfessionalFollowUpDraft` + enum `DraftStatus {GENERATED, VOIDED}` + enum `DraftGeneratorType {RULE_BASED_TEMPLATE, AI_ASSISTED}` (`com.integrityfamily.support.domain`).
3. [x] `ProfessionalFollowUpDraftService` — puerto de `generateAiSummaryText()`/`recomendacionesPlan` desde `professional-dashboard.component.ts:284-343`, versionado como `professional-follow-up-v1.0`; marca `VOIDED` el borrador anterior del mismo `assignmentId` al generar uno nuevo.
4. [x] `POST /api/families/{familyId}/support/follow-up-drafts` en `SupportNetworkController`, mismo `@PreAuthorize` que `/notes`.
5. [x] `AuditEventType.PROFESSIONAL_DRAFT_GENERATED` / `PROFESSIONAL_DRAFT_USED_AS_NOTE`, invocados vía `AuditService.registerSystemEvent`. Solo `PROFESSIONAL_DRAFT_GENERATED` se emite hoy — `_USED_AS_NOTE` queda reservado para cuando `addNote` acepte `followUpDraftId` (fuera de alcance de este ADR).
6. [x] Frontend: eliminado `generateAiSummaryText()`; `professional-dashboard.component.ts` llama al nuevo endpoint (botón explícito "Generar borrador", no automático — evita voidar un borrador en cada simple visita a la familia); leyenda de disclaimer construida desde `warnings` de la respuesta.
7. [x] Tests unitarios de `ProfessionalFollowUpDraftService` (5 casos) + `ProfessionalFollowUpDraftControllerIntegrationTest` (4 casos, `@SpringBootTest`+`@AutoConfigureMockMvc`+JWT real, mismo patrón que `FamilyHomeControllerJwtIntegrationTest`): asignación tradicional con usuario multi-rol → 200 y persiste `GENERATED`; regenerar → anterior `VOIDED`; profesional conectado vía `FamilyEcosystemLink` → 200; usuario sin rol profesional → 403. Los casos 1 y 3 son regresión deliberada de los dos bugs reales encontrados en verificación manual (2026-07-18): el bug de `CustomUserDetailsService` con roles múltiples, y la FK de `V107` corregida en `V108`. Verificado además end-to-end en navegador real contra MySQL.

### Corrección — `V108`, `assignment_id` polimórfico

Verificación en navegador (profesional real conectado vía Ecosistema de Apoyo, no vía asignación tradicional) encontró que `V107` le puso una FK a `professional_follow_up_drafts.assignment_id` contra `family_support_assignments(id)`. Pero `SupportNetworkService.getDataView()` — que este ADR reutiliza deliberadamente para autorización y datos (Decisión 4) — resuelve `assignmentId` por **dos** caminos con espacios de ID distintos: `FamilySupportAssignment` o `FamilyEcosistemLink` (profesional del Ecosistema de Apoyo). Un profesional del segundo camino ve los datos correctamente (mismo nivel de acceso 5/5) pero la generación del borrador fallaba al guardar, con violación de FK.

`V108` elimina esa FK. No es una corrección improvisada: `support_access_log` (mismo módulo, V77/V82) ya resolvió exactamente este problema antes — `assignment_id` ahí es deliberadamente polimórfico, sin FK, solo indexado — porque ese log también se escribe desde ambos caminos de `getDataView()`. `V108` alinea `professional_follow_up_drafts` con ese precedente ya establecido, en vez de restringir la generación de borradores solo a asignaciones tradicionales (lo que excluiría, sin justificación real, a profesionales del Ecosistema con acceso legítimo idéntico).

### Corrección 2 — `V109`, mismo problema en `support_professional_notes`

Verificación en navegador (mismo profesional real, vía "Revisar y editar borrador" → "Guardar nota") encontró el mismo problema una capa más adelante: `addNote()` (código preexistente, no tocado originalmente por este ADR) solo resolvía `assignmentId` contra `FamilySupportAssignment` — el camino Ecosistema, que `V108` ya había habilitado para *generar* el borrador, seguía sin poder *guardarlo* como nota. Guardar fallaba silenciosamente (404 "Asignación no encontrada").

Esto rompía la promesa implícita del propio botón "Revisar y editar borrador" (Decisión 3: la nota final se guarda "como hoy", vía `/support/notes`) para cualquier profesional conectado vía Ecosistema — exactamente la inconsistencia que `V108` ya había identificado y resuelto para la generación, pero sin propagarla al guardado.

Se corrige con el mismo patrón exacto: `addNote()` ahora resuelve primero `FamilySupportAssignment`, y si no existe, cae al camino `FamilyEcosystemLink` (validando `participant.contactEmail` contra el email del profesional, igual que `getDataView()`); `V109` elimina la FK equivalente de `support_professional_notes.assignment_id` (V77), alineándola también con el precedente de `support_access_log`. Deliberadamente **no** se extendió el `resolveSupportMemberId()` del controller (que exige un perfil `SupportNetworkMember` para cualquier profesional, tradicional o de Ecosistema) — sigue siendo un requisito previo para dejar notas, sin cambios; no era lo que bloqueaba al caso real verificado.
