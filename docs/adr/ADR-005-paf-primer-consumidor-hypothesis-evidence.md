# ADR-005: PAF como primer consumidor real de `hypothesis_evidence`

**Status:** Accepted
**Date:** 2026-07-16
**Deciders:** William Lopez

## Context

ADR-004 estableció el patrón general (`hypothesis_evidence`, append-only, independiente del estado operacional) pero deliberadamente no lo conectó a nada — su Action Item 3 delegó explícitamente "adaptar PAF como primer consumidor real" a este ADR. Sin un consumidor real, el patrón queda como infraestructura vacía, exactamente lo que ADR-004 advirtió en su propia sección de alcance descartado.

PAF (ADR-003) ya calcula, operacionalmente, un streak de ciclos consecutivos con `dimScore >= 90` por dimensión ICF, almacenado en `FamilyLongitudinalState` (`emocionesPlenoStreak`, etc.). Ese streak es el estado operacional correcto para *operar* el producto, pero — por diseño de ADR-003 mismo — no es evidencia: es una fila única sobrescrita en cada ciclo, sin historial.

## Decision

`LongitudinalStateService.onIcfRecalculated()` escribe, además de actualizar el streak operacional, **una fila en `hypothesis_evidence` por cada dimensión que llegue en el evento**, con estos valores:

| Campo | Valor |
|---|---|
| `hypothesis` | `"PAF"` |
| `hypothesis_version` | `"v1"` (la definición de ADR-003: umbral 90, streak 3 — si el umbral cambia, esta versión sube) |
| `subject_type` | `"FAMILY"` |
| `subject_id` | `event.familyId()` |
| `measurement_type` | `"DIM_EMOCIONES"` / `"DIM_COMUNICACION"` / `"DIM_HABITOS"` / `"DIM_TIEMPOS"` — prefijado para no colisionar con `measurement_type` de futuras hipótesis que compartan la tabla |
| `measurement_value` | el **dimScore crudo** (ej. `92.0`), no el streak |
| `instrument` | `"ICF"` |
| `instrument_version` | `"1"` |
| `source` | `AUTOMATIC` |
| `observed_at` | `event.occurredAt()` |

### Por qué se guarda el dimScore crudo, no el streak

Esta es la decisión de diseño central de este ADR, y no es obvia. La alternativa — guardar directamente el valor del streak (ej. `streak=3`) — parecía más cercana al ejemplo ilustrativo de ADR-004 ("ICF=92, streak=3, dimensión=Comunicación"), pero se descarta:

Si `hypothesis_evidence` solo guardara el streak ya calculado, cualquier auditoría futura tendría que **confiar ciegamente** en que `nextPlenoStreak()` lo calculó bien — no habría forma de verificarlo independientemente. Guardando el dimScore crudo de cada ciclo, el streak puede **recalcularse de forma independiente** a partir de la secuencia de filas en `hypothesis_evidence` (contar cuántas observaciones consecutivas cumplen `>= 90`), sirviendo como verificación cruzada contra el contador operacional. Esto no viola el Principio de no reconstrucción de ADR-004 — ese principio prohíbe depender de reconstruir evidencia *desde estado operacional o eventos genéricos*; reconstruir un agregado a partir de las propias filas inmutables de `hypothesis_evidence` es, precisamente, el "análisis posterior" que la tabla existe para habilitar.

### Por qué una fila por dimensión, no una fila por ciclo con 4 campos

El esquema de `hypothesis_evidence` (ADR-004, Decisión 2) es una medición por fila (`measurement_type`/`measurement_value` singular). Cuatro dimensiones en un mismo ciclo de evaluación son cuatro observaciones distintas, no una — se ajustan al esquema tal como está, sin extenderlo.

## Trade-off Analysis

Frente a guardar el streak directamente: se pierde la conveniencia de leer el valor "listo" sin recalcular, pero se gana verificabilidad independiente — y el costo de recalcular un streak sobre unas pocas decenas de filas por familia es trivial.

Frente a no escribir evidencia en absoluto (dejar el Action Item 3 de ADR-004 sin resolver): PAF seguiría siendo, en la práctica, una hipótesis sin mecanismo de validación real — el problema que motivó ADR-004 completo.

## Consequences

- **Más fácil:** PAF por fin tiene un camino real hacia "validado" (ADR-003 lo dejó pendiente de "evidencia real" sin decir dónde viviría) — cuando haya suficiente volumen, se puede consultar `hypothesis_evidence` para responder si el streak de 3 ciclos en ≥90 realmente distingue algo, en vez de solo confiar en el contador operacional.
- **Más difícil:** cada evento `assessment.completed`/`icf.recalculated` ahora escribe hasta 4 filas adicionales — volumen a monitorear si el sistema escala, aunque hoy es insignificante.
- **Habrá que revisitar:** si `hypothesis_version` de PAF cambia (ej. el umbral deja de ser 90, o el streak deja de ser 3), este ADR debe actualizarse o crear uno nuevo — no se cambia el valor `"v1"` sin dejar rastro documental (Action Item de gobernanza, ADR-004 #4).

## Action Items

1. [x] Entidad `HypothesisEvidence` + enum `EvidenceSource` + `HypothesisEvidenceRepository` (`com.integrityfamily.domain`).
2. [x] `LongitudinalStateService.onIcfRecalculated()` escribe una fila por dimensión presente en el evento, vía `recordPafEvidence()`.
3. [x] Tests unitarios agregados a `LongitudinalStateServiceTest`: campos correctos de la fila escrita, dimensión ausente no genera fila, 4 dimensiones → 4 filas, el streak operacional no se ve afectado por la escritura de evidencia. 31/31 pasan.
4. [x] Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa (backend).
