# IF-SG-SCHEMA-000 — Normative Common Types

Version: 0.1.1
Status: DRAFT
Document Family: IF-SG-SCHEMA
Normative Scope: Integrity Family Safety Gate
Depends on: IF-SG-ARCH-002, IF-SG-ARCH-005, IF-SG-TAX-001
Consumed by: IF-SG-CANON-001, IF-SG-CONF-DATA-001, IF-SG-SCHEMA-001, IF-SG-SCHEMA-002, IF-SG-SCHEMA-003

> Nota de procedencia: este documento se trasladó verbatim desde una sesión de diseño conceptual externa al repositorio (no generada por Claude Code). No corresponde a ningún módulo implementado en `backend/`. Es la base normativa antes de cualquier especificación de implementación (JSON Schemas, interfaces de dominio, evaluador de referencia).

## 1. Purpose

This specification defines the common normative types used by Integrity Family Safety Gate artifacts.

Its purpose is to prevent different implementations from assigning different meanings to:

- identifiers;
- versions;
- decimal values;
- timestamps;
- temporal windows;
- missing information;
- evidence provenance;
- integrity references;
- retry semantics;
- normative statuses;
- reason codes.

This document defines representation semantics. It does not define clinical interpretation, human classification, intervention eligibility, authorization outcomes, or implementation technology.

## 2. Normative principles

### 2.1 Semantic uniqueness

Every normative value MUST have one unambiguous interpretation.

### 2.2 Representation fidelity

An implementation MUST NOT silently transform one valid normative value into another representation when that transformation may alter declared precision, provenance, validity, identity, or meaning.

### 2.3 Non-equivalence of identity types

Logical identity, operational instance identity, and content-derived identity MUST NOT be treated as interchangeable.

### 2.4 Non-collapse of epistemic states

The following concepts MUST remain distinguishable:

- false;
- absent;
- unknown;
- indeterminate;
- not applicable;
- explicitly declined;
- unavailable.

### 2.5 No human classification

No type defined by this specification may be used to classify the identity, worth, personality, dangerousness, functionality, or diagnosis of a person or family.

## 3. Core identity types

### 3.1 NormativeIdentifier

A NormativeIdentifier identifies a stable logical resource within a normative namespace.

**Structure**

```json
{
  "namespace": "if-sg",
  "resourceType": "capability",
  "localId": "activation-reduction"
}
```

**Canonical textual representation**

```
if-sg:capability:activation-reduction
```

**Constraints**

- `namespace` MUST be lowercase ASCII.
- `resourceType` MUST be lowercase kebab-case.
- `localId` MUST be lowercase kebab-case.
- Whitespace is prohibited.
- The identifier MUST remain stable across compatible revisions of the same logical resource.
- A change in human-readable label MUST NOT require a new identifier.
- A change in normative meaning MUST require a new identifier or a new incompatible version.

### 3.2 InstanceIdentifier

An InstanceIdentifier identifies a specific operational instance.

For version 0.1.1, the required representation is UUID v4 in lowercase canonical form.

```
550e8400-e29b-41d4-a716-446655440000
```

**Constraints**

- Uppercase hexadecimal characters are prohibited.
- Braces are prohibited.
- Compact UUID representations are prohibited.
- Instance identifiers MUST NOT be used as substitutes for normative logical identifiers.

### 3.3 ContentAddressedIdentifier

A ContentAddressedIdentifier identifies content from its canonical digest.

```json
{
  "hashProfileReference": {
    "artifactId": "IF-SG-HASH-SHA256-V1",
    "artifactVersion": "1.0.0"
  },
  "digest": "64-lowercase-hex-characters"
}
```

Canonical textual form:

```
sha256:<digest>
```

A content-addressed identifier MUST NOT replace the logical identity of an artifact unless the governing artifact profile explicitly requires content-addressed identity.

## 4. Versioning and governance types

### 4.1 SemanticVersion

SemanticVersion MUST conform to Semantic Versioning 2.0.0.

Base structure: `MAJOR.MINOR.PATCH`

Example: `0.1.1`

**Constraints**

- The prefix `v` MUST NOT appear in the stored normative value.
- Document lifecycle status MUST NOT be embedded in the version.
- Prerelease and build metadata MAY be used only when explicitly permitted by the governing artifact profile.
- Version comparison MUST follow Semantic Versioning precedence rules.

Correct:

```json
{
  "version": "0.1.1",
  "status": "DRAFT"
}
```

Incorrect: `v0.1.1-DRAFT`

### 4.2 NormativeStatus

`DRAFT` · `TESTING` · `PROVISIONAL_STABLE` · `STABLE` · `DEPRECATED` · `RETIRED`

**Semantics**

