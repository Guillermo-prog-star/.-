# IF-SG-CONF-DATA-001 — Canonical Conformance Fixtures

Version: 0.1.0
Status: DRAFT
Document Family: IF-SG-CONF-DATA
Normative Scope: Integrity Family Safety Gate
Depends on: [IF-SG-SCHEMA-000](IF-SG-SCHEMA-000.md), [IF-SG-CANON-001](IF-SG-CANON-001.md)
Consumed by: IF-SG-EVAL-001 (Reference Evaluator, not yet drafted)

> Nota de procedencia: este documento nunca se llegó a redactar en la conversación de origen — era el siguiente paso declarado del roadmap. Su contenido se construye transcribiendo, en el formato de paquete de fixture que la propia conversación acordó (`manifest` + artefactos de entrada + resultado esperado + constraints del grafo + resultados prohibidos), los doce casos canónicos y las cuatro propiedades sistémicas que sí quedaron especificados sin ambigüedad en prosa. No se inventa ninguna condición, resultado obligatorio ni resultado prohibido: todos provienen literalmente de esa especificación.
>
> **Limitación deliberada de esta versión:** IF-SG-SCHEMA-001/002/003 (`SafetyGateDecision`, `ResolutionSnapshot`, `JustificationGraph`) todavía no existen como esquemas formales. Por tanto, estos fixtures se expresan al nivel de **caso normativo** — condición de entrada, resultado obligatorio, resultados prohibidos, requisitos mínimos del grafo de justificación — y no como JSON literal validable contra un esquema, porque ese esquema aún no está definido. Cuando SCHEMA-001/002/003 se redacten, cada fixture de este documento debe poder expresarse como instancias JSON concretas sin cambiar su semántica.

## 1. Propósito

Fijar, en forma de fixtures reproducibles, el comportamiento observable que **toda** implementación conforme del Safety Gate debe producir. Los esquemas (SCHEMA-001+) serán la materialización estructural de este comportamiento — no al revés.

Cada fixture es una `ConformanceCase`:

```
ConformanceCase
│
├── caseId
├── normativePurpose
├── preconditions
├── requiredResult
├── forbiddenOutcomes
├── justificationGraphRequirements
└── reproducibilityRequirement
```

## 2. Formato de paquete de fixture

Cuando se materialicen como archivos ejecutables (post SCHEMA-001/002/003), cada fixture debe seguir esta estructura, ya acordada en la conversación de origen:

```
CONF-0NN/
├── manifest.json
├── intervention-definition.json
├── capability-taxonomy.json
├── policy-artifact.json
├── context-projection.json
├── evidence-snapshot.json
├── expected-result.json
├── graph-constraints.json
└── forbidden-outcomes.json
```

`manifest.json` mínimo:

```json
{
  "fixtureId": "if-sg:conf-data:conf-001",
  "fixtureVersion": "0.1.0",
  "conformanceCaseReference": "CONF-001",
  "canonicalizationProfileReference": { "artifactId": "IF-SG-CANON-001", "artifactVersion": "0.1.1" },
  "hashProfileReference": { "artifactId": "IF-SG-HASH-SHA256-V1", "artifactVersion": "1.0.0" },
  "inputArtifactReferences": [],
  "expectedResultReference": "expected-result.json",
  "forbiddenOutcomeReferences": "forbidden-outcomes.json"
}
```

Dos clases de aserción, ambas ya definidas en la conversación de origen:

- **Exact assertions** — valores que deben ser idénticos: `authorizationStatus`, `operationalDisposition`, `evidenceResolutionState`, `decisionHash`.
- **Constraint assertions** — restricciones estructurales donde pueden existir identificadores operativos distintos: *el grafo contiene un nodo de lifecycle*, *el grafo no tiene ciclos*, *todos los nodos de evidencia preservan procedencia*, *no existe campo `authorizationStatus` en un resultado de fallo*.

## 3. Los doce casos canónicos

### CONF-001 — Resolución satisfactoria

**Normative purpose:** confirmar el camino feliz completo: todos los contratos satisfechos producen autorización y un grafo de justificación completo.

**Preconditions**

- Lifecycle: `valid`
- Purpose Binding: `valid`
- Applicability: `satisfied`
- Evidence: `complete`
- Coherence: `satisfied`
- Contraindications: `absent`
- Capability: `matched`
- Policy: `permits`

**Required result**

```
authorizationStatus: AUTHORIZED
operationalDisposition: PROCEED
```

