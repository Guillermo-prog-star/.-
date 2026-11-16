# ADR-016: TSIRCA (PLAC/SPLAC/CPLAC) — arquitectura epistemológica, diseño diferido

**Status:** Proposed
**Date:** 2026-09-20
**Deciders:** William Lopez

## Context

Una sesión de reformulación conceptual (tres iteraciones sucesivas de crítica y rediseño sobre un texto propio) propone una "Teoría Sistémica Integrativa de Realidad Contextual y Agencia" (**TSIRCA**), que unifica tres siglas previas — **PLAC** (Persona/Lugar/Animal/Cosa — discriminar el evento del significado), **SPLAC** (estado corporal/interocepción — discriminar la sensación de la evidencia externa) y **CPLAC** (contexto — evidencia que arbitra entre hipótesis competidoras) — en un único bucle: `R (realidad) → S (señal) → P (percepción) → PLAC (entidades) → E (evento) ↔ B/SPLAC (cuerpo) → M (modelo/significado) → H (hipótesis competidoras) → C/CPLAC (contraste) → VO (vacío operativo) → A (acción investigativa) → R' (nueva realidad) → F (falsabilidad) → U (actualización) → L (aprendizaje longitudinal)`, con una escala de competencia N1→N5 (Discriminar → Detectar → Modelar → Investigar → Actualizar).

El propio proceso de rediseño ya corrigió sus dos afirmaciones más frágiles antes de llegar aquí: (a) la idea inicial de que "la realidad no existe más allá de la imaginación" se sustituyó por un realismo crítico explícito (`REALIDAD ≠ EXPERIENCIA` pero tampoco `REALIDAD = IMAGINACIÓN`); (b) el modelo popular de "cerebro triuno" y la dicotomía "hemisferio derecho = percepción / hemisferio izquierdo = significado" se descartaron por no representar la organización funcional real (redes distribuidas e interdependientes), y no deben usarse como afirmación neurocientífica en ningún copy dirigido a familias — mismo principio que ya fijó [ADR-013](ADR-013-ciclo-if7-marco-conceptual.md) Decisión 3 ("ninguna afirmación neurocientífica promocional").

### Verificado en este repo, no asumido

- **Sin colisión de nombres:** búsqueda dirigida de `PLAC|SPLAC|CPLAC|TSIRCA` (palabra completa) en `backend/`, `if-frontend/` y `docs/` — cero resultados. Es terminología enteramente nueva, no continuación de trabajo ya empezado.
- **El bucle completo ya es, en lo esencial, una tercera lectura del mismo ciclo que ADR-011 (IF-CAM) y ADR-013 (IF-7) ya documentaron:**

  | TSIRCA | IF-7 (ADR-013) | IF-CAM (ADR-011) |
  |---|---|---|
  | `S → P → PLAC → E` | `VER` | `Percibir → Atender` |
  | `M` (modelo/significado) | — | `Interpretar → Relacionar → Imaginar` |
  | `H` (hipótesis competidoras) | `PREGUNTAR → PROPONER → PREDECIR` | `Evaluar → Decidir` |
  | `A` (acción investigativa) | `ACTUAR` | `Actuar` |
  | `C/CPLAC → F` (contraste/falsabilidad) | `CONTRASTAR` | `Reflexionar` |
  | `U → L` (actualización/aprendizaje) | `APRENDER` | `Aprender → Transformación` |

  Lo que TSIRCA añade genuinamente sobre esos dos marcos ya aceptados es (1) una capa corporal explícita antes del modelo (`SPLAC`/`B`), donde IF-7/IF-CAM la dejan implícita dentro de "Percibir", y (2) una competencia de falsabilidad explícita (`F(H) = ∃E tal que E contradice H`) como paso nombrado, no solo implícito en "Reflexionar/Contrastar". No es un ciclo incompatible; es un refinamiento de vocabulario sobre el mismo mecanismo, mismo criterio que ADR-011 ya aplicó al leer `SCENARIO_V1_2` en clave IF-CAM.