- `DRAFT`: incomplete and not eligible for production resolution.
- `TESTING`: eligible only for controlled conformance or validation environments.
- `PROVISIONAL_STABLE`: semantically stable but still subject to controlled validation.
- `STABLE`: approved for governed production use.
- `DEPRECATED`: retained for historical verification but not recommended for new resolutions.
- `RETIRED`: prohibited for new resolutions unless a specific historical reconstruction profile permits it.

Lifecycle eligibility MUST be determined by the applicable lifecycle policy. Status alone does not authorize use.

## 5. Decimal and measurement types

### 5.1 FixedScaleDecimal

A FixedScaleDecimal represents a decimal without binary floating-point ambiguity.

```json
{
  "unscaled": 600,
  "scale": 3
}
```

Semantic value: `value = unscaled × 10^(-scale)`

**Constraints**

- `unscaled` MUST be an integer.
- `scale` MUST be an integer between 0 and 18 inclusive.
- Scientific notation is prohibited.
- Floating-point JSON numbers are prohibited for normative decimal values.
- Silent scale normalization is prohibited.
- Each consuming type MUST declare either `requiredScale` or `allowedScaleRange`.

The following values are mathematically equal but not necessarily canonically equivalent:

```json
{"unscaled": 6, "scale": 1}
{"unscaled": 600, "scale": 3}
```

When `scale` represents declared precision, the two values MUST remain distinct.

### 5.2 UnitOfMeasurementReference

```json
{
  "system": "UCUM",
  "code": "min",
  "version": "1"
}
```

Integrity Family internal example:

```json
{
  "system": "IF-NORMATIVE-UNITS",
  "code": "activation-normalized-0-1",
  "version": "1.0.0"
}
```

**Constraints**

- `system` MUST identify the unit system.
- `code` MUST be defined within that system.
- Ambiguous units such as `points`, `level`, or `score` are prohibited unless accompanied by a governed interpretation profile.
- A normalized ratio, probability, confidence score, intensity, and proportion MUST NOT share a unit code merely because each uses a range from zero to one.

### 5.3 BoundedMeasure

```json
{
  "value": { "unscaled": 600, "scale": 3 },
  "unitReference": {
    "system": "IF-NORMATIVE-UNITS",
    "code": "activation-normalized-0-1",
    "version": "1.0.0"
  },
  "interpretationProfileReference": {
    "artifactId": "IF-MEASURE-ACTIVATION-001",
    "artifactType": "MEASUREMENT_PROFILE",
    "artifactVersion": "1.0.0"
  }
}
```

The permitted range and scale SHOULD be resolved through the referenced interpretation profile rather than duplicated inconsistently across instances.

## 6. Temporal types

### 6.1 NormativeTimestamp

Required representation: `YYYY-MM-DDTHH:mm:ss.SSSZ`

Example: `2026-07-19T19:00:00.000Z`

**Constraints**

- UTC `Z` is mandatory.
- Millisecond precision is mandatory.
- Offsets other than `Z` are prohibited.
- Timestamps without a timezone are prohibited.
- More or fewer than three fractional digits are prohibited.
- Impossible calendar values are prohibited.
- Silent truncation or rounding is prohibited.
- Leap seconds are not supported in version 0.1.1.

### 6.2 TimeWindow

A TimeWindow uses the universal boundary convention: `[start, end)`

```json
{
  "start": "2026-07-19T18:45:00.000Z",
  "end": "2026-07-19T19:00:00.000Z"
}
```

**Constraints**

- `start` is inclusive.
- `end` is exclusive.
- `start` MUST be earlier than `end`.
- Empty windows are prohibited.
- The boundary convention MUST NOT be overridden by an instance.

### 6.3 ExactDuration

ExactDuration uses a restricted ISO 8601 duration profile.

Valid examples: `PT15M` · `PT300S` · `P1D`

**Permitted components**: days, hours, minutes, seconds.

**Prohibited components**: years, months, calendar-dependent durations, fractional components unless explicitly permitted by a consuming profile.

`P1M` is prohibited because its exact length depends on a calendar anchor.

`CalendarDuration` is deferred to a future specification.

### 6.4 ValidityWindow

```json
{
  "validFrom": "2026-07-19T19:00:00.000Z",
  "validUntil": "2026-07-20T19:00:00.000Z"
}
```

**Constraints**

- `validFrom` is inclusive.
- `validUntil` is exclusive.
- An omitted `validUntil` is permitted only when the governing lifecycle policy explicitly permits open-ended validity.
- Validity windows and observation windows MUST NOT be treated as the same concept.

## 7. Integrity and artifact-reference types

### 7.1 ArtifactReference

```json
{
  "artifactId": "IF-SG-TAX-001",
  "artifactType": "CAPABILITY_TAXONOMY",
  "artifactVersion": "0.1.0"
}
```

