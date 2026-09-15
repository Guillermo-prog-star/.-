# Integrity Family — Visión y Principios

**Última actualización:** 2026-09-12  
**Versión del sistema:** v1.1.9

---

## Qué es Integrity Family

Plataforma de acompañamiento familiar que combina IA adaptativa, evaluación diagnóstica y narrativa generacional para guiar a familias en un proceso de transformación sostenida de 36 meses.

No es una app de tareas. Es un sistema operativo familiar.

---

## Las cuatro capas del proyecto

Integrity Family no es solo una plataforma con una buena teoría — se sostiene en cuatro arquitecturas complementarias. Las primeras tres describen **cómo funciona** el sistema; la cuarta describe **qué asume** el sistema sobre lo que es una familia, antes de medir, sugerir o acompañar nada.

| Capa | Responde | Dónde vive |
|---|---|---|
| **Computacional** | Cómo se construye (Hogar Digital, HUD, IA, ICaF) | `docs/architecture.md`, código |
| **Metodológica** | Cómo se valida (instrumento, piloto, investigación) | `docs/Directriz_Operativa_Piloto_V1.1.md`, `docs/Validacion_Tecnica_Piloto_V1.1.md` |
| **Matemática** | Cómo se mide (indicadores, trayectorias, modelos) | Banco de Trayectorias, ICaF, `RiskAlgoV1Engine` |
| **Antropológica** | Qué es una familia, un hogar, cuidar, amar, heredar | Esta sección |

Toda arquitectura computacional oculta una antropología, la reconozca o no. Integrity Family prefiere hacerla explícita — y, como el resto del proyecto, sujeta a la Regla V1.1.1 de evolución metodológica basada en evidencia: esto es un marco conceptual propuesto, no una tesis antropológica demostrada.

**Alcance de la Regla V1.1.1:** nació acotada a `scenario_bank` (`docs/Validacion_Tecnica_Piloto_V1.1.md`), pero gobierna hoy **toda hipótesis científica o conductual incorporada al modelo de Integrity Family** — no "todo el sistema" (módulos sin hipótesis que validar, como autenticación, caché o mensajería, quedan fuera). El mecanismo que la sostiene es `hypothesis_evidence` (ADR-004): ninguna hipótesis —incluida esta capa antropológica, si llegara a formalizarse en constructos medibles como PAF (ADR-003)— se considera validada usando solo estado operacional; requiere evidencia longitudinal reproducible, registrada aparte, versionada por hipótesis e instrumento. Ver `docs/adr/ADR-004-hypothesis-evidence-pattern.md`.

### Principio de Construcción Compartida de Significado Familiar (CCSF)

> La transformación familiar ocurre cuando las experiencias individuales dejan de ser interpretaciones aisladas y se convierten, mediante procesos deliberados de comunicación, reflexión y memoria compartida, en una comprensión colectiva que orienta nuevas decisiones y nuevas formas de convivencia.

Bajo este principio, la comunicación deja de ser un módulo funcional del proyecto y se convierte en su mecanismo generativo: el proceso mediante el cual una familia se reconoce, construye significado compartido y transforma deliberadamente su manera de vivir.

### ¿Qué es una familia?

Una familia no es un conjunto de personas que comparten dirección, apellido o sangre — eso describe un hogar censal, no un hogar humano.

Una familia es un sistema vivo de significados compartidos que se construye, se sostiene y se transforma en el tiempo mediante la comunicación. No es una estructura fija que se fotografía en un diagnóstico; es un proceso que el diagnóstico apenas interrumpe por un instante para observarse a sí mismo.

Por eso Integrity Family no define a la familia por su composición (nuclear, extendida, monoparental, reconstituida) sino por su función: un lugar donde varias historias individuales negocian, día tras día, convertirse en una historia común sin dejar de ser distintas.

### ¿Qué significa vivir en familia?

