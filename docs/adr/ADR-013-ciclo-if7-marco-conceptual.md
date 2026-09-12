# ADR-013: Ciclo IF-7 — marco conceptual, sin instrumento nuevo

**Status:** Proposed
**Date:** 2026-09-05
**Deciders:** William Lopez
**Amendment (2026-09-12):** se agregan Decisión 4 (riesgo de Goodhart: métrica ≠ propósito) y Decisión 5 (Autonomía Familiar como hipótesis candidata, no constructo). Misma sesión conceptual (video sobre Feynman y el "juego amañado" de las métricas de éxito), verificado contra el código antes de escribir — ver bullets nuevos bajo "Verificado en este repo, no asumido".

## Context

Varios textos externos analizados en la misma sesión (técnica de Feynman; adaptación del método galileano; «Neurociencia del cuerpo» de Castellanos; conversatorio de José Luis Díaz sobre la conciencia; prácticas de Tolle) convergen en el mismo circuito, que puede condensarse como **IF-7**: `VER → PREGUNTAR → PROPONER → PREDECIR → ACTUAR → CONTRASTAR → APRENDER`.

Este ADR se limita a lo que hoy es decidible: **integrar IF-7 como lectura del ciclo que ya existe** (mismo criterio que ADR-011 con IF-CAM y ADR-012 con el Principio de Altura de Observación), y **registrar dos huecos verificados contra el código** sin construirlos todavía. Una versión anterior de este borrador proponía además migración `V113`, dos servicios nuevos (pre-registro de predicción y contraste), un escalón de "apropiación de habilidad" y cambios en `CopilotService`; todo eso se recorta — ninguna pieza está construida, y apilar tres capas nuevas sobre ADR-012 (recién mergeado) sin un disparador concreto es la trampa que la Regla V1.1.1 y ADR-004 existen para evitar.

### Verificado en este repo, no asumido

