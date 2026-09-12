# ADR-004: Hypothesis Evidence Pattern — evidencia append-only independiente del estado operacional

**Status:** Accepted
**Date:** 2026-07-16
**Deciders:** William Lopez

## Context

**Motivación:** los estados operacionales (`family_longitudinal_state` y equivalentes) están optimizados para el funcionamiento del producto y pueden sobrescribirse. Esa característica, necesaria para operar, los vuelve insuficientes como fuente de evidencia para validar hipótesis longitudinales. Este ADR establece un mecanismo independiente para preservar evidencia reproducible, sin alterar el diseño operacional existente.

El problema se hizo visible al implementar ADR-003 (streak de PLENO sostenido por dimensión, como base inferida de Identidad Familiar). `FamilyLongitudinalState` es una fila única por familia (`@UniqueConstraint(columnNames = "family_id")`, sin historial) — el mismo diseño que ya resuelve `consecutiveImprovements`/`consecutiveDeteriorations` para operar el producto. Sirve perfectamente para eso. Pero significa que **hoy no se puede auditar retrospectivamente** si una familia realmente sostuvo 3 ciclos en ≥90, ni con qué valores exactos — el streak actual sobrevive, la curva que lo produjo no.

Esto no es un problema exclusivo de PAF. El proyecto ya resolvió una versión de este mismo problema antes, para un caso distinto — y ese precedente sí funcionó:

> *"Ningún cambio al banco de escenarios (`scenario_bank`) podrá realizarse directamente por intuición. Todo cambio futuro deberá originarse en evidencia real recolectada en `scenario_validation_log` durante este piloto (...) Esta regla convierte a Integrity Family en un instrumento versionado, reproducible y científicamente auditable."*
> — Regla V1.1.1, `docs/Validacion_Tecnica_Piloto_V1.1.md:108-116`

`scenario_validation_log` cumplió exactamente esa función para `scenario_bank`: una tabla de solo-registro (append-only), separada del estado operacional, que permitió que el banco evolucionara por evidencia y no por intuición. `vision.md` y ADR-002/003 ya citan la Regla V1.1.1 por analogía para gobernar hipótesis nuevas (PAF, Determinantes Transformacionales de V96), pero ninguna de ellas tiene su propio `scenario_validation_log` — cada una repetiría el problema por separado si no se generaliza el patrón ahora.

### Definición: qué es una hipótesis, para efectos de este ADR

El ADR usa "hipótesis" continuamente sin definirlo — eso invita a interpretaciones distintas. Se fija aquí:

> **Hipótesis:** afirmación explícita del modelo de Integrity Family cuya validez depende de evidencia longitudinal, y que puede evolucionar mediante versionado independiente del versionado del software.

Por exclusión, una hipótesis **no** es: una regla técnica, una clase Java, un endpoint, una configuración, ni un flag de feature. `PAF` (ADR-003) y los Determinantes Transformacionales (V96) son hipótesis bajo esta definición; `SecurityValidator.validateFamilyOwnership()` no lo es, aunque también sea "lógica de negocio" — no depende de evidencia longitudinal para ser correcta.

### Alcance descartado deliberadamente

Una versión más ambiciosa de esta decisión propuso un bounded context `research` completo: entidades `ResearchStudy`/`ResearchParticipant`/`Phenomenon`/`Intervention`/`Outcome`, roles (Investigador Principal, Coordinador, Monitor, Estadístico, Comité Científico), un Research Warehouse separado de la base operacional, y generadores de reportes bajo estándares clínicos (STROBE, CONSORT, PRISMA, SPIRIT, CARE, TRIPOD). Se descarta por ahora — no por estar mal pensado, sino por violar el principio que el propio proyecto ya aplicó en interoperabilidad:

> *"No se recomienda construir el API Gateway/OAuth2 completo antes de tener un consumidor real (una IPS, SISPRO) del otro lado."* — `CLAUDE.md`

Hoy el sistema tiene una hipótesis con mecanismo construido (PAF/streak, ADR-003) y cero filas de evidencia acumuladas. Diseñar un Comité Científico y un pipeline PRISMA antes de que exista una sola fila real en una tabla de evidencia es la misma trampa, solo que en investigación en vez de en interoperabilidad. Esa visión de largo plazo queda registrada en Consequences, no descartada — pero no se implementa aquí.

