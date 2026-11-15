# ADR-019: La IA propone, la familia decide (asistente, comandos y aceptación por participante)

## Estado

Propuesto (2026-11-15). Pendiente de aprobación antes de tocar código o crear la migración.

## Contexto

Se propuso un resumen operativo de Integrity Family para que funcione con IA
y agentes. El ciclo a cerrar:

> Situación → comprensión compartida → acuerdo → práctica → registro → revisión.

La propuesta plantea cinco roles (coordinador, organizador, facilitador,
entrenador, revisor), un contrato mínimo para cada acuerdo y un principio:
**el modelo propone; las reglas verifican permisos y condiciones antes de
guardar o ejecutar**. Recomienda empezar con un asistente coordinador y
separar agentes solo cuando una evaluación lo justifique.

### Lo que ya existe (verificado en el código antes de diseñar)

| Elemento propuesto | Ya cubierto por |
|---|---|
| La IA no ejecuta por su cuenta | No hay *tool use* en ningún módulo: la IA solo genera texto. Hoy no puede mutar nada. |
| Capa que verifica antes de ejecutar | Family Action Engine (`familyhome`, IFRM-D Hito 5): autoriza, valida el estado de journey (`EvidencePolicyGate`), ejecuta la acción de dominio, garantiza idempotencia (`family_action_executions`, V102) y deja rastro. Hoy tiene un solo comando: `accept-first-sprint`. |
| La IA no diagnostica ni arbitra | `SAFETY_RULES` (reglas 4–8), comunes a todos los modos del mentor; ADR-018 §4: no decide entre versiones |
| Separar hecho e interpretación | ADR-018 (aprobado), V115 |
| Diarios privados fuera de informes | Lo `PRIVATE` solo lo ve su autor, sin bypass de admin ni creador (ADR-012 Amendment 1) |
| No confundir registro con realidad | Sentinel AI ya no califica ni valida evidencias; completar una tarea ya no fabrica bitácora; estados de misión `CHANGED_BY_AGREEMENT`/`POSTPONED`; `attempt_status` (V114) |
| Criterio de cumplimiento antes de ejecutar | `success_indicator` ("terminado cuando…") en los planes generados por IA |
| Registro con fuente | `EpistemicSource` (`OBSERVED`/`TOLD_BY_OTHER`/`DOCUMENT`/`UNCERTAIN`) |
| No alterar la medición durante el piloto | Directriz V1.2: `SCENARIO_V1_2` congelado |

### Lo que falta

1. **No hay una regla escrita sobre cómo actuaría la IA si tuviera
   herramientas.** Que hoy no tenga ninguna es una propiedad accidental, no
   una decisión. La primera herramienta que se agregue podría escribir
   directo en repositorios.
2. **Ningún compromiso tiene aceptación por participante.**
   - `ImprovementPlan` (V111, ADR-010) tiene una sola aceptación
     (`accepted_by`): una persona acepta por toda la familia, y el estado no
     bloquea nada (ADR-010, Decisión 4).
   - `PlanTask.responsible` se asigna automáticamente al miembro de la
     evaluación (`PlanTaskService`), sin que esa persona lo acepte.
   - `SprintMission` no tiene participantes ni aceptación: la escribe quien
     crea el sprint.
   - No existe una entidad "acuerdo": `FamilyLogbookEntry.familyAgreement`
     es texto libre.

## Decisión

### 1. Un solo asistente; los roles son modos, no agentes

Los cinco roles se implementan como **modos de prompt del mentor que ya
existe**, compartiendo `SAFETY_RULES`. No se crean agentes separados ni
orquestación entre agentes. Se separaría un agente solo si una evaluación
muestra que mejora el resultado, como pide la propia propuesta.

| Rol | Puede | No puede |
|---|---|---|
| Coordinador | Identificar la intención y conducir el recorrido | Tomar decisiones familiares |
| Organizador | Convertir un relato en situación, pendientes y **borradores** de acuerdo | Asignar compromisos sin aceptación |
| Facilitador | Separar observación, interpretación, emoción y petición (ADR-018) | Atribuir intenciones o diagnosticar |
| Entrenador | Proponer una práctica del catálogo existente | Presentar una práctica nueva como validada |
| Revisor | Comparar acuerdos, registros y percepciones; señalar datos faltantes | Afirmar causalidad o inventar cumplimiento |

### 2. La IA solo propone; toda mutación es un comando confirmado del Action Engine

Regla de arquitectura, válida para cualquier herramienta futura:

- **Lectura:** si la IA recibe herramientas de lectura, se ejecutan **como
  el usuario que conversa**, nunca con privilegios de sistema. Reutilizan
  las consultas que ya filtran visibilidad (`findVisibleToMember`): lo
  `PRIVATE` de otro miembro nunca entra al contexto.
- **Escritura:** la IA no escribe en repositorios ni llama servicios de
  dominio. Su salida es un **borrador** que se muestra a la persona. Solo
  cuando la persona confirma, el cliente envía un comando al Family Action
  Engine, que autoriza, valida, ejecuta, aplica idempotencia y deja rastro
  en `family_action_executions`.
