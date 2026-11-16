# ADR-020: Desacoplar los efectos posteriores a `EvaluationService.finalize()` (evento después del commit y pipeline de pasos)

## Estado

Aprobado y aplicado (2026-11-16) para los cortes **A** y **B**. El corte **C** queda diferido hasta resolver la validación pendiente V1.

## Contexto

`EvaluationService` es el motor del ICF. Una medición del 2026-11-16 sobre
`backend/src/main/java` lo ubica como el servicio de dominio con más
dependencias inyectadas: **27** (el siguiente, `ContextSynthesizer`, tiene 20)
y 637 líneas.

`finalize()` ejecuta, dentro de **una sola transacción**, tres trabajos: persistir
el diagnóstico, ejecutar el motor de reglas (IF-CIS/IF-REE/IF-ALT) y disparar
más de una docena de efectos secundarios (`processPostFinalization`): snapshot de
riesgo, sincronización longitudinal, hito, misiones, memoria cognitiva,
habilidades, grafo de identidad, narrativa, reflexión, analítica, notificación y
evento `evaluation.completed`.

### Lo que ya existe (verificado en el código antes de diseñar)

| Hecho | Evidencia |
|---|---|
| 27 dependencias inyectadas | `EvaluationService.java:54-80` |
| `aiService` y `planGenerationService` no se invocan (0 usos) | grep sobre el archivo |
| No existe `channelTransacted` en el backend: `rabbitTemplate.convertAndSend` sale en el acto, antes del commit de `finalize()` | grep en `backend/src/main/java` |
| El consumidor del evento resuelve la evaluación con `findById(evaluationId).orElseThrow(EVALUATION_NOT_FOUND)` | `PlanGenerationService.java:108` |
| El patrón correcto ya existe en el proyecto: `TransactionSynchronization.afterCommit` | `TaskEvidenceService.java:108` |
| Los 13 colaboradores tienen métodos `@Transactional` (REQUIRED): se unen a la transacción de `finalize()` | grep; método exacto confirmado en `FamilySkillEngine.analyze` (l. 54) |
| No hay dependencia circular: solo 3 controladores y `SentinelSimulationService` inyectan `EvaluationService` | grep |
| `FamilyIcfRecalculatedEvent` solo lo publica `CrisisServiceImpl`; la evaluación llama directo a `syncFromSnapshot` | grep |
| Hay 3 clases de test sobre el servicio; una sola assertion depende de un colaborador a mover (`EvaluationServiceConsciousTest:127`) | lectura de los tests |

### Lo que falla

1. **Evento antes del commit.** El consumidor puede ejecutarse antes de que la
   evaluación exista para otra transacción (carrera). Además el evento sale
   aunque la transacción termine en rollback: queda un evento de una evaluación
   que no existe.
2. **Aislamiento aparente.** Los `catch (Exception)` prometen "no bloqueante",
   pero un colaborador `@Transactional` que lanza marca la transacción compartida
   como `rollback-only`; el commit de `finalize()` lanzaría
   `UnexpectedRollbackException`. (Inferencia por semántica de Spring; no
   observada en este proyecto.)
3. **Clase de Dios.** Agregar un efecto obliga a editar `EvaluationService`; 27
   mocks encarecen su test.
4. **Higiene.** 2 dependencias muertas; `"RISK_ALGO_V1"` repetido; un comentario
   (l. 257–260) afirma "sin @Transactional" sobre un método que sí lo tiene.

## Decisión

### Corte A — Evento después del commit

- Nueva utilidad `common.event.AfterCommit.run(Runnable)`: registra la acción como
  `afterCommit` cuando hay sincronización transaccional activa; si no la hay, la
  ejecuta de inmediato (compatible con tests unitarios y con llamadas sin
  transacción).
- `EvaluationService` publica `evaluation.completed` mediante `AfterCommit`.
  Sigue siendo no bloqueante: un fallo de publicación se registra y no se
  propaga.
- Se eliminan `AiService` y `PlanGenerationService` (inyectados, nunca usados).

### Corte B — Pipeline de pasos post-finalización

- `PostFinalizationStep` (nombre + `execute(ctx)`), `PostFinalizationContext`
  (evaluación, resultado del algoritmo —`null` en simulación— y `riskLevel`
  propagable) y `PostFinalizationPipeline`, que ejecuta los pasos en orden
  (`@Order`) y aísla cada fallo con el mismo `try/catch` no bloqueante de hoy.
- Cada bloque actual de `processPostFinalization` pasa a un paso con el mismo
  cuerpo, **en el mismo orden y en la misma transacción**. `riskLevel` se
  propaga por el contexto, como hoy.
- Orden: 10 riesgo + sincronización longitudinal · 20 hito · 30 misiones de
  diagnóstico · 40 memoria · 50 habilidades · 60 grafo de identidad · 70
  narrativa · 80 reflexión · 90 analítica de progreso · 100 notificación · 110
  evento `evaluation.completed`.
- `EvaluationService` queda con 14 dependencias (27 − 2 muertas − 12 que se
  mudan + el pipeline).

## Criterios de aceptación