## Decision

**Toda hipótesis del sistema que pretenda evolucionar mediante evidencia longitudinal deberá registrar sus observaciones crudas en `hypothesis_evidence`, un mecanismo append-only independiente del estado operacional.**

(Se descarta el nombre `research_evidence`: presupondría que investigación es el único consumidor, cuando la misma tabla sirve también para auditoría, reproducibilidad, entrenamiento de IA y debugging. Se descarta también `evidence_log`: colisiona semánticamente con `audit_events`, que ya existe en el dominio para el rastro de auditoría del sistema — un consumidor distinto, con un propósito distinto.)

`hypothesis_evidence` constituye un registro de **observaciones primarias**, no de conclusiones ni de inferencias. Una fila válida es `ICF=92, streak=3, dimensión=Comunicación` — no `"la familia mejoró"`. La interpretación ocurre en el análisis posterior, nunca al momento de escribir la fila; escribir una conclusión en vez de una medición rompe la reproducibilidad que este ADR existe para garantizar.

### Decisión 1 — el estado operacional nunca es evidencia

Toda evidencia utilizada para validar o refutar una hipótesis deberá registrarse en `hypothesis_evidence`. Los estados operacionales podrán sobrescribirse cuando sea necesario para el funcionamiento del sistema, pero **no constituyen evidencia primaria para la validación de hipótesis** — no importa cuántos campos de "estado actual" se agreguen a una entidad operacional, ninguno sustituye a `hypothesis_evidence`. Esto es deliberado: evita que en el futuro alguien argumente "ya guardamos el último valor, no necesitamos la tabla de evidencia" — el punto no es que el estado se pueda perder, es que el estado *nunca fue* evidencia, sin importar si se pierde o no.

(Precisión: una fila en `hypothesis_evidence` tampoco es, por sí sola, "evidencia científica" — es un dato primario. La evidencia científica en sentido estricto aparece después del análisis. Este ADR gobierna dónde vive el dato primario que hace posible ese análisis, no el análisis mismo.)

### Decisión 2 — toda observación es autocontenida y trazable

Cada fila de `hypothesis_evidence` debe registrar, como mínimo:

| Campo | Propósito |
|---|---|
| `hypothesis` | Qué hipótesis respalda esta observación (ej. `PAF`, `DETERMINANTES_TRANSFORMACIONALES`) |
| `hypothesis_version` | Bajo qué versión de la **definición de la hipótesis** se registró — no la versión del software ni del release. Precisión deliberada: sin esto, un futuro `v2.1` en esta columna es ambiguo (¿es Spring Boot? ¿el instrumento? ¿la hipótesis?). Gobernanza: toda modificación de `hypothesis_version` para una hipótesis existente deberá quedar documentada en el ADR de esa hipótesis (o mecanismo formal equivalente) — no se cambia "porque sí" |
| `subject_type` / `subject_id` | Sobre qué entidad se observó (familia, miembro, dimensión). Catálogo abierto: nuevos `subject_type` (ej. `MISSION`, `SPRINT`, `PHENOMENON`, `COMMUNICATION_EPISODE`) podrán incorporarse sin reabrir este ADR, siempre que representen entidades reales, persistentes o conceptuales, definidas por el dominio — no valores ad hoc como `TEMP`/`TEST`/`OTHER`/`UNKNOWN` |
| `measurement_type` / `measurement_value` | Qué se midió y su valor |
| `instrument` / `instrument_version` | Con qué instrumento se midió, y su versión — si el instrumento cambia, la observación sigue siendo interpretable |
| `source` | Origen de la observación: `MANUAL` / `AUTOMATIC` / `DERIVED` / `IMPORT` / `SIMULATION` (mismo patrón de string-literal ya usado por `FamilyJournalEntryEvent`, que hoy distingue origen `"MANUAL"` de otros) |
| `observed_at` / `recorded_at` | Instante efectivo de la observación vs. instante de persistencia. En un sistema con `@Async @EventListener` sobre RabbitMQ (como `LongitudinalStateService` hoy), ambos pueden diferir por el tiempo en cola — distinguirlos evita ambigüedad si esa brecha alguna vez importa para el análisis. Cuando ambos coinciden, `recorded_at` se puebla automáticamente desde la infraestructura de persistencia (`created_at` estándar) sin lógica especial |

