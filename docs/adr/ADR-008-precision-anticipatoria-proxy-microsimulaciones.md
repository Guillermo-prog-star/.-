# ADR-008: `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` — precisión anticipatoria intra-sesión sobre `SCENARIO_V1_2`, conectada a `hypothesis_evidence`

**Status:** Accepted
**Date:** 2026-07-20
**Deciders:** William Lopez

## Context

Surgió una propuesta externa (documento conceptual "IF-TEM", no generado en este repo) que describe un algoritmo de procesamiento fenomenológico de 12 etapas para microsimulaciones, más tres índices nuevos: Índice de Utilidad Cognitiva (IUC), Índice de Precisión Predictiva (IPP) e Índice de Coherencia Personal (ICP). Al contrastarlo contra el código real (no contra la idea en abstracto), se encontró que 5 de las 12 etapas ya están implementadas: el modelo `SCENARIO_V1_2`/`NEURO_AWARENESS` (`V89`–`V95`) ya recorre `NOTICE` (sensación corporal) → `THINK` (pensamiento) → `ACT` (respuesta) → `AFTERMATH` (consecuencia inmediata) → `EFFECT` (impacto estructural), con `rubric_level` 1-5 por opción de respuesta desde `V89__poc_v1_2_parallel.sql`.

De las tres métricas nuevas, **IPP es la única que no requiere instrumentación adicional**: la fase `THINK` ya captura la interpretación anticipatoria de una situación, y la fase `AFTERMATH` ya captura qué ocurrió realmente — ambas ya persistidas hoy en `evaluation_answers` para el mismo `evaluation_id`. IUC (requiere preguntar "utilidad percibida", que no se captura hoy) e ICP (requiere comparar declaración vs. conducta observada en el tiempo) quedan fuera de este ADR — no se descartan, simplemente no tienen datos que los alimenten hoy sin diseño nuevo.

### Por qué el nombre no es "IPP"

Lo que IF-TEM describe en su Etapa 5 es una **predicción prospectiva verificada después**: anticipar algo, dejar pasar tiempo, comprobar si ocurrió. Lo que `THINK`→`AFTERMATH` mide hoy es distinto: ambas respuestas se dan **en la misma sesión de evaluación**, sobre el mismo episodio hipotético o recordado — es un auto-reporte retrospectivo de "qué anticipé" y "qué realmente pasó", no un seguimiento longitudinal de una predicción real verificada con el tiempo. Llamarlo "IPP" a secas prometería más de lo que el dato sostiene. Se registra como `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` — el nombre mismo carga la salvedad epistémica, mismo principio que ya aplicó ADR-007 al rechazar un booleano `PAUSA_DETECTADA` que hubiera prometido más precisión de la que el dato real permite.

### Verificado en este repo, no asumido

- `questions.parent_key` ([`V84__alter_neuro_schema.sql`](../../backend/src/main/resources/db/migration/V84__alter_neuro_schema.sql)) agrupa el quinteto fijo de fases de un mismo escenario; `phase_prompt`/`rubric_level` ([`V89__poc_v1_2_parallel.sql`](../../backend/src/main/resources/db/migration/V89__poc_v1_2_parallel.sql)) separan el prompt clínico del texto de la situación y el nivel metodológico del `score_value`.
- **22 escenarios distintos** (`M-POC-S1` … `M-POC-S22`, sembrados en `V89`, `V90`, `V92`–`V95`) tienen las 5 fases completas (`NOTICE`/`THINK`/`ACT`/`AFTERMATH`/`EFFECT`) — no es un caso aislado de prueba de concepto, es el banco completo.
- `evaluation_answers` ([`EvaluationAnswer.java`](../../backend/src/main/java/com/integrityfamily/domain/EvaluationAnswer.java), constraint única `evaluation_id`+`question_key`) ya persiste el `score` elegido por fase, por evaluación — el dato crudo que este ADR necesita ya existe, sin migración nueva.
- `questions.metadata` (JSON, [`V91__add_scenario_metadata.sql`](../../backend/src/main/resources/db/migration/V91__add_scenario_metadata.sql)) ya contiene un campo `clinical_hypothesis` de ejemplo para `M-POC-S1`: *"Una mayor regulación corporal inicial (NOTICE) se asociará probabilísticamente con una menor disonancia relacional al día siguiente (EFFECT)"* — un par de fases distinto al que usa este ADR, pero confirma que el diseño ya anticipaba inferencia cruzada entre fases del mismo escenario.
- `hypothesis_evidence` ([`V106`](../../backend/src/main/resources/db/migration/V106__hypothesis_evidence.sql), ADR-004) ya gobierna `PAF` (ADR-003/005) y `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` (ADR-007, [`EvaluationService.java:208-220`](../../backend/src/main/java/com/integrityfamily/evaluation/service/EvaluationService.java)) — mismo mecanismo, sin extender el esquema.

