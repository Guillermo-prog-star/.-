# IF-SG-SCHEMA-001 — SafetyGateDecision and ResolutionFailure

Version: 0.1.0
Status: DRAFT
Document Family: IF-SG-SCHEMA
Normative Scope: Integrity Family Safety Gate
Depends on: [IF-SG-SCHEMA-000](IF-SG-SCHEMA-000.md), [IF-SG-CANON-001](IF-SG-CANON-001.md)
Consumed by: IF-SG-SCHEMA-002 (ResolutionSnapshot, not yet drafted), IF-SG-SCHEMA-003 (JustificationGraph, not yet drafted), [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md), IF-SG-EVAL-001, IF-SG-IFACE-001

> Nota de procedencia: como [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md), este documento nunca se ensambló como archivo único en la conversación de origen. Se reconstruye transcribiendo el contenido normativo que sí quedó fijado sin ambigüedad y sin contradicción a lo largo de varias iteraciones: la separación en tres dimensiones (`ContractResolution`, `AuthorizationStatus`, `OperationalDisposition`), la lista de campos obligatorios de `SafetyGateDecision` ([IF-SG-CANON-001 §8.3](IF-SG-CANON-001.md#83-safetygatedecisionhash-material)), la lista de campos de `ResolutionFailure`, la `Decision Matrix` normativa completa, y el `Decision Precedence Model`. Cada tabla y enumeración de este documento tiene una fuente literal identificable en la conversación; no se inventan valores nuevos.

## 1. Purpose

This specification defines `SafetyGateDecision` and `ResolutionFailure` as the two — and only two — possible outcomes of a Safety Gate resolution, and fixes them as a **structurally disjoint union**: no valid `ResolutionResult` may satisfy both shapes at once, and no implementation may represent a technical failure using the vocabulary of a domain decision.

This document does not define how contracts are evaluated ([IF-SG-ARCH-002](README.md), not yet drafted) nor how the justification graph is constructed (IF-SG-SCHEMA-003, not yet drafted). It defines only the shape and semantics of the final resolution output.

## 2. Governing principle: the disjoint union

```
ResolutionResult
=
SafetyGateDecision
OR
ResolutionFailure
```
Never both.

**Rationale**, stated verbatim in the source conversation: *"La imposibilidad técnica de resolver una solicitud no puede representarse como autorización, denegación ni clasificación de riesgo."* A fail-closed operational stop and a domain-level `NOT_AUTHORIZED` are categorically different events, and the type system — not a code comment — must make it impossible to confuse them.

### 2.1 Structural representation

`ResolutionResult` MUST be a discriminated union keyed by `resolutionOutcomeType`, where the discriminant value determines which single nested object is legal:

```json
{
  "resolutionOutcomeType": "DECISION",
  "decision": { "...": "SafetyGateDecision, see §3" }
}
```

```json
{
  "resolutionOutcomeType": "FAILURE",
  "failure": { "...": "ResolutionFailure, see §12" }
}
```

**Constraints**

- The `decision` property MUST be absent when `resolutionOutcomeType = "FAILURE"`.
- The `failure` property MUST be absent when `resolutionOutcomeType = "DECISION"`.
- A conforming JSON Schema for `ResolutionResult` MUST express this via `oneOf` with `additionalProperties: false` on both branches, so that a payload containing both `decision` and `failure` fails schema validation — not just code review.
- No field of `SafetyGateDecision` (`authorizationStatus`, `operationalDisposition`, `decisionHash`, ...) may appear anywhere inside a `ResolutionFailure` object, and vice versa (`failureCategory`, `failureCode`, `retryability`).

## 3. SafetyGateDecision — required fields

Per [IF-SG-CANON-001 §8.3](IF-SG-CANON-001.md#83-safetygatedecisionhash-material), extended with `decisionHash` (computed only after all other fields are finalized, per [IF-SG-CANON-001 §6.4](IF-SG-CANON-001.md#64-safetygatedecision)):

```json
{
  "decisionIdentity": { "...": "InstanceIdentifier, see IF-SG-SCHEMA-000 §3.2" },
  "decisionProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactType": "RESOLUTION_PIPELINE", "artifactVersion": "1.0.0" },
  "resolutionSnapshotReference": { "...": "IntegrityBoundArtifactReference, see IF-SG-SCHEMA-002 (not yet drafted)" },
  "contractResolutionSummary": [ "...": "see §5" ],
  "evidenceResolutionState": "COMPLETE",
  "authorizationStatus": "AUTHORIZED",
  "operationalDisposition": "PROCEED",
  "justificationGraphReference": { "...": "IntegrityBoundArtifactReference, see IF-SG-SCHEMA-003 (not yet drafted)" },
  "revalidationRequirement": { "...": "see §10" },
  "decisionHash": { "...": "ContentHash, see IF-SG-SCHEMA-000 §7.3" }
}
```

**Constraints**

- Every field listed above is mandatory. A `SafetyGateDecision` missing any of them is not conforming — it is not a partial decision, it is an invalid one.
- `decisionIdentity` uses `InstanceIdentifier` ([IF-SG-SCHEMA-000 §3.2](IF-SG-SCHEMA-000.md#32-instanceidentifier)); it identifies this decision instance, not the intervention or the participant.
- `evidenceResolutionState` MUST use one of the four states defined in §6.
- `decisionHash` MUST be computed strictly after `resolutionSnapshotReference`, `contractResolutionSummary`, `evidenceResolutionState`, `authorizationStatus`, `operationalDisposition`, `justificationGraphReference` and `revalidationRequirement` are finalized, per the acyclic hash-construction order in [IF-SG-CANON-001 §6](IF-SG-CANON-001.md#6-normative-hash-construction-order). `decisionHash` MUST NOT be part of the material it hashes.

## 4. Three independent dimensions

A `SafetyGateDecision` is **not** a single flat status. It is stated verbatim in the source conversation that *"SafetyGateDecision no debería ser un único estado plano. Debe contener al menos tres dimensiones independientes."* These three dimensions are separate fields, never collapsed into one enum:

```
A. Contract Resolution     — what happened during evaluation      (§5)
B. Authorization Status    — whether the intervention is cleared  (§6)
C. Operational Disposition — what the system should do next       (§7)
```

Mixing "not authorized" with "what happens now" in a single value was explicitly rejected during design, because it repeatedly produced ambiguous cases (e.g. a temporal contraindication is `NOT_AUTHORIZED` today but must not be treated the same as a structural, permanent block — see §8).

## 5. ContractResolution (per-contract, dimension A)

```
SATISFIED
UNSATISFIED
UNRESOLVED
NOT_APPLICABLE
```

`contractResolutionSummary` is an array of per-stage results, each referencing a `ResolutionStageReference` ([IF-SG-SCHEMA-000 §11.2](IF-SG-SCHEMA-000.md#112-resolutionstagereference)):

```json
{
  "stageReference": { "stageId": "EVIDENCE_RESOLUTION", "pipelineProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactVersion": "1.0.0" } },
  "resolutionStatus": "SATISFIED",
  "reasonCodes": []
}
```

**Constraint:** the order of entries in `contractResolutionSummary` is normative (it follows the Decision Precedence Model, §8) and MUST NOT be reordered by an implementation — this is the same rule as Normative Array Ordering in [IF-SG-CANON-001 §3.4](IF-SG-CANON-001.md#34-arrays) and is the exact condition tested by NEG-009.

## 6. EvidenceResolutionState (dimension A, evidence-specific)

```
COMPLETE
INCOMPLETE
INCONSISTENT
INDETERMINATE
```

**Semantics**

- `COMPLETE` — required evidence exists, meets quality and freshness thresholds, and is coherent across sources.
- `INCOMPLETE` — required evidence is absent, or present but expired beyond its `FreshnessContract` (freshness failures are represented as `INCOMPLETE` with a `ReasonCode` identifying staleness — see [IF-SG-SCHEMA-000 §11.1](IF-SG-SCHEMA-000.md#111-reasoncode)). A dedicated `STALE` top-level state was explicitly considered and deferred: *"posiblemente añadir STALE, INVALID_PROVENANCE pero como subestados o razones, no necesariamente como estados principales"*.
- `INCONSISTENT` — evidence exists and is fresh, but two or more sources violate a declared `CoherenceRule` (e.g. narrative "no conflict" + affective "high distress"). Both sources MUST be preserved, never silently discarded.
- `INDETERMINATE` — evidence cannot be resolved to a definite value (e.g. "I don't know", or a source that is unavailable without being a technical failure). MUST NOT be collapsed into `INCONSISTENT` — there is no contradiction, only absence of determination.

**Invariant:** `UNKNOWN` MUST NOT be converted into `FALSE`; `INDETERMINATE` MUST NOT be converted into `UNSATISFIED` (restated from [IF-SG-SCHEMA-000 §9.4](IF-SG-SCHEMA-000.md#94-indeterminatevalue)).

## 7. AuthorizationStatus (dimension B)

```
AUTHORIZED
NOT_AUTHORIZED
PENDING_RESOLUTION
```

**Invariant — no `CONDITIONALLY_AUTHORIZED`.** A fourth value was proposed and explicitly rejected during design: *"Una autorización condicionada suele significar que aún existe una condición no satisfecha. En ese caso, arquitectónicamente debería permanecer: PENDING_RESOLUTION hasta que la condición sea resuelta."* An implementation MUST NOT introduce a conditional-authorization value; unresolved conditions belong in `PENDING_RESOLUTION` with the outstanding condition recorded via `contractResolutionSummary` and `revalidationRequirement`.

## 8. OperationalDisposition (dimension C)

```
PROCEED
COLLECT_ADDITIONAL_EVIDENCE
REASSESS
WAIT_AND_REASSESS
RESOLVE_ALTERNATIVE_CAPABILITY
ESCALATE
STOP
```

**Semantics**

- `PROCEED` — the intervention may execute now.
- `COLLECT_ADDITIONAL_EVIDENCE` — evidence is absent or stale; nothing about the request is denied, more input is needed.
- `REASSESS` — evidence exists but is internally inconsistent; a human or upstream process must reconcile it before resolution can continue.
- `WAIT_AND_REASSESS` — evidence is indeterminate, or a contraindication is temporal (self-resolving with time); retry later without new input.
- `RESOLVE_ALTERNATIVE_CAPABILITY` — a contextual contraindication blocks the requested intervention specifically, but the required `Capability` (see [IF-SG-TAX-001](README.md), not yet drafted) may be satisfiable by a different candidate intervention.
- `ESCALATE` — resolution requires a decision outside the Gate's authority (e.g. human judgment, safety protocol per V97/V98 in the main project).
- `STOP` — the intervention must not proceed and there is no self-resolving path within this resolution cycle (structural contraindication, invalid lifecycle, invalid purpose binding).

`RESOLVE_ALTERNATIVE_CAPABILITY` and `ESCALATE` were left as two candidate outcomes for the same precondition (contextual contraindication) in the source material, with the explicit note that policy determines which applies. This document does not resolve that choice generically — it is resolved per-fixture in [IF-SG-CONF-DATA-001 CONF-007](IF-SG-CONF-DATA-001.md#conf-007--contraindicación-contextual).

## 9. Decision Precedence Model

Ante señales simultáneas o contradictorias de varios contratos, la precedencia normativa es:

```
Technical Validity
    ↓
Lifecycle Validity
    ↓
Purpose Binding
    ↓
Structural Contraindications
    ↓
Evidence Resolution
    ↓
Contextual and Temporal Contraindications
    ↓
Applicability
    ↓
Capability Compatibility
    ↓
Authorization Policy
    ↓
Operational Disposition
```

**Invariant — monotonicity of structural block.** *"Capability Match = satisfied, Structural Contraindication = present → Resultado: NOT_AUTHORIZED. La coincidencia de capacidad nunca puede prevalecer sobre una contraindicación estructural."* No downstream stage (capability match, complete evidence, satisfied applicability) may override a `STOP` produced upstream (structural contraindication, invalid lifecycle, invalid purpose binding). This is exactly the property tested by [CONF-008](IF-SG-CONF-DATA-001.md#conf-008--contraindicación-estructural) and [CONF-010](IF-SG-CONF-DATA-001.md#conf-010--lifecycle-inválido).

## 10. Decision Matrix (normative)

| Condición dominante | AuthorizationStatus | OperationalDisposition | Fixture |
|---|---|---|---|
| Evidencia completa, contratos satisfechos, sin contraindicación | `AUTHORIZED` | `PROCEED` | CONF-001 |
| Evidencia incompleta | `PENDING_RESOLUTION` | `COLLECT_ADDITIONAL_EVIDENCE` | CONF-002 |
| Evidencia inconsistente | `PENDING_RESOLUTION` | `REASSESS` | CONF-003 |
| Evidencia indeterminada | `PENDING_RESOLUTION` | `WAIT_AND_REASSESS` | CONF-004 |
| Evidencia vencida | `PENDING_RESOLUTION` | `COLLECT_ADDITIONAL_EVIDENCE` | CONF-005 |
| Contraindicación temporal | `NOT_AUTHORIZED` | `WAIT_AND_REASSESS` | CONF-006 |
| Contraindicación contextual | `NOT_AUTHORIZED` | `RESOLVE_ALTERNATIVE_CAPABILITY` (o `ESCALATE`, según política) | CONF-007 |
| Contraindicación estructural | `NOT_AUTHORIZED` | `STOP` | CONF-008 |
| Capability match sin evaluación completa | `PENDING_RESOLUTION` | — (candidatura, no autorización) | CONF-009 |
| Lifecycle inválido | `NOT_AUTHORIZED` | `STOP` | CONF-010 |
| Purpose binding inválido (verificado) | `NOT_AUTHORIZED` | `STOP` | CONF-011 |
| Fallo técnico | *sin decisión de dominio — `ResolutionFailure`* | *retryability, no disposition* | CONF-012 |

Esta tabla es la fuente única de verdad para el mapeo condición → resultado; los doce fixtures de [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md) son instancias concretas de cada fila.

## 11. RevalidationRequirement

```json
{
  "revalidationRequired": false,
  "validityWindow": { "validFrom": "2026-07-19T19:00:00.000Z", "validUntil": "2026-07-20T19:00:00.000Z" },
  "revalidationTriggerReferences": []
}
```

**Semantics.** A decision is not valid indefinitely. Recognized triggers, per the source material: nueva evidencia relevante, cambio de `ContextEpisode`, aparición de contraindicación, expiración temporal, cambio de política, cambio de estado de la intervención.

**Invariant — historical immutability, not rewrite.** When new evidence at `T2` would change the outcome of a decision already emitted at `T3` (based on a snapshot captured at `T0`), the system MUST preserve the original decision unchanged and set `revalidationRequired: true`. It MUST NOT retroactively rewrite `authorizationStatus` or `operationalDisposition` on the original decision. This is exactly [SYS-003](IF-SG-CONF-DATA-001.md#sys-003--cambio-contextual-concurrente).

## 12. ResolutionFailure — required fields

```json
{
  "failureCategory": "ARTIFACT_UNAVAILABLE",
  "failureCode": "POLICY_ARTIFACT_UNAVAILABLE",
  "failedStage": { "stageId": "POLICY_RESOLUTION", "pipelineProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactVersion": "1.0.0" } },
  "technicalArtifactReferences": [],
  "retryability": { "status": "CONDITIONALLY_RETRYABLE", "conditionReferences": [ { "namespace": "IF-SG-RETRY", "code": "POLICY_STORE_RESTORED", "version": "1.0.0" } ] },
  "failureTimestamp": "2026-07-19T19:00:00.000Z",
  "correlationReference": { "...": "InstanceIdentifier, operational — excluded from any normative hash per IF-SG-CANON-001 §9.1" }
}
```

**Constraints**

- Every field listed above is mandatory.
- `retryability` MUST use `NormativeRetryability` ([IF-SG-SCHEMA-000 §12.1](IF-SG-SCHEMA-000.md#121-normativeretryability)), never a bare boolean.
- `failureTimestamp` uses `NormativeTimestamp` ([IF-SG-SCHEMA-000 §6.1](IF-SG-SCHEMA-000.md#61-normativetimestamp)).
- `correlationReference` is operational (transport-level correlation) and MUST be excluded from any content hash computed over this artifact, consistent with [IF-SG-CANON-001 §9.1](IF-SG-CANON-001.md#91-transport-metadata).
- `ResolutionFailure` MUST NOT declare `authorizationStatus`, `operationalDisposition` or `decisionHash`. It has no domain-level opinion to express.

## 13. FailureCategory

```
NORMATIVE_ASSEMBLY_FAILURE     — e.g. cyclic hash dependency, cyclic justification graph (IF-SG-CANON-001 §7, §11)
ARTIFACT_UNAVAILABLE           — a required artifact (policy, catalog, taxonomy) could not be retrieved
CANONICALIZATION_FAILURE       — input could not be reduced to canonical form (IF-SG-CANON-001 §16)
INTEGRITY_VERIFICATION_FAILURE — a declared hash did not match, or could not be verified (IntegrityStatus MISMATCH / NOT_VERIFIABLE, IF-SG-SCHEMA-000 §7.4)
PURPOSE_VERIFICATION_FAILURE   — purpose binding could not be verified due to a technical condition (distinct from a verified mismatch, which is a domain decision — see §14)
```

Every `failureCode` under `CANONICALIZATION_FAILURE` corresponds one-to-one with the failure conditions enumerated in [IF-SG-CANON-001 §16](IF-SG-CANON-001.md#16-conformance-failure-semantics): `INVALID_NORMATIVE_REPRESENTATION`, `UNSUPPORTED_CANONICALIZATION_PROFILE`, `UNSUPPORTED_HASH_PROFILE`, `HASH_MISMATCH`, `CYCLIC_HASH_DEPENDENCY`, `CYCLIC_JUSTIFICATION_GRAPH`, `NON_CANONICAL_DECIMAL`, `INVALID_NORMATIVE_TIMESTAMP`, `AMBIGUOUS_NULL_SEMANTICS`.

## 14. The purpose-binding boundary (decision vs. failure)

This is the sharpest edge in the whole model, and it was corrected explicitly during design after an earlier draft conflated it:

```
Purpose binding checked and found to mismatch  →  SafetyGateDecision: NOT_AUTHORIZED, STOP   (CONF-011)
Purpose binding could not be checked (technical)→  ResolutionFailure: PURPOSE_VERIFICATION_FAILURE (CONF-012 pattern)
```

**Invariant:** *"Verified mismatch = domain decision. Unable to verify = resolution failure."* An implementation MUST distinguish "I checked and it doesn't match" from "I couldn't check." Collapsing the two into a single `NOT_AUTHORIZED` silently converts a technical inability into a false domain conclusion — the exact failure mode this whole schema exists to prevent.

## 15. Worked examples

### 15.1 CONF-001 (AUTHORIZED / PROCEED)

```json
{
  "resolutionOutcomeType": "DECISION",
  "decision": {
    "decisionIdentity": "550e8400-e29b-41d4-a716-446655440000",
    "decisionProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactType": "RESOLUTION_PIPELINE", "artifactVersion": "1.0.0" },
    "resolutionSnapshotReference": { "artifactId": "if-sg:snapshot:...", "artifactType": "RESOLUTION_SNAPSHOT", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
    "contractResolutionSummary": [
      { "stageReference": { "stageId": "LIFECYCLE_VALIDATION" }, "resolutionStatus": "SATISFIED", "reasonCodes": [] },
      { "stageReference": { "stageId": "EVIDENCE_RESOLUTION" }, "resolutionStatus": "SATISFIED", "reasonCodes": [] },
      { "stageReference": { "stageId": "CONTRAINDICATION_RESOLUTION" }, "resolutionStatus": "NOT_APPLICABLE", "reasonCodes": [] },
      { "stageReference": { "stageId": "CAPABILITY_RESOLUTION" }, "resolutionStatus": "SATISFIED", "reasonCodes": [] }
    ],
    "evidenceResolutionState": "COMPLETE",
    "authorizationStatus": "AUTHORIZED",
    "operationalDisposition": "PROCEED",
    "justificationGraphReference": { "artifactId": "if-sg:graph:...", "artifactType": "JUSTIFICATION_GRAPH", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
    "revalidationRequirement": { "revalidationRequired": false, "validityWindow": { "validFrom": "2026-07-19T19:00:00.000Z", "validUntil": "2026-07-19T19:15:00.000Z" }, "revalidationTriggerReferences": [] },
    "decisionHash": { "...": "ContentHash" }
  }
}
```

### 15.2 CONF-008 (NOT_AUTHORIZED / STOP, structural block despite capability match)

```json
{
  "resolutionOutcomeType": "DECISION",
  "decision": {
    "contractResolutionSummary": [
      { "stageReference": { "stageId": "CONTRAINDICATION_RESOLUTION" }, "resolutionStatus": "UNSATISFIED", "reasonCodes": [ { "namespace": "IF-SG-CONTRAINDICATION", "code": "STRUCTURAL_IMMEDIATE_PHYSICAL_THREAT", "version": "1.0.0" } ] },
      { "stageReference": { "stageId": "CAPABILITY_RESOLUTION" }, "resolutionStatus": "SATISFIED", "reasonCodes": [] }
    ],
    "evidenceResolutionState": "COMPLETE",
    "authorizationStatus": "NOT_AUTHORIZED",
    "operationalDisposition": "STOP"
  }
}
```

Nótese: `CAPABILITY_RESOLUTION: SATISFIED` no cambia el resultado — es exactamente la prueba de monotonicidad de §9.

### 15.3 CONF-012 (ResolutionFailure)

```json
{
  "resolutionOutcomeType": "FAILURE",
  "failure": {
    "failureCategory": "ARTIFACT_UNAVAILABLE",
    "failureCode": "POLICY_ARTIFACT_UNAVAILABLE",
    "failedStage": { "stageId": "POLICY_RESOLUTION" },
    "technicalArtifactReferences": [],
    "retryability": { "status": "CONDITIONALLY_RETRYABLE", "conditionReferences": [ { "namespace": "IF-SG-RETRY", "code": "POLICY_STORE_RESTORED", "version": "1.0.0" } ] },
    "failureTimestamp": "2026-07-19T19:00:00.000Z",
    "correlationReference": "6b1a...operational-uuid"
  }
}
```

No `authorizationStatus` field exists anywhere in this payload — that absence is itself the assertion under test (constraint assertion, per [IF-SG-CONF-DATA-001 §2](IF-SG-CONF-DATA-001.md#2-formato-de-paquete-de-fixture)).

## 16. Forbidden representations

- A `ResolutionResult` containing both `decision` and `failure`.
- `CONDITIONALLY_AUTHORIZED` as a value of `authorizationStatus`.
- `authorizationStatus` or `operationalDisposition` present inside a `ResolutionFailure`.
- `failureCategory` or `retryability` present inside a `SafetyGateDecision`.
- `INDETERMINATE` evidence represented as `INCONSISTENT`, or vice versa.
- A `STOP` disposition overridden by a later-evaluated `SATISFIED` capability or applicability result.
- A verified purpose-binding mismatch represented as `ResolutionFailure` (it is a domain decision), or an unverifiable purpose binding represented as `SafetyGateDecision: NOT_AUTHORIZED` (it is a technical failure).
- `decisionHash` computed before `justificationGraphReference` or `revalidationRequirement` are finalized.
- `contractResolutionSummary` reordered by an implementation instead of following the Decision Precedence Model.
- A rewritten `authorizationStatus`/`operationalDisposition` on a previously emitted decision instead of a new `revalidationRequired: true` marker (see §11).

## 17. Exit criteria

This specification is eligible to advance from DRAFT to TESTING when:

- a formal JSON Schema exists that mechanically rejects a `ResolutionResult` containing both `decision` and `failure`;
- all twelve fixtures in [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md) validate against this schema, producing exactly the `authorizationStatus`/`operationalDisposition`/`ResolutionFailure` combination fixed in the Decision Matrix (§10);
- CONF-011 and CONF-012 are demonstrated as distinguishable by at least one reference implementation (§14);
- CONF-008 and CONF-010 demonstrate that a `STOP` from an upstream stage is never overridden by a downstream `SATISFIED` result;
- SYS-001 (determinism) and SYS-003 (revalidation without rewrite) pass against two independent evaluator runs.

## 18. Governing invariant

> Un `SafetyGateDecision` describe qué se resolvió y qué debe hacer el sistema a continuación. Un `ResolutionFailure` describe que no fue posible resolverlo. Ningún campo, valor por defecto ni conversión implícita puede hacer que uno se lea como el otro.