Vivir en familia no es coexistir bajo el mismo techo. Es participar, con distinto grado de conciencia, en la construcción continua de esa historia común.

Se puede vivir bajo el mismo techo sin vivir en familia: cuando las rutinas se cruzan pero los significados nunca se encuentran. Y se puede vivir en familia a la distancia, cuando el vínculo sigue produciendo comprensión compartida aunque los cuerpos no compartan espacio.

Vivir en familia, entonces, es sostener una conversación que nunca termina — a veces en palabras, a veces en gestos, a veces en el silencio que solo tiene sentido para quienes comparten la historia.

### ¿Qué es el hogar?

El hogar no es la casa. La casa se habita; el hogar se construye.

Un hogar es el lugar — físico o no — donde una persona puede mostrarse tal como es sin temor a perder el vínculo por hacerlo. Es el único espacio donde la vulnerabilidad no debería ser un riesgo sino una condición de pertenencia.

Por eso el **Hogar Digital** no pretende ser un hogar sustituto ni una casa virtual. Pretende ser un espejo fiel de ese hogar real: un lugar donde la familia puede volver a mirarse, reconocer en qué etapa de su historia está, y decidir con más conciencia cómo seguir construyéndolo.

### ¿Qué es una conversación familiar?

No toda comunicación dentro de una familia es una conversación familiar. Coordinar horarios, resolver logística, repartir tareas — es comunicación funcional, necesaria, pero no es lo que transforma.

Una conversación familiar ocurre cuando al menos dos integrantes se permiten ser modificados por lo que el otro dice. No es intercambio de información; es intercambio de vulnerabilidad. Por eso las conversaciones familiares más transformadoras casi nunca son las más largas ni las más elocuentes — son las que alguien se atrevió a empezar con honestidad y alguien más se atrevió a recibir sin defenderse.

Las **misiones** no buscan producir conversaciones más frecuentes. Buscan crear las condiciones — el momento, la pregunta, el pretexto legítimo — para que ocurra una conversación familiar real, aunque dure tres minutos.

### ¿Qué es el cuidado?

El cuidado no es resolver los problemas del otro. Es sostener la presencia necesaria para que el otro no enfrente sus problemas en soledad, incluso cuando no se pueda resolver nada.

Cuidar es una decisión que se renueva, no un sentimiento que simplemente ocurre. Por eso una familia puede amarse profundamente y aun así fallar en cuidarse — el amor sin las prácticas cotidianas que lo sostienen se queda en intención.

El cuidado es lo que las **trayectorias de riesgo** y el **protocolo de seguridad** intentan proteger cuando una familia, por la razón que sea, pierde temporalmente la capacidad de cuidarse a sí misma. La tecnología no puede cuidar en lugar de la familia — pero puede negarse a mirar hacia otro lado cuando el cuidado falla en un punto crítico.

### ¿Qué es el amor entendido como práctica cotidiana?

El amor como emoción es involuntario; aparece o no aparece. El amor como práctica es una elección diaria, deliberada, muchas veces silenciosa: la llamada que se hace aunque no haya nada urgente que decir, la paciencia que se sostiene en el enésimo desacuerdo repetido, el perdón que se ofrece antes de que se lo pidan.

Integrity Family no mide el amor — sería una pretensión absurda e imposible. Pero sí puede hacer visibles sus prácticas: los hábitos, los tiempos compartidos, los gestos que se repiten. Ahí donde la emoción es invisible, la práctica deja huella. Y esa huella es, en parte, lo que el ICF intenta aproximar — no el amor en sí, sino su ejercicio cotidiano y verificable.

### ¿Qué es el legado familiar?

El legado no es lo que una familia deja cuando termina. Es lo que una familia transmite mientras continúa.

No es solamente patrimonio, ni memoria, ni historia contada — es la manera en que una generación le ahorra a la siguiente el tener que aprender, desde cero y por las malas, aquello que ya se comprendió con dolor. Por eso el legado más valioso casi nunca es material: es una forma de mirar el conflicto, de repararse después del error, de nombrar lo que antes no tenía nombre.

