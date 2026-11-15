# ADR-018: Separar lo ocurrido de lo interpretado ("Antes de concluir, pregunta")

## Estado

Propuesto. Pendiente de aprobación antes de tocar código o crear la migración.

## Contexto

Se propuso una práctica para que el miembro deje de ser solo un observador
y participe aprendiendo: distinguir lo que ocurrió de lo que interpretó, y
elegir una respuesta que pueda revisar con los demás. Apunta a un ciclo
frecuente:

> «Creo que no me escuchan → hablo acusando → la otra persona se retira →
> interpreto su retirada como confirmación».

La propuesta original ("Antes de concluir, pregunta") incluía una misión que
cruza las 4 dimensiones del ICF, una bitácora de 5 preguntas, límites para
el consultor de IA y un seguimiento separado de cumplimiento, sensación de
escucha y presión percibida.

### Lo que ya existe (verificado en el código antes de diseñar)

| Elemento propuesto | Ya cubierto por |
|---|---|
| Bitácora guiada (ocurrió / sentí / hice / acordar / aprender) | `FamilyErrorProtocol`: pasos `DETECT → FEEL → UNDERSTAND → ACTION → AGREEMENT → FOLLOWUP → LEARNING` |
| Perspectiva del otro sobre el mismo episodio | `JournalEntry.perceivedEventKey` (ADR-012) y `BiographyAnswer.perceivedEventKey` (ADR-017) |
| Fuente de lo que se afirma | `EpistemicSource` (`OBSERVED`/`TOLD_BY_OTHER`/`DOCUMENT`/`UNCERTAIN`) |
| Realización del acuerdo | `PlanTask` / `JournalEntry.complianceStatus` |
| Presión percibida | `SprintRetrospective.tensionLevel` (1–10, un único valor por retrospectiva) |
| No alterar ICF/riesgo/conciencia con resultados no validados | Regla V1.1.1 + `hypothesis_evidence` (ADR-004, V106) |

### Lo que falta

1. **No hay dónde separar el hecho de su atribución.** "Acordamos hacer
   esta tarea el martes y no se hizo" (un incumplimiento) y "no se hizo
   porque no le importa la familia" (una atribución que hay que contrastar)
   hoy van al mismo campo de texto libre: `FamilyErrorProtocol.whatHappened`,
   o `JournalEntry.observations`/`reflection`. Esa separación es el núcleo de
   la práctica.
2. **No se mide la escucha por participante.** `tensionLevel` es un único
   número por retrospectiva. La propuesta exige ver por separado, para cada
   persona, si se sintió escuchada, cuánta presión sintió y si está dispuesta
   a continuar. Si cumplir más va acompañado de más presión, hay que revisar
   la práctica.
3. **No hay un límite explícito para que la IA no atribuya intenciones.**
   Un grep sobre `PromptGenerator`/`CopilotService` no encontró ninguna regla
   que prohíba adjudicar intenciones o decidir entre versiones
   contradictorias. Hallazgo relacionado: `PostSessionAnalyzer` le pide al
   modelo clasificar al miembro como
   `OPEN | STRUGGLING | IMPROVING | RESISTANT | NEUTRAL` a partir de una sola
   conversación, y `SessionMemoryConsolidator` lo guarda en memoria. Una
   etiqueta como `RESISTANT` es justo el tipo de atribución que esta
   práctica busca evitar.

## Decisión

### 1. La separación va en `JournalEntry`, no en `FamilyErrorProtocol`

- `FamilyErrorProtocol` es por familia (`familyId`, sin autor) y está
  pensado para misiones fallidas. No modela que dos personas narren el
  mismo episodio.
- `JournalEntry` ya tiene un autor (`member`, V112), visibilidad,
  `perceivedEventKey` para cruzar perspectivas y un origen (`origin`). Es
  el lugar natural para un episodio relacional.

**Migración (próximo número libre; V114 la tomó `sprint_dailies.attempt_status`)** (solo DDL, columnas NULL, sin backfill; Amendment 1 agrega `body_signal`, Amendment 3 agrega `explicit_responses`):

| Columna | Pregunta de la bitácora |
|---|---|
| `observations` (existente) | ¿Qué ocurrió concretamente? Se reinterpreta como "lo observable", solo en documentación y UI |
| `interpretation` (nueva, TEXT NULL) | ¿Qué interpreté? |
| `emotion` (existente) | ¿Qué sentí? |
| `alternative_explanation` (nueva, TEXT NULL) | ¿Qué otra explicación considero? |
| `reflection` (existente) | ¿Qué hice y qué expresó la otra persona? |
| `learning` (existente) | ¿Qué corresponde mantener, reparar o cambiar? |