- **AC1.** Si `finalize()` hace rollback, no se publica `evaluation.completed`.
- **AC2.** Con commit exitoso, el evento se publica exactamente una vez y
  después del commit; antes del commit no se publica.
- **AC3.** El orden de los pasos y el `riskLevel` propagado a notificación y
  evento son idénticos a los de hoy (test con `InOrder`).
- **AC4.** `EvaluationService` queda con 14 dependencias o menos y sin
  `AiService` ni `PlanGenerationService`.
- **AC5.** Cada paso nuevo tiene su test; el umbral de líneas de JaCoCo (65 %)
  se mantiene.
- **AC6.** Agregar un efecto es agregar una clase, sin tocar `EvaluationService`.

## Alcance explícitamente excluido

- **Aislamiento transaccional real (Corte C).** Los pasos siguen en la
  transacción de `finalize()`. Un `REQUIRES_NEW` dentro de `finalize()` no sirve:
  la evaluación aún no está confirmada y otra transacción no la vería (FK).
  La solución —evento de dominio con `@TransactionalEventListener(AFTER_COMMIT)`
  y una transacción por paso— cambia qué efectos son síncronos para el cliente.
  Queda diferida a V1.
- El bloque IF-CIS / IF-REE / IF-ALT (`finalize()`, tras el `save`). Comparte
  datos (`algo`, `activations`) entre sus tres llamadas; se rediseña aparte.
- `processSimulatedResult`: solo se adapta a la nueva firma de
  `processPostFinalization`; su comportamiento no cambia (ver V3).
- Cualquier cambio en el cálculo del ICF, en `RISK_ALGO_V1` o en el contrato de
  la API.

## Consecuencias

- `evaluation.completed` ya no puede adelantarse al commit ni sobrevivir a un
  rollback. Los consumidores (`PlanGenerationService`, `AnalyticsEventConsumer`,
  `DashboardWebSocketConsumer`, inferencia IA) son asíncronos, así que solo
  cambia el instante de la publicación.
- El comportamiento de los efectos (orden, transacción, aislamiento por
  `try/catch`) no cambia en B. **El riesgo `rollback-only` (falla 2) permanece**
  hasta el corte C.
- Los tests que hoy dependían de colaboradores nulos tragados por los `catch`
  (p. ej. `EvaluationServiceConsciousTest`) deben ajustarse: la assertion sobre
  `planTaskService` pasa al test de su paso.
- Aumenta el número de clases (11 pasos) a cambio de que cada una sea testeable
  sin los otros 26 colaboradores.
- **Cambio de logs (único delta observable de B además del momento del evento).**
  Los fallos por paso se registran ahora con un formato único,
  `[EVALUATION][NOMBRE_PASO] Paso omitido (no bloqueante…)`, siempre en nivel
  ERROR. Antes cada bloque tenía su propio texto y dos de ellos (hito y
  notificación) usaban WARN. Cualquier alerta o búsqueda de logs basada en esos
  textos antiguos debe actualizarse.
- `EvaluationService` quedó con 14 dependencias (AC4) y los pasos usan el mismo
  cuerpo que los bloques originales.

## Resultado de la implementación

| Criterio | Cómo se verifica |
|---|---|
| AC1, AC2 | `EvaluationServiceEventTimingTest` (finalize() completo con pipeline real y transacción simulada). Se comprobó por mutación que **falla** si `AfterCommit` ejecuta de inmediato. |
| AC3 | `PostFinalizationPipelineTest`: orden por `@Order`, orden de inyección en un contexto Spring real y propagación de `riskLevel` de snapshot a notificación y evento. |
| AC4 | 14 campos `private final` en `EvaluationService`; sin `AiService` ni `PlanGenerationService`. |
| AC5 | `PostFinalizationStepsTest`: un `@Nested` por paso. |
| AC6 | `PostFinalizationPipelineTest.aNewStepIsPickedUpWithoutChangingAnythingElse`. |

Archivos: `common/event/AfterCommit.java` y el paquete
`evaluation/service/post/` (14 clases) en `backend/src/main/java/com/integrityfamily/`.

**Sigue abierto, sin cambio:** la falla 2 (transacción compartida / `rollback-only`),
el bloque IF-CIS/IF-REE/IF-ALT dentro de `finalize()` y V1–V3.

## Validación pendiente (Regla V1.1.1)

- **V1.** ¿Qué efectos necesita el cliente de forma síncrona justo después de
  `POST /evaluations/{id}/finalize` (hito, misiones, estado longitudinal)?
  Bloquea el corte C.
- **V2.** ¿Hay `EVALUATION_NOT_FOUND` o mensajes en DLQ en los logs reales? Si
  los hay, confirma la carrera del hecho 3 de "Lo que falla". Prod corre desde
  una imagen de Docker Hub desconectada de este repo.
- **V3.** `processSimulatedResult` guarda una `Evaluation` FINALIZED sobre el
  `familyId` recibido y corre toda la cadena cognitiva. ¿Solo se usa con
  familias sandbox?
- **V4.** Resuelto al redactar: de los 3 tests de `EvaluationService`, solo
  `EvaluationServiceConsciousTest:127` verifica una interacción que se muda a
  un paso.