El **linaje**, el **ADN cultural** y la **película narrativa** de la familia no documentan el pasado por nostalgia. Lo documentan porque una familia que puede nombrar su propia historia tiene más libertad para decidir cuál de esas historias quiere repetir y cuál quiere, finalmente, dejar de heredar.

### Cómo esta capa sostiene a las otras tres

Estas definiciones no son ornamento filosófico — son la razón de ser de decisiones ya tomadas en el resto del sistema:

- El **CCSF** presupone una definición de familia como sistema de significados, no como estructura fija.
- La **Ruta de Conciencia** (INCONSCIENTE→PLENO) presupone una definición de conversación familiar como vulnerabilidad compartida, no como intercambio de información.
- El **Hogar Digital** presupone una definición de hogar como espejo de un espacio real, no como sustituto de él.
- El **Protocolo de Seguridad** presupone una definición de cuidado como práctica que a veces falla y necesita respaldo externo.
- El módulo de **legado/linaje/ADN** presupone una definición de legado como transmisión activa, no como archivo pasivo.

Sin esta capa, cada módulo es una decisión de producto que parece arbitraria. Con ella, cada módulo es la respuesta técnica a una pregunta antropológica que el proyecto ya se había hecho, aunque no la hubiera escrito todavía.

---

## Lo que nunca debe cambiar

1. **La memoria familiar es patrimonio permanente.** Ningún dato de historia, documental, evidencia o legado puede eliminarse sin consentimiento explícito de la familia.
2. **Los datos pertenecen a la familia, no a la plataforma.** Cualquier familia debe poder exportar toda su información en formatos abiertos (JSON, PDF).
3. **Ninguna IA es fuente única de verdad.** Claude, Gemini u OpenAI generan sugerencias — la familia decide.
4. **El ICF es diagnóstico, no juicio.** El Índice de Cohesión Familiar mide para orientar, nunca para etiquetar.

---

## Lo que puede evolucionar

- Proveedores de IA (Claude → cualquier otro via `AiProvider`)
- Plataforma de despliegue (Railway → cualquier cloud)
- Frontend (Angular → cualquier SPA)
- Integraciones (Alexa, WhatsApp, etc.)

---

## Modelo de transformación familiar

```
Mes 1-6    Reconocimiento (M1) — Ver, diagnosticar, establecer base
Mes 7-12   Amor (M2)           — Construir vínculos, rituales, hábitos
Mes 13-36  Entrega y Legado    — Transmitir, documentar, consolidar
```

**ICF (Índice de Cohesión Familiar):** 0–100, 4 dimensiones
- Emociones · Comunicación · Hábitos · Tiempos

---

## El eje de regulación: de la biología a la identidad

Las "cuatro capas del proyecto" (arriba) responden **qué es** Integrity Family. Esta sección responde algo más estrecho: **por qué camino concreto** viaja una observación de una familia — desde el dato biológico más crudo hasta su consolidación como identidad familiar. No es una quinta capa de arquitectura ni un módulo nuevo: es una forma de leer, en orden, decisiones que ya están tomadas y en producción, dispersas en varios ADRs porque cada una se descubrió por separado, contrastando propuestas externas contra el código real.