- La IA nunca modifica, cancela ni da por cumplido un acuerdo existente.
  Puede sugerir "revisar este acuerdo"; el cambio lo hace una persona.
- Ningún comando activa un compromiso de otra persona (ver punto 3).

Esto no requiere código ahora: hoy no hay herramientas. Es la condición para
agregar la primera.

### 3. Aceptación por participante en la misión de sprint

La unidad de práctica que ya existe es `SprintMission` (tiene estados desde
este mismo ciclo). En vez de crear una entidad "acuerdo" nueva, se le agregan
participantes con aceptación individual.

**Migración (próximo número libre, hoy V116)**, solo DDL:

`sprint_mission_participants`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | BIGINT PK | |
| `mission_id` | FK `sprint_missions` | `ON DELETE CASCADE` |
| `member_id` | FK `family_members` | único por misión |
| `acceptance` | VARCHAR(20) | `PROPOSED` / `ACCEPTED` / `DECLINED` |
| `recorded_source` | VARCHAR(20) NULL | `EpistemicSource`: `OBSERVED` si responde la propia persona; `TOLD_BY_OTHER` si otro adulto registra lo que la persona dijo (niños sin cuenta) |
| `recorded_by_member_id` | FK `family_members` NULL | quién registró la respuesta, resuelto del principal |
| `responded_at` | DATETIME NULL | |

Reglas:

- La **aceptación es un eje separado del estado** de la misión
  (`PENDING`/`COMPLETED`/`CHANGED_BY_AGREEMENT`/`POSTPONED`). No se mezclan
  en un solo enum.
- Una misión sin participantes declarados se comporta como hoy, así que no
  rompe los sprints existentes. No hay backfill.
- Una misión con participantes está **activa solo si todos aceptaron**.
  Mientras alguno esté en `PROPOSED`, se muestra como propuesta.
- `DECLINED` no es incumplimiento ni se reprocha: la misión se ajusta
  (`CHANGED_BY_AGREEMENT`) o se retira a esa persona. No cuenta en
  `consistencyScore`.
- Una persona solo puede aceptar o rechazar **su propia** participación.
  Registrar por otro exige `TOLD_BY_OTHER` y deja el autor real. La
  aceptación de un niño registrada por un adulto queda marcada como tal; no
  equivale a que el niño la haya dado en la plataforma.
- `PlanTask.responsible` (asignado automáticamente) se muestra como
  **responsable propuesto**, no como compromiso. Cambiar ese flujo queda
  fuera de este ADR.

### 4. Contrato mínimo del acuerdo, sobre lo que ya existe

| Campo de la propuesta | Dónde vive |
|---|---|
| Situación | `JournalEntry.observations`, separada de `interpretation` (V115) |
| Acción observable | `SprintMission.description` |
| Participantes y aceptación | `sprint_mission_participants` (punto 3) |
| Plazo | `FamilySprint` (7 o 15 días) + fecha de revisión (ADR-018 Amendment 5) |
| Cumplimiento definido antes | `success_indicator` / `indicadorCumplimiento` |
| Registro con fuente | `EpistemicSource`; el autorreporte se distingue de la observación |
| Privacidad | `EntryVisibility` |
| Estado | estado de misión + aceptación (dos ejes) |

## Alcance explícitamente excluido

- Agentes múltiples, orquestación entre agentes, memoria compartida entre
  agentes.
- *Tool use* real: este ADR fija las condiciones, no lo implementa.
- Los espacios Sala, Taller, Consejo, Mapa y Archivo: no existen en el
  frontend. Hoy son nombres de interfaz, no componentes, y este ADR no los
  crea.
- Cambiar la aceptación única de `ImprovementPlan` (ADR-010).
- Cualquier efecto sobre ICF, riesgo o Ruta de Conciencia.

## Consecuencias

- Costo bajo: una tabla, endpoints para responder la aceptación propia, y
  UI de "propuesta / aceptada" en las dos vistas de sprint.
- La regla del punto 2 hace auditable cualquier acción futura de la IA: toda
  mutación pasa por un comando con rastro, no por una llamada directa.
- Riesgo: si la aceptación se vuelve un trámite ("acepta para continuar"),
  pierde sentido. La UI debe permitir rechazar o proponer un cambio con la
  misma facilidad que aceptar.

## Validación pendiente (Regla V1.1.1)

Antes de medir beneficios familiares, la primera versión debe demostrar
cuatro propiedades verificables:

1. Registra fielmente lo dicho (sin reescribir el texto de la familia).
2. Respeta la privacidad (lo `PRIVATE` de otro nunca aparece en el contexto
   de la IA ni en una vista compartida).
3. Ningún compromiso se activa sin la aceptación de cada participante.
4. Una familia puede completar y revisar un acuerdo con una carga razonable.

La comparación de interés es la propuesta por la propia propuesta: un flujo
guiado **sin** IA frente al asistente. Si el asistente no reduce la carga de
recorrer el ciclo, no se justifica su costo.