**Forbidden outcomes:** `ResolutionFailure` (ninguna condición técnica impide resolver).

**JustificationGraph requirements:** debe contener nodos para las nueve etapas evaluadas (lifecycle, purpose binding, applicability, evidence — existence/quality/coherence/freshness —, contraindication, capability, policy, disposition) todos en estado `SATISFIED`, sin nodos `UNRESOLVED`.

---

### CONF-002 — Evidencia incompleta

**Normative purpose:** la ausencia de evidencia requerida nunca debe convertirse en denegación definitiva.

**Preconditions:** `BodyStateSnapshot` requerido está ausente.

**Required result**

```
evidenceResolutionState: INCOMPLETE
authorizationStatus: PENDING_RESOLUTION
operationalDisposition: COLLECT_ADDITIONAL_EVIDENCE
```

**Forbidden outcomes:** `AUTHORIZED`.

---

### CONF-003 — Evidencia inconsistente

**Normative purpose:** verificar que el Gate detecta incoherencia entre fuentes de evidencia y no descarta silenciosamente ninguna de ellas.

**Preconditions:** evidencia narrativa y evidencia afectiva violan una `CoherenceRule` declarada (ej. narrativa "sin conflicto" + respuesta afectiva "estrés alto").

**Required result**

```
evidenceResolutionState: INCONSISTENT
authorizationStatus: PENDING_RESOLUTION
operationalDisposition: REASSESS
```

**Forbidden outcomes:** cualquier resultado que descarte una de las dos fuentes de evidencia en vez de preservar ambas.

---

### CONF-004 — Evidencia indeterminada

**Normative purpose:** distinguir indeterminación (no se sabe) de inconsistencia (se sabe, pero se contradice).

**Preconditions:** respuesta del participante = "no sé"; medida derivada del sistema no disponible.

**Required result**

```
evidenceResolutionState: INDETERMINATE
authorizationStatus: PENDING_RESOLUTION
operationalDisposition: WAIT_AND_REASSESS
```

**Forbidden outcomes:** clasificar este caso como `INCONSISTENT` (no hay contradicción, hay ausencia de determinación).

---

### CONF-005 — Evidencia vencida

**Normative purpose:** una evidencia históricamente válida pero fuera del `FreshnessContract` no debe autorizar.

**Preconditions:** antigüedad de la evidencia excede el contrato de vigencia declarado por la intervención.

**Required result**

```
authorizationStatus: PENDING_RESOLUTION
operationalDisposition: COLLECT_ADDITIONAL_EVIDENCE
```

**Forbidden outcomes:** `AUTHORIZED` usando evidencia vencida.

---

### CONF-006 — Contraindicación temporal

**Normative purpose:** una contraindicación temporal es reversible con el tiempo — nunca debe tratarse como bloqueo permanente.

**Preconditions:** `TemporalContraindication` presente.

**Required result**

```
authorizationStatus: NOT_AUTHORIZED
operationalDisposition: WAIT_AND_REASSESS
```

**Forbidden outcomes:** `STOP` (la intervención puede volverse elegible más adelante; `STOP` implicaría cierre definitivo).

---

### CONF-007 — Contraindicación contextual

**Normative purpose:** una contraindicación contextual (ej. coerción activa) bloquea la intervención solicitada, pero el sistema debe ofrecer una vía normativa concreta, no ambigua.

**Preconditions:** `ContextualContraindication: ACTIVE_COERCION`.

**Required result**

```
authorizationStatus: NOT_AUTHORIZED
operationalDisposition: RESOLVE_ALTERNATIVE_CAPABILITY
```

*Nota normativa:* la especificación de origen dejó dos disposiciones candidatas (`RESOLVE_ALTERNATIVE_CAPABILITY` o `ESCALATE`) "según política versionada", pero exigió fijar una sola salida concreta por fixture para evitar ambigüedad. Este fixture fija `RESOLVE_ALTERNATIVE_CAPABILITY` como caso base; un fixture hermano `CONF-007b` con política que exige escalamiento humano puede añadirse cuando exista un `PolicyArtifact` real que lo declare.

**Forbidden outcomes:** `AUTHORIZED`, `PENDING_RESOLUTION`.

---

### CONF-008 — Contraindicación estructural

**Normative purpose:** ninguna combinación de evidencia completa o coincidencia de capacidad puede prevalecer sobre una contraindicación estructural.

**Preconditions:** `StructuralContraindication: IMMEDIATE_PHYSICAL_THREAT` (aun con evidencia completa y capability match).

**Required result**