| Eje | Qué mide | Dónde vive | Gobernado por |
|---|---|---|---|
| **Biología** | Sueño, ejercicio, nutrición, fatiga | `DailyVitalityLog`, `RecoveryIndexService` (módulo `vitality`) | `RECOVERY_INDEX_HYPOTHESIS` v1 — [ADR-009](adr/ADR-009-panel-biologico-recovery-index-hypothesis.md), `Accepted` |
| **Regulación** | Capacidad de pausa antes de reaccionar | `pauseCapacity` (`RiskAlgoV1Engine`) | `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` v1 — [ADR-007](adr/ADR-007-episodio-procesual-interrupcion-deliberativa.md), `Accepted` |
| **Procesamiento de la experiencia** | Lo que la familia anticipa (`THINK`) vs. lo que realmente ocurre (`AFTERMATH`) en un escenario `SCENARIO_V1_2` | `EvaluationService`, banco `SCENARIO_V1_2` (V89–V95) | `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` v1 — [ADR-008](adr/ADR-008-precision-anticipatoria-proxy-microsimulaciones.md), `Accepted` |
| **Convivencia** | Misiones, dailies, retrospectivas, consejo familiar, reparación de errores | módulos `bitacora`, `council`, `chat`, `errorprotocol` | Sin hipótesis propia todavía — alimenta directamente ICF/hábitos y `SprintRetrospective` |
| **Identidad familiar** | Sostenimiento de `dimScore ≥ 90` durante `≥ 3` ciclos consecutivos, por dimensión ICF | streaks en `FamilyLongitudinalState` (`LongitudinalStateService.onIcfRecalculated()`) | PAF — [ADR-003](adr/ADR-003-identidad-familiar-como-patron-inferido.md) (`Accepted`) y [ADR-005](adr/ADR-005-paf-primer-consumidor-hypothesis-evidence.md), primer consumidor de `hypothesis_evidence` |

Tres reglas mantienen este eje coherente con el resto del sistema:

1. **Ninguna capa escribe directo al ICF ni dispara acciones automáticas.** Cada una primero acumula evidencia versionada en `hypothesis_evidence` ([ADR-004](adr/ADR-004-hypothesis-evidence-pattern.md)) — la promoción a señal operacional (input del ICF, disparador de `AdaptivePlanService`, alerta) requiere evidencia longitudinal real, nunca se asume al momento de instrumentar la captura.
2. **Cada fila es la medición cruda, nunca una interpretación ya calculada** — mismo criterio en las cinco filas de la tabla: se guarda `pauseCapacity` (no "hubo pausa: sí/no"), el índice de recuperación 0-100 (no el semáforo), `dimScore` (no el streak).
3. **El estado de un ADR es el estado real de la hipótesis.** Las cinco filas de la tabla están `Accepted` — cada una con captura implementada, tests pasando y verificación contra MySQL real. Si una fila futura se agrega en estado `Proposed`, significa que esa capa describe una decisión de diseño todavía sin construir, no un mecanismo ya operando como las demás.

Como todo lo que depende de `hypothesis_evidence`, este eje está sujeto a la Regla V1.1.1: es una forma útil de narrar el sistema hoy, no una arquitectura fija — si una capa deja de sostenerse con evidencia, se ajusta o se descarta sin ceremonia.

---

## Arquitectura cognitiva (IF-CAM) — marco conceptual, sin instrumento nuevo

Toda transformación familiar puede leerse como un ciclo de procesamiento de una experiencia: **Percibir → Atender → Interpretar → Relacionar → Imaginar → Evaluar → Decidir → Actuar → Reflexionar → Aprender → Transformación**. La pregunta que responde no es "¿qué siente una familia?" sino "¿cómo procesa una familia una experiencia, y cómo ese procesamiento cambia su forma de convivir?".

Este ciclo (**IF-CAM**, Integrity Family – Cognitive Architecture Model) **no introduce fases ni columnas nuevas** — las 5 fases de `SCENARIO_V1_2` (`NOTICE/THINK/ACT/AFTERMATH/EFFECT`), congeladas por la [Directriz Operativa V1.2](Directriz_Operativa_Piloto_V1.2.md) mientras el piloto está en curso, ya cubren una versión más gruesa del mismo ciclo:

