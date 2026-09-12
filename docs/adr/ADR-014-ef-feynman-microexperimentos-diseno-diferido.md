# ADR-014: EF-Feynman (microexperimentos familiares) — diseño candidato, explícitamente diferido

**Status:** Proposed (diferido)
**Date:** 2026-09-12
**Deciders:** William Lopez
**Nota (mismo día):** el contenido descrito abajo ya no es solo un diseño de arquitectura — existen dos documentos Word ya redactados fuera del repo (`Instrumento_EF_Feynman_experimentos_filosoficos.docx`, la ficha/rúbrica genérica de 14 campos; `Integrity_Family_300_Microexperimentos_Filosoficos_Feynman.docx`, el catálogo completo de 300 fichas). Se leyeron y verificaron íntegramente antes de escribir esta nota — ver bullets nuevos bajo "Verificado".

## Context

En la misma línea de sesiones que produjo [ADR-013](ADR-013-ciclo-if7-marco-conceptual.md) (IF-7), un análisis posterior sobre *Principles of Secondary Education* (Inglis, 1918), *Six Easy Pieces* (Feynman) y la anécdota del cálculo autodidacta de Feynman desemboca en una propuesta de sistema concreto — provisionalmente llamado **EF-Feynman** — bastante más grande que lo que ADR-013 decidió construir:

- Catálogo fijo de **300 microexperimentos** familiares (`MicroExperimentDefinition`: código, área, subárea, mecanismo, pregunta, contexto, dificultad, nivel de riesgo, contraindicaciones, prerrequisitos).
- 5 entidades nuevas de instancia/ejecución: `FamilyExperiment`, `ExperimentEvidence`, `ExperimentReview`, `FamilyLearningRecord`, `FamilyLearningPrinciple`.
- Máquina de estados propia: `OBSERVED → HYPOTHESIS → PREDICTED → AUTHORIZED → ACTIVE → EVIDENCE_CAPTURED → REVIEWED → LEARNED → NEXT_EXPERIMENT` (con ramas `PAUSED`/`ESCALATED`/`CANCELLED`).
- Selector adaptativo con fórmula de ranking propia (`score = 0.30·area_match + 0.20·mechanism_match + ... - safety_penalty - repetition_penalty`).
- ~13 endpoints REST nuevos y 6 vistas Angular nuevas.
- Un concepto adicional, "Family Model Builder" / "Family Discovery Notebook": campos de representación libre (dibujos, símbolos inventados por la familia, ej. `🌡️ = activación alta`) que la familia construye y traduce después a lenguaje común.

### Verificado en este repo, no asumido

