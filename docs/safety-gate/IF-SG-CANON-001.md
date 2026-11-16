# IF-SG-CANON-001 — Canonicalization and Hashing Profile

Version: 0.1.1
Status: DRAFT
Document Family: IF-SG-CANON
Normative Scope: Integrity Family Safety Gate
Hash Profile: IF-SG-HASH-SHA256-V1
Depends on: IF-SG-SCHEMA-000
Consumed by: IF-SG-CONF-001, IF-SG-CONF-DATA-001, IF-SG-SCHEMA-001, IF-SG-SCHEMA-002, IF-SG-SCHEMA-003

> Nota de procedencia: trasladado verbatim desde una sesión de diseño conceptual externa al repositorio. No corresponde a ningún módulo implementado en `backend/`. Depende de [IF-SG-SCHEMA-000](IF-SG-SCHEMA-000.md).

## 1. Purpose

This specification defines the canonical representation and hashing rules for Integrity Family Safety Gate normative artifacts.

Its purpose is to ensure that independent conforming implementations produce:

- identical canonical bytes;
- identical source artifact hashes;
- identical snapshot hashes;
- identical justification graph hashes;
- identical decision hashes;

when given the same normative values and the same applicable profiles.

Canonicalization is not a mechanism for repairing invalid data. Invalid normative input MUST be rejected before hashing.

## 2. Governing principles

### 2.1 Canonical value before canonical bytes

Only values valid under their governing schema may be canonicalized.

```
Schema validation
    ↓
Normative validation
    ↓
Canonical serialization
    ↓
Hash computation
```

### 2.2 Normative and operational separation

Normative hashes MUST represent the artifacts, rules, evidence, provenance and outcomes that constitute the resolution. They MUST NOT represent: runtime behavior; infrastructure topology; internal logs; latency; retries; display formatting; human-readable explanations.

### 2.3 Historical immutability

A historical hash MUST always be verified with the profiles originally declared in its ContentHash. A new canonicalization or hash profile MUST NOT silently replace or reinterpret an earlier profile.

### 2.4 Acyclic hash construction

Hash dependencies MUST form a directed acyclic structure. Circular hash dependency is prohibited.

## 3. Canonicalization profile

### 3.1 Encoding

Canonical output MUST use UTF-8 without BOM.

### 3.2 Base JSON canonicalization

The profile MUST use a deterministic JSON canonicalization procedure compatible with the rules fixed by this document. The canonical output MUST:

- contain no insignificant whitespace;
- serialize object properties in the required deterministic order;
- preserve string content exactly;
- reject duplicate object keys;
- reject values not permitted by the applicable schema;
- preserve array order unless the consuming schema explicitly defines the array as a semantic set.

### 3.3 Property ordering

Object properties MUST be ordered deterministically according to the adopted canonical JSON profile. Input property order has no normative meaning unless a property is represented as an array whose order is explicitly normative.

These inputs are canonically equivalent:

```json
{"a":1,"b":2}
```
```json
{ "b": 2, "a": 1 }
```

They MUST produce identical canonical bytes and identical hashes.

### 3.4 Arrays

Arrays are ordered by default. An implementation MUST NOT sort arrays automatically. A schema MAY declare an array to represent a semantic set. In that case, the schema or artifact profile MUST define: the canonical sort key; duplicate handling; identity rules; collision handling. Without such a declaration, original array order is normative.

### 3.5 Optional properties and null

Property absence and explicit `null` are not automatically equivalent.

Rules:

- Optional information that is not present SHOULD normally be omitted.
- `null` MAY be used only where the governing schema assigns it an explicit normative meaning.
- Canonicalization MUST NOT add absent optional properties.
- Canonicalization MUST NOT remove explicitly valid `null` properties.
- Missingness semantics MUST be represented through governed discriminated types when normatively relevant.

## 4. Primitive canonicalization rules

### 4.1 FixedScaleDecimal

A normative decimal MUST use `{"unscaled": 600, "scale": 3}`. Floating-point representations are prohibited. Canonicalization MUST NOT reduce trailing zero precision.