| Fase actual (congelada) | Etapas cognitivas IF-CAM que ya cubre |
|---|---|
| `NOTICE` | Percibir + Atender |
| `THINK` | Interpretar + Relacionar + Imaginar |
| `ACT` | Evaluar + Decidir + Actuar |
| `AFTERMATH` | Insumo crudo de Observar |
| `EFFECT` | Reflexionar + Aprender |

Un refinamiento a las 11 etapas explícitas queda pospuesto a una eventual V1.3 post-piloto — no se toca el instrumento congelado mientras haya familias reales evaluándose sobre él.

**Convergencia independiente:** dos reflexiones externas han llegado por caminos distintos a la misma estructura `percepción → atención → pausa → interpretación → elección → acción`. La primera, sobre la desconexión sensorial cotidiana ("mira sin ver, oye sin escuchar... habla sin pensar") — fenomenología de los sentidos, no diseño de instrumento. La segunda, «Neurociencia del cuerpo» (Nazaret Castellanos): la interocepción — notar la señal corporal antes de interpretar el conflicto — como base de la autorregulación. Ninguna aporta algo operativo nuevo: el tramo que ambas enfatizan (la pausa entre observar y elegir) ya es exactamente `pauseCapacity` — calculado por `RiskAlgoV1Engine`, persistido en `evaluations.pause_capacity` desde V84, y conectado a `hypothesis_evidence` como `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` v1 ([ADR-007](adr/ADR-007-episodio-procesual-interrupcion-deliberativa.md)) — y la "capa corporal previa" que propone Castellanos no es una capa que falte: es la fase `NOTICE` de `SCENARIO_V1_2`, cuyo `phase_prompt` pregunta literalmente "¿Qué fue lo primero que notaste en tu cuerpo?" (V89).

De Castellanos se adopta vocabulario, no instrumento: **abajo-arriba** (*bottom-up*) nombra la ruta `NOTICE → THINK`, donde la señal corporal entra antes que la interpretación; **arriba-abajo** (*top-down*), la reevaluación cognitiva de las fases posteriores. El sistema ya recorre ambas. La única cautela no redundante — *notar la señal corporal no es obedecerla* — ya está en el eje que mide la fase `THINK` ("¿logra hacer una pausa o la reacción es automática?"); una eventual revisión del banco `SCENARIO_V1_2` para descartar opciones de `rubric_level` alto que premien seguir el impulso visceral sin pausa se haría bajo la Regla V1.1.1, con evidencia del piloto, no antes.

Que dos formulaciones independientes converjan en la misma estructura es, en sí, una validación conceptual del ciclo — no un motivo para instrumentar nada adicional.

Ocho dominios cognitivos (percepción, curiosidad, pensamiento crítico, flexibilidad cognitiva, creatividad, pensamiento analítico, pensamiento sistémico, autorregulación corporal), sintetizables en un dominio transversal **CCTF** (Competencias Cognitivas para la Transformación Familiar), quedan registrados como hipótesis candidata a `hypothesis_evidence` — mismo estado que hoy tiene la fila "Convivencia" de la tabla anterior: sin instrumento propio todavía, a la espera de evidencia real de que el proxy actual (`PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS`, ADR-008) no explica ya las divergencias que se observarían.

Ver [ADR-011](adr/ADR-011-if-cam-arquitectura-cognitiva-marco-conceptual.md) para la verificación completa contra el código y el razonamiento de por qué se integra como marco narrativo y no como cuestionarios independientes.

---

## Principio de Altura de Observación — perspectiva multiescala sobre la experiencia familiar

Inspirado en un principio epistemológico, no estético: el documentalista Chi Po-lin (*Beyond Beauty: Taiwan from Above*) mostró que un mismo territorio revela cosas distintas según la altura desde la que se observa — no porque el territorio cambie, sino porque la proximidad oculta lo que la distancia revela, y viceversa. Trasladado a Integrity Family: **un acontecimiento familiar cambia de significado cuando cambia la escala desde la cual se observa**, y ningún evento relevante debería interpretarse desde una sola altura.