`source` se agrega no por necesidad de investigación, sino de trazabilidad: distinguir una observación generada por un evento real de una derivada, simulada o importada cuesta casi nada al momento de escribir la tabla y evita ambigüedad después.

### Decisión 3 — ninguna hipótesis se considera validada sin evidencia histórica

Ninguna hipótesis podrá considerarse validada ni incorporarse al conocimiento estable del sistema utilizando únicamente estados operacionales. Requerirá evidencia histórica reproducible registrada en `hypothesis_evidence`. Este ADR no decide qué significa "incorporarse al conocimiento estable" en cada caso — cada hipótesis, en su propio ADR, decide si eso implica modificar `vision.md`, cerrar un action item, cambiar un algoritmo o liberar una versión. Ese detalle no le corresponde a este ADR.

### Principio de no reconstrucción

Una observación registrada en `hypothesis_evidence` constituye la fuente primaria de verdad para esa hipótesis y no deberá requerir reconstrucción a partir de estados operacionales ni de eventos históricos para ser interpretada. Este principio cierra explícitamente la alternativa descartada de "reconstruir la historia desde los eventos del bus" (considerada y rechazada al diseñar este ADR): no garantiza que existan todos los eventos, ni que no cambien, ni que la reconstrucción produzca siempre el mismo resultado — y la evidencia científica no debería depender de una reconstrucción. Si algo necesita ser evidencia, se escribe como evidencia directamente; no se infiere después.

### Regla V1.1.1 — alcance explícito, no total

La Regla V1.1.1 pasa a gobernar **toda hipótesis científica o conductual incorporada al modelo de Integrity Family** — no "todo el sistema". Existen módulos sin hipótesis que validar (autenticación, caché, Docker, RabbitMQ, correlación de requests, seguridad HTTP): la regla no les aplica, y decir "todo el sistema" invitaría a interpretaciones absurdas. El texto original de la regla permanece en `docs/Validacion_Tecnica_Piloto_V1.1.md` (es correcto históricamente — ahí se aplicó por primera vez, a `scenario_bank`); este ADR la generaliza como principio, no reescribe su origen.

### Convención de nomenclatura — `hypothesis` y `hypothesis_version`

Resuelve el Action Item 4: antes de que exista un segundo consumidor de `hypothesis_evidence`, la convención debe estar fijada — no cada hipótesis nueva inventando su propio formato. PAF (ADR-003/005) es el caso fundacional y no necesitó esperar esta convención; el siguiente consumidor sí debe seguirla.

**`hypothesis` (código):**
- `UPPER_SNAKE_CASE`, corto, mnemónico — no texto libre ni el nombre completo de la hipótesis (`PAF`, no `Patrimonio Automático Familiar`).
- Debe coincidir exactamente con el identificador usado en el ADR que define la hipótesis, y quedar registrado en la tabla de abajo antes de escribir la primera fila.

**`hypothesis_version`:**
- Formato `"v<entero>"` — `v1`, `v2`, sin decimales ni fechas. No es semver: una hipótesis no tiene "parches", tiene versiones de su definición.
- Se incrementa únicamente cuando cambia la definición testeable de la hipótesis (umbral, fórmula, población, alcance) — no por cambios de código que no alteren qué se está afirmando. Documentado en el ADR que gobierna esa hipótesis (ver Decisión 2, gobernanza de `hypothesis_version`).

**Registro de hipótesis activas** (interino — reemplaza, sin construir todavía, el catálogo `Hypothesis` mencionado en Consequences → Habrá que revisitar; se formaliza como tabla solo si el número de hipótesis lo justifica):

| Código | `hypothesis_version` actual | Gobernado por |
|---|---|---|
| `PAF` | `v1` | ADR-003 (definición) · ADR-005 (consumidor) |
| `DELIBERATIVE_INTERRUPTION_HYPOTHESIS` | `v1` | [ADR-007](./ADR-007-episodio-procesual-interrupcion-deliberativa.md) (definición y consumidor) |
| `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` | `v1` | [ADR-008](./ADR-008-precision-anticipatoria-proxy-microsimulaciones.md) (definición y consumidor) |
| `RECOVERY_INDEX_HYPOTHESIS` | `v1` | [ADR-009](./ADR-009-panel-biologico-recovery-index-hypothesis.md) (definición y consumidor) |

