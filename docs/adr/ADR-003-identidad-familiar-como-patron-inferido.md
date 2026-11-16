# ADR-003: Identidad Familiar como patrón inferido, no como sexto nivel de conciencia

**Status:** Accepted
**Date:** 2026-07-16
**Deciders:** William Lopez

## Context

Durante una exploración teórica sobre mecanismos de cambio conductual (con el ejemplo clínico de la obesidad como punto de partida), se propuso un modelo de cinco "trayectorias" familiares — Automática (inconsciente), Consciente, Deliberativa, Automatización Saludable, e Identidad Familiar — describiendo cómo una respuesta familiar automática puede transformarse mediante conciencia y repetición sostenida hasta integrarse como parte de la identidad del núcleo.

Al contrastar ese modelo contra el código existente, se encontró — otra vez — el mismo patrón ya señalado en ADR-002: **el concepto ya estaba construido, de forma independiente, en dos lugares distintos:**

1. **`RUTA_CONCIENCIA_SCALE`** (`rutaConcienciaDomain.ts`): 5 niveles `INCONSCIENTE → REACTIVO → CONSCIENTE → INTENCIONAL → PLENO`, usados como escala de autorreporte genérica en el diagnóstico.
2. **El tipo de pregunta `TRAJECTORY`** (`evaluation.component.ts:549-556`), un modelo epistemológico separado ("Señal Corporal → Conciencia → Acción") que el propio `CLAUDE.md` documenta como **deliberadamente no unificado** con la Ruta de Conciencia.

Cuatro de las cinco trayectorias propuestas corresponden, casi palabra por palabra, a niveles ya existentes en ambas escalas. La quinta — **Identidad Familiar** ("esto ya expresa quiénes somos", no solo "ya es automático") — no tiene equivalente en ninguna de las dos. Este ADR decide qué hacer con esa quinta pieza, sin tocar las dos escalas ya construidas.

### Opciones descartadas, y por qué

**Agregar un sexto valor literal a la escala (score 6).** Descartado: la conversión de nivel a ICF está hardcodeada asumiendo un techo de 5 en al menos dos lugares —

```java
// FamilyIndicatorsService.java:438
double valor = (double) nivel / 5.0 * 100;

// IcafDomainResolver.java:187
return nivel * 20.0;
```

Un sexto nivel rompe esa normalización (6/5×100 = 120%, fuera de rango) y vuelve incomparables las respuestas históricas, ya capturadas contra un techo de 5. No es un cambio de UI, es un cambio de instrumento.

**Usar "esto ya funciona para mi familia" como texto de anclaje.** Descartado: mide utilidad percibida, no alineación con valores. El propio ejemplo de la obesidad lo ilustra — comer dulce ante la ansiedad también "funciona" (alivio temporal) y es exactamente el automatismo disfuncional que el modelo quiere ayudar a transformar. Ese texto invertiría el sentido de la escala.

**Medir por trayectoria individual** (un estímulo específico, ej. "cuando el hijo llega tarde"). Teóricamente es la unidad más fiel al modelo, pero es impracticable con el diseño actual del motor de evaluación:

```java
// AssessmentController.java:171-172
Collections.shuffle(parentKeys);
String selectedParentKey = parentKeys.get(0);
```

Con ~20 escenarios en el banco `SCENARIO_V1_2` (V89-V95) y 1 elegido al azar por sesión, la probabilidad de que a una familia le toque el mismo escenario dos sesiones seguidas es ~1/20 — en promedio se necesitarían ~20 evaluaciones para ver el mismo estímulo repetirse. No es un límite teórico, es una consecuencia del muestreo aleatorio actual.

## Decision

**Identidad Familiar (Patrimonio Automático Familiar, PAF) no se autoevalúa — se infiere del comportamiento sostenido, por dimensión ICF.**

1. **Granularidad: por dimensión ICF**, no por trayectoria individual ni por familia completa. Las 4 dimensiones (emociones, comunicación, hábitos, tiempos) ya se recalculan en cada ciclo de evaluación vía `FamilyIcfRecalculatedEvent`, dando densidad de datos suficiente sin depender de que el muestreo aleatorio repita un escenario específico. Medir por familia completa se descarta por redundante — `MilestoneService` ya combina tiempo + ICF + tareas a ese nivel.

2. **Umbral de score: `dimScore >= 90`** (no 80). `RiskLevel.LOW` usa 80-100, pero 80 es exactamente lo que produce una dimensión donde *todas* las respuestas fueron `INTENCIONAL` (4×20). Usar 80 confundiría esfuerzo sostenido con automaticidad real — la misma distinción que motivó descartar "funciona" como texto. 90 exige que la mezcla de respuestas esté predominantemente en `PLENO`.

3. **Umbral de sostenimiento: `streak >= 3` ciclos consecutivos.** Reutiliza `DETERIORATION_THRESHOLD = 3` (`FamilyCausalEngine.java:48`), la misma constante que el sistema ya usa para declarar un patrón de deterioro sostenido (`consecutiveDeteriorations >= 3` en `hasEmotionalDeterioration()`). Mismo estándar de evidencia, aplicado en sentido positivo.