An ArtifactReference MAY be used for navigation, discovery, documentation, or unresolved lookup. It is insufficient for reconstructing a normative resolution.

### 7.2 IntegrityBoundArtifactReference

```json
{
  "artifactId": "IF-SG-TAX-001",
  "artifactType": "CAPABILITY_TAXONOMY",
  "artifactVersion": "0.1.0",
  "contentHash": {
    "hashProfileReference": { "artifactId": "IF-SG-HASH-SHA256-V1", "artifactVersion": "1.0.0" },
    "canonicalizationProfileReference": { "artifactId": "IF-SG-CANON-001", "artifactVersion": "0.1.1" },
    "digestEncoding": "LOWERCASE_HEX",
    "digest": "64-lowercase-hex-characters"
  }
}
```

Every artifact materially used in a Safety Gate resolution MUST be represented by an IntegrityBoundArtifactReference.

### 7.3 ContentHash

Required structure:

```json
{
  "hashProfileReference": { "artifactId": "IF-SG-HASH-SHA256-V1", "artifactVersion": "1.0.0" },
  "canonicalizationProfileReference": { "artifactId": "IF-SG-CANON-001", "artifactVersion": "0.1.1" },
  "digestEncoding": "LOWERCASE_HEX",
  "digest": "64-lowercase-hex-characters"
}
```

**Constraints**

- Digest length MUST be exactly 64 hexadecimal characters.
- Only lowercase hexadecimal is permitted.
- `0x` prefixes are prohibited.
- Base64 is prohibited under this profile.
- Whitespace is prohibited.
- The hash and canonicalization profiles MUST be explicit.
- A digest without its governing profiles is invalid.

### 7.4 IntegrityStatus

`VERIFIED` · `MISMATCH` · `NOT_VERIFIABLE` · `NOT_PROVIDED`

**Semantics**

- `VERIFIED`: canonicalization succeeded and the computed digest matched.
- `MISMATCH`: verification completed and the digest did not match.
- `NOT_VERIFIABLE`: integrity material exists, but a technical condition prevented verification.
- `NOT_PROVIDED`: no required integrity material was supplied.

`MISMATCH` and `NOT_VERIFIABLE` MUST NOT be treated as equivalent.

## 8. Evidence and provenance types

### 8.1 EvidenceReference

```json
{
  "evidenceId": "if-sg:evidence:body-state-snapshot-001",
  "evidenceType": "BODY_STATE_SNAPSHOT",
  "evidenceVersion": "1.0.0",
  "contentHash": {}
}
```

Evidence materially used by a resolution MUST be integrity-bound.

### 8.2 EvidenceProvenanceReference

```json
{
  "sourceArtifactReference": {},
  "observationSourceReference": {},
  "acquisitionMethodReference": {
    "artifactId": "IF-SG-METHOD-SELF-REPORT-001",
    "artifactType": "ACQUISITION_METHOD",
    "artifactVersion": "1.0.0"
  },
  "capturedAt": "2026-07-19T19:00:00.000Z",
  "integrityHash": {}
}
```

Provenance MUST preserve the origin and acquisition pathway without requiring unnecessary direct identity disclosure.

### 8.3 ObservationSourceReference

```json
{
  "sourceType": "PARTICIPANT_SELF_REPORT",
  "sourceInstanceReference": "urn:if:subject:pseudonymous:abc123",
  "acquisitionMethodReference": {
    "artifactId": "IF-SG-METHOD-SELF-REPORT-001",
    "artifactType": "ACQUISITION_METHOD",
    "artifactVersion": "1.0.0"
  }
}
```

Permitted source types: `PARTICIPANT_SELF_REPORT` · `FAMILY_MEMBER_REPORT` · `PROFESSIONAL_OBSERVATION` · `SYSTEM_DERIVED_MEASURE` · `EXTERNAL_ARTIFACT`

**Invariant**: `sourceType` describes provenance. It MUST NOT encode authority, credibility ranking, or epistemic superiority.

## 9. Missingness and knowledge-state types

### 9.1 MissingnessSemantics

`NOT_OBSERVED` · `NOT_REPORTED` · `NOT_MEASURABLE` · `DECLINED` · `UNAVAILABLE` · `NOT_APPLICABLE`

The omission of a property MUST NOT be used to encode one of these meanings when the missingness state is normatively relevant.

### 9.2 KnownValue

```json
{ "state": "KNOWN", "value": false }
```

### 9.3 UnknownValue

```json
{
  "state": "UNKNOWN",
  "reasonCode": { "namespace": "IF-SG-EVIDENCE", "code": "SOURCE_UNAVAILABLE", "version": "1.0.0" }
}
```

### 9.4 IndeterminateValue