```
authorizationStatus: NOT_AUTHORIZED
operationalDisposition: STOP
```

**Forbidden outcomes:** `AUTHORIZED`, `PENDING_RESOLUTION` — bajo ninguna circunstancia. Este es el vector que prueba la **monotonicidad de bloqueo estructural** (§7 del comportamiento sistémico, ver sección 4).

---

### CONF-009 — Capability match sin autorización

**Normative purpose:** que una intervención candidata declare la capacidad requerida genera candidatura, nunca autorización automática.

**Preconditions:** capacidad requerida `ACTIVATION_REDUCTION`; intervención candidata declara esa capacidad; `EvidenceContract` aún no resuelto.

**Required result**

```
candidateMatch: true
authorizationStatus: PENDING_RESOLUTION
```

**Forbidden outcomes:** `AUTHORIZED` únicamente por coincidencia de capacidad.

---

### CONF-010 — Lifecycle inválido

**Normative purpose:** un lifecycle inválido bloquea aunque todos los demás contratos estén satisfechos.

**Preconditions:** `intervention.status = RETIRED`; applicability, evidence, contraindications y capability todos satisfechos.

**Required result**

```
authorizationStatus: NOT_AUTHORIZED
operationalDisposition: STOP
```

**Forbidden outcomes:** `AUTHORIZED`. Prueba de precedencia: `Lifecycle Validity` está por encima de `Applicability`/`Evidence`/`Capability` en el `Decision Precedence Model`.

---

### CONF-011 — Purpose Binding inválido (mismatch verificado)

**Normative purpose:** una proyección contextual generada para un propósito no puede reutilizarse para evaluar otro propósito distinto — la reutilización debe quedar registrada, no autorizada.

**Preconditions:** `SafetyRelevantContextProjection` generada para propósito A; resolución solicitada para propósito B; el mismatch es **verificable** (no un fallo técnico — se comparó y se confirmó que difieren).

**Required result**

```
authorizationStatus: NOT_AUTHORIZED
operationalDisposition: STOP
```

**Forbidden outcomes:** `AUTHORIZED`, `PENDING_RESOLUTION`.

*Distinción obligatoria con CONF-012:* si el propósito **no puede verificarse** por una condición técnica (por ejemplo, el artefacto de purpose binding es ilegible), el resultado correcto no es este — es `ResolutionFailure`, como en CONF-012. `Verified mismatch = decisión de dominio`; `unable to verify = fallo técnico`.

---

### CONF-012 — Fallo técnico

**Normative purpose:** un fallo técnico nunca debe representarse como una decisión de dominio. Es el vector más estricto de toda la suite.

**Preconditions:** `PolicyArtifact` no disponible (Policy Store caído).

**Required result:** `ResolutionFailure`

```
failureCategory: NORMATIVE_ASSEMBLY_FAILURE  # o categoría equivalente de disponibilidad de artefacto
failureCode: POLICY_ARTIFACT_UNAVAILABLE
failedStage: POLICY_RESOLUTION
retryability: CONDITIONALLY_RETRYABLE
```

**Forbidden outcomes:** `AUTHORIZED`, `NOT_AUTHORIZED`, `PENDING_RESOLUTION` — ninguno de los tres es válido. El sistema no negó la intervención: no pudo resolverla.

> **Fail-closed operacional ≠ denegación normativa.** El sistema detiene la ejecución (operacionalmente), pero eso nunca debe registrarse como "el Gate decidió que no" (normativamente). Esta distinción es la corrección más importante que la revisión final introdujo sobre el borrador inicial de CONF-001.

## 4. Las cuatro pruebas de propiedades sistémicas

Estas no son casos de entrada/salida puntuales — son invariantes que la suite completa debe demostrar sobre el comportamiento del evaluador.

### SYS-001 — Determinismo

**Enunciado:** ante el mismo `ResolutionSnapshot` (mismos artefactos canónicos de entrada) y la misma versión del evaluador, dos ejecuciones independientes deben producir:

- la misma decisión (`authorizationStatus`, `operationalDisposition`);
- el mismo `JustificationGraph`;
- el mismo `DecisionHash`.