Toda hipótesis nueva que escriba en `hypothesis_evidence` debe agregar su fila aquí, en el ADR que la introduce, antes de su primera escritura.

## Trade-off Analysis

Frente a no hacer nada (seguir citando V1.1.1 por analogía, sin tabla): más simple hoy, pero cada hipótesis nueva (Determinantes, Fenómenos, cualquier futura) repetiría el mismo problema de auditabilidad que ADR-003 encontró — el costo se paga una vez por hipótesis, en vez de una vez por el sistema completo.

Frente al bounded context `research` completo: se pierde la ambición de "plataforma científica" a corto plazo, pero se evita construir cinco roles, un Warehouse y generadores de reportes sin un solo consumidor que los necesite todavía. El costo de construir de más es mucho más caro de revertir que el costo de esperar a que un segundo consumidor real lo justifique.

## Consequences

- **Más fácil:** cualquier hipótesis futura (Determinantes Transformacionales de V96, Fenomenología Computacional si se formaliza, o cualquier otra) tiene desde el día uno un lugar donde registrar evidencia reproducible, sin diseñar su propia tabla ad-hoc.
- **Más fácil (adicional):** la misma infraestructura habilita, sin trabajo extra, análisis retrospectivos, auditorías metodológicas y entrenamiento de modelos predictivos que no dependan del estado operacional vigente — no es una capacidad exclusiva de "investigación", aunque investigación sea el primer caso que la motivó.
- **Más difícil:** cada consumidor de `hypothesis_evidence` debe decidir en su propio ADR qué constituye una observación válida para su hipótesis específica — este ADR da la forma de la tabla, no la semántica de cada hipótesis.
- **Habrá que revisitar (visión de largo plazo, no comprometida a implementarse):** si `hypothesis_evidence` acumula múltiples hipótesis activas con volumen real y aparece un segundo o tercer consumidor que lo justifique, evaluar entonces: un bounded context `research` separado de `clinical`/`family`; roles diferenciados (Investigador Principal, Coordinador, Monitor, Estadístico, Comité Científico); un Research Warehouse replicado y anonimizado, separado de la base operacional; generadores de reportes bajo estándares clínicos (STROBE/CONSORT/PRISMA/SPIRIT/CARE/TRIPOD); un objeto de dominio `Phenomenon` como unidad científica en vez de `Family`. Ninguna de estas piezas se descarta — se posponen hasta que la evidencia de uso real las justifique, no antes. También pertenece aquí: si el número de hipótesis activas crece más allá de dos o tres, formalizar un catálogo `Hypothesis` (id, code, version, status, owner, ADR, referencia a `vision.md`) para evitar que `hypothesis`/`hypothesis_version` se conviertan en cadenas de texto dispersas y sin fuente única de verdad (`"PAF"`, `"PAF_V2"`, `"DET_TRANS"`, ...). No se implementa con una sola hipótesis activa (PAF) — el costo de mantenerlo sin un segundo caso real no se justifica todavía.

## Action Items

1. [x] Migración `V106` — crea tabla `hypothesis_evidence` (columnas de la Decisión 2), sin FK obligatoria a una entidad específica. Verificada contra MySQL real (Docker) vía `FamilyLifecycleIntegrationTest` (6/6) y suite completa (2078/2078).
2. [x] `docs/vision.md` actualizado — párrafo nuevo bajo la cita de V1.1.1, precisando el alcance generalizado (hipótesis científica/conductual, no todo el sistema) y enlazando a este ADR.
3. [x] Resuelto en ADR-005 — PAF como primer consumidor real: `LongitudinalStateService` escribe en `hypothesis_evidence` además de actualizar el streak operacional. Verificado (E2E 6/6, suite completa 2082/2082).
4. [x] Convención de nomenclatura para `hypothesis`/`hypothesis_version` fijada arriba (sección "Convención de nomenclatura"), con registro interino de hipótesis activas. PAF es la única entrada hoy — el siguiente consumidor debe agregar su fila ahí antes de escribir en `hypothesis_evidence`.
