# ADR-010: `PlanAcceptanceStatus` — declaración de intención (Fase 0b) sobre `ImprovementPlan`

**Status:** Accepted
**Date:** 2026-07-25
**Deciders:** William Lopez

## Context

Al auditar, fase por fase, en qué interfaz se reporta cada etapa del documento externo ya mapeado en [ADR-009](./ADR-009-panel-biologico-recovery-index-hypothesis.md) (Fase 0 → `evaluation`/ICF, Fase 1 → `pauseCapacity`, Fase 2 → `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS`, Fase 3 → `bitacora`, Fase 4 → `vitality`, Fase 5 → `council`/`chat`, Fase 6 → `errorprotocol`, Fase 7 → `SprintRetrospective`), aparecieron dos huecos reales, no supuestos:

1. **Fase 4** (base biológica) tiene backend completo (ADR-009) pero cero interfaz de frontend — fuera de alcance de este ADR, queda para una tarea separada.
2. **Fase 0b** ("priorización + declaración de intención" del documento original) no tiene ningún equivalente, ni de backend ni de frontend. Este ADR resuelve el punto 2.

### Verificado en este repo, no asumido

- `PlanGenerationService.processHybridPlan()` ([`PlanGenerationService.java:214`](../../backend/src/main/java/com/integrityfamily/plan/service/PlanGenerationService.java#L214)) persiste el `ImprovementPlan` generado por IA en cuanto la IA responde — `planService.createPlan(p)` se ejecuta sin ningún paso de confirmación por parte de la familia.
- `ImprovementPlan` ([`ImprovementPlan.java`](../../backend/src/main/java/com/integrityfamily/domain/ImprovementPlan.java)) no tiene ningún campo `status`, `accepted` ni `confirmedAt`.
- Sí existe un mecanismo de aprobación, pero para *ajustes posteriores* del plan, no para el plan inicial: `PlanAdjustment` ([`PlanAdjustment.java:39-52`](../../backend/src/main/java/com/integrityfamily/domain/PlanAdjustment.java#L39-L52)) tiene `AdjustmentStatus status` (`PROPOSED, APPROVED, APPLIED, REJECTED, REVERTED`), `approvedAt`, `approvedBy`, expuesto vía `POST /api/adaptive-plans/adjustment/{id}/approve` ([`AdaptivePlanController.java:33-39`](../../backend/src/main/java/com/integrityfamily/plan/controller/AdaptivePlanController.java#L33-L39)).
- No existe ningún campo de "prioridades" elegidas por la familia. Lo único parecido es `criticalDimension` en `Evaluation` ([`Evaluation.java:67`](../../backend/src/main/java/com/integrityfamily/domain/Evaluation.java#L67)) — un valor **calculado** por `RiskAlgoV1Engine`, nunca elegido por el usuario.
- El ICF tiene exactamente **4 dimensiones** (emociones, comunicación, hábitos, tiempos). Pedirle a la familia que "elija 3 de 4" no discrimina nada que `criticalDimension` no sepa ya — mismo argumento que [ADR-003](./ADR-003-identidad-familiar-como-patron-inferido.md) ya usó para preferir inferencia sobre autorreporte cuando el autorreporte no aporta señal nueva.
- Invariante ya existente y reutilizable: **solo 1 plan activo por familia** — de-duplicación estricta en `PlanGenerationService.java:150-156` (`planRepository.deleteAll(existingPlans)` antes de persistir el nuevo).

## Decision

### Decisión 1 — alcance: confirmación del plan, no selector de prioridades

La "declaración de intención" **no** es un formulario donde la familia elige áreas críticas — eso ya lo calcula `criticalDimension` sobre un total de 4 dimensiones, y agregar una elección manual sería redundante (Decisión ya tomada en ADR-003, aplicada aquí en el mismo sentido). Es, en cambio, **el paso de confirmación que hoy no existe**: la familia acepta explícitamente el plan que la IA ya generó, antes de que cuente como "en curso".

### Decisión 2 — extender `ImprovementPlan` directamente, no una tabla nueva

A diferencia de `PlanAdjustment` (que modela un historial de múltiples propuestas de ajuste a lo largo del tiempo, una por evento de riesgo o inactividad), la declaración de intención ocurre **una sola vez por plan** — y la invariante "solo 1 plan activo por familia" (`PlanGenerationService.java:150-156`) ya garantiza que no hay historial que preservar. Se agregan columnas nuevas a `plans`, no una entidad nueva:

| Campo | Tipo | Notas |
|---|---|---|
| `acceptance_status` | `VARCHAR(20) NOT NULL DEFAULT 'PROPOSED'` | Enum nuevo `PlanAcceptanceStatus { PROPOSED, ACCEPTED }` — deliberadamente sin `REJECTED`/`REVERTED` (ver Trade-off) |
| `accepted_at` | `DATETIME NULL` | |
| `accepted_by` | `VARCHAR(120) NULL` | Mismo patrón que `approved_by` de `PlanAdjustment.java:51-52` — email, no FK |
| `intention_statement` | `TEXT NULL` | Motivación breve, opcional (Decisión 3) |

**Por qué no se enruta por `hypothesis_evidence`** (a diferencia de ADR-007/008/009): `acceptance_status` no es una hipótesis en validación — es estado operacional plano. La familia aceptó el plan o no; no hay incertidumbre epistémica que resolver con evidencia longitudinal. Disfrazarlo de evidencia violaría, en sentido inverso, la misma distinción que ADR-004 Decisión 1 ya fija entre estado operacional e hipótesis.

### Decisión 3 — la motivación breve es opcional, nunca bloqueante

`intention_statement` puede quedar `NULL`. Nadie debe quedar atrapado sin poder aceptar el plan por no escribir texto libre — no hay evidencia hoy de que ese texto aporte algo medible; se guarda si se escribe, no se exige para avanzar. Mismo espíritu de "Simplicidad Progresiva" que ya gobierna `DailyVitalityLog` (ADR-009 Decisión 1, todos los campos nullable).

### Decisión 4 — el estado no bloquea nada todavía

Igual que ADR-009 Decisión 4 (el semáforo de recuperación no dispara ninguna acción): `acceptance_status = PROPOSED` **no** oculta tareas, no impide `completeTask`, no bloquea ningún flujo existente. Es puramente informacional en v1 — evita romper el uso actual, donde las familias ya trabajan sobre planes sin ningún paso de confirmación.

### Decisión 5 — backfill: los planes existentes se marcan `ACCEPTED`, no `PROPOSED`

La migración debe marcar todo plan ya existente como `ACCEPTED`, con `accepted_at = COALESCE(ai_generated_at, NOW())` y `accepted_by = NULL` (desconocido, histórico). Razón: esos planes ya están operando activamente para familias reales — marcarlos retroactivamente como "no aceptados" falsearía el historial en vez de protegerlo, el mismo tipo de error que ADR-004 previene, aplicado aquí a la migración en vez de a una hipótesis.

### Decisión 6 — endpoint: mismo patrón que `tasks/{id}/complete`

`PUT /api/plans/{id}/accept`, mismo estilo que `PUT /api/plans/tasks/{id}/complete` ([`PlanController.java:101-117`](../../backend/src/main/java/com/integrityfamily/plan/controller/PlanController.java#L101-L117)): recibe `Principal` para `accepted_by`, body opcional `{ intentionStatement }`, registra un valor nuevo `AuditEventType.PLAN_ACCEPTED` (mismo patrón que `PLAN_TASK_TOGGLED`) vía `AuditService`. **Idempotente:** volver a llamarlo sobre un plan ya `ACCEPTED` actualiza `accepted_at`/`accepted_by`/`intention_statement` en vez de fallar — permite corregir o enriquecer la declaración sin un endpoint de edición separado.

## Trade-off Analysis

Frente a un selector de prioridades estilo `priorities.json` (3 de N áreas): se pierde la sensación de "elegir dónde enfocarse" del documento original, pero se evita inventar una elección redundante sobre una escala de solo 4 dimensiones donde `criticalDimension` ya calcula lo mismo — mismo argumento que ADR-003.

Frente a replicar el patrón completo de `PlanAdjustment` (estado con 5 valores y entidad propia con historial): se pierde la posibilidad de reconstruir múltiples ciclos de aceptación/rechazo, pero se evita construir infraestructura para un caso de uso — rechazar y pedir un plan nuevo — que no existe hoy. Mismo criterio que ADR-007 aplicó al posponer el episodio procesual rico.

Frente a no hacer nada: se dejaría sin resolver el único hueco de Fase 0 detectado en la auditoría — la familia nunca declara, ni siquiera implícitamente, que empieza el plan por decisión propia.

## Consequences

- **Más fácil:** cierra el gap de Fase 0b identificado en la auditoría fase-por-fase; reutiliza el patrón `approved_by`/`approved_at` ya validado en `PlanAdjustment`; no requiere tabla nueva.
- **Más difícil:** a diferencia de ADR-007/008 (que reutilizaron cálculo ya existente), aquí todo el pipeline es nuevo — enum, migración, endpoint, valor de `AuditEventType`, cambios en `PlanController`/`PlanService`/`ImprovementPlan`. Mismo perfil de "más difícil" que tuvo `vitality` en ADR-009.
- **Habrá que revisitar:** si aparece evidencia real de que las familias quieren rechazar un plan y pedir uno nuevo, promover `PlanAcceptanceStatus` a un estado más rico (`REJECTED`, posiblemente conectado a `AdaptivePlanService`) — no se construye aquí.
- **Habrá que revisitar:** la interfaz de frontend que consuma `PUT /api/plans/{id}/accept` queda fuera de este ADR, igual que la Fase 4 (`vitality`) quedó sin frontend en ADR-009.

## Action Items

1. [x] Migración `V111__plan_acceptance_status.sql` — columnas `acceptance_status`/`accepted_at`/`accepted_by`/`intention_statement` en `plans`; backfill de filas existentes a `ACCEPTED` (Decisión 5).
2. [x] Enum `PlanAcceptanceStatus` (`PROPOSED`, `ACCEPTED`) + campos correspondientes en `ImprovementPlan.java`.
3. [x] `PlanService.acceptPlan(Long planId, String acceptedByEmail, String intentionStatement)` — idempotente (Decisión 6). Expuesto también en `PlanResponse` (`acceptanceStatus`/`acceptedAt`/`acceptedBy`/`intentionStatement`).
4. [x] Endpoint `PUT /api/plans/{id}/accept` en `PlanController`, con `Principal`, registrando `AuditEventType.PLAN_ACCEPTED` (nuevo valor) vía `AuditService` — mismo patrón que `completeTask` (`PlanController.java:101-117`).
5. [x] Tests unitarios en `PlanServiceTest$AcceptPlan`: plan `PROPOSED` → `ACCEPTED` con campos correctos; `intentionStatement` ausente no bloquea la aceptación; re-aceptar un plan ya `ACCEPTED` actualiza campos sin error (idempotencia); ID inexistente → excepción. 4/4 pasan (21/21 en `PlanServiceTest` completo, sin regresión).
6. [x] Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa del backend con `mvn clean test` (0 fallos, BUILD SUCCESS).
7. [x] Interfaz de frontend en `plan-list-page.component.{ts,html}` — banner de declaración de intención (textarea opcional + botón "Aceptar y comenzar") cuando `acceptanceStatus === 'PROPOSED'`, y confirmación en línea (`acceptedBy`/`intentionStatement`) cuando ya está `ACCEPTED`. Campos nuevos agregados a la interfaz `Plan` en `core/models/models.ts`. Verificado en navegador real contra backend real (familia de prueba, plan generado vía `generate-deterministic`, `PUT /api/plans/{id}/accept` → `200`, UI actualizada reactivamente, fila confirmada en MySQL).
8. [ ] (Fuera de alcance) Interfaz de Fase 4 (`vitality`) — mismo hueco identificado en la auditoría, se resuelve en tarea separada.
