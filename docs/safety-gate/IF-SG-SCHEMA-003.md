# IF-SG-SCHEMA-003 — JustificationGraph

Version: 0.1.0
Status: DRAFT
Document Family: IF-SG-SCHEMA
Normative Scope: Integrity Family Safety Gate
Depends on: [IF-SG-SCHEMA-000](IF-SG-SCHEMA-000.md), [IF-SG-CANON-001](IF-SG-CANON-001.md), [IF-SG-SCHEMA-001](IF-SG-SCHEMA-001.md), [IF-SG-SCHEMA-002](IF-SG-SCHEMA-002.md)
Consumed by: [IF-SG-CONF-DATA-001](IF-SG-CONF-DATA-001.md), IF-SG-EVAL-001, IF-SG-IFACE-001

> Nota de procedencia: como SCHEMA-001 y SCHEMA-002, este documento nunca se ensambló como archivo único. Se reconstruye a partir de la lista de campos madura ya transcrita verbatim en [IF-SG-CANON-001 §8.2](IF-SG-CANON-001.md#82-justificationgraphhash-material) y §11, la estructura de nodo/arista (`JustificationNode`/`JustificationEdge`) especificada dos veces de forma consistente, y una corrección explícita que quedó registrada entre dos mensajes consecutivos de la conversación original — documentada aquí en vez de ocultada (ver §3).

## 1. Purpose

`JustificationGraph` is the structured, machine-verifiable trace of *how* a `SafetyGateDecision` was reached: which predicates were evaluated, in what order, against which evidence and policy, and which relationships of dependency, support, contradiction or override held between them.

It exists so a decision can be audited, reconstructed, and challenged without trusting a human-written explanation. Stated verbatim in the source conversation:

> *"El JustificationGraph no guarda una cadena libre de pensamiento ni explicaciones generadas por un modelo. Conserva: predicados evaluados; referencias de evidencia; reglas aplicadas; estados resultantes; precedencias; disposición final. Es decir: traza normativa verificable — no: narración interna del evaluador. Una explicación humana puede derivarse posteriormente del grafo."*

## 2. Required fields

Per [IF-SG-CANON-001 §8.2](IF-SG-CANON-001.md#82-justificationgraphhash-material):

```json
{
  "graphIdentity": "c3d4e5f6-0001-4000-8000-000000000001",
  "graphProfileVersion": "1.0.0",
  "resolutionSnapshotReference": { "...": "IntegrityBoundArtifactReference, see IF-SG-SCHEMA-002" },
  "rootNodeReference": { "...": "JustificationNode reference, see §4" },
  "terminalNodeReferences": [ { "...": "JustificationNode reference" } ],
  "nodes": [ { "...": "JustificationNode, see §4" } ],
  "edges": [ { "...": "JustificationEdge, see §5" } ],
  "constraints": { "...": "see §7" },
  "graphHash": { "...": "ContentHash, see IF-SG-SCHEMA-000 §7.3" }
}
```

Every field is mandatory. `graphIdentity` is an `InstanceIdentifier` ([IF-SG-SCHEMA-000 §3.2](IF-SG-SCHEMA-000.md#32-instanceidentifier)) for this specific graph instance, distinct from `graphHash`.

## 3. A field that was proposed and rejected: `decisionReference`

An early sketch in the source conversation included `decisionReference` as a top-level field of `JustificationGraph`, alongside `graphIdentity`, `nodes`, `edges`. This was corrected in the very next message of the same design exchange, once the acyclic hash-construction order was fixed: a `JustificationGraph` referencing the decision it justifies would make `GraphHash` depend on `DecisionHash` while `DecisionHash` also depends on `GraphHash` — a direct circularity, explicitly prohibited by [IF-SG-CANON-001 §7](IF-SG-CANON-001.md#7-circularity-prohibition).

**Corrected rule**, stated verbatim: *"El grafo MUST reference the snapshot or its hash. El grafo MUST NOT reference the final DecisionHash."*

This document therefore does not define `decisionReference`. The graph references `resolutionSnapshotReference` ([IF-SG-SCHEMA-002](IF-SG-SCHEMA-002.md)) instead — the decision references the graph (`SafetyGateDecision.justificationGraphReference`, [IF-SG-SCHEMA-001 §3](IF-SG-SCHEMA-001.md#3-safetygatedecision--required-fields)), never the other way around. This is recorded here explicitly, in the same spirit as the rejected `CONDITIONALLY_AUTHORIZED` value in [IF-SG-SCHEMA-001 §7](IF-SG-SCHEMA-001.md#7-authorizationstatus-dimension-b): a discarded option is part of the specification's history, not an error to be hidden.

## 4. JustificationNode

```json
{
  "nodeId": "node-001",
  "nodeType": "EVIDENCE_RESOLUTION",
  "evaluatedArtifactReference": { "...": "IntegrityBoundArtifactReference to the artifact this node evaluated" },
  "predicateReference": { "...": "PredicateReference, see IF-SG-SCHEMA-000 §10.4-adjacent" },
  "resolutionStatus": "SATISFIED",
  "reasonCodes": [ { "namespace": "IF-SG-EVIDENCE", "code": "REQUIRED_EVIDENCE_ABSENT", "version": "1.0.0" } ],
  "evidenceReferences": [ { "...": "EvidenceReference, see IF-SG-SCHEMA-000 §8.1" } ],
  "policyReferences": [ { "...": "PolicyReference, see IF-SG-SCHEMA-000 §10.3" } ]
}
```

`resolutionStatus` reuses the `ContractResolution` enum fixed in [IF-SG-SCHEMA-001 §5](IF-SG-SCHEMA-001.md#5-contractresolution-per-contract-dimension-a): `SATISFIED`, `UNSATISFIED`, `UNRESOLVED`, `NOT_APPLICABLE`.

### 4.1 nodeType taxonomy

The source conversation fixes a node hierarchy aligned to the resolution pipeline (the same stage sequence already used for `ResolutionStageReference` and `contractResolutionSummary` in [IF-SG-SCHEMA-001](IF-SG-SCHEMA-001.md)):

```
LIFECYCLE_VALIDATION
PURPOSE_BINDING_VALIDATION
APPLICABILITY_RESOLUTION
EVIDENCE_RESOLUTION
    ├── EVIDENCE_EXISTENCE
    ├── EVIDENCE_QUALITY
    ├── EVIDENCE_COHERENCE
    └── EVIDENCE_FRESHNESS
CONTRAINDICATION_RESOLUTION
CAPABILITY_RESOLUTION
POLICY_RESOLUTION
OPERATIONAL_DISPOSITION
```

The four evidence sub-nodes correspond exactly to the four sub-questions `EvidenceContract` was designed to answer in the intervention catalog discussion (Existence, Calidad, Coherence, Freshness): *"A. Existencia... B. Calidad... C. Coherencia... D. Vigencia."* A single `EVIDENCE_RESOLUTION` node MAY aggregate its four children via `DEPENDS_ON` edges (§5) rather than duplicating their content.

## 5. JustificationEdge

```json
{
  "sourceNodeReference": "node-001",
  "targetNodeReference": "node-002",
  "relationType": "OVERRIDES",
  "precedenceOrder": 3
}
```

### 5.1 relationType semantics

| Relation | Meaning | Typical use |
|---|---|---|
| `EVALUATED_BEFORE` | Precedence ordering — source was resolved before target per the Decision Precedence Model ([IF-SG-SCHEMA-001 §9](IF-SG-SCHEMA-001.md#9-decision-precedence-model)) | `LIFECYCLE_VALIDATION → EVALUATED_BEFORE → EVIDENCE_RESOLUTION` |
| `DEPENDS_ON` | Target's resolution requires source's result as input | `EVIDENCE_RESOLUTION → DEPENDS_ON → EVIDENCE_EXISTENCE` |
| `SUPPORTED_BY` | Node's `resolutionStatus` is backed by the referenced evidence or policy | `EVIDENCE_RESOLUTION (SATISFIED) → SUPPORTED_BY → BodyStateSnapshot` |
| `CONTRADICTED_BY` | Links a node in `INCONSISTENT` state to the conflicting sources it preserves without discarding either | `EVIDENCE_COHERENCE (UNSATISFIED) → CONTRADICTED_BY → [narrative evidence, affective evidence]` |
| `OVERRIDES` | One resolution result takes precedence over another's outcome for the final disposition | see §5.2 |
| `RESULTS_IN` | A resolution node's outcome feeds the final `OPERATIONAL_DISPOSITION` node | `CONTRAINDICATION_RESOLUTION (UNSATISFIED) → RESULTS_IN → OPERATIONAL_DISPOSITION (STOP)` |
| `REQUIRES` | A node's evaluation presupposes another node exists and was resolved | `APPLICABILITY_RESOLUTION → REQUIRES → CAPABILITY_RESOLUTION` |

**Invariant**, stated verbatim: *"OVERRIDES debe usarse únicamente cuando la precedencia normativa lo permita explícitamente."* An `OVERRIDES` edge MUST correspond to a rule already fixed in [IF-SG-SCHEMA-001 §9's monotonicity invariant](IF-SG-SCHEMA-001.md#9-decision-precedence-model) — it is not a general-purpose escape hatch for an evaluator to record "this beat that" outside the governed precedence order.

### 5.2 Worked OVERRIDES example — CONF-008

The exact scenario the monotonicity invariant exists to prevent — capability match trying to outrank a structural contraindication — is expressed as:

```json
{
  "sourceNodeReference": "node-contraindication-structural",
  "targetNodeReference": "node-capability-resolution",
  "relationType": "OVERRIDES",
  "precedenceOrder": 1
}
```

reflecting: *"Capability Match = satisfied, Structural Contraindication = present → Resultado: NOT_AUTHORIZED. La coincidencia de capacidad nunca puede prevalecer sobre una contraindicación estructural."* The edge direction — contraindication overrides capability, never the reverse — is itself part of the auditable trace, not just a comment in this document.

## 6. Graph shape: root and terminal nodes

- `rootNodeReference` — the first stage evaluated in the Decision Precedence Model. Technical validity (schema/canonicalization checks) happens *before* a graph exists at all — if it fails, the result is a `ResolutionFailure` with no `JustificationGraph` ([IF-SG-CANON-001 §16](IF-SG-CANON-001.md#16-conformance-failure-semantics)). The first node in a graph that does get built is therefore `LIFECYCLE_VALIDATION`.
- `terminalNodeReferences` — plural, because more than one terminal node may be reachable depending on the path (e.g. a `CONTRAINDICATION_RESOLUTION` node that itself `RESULTS_IN` the final `OPERATIONAL_DISPOSITION`, alongside the `OPERATIONAL_DISPOSITION` node itself as the ultimate sink). At least one terminal node MUST be of type `OPERATIONAL_DISPOSITION`.

## 7. `constraints` — structural self-certification

Turning the mandatory properties fixed in [IF-SG-CANON-001 §11](IF-SG-CANON-001.md#11-justificationgraph-canonical-constraints) into a declared, checkable record:

```json
{
  "isDirectedAcyclicGraph": true,
  "hasUniqueNodeIdentifiers": true,
  "hasValidEdgeEndpoints": true,
  "hasAtLeastOneRoot": true,
  "hasAtLeastOneTerminalNode": true,
  "hasNoDanglingReferences": true
}
```

**Constraint:** every value in `constraints` MUST be `true` for the graph to be well-formed. A `false` value, or a graph that in fact contains a cycle regardless of what `constraints` claims, MUST cause the evaluator to abort graph assembly and emit a `ResolutionFailure` — never a `SafetyGateDecision` with an inconsistent graph attached. This is exactly [NEG-014](IF-SG-CANON-001.md#13-negative-conformance-vectors): *"Cyclic JustificationGraph → ResolutionFailure. Forbidden: Any SafetyGateDecision."*

**Repair prohibition**, stated verbatim: a graph cycle *"MUST NOT be repaired silently by: removing an edge; changing an edge type; choosing an arbitrary root; discarding a node; flattening the graph."* Detecting a cycle is a failure condition to report, not a malformed graph to quietly fix.

## 8. GraphHash computation

Per [IF-SG-CANON-001 §6.3](IF-SG-CANON-001.md#63-justificationgraph) and the hash-construction order:

```
ResolutionSnapshot
        ↓
SnapshotHash
        ↓
JustificationGraph
        ↓
GraphHash          ← this document
        ↓
SafetyGateDecision
        ↓
DecisionHash
```

**Constraints**

- `GraphHash` MUST be computed over exactly the positive material fixed in [IF-SG-CANON-001 §8.2](IF-SG-CANON-001.md#82-justificationgraphhash-material): `graphIdentity`, `graphProfileVersion`, `resolutionSnapshotReference`, `rootNodeReference`, `terminalNodeReferences`, `nodes`, `edges`, `constraints`.
- `graphHash` itself MUST NOT be included in its own hash material.
- When node or edge order is not itself normative, this schema fixes canonical ordering as: `nodes` sorted by `nodeId` (lexicographic, per the canonical string ordering already implied by [IF-SG-CANON-001 §3.3](IF-SG-CANON-001.md#33-property-ordering)); `edges` sorted first by `precedenceOrder`, then by `sourceNodeReference`. This did not need to be invented from nothing — [IF-SG-CANON-001 §3.4](IF-SG-CANON-001.md#34-arrays) already requires that any schema declaring an array as a semantic set "MUST define: the canonical sort key" — this is that definition for `nodes` and `edges`.
- `GraphHash` MUST NOT reference or include `DecisionHash` (§3).

## 9. What the graph excludes

Consistent with [IF-SG-CANON-001 §9.4 Presentation metadata](IF-SG-CANON-001.md#94-presentation-metadata), a `JustificationGraph` MUST NOT include, anywhere in `nodes` or `edges`:

- `displayText`, `localizedLabel`, `humanReadableExplanation`, `translatedExplanation`, `uiHints`;
- internal model reasoning, chain-of-thought text, or any free-form narrative of *how* the evaluator arrived at a `resolutionStatus`;
- debug traces, intermediate scratch values, or anything from [IF-SG-CANON-001 §9.3](IF-SG-CANON-001.md#93-internal-diagnostic-data).

A human-readable explanation MAY be *derived* from the graph after the fact (e.g. for a professional-facing UI), but that derived text is not part of the graph and MUST NOT affect `graphHash`.

## 10. JSON-LD: deferred, not adopted

The source conversation explicitly considered and deferred representing `JustificationGraph` as JSON-LD:

> *"No congelaría todavía el JustificationGraph como JSON-LD... introduce decisiones adicionales: contextos; identificadores IRIs; expansión y compactación; reglas de canonicalización RDF; normalización de blank nodes. Para la primera versión normativa usaría: strict canonical JSON graph representation."*

This version (0.1.0) uses plain canonical JSON as fixed in §2. JSON-LD interoperability, if needed, belongs to a future `IF-SG-INTEROP-001` profile layered on top — it MUST NOT be folded into this schema's hash material, for the same reason presentation metadata is excluded in §9.

## 11. Worked examples

### 11.1 CONF-001 — completing the AUTHORIZED / PROCEED graph

Completes the `justificationGraphReference` left as a placeholder in [IF-SG-SCHEMA-001 §15.1](IF-SG-SCHEMA-001.md#151-conf-001-authorized--proceed):

```json
{
  "graphIdentity": "d4e5f6a7-0001-4000-8000-000000000001",
  "graphProfileVersion": "1.0.0",
  "resolutionSnapshotReference": { "artifactId": "b2c3d4e5-0001-4000-8000-000000000001", "artifactType": "RESOLUTION_SNAPSHOT", "artifactVersion": "1.0.0", "contentHash": { "...": "ContentHash" } },
  "rootNodeReference": "node-lifecycle",
  "terminalNodeReferences": [ "node-disposition" ],
  "nodes": [
    { "nodeId": "node-lifecycle", "nodeType": "LIFECYCLE_VALIDATION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-evidence", "nodeType": "EVIDENCE_RESOLUTION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [ { "...": "EvidenceReference" } ], "policyReferences": [] },
    { "nodeId": "node-contraindication", "nodeType": "CONTRAINDICATION_RESOLUTION", "resolutionStatus": "NOT_APPLICABLE", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-capability", "nodeType": "CAPABILITY_RESOLUTION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-disposition", "nodeType": "OPERATIONAL_DISPOSITION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] }
  ],
  "edges": [
    { "sourceNodeReference": "node-lifecycle", "targetNodeReference": "node-evidence", "relationType": "EVALUATED_BEFORE", "precedenceOrder": 1 },
    { "sourceNodeReference": "node-evidence", "targetNodeReference": "node-contraindication", "relationType": "EVALUATED_BEFORE", "precedenceOrder": 2 },
    { "sourceNodeReference": "node-contraindication", "targetNodeReference": "node-capability", "relationType": "EVALUATED_BEFORE", "precedenceOrder": 3 },
    { "sourceNodeReference": "node-capability", "targetNodeReference": "node-disposition", "relationType": "RESULTS_IN", "precedenceOrder": 4 }
  ],
  "constraints": { "isDirectedAcyclicGraph": true, "hasUniqueNodeIdentifiers": true, "hasValidEdgeEndpoints": true, "hasAtLeastOneRoot": true, "hasAtLeastOneTerminalNode": true, "hasNoDanglingReferences": true },
  "graphHash": { "...": "ContentHash" }
}
```

### 11.2 CONF-008 — the OVERRIDES graph

Completes [IF-SG-SCHEMA-001 §15.2](IF-SG-SCHEMA-001.md#152-conf-008-not_authorized--stop-structural-block-despite-capability-match), making the monotonicity invariant an inspectable edge rather than only a rule in prose:

```json
{
  "graphIdentity": "d4e5f6a7-0008-4000-8000-000000000008",
  "graphProfileVersion": "1.0.0",
  "resolutionSnapshotReference": { "...": "IntegrityBoundArtifactReference" },
  "rootNodeReference": "node-lifecycle",
  "terminalNodeReferences": [ "node-disposition" ],
  "nodes": [
    { "nodeId": "node-lifecycle", "nodeType": "LIFECYCLE_VALIDATION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-evidence", "nodeType": "EVIDENCE_RESOLUTION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-contraindication-structural", "nodeType": "CONTRAINDICATION_RESOLUTION", "resolutionStatus": "UNSATISFIED", "reasonCodes": [ { "namespace": "IF-SG-CONTRAINDICATION", "code": "STRUCTURAL_IMMEDIATE_PHYSICAL_THREAT", "version": "1.0.0" } ], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-capability-resolution", "nodeType": "CAPABILITY_RESOLUTION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] },
    { "nodeId": "node-disposition", "nodeType": "OPERATIONAL_DISPOSITION", "resolutionStatus": "SATISFIED", "reasonCodes": [], "evidenceReferences": [], "policyReferences": [] }
  ],
  "edges": [
    { "sourceNodeReference": "node-lifecycle", "targetNodeReference": "node-evidence", "relationType": "EVALUATED_BEFORE", "precedenceOrder": 1 },
    { "sourceNodeReference": "node-evidence", "targetNodeReference": "node-contraindication-structural", "relationType": "EVALUATED_BEFORE", "precedenceOrder": 2 },
    { "sourceNodeReference": "node-contraindication-structural", "targetNodeReference": "node-capability-resolution", "relationType": "OVERRIDES", "precedenceOrder": 3 },
    { "sourceNodeReference": "node-contraindication-structural", "targetNodeReference": "node-disposition", "relationType": "RESULTS_IN", "precedenceOrder": 4 }
  ],
  "constraints": { "isDirectedAcyclicGraph": true, "hasUniqueNodeIdentifiers": true, "hasValidEdgeEndpoints": true, "hasAtLeastOneRoot": true, "hasAtLeastOneTerminalNode": true, "hasNoDanglingReferences": true },
  "graphHash": { "...": "ContentHash" }
}
```

Reading this graph alone — without any accompanying prose — already tells an auditor that the intervention was blocked *despite* a satisfied capability match, and exactly which node overrode which. That is the entire purpose of §5.2's invariant made concrete.

## 12. Forbidden representations

- `decisionReference` (or any reference to `SafetyGateDecision` / `DecisionHash`) anywhere in the graph (§3).
- `graphHash` included in its own hash material.
- An `OVERRIDES` edge not backed by a rule in the Decision Precedence Model ([IF-SG-SCHEMA-001 §9](IF-SG-SCHEMA-001.md#9-decision-precedence-model)).
- Any node or edge carrying `displayText`, `humanReadableExplanation`, or free-form model reasoning (§9).
- A graph with `constraints.isDirectedAcyclicGraph: true` that in fact contains a cycle — or any cycle "repaired" by silently dropping a node or edge instead of failing (§7).
- Fewer than one root node or fewer than one terminal node of type `OPERATIONAL_DISPOSITION`.
- Reordering `nodes` or `edges` inconsistently with the canonical sort keys fixed in §8, causing two conforming implementations to produce different `graphHash` values for the same normative content.
- Representing the graph as JSON-LD in this schema version (§10).

## 13. Exit criteria

This specification is eligible to advance from DRAFT to TESTING when:

- a JSON Schema exists that rejects any `decisionReference`-shaped field and enforces the `oneOf`/discriminator patterns already required by [IF-SG-SCHEMA-001 §2.1](IF-SG-SCHEMA-001.md#21-structural-representation) style validation;
- the worked examples in §11 both round-trip through canonicalization to a stable, reproducible `graphHash` (Level B, [IF-SG-CANON-001 §14](IF-SG-CANON-001.md#14-conformance-levels));
- CONF-001's graph (§11.1, no `OVERRIDES` edges) and CONF-008's graph (§11.2, one `OVERRIDES` edge) are both accepted, and a graph with an `OVERRIDES` edge not grounded in the Decision Precedence Model is rejected;
- a synthetically cyclic graph is rejected with `ResolutionFailure` / `CYCLIC_JUSTIFICATION_GRAPH`, matching NEG-014;
- two independent implementations produce the same `graphHash` for the same `nodes`/`edges` supplied in different input order (proving the canonical sort keys in §8 are sufficient).

## 14. Governing invariant

> Un `JustificationGraph` debe permitir reconstruir, sin preguntarle nada al evaluador que lo produjo, exactamente qué se evaluó, en qué orden, con qué evidencia, y por qué un resultado prevaleció sobre otro cuando ambos estaban presentes.