- **El "vacío operativo" (`VO`) no es un concepto nuevo por construir — ya existe, medido y conectado a `hypothesis_evidence`.** `RiskAlgoV1Engine.compute()` ([`RiskAlgoV1Engine.java:161-169,218,231-238`](../../backend/src/main/java/com/integrityfamily/risk/service/RiskAlgoV1Engine.java)) calcula `NeuroProfile.pauseCapacity` a partir de opciones `vector_tag='PAUSE'` (22 opciones reales, `V85`/`V86`), persistido en `evaluations.pause_capacity` (`V84`) y registrado como `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` en `hypothesis_evidence` desde [`EvaluationService.finalize()`](../../backend/src/main/java/com/integrityfamily/evaluation/service/EvaluationService.java) (ADR-007). El "intervalo entrenable entre activación e intervención" que este documento describe es, operacionalmente, la misma cosa que `pauseCapacity` ya mide.
- **El par "hipótesis (M) → contraste (evidencia)" ya tiene un proxy real, no hipotético.** `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` (ADR-008) compara la fase `THINK` (interpretación anticipatoria) contra `AFTERMATH` (qué ocurrió realmente) del mismo episodio en `SCENARIO_V1_2`, ya persistido en `evaluation_answers` y ya conectado a `hypothesis_evidence`. La "competencia de falsabilidad" que aquí se propone como paso N4 es una lectura conceptual de ese mismo proxy — no requiere instrumento nuevo.
- **La fórmula "Índice de Autonomía Investigativa" (`AI = decisiones válidas iniciadas por la familia / intervenciones necesarias de Integrity Family`) ya se registró — literalmente el mismo día — como candidata NO adoptada en el Amendment 2 de [ADR-014](ADR-014-ef-feynman-microexperimentos-diseno-diferido.md#L74), gateada por el mismo disparador que [ADR-013](ADR-013-ciclo-if7-marco-conceptual.md#L53-L57) Decisión 5 ya fijó para `AUTONOMY_HYPOTHESIS`.** No es una fórmula distinta; es la misma idea apareciendo en dos sesiones de la misma línea de trabajo. Este ADR no la vuelve a evaluar — reafirma el mismo estado: no adoptada, no implementada.
- **Colisión de numeración N1–N5 con trabajo ya registrado.** [ADR-014](ADR-014-ef-feynman-microexperimentos-diseno-diferido.md) Decisión 5 (Bruno-Feynman) ya fija "N1–N5" como niveles de *apropiación por representación espacial* (arte de la memoria). Este documento propone otra escala N1–N5 sobre un eje distinto (competencia epistémica: Discriminar→Detectar→Modelar→Investigar→Actualizar). Además, `CLAUDE.md` ya documenta la **Ruta de Conciencia** (`INCONSCIENTE→REACTIVO→CONSCIENTE→INTENCIONAL→PLENO`), la escala oficial de 5 niveles del proyecto para "qué tan consciente está la familia de una realidad" — un tercer eje, también de 5 pasos, ya con fuente única de verdad (`rutaConcienciaDomain.ts`) y con una regla explícita de **no volver a bifurcar por dimensión**. Fijar aquí una cuarta escala de 5 niveles con el mismo prefijo `N` repetiría exactamente el error que la Ruta de Conciencia ya corrigió una vez (variantes casi idénticas compitiendo por el mismo espacio conceptual).

## Decision

### Decisión 1 — TSIRCA se documenta en este ADR como marco epistemológico de referencia, no como instrumento ni `vision.md`

A diferencia de IF-7/IF-CAM (lecturas de mecanismos que *ya existen* en el sistema, por eso sí entraron a `vision.md`), TSIRCA introduce vocabulario y una capa (`SPLAC`, `H` como espacio explícito de hipótesis competidoras, `F` como paso nombrado) que hoy no tiene instrumento propio. Sigue el mismo criterio que ADR-014/ADR-015: se registra aquí como referencia para no perder el razonamiento, **sin agregarse a `vision.md`**.

### Decisión 2 — no se fija ninguna numeración N1–N5 nueva

La propuesta de niveles (Discriminar/Detectar/Modelar/Investigar/Actualizar) queda registrada por su contenido, no por su numeración. Si algún día se instrumenta, la primera pregunta a responder no es "qué escala nueva se crea" sino **si esto es una cuarta dimensión de la Ruta de Conciencia, una variante de los niveles Bruno-Feynman de ADR-014, o realmente un tercer eje independiente** — decisión que un ADR de consolidación futuro debe tomar explícitamente, no algo que se resuelva por default reutilizando "N1–N5" tres veces con significados distintos.

### Decisión 3 — el bucle completo se registra como lectura enriquecida de IF-7/IF-CAM, sin reemplazarlos ni fusionarse con ellos todavía

Se deja constancia de la tabla de correspondencia (arriba) como documentación de por qué TSIRCA no es territorio nuevo sino una variación de vocabulario sobre ADR-011/ADR-013. No se edita `vision.md` para incorporarla — eso implicaría decidir cuál de los tres lenguajes (IF-7, IF-CAM o TSIRCA) es la lectura canónica, decisión que este ADR no está en condición de tomar sin un caso de uso de producto que la reclame.

### Decisión 4 — explícitamente NO se construye

- Ninguna migración, entidad, campo o servicio nuevo. No se agrega `PLAC`/`SPLAC`/`CPLAC` a `TaskEvidence`, `ChecklistItem`, `SprintDaily` ni ninguna entidad existente — repetiría el error que Decisión 3 de ADR-014 ya previno.
- No se reinstrumenta `pauseCapacity` ni el proxy `THINK`/`AFTERMATH` — ya existen y ya están conectados a `hypothesis_evidence` (ADR-007, ADR-008). Este documento solo los reconecta conceptualmente a la nueva terminología.
- No se adopta la fórmula "Índice de Autonomía Investigativa" — sigue exactamente en el mismo estado que fijó ADR-014 Amendment 2 el mismo día.
- Ninguna afirmación neurocientífica del tipo "hemisferio derecho/izquierdo" o "cerebro triuno" se traslada a copy de producto — quedan descartadas por el propio proceso de rediseño que las revisó, y por la Decisión 3 de ADR-013.
- Los "bucles de confirmación interpersonal" (`M_A → A_A → E_B → M_B → A_B → E_A`, retroalimentación entre dos miembros de familia) se registran como idea de contenido para un futuro microexperimento — territorio ya diferido de ADR-014, no de este documento.

## Trade-off Analysis

Frente a **construir un instrumento nuevo ahora** (campos PLAC/SPLAC en evidencias, servicio de hipótesis competidoras, etc.): duplicaría superficie de mantenimiento sobre mecanismos que en su núcleo ya existen (`pauseCapacity`, proxy `THINK`/`AFTERMATH`) y sobre un piloto (`SCENARIO_V1_2`) todavía congelado — exactamente la trampa que la Regla V1.1.1 y ADR-011/013 ya evitaron dos veces.

Frente a **fusionar TSIRCA con IF-7/IF-CAM en `vision.md` ya mismo**: forzaría a elegir un vocabulario canónico sin un disparador de producto que lo exija, y arriesgaría el mismo problema de "tres nombres para el mismo ciclo" que ya se evitó con la Ruta de Conciencia.

Frente a **no registrar nada**: se perdería la corrección epistemológica real que este proceso sí produjo (realismo crítico en vez de idealismo radical; descarte del cerebro triuno; identificación de que `VO` y el proxy de falsabilidad ya existen en el código) y se correría el riesgo de reconstruir esta discusión desde cero si se retoma más adelante.

## Consequences

- **Más fácil:** queda un mapeo explícito entre tres lenguajes conceptuales (IF-7, IF-CAM, TSIRCA) y dos mecanismos reales (`pauseCapacity`, proxy `THINK`/`AFTERMATH`), útil si se decide consolidar terminología más adelante. Queda también una advertencia explícita contra crear una cuarta escala de 5 niveles sin resolver primero su relación con la Ruta de Conciencia y con Bruno-Feynman.
- **Más difícil:** nada — cero cambios de código, cero migraciones.
- **Habrá que revisitar:** (a) si se abre `AUTONOMY_HYPOTHESIS` en `hypothesis_evidence` (disparador de ADR-013 Decisión 5), la fórmula AI de este documento y de ADR-014 es un punto de partida a evaluar, no una definición ya fijada; (b) si el piloto V1.2 o una necesidad de producto concreta reclama consolidar Ruta de Conciencia + Bruno-Feynman + esta propuesta en una sola taxonomía de niveles, eso amerita su propio ADR de consolidación — no antes.

## Action Items

1. [x] Documentar TSIRCA/PLAC/SPLAC/CPLAC en este ADR, verificado contra el código (`pauseCapacity`/ADR-007, proxy `THINK`/`AFTERMATH`/ADR-008, ausencia de colisión de nombres) y contra el resto de la documentación conceptual (IF-7/ADR-013, IF-CAM/ADR-011, Ruta de Conciencia/`CLAUDE.md`, Bruno-Feynman/ADR-014).
2. [ ] (Condicional) Si se abre `AUTONOMY_HYPOTHESIS`, revisar la fórmula AI de este documento junto con la de ADR-014 Amendment 2 como candidatas de partida — no antes.
3. [ ] (Condicional) Si surge un disparador de producto real para instrumentar niveles de competencia epistémica, abrir primero un ADR de consolidación que decida la relación entre Ruta de Conciencia, Bruno-Feynman N1–N5 y esta propuesta — nunca fijar una cuarta numeración N1–N5 sin esa decisión.
