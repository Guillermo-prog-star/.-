# ADR-007: `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` — conectar `pauseCapacity` (ya existente) a `hypothesis_evidence`

**Status:** Accepted
**Date:** 2026-07-18
**Deciders:** William Lopez

## Context

Surgió una propuesta para evolucionar las microsimulaciones (`SCENARIO_V1_2`, V89-V95) de medir solo el resultado de un escenario a modelar el proceso interno completo: interpretación → impulso → pausa → elección → resultado. La primera formulación (arquitectura de 9 componentes, vector `TransformationalCapabilityVector`, rediseño completo de fases) se descartó por desproporcionada frente a la Regla V1.1.1 — mismo principio que ya gobernó ADR-004 al descartar el bounded context `research` completo.

Una segunda formulación (mini-vector procesual: `interpretation → impulse → pause → choice → proximalOutcome`, tabla nueva `ProcessEpisodeEvidence`) corrigió el reduccionismo de mi primera contrapropuesta (un booleano `PAUSA_DETECTADA`), pero al diseñarla se encontró algo que invalida su premisa central: **no hace falta construir captura nueva, porque ya existe.**

### El hallazgo: `pauseCapacity` ya está calculado, versionado y persistido en producción

Verificado en este repo, no asumido:

- `RiskAlgoV1Engine.compute()` ([`RiskAlgoV1Engine.java:161-169,218,231-238`](../../backend/src/main/java/com/integrityfamily/risk/service/RiskAlgoV1Engine.java)) recorre las respuestas de tipo `NEURO_AWARENESS`/`SCENARIO_V1_2`, ubica la `QuestionOption` cuyo `score_value` coincide con la respuesta, y cuenta cuántas tienen `vector_tag = 'PAUSE'`. El resultado (×10, capado en 100) se arma en `NeuroProfile.pauseCapacity`.
- `EvaluationService.finalize()` ([línea 202-209](../../backend/src/main/java/com/integrityfamily/evaluation/service/EvaluationService.java)) ya lo persiste: `if (algo.neuroProfile() != null) { ... existing.setPauseCapacity(algo.neuroProfile().getPauseCapacity()); ... }` → columna `evaluations.pause_capacity`, existente desde `V84__alter_neuro_schema.sql`.
- El banco de preguntas ya tiene **22 opciones reales** con `vector_tag='PAUSE'`, sembradas en `V85__seed_neuro_phenomenological_pilot_clusters.sql` y `V86__seed_neuro_master_bank_v1.sql` — no es un concepto vacío.
- Cada evaluación ya registra `algorithm_version` (ej. `"RISK_ALGO_V1"`, `EvaluationService.java:137`) — la gobernanza de versión de instrumento que la Alternativa B pedía ya existe, sin costo adicional.

Esto vuelve innecesario todo lo que ADR-007 diseñaba originalmente para *capturar* el dato: dos migraciones nuevas, una tabla nueva, cambios de frontend. El dato ya se calcula, en cada evaluación que incluya preguntas del banco neuro-conductual. Lo único que falta es exactamente lo que ADR-005 ya resolvió una vez para PAF: **conectar un valor operacional ya calculado a `hypothesis_evidence`**, para que deje de perderse en cuanto la fila de `Evaluation` se considere "vieja" — hoy `pauseCapacity` sobrevive porque `Evaluation` no se sobrescribe en la práctica, pero ADR-004 Decisión 1 es deliberadamente incondicional: un estado operacional no es evidencia *sin importar si hoy se conserva o no* — no depende de la casualidad de que nadie borre evaluaciones viejas.

## Decision

### Decisión 1 — mismo patrón exacto de ADR-005 (PAF), no una tabla nueva

`EvaluationService.finalize()` escribe, junto a donde ya persiste `pauseCapacity` (línea 207), **una fila en `hypothesis_evidence`** — solo dentro del mismo bloque `if (algo.neuroProfile() != null)`, nunca fuera de él:

| Campo | Valor |
|---|---|
| `hypothesis` | `"DELIBERATIVE_INTERRUPTION_HYPOTHESIS"` |
| `hypothesis_version` | `"v1"` (definición: `pauseCapacity` tal como lo calcula `RiskAlgoV1Engine` hoy — conteo de opciones `vector_tag=PAUSE` seleccionadas, ×10, capado en 100. Si esa fórmula cambia, esta versión sube) |
| `subject_type` | `"FAMILY"` |
| `subject_id` | `existing.getFamily().getId()` |
| `measurement_type` | `"PAUSE_CAPACITY"` |
| `measurement_value` | `algo.neuroProfile().getPauseCapacity()` — el valor crudo 0-100, no una interpretación de "hubo pausa" |
| `instrument` | `existing.getAlgorithmVersion()` (ej. `"RISK_ALGO_V1"`) |
| `instrument_version` | `"1"` |
| `source` | `AUTOMATIC` |
| `observed_at` | `existing.getFinalizedAt()` |

No se crea tabla nueva, no se crea migración de captura, no se toca el frontend. `microsimulation_process_episodes` (diseño de la versión anterior de este ADR) se descarta — el `if (algo.neuroProfile() != null)` existente ya es el guard correcto: sin él, evaluaciones sin preguntas neuro-conductuales escribirían `pauseCapacity=0.0` (primitivo `double`, nunca `null` — `NeuroProfile.java:22`) como si fuera una observación real de ausencia de pausa, cuando en realidad es "no se preguntó". Ese guard evita exactamente el mismo tipo de error que motivó rechazar el booleano `PAUSA_DETECTADA` en la primera versión de este ADR: una fila que parece medición pero es artefacto de la ausencia de dato.

