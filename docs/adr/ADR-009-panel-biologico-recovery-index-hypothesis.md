# ADR-009: `DailyVitalityLog` — Fase 4 (base biológica) y `RECOVERY_INDEX_HYPOTHESIS`

**Status:** Accepted
**Date:** 2026-07-20
**Deciders:** William Lopez
**Instrumento de campo:** [ADR-009-cuadernillo-fase4-biologico.html](./ADR-009-cuadernillo-fase4-biologico.html) — cuadernillo imprimible, campos 1:1 con `DailyVitalityLog` (Decisión 1)

## Context

Documento externo ("Método de Cultivo de Calma Mental, Bienestar Corporal y Hogar Afectuoso") propone un protocolo de 8 fases (0-7) con instrumentos estandarizados por fase. Al contrastarlo contra el código real, 7 de las 8 fases ya tienen implementación equivalente bajo otro nombre:

| Fase del documento | Ya implementado como |
|---|---|
| Fase 0 (línea base) | `evaluation`/ICF |
| Fase 1 (pausa/interocepción) | `pauseCapacity` (ADR-007) + fase `NOTICE` de `SCENARIO_V1_2` |
| Fase 2 (utilidad del pensamiento) | ADR-008 (`PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS`) |
| Fase 3 (microexperimentos) | módulo `bitacora` (`SprintMission`/`SprintDaily`/`SprintRetrospective`) |
| Fase 5 (comunicación familiar) | módulos `council`/`chat` |
| Fase 6 (manejo del sufrimiento) | módulo `errorprotocol` (`FamilyErrorProtocol`, V98) |
| Fase 7 (cultura científica) | `SprintRetrospective` + `hypothesis_evidence` (ADR-004) |

**Fase 4 (base biológica: sueño, alimentación, ejercicio, índice de recuperación) es la única sin cobertura existente.** Verificado por grep sobre `domain/`, `myspace/` y `checklist/`: cero clases relacionadas con sueño, nutrición o actividad física en todo el proyecto.

### Por qué no se reutiliza `SprintDaily`

`SprintDaily` ([`SprintDaily.java`](../../backend/src/main/java/com/integrityfamily/domain/SprintDaily.java)) es el candidato más cercano — un check-in diario existente — pero no sirve para esto por dos razones estructurales, no estilísticas:

1. `sprint_id` es `nullable = false` — un `SprintDaily` no puede existir sin un `FamilySprint` activo. El registro biológico de Fase 4 debe poder llevarse todos los días, exista o no un sprint en curso (la propuesta original habla de "rutina de sueño consistente" como práctica continua, no acotada a 7-21 días de sprint).
2. Sus campos son texto libre (`yesterdayText`, `todayText`, `blockagesText`, `resolutionText`) — Fase 4 necesita campos numéricos (horas de sueño, minutos de ejercicio) y escalas 1-5, no narrativa.

Extender `SprintDaily` con columnas biológicas opcionales + hacer `sprint_id` nullable rompería su propósito actual (check-in de sprint) para forzar un caso de uso distinto. Se crea una entidad nueva y pequeña en su lugar.

### Relación con la dimensión "hábitos" del ICF

El ICF ya tiene una dimensión `habitos` (evaluada vía `evaluation_answers`, escala de Ruta de Conciencia). Esa dimensión mide **conciencia percibida** sobre hábitos ("¿qué tan consciente está la familia de su patrón de sueño/ejercicio?"). Fase 4 mide algo distinto y complementario: **el dato crudo diario** (cuántas horas durmió, cuántos minutos hizo ejercicio) — no reemplaza la dimensión `habitos`, la alimenta con datos objetivos que hoy no existen en ningún lado. Esta distinción se documenta explícitamente para no repetir el error que motivó separar `THINK` de `EFFECT` en ADR-008: dos cosas relacionadas pero no intercambiables.

## Decision

### Decisión 1 — nueva entidad mínima, no el panel completo de una vez

Se crea `DailyVitalityLog` (nueva tabla `daily_vitality_logs`, migración `V110`), un registro por `family_member_id` + `log_date` (única por día por miembro):