These values are not automatically canonically equivalent:

```json
{"unscaled":6,"scale":1}
{"unscaled":600,"scale":3}
```

They are equivalent only when the consuming type explicitly declares scale-insensitive semantics. The default is scale-sensitive.

### 4.2 NormativeTimestamp

The only permitted format is `YYYY-MM-DDTHH:mm:ss.SSSZ`, e.g. `2026-07-19T19:00:00.000Z`.

Canonicalization MUST NOT transform alternate time representations into the normative form. The following MUST be rejected rather than normalized:

```
2026-07-19T19:00:00Z
2026-07-19T14:00:00.000-05:00
2026-07-19 19:00:00
```

### 4.3 ExactDuration

Only the restricted exact-duration profile defined in IF-SG-SCHEMA-000 is permitted. Calendar-relative durations such as `P1M` MUST be rejected when an exact duration is required. Equivalent duration expressions MAY remain distinct unless a consuming profile defines normalization. For version 0.1.1, implementations MUST preserve the exact valid lexical representation supplied after schema validation.

### 4.4 Identifiers

Identifiers MUST preserve their schema-defined case and structure. Implementations MUST NOT: lowercase values not declared case-insensitive; replace logical identifiers with UUIDs; derive new identifiers from display labels; rewrite namespaces; remove version information.

## 5. Hash profile

### 5.1 Profile identity

Profile: `IF-SG-HASH-SHA256-V1` — Algorithm: SHA-256 — Digest encoding: lowercase hexadecimal — Digest length: 64 hexadecimal characters.

### 5.2 Hash input

The digest MUST be computed over the exact canonical UTF-8 byte sequence of the designated hash material.

```
digest = SHA-256(canonical_utf8_bytes)
```

### 5.3 ContentHash structure

Every digest MUST declare: hash profile; canonicalization profile; digest encoding; digest value. A bare digest string is insufficient for normative reconstruction.

### 5.4 Digital signatures

A DecisionHash is not a digital signature. Digital signatures, certificate chains, countersignatures and trusted timestamps belong to a separate integrity-envelope layer. A signature MAY sign a normative hash after that hash has been computed. A signature MUST NOT be included in the material used to calculate the hash it signs.

## 6. Normative hash-construction order

```
Canonical Source Artifacts
        ↓
Source Artifact Hashes
        ↓
ResolutionSnapshot
        ↓
SnapshotHash
        ↓
JustificationGraph
        ↓
GraphHash
        ↓
SafetyGateDecision
        ↓
DecisionHash
        ↓
Optional Digital Signature
        ↓
Immutable Ledger Manifest
```

### 6.1 Source artifacts

Each source artifact MUST first be: schema-valid; normatively valid; canonically serialized; independently hashed.

### 6.2 ResolutionSnapshot

The snapshot MUST reference integrity-bound source artifacts. Its hash MUST be computed before the justification graph is finalized.

### 6.3 JustificationGraph

The graph MUST reference the snapshot or its hash. The graph MUST NOT reference the final DecisionHash.

### 6.4 SafetyGateDecision

The decision MAY reference SnapshotHash and GraphHash. The DecisionHash MUST be computed only after these references are finalized.

## 7. Circularity prohibition

Prohibited pattern: DecisionHash included in GraphHash material, and GraphHash included in DecisionHash material.

Permitted dependency direction: `SourceArtifactHash → SnapshotHash → GraphHash → DecisionHash`

A circular dependency MUST produce a ResolutionFailure, not a SafetyGateDecision.

Recommended failure representation:

```
failureCategory: NORMATIVE_ASSEMBLY_FAILURE
failureCode: CYCLIC_HASH_DEPENDENCY
failedStage: DECISION_FINALIZATION
```

## 8. Positive hash-material rules

### 8.1 ResolutionSnapshotHash material

Must include: `interventionDefinitionReference`, `capabilityTaxonomyReference`, `policyArtifactReference`, `contextProjectionReference`, `evidenceSnapshotReference`, `purposeBindingReference`, `resolutionProfileReference`, `canonicalizationProfileReference`, `hashProfileReference`, `evaluatorConformanceReference`, `normativeParameters`, `snapshotCapturedAt`. Only integrity-bound references are permitted for resolution-critical artifacts.