### Decisión 2 — se mantiene el rechazo al booleano de "pausa detectada"

`measurement_value` sigue siendo el score crudo 0-100 (`pauseCapacity`), no un booleano derivado. La interpretación de qué umbral de `pauseCapacity` constituye "hay interrupción deliberativa" queda para el análisis posterior sobre las filas de `hypothesis_evidence` — no se decide ni se congela al momento de escribir la fila, mismo principio ya aplicado en ADR-004 Decisión 2 y en ADR-005 (dimScore crudo, no el streak ya calculado).

### Decisión 3 — el episodio procesual rico queda documentado, no descartado

El mini-vector `interpretation → impulse → pause → choice → proximalOutcome` con captura por episodio (no por evaluación agregada) sigue siendo válido como evolución futura — permite análisis causal más fino que un agregado por evaluación. Se pospone explícitamente: no se construye la tabla nueva, el evento nuevo, ni los cambios de frontend hasta que el análisis sobre `PAUSE_CAPACITY` (este ADR) muestre que el agregado no alcanza para responder la pregunta de investigación. Mismo criterio que ya separó ADR-004 (patrón) de ADR-005 (primer consumidor real): no se construye el segundo nivel de detalle antes de tener evidencia acumulada del primero.

## Trade-off Analysis

Frente al diseño anterior de este mismo ADR (tabla `microsimulation_process_episodes` + dos migraciones + cambios de frontend): se pierde la posibilidad de análisis causal episodio-a-episodio (qué eligió la familia exactamente, en qué escenario, con qué resultado inmediato) — solo queda un agregado por evaluación. Se gana: cero migraciones de captura nuevas, cero cambios de frontend, datos reales desde la primera evaluación que se finalice después de este cambio, en vez de datos que empezarían a existir solo después de que alguien complete el pipeline completo de captura.

Frente a no escribir nada y seguir citando `pauseCapacity` como si ya fuera evidencia por estar en `Evaluation`: se violaría directamente ADR-004 Decisión 1 — el hecho de que `Evaluation` no se sobrescriba hoy es una casualidad de implementación, no una garantía. Si en el futuro alguna migración de limpieza purga evaluaciones antiguas, o `RiskAlgoV1Engine` cambia de versión y recalcula in situ, la "evidencia" implícita en `evaluations.pause_capacity` desaparecería sin dejar rastro — exactamente el problema que `FamilyLongitudinalState` tenía antes de ADR-004/005.

## Consequences

- **Más fácil:** `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` tiene evidencia real desde el primer día de implementación — sin esperar ningún pipeline de captura nuevo, porque el dato de origen ya existe y ya se calcula en cada evaluación relevante.
- **Más fácil (adicional):** cualquier evaluación histórica que aún conserve su fila en `evaluations` con `pause_capacity` poblado podría, opcionalmente, backfillearse a `hypothesis_evidence` en un script one-off — no obligatorio, pero disponible porque el dato de origen no se pierde retroactivamente.
- **Más difícil:** el agregado por evaluación no permite responder preguntas causales finas ("¿qué opción específica eligió, en qué escenario, llevó a qué resultado?") — solo correlación entre `pauseCapacity` y otras variables ya existentes (ICF, riesgo, dimensión crítica) a nivel de evaluación completa.
- **Habrá que revisitar:** si el análisis sobre las primeras filas de `PAUSE_CAPACITY` sugiere que el agregado sí correlaciona con algo relevante pero no alcanza a explicar *por qué*, retomar el diseño de episodio procesual rico (Decisión 3) en un ADR-008 — con `microsimulation_process_episodes` como punto de partida ya documentado en el historial de este ADR.
- **Habrá que revisitar:** si `RiskAlgoV1Engine` cambia la fórmula de `pauseCapacity` (otros `vector_tag`, otro factor de escala), subir `hypothesis_version` a `"v2"` y documentar el cambio aquí — no se cambia silenciosamente (gobernanza fijada en ADR-004 Decisión 2).

## Action Items

1. [x] Constantes `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` / `"v1"` y método privado `recordPauseCapacityEvidence(...)` agregados en `EvaluationService`, siguiendo la forma de `LongitudinalStateService.recordPafEvidence()` ([`EvaluationService.java:375-388`](../../backend/src/main/java/com/integrityfamily/evaluation/service/EvaluationService.java)).
2. [x] Llamada agregada dentro del `if (algo.neuroProfile() != null)` de `EvaluationService.finalize()`, junto a `existing.setPauseCapacity(...)` — nunca fuera del guard.
3. [x] `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` registrado en la tabla de "Registro de hipótesis activas" de [ADR-004](./ADR-004-hypothesis-evidence-pattern.md).
4. [x] Tests unitarios en `EvaluationServiceTest$FinalizePauseCapacityEvidence`: fila escrita con los campos correctos cuando `neuroProfile != null`; **ninguna fila escrita** cuando `neuroProfile == null` — el caso negativo más importante, dado el bug que evita (Decisión 1). 12/12 tests pasan (2 nuevos + 10 existentes, sin regresión).
5. [x] Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa del backend (2099/2099), mismo patrón que ADR-005.