| Campo | Tipo | Corresponde a (documento) |
|---|---|---|
| `family_member_id` | FK, not null | — |
| `log_date` | `DATE`, not null | — |
| `sleep_hours` | `DOUBLE`, nullable | IF-40 Sueño (horas) |
| `sleep_quality` | `INT` 1-5, nullable | IF-40 Sueño (calidad) |
| `exercise_minutes` | `INT`, nullable | IF-40 Actividad física |
| `nutrition_quality` | `INT` 1-5, nullable | IF-40 Alimentación — se usa una escala 1-5 autoreportada, no un diario de comidas; registrar cada comida es una carga operacional que el documento no justifica para un v1 |
| `screen_time_before_bed_minutes` | `INT`, nullable | IF-40 Pantallas |
| `fatigue_level` | `INT` 1-5, nullable | insumo de IF-42 (Índice de Recuperación), no está en el listado de IF-40 pero el documento lo exige como componente del índice |
| `source` | `VARCHAR`, default `MANUAL` | mismo patrón ya usado por `EvidenceSource` |

Todos los campos numéricos son nullable — un miembro puede registrar solo sueño un día y solo ejercicio otro; no se fuerza a completar el panel entero para que el registro sea válido (mismo espíritu que "simplicidad progresiva" del propio documento).

### Decisión 2 — el Índice de Recuperación (IF-42) es una hipótesis, no un cálculo directo al ICF

`RecoveryIndexService` calcula el índice 0-100 a partir de `sleep_hours`/`sleep_quality`/`exercise_minutes`/`nutrition_quality`/`fatigue_level` de los últimos N días — **pero el resultado se escribe en `hypothesis_evidence`, nunca directamente en `FamilyLongitudinalState` ni como input del ICF.** Se registra como `RECOVERY_INDEX_HYPOTHESIS` / `v1` en la tabla de hipótesis activas de ADR-004:

| Campo | Valor |
|---|---|
| `hypothesis` | `RECOVERY_INDEX_HYPOTHESIS` |
| `hypothesis_version` | `v1` (fórmula: promedio ponderado de sueño/ejercicio/nutrición, penalizado por fatiga — la ponderación exacta queda fijada en el código, no en este ADR, para poder ajustarla sin reabrir el documento si `v1` resulta mal calibrada) |
| `subject_type` | `FAMILY_MEMBER` (nuevo `subject_type` — catálogo abierto por diseño, ADR-004 Decisión 2) |
| `subject_id` | `familyMemberId` |
| `measurement_type` | `RECOVERY_INDEX` |
| `measurement_value` | el índice 0-100 calculado |
| `instrument` | `"RECOVERY_INDEX_V1"` |
| `source` | `DERIVED` (se deriva de `DailyVitalityLog`, no es una observación directa — valor de `EvidenceSource` ya existente en ADR-004) |

Por qué no se conecta al ICF todavía: la fórmula del índice es, ella misma, una hipótesis sin validar sobre qué combinación de sueño/ejercicio/nutrición/fatiga predice bienestar real — exactamente el caso que la Regla V1.1.1 existe para gobernar (mismo criterio que ya aplicó ADR-003/005 con PAF: primero evidencia acumulada, después promoción a señal operacional).

### Decisión 3 — el Semáforo (IF-41) es una vista, no una columna

Verde/Amarillo/Rojo es una función de bucketing sobre el Índice de Recuperación ya calculado (`>=70` verde, `40-69` amarillo, `<40` rojo — mismo umbral que el Gate del documento) — se calcula en el momento de lectura (frontend o DTO de respuesta), no se persiste. Persistir el semáforo además del índice violaría el mismo principio de ADR-005 (no guardar un derivado cuando el dato crudo ya está disponible para recalcularlo).

### Decisión 4 — el Gate (">70% recuperación") no dispara nada todavía

El documento sugiere el semáforo como criterio de salida de fase. Este ADR **no** conecta el índice a ninguna acción automática (alertas, tareas de plan, notificaciones) — sería operacionalizar una hipótesis sin validar, el mismo error que ADR-004 previene explícitamente para el estado operacional. Queda como "Habrá que revisitar" una vez haya evidencia real en `hypothesis_evidence`.

## Trade-off Analysis

Frente a construir los 3 instrumentos completos de una vez (Panel + Semáforo + Índice) como una única pieza operacional: se pierde inmediatez de "ver el semáforo ya", pero se evita repetir el error que ADR-004 documentó — tratar un cálculo recién inventado como si ya fuera conocimiento validado del sistema.

Frente a extender `SprintDaily`: se pierde la conveniencia de no crear una tabla nueva, pero se evita acoplar un registro biológico continuo a la existencia de un sprint activo, y evita mezclar campos numéricos con un modelo de texto libre que no los necesita.

