# IF-SG-SCHEMA-002 — ResolutionSnapshot

Version: 0.1.0
Status: DRAFT
Document Family: IF-SG-SCHEMA
Normative Scope: Integrity Family Safety Gate
Depends on: [IF-SG-SCHEMA-000](IF-SG-SCHEMA-000.md), [IF-SG-CANON-001](IF-SG-CANON-001.md), [IF-SG-SCHEMA-001](IF-SG-SCHEMA-001.md)
Consumed by: IF-SG-SCHEMA-003 (JustificationGraph, not yet drafted), [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md), IF-SG-EVAL-001, IF-SG-IFACE-001

> Nota de procedencia: como SCHEMA-001, este documento nunca se ensambló como archivo único en la conversación de origen. Se reconstruye a partir de dos enumeraciones de campos consistentes entre sí — el boceto inicial de `ResolutionSnapshot` y la lista de material positivo de hash en [IF-SG-CANON-001 §8.1](IF-SG-CANON-001.md#81-resolutionsnapshothash-material), que ya se transcribió literalmente en ese documento — más las reglas de inmutabilidad, relación uno-a-muchos con las decisiones, y el escenario de concurrencia T0–T3, todos citados textualmente donde corresponde.

## 1. Purpose

`ResolutionSnapshot` is the frozen, integrity-bound record of every input artifact a `SafetyGateDecision` was computed from. It exists so that a decision can be reconstructed and re-verified later without re-querying live systems (a policy store, a catalog, a context service) whose state may have already changed.

This document does not define the internal structure of the artifacts it references (`InterventionDefinition`, `SafetyRelevantContextProjection`, `PolicyArtifact`, ...). It defines only how those artifacts are frozen together into one hashable, immutable unit.

## 2. Why a separate schema from SafetyGateDecision

Stated verbatim in the source conversation: *"No uniría SafetyGateDecision y ResolutionSnapshot en un único esquema principal. Son conceptos relacionados, pero distintos."*

```
SafetyGateDecision
Representa la conclusión normativa.

ResolutionSnapshot
Representa los insumos congelados usados para resolver.
```

*"La decisión referencia el snapshot, pero no debería duplicarlo completamente."* This is why [IF-SG-SCHEMA-001 §3](IF-SG-SCHEMA-001.md#3-safetygatedecision--required-fields) carries only a `resolutionSnapshotReference` (an `IntegrityBoundArtifactReference`), never a copy of the snapshot's contents.

## 3. Required fields

```json
{
  "snapshotIdentity": "6b1a7e10-...-instance-identifier",
  "interventionDefinitionReference": { "...": "IntegrityBoundArtifactReference" },
  "capabilityTaxonomyReference": { "...": "IntegrityBoundArtifactReference" },
  "policyArtifactReference": { "...": "IntegrityBoundArtifactReference" },
  "contextProjectionReference": { "...": "IntegrityBoundArtifactReference" },
  "evidenceSnapshotReference": { "...": "IntegrityBoundArtifactReference" },
  "purposeBindingReference": { "...": "PurposeBindingReference, see IF-SG-SCHEMA-000 §10.2" },
  "resolutionProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactType": "RESOLUTION_PIPELINE", "artifactVersion": "1.0.0" },
  "canonicalizationProfileReference": { "artifactId": "IF-SG-CANON-001", "artifactVersion": "0.1.1" },
  "hashProfileReference": { "artifactId": "IF-SG-HASH-SHA256-V1", "artifactVersion": "1.0.0" },
  "evaluatorConformanceReference": { "artifactId": "IF-SG-EVAL-001", "artifactType": "EVALUATOR_CONFORMANCE_PROFILE", "artifactVersion": "1.0.0" },
  "normativeParameters": {},
  "snapshotCapturedAt": "2026-07-19T19:00:00.000Z",
  "snapshotHash": { "...": "ContentHash, see IF-SG-SCHEMA-000 §7.3" }
}
```

Every field is mandatory except `normativeParameters`, which MAY be an empty object but MUST be present (its absence vs. an empty object is not an ambiguity this schema permits — see [IF-SG-CANON-001 §3.5](IF-SG-CANON-001.md#35-optional-properties-and-null)).

## 4. Field-by-field semantics

| Field | Meaning |
|---|---|
| `snapshotIdentity` | `InstanceIdentifier` ([IF-SG-SCHEMA-000 §3.2](IF-SG-SCHEMA-000.md#32-instanceidentifier)) for this specific snapshot instance. Required so `SafetyGateDecision.resolutionSnapshotReference` and idempotency comparisons (§6) have a stable target distinct from `snapshotHash`. |
| `interventionDefinitionReference` | The candidate intervention definition being evaluated ([IF-SG-ARCH-002](README.md), not yet drafted). |
| `capabilityTaxonomyReference` | The capability taxonomy version in force ([IF-SG-TAX-001](README.md), not yet drafted). |
| `policyArtifactReference` | The resolved policy governing this evaluation. |
| `contextProjectionReference` | The `SafetyRelevantContextProjection` (IF-CORE-001, not yet drafted in full) — referenced opaquely; this schema does not define its internal shape. |
| `evidenceSnapshotReference` | The evidence bundle frozen at capture time. |
| `purposeBindingReference` | Proof that `contextProjectionReference` was generated for the purpose this resolution is being requested under. See §9 for how this relates to the decision-vs-failure boundary. |
| `resolutionProfileReference` | Which version of the resolution pipeline ([IF-SG-ARCH-005](README.md), not yet drafted) governs precedence and stage ordering for this snapshot. |
| `canonicalizationProfileReference` / `hashProfileReference` | Self-describing: which profiles were used to canonicalize and hash this very snapshot, so it remains verifiable under [IF-SG-CANON-001 §15 Compatibility policy](IF-SG-CANON-001.md#15-compatibility-policy) even after newer profiles exist. |
| `evaluatorConformanceReference` | Which evaluator conformance level (§14 of IF-SG-CANON-001) produced or is expected to consume this snapshot. |
| `normativeParameters` | Any additional normative inputs a specific resolution profile requires, not otherwise covered by the named references above. Left deliberately opaque in this version — the source conversation never enumerated its contents beyond naming the field. |
| `snapshotCapturedAt` | `NormativeTimestamp` ([IF-SG-SCHEMA-000 §6.1](IF-SG-SCHEMA-000.md#61-normativetimestamp)) marking the instant this snapshot was frozen. Normative (affects freshness, staleness, revalidation) — not to be confused with the operational timestamps excluded by [IF-SG-CANON-001 §9.5](IF-SG-CANON-001.md#95-operational-timestamps). |
| `snapshotHash` | `ContentHash` computed over all fields above except itself, per §7. |

**Naming note.** Two earlier names for the same two fields appear in the source material at different points in the design iteration: `evaluatorReference` → `evaluatorConformanceReference`, and `resolutionParameters` → `normativeParameters`. This document uses the later, more mature names — the same convention already applied when [IF-SG-SCHEMA-001](IF-SG-SCHEMA-001.md) preferred [IF-SG-CANON-001 §8.3](IF-SG-CANON-001.md#83-safetygatedecisionhash-material) over an earlier sketch.

## 5. Immutability

A `ResolutionSnapshot`, once captured, is append-only in the same sense as [ADR-004's Hypothesis Evidence Pattern](../adr/ADR-004-hypothesis-evidence-pattern.md) in the main project: it is never edited in place. Any of its referenced artifacts changing (policy updated, catalog revised, new evidence arriving) does not mutate an existing snapshot — it requires capturing a **new** snapshot for any subsequent resolution.

## 6. One-to-many relationship with resolution attempts

*"Esto permite conservar: One ResolutionSnapshot ↓ One or more resolution attempts, sin confundir el estado de entrada con el resultado."*

A single `ResolutionSnapshot` MAY be referenced by more than one `SafetyGateDecision` (or `ResolutionFailure`) over time — for example, when a request is retried after a technical failure, or reprocessed for idempotency. This is exactly the property [SYS-002 (idempotencia)](IF-SG-CONF-DATA-001.md#sys-002--idempotencia) requires:

> reprocesar una solicitud ya resuelta no debe crear dos decisiones lógicamente distintas... debe devolver la resolución existente, o producir una resolución equivalente vinculada al mismo snapshot.

**Invariant:** the snapshot records what was frozen for evaluation, never what was concluded. `authorizationStatus`, `operationalDisposition`, `evidenceResolutionState` and every other decision-side field defined in [IF-SG-SCHEMA-001](IF-SG-SCHEMA-001.md) MUST NOT appear anywhere in a `ResolutionSnapshot`.

## 7. SnapshotHash computation

Per the acyclic hash-construction order fixed in [IF-SG-CANON-001 §6](IF-SG-CANON-001.md#6-normative-hash-construction-order):

```
Canonical Source Artifacts
        ↓
Source Artifact Hashes
        ↓
ResolutionSnapshot
        ↓
SnapshotHash          ← this document
        ↓
JustificationGraph
        ↓
GraphHash
        ↓
SafetyGateDecision
        ↓
DecisionHash
```

**Constraints**

- Every source artifact referenced by this snapshot ([§3](#3-required-fields)) MUST already be schema-valid, canonically serialized and independently hashed *before* `SnapshotHash` is computed — the snapshot cannot hash artifacts it hasn't verified.
- `SnapshotHash` MUST be computed over exactly the positive material list fixed in [IF-SG-CANON-001 §8.1](IF-SG-CANON-001.md#81-resolutionsnapshothash-material): `interventionDefinitionReference`, `capabilityTaxonomyReference`, `policyArtifactReference`, `contextProjectionReference`, `evidenceSnapshotReference`, `purposeBindingReference`, `resolutionProfileReference`, `canonicalizationProfileReference`, `hashProfileReference`, `evaluatorConformanceReference`, `normativeParameters`, `snapshotCapturedAt`.
- `snapshotIdentity` and `snapshotHash` itself MUST NOT be included in the hashed material (an identifier and a hash cannot be inputs to their own computation without circularity).
- `SnapshotHash` MUST be computed before the `JustificationGraph` is finalized ([IF-SG-CANON-001 §6.2](IF-SG-CANON-001.md#62-resolutionsnapshot)); the graph may reference the snapshot or its hash, but the snapshot MUST NOT reference the graph or the decision.

## 8. Temporal semantics and concurrency

The source conversation fixes a concrete scenario that this schema must support without ambiguity:

```
T0: se genera SafetyRelevantContextProjection
T1: comienza la resolución
T2: aparece una nueva contraindicación
T3: se emite la decisión
```

*"La decisión debe vincularse al snapshot de T0, pero puede quedar obsoleta inmediatamente."*

**Required behavior**

- `snapshotCapturedAt` records `T0` (or the moment the last input artifact was frozen into this snapshot) — never `T1` or `T3`.
- A `SafetyGateDecision` computed at `T3` referencing this snapshot remains historically valid and MUST NOT be rewritten when the `T2` event is later discovered.
- The `T2` event triggers `revalidationRequired: true` on the *decision* ([IF-SG-SCHEMA-001 §11](IF-SG-SCHEMA-001.md#11-revalidationrequirement)), not a mutation of the *snapshot*. The snapshot stays exactly as frozen at `T0`; only the decision's revalidation state changes, and only by creating that explicit marker — never by editing `authorizationStatus` in place.

This is the same invariant tested operationally by [SYS-003](IF-SG-CONF-DATA-001.md#sys-003--cambio-contextual-concurrente), viewed from the snapshot's side rather than the decision's side.

## 9. Purpose binding: carried here, verified there

The snapshot **carries** `purposeBindingReference` — proof that its `contextProjectionReference` was generated for a declared purpose. It does not itself judge whether that purpose matches the purpose of the *current* resolution request; that judgment, and the decision/failure distinction it produces, belongs to [IF-SG-SCHEMA-001 §14](IF-SG-SCHEMA-001.md#14-the-purpose-binding-boundary-decision-vs-failure):

```
purposeBindingReference present and readable, but does not match
  the requested purpose
      → SafetyGateDecision: NOT_AUTHORIZED, STOP    (CONF-011)

purposeBindingReference present but cannot be resolved/read
  (e.g. its target artifact is unavailable)
      → ResolutionFailure: PURPOSE_VERIFICATION_FAILURE   (CONF-012 pattern)
```

**Invariant:** a `ResolutionSnapshot` MUST include `purposeBindingReference` even when the eventual outcome is a `ResolutionFailure` — the snapshot records what was available to check, not the result of checking it.

## 10. Relationship to ResolutionRequest (out of scope)

What was actually *asked for* — a specific intervention, or a required capability such as `ACTIVATION_REDUCTION` with candidate interventions to be matched against it (see the capability-resolution scenario in [CONF-009](IF-SG-CONF-DATA-001.md#conf-009--capability-match-sin-autorización)) — is not part of `ResolutionSnapshot`. In the source conversation this belongs to a `ResolutionRequest` consumed by a `SafetyGateResolver.resolve(ResolutionRequest): ResolutionResult` interface, sketched only in passing as part of IF-SG-IFACE-001 (not yet drafted).

This document deliberately does not invent a `resolutionRequestReference` field: the conversation never fixed whether the request is itself an integrity-bound artifact folded into the snapshot, or a separate ephemeral input that merely selects which `interventionDefinitionReference` gets frozen into it. That decision is left open for IF-SG-IFACE-001.

## 11. Worked example — completing CONF-001

[IF-SG-SCHEMA-001 §15.1](IF-SG-SCHEMA-001.md#151-conf-001-authorized--proceed) left `resolutionSnapshotReference` as a placeholder. With this schema, the referenced snapshot is:

```json
{
  "snapshotIdentity": "b2c3d4e5-0001-4000-8000-000000000001",
  "interventionDefinitionReference": { "artifactId": "if-sg:intervention:observational-pause-001", "artifactType": "INTERVENTION_DEFINITION", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
  "capabilityTaxonomyReference": { "artifactId": "IF-SG-TAX-001", "artifactType": "CAPABILITY_TAXONOMY", "artifactVersion": "0.1.0", "contentHash": { "...": "ContentHash" } },
  "policyArtifactReference": { "artifactId": "if-sg:policy:default-001", "artifactType": "POLICY_ARTIFACT", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
  "contextProjectionReference": { "artifactId": "if-core:projection:...", "artifactType": "SAFETY_RELEVANT_CONTEXT_PROJECTION", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
  "evidenceSnapshotReference": { "artifactId": "if-sg:evidence:body-state-snapshot-001", "artifactType": "EVIDENCE_SNAPSHOT", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
  "purposeBindingReference": { "purposeReference": { "artifactId": "IF-SG-PURPOSE-DEESCALATION-001", "artifactType": "NORMATIVE_PURPOSE", "artifactVersion": "1.0.0" }, "boundArtifactReference": { "...": "IntegrityBoundArtifactReference" }, "bindingArtifactReference": { "...": "IntegrityBoundArtifactReference" }, "bindingValidityWindow": { "validFrom": "2026-07-19T18:45:00.000Z", "validUntil": "2026-07-19T19:15:00.000Z" } },
  "resolutionProfileReference": { "artifactId": "IF-SG-ARCH-005", "artifactType": "RESOLUTION_PIPELINE", "artifactVersion": "1.0.0" },
  "canonicalizationProfileReference": { "artifactId": "IF-SG-CANON-001", "artifactVersion": "0.1.1" },
  "hashProfileReference": { "artifactId": "IF-SG-HASH-SHA256-V1", "artifactVersion": "1.0.0" },
  "evaluatorConformanceReference": { "artifactId": "IF-SG-EVAL-001", "artifactType": "EVALUATOR_CONFORMANCE_PROFILE", "artifactVersion": "1.0.0" },
  "normativeParameters": {},
  "snapshotCapturedAt": "2026-07-19T18:45:00.000Z",
  "snapshotHash": { "...": "ContentHash" }
}
```

The `SafetyGateDecision.resolutionSnapshotReference` in CONF-001 is an `IntegrityBoundArtifactReference` pointing at `snapshotIdentity: b2c3d4e5-...` with `contentHash` matching `snapshotHash` above — never a copy of these fields.

## 12. Forbidden representations

- Any decision-side field (`authorizationStatus`, `operationalDisposition`, `evidenceResolutionState`, `decisionHash`) present inside a `ResolutionSnapshot`.
- `snapshotHash` computed before every referenced source artifact has its own independent, verified hash.
- `snapshotHash` or `snapshotIdentity` included in the material used to compute `snapshotHash` itself.
- `snapshotCapturedAt` set to the time the decision was emitted (`T3`) instead of the time the inputs were frozen (`T0`).
- Editing a previously captured snapshot in place instead of capturing a new one.
- A `SafetyGateDecision` embedding a full copy of its snapshot's fields instead of an `IntegrityBoundArtifactReference`.
- `purposeBindingReference` omitted because the eventual result was a `ResolutionFailure` rather than a `SafetyGateDecision`.

## 13. Exit criteria

This specification is eligible to advance from DRAFT to TESTING when:

- a JSON Schema exists that validates every field in [§3](#3-required-fields) as `IntegrityBoundArtifactReference` (or the specific typed exception, e.g. `snapshotCapturedAt`);
- the worked example in [§11](#11-worked-example--completing-conf-001) round-trips through canonicalization and produces a stable `snapshotHash` reproducible by two independent implementations (Level B per [IF-SG-CANON-001 §14](IF-SG-CANON-001.md#14-conformance-levels));
- SYS-002 (idempotencia) is demonstrated with two `SafetyGateDecision` instances referencing the same `ResolutionSnapshot`;
- SYS-003 (revalidación sin reescritura) is demonstrated with a snapshot whose `T0` predates a decision's `T3`, per §8.

## 14. Governing invariant

> Un `ResolutionSnapshot` es la fotografía congelada de lo que se sabía cuando se resolvió — nunca lo que se concluyó, y nunca lo que se sabe ahora.