- **Cero colisión con código existente.** Búsqueda dirigida de `MicroExperimentDefinition`, `FamilyExperiment`, `ExperimentEvidence`, `ExperimentReview`, `FamilyLearningRecord`, `FamilyLearningPrinciple` en todo el repo: sin resultados. No hay nada parcialmente construido que este ADR deba reconciliar.
- **Este territorio ya fue pisado y diferido explícitamente por ADR-013**, Decisión 3: *"El escalón de 'apropiación de habilidad' (Feynman: explicar → transferir → sostener) y el copiloto socrático se posponen: son refinamientos de instrumento, territorio de una eventual V1.3 post-piloto... no de ahora."* EF-Feynman es, en sustancia, la versión desarrollada de exactamente ese escalón — con la diferencia de que ahora llega como diseño de sistema completo (entidades, endpoints, UI) en vez de una frase pospuesta.
- **Los huecos que EF-Feynman quiere llenar ya están registrados, no descubiertos ahora.** La ampliación de ADR-013 del 2026-09-12 ya verificó contra el código: `ChecklistItem`/`SprintMission` no tienen campo de hipótesis/predicción estructurada; lo más cercano (`SprintDaily`, `SprintRetrospective`) opera a nivel de ciclo de sprint, no de misión individual; `family_predictions` (`twin`) nunca transiciona de `ACTIVE`. EF-Feynman propone resolver estos mismos tres huecos, pero con 6 entidades nuevas y un catálogo paralelo de 300 ítems en vez de extender lo que ya existe.
- **Un catálogo de contenido de 300 ítems no es solo trabajo técnico.** El precedente en este repo (`risk_trajectories.requires_safety_protocol`, V97) muestra que el proyecto ya tiene un proceso serio para contenido que toca zonas de riesgo (violencia intrafamiliar, ideación suicida, autolesiones, etc.): 7 trayectorias confirmadas + 4 reglas contextuales, con activación siempre manual. Un catálogo nuevo de "microexperimentos" que toque comunicación, emociones o conflicto necesitaría el mismo nivel de revisión antes de poblarse — hoy no existe ese proceso para este catálogo, y no es algo que se decida en un ADR de arquitectura.
- **El catálogo de 300 fichas ya está redactado, no es solo una idea.** `Integrity_Family_300_Microexperimentos_Filosoficos_Feynman.docx` contiene 300 fichas reales (verificado por extracción íntegra del XML), organizadas en `Emociones · Comunicación · Hábitos · Tiempos` (75 cada una). Cada ficha sigue el mismo esqueleto de 16 campos del instrumento genérico (fenómeno, hecho≠interpretación, pregunta, hipótesis, predicción, microintervención, control, seguridad, evidencia, resultado-que-cambiaría-de-idea, explicación sin jerga, hueco de comprensión, intento de refutación, reconstrucción, prueba Feynman final, siguiente misión) más una rúbrica O-M-P-R-E (0-20).
- **Pero las 300 fichas son 60 mecanismos únicos × 5 contextos, no 300 ideas distintas.** Verificado por conteo: cada área tiene exactamente 15 mecanismos base (ej. "Nombrar antes de reaccionar", "Intensidad emocional 0–10") repetidos textualmente en 5 contextos domésticos (mañana, comida, tareas, llegada a casa, pantallas), con solo la frase de contexto sustituida dentro de la misma plantilla. Esto no resta valor al diseño, pero corrige el tamaño real del esfuerzo de autoría: es un catálogo de 60 mecanismos con una capa de generación por contexto, no 300 piezas de contenido redactadas una por una.
- **El catálogo ya incluye su propia regla de seguridad genérica**, distinta de (y más simple que) el proceso de 4 puertas de V97: "No se experimenta con personas. Se prueban cambios voluntarios, reversibles y de bajo riesgo... No se provocan conflictos, celos, miedo, privación, engaño, castigo, exposición de secretos ni crisis" + "Si aparece riesgo, coerción, violencia, abuso, autolesión u otra crisis, se suspende la lógica experimental y se prioriza protección y atención profesional" (regla general, repetida igual en las 300 fichas — no hay ficha alguna cuyo contenido trate directamente violencia, ideación suicida o autolesión; se confirmó por búsqueda de texto sobre las 300 fichas). Esto reduce el riesgo de la Decisión 4 original de este ADR, pero no lo sustituye: sigue siendo una cláusula genérica de "detente si algo grave aparece", no una revisión diferenciada por trayectoria como la de V97.
- **El catálogo ya separa explícitamente la puntuación O-M-P-R-E de ICF/ICaF, riesgo y adherencia** en su propio texto ("Esta puntuación mide aprendizaje del microexperimento y debe mantenerse separada de ICF/ICaF, riesgo y adherencia") — coincide, sin haberlo coordinado, con la Decisión 4 de ADR-013 (métrica ≠ propósito) y con el criterio ya vigente en este repo de no colapsar dominios de datos distintos en un solo score.
- **Ninguno de los dos documentos está en el repositorio.** Viven hoy en `Downloads` del usuario, fuera de control de versiones — este ADR no los importa ni los mueve; solo dejar constancia de que existen y de qué contienen, para no perder el trabajo ya hecho si en el futuro se retoma esta línea.
- **No hay todavía un piloto que reclame esto.** El banco `SCENARIO_V1_2` está congelado por la Directriz Operativa V1.2 mientras el piloto está en curso (ver ADR-011). Instrumentar un segundo sistema paralelo de "misiones con hipótesis y predicción" mientras el primero (`SCENARIO_V1_2`) sigue congelado y sin resultados de piloto analizados sería exactamente la trampa que la Regla V1.1.1 existe para evitar.

## Decision

### Decisión 1 — se registra la arquitectura EF-Feynman como diseño candidato, en este ADR

El diseño (entidades, máquina de estados, selector, endpoints, vistas) queda documentado aquí como referencia futura, para no perderlo y no tener que reconstruirlo desde cero si algún día hay un disparador real. **No se agrega a `vision.md`** — a diferencia de IF-7 (ADR-013) o IF-CAM (ADR-011), que son lecturas de mecanismos que *ya existen* en el sistema, EF-Feynman es un instrumento nuevo sin construir todavía; no hay nada que "leer en conjunto".

El contenido (instrumento genérico de 14 campos + catálogo de 300 fichas, 60 mecanismos × 5 contextos) ya está redactado en los dos documentos Word verificados arriba. Este ADR no los mueve al repositorio ni los transcribe — quedan referenciados por nombre de archivo como insumo ya disponible para el día en que se retome esta línea, sin ningún compromiso de fecha.

### Decisión 2 — explícitamente NO se construye ahora