## Decision

### Decisión 1 — alcance: `THINK` vs. `AFTERMATH`, nunca `EFFECT`

La comparación es exclusivamente `THINK` (interpretación anticipatoria) contra `AFTERMATH` (qué ocurrió en los minutos siguientes) — nunca contra `EFFECT` (impacto estructural en la relación a más largo plazo). Son constructos distintos: `AFTERMATH` es el desenlace inmediato del mismo episodio, comparable directamente con lo que `THINK` anticipó; `EFFECT` es una consecuencia relacional derivada que pertenece a otra pregunta de investigación (más cercana al territorio ya cubierto por `clinical_hypothesis` de `V91`, o a una hipótesis futura propia). Mezclar ambos en una sola métrica repetiría el error que ADR-007 evitó al separar `pauseCapacity` (dato crudo) de una interpretación prematura.

### Decisión 2 — se guardan dos observaciones crudas, nunca una brecha ya calculada

Mismo principio de ADR-005 (*"se guarda el dimScore crudo, no el streak"*) y de ADR-004 Decisión 2 (*"una fila válida es la medición, no la conclusión"*): por cada episodio (`evaluation_id` + `parent_key`) donde existan respuestas registradas en **ambas** fases `THINK` y `AFTERMATH`, se escriben **dos filas** en `hypothesis_evidence`, no una resta pre-calculada:

| Campo | Fila 1 (anticipación) | Fila 2 (desenlace) |
|---|---|---|
| `hypothesis` | `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` | `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` |
| `hypothesis_version` | `v1` | `v1` |
| `subject_type` | `FAMILY` | `FAMILY` |
| `subject_id` | `familyId` | `familyId` |
| `measurement_type` | `ANTICIPATED_THREAT_LEVEL` | `OUTCOME_SEVERITY_LEVEL` |
| `measurement_value` | `rubric_level` de la opción elegida en `THINK` (1-5) | `rubric_level` de la opción elegida en `AFTERMATH` (1-5) |
| `instrument` | `parent_key` del escenario (ej. `"M-POC-S7"`) | igual |
| `instrument_version` | `pilot_version` de `questions.metadata` si existe, si no `"1.2.0"` | igual |
| `source` | `AUTOMATIC` | `AUTOMATIC` |
| `observed_at` | `existing.getFinalizedAt()` | igual |

**Por qué `instrument` = `parent_key` y no el algoritmo de riesgo** (repropósito deliberado respecto a ADR-007, donde `instrument` = `algorithmVersion`): `hypothesis_evidence` no tiene una columna de "episodio" o "escenario" — sin usar `instrument` para identificar cuál de los 22 escenarios generó la observación, sería imposible reconstruir qué fila de `ANTICIPATED_THREAT_LEVEL` corresponde a qué fila de `OUTCOME_SEVERITY_LEVEL` cuando una familia completa varios escenarios en la misma evaluación. `instrument` ya significa, en ADR-004 Decisión 2, *"con qué instrumento se midió"* — un escenario fenomenológico específico (con su propio `phase_prompt`, su propia intensidad declarada en `metadata.scenario_intensity`) es tan legítimamente "el instrumento de medición" como una versión de algoritmo. El par `(subject_id, instrument, observed_at)` identifica el episodio de forma inequívoca sin agregar columnas nuevas.

### Decisión 3 — guard: ambas fases deben existir, o no se escribe nada

Igual que ADR-007 Decisión 1 (el guard de `neuroProfile() != null` evita escribir `pauseCapacity=0.0` como si fuera ausencia real de pausa): si un episodio solo tiene respuesta en `THINK` pero no en `AFTERMATH` (evaluación incompleta, o el family abandonó el escenario a medio camino), **no se escribe ninguna fila** para ese episodio. Escribir solo la mitad de un par sería una observación sin sentido — no hay "precisión anticipatoria" que medir sin ambos lados.

### Decisión 4 — la predicción prospectiva real queda documentada, no descartada

Si el análisis sobre este proxy sugiere que vale la pena medir precisión anticipatoria *real* (predicción hoy, verificación días después, con una notificación que le pregunte a la familia "¿qué tan preciso fue tu miedo?"), eso requiere diseño nuevo: una tabla de predicciones pendientes, un mecanismo de recordatorio, y una fase de verificación diferida — ninguna de esas piezas existe hoy ni se construye en este ADR. Mismo criterio que ya separó ADR-004 (patrón) de ADR-005 (primer consumidor) y que ADR-007 aplicó al posponer el episodio procesual rico: no se construye la versión más cara antes de tener evidencia de que la versión barata (este proxy) responde algo útil.