4. **Almacenamiento: 4 columnas nuevas en `FamilyLongitudinalState`**, no una tabla ni servicio nuevo. `FamilyLongitudinalState` es una fila única por familia (`@UniqueConstraint(columnNames = "family_id")`, sin historial) que ya resuelve este mismo problema para bitácora con `consecutiveImprovements`/`consecutiveDeteriorations` — contadores que se incrementan o resetean por evento, sin guardar una fila por ciclo. Se extiende el mismo patrón:

   ```java
   private Integer emocionesPlenoStreak = 0;
   private Integer comunicacionPlenoStreak = 0;
   private Integer habitosPlenoStreak = 0;
   private Integer tiemposPlenoStreak = 0;
   ```

   La lógica de incremento/reset vive en `LongitudinalStateService.onIcfRecalculated()` — el mismo método que ya sincroniza `dimEmociones`/`dimComunicacion`/etc. desde `FamilyIcfRecalculatedEvent`: si el nuevo valor de la dimensión ≥ 90, incrementa; si no, resetea a 0.

### El eje completo (para referencia)

```
AUTOMATISMO          → INCONSCIENTE / "no noto nada" (ya existe)
CONCIENCIA           → REACTIVO / CONSCIENTE (ya existe)
REGULACIÓN           → INTENCIONAL / "hago pausa antes de responder" (ya existe)
AUTOMATIZACIÓN SANA  → PLENO / "mantengo calma" (ya existe)
IDENTIDAD FAMILIAR   → dimScore ≥ 90 sostenido ≥ 3 ciclos (nuevo — inferido, no preguntado)
```

## Trade-off Analysis

Frente a un sexto nivel autoevaluado: se pierde la simplicidad de una respuesta directa ("la familia dice que sí, ya somos así") — el indicador se calcula en silencio y no tiene todavía un punto de retroalimentación visible en el HUD o dashboard (ver Consequences). A cambio, no se rompe la normalización de ICF ni la comparabilidad de datos históricos, y se evita el sesgo de deseabilidad social que un autorreporte de "identidad" invitaría (es fácil responder aspiracionalmente sobre quién *cree* ser la familia; es más difícil fingir 3 ciclos consecutivos de comportamiento real).

Frente a trayectoria individual: se pierde especificidad narrativa (no se puede decir "esta familia ya automatizó su respuesta a las llegadas tarde", solo "su dimensión de comunicación está consolidada"). A cambio, es medible hoy, sin rediseñar el motor de muestreo del diagnóstico.

Frente a una tabla de historial nueva: se pierde la curva completa (no se puede reconstruir visualmente "cómo llegamos aquí", solo el streak actual). A cambio, no se duplica infraestructura — se reutiliza el mismo patrón de contador que ya existe y ya se entiende.

## Consequences

- **Más fácil:** no rompe la normalización ICF (`nivel/5.0*100`, `nivel*20.0`) ni los datos históricos ya capturados en escala 1-5; reutiliza un patrón de contador ya existente (`consecutiveImprovements`/`consecutiveDeteriorations`) y una constante ya establecida (`DETERIORATION_THRESHOLD = 3`); no requiere tocar `AssessmentController` ni el banco `SCENARIO_V1_2`.
- **Más difícil:** hoy no existe ningún lugar en el frontend/HUD que muestre este streak — exponerlo (si se decide hacerlo) es trabajo nuevo, fuera del alcance de este ADR; `LongitudinalStateService.onIcfRecalculated()` necesita la lógica de incremento/reset, que hoy no existe.
- **Habrá que revisitar:** si en el futuro se quiere medir por trayectoria individual (el nivel de detalle teóricamente más fiel al modelo original), el prerrequisito es rediseñar el muestreo aleatorio de `AssessmentController` (`Collections.shuffle`) para reintroducir deliberadamente escenarios ya vistos — decisión de producto separada, no resuelta aquí.

**Nota de estatus epistémico:** el mecanismo de conteo (columnas, umbral 90, streak 3) es una decisión de arquitectura verificable hoy. La afirmación teórica de que ese streak *constituye* Patrimonio Automático Familiar — "el verdadero objeto de transformación" — es una hipótesis, no un hecho comprobado, sujeta a la Regla V1.1.1 (evolución basada en evidencia) ya establecida en `docs/vision.md`. No se promueve a `vision.md` hasta que exista evidencia real de familias sosteniendo el streak.

## Action Items

1. [x] Migración `V105` — agrega `emociones_pleno_streak`, `comunicacion_pleno_streak`, `habitos_pleno_streak`, `tiempos_pleno_streak` (`INT NOT NULL DEFAULT 0`) a `family_longitudinal_state`. Verificado contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6).
2. [x] Campos `Integer` agregados a `FamilyLongitudinalState.java`, con `@Builder.Default ... = 0`, mismo patrón que `consecutiveImprovements`.
3. [x] Lógica de incremento/reset implementada en `LongitudinalStateService.onIcfRecalculated()` vía el helper `nextPlenoStreak()` — solo se evalúa cuando la dimensión llega en el evento (mismo guard que su sincronización), evitando resetear un streak por ausencia de dato.
4. [x] Tests unitarios agregados a `LongitudinalStateServiceTest` (umbral exacto en 90, reset en 89, dimensión ausente no se toca, streaks de las 4 dimensiones se mueven de forma independiente). 27/27 pasan.
5. [ ] (Fuera de alcance de este ADR) Exponer el streak al frontend/HUD — no hay UI hoy que lo muestre.
6. [ ] (Fuera de alcance de este ADR) Si se decide medir trayectoria individual en el futuro, rediseñar el muestreo de `AssessmentController` para reintroducir escenarios ya vistos.