Solo se agregan dos columnas. No se crea un módulo nuevo ni un enum de pasos.

### 2. La escucha por participante va a `hypothesis_evidence`, no a estado operacional

Cada participante del episodio puede registrar tres observaciones (MANUAL)
en la tabla que ya existe, sin cambiarle el schema:

- `hypothesis = 'ANTES_DE_CONCLUIR_PREGUNTA'`, `hypothesis_version = '0.1'`
- `subject_type = 'FAMILY_MEMBER'`, `subject_id = member.id`
- `measurement_type` ∈ `FELT_HEARD`, `PERCEIVED_PRESSURE`, `WILLINGNESS_TO_CONTINUE` (escala 1–5)
- `instrument = 'JOURNAL_EPISODE_FOLLOWUP'`, `instrument_version = '0.1'`

Estas observaciones **no** modifican el ICF ni `risk_level` ni la Ruta de
Conciencia. La regla "más cumplimiento con más presión → revisar la
práctica" se aplica leyendo los datos, no como un disparador automático.

### 3. La misión es contenido, no código

"Antes de concluir, pregunta" se carga como una misión de sprint con su
acción y su evidencia mínima por dimensión:

| Dimensión | Acción | Evidencia mínima |
|---|---|---|
| Emociones | Nombrar lo que siento sin atribuirlo a la intención del otro | «Sentí frustración» |
| Comunicación | Describir el episodio y preguntar por la perspectiva del otro | «Ayer no terminamos de hablar. ¿Qué ocurrió para ti?» |
| Hábitos | Elegir una conducta propia que se pueda repetir | Dejar terminar una intervención antes de responder |
| Tiempos | Acordar cuándo conversar y cuándo revisar | Un horario aceptado por quienes participan |

La misión usa los mecanismos que ya existen (`SprintMission`, evidencias).

### 4. Límites para el consultor de IA

Cuando el contexto del prompt incluya episodios de diario con
`interpretation`/`alternativeExplanation`, se agrega al prompt una regla
explícita:

- Puede formular preguntas y señalar información que falta.
- No adjudica intenciones a otro miembro.
- No resuelve versiones contradictorias sin evidencia: muestra las
  perspectivas (`perceivedEventKey`) sin decidir cuál es la verdadera.
- No convierte una interpretación en un diagnóstico familiar.

## Amendment 1 — señal corporal y límites de la reinterpretación (2026-11-15)

Origen: la evaluación crítica de una síntesis de la conversación entre
Verónica Gamio y David del Rosario (pensamiento como propuesta, emoción
construida, interocepción). No se verificó el video. Se conservan tres
criterios: **evidencia, utilidad y responsabilidad**. La utilidad no
reemplaza a la evidencia: una creencia falsa puede tranquilizar y una
información verdadera puede incomodar.

**Una columna más en la misma migración:** `body_signal` (TEXT NULL), "¿qué noto en el
cuerpo?", separada de `emotion` ("¿cómo lo nombro?"). Así la bitácora queda
en 6 pasos:

| Paso | Pregunta | Campo |
|---|---|---|
| Observar | ¿Qué ocurrió de forma observable? | `observations` |
| Distinguir | ¿Qué interpretación apareció? | `interpretation` |
| Registrar | ¿Qué noto en el cuerpo y cómo lo nombro? | `body_signal` + `emotion` |
| Contrastar | ¿Qué evidencia tengo y qué me falta saber? | `alternative_explanation` |
| Elegir | ¿Qué acción respeta mis necesidades y las del otro? | `reflection` |
| Revisar | ¿Qué ocurrió después? | `learning` + `complianceStatus` |

La señal corporal se registra, **no decide**: no determina si una
interpretación es verdadera ni si una acción conviene. Esto no se unifica
con el modelo `NEURO_AWARENESS` (Señal Corporal → Conciencia → Acción) de la
evaluación, que es un instrumento de medición distinto (ver CLAUDE.md, Ruta
de Conciencia).

**Límites ya aplicados en `SAFETY_RULES`** (reglas 7 y 8, comunes a los tres
modos del mentor):

- Un insulto, una amenaza o una agresión relatados **no** se reencuadran
  como un error de interpretación. Decir "no es el insulto, es tu
  interpretación" desplaza la responsabilidad hacia quien recibe la
  conducta. Cada persona responde por su conducta, y lo primero es la
  protección.
- No se infieren estados cerebrales, hormonales ni fisiológicos a partir de
  un relato, ni se usa la calma como prueba de éxito.