- El ciclo documentado en `CLAUDE.md` (`Diagnóstico → Plan → Misiones → Evidencias → Reevaluación → Aprendizaje → Legado`) ya cubre ~5 de los 7 pasos de IF-7. `NOTICE/THINK/ACT/AFTERMATH/EFFECT` (`SCENARIO_V1_2`, V89–V95, congelado por la Directriz Operativa V1.2) es la versión operacional del mismo ciclo; IF-CAM (ADR-011) lo detalla en 11 etapas.
- **Paso PREDECIR — hueco verificado:** `FamilyPrediction` ([`FamilyPrediction.java`](../../backend/src/main/java/com/integrityfamily/twin/domain/FamilyPrediction.java), tabla `family_predictions`, V43) tiene `status ∈ {ACTIVE, CONFIRMED, DISMISSED, EXPIRED}`, pero `DigitalTwinService` solo crea filas `ACTIVE` — **nunca se contrastan**. Además no hay vínculo `plan_task_id` ni código de hipótesis: la predicción es un pronóstico global del gemelo digital, no algo atado a una misión. `PlanTask.impactoIcf` es un entero de impacto esperado, sin línea base ni contraste posterior.
- **Módulo `lts` — código muerto:** `lts_sessions → lts_attempts → lts_errors → lts_hypotheses → lts_corrections → lts_comparisons → lts_insights` (schema `V7`) modela un loop intento→error→hipótesis→corrección→insight por familia/miembro, pero [`LearningSessionService`](../../backend/src/main/java/com/integrityfamily/lts/service/LearningSessionService.java) no tiene controller y ningún código lo invoca.
- **`FamilyCausalEngine`** ([`FamilyCausalEngine.java`](../../backend/src/main/java/com/integrityfamily/risk/service/FamilyCausalEngine.java)) se llama "Motor Inferencial Causal" pero implementa reglas heurísticas de correlación (R1–R7) con explicabilidad — no infiere causalidad `misión → resultado`. El nombre invita al error que el método galileano advierte (`ICaF ↓ + misión + ICaF ↑ ≠ causalidad`).
- La disciplina epistémica que estos textos piden (separar `dato ≠ interpretación ≠ hipótesis ≠ resultado ≠ causa`; "patrones contextuales, revisables", no "leyes familiares") ya es doctrina vigente: Regla V1.1.1 + ADR-004 ("observaciones primarias, no conclusiones").
- **No existe sistema de gamificación explícito que premie el cumplimiento por sí mismo.** Búsqueda dirigida en `backend/src/main/java`: no hay entidades `Badge`/`Points`/`Level` ni servicio de recompensas. El módulo `participation` ([`ParticipationService`](../../backend/src/main/java/com/integrityfamily/participation/service/ParticipationService.java)) registra actividad y calcula una señal de fatiga (NONE/MILD/HIGH), no puntos ni insignias. El riesgo de Goodhart que motiva la Decisión 4 es, por ahora, un riesgo de diseño a vigilar — no una falla ya presente en el producto.
- **`adherence` (tareas completadas / total) sí existe como métrica calculada — en varios sitios, sin fuente única** ([`CopilotService.java:82`](../../backend/src/main/java/com/integrityfamily/ai/service/CopilotService.java), [`ConvivenceAnalyticsService.java:57`](../../backend/src/main/java/com/integrityfamily/analytics/service/ConvivenceAnalyticsService.java), [`JournalService.java:298`](../../backend/src/main/java/com/integrityfamily/bitacora/service/JournalService.java), [`FamilySkillEngine.java:78`](../../backend/src/main/java/com/integrityfamily/cognitive/service/FamilySkillEngine.java)) y se usa en [`AdaptivePlanService`](../../backend/src/main/java/com/integrityfamily/adaptive/AdaptivePlanService.java#L44-L48) para **reducir** la carga de misiones cuando adherencia < 40%. Es decir: el sistema ya reacciona a adherencia baja disminuyendo exigencia, no premiando adherencia alta con más recompensa — el vector de gaming no está en `adherence` en sí.
- **El ICF no sube por completar misiones.** `LongitudinalStateService` fija `icfCurrent` a partir de `FamilyIcfRecalculatedEvent`, derivado de evaluaciones — no de tareas/checklist. El vector de gaming verificado y concreto está en otro sitio, más angosto: [`MilestoneService.evaluate()`](../../backend/src/main/java/com/integrityfamily/milestone/service/MilestoneService.java#L180-L195) exige simultáneamente ICF≥umbral **y** % de tareas completadas≥umbral para avanzar de hito — ahí sí completar tareas por completarlas mueve una aguja real del sistema (el hito), aunque no mueva el ICF.
- **La Ruta de Conciencia (INCONSCIENTE→PLENO) no tiene sistema de desbloqueo.** `AnalyticsServiceImpl.deriveConsciousnessLevel/Label()` solo mapea rangos de ICF a una etiqueta para dashboards; no hay `if (nivel >= X) unlock(...)`. Confirma que hoy no existe un "juego de niveles" que perseguir — coherente con el bullet de gamificación de arriba.
- **No hay campo de hipótesis/predicción atado a una misión o evidencia individual.** `ChecklistItem` y `SprintMission` no lo tienen; `TaskEvidence` tiene texto libre descriptivo, no estructurado. Lo más cercano — `SprintDaily` (`yesterdayText/todayText/blockagesText/resolutionText`) y `SprintRetrospective` (`whatWentWell/whatWasDifficult/whatLearned/whatToAdjust`) — opera a nivel de ciclo de sprint, no de misión individual. Confirma el mismo hueco que ya registra la Decisión 2 sobre `family_predictions`: el loop PREDECIR→CONTRASTAR no está atado a una unidad de trabajo verificable.
- **La generación de misiones no está cerrada a la IA por diseño.** `PlanGenerationService` genera vía `AiService`, pero `ChecklistController.createItem()` acepta un `source` arbitrario en el body sin validarlo contra un enum cerrado — el modelo de datos ya admite un ítem de origen distinto a IA; falta el flujo de producto (UI, validación, criterio de cuándo ofrecerlo), no el campo.

## Decision

### Decisión 1 — IF-7 se documenta en `vision.md` como marco, sin instrumento nuevo

Se agrega IF-7 a `docs/vision.md` junto a "El eje de regulación", IF-CAM y el Principio de Altura de Observación, con el mapa de cada paso al mecanismo que ya lo cubre. No introduce fases, columnas, tablas ni servicios.

### Decisión 2 — dos huecos verificados quedan registrados, no construidos

1. **`family_predictions` nunca se contrasta.** El loop PREDECIR → CONTRASTAR está abierto: se generan predicciones y no se confrontan contra lo observado. Se registra como deuda conocida. **No se construye** el pre-registro atado a misión ni el servicio de contraste hasta que exista un caso concreto que lo reclame (p. ej. que el piloto V1.2 pida medir precisión anticipatoria más allá de `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS`, ADR-008).
2. **El módulo `lts` es código muerto.** Se registra como candidato a eliminación (schema `V7` + dominio + servicio + repos). **No se cablea** como motor de IF-7 — duplicaría `hypothesis_evidence` y `family_predictions`. La baja efectiva queda para una tarea de limpieza dedicada, no para este ADR.

### Decisión 3 — explícitamente NO se construye

- Ninguna migración, entidad ni servicio nuevo en este ADR.
- Ningún motor de confusores, control estadístico, grupos de control ni aparato tipo RCT por misión. La salida del ciclo es siempre "patrón contextual, probabilístico y revisable".
- Ninguna afirmación neurocientífica promocional derivada de los textos ("una misión aumenta serotonina", "reprograma el cerebro").
- `FamilyCausalEngine` no se renombra (evitar churn); solo se acota en su Javadoc y en `vision.md` que es correlacional con explicabilidad.
- El escalón de "apropiación de habilidad" (Feynman: explicar → transferir → sostener) y el copiloto socrático se posponen: son refinamientos de instrumento, territorio de una eventual V1.3 post-piloto (ADR-011 ya reserva ese espacio), no de ahora.

### Decisión 4 — riesgo de Goodhart (métrica ≠ propósito) se documenta, sin mecanismo de mitigación nuevo

Un segundo texto externo (sobre el "juego amañado" de las métricas de éxito) plantea un riesgo distinto al de IF-7: no que la familia confunda una etiqueta con comprensión, sino que el propio sistema le enseñe a optimizar el indicador en vez del fenómeno — `actividad → medio para conseguir recompensa` en vez de `actividad valiosa`. Aplicado a Integrity: una familia podría aprender a "ganarle" al sistema (completar tareas por completarlas, responder lo socialmente deseable) sin que la convivencia real cambie.

La verificación contra el código (bullets de arriba) acota el riesgo real hoy a un solo punto concreto: `MilestoneService.evaluate()` liga avance de hito a % de tareas completadas. No hay badges, puntos, niveles desbloqueables ni ICF que suba por volumen de misiones. Por eso esto se documenta como **principio de diseño a vigilar hacia adelante** (cualquier métrica nueva de adherencia/cumplimiento que se proponga debe describir el proceso, no convertirse en objetivo que la familia persigue) y **no** como corrección de algo roto — el único vector verificado (`MilestoneService`) es demasiado estrecho para justificar tocarlo sin un caso concreto que lo reclame.

### Decisión 5 — "Autonomía Familiar" queda registrada como hipótesis candidata, no como constructo ni métrica

El mismo texto propone que el éxito de Integrity no sea "la familia usa más la app" sino "la familia necesita cada vez menos indicaciones externas" — una noción de autonomía creciente, con la generación de misiones desplazándose gradualmente de IA hacia familia.

Se verificó que el modelo de datos ya no bloquea un primer paso literal de esto: `ChecklistController.createItem()` acepta `source` arbitrario, sin enum cerrado que fuerce origen-IA. Pero **no existe hoy ningún constructo, medición ni umbral de "autonomía"** — inventar uno ahora repetiría exactamente el error que ADR-004/Regla V1.1.1 existen para evitar (tratar una idea de diseño como si ya fuera una hipótesis medible). Se registra como candidata a `hypothesis_evidence` ([ADR-004](ADR-004-hypothesis-evidence-pattern.md)), mismo estado que hoy tienen CCTF (ver `vision.md`, IF-CAM) y la fila "Convivencia" del eje de regulación: sin instrumento propio, a la espera de un disparador concreto (p. ej. que el piloto V1.2 muestre familias estancadas en dependencia de sugerencias IA pese a ICF estable o ascendente).

## Trade-off Analysis

Frente a **aceptar el borrador completo** (V113 + pre-registro + contraste + apropiación + copiloto socrático): cierra huecos reales, pero cero de sus piezas está construida y ninguna tiene un disparador concreto. Apilarlas sobre ADR-012 recién mergeado, con el banco `SCENARIO_V1_2` congelado, multiplica el trabajo especulativo — exactamente lo que ADR-004 evitó al descartar el bounded context `research` sin un segundo consumidor real.

Frente a **no escribir nada**: se pierde el registro verificado de dos huecos (`family_predictions` sin contraste, `lts` muerto) que de otro modo se redescubrirían, y la integración de IF-7 como marco — barata y consistente con ADR-011/012.

Frente a **construir ahora un mecanismo anti-Goodhart o una métrica de Autonomía Familiar** (Decisiones 4-5): sería repetir el mismo error que este ADR ya evitó una vez con el borrador de predicción/contraste — instrumentar una hipótesis de diseño antes de tener evidencia de que el riesgo se manifestó. La verificación de código muestra que el vector real hoy es estrecho (`MilestoneService`), no generalizado.

Frente a **no registrar el riesgo de Goodhart en absoluto**: se perdería un principio de diseño barato de mantener (cualquier métrica nueva debe describir el proceso, no ser el objetivo) justo antes de una fase (V1.2/V1.3) donde es previsible que se propongan más métricas de cumplimiento.

## Consequences

- **Más fácil:** hay una lectura única del ciclo (IF-7), un registro explícito de dos deudas técnicas verificadas, y un principio de diseño explícito (métrica ≠ propósito) para evaluar cualquier métrica de adherencia/cumplimiento futura contra `MilestoneService` como único precedente real.
- **Más difícil:** nada — este ADR no añade superficie.
- **Habrá que revisitar:** si el piloto V1.2 genera necesidad real de medir predicción vs. resultado por misión, se abre un ADR-013b (o se reactiva el borrador recortado) con la migración y los servicios. Si la limpieza de código muerto se prioriza, `lts` se elimina en su propia tarea. Si el piloto muestra familias dependientes de sugerencias IA pese a ICF estable, se abre una hipótesis formal de Autonomía Familiar en `hypothesis_evidence` (Decisión 5). Si se propone una métrica nueva de cumplimiento, se contrasta primero contra la Decisión 4.

## Action Items

1. [x] `docs/vision.md` — sección "Ciclo IF-7" como marco conceptual (Decisión 1), con el mapa paso→mecanismo, la nota de que `FamilyCausalEngine` es correlacional, no causal, y el principio métrica ≠ propósito (Decisión 4).
2. [ ] Registrar en el backlog técnico: (a) `family_predictions` nunca transiciona de `ACTIVE` — loop PREDECIR/CONTRASTAR abierto; (b) módulo `lts` es código muerto, candidato a baja; (c) `MilestoneService.evaluate()` es el único punto verificado donde completar tareas mueve una aguja del sistema sin pasar por el ICF — vigilar si se propone premiarlo directamente.
3. [ ] (Condicional) Reabrir con migración + servicios solo si el piloto V1.2 reclama medición de precisión anticipatoria por misión.
4. [ ] (Condicional) Abrir hipótesis `AUTONOMY_HYPOTHESIS` en `hypothesis_evidence` solo si el piloto V1.2 muestra evidencia real de dependencia sostenida (Decisión 5) — no antes.