**Constraint:** los timestamps puramente operativos pueden variar entre ejecuciones únicamente si están fuera del material normativo de hash (ver [IF-SG-CANON-001 §9](IF-SG-CANON-001.md#9-structural-exclusion-list)).

**Forbidden:** cualquier divergencia de `decisionHash` entre dos ejecuciones con el mismo `ResolutionSnapshot` y perfil de resolución.

### SYS-002 — Idempotencia

**Enunciado:** reprocesar una solicitud ya resuelta no debe crear dos decisiones lógicamente distintas.

**Required behavior:** el sistema debe devolver la resolución existente, o producir una resolución equivalente vinculada al mismo `ResolutionSnapshot`.

**Forbidden:** generar una segunda `SafetyGateDecision` divergente para el mismo snapshot sin una razón normativa explícita (p. ej. cambio de política).

### SYS-003 — Cambio contextual concurrente

**Enunciado:** una decisión basada en un snapshot válido en `T0` puede quedar obsoleta si en `T2` aparece evidencia nueva o una contraindicación, aunque la decisión ya se haya emitido en `T3`.

**Required behavior:**

```
preserve original decision (bound to T0 snapshot)
+
mark revalidationRequired: true
```

**Forbidden:** reescribir retroactivamente la decisión original emitida en `T3` para reflejar el estado de `T2`. La decisión histórica es inmutable; lo que cambia es que queda marcada como necesitada de revalidación.

### SYS-004 — Discordancia de resultados preservada

**Enunciado:** cuando dos fuentes de observación reportan resultados distintos sobre el mismo fenómeno (ej. `participantSelfReport: NO_CHANGE` vs. `systemDerivedMeasure: LOWER_ACTIVATION`), el sistema debe preservar ambas sin resolver una "verdad" única.

**Required result:**

```
discordanceStatus: PRESERVED
```
con ambas observaciones conservando `sourceType`, método de adquisición y ventana temporal.

**Forbidden:**

- promediar ambas observaciones;
- seleccionar una fuente como verdadera y descartar la otra;
- tratar la medida del sistema como árbitro de mayor autoridad epistémica que el autorreporte;
- alterar una fuente para hacerla coherente con la otra.

## 5. Cobertura cruzada con IF-SG-CANON-001

Los siguientes vectores negativos de canonicalización ([IF-SG-CANON-001 §13](IF-SG-CANON-001.md#13-negative-conformance-vectors)) son prerrequisito de fidelidad para que estos doce fixtures sean reproducibles bit a bit entre implementaciones: **NEG-001** (timestamp operativo excluido), **NEG-003** (decimal de punto flotante prohibido en `EvidenceResolutionState`/umbrales), **NEG-005** y **NEG-014** (orden acíclico de hashes y grafo sin ciclos — relevante para CONF-001, cuyo `JustificationGraph` debe poder hashearse). Un evaluador que pase los doce `CONF-0NN` pero falle los vectores negativos de CANON-001 no es conforme.

## 6. Qué falta para que estos fixtures sean ejecutables

1. **IF-SG-SCHEMA-001** (`SafetyGateDecision` / `ResolutionFailure`, disjuntos) — para expresar `expected-result.json` como JSON validable.
2. **IF-SG-SCHEMA-002** (`ResolutionSnapshot`) — para expresar artefactos de entrada reales en vez de condiciones en prosa.
3. **IF-SG-SCHEMA-003** (`JustificationGraph`) — para expresar `graph-constraints.json` como aserciones sobre nodos/aristas reales.
4. Un `InterventionDefinition` de referencia concreto (el catálogo IF-SG-ARCH-002 nunca se congeló) contra el cual instanciar CONF-001, CONF-002, CONF-009 y CONF-010.

Sin (1)-(3), este documento es la especificación normativa completa pero no el paquete de archivos JSON ejecutable descrito en la sección 2. Ese es el hueco real entre "sabemos exactamente qué debe pasar" y "podemos correr esto en CI".

## 7. Exit criteria

Estos fixtures son elegibles para pasar de DRAFT a TESTING cuando:

- IF-SG-SCHEMA-001/002/003 existan y cada `expected-result.json` valide contra ellos;
- exista al menos un `InterventionDefinition` de referencia (`IF-SG-ARCH-002` con al menos las intervenciones `OBSERVATIONAL_PAUSE` y `COGNITIVE_DEFUSION_STANDARD` congeladas);
- un evaluador de referencia (`IF-SG-EVAL-001`) pase los doce casos y las cuatro pruebas sistémicas sin excepción;
- un segundo evaluador independiente reproduzca los mismos `decisionHash` para CONF-001 (prueba cruzada de SYS-001).

## 8. Invariante rector

> Un fixture no describe cómo se implementó el Gate. Describe qué debe ser verdadero de su comportamiento observable — decisión, disposición, grafo, hash — sin importar en qué lenguaje o motor se ejecutó.