## Trade-off Analysis

Frente a guardar una sola fila con la brecha ya calculada (`|THINK − AFTERMATH|`): se pierde la conveniencia de leer un valor "listo", pero se gana la posibilidad de recalcular con fórmulas distintas después (brecha absoluta, brecha direccional, ponderada por `scenario_intensity`) sin haber perdido información en la escritura — mismo argumento que ADR-005 ya hizo para PAF.

Frente a construir la predicción prospectiva real (verificación diferida en el tiempo): se pierde fidelidad al Etapa 5 original del documento IF-TEM, pero se evita construir un pipeline de recordatorios y verificación diferida sin tener todavía una sola fila de evidencia que justifique que vale la pena. El proxy intra-sesión da señal utilizable desde la primera evaluación finalizada después de este cambio.

Frente a no escribir nada y descartar la idea completa: se perdería la única de las tres métricas nuevas de IF-TEM que no requiere instrumentación adicional — el dato ya existe en `evaluation_answers`, sin explotarlo.

## Consequences

- **Más fácil:** `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` tiene datos disponibles desde el primer día de implementación, sin migración nueva, sin cambio de frontend — reutiliza las 22 preguntas del banco `SCENARIO_V1_2` ya sembradas.
- **Más difícil (a diferencia de ADR-007):** este ADR no reutiliza un valor ya calculado por `RiskAlgoV1Engine` — requiere lógica nueva en `EvaluationService` (o un servicio dedicado) que agrupe `evaluation_answers` por `parent_key` vía join con `questions`, detecte pares `THINK`+`AFTERMATH` completos, y escriba las dos filas correspondientes. No es "gratis" como lo fue conectar `pauseCapacity`.
- **Habrá que revisitar:** si el análisis sobre las primeras filas muestra que la brecha `THINK`/`AFTERMATH` correlaciona con algo relevante (riesgo, ICF, dimensión crítica) pero la fidelidad prospectiva importa para la pregunta de investigación, retomar la Decisión 4 (predicción real verificada en el tiempo) en un ADR futuro.
- **Habrá que revisitar:** si `rubric_level` deja de ser la unidad de medida (ej. se introduce una escala distinta para `THINK`/`AFTERMATH`), subir `hypothesis_version` a `v2` y documentarlo aquí — no se cambia silenciosamente (gobernanza fijada en ADR-004 Decisión 2).

## Action Items

1. [x] Registrado `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` / `v1` en la tabla de "Registro de hipótesis activas" de [ADR-004](./ADR-004-hypothesis-evidence-pattern.md).
2. [x] `recordPredictiveAccuracyEvidence()` agregado en `EvaluationService`, llamado **después** de `evaluationRepository.save(existing)` (no junto al bloque `neuroProfile`, ver nota de implementación abajo): agrupa las respuestas por `parent_key` vía `questionRepository.findAllById(...)`, detecta los pares `THINK`+`AFTERMATH` completos, y escribe las dos filas de `hypothesis_evidence` por episodio (Decisión 2), respetando el guard de la Decisión 3. Sin guard de idempotencia ante re-finalización — mismo perfil de riesgo ya aceptado en `recordPauseCapacityEvidence` (ADR-007); si aparece evidencia real del problema, se corrige una vez a nivel de `finalize()` para las tres hipótesis, no aquí.
3. [x] Tests unitarios en `EvaluationServiceTest$FinalizePredictiveAccuracyEvidence`: par completo escribe exactamente 2 filas con los campos correctos; falta `AFTERMATH` → no se escribe nada; dos escenarios completos en la misma evaluación escriben 4 filas, cada una atribuida a su `parent_key` vía `instrument`. 3/3 pasan (15/15 en `EvaluationServiceTest` completo, sin regresión).
4. [x] Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa del backend (2099/2099), mismo patrón que ADR-005/007.

### Nota de implementación — por qué después del `save`, no antes

`EvaluationService` no tiene `@Transactional`. En el flujo clásico (respuestas en el body del request), las nuevas `EvaluationAnswer` solo existen en memoria (`existing.getAnswers()`, cascade) hasta que se llama `evaluationRepository.save(existing)`. Colocar la llamada antes de esa línea (como sugería una versión anterior de este documento) habría consultado `evaluation_answers` sin ver las respuestas recién enviadas en ese flujo — fallando en silencio, sin excepción, simplemente sin escribir evidencia. Corregido antes de implementar, no después.