### 8.2 JustificationGraphHash material

Must include: `graphIdentity`, `graphProfileVersion`, `resolutionSnapshotReference`, `rootNodeReference`, `terminalNodeReferences`, `nodes`, `edges`, `constraints`. When node or edge order is not normative, the graph schema MUST define deterministic canonical ordering.

### 8.3 SafetyGateDecisionHash material

Must include: `decisionIdentity`, `decisionProfileReference`, `resolutionSnapshotReference`, `contractResolutionSummary`, `evidenceResolutionState`, `authorizationStatus`, `operationalDisposition`, `justificationGraphReference`, `revalidationRequirement`. A field that changes the normative meaning or reproducibility of the decision MUST be included.

## 9. Structural exclusion list

### 9.1 Transport metadata
`correlationId`, `requestId`, `traceId`, `spanId`, `messageId`, `deliveryAttempt`, `queueOffset`, `transportHeaders`

### 9.2 Runtime and infrastructure metadata
`processingDuration`, `stageLatency`, `cpuTime`, `memoryUsage`, `retryCount`, `threadIdentifier`, `workerIdentifier`, `hostIdentifier`, `containerIdentifier`, `deploymentRegion`, `runtimeVersion`. An evaluator conformance profile version may be normative. A concrete host or deployment instance is not.

### 9.3 Internal diagnostic data
`internalExecutionLogs`, `debugTrace`, `stackTrace`, `engineDiagnostics`, `profilingData`, `intermediateDebugValues`

### 9.4 Presentation metadata
`displayText`, `localizedLabel`, `humanReadableExplanation`, `translatedExplanation`, `uiHints`, `renderingCoordinates`, `visualStyle`, `formattingMetadata`

### 9.5 Operational timestamps
`receivedAt`, `serializedAt`, `persistedAt`, `transmittedAt`, `lastViewedAt`, `loggedAt`

### 9.6 Signature material
`digitalSignature`, `signatureTimestamp`, `certificateChain`, `transportEnvelope`, `countersignature`

## 10. Normative timestamps that remain included

The exclusion of operational timestamps MUST NOT be interpreted as a general exclusion of time. The following MUST be included when they affect resolution semantics: `snapshotCapturedAt`, `evidenceObservedAt`, `policyEffectiveFrom`, `policyEffectiveUntil`, `decisionValidityFrom`, `decisionValidityUntil`, `interventionEffectiveFrom`, `interventionEffectiveUntil`. Time affecting freshness, validity, applicability, lifecycle or revalidation is normative.

## 11. JustificationGraph canonical constraints

The graph MUST be a directed acyclic graph. It MUST include: unique node identifiers; valid edge endpoints; at least one root; at least one terminal node; governed relation types; reconstructible precedence; no dangling references; no cycles.

Cycle detection failure MUST produce ResolutionFailure. A graph cycle MUST NOT be repaired silently by: removing an edge; changing an edge type; choosing an arbitrary root; discarding a node; flattening the graph.

## 12. Positive conformance vectors

- **POS-001 — Property ordering**: `{"artifactVersion":"1.0.0","artifactId":"IF-SG-TAX-001"}` vs. the same object with different key order → same canonical bytes, same SHA-256 digest.
- **POS-002 — Whitespace independence**: pretty-printed and compact representations of the same valid JSON normative value MUST produce identical hashes.
- **POS-003 — Historical profile verification**: an artifact created under an older supported canonicalization profile MUST remain verifiable using that original profile. It MUST NOT be rehashed automatically under a newer profile.
- **POS-004 — Operational metadata exclusion**: two otherwise identical decisions with different `traceId`, `hostIdentifier`, `processingDuration` MUST produce the same DecisionHash.

## 13. Negative conformance vectors