- Ninguna migración, entidad, endpoint ni vista de las descritas arriba.
- Ningún catálogo de microexperimentos, ni siquiera un subconjunto piloto de 10-20 ítems, hasta que exista el proceso de revisión de contenido equivalente al de V97 para trayectorias de riesgo.
- Ninguna máquina de estados ni selector adaptativo nuevos — duplicarían `PlanGenerationService`/`AdaptivePlanService` sin evidencia de que el mecanismo actual sea insuficiente.
- El "Family Model Builder" / símbolos familiares inventados no se implementa como campo de UI — es una idea de producto interesante pero sin ningún hueco verificado en el código que la reclame hoy (a diferencia de, por ejemplo, H2 en ADR-012, que sí tenía un hueco de captura concreto).

### Decisión 3 — si se reabre, extender antes que duplicar

Si en el futuro un disparador real justifica retomar esto, la primera opción a evaluar **no** es el diseño de 6 entidades nuevas: es agregar campos opcionales de hipótesis/predicción/representación a `TaskEvidence` (o una tabla satélite 1:1 con ella) y reutilizar `PlanTask`/`ChecklistItem`/`SprintMission` como la unidad de "misión", en vez de crear un catálogo y un ciclo de vida paralelos. Esto es una nota de diseño para el futuro, no una decisión de construir nada ahora.

### Decisión 4 — el catálogo ya redactado respeta el espíritu de V97 pero no su proceso; una futura publicación necesita el proceso, no solo la cláusula

El catálogo verificado ya trae su propia cláusula de seguridad genérica ("se suspende la lógica experimental y se prioriza protección y atención profesional" ante riesgo/violencia/autolesión) y ninguna de las 300 fichas trata directamente un tema de `requires_safety_protocol` (V97). Eso es un buen punto de partida, pero no equivale al proceso de 4 puertas que V97 exige para contenido de riesgo — es una cláusula uniforme de "detente", no una revisión diferenciada por ítem. Si algún día este catálogo se publica dentro de Integrity Family, cada uno de los 60 mecanismos (no las 300 fichas, que son variaciones de contexto) pasa por el mismo criterio de revisión que V97 antes de activarse — no se asume que la cláusula genérica ya redactada sea suficiente solo porque el documento la incluye.

## Trade-off Analysis

Frente a **construir EF-Feynman ahora**: es un diseño coherente y bien pensado, pero (a) duplica trabajo ya diferido conscientemente en ADR-013, (b) no tiene disparador — ningún piloto ha pedido esto todavía, (c) un catálogo de 300 ítems es un proyecto de contenido con implicaciones de seguridad que no tiene proceso de revisión definido, y (d) instrumentar un segundo ciclo de misión/evidencia mientras `SCENARIO_V1_2` sigue congelado por el piloto duplicaría superficie de mantenimiento sin necesidad demostrada.

Frente a **no registrar nada**: se perdería un diseño detallado (útil si el escalón de "apropiación de habilidad" que ADR-013 pospuso alguna vez se activa) y el proyecto lo redescubriría desde cero.

Frente a **construir una versión mínima ahora** (p. ej. solo el campo de hipótesis en `TaskEvidence`, sin catálogo ni entidades nuevas): sería la opción más barata si hubiera evidencia de que la falta de ese campo está limitando el aprendizaje familiar hoy — pero esa evidencia no existe todavía; es la misma disciplina que ya aplicó ADR-013 Decisión 2 sobre `family_predictions`.

## Consequences

- **Más fácil:** existe un registro único del diseño EF-Feynman, evita que se re-proponga desde cero en una futura sesión, y dos ADRs (013 y 014) documentan de forma coherente por qué este territorio se difiere.
- **Más difícil:** nada — este ADR no añade superficie de código.
- **Habrá que revisitar:** si el piloto V1.2 concluye y muestra evidencia de que las familias cumplen misiones sin comprensión real (el riesgo de Goodhart ya registrado en ADR-013 Decisión 4), o si se define un proceso de revisión de contenido equivalente al de V97 para un catálogo nuevo — lo que ocurra primero.

## Action Items

1. [x] Registrar el diseño EF-Feynman en este ADR, sin construir nada.
2. [ ] (Condicional) Retomar solo si el piloto V1.2 muestra el patrón de cumplimiento-sin-comprensión de ADR-013 Decisión 4 — empezando por Decisión 3 de este ADR (extender `TaskEvidence`, no duplicar entidades).
3. [ ] (Condicional) Si se retoma el catálogo, definir primero el proceso de revisión de seguridad equivalente a V97 y aplicarlo a los 60 mecanismos ya redactados en `Integrity_Family_300_Microexperimentos_Filosoficos_Feynman.docx` antes de publicar cualquiera — el contenido ya existe, lo que falta es el proceso de revisión, no la redacción.
