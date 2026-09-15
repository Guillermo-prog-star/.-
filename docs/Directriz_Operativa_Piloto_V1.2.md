# Directriz Operativa — Etapa 4 (continuación): Piloto con Familias V1.2

**Fecha:** 2026-07-20
**Actualiza el congelamiento de instrumento de:** [`Directriz_Operativa_Piloto_V1.1.md`](./Directriz_Operativa_Piloto_V1.1.md) (2026-07-05)

## Contexto — por qué esta actualización era necesaria

La Directriz V1.1 declaró abierta la Etapa 4 el 2026-07-05 y congeló el banco de escenarios en esa versión, con una regla explícita: *"No se crearán nuevos escenarios"* mientras el piloto estuviera en curso, y toda evidencia debía registrarse en `scenario_validation_log`.

Verificado contra el código real, no contra la intención documentada:

- `scenario_validation_log` **nunca se construyó** — ninguna migración crea esa tabla, ningún servicio la referencia.
- El banco **no permaneció congelado**: las migraciones `V89`–`V95`, posteriores a la apertura del piloto, agregaron **22 escenarios nuevos** (`SCENARIO_V1_2`, `M-POC-S1`…`M-POC-S22`) bajo un modelo estructuralmente distinto al vigente en V1.1 — `ENTRY/TIMING/ACTION` pasó a `NOTICE/THINK/ACT/AFTERMATH/EFFECT`.
- Lo que sí se construyó en su lugar es `hypothesis_evidence` (ADR-004) — un mecanismo más general y mejor diseñado, pero bajo otro nombre, sin que ningún documento declarara que sustituía al designado originalmente en V1.1.

Esta directriz no revierte nada de lo ya construido — sería descartar trabajo real para volver a un estado que, de todas formas, nunca se sostuvo en la práctica. En cambio, formaliza el estado actual como la nueva línea base y cierra la ambigüedad antes de reclutar o continuar con familias.

## Decisión 1 — V1.2 es la nueva línea base congelada

El instrumento a congelar durante lo que resta de la Etapa 4 es **V1.2**: los 22 escenarios `SCENARIO_V1_2` sembrados en `V89`–`V95`, bajo el modelo `NOTICE → THINK → ACT → AFTERMATH → EFFECT`. V1.1 (`ENTRY/TIMING/ACTION`) queda como versión histórica del instrumento, no como línea base activa de piloto.

Se mantienen, sin cambio, las reglas de gestión de cambios ya fijadas en V1.1 ("Gestión de cambios"), aplicadas ahora sobre V1.2:

- No se editará directamente el banco de escenarios `SCENARIO_V1_2`.
- No se eliminarán escenarios de los 22 existentes.
- No se crearán escenarios nuevos.
- No se modificarán escalas (`rubric_level`/`score_value`) ni las fases de cada escenario.
- No se alterará la lógica diagnóstica (`RiskAlgoV1Engine`).

Todo cambio propuesto durante el piloto queda registrado para evaluación posterior — igual que en V1.1, registrar un hallazgo no implica modificar el instrumento.

## Decisión 2 — qué mecanismo de evidencia existe realmente, y qué cubre cada uno

`scenario_validation_log` no se reconstruye. Esta directriz reconoce, en su lugar, dos mecanismos con alcances distintos y complementarios — y es explícita sobre lo que ninguno de los dos cubre, para no repetir la ambigüedad que dejó V1.1.

### 2.1 Evidencia cuantitativa automática — `hypothesis_evidence`

Cada evaluación finalizada con preguntas `SCENARIO_V1_2` ya escribe, sin intervención manual del facilitador, filas en `hypothesis_evidence` (ADR-004):

| Hipótesis | Qué mide del instrumento |
|---|---|
| `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` (ADR-008) | Si la interpretación anticipatoria (`THINK`) de un escenario se corresponde con el desenlace declarado (`AFTERMATH`) — evidencia directa de si ese escenario genera respuestas calibradas o desconectadas de lo que la familia anticipa. |
| `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` (ADR-007) | Si se seleccionan opciones `vector_tag = PAUSE` — evidencia de si el escenario activa el componente reflexivo que se propuso medir. |

Esto ya es evidencia real sobre el comportamiento del instrumento, corriendo hoy, sin necesidad de `scenario_validation_log`. No requiere ninguna acción adicional del facilitador durante la sesión — es automático, por cada evaluación finalizada.

### 2.2 Evidencia cualitativa de sesión — sin mecanismo digital todavía

Lo que V1.1 exigía por sesión (facilitador responsable, familia, número de integrantes, incidencias técnicas, observaciones metodológicas, retroalimentación espontánea) y los criterios de hallazgo que definía (escenario ambiguo, confusión frecuente, fatiga, comportamiento inesperado, reflexión especialmente significativa) **no tienen hoy ningún mecanismo digital construido** — ni lo tenían en V1.1 (`scenario_validation_log` nunca existió), ni lo tiene V1.2.

Construir una tabla nueva para esto ahora, antes de que el piloto arranque y sin conocer el volumen real de sesiones, repetiría el error que este mismo proyecto ya evitó varias veces (Regla V1.1.1 aplicada a sí misma: no se construye captura para una necesidad todavía no validada — mismo criterio que pospuso IUC/ICP y la predicción prospectiva real en ADR-008).

**Registro interino:** durante el piloto, cada sesión se documenta en una planilla externa (hoja de cálculo compartida, fuera de la aplicación), con exactamente los campos ya definidos por V1.1 en su sección "Registro obligatorio por sesión". Si el volumen de sesiones o la necesidad de análisis cruzado con `hypothesis_evidence` lo justifica más adelante, se diseña entonces una tabla real, con su propio ADR — no antes.

## Decisión 3 — la unidad de observación sigue siendo el escenario

Sin cambio respecto a V1.1: la unidad principal de análisis es el escenario (`parent_key`), no la pregunta individual. En V1.2 esto se traduce directamente en `M-POC-S1`…`M-POC-S22` como unidades — el mismo `parent_key` que ya usa `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` como valor de `instrument` (ADR-008, Decisión 2). Cruzar la planilla de sesiones (2.2) con `hypothesis_evidence` (2.1) para un mismo `parent_key` es, hoy, un análisis manual — no automatizado.

## Cierre del piloto

Sin cambios respecto a V1.1: al finalizar la recolección, análisis integrado de la evidencia cuantitativa (`hypothesis_evidence`) y cualitativa (planilla de sesiones, sección 2.2), y solo entonces decisión de qué escenarios de los 22 permanecen sin cambios, requieren ajustes, deben eliminarse, o ameritan una V1.3. La evolución del instrumento sigue sustentada exclusivamente en evidencia recolectada durante el piloto, con trazabilidad completa entre observación de campo, análisis y decisión de diseño.