**Explicación rival para la validación:** que la mejora se deba solo al
tiempo extra de conversación. Una comparación válida debe igualar tiempo y
atención entre grupos y medir resultados posteriores (cumplimiento,
reparación), no solo la satisfacción inmediata.

## Amendment 2 — libertad para disentir, necesidad y privacidad real (2026-11-15)

Origen: una segunda propuesta de aplicación ("observar → contrastar →
actuar → revisar"). Casi todo ya está cubierto por la decisión y el
Amendment 1. Aporta tres cosas nuevas:

**1. Libertad para disentir como cuarta medida.** Que haya menos discusiones
puede significar acuerdo o puede significar miedo a expresarse. El silencio
o la obediencia no son indicadores de armonía. Se agrega
`measurement_type = FREE_TO_DISAGREE` (1–5) a las medidas por participante
en `hypothesis_evidence`, que pasan a ser cuatro. La práctica se considera
útil solo si `FELT_HEARD` y `FREE_TO_DISAGREE` suben o se mantienen sin que
suba `PERCEIVED_PRESSURE`. La percepción de **seguridad física** no se mide
como una escala 1–5: si aparece una amenaza, coerción o violencia, la
práctica se detiene y se pasa al protocolo de seguridad (V98,
`safety_protocol_activations`), que exige confirmación humana.

**2. "Esto necesito" va en el texto, no en el schema.** El paso *Elegir* se
presenta en la UI como "¿Qué necesito y qué acción elijo?" y se guarda en
`reflection`. No se agrega columna.

**3. Precondición de privacidad — RESUELTA (2026-11-15, ADR-012 Amendment 1).**
Pertenecer a la misma familia no debe dar acceso automático a un registro
personal. Al escribir este amendment sí lo daba:
`JournalEntryRepository.findVisibleToMember` trataba `viewerMemberId IS NULL`
como bypass, y `SecurityValidator.resolveViewerMemberId` devolvía null para el
creador de la familia y para `ROLE_ADMIN` (ADR-012). El mismo patrón estaba en
`CriticalDayRepository` y en `FamilyTimelineController`. Con eso, la
`interpretation` y el `body_signal` marcados como PRIVATE los habría leído el
creador de la familia.

Se quitó el bypass con el mismo criterio que en el diario biográfico
(ADR-017): lo PRIVATE solo lo ve su autor. Antes se verificó que ni
`guardian` ni `risk` ni `context` dependen de ese acceso. Ver ADR-012
Amendment 1. La migración ya no está bloqueada por privacidad; solo falta aprobar
este ADR.

**Ritmo de la práctica:** es breve y voluntaria, vinculada a un episodio que
la persona elige, y se puede omitir cualquier pregunta (todas las columnas
nuevas son NULL). No se pide foto ni grabación como evidencia: basta con
registrar si conversaron y qué acordaron. La IA no interviene en la primera
versión. Se incorpora solo cuando haya una necesidad demostrada y un
criterio para evaluar sus errores.

## Amendment 3 — no usar las micro-simulaciones, respuestas explícitas y pausa como comparador (2026-11-15)

Origen: una propuesta de integrar la observación corporal en la secuencia
de micro-simulación `NOTICE → THINK → ACT → AFTERMATH → EFFECT`.

**1. La práctica no se registra en las micro-simulaciones.** Esa secuencia
ya existe, pero es un **instrumento de evaluación** (`SCENARIO_V1_2`,
V89–V95, 22 escenarios con las 5 fases): situaciones hipotéticas o
recordadas, opciones cerradas puntuadas con `rubric_level` 1–5, y `EFFECT`
autoinformado en la misma sesión. Esta práctica registra episodios reales,
con texto libre, sin puntaje, y un seguimiento días después. Mezclarlas
tendría dos efectos:

- Contaminaría `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` (ADR-008), que compara
  `THINK` con `AFTERMATH` dentro de una sola sesión. Con episodios reales y
  seguimiento diferido se mezclarían dos poblaciones distintas.
- Convertiría una práctica voluntaria en una evaluación permanente, justo la
  vigilancia que este ADR quiere evitar.

Se mantiene el registro en `JournalEntry`. La UI puede usar **el mismo
vocabulario** (Notar → Interpretar → Elegir → Revisar → Comprobar) para que
la familia reconozca el recorrido, sin compartir tablas, preguntas ni
puntajes con la evaluación.

**2. Respuestas explícitas, no un NULL ambiguo.** En el diseño original un
campo NULL podía significar "omití la pregunta" o "no noté nada", y para
cualquier análisis son datos distintos. Cada paso de la práctica acepta,
además del texto libre, una respuesta explícita que se guarda tal cual:

| Respuesta explícita | Significa |
|---|---|
| `NONE_NOTICED` | "No identifico ninguna señal" (es un dato, no una ausencia) |
| `UNKNOWN` | "No sé" |
| `DECLINED` | "Prefiero no compartir" |
| `NOT_HELPFUL` | "No funcionó" (solo en los pasos Revisar y Comprobar) |

En esa migración esto se agrega como una sola columna JSON NULL,
`explicit_responses` (mapa paso → respuesta), en vez de una columna por
paso. NULL sigue significando "no respondido". `DECLINED` nunca se muestra
a otros miembros como si fuera una respuesta.

**3. Comparador de validación: pausa sola contra pausa con observación
corporal.** Es más fuerte que el comparador del Amendment 1 (igualar tiempo
de conversación), porque aísla el componente corporal. Se registra como una
hipótesis propia en `hypothesis_evidence`:

- `hypothesis = 'BODY_SIGNAL_PAUSE_HYPOTHESIS'`, `hypothesis_version = '0.1'`.
- Se relaciona con `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` (ADR-007,
  `pauseCapacity`) sin duplicarla. ADR-007 mide la capacidad de pausa en
  la evaluación; esta hipótesis compara si **agregar** la observación
  corporal a una pausa acordada cambia los resultados de episodios reales.
- Contexto: solo desacuerdos cotidianos de baja intensidad. Una amenaza,
  coerción o violencia sale del experimento y va al protocolo de seguridad
  (V98).
- Medidas de conducta, reportadas por cada participante:
  `CONVERSATION_RESUMED`, `INSULTS_OR_INTERRUPTIONS`, `AGREEMENT_REACHED`
  y `AGREEMENT_KEPT` (este último en el seguimiento).
- Medidas de aceptación: `PRACTICE_COMFORT` y `PRACTICE_USEFUL`.
- La calma subjetiva se registra como un resultado **separado**, nunca como
  criterio de éxito.

**Se mantiene, sin cambios:** los sensores y la VFC se posponen hasta que
haya una necesidad concreta (su asociación con la función ejecutiva es
pequeña y no justifica una calificación individual ni familiar). La señal
corporal no decide verdad ni conveniencia (Amendment 1). La pausa nunca
sirve para exigir serenidad ni silenciar una queja (`SAFETY_RULES` regla
7). Nada de esto modifica el ICF ni los niveles de conciencia.

## Alcance explícitamente excluido

- **Ningún vínculo con hipótesis físicas.** La conversación de la que
  surgió esta práctica trataba una hipótesis de restricción temporal
  conjunta. Que la práctica resulte útil no aporta evidencia a favor de esa
  hipótesis, ni de un mecanismo cuántico o de "manifestación". No se
  referencian entre sí en código ni en documentación de producto.
- No hay puntaje automático de "calidad de interpretación" ni clasificación
  por IA de los episodios.
- ~~`memberState`/`RESISTANT`~~ — **resuelto aparte (2026-11-15)**:
  `buildSessionSynthesisPrompt` ahora pide `sessionTone` (describe la
  conversación: `EXPLORATORY | DIFFICULT | PROGRESS_REPORTED | BRIEF | NEUTRAL`)
  y `SessionMemoryConsolidator` descarta cualquier `memberState` residual.
  La regla anti-atribución (punto 4 de la Decisión) ya está en
  `SAFETY_RULES`, que comparten los tres modos del mentor y el camino sin
  contexto. Queda pendiente `buildIdentityAnalysisPrompt`
  (`changeResistance`, `AVOIDANT`), que alimenta el perfil de identidad
  persistido y requiere su propio análisis.

## Consecuencias

- Costo bajo: una migración con dos columnas NULL, campos nuevos en
  DTO/servicio/UI del diario, una regla de prompt y contenido de misión.
- Las observaciones por participante quedan versionadas por hipótesis e
  instrumento, así que se pueden evaluar o descartar sin tocar el estado
  operacional.
- Riesgo: si la UI presenta `interpretation` como "tu error", la práctica
  culpabiliza en vez de ayudar a contrastar. El texto de la UI debe hablar
  de "lo que pensé", nunca de "lo que interpreté mal".

## Validación pendiente (Regla V1.1.1)

Hipótesis: separar observación, interpretación y acción facilita algunos
acuerdos sin aumentar la presión percibida. Hoy no hay resultados en las
familias participantes. La práctica se considera útil solo si, en las
familias que la usan, `FELT_HEARD` y `FREE_TO_DISAGREE` suben o se mantienen sin que suba
`PERCEIVED_PRESSURE`.