Esto no es una capa nueva de arquitectura ni un módulo aislado — es, igual que "El eje de regulación" e IF-CAM (arriba), una forma de leer en conjunto módulos que ya existen, y en un solo caso, señalar uno que todavía falta construir:

| Altura | Pregunta | Dónde vive |
|---|---|---|
| **H1 — Experiencia** | ¿Qué estoy viviendo ahora? | `JournalEntry`, `CriticalDay`, módulos `myspace`/`chat`/`cognitive` |
| **H2 — Perspectivas** | ¿Cómo vivimos el mismo acontecimiento, cada quien? | `JournalEntry.member` + `visibility` + `perceived_event_key` (V112) — mecanismo de captura descrito en [ADR-012](adr/ADR-012-perspectivas-multiples-mismo-evento.md) (`Accepted`) |
| **H3 — Patrón** | ¿Esto ya nos ha ocurrido antes? | `FamilyCausalEngine` (umbrales de deterioro/mejora sobre `dimScore`) cubre el patrón numérico; el patrón narrativo (situación→interpretación→emoción→reacción→consecuencia) queda sin instrumentar |
| **H4 — Trayectoria** | ¿Hacia dónde estamos yendo? | `LongitudinalStateService` + `hypothesis_evidence` (ver "El eje de regulación", arriba) |
| **H5 — Legado** | ¿Qué estamos dejando como forma de vivir juntos? | módulos `legado`, `lineage`, `dna`, `documentary`, `movie` |

De las cinco alturas, cuatro (H1, H3 parcial, H4, H5) ya tenían mecanismo operacional — no ameritan infraestructura nueva, solo esta lectura conjunta. **H2 era la única con un hueco de captura verificado contra el código real**: `JournalEntry` no tenía autor y `CriticalDay` no tenía forma de vincular dos relatos del mismo incidente — ver la verificación completa en [ADR-012](adr/ADR-012-perspectivas-multiples-mismo-evento.md), cerrado por V112.

H2 opera bajo la misma restricción ética que gobierna el resto del sistema: hacer visible lo que normalmente permanece invisible **sin apropiarse de la voz de las personas**. Por eso ADR-012 fija visibilidad privada por defecto — ninguna perspectiva se expone a otro miembro de la familia salvo que su propio autor la comparta explícitamente. Esto conecta directamente con el **CCSF** (arriba): H2 es, en términos operacionales, el instante anterior a que "las experiencias individuales dejen de ser interpretaciones aisladas" — hoy el sistema no tiene dónde guardar la interpretación aislada de cada quien antes de que se convierta en comprensión colectiva.

---

## Ciclo IF-7 — un experimento familiar, no una tarea completada

Varios textos externos analizados en la misma sesión (técnica de Feynman, adaptación del método galileano, «Neurociencia del cuerpo» de Castellanos, conversatorio de José Luis Díaz, prácticas de Tolle) convergen en el mismo circuito, condensable como **IF-7**: **VER → PREGUNTAR → PROPONER → PREDECIR → ACTUAR → CONTRASTAR → APRENDER**.

Igual que "El eje de regulación", IF-CAM y el Principio de Altura de Observación (arriba), IF-7 **no introduce fases, columnas ni servicios nuevos** — es una lectura del ciclo `Diagnóstico → Plan → Misiones → Evidencias → Reevaluación → Aprendizaje → Legado` que ya existe, y de las 5 fases de `SCENARIO_V1_2` (`NOTICE/THINK/ACT/AFTERMATH/EFFECT`):