```json
{
  "state": "INDETERMINATE",
  "reasonCode": { "namespace": "IF-SG-EVIDENCE", "code": "INSUFFICIENT_RESOLUTION", "version": "1.0.0" }
}
```

**Invariants**

- `false` MUST NOT mean unknown.
- `null` MUST NOT mean absent, unknown, or not applicable unless a consuming profile explicitly defines that meaning.
- `UNKNOWN` MUST NOT be converted into `FALSE`.
- `INDETERMINATE` MUST NOT be converted into `UNSATISFIED`.
- `NOT_APPLICABLE` MUST NOT be treated as missing evidence.

## 10. Purpose and policy references

### 10.1 PurposeReference

```json
{
  "artifactId": "IF-SG-PURPOSE-DEESCALATION-001",
  "artifactType": "NORMATIVE_PURPOSE",
  "artifactVersion": "1.0.0"
}
```

### 10.2 PurposeBindingReference

```json
{
  "purposeReference": {},
  "boundArtifactReference": {},
  "bindingArtifactReference": {},
  "bindingValidityWindow": {}
}
```

A PurposeReference identifies the declared purpose. A PurposeBindingReference provides the governed relationship proving that an artifact is eligible for use under that purpose. The two MUST NOT be treated as equivalent.

### 10.3 PolicyReference

A policy materially used in resolution MUST use IntegrityBoundArtifactReference.

### 10.4 CapabilityReference

A capability reference MUST identify a versioned capability declared in a governed capability taxonomy.

Capability references MUST NOT imply: successful outcome; applicability; authorization; causality; human classification.

## 11. Reason and pipeline types

### 11.1 ReasonCode

```json
{ "namespace": "IF-SG-EVIDENCE", "code": "REQUIRED_EVIDENCE_ABSENT", "version": "1.0.0" }
```

**Constraints**

- `namespace` MUST identify the governing reason-code family.
- `code` MUST be stable and machine-readable.
- `version` MUST identify the semantic version of the reason.
- Human-readable text MUST NOT be embedded as the primary reason.
- A published code MUST NOT change meaning silently.
- A semantic change requires a new code or incompatible version.

### 11.2 ResolutionStageReference

```json
{
  "stageId": "EVIDENCE_RESOLUTION",
  "pipelineProfileReference": {
    "artifactId": "IF-SG-ARCH-005",
    "artifactType": "RESOLUTION_PIPELINE",
    "artifactVersion": "1.0.0"
  }
}
```

The normative order MUST be resolved from the pipeline profile. An implementation MAY record operational execution order separately, but operational order MUST NOT redefine normative precedence.

## 12. Retry types

### 12.1 NormativeRetryability

```json
{
  "status": "CONDITIONALLY_RETRYABLE",
  "conditionReferences": [
    { "namespace": "IF-SG-RETRY", "code": "POLICY_STORE_RESTORED", "version": "1.0.0" }
  ]
}
```

Permitted values: `RETRYABLE` · `NON_RETRYABLE` · `CONDITIONALLY_RETRYABLE`

When status is `CONDITIONALLY_RETRYABLE`, at least one condition reference is mandatory.

### 12.2 OperationalRetryPolicy

Operational retry controls are separate from normative retryability.

```json
{
  "minimumDelay": "PT30S",
  "maximumAttempts": 3,
  "backoffProfileReference": {}
}
```

Operational retry fields MUST NOT be interpreted as part of a domain authorization decision.

## 13. Type eligibility for normative resolution

Artifacts used in production resolution MUST:

- have status `PROVISIONAL_STABLE` or `STABLE`, subject to lifecycle policy;
- use integrity-bound references;
- use supported canonicalization and hash profiles;
- satisfy all type-specific constraints;
- preserve missingness and provenance semantics;
- avoid untyped free-form values where governed types exist.

## 14. Forbidden representations

- Unqualified UUID used as logical identity
- Floating-point normative decimal
- Timestamp without UTC `Z`
- Timestamp without millisecond precision
- Calendar-dependent duration used as exact duration
- Reason text used instead of ReasonCode
- ArtifactReference without hash in a ResolutionSnapshot
- Generic "unknown" used to encode all missingness
- `null` used ambiguously
- Observation source used as credibility ranking
- Free-form measurement unit
- Semantic version containing lifecycle status

## 15. Exit criteria

This specification is eligible to advance from DRAFT to TESTING when:

- JSON Schemas validate every type;
- canonicalization vectors cover every primitive with hash relevance;
- invalid-state examples are rejected;
- at least two independent runtimes produce equivalent canonical output;
- the initial conformance fixtures can express unknown, false, absence, and indeterminacy independently;
- no resolution-critical reference remains unbound to content integrity.

## 16. Governing invariant

> Before evaluating a normative decision, every implementation must agree on the exact meaning and canonical representation of identity, precision, time, absence, provenance, version and integrity.