Frente a no registrar nada y esperar a que el documento completo esté validado: Fase 4 es la única fase sin ningún dato hoy — esperar más solo pospone la única brecha real identificada.

## Consequences

- **Más fácil:** por primera vez en el proyecto existe una fuente de datos objetiva de sueño/ejercicio/nutrición, independiente de sprints activos o evaluaciones ICF.
- **Más difícil:** nuevo módulo pequeño (`vitality` o similar) con su propia entidad, repositorio, servicio y controller — a diferencia de ADR-007/008, no hay ningún cálculo existente que reutilizar; todo el pipeline de captura es nuevo.
- **Habrá que revisitar:** si `RECOVERY_INDEX_HYPOTHESIS` acumula evidencia suficiente y muestra correlación real con ICF/riesgo, decidir en un ADR propio si se promueve a input del ICF o de `AdaptivePlanService` — no se decide aquí.
- **Habrá que revisitar:** si la fórmula `v1` del índice cambia (otros pesos, otras variables), subir a `v2` y documentarlo, gobernanza ya fijada en ADR-004 Decisión 2.

## Action Items

1. [x] Migración `V110__daily_vitality_log.sql` — tabla `daily_vitality_logs` (Decisión 1), única por `family_member_id`+`log_date`.
2. [x] Módulo nuevo `vitality`: entidad `DailyVitalityLog`, `DailyVitalityLogRepository`, `VitalityService` (registrar log — upsert que solo sobreescribe campos presentes, listar por miembro/rango de fechas, valida que el miembro pertenezca a la familia), `VitalityController` (`POST/GET /api/families/{familyId}/members/{memberId}/vitality`, `GET .../recovery-index`).
3. [x] `RecoveryIndexService.calculateAndRecord(familyId, familyMemberId, windowDays)` — calcula el índice 0-100 (fórmula v1 documentada en Javadoc: media de sueño/ejercicio/nutrición/fatiga-invertida sobre los componentes presentes) y escribe fila en `hypothesis_evidence` vía `RECOVERY_INDEX_HYPOTHESIS`/`v1` (Decisión 2). Retorna `null` sin escribir nada cuando la ventana no tiene ningún dato.
4. [x] Registrado `RECOVERY_INDEX_HYPOTHESIS` / `v1` en la tabla de hipótesis activas de [ADR-004](./ADR-004-hypothesis-evidence-pattern.md).
5. [x] Semáforo (`RecoveryIndexService.semaphore()`) como campo calculado en `RecoveryIndexResponse`, sin persistencia propia (Decisión 3).
6. [x] Tests unitarios (22 nuevos, `VitalityServiceTest` + `RecoveryIndexServiceTest`): upsert con campos parciales conserva lo ya guardado; membresía familia/miembro validada; fórmula v1 correcta por componente y promediada entre días de la ventana; ventana sin datos → `null` sin escritura; escritura en `hypothesis_evidence` con los campos correctos; umbrales del semáforo.
7. [x] Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa del backend (2121/2121).

### Nota de implementación — `@EntityScan` explícito

`IntegrityFamilyApplication` restringe el escaneo de entidades JPA a una lista explícita de paquetes (no un prefijo `com.integrityfamily.*`) — sin agregar `com.integrityfamily.vitality.domain` a esa lista, el arranque fallaba con `BeanCreationException: Not a managed type: DailyVitalityLog`, detectado por el E2E antes de llegar a producción. Cualquier módulo nuevo futuro debe registrar su paquete `<módulo>.domain` ahí explícitamente.

### Cierre del gap de frontend (2026-07-25)

Al auditar, fase por fase, en qué interfaz se reporta cada etapa del método externo (mismo ejercicio que originó este ADR), Fase 4 quedó identificada como la única con backend completo pero **cero interfaz** — nadie podía registrar un `DailyVitalityLog` desde la UI. Cerrado con `vitality-page.component` (`if-frontend/src/app/features/vitality/`): selector de miembro, formulario diario (todos los campos opcionales, igual que el backend), y tarjeta de Índice de Recuperación con semáforo. Ruta `/vitality`, enlace nuevo en el sidebar (bloque "Transformación Diaria"). Verificado en navegador real contra backend real: registro guardado, fila confirmada en MySQL, índice recalculado correctamente (86.7/100 → 🟢), y precarga del formulario al recargar la página.