| ID | Condition | Expected |
|---|---|---|
| NEG-001 | `serializedAt` included in normative hash material | Conformance Failure |
| NEG-002 | `2026-07-19T14:00:00.000-05:00` accepted as normative timestamp | Conformance Failure |
| NEG-003 | `{"activation":0.6}` used instead of FixedScaleDecimal | Schema or Conformance Failure |
| NEG-004 | `debugTrace` affects the digest | Conformance Failure |
| NEG-005 | GraphHash depends on DecisionHash | ResolutionFailure |
| NEG-006 | SHA-512 or BLAKE3 used while declaring IF-SG-HASH-SHA256-V1 | Conformance Failure |
| NEG-007 | Localized explanation changes the DecisionHash | Conformance Failure |
| NEG-008 | Digital signature included in the digest it signs | Conformance Failure |
| NEG-009 | Implementation sorts a precedence-sensitive array | Conformance Failure |
| NEG-010 | `purposeBindingReference` omitted from SnapshotHash material | Conformance Failure |
| NEG-011 | `{}` treated as canonically equivalent to `{"value":null}` without schema authorization | Conformance Failure |
| NEG-012 | `{"unscaled":6,"scale":1}` silently rewritten to `{"unscaled":600,"scale":3}` | Conformance Failure (unless consuming profile explicitly permits scale normalization) |
| NEG-013 | Old artifact rehashed using the newest profile, historical hash replaced | Conformance Failure |
| NEG-014 | JustificationGraph contains a directed cycle | ResolutionFailure (forbidden: any SafetyGateDecision) |

## 14. Conformance levels

- **Level A — Canonical Representation**: validates normative types; produces correct canonical bytes; rejects invalid representations.
- **Level B — Reproducible Integrity**: satisfies Level A; produces correct source, snapshot, graph and decision hashes; respects inclusion/exclusion lists; prevents circular dependencies.
- **Level C — Full Fixture Compatibility**: satisfies Levels A and B; passes every official IF-SG-CONF-DATA-001 fixture; produces all required results; produces none of the declared forbidden outcomes.

A production Safety Gate implementation MUST achieve Level C.

## 15. Compatibility policy

Canonicalization and hash profiles are immutable and additive. A new profile: MUST receive a new identifier or incompatible version; MUST NOT alter historical profile semantics; MUST NOT replace hashes in previously resolved decisions; MUST remain distinguishable in every ContentHash; MUST define migration rules without mutating historical evidence.

Example: `IF-SG-CANON-001 / 0.1.1` may coexist with a future `IF-SG-CANON-002 / 1.0.0`. Historical artifacts remain verifiable under their declared profile.

## 16. Conformance failure semantics

Canonicalization failure MUST occur before a domain decision is emitted. Failures include: `INVALID_NORMATIVE_REPRESENTATION`, `UNSUPPORTED_CANONICALIZATION_PROFILE`, `UNSUPPORTED_HASH_PROFILE`, `HASH_MISMATCH`, `CYCLIC_HASH_DEPENDENCY`, `CYCLIC_JUSTIFICATION_GRAPH`, `NON_CANONICAL_DECIMAL`, `INVALID_NORMATIVE_TIMESTAMP`, `AMBIGUOUS_NULL_SEMANTICS`.

Such conditions MUST produce a ResolutionFailure. They MUST NOT produce `AUTHORIZED`, `NOT_AUTHORIZED`, or `PENDING_RESOLUTION`.

## 17. Required implementation evidence

An implementation claiming conformity MUST provide: canonical input bytes for every official vector; resulting digest; declared profile references; validation result; failure result for every negative vector; evidence that excluded fields do not affect hashes; evidence that historical hashes remain verifiable; evidence that repeated executions are deterministic.

## 18. Exit criteria

This profile may advance from DRAFT to TESTING when: all positive vectors have fixed canonical outputs; all negative vectors have fixed failure classifications; at least two independent implementations produce identical hashes; timestamp, decimal, null and array-order rules are implemented; historical verification is demonstrated; circular graph and circular hash dependencies are rejected; no operational field affects normative hashes.

## 19. Governing invariant

> A normative hash must represent exactly the governed meaning of an artifact, never the incidental circumstances of its execution, transport, display or storage.