| Paso IF-7 | Pregunta que responde | Dónde vive hoy |
|---|---|---|
| **VER** | ¿Qué está pasando realmente, sin ponerle todavía una etiqueta? | `JournalEntry`, `CriticalDay`, fase `NOTICE` |
| **PREGUNTAR** | ¿Qué creemos que lo está produciendo? | Diagnóstico / banco de evaluación |
| **PROPONER** | ¿Qué explicación provisional construimos? | `ImprovementPlan`, fase `THINK` |
| **PREDECIR** | ¿Qué esperamos que ocurra si actuamos así? | `FamilyPrediction` (`twin`) — **hueco verificado, ver abajo** |
| **ACTUAR** | El experimento concreto | `PlanTask`, `SprintMission`, fase `ACT` |
| **CONTRASTAR** | ¿Qué ocurrió realmente? | `TaskEvidence`, `SprintDaily`, fase `AFTERMATH` |
| **APRENDER** | ¿Qué sabemos ahora que no sabíamos antes? | `SprintRetrospective`, fase `EFFECT`, Reevaluación |

El paso PREDECIR es, hoy, el eslabón más débil: `DigitalTwinService` crea predicciones (`family_predictions`, `status=ACTIVE`) pero nada las contrasta después — el loop PREDECIR→CONTRASTAR está abierto, sin `plan_task_id` que ate una predicción a una misión concreta. Se registra como deuda técnica conocida, no se construye el pre-registro ni el servicio de contraste hasta que un caso real lo reclame (ver [ADR-013](adr/ADR-013-ciclo-if7-marco-conceptual.md)). `FamilyCausalEngine`, pese a su nombre, implementa reglas heurísticas de correlación con explicabilidad — no causalidad `misión → resultado`; IF-7 no cambia eso.

### Métrica ≠ propósito (riesgo de Goodhart)

Un segundo texto de la misma sesión plantea un riesgo distinto: que el indicador sustituya aquello que originalmente medía —`actividad valiosa → métrica → recompensa` invertido en `actividad → medio para conseguir recompensa`—. Aplicado a Integrity: una familia podría aprender a completar misiones, subir adherencia o avanzar de nivel sin que la convivencia real cambie.

Verificado contra el código antes de escribir esto (ver ADR-013): **no existe sistema de puntos, insignias ni niveles desbloqueables**, y el ICF se calcula desde evaluaciones, no desde volumen de misiones completadas — el "juego de puntos" que el riesgo describe no está construido. El único vector real y verificado es más estrecho: `MilestoneService.evaluate()` liga el avance de hito a `% de tareas completadas`, además del ICF. Por eso esto se documenta como **principio de diseño hacia adelante**, no como corrección de algo roto:

> Cualquier métrica de adherencia o cumplimiento que se proponga debe describir el proceso — nunca convertirse en el objetivo que la familia persigue.

### Autonomía Familiar — hipótesis candidata, no métrica

El mismo texto sugiere que el éxito de Integrity no debería medirse por cuánto usa la familia la plataforma, sino por cuánto deja de necesitarla — con la autoría de misiones desplazándose gradualmente de la IA hacia la familia. El modelo de datos ya no lo bloquea del todo: `ChecklistController.createItem()` acepta un `source` sin forzar un enum cerrado a "IA". Pero **no existe hoy ningún constructo, medición ni umbral de autonomía** — formalizarlo ahora repetiría el error que la Regla V1.1.1 y [ADR-004](adr/ADR-004-hypothesis-evidence-pattern.md) existen para evitar.

Queda registrado como hipótesis candidata a `hypothesis_evidence`, mismo estado que hoy tiene CCTF (ver IF-CAM, arriba): sin instrumento propio, a la espera de un disparador concreto — por ejemplo, que el piloto V1.2 muestre familias con ICF estable o ascendente pero dependientes de forma sostenida de las sugerencias de la IA.

---

## Usuarios objetivo

- Familias nucleares con hijos (principal)
- Familias reconstituidas
- Guardián familiar (terapeuta/coach externo)
- Administrador del sistema (William Lopez)

---

## Horizonte temporal

El sistema debe poder funcionar y preservar la memoria familiar durante **20-30 años**, independientemente de cambios tecnológicos.
