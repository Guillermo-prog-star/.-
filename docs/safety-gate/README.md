# IF-SG — Safety Gate: serie documental de arquitectura de referencia

Esta carpeta traslada al repositorio una línea de diseño conceptual (IF-SG-*, IF-CORE-*) que se desarrolló íntegramente en una sesión externa a Claude Code, sin tocar `backend/` ni `if-frontend/`. Ningún documento de esta carpeta corresponde a código implementado hoy en el proyecto. Se guarda aquí para que deje de vivir solo en un transcript de chat y quede versionado en git.

## Qué es esto

Un Safety Gate conceptual para intervenciones dentro de Integrity Family: un modelo que separa **identidad/contexto** (IF-CORE), **catálogo normativo de intervenciones** (IF-SG-ARCH-002), **taxonomía de capacidades** (IF-SG-TAX-001), **tipos comunes y canonicalización/hashing** (IF-SG-SCHEMA-000, IF-SG-CANON-001) y **motor de resolución de decisiones auditables** (IF-SG-ARCH-005), con una suite de conformidad (IF-SG-CONF-001) que fija comportamiento antes que estructura de datos.

## Estado real de cada documento (verificado contra la conversación de origen)

| Documento | Estado declarado en la conversación | ¿Texto completo disponible? | Guardado en este repo |
|---|---|---|---|
| IF-SG-ARCH-001 — Conceptual Reference Architecture | APPROVED, congelado v1.0.0 | No — solo discutido/revisado (slides originales no pegadas) | ❌ Pendiente |
| IF-SG-ARCH-001-RATIONALE | ACCEPTED | No — solo resumen de las 5 decisiones | ❌ Pendiente |
| IF-SG-ARCH-002 — Versioned Intervention Catalog | Iteró v1.0.0-DRAFT → v1.2.0-TESTING, nunca llegó a congelarse | No — solo fragmentos JSON/prosa de cada refactorización | ❌ Pendiente |
| IF-SG-ARCH-003 — Identidad y Contexto del Participante | WITHDRAWN / SUPERSEDED por IF-CORE-001 | No | ❌ No aplica (retirado) |
| IF-CORE-001 — Identidad del Participante y Continuidad Contextual | v0.1.0 texto dado; v0.2.0 solo resumida, nunca pegada completa | Parcial (v0.1.0 sí, v0.2.0 no) | ❌ Pendiente |
| IF-SG-TAX-001 — Capability Taxonomy | v0.1.0, mencionado como generado | No — solo categorías en prosa | ❌ Pendiente |
| **IF-SG-SCHEMA-000 — Normative Common Types** | v0.1.1 DRAFT | **Sí, íntegro** | ✅ [IF-SG-SCHEMA-000.md](IF-SG-SCHEMA-000.md) |
| **IF-SG-CANON-001 — Canonicalization and Hashing** | v0.1.1 DRAFT | **Sí, íntegro** | ✅ [IF-SG-CANON-001.md](IF-SG-CANON-001.md) |
| IF-SG-CONF-001 — Normative Conformance Specification | v0.1.1-DRAFT | No — solo extracto (secciones 1-2 y matriz), no el documento de 20+ secciones que sí se completó para SCHEMA-000/CANON-001 | ❌ Pendiente |
| **IF-SG-CONF-DATA-001 — Canonical Conformance Fixtures** | Nunca se redactó en la conversación de origen — era el siguiente paso declarado | Sí — reconstruido a partir de la especificación en prosa de los 12 casos + 4 propiedades sistémicas, que sí quedó completa sin ambigüedad | ✅ [IF-SG-CONF-DATA-001.md](IF-SG-CONF-DATA-001.md) |
| **IF-SG-SCHEMA-001 — SafetyGateDecision / ResolutionFailure** | Nunca se redactó como archivo — el "árbol de decisión" quedó fijado en prosa a través de varias iteraciones (3 dimensiones, Decision Matrix, Decision Precedence Model) | Sí — reconstruido; cada tabla/enum tiene fuente literal en la conversación | ✅ [IF-SG-SCHEMA-001.md](IF-SG-SCHEMA-001.md) |
| **IF-SG-SCHEMA-002 — ResolutionSnapshot** | Nunca se redactó como archivo — campos fijados dos veces de forma consistente (boceto inicial + material de hash de CANON-001 §8.1) | Sí — reconstruido; incluye nota de evolución de nombres de campo (`evaluatorReference`→`evaluatorConformanceReference`, `resolutionParameters`→`normativeParameters`) | ✅ [IF-SG-SCHEMA-002.md](IF-SG-SCHEMA-002.md) |
| **IF-SG-SCHEMA-003 — JustificationGraph** | Nunca se redactó como archivo — campos fijados en CANON-001 §8.2/§11, estructura de nodo/arista especificada dos veces | Sí — reconstruido; documenta explícitamente el campo `decisionReference` propuesto y descartado por circularidad entre dos mensajes consecutivos de la conversación original | ✅ [IF-SG-SCHEMA-003.md](IF-SG-SCHEMA-003.md) |
| IF-SG-ARCH-005 — SafetyGateDecision Resolution Model | Solo esqueleto/estructura de capítulos | No | ❌ Pendiente |

**Regla aplicada al trasladar esto:** solo se guardan documentos de los que existe texto verbatim en la conversación de origen. Reconstruir de memoria los que solo quedaron resumidos violaría el principio rector que la propia serie se impuso (`IF-SG-SCHEMA-000 §2.2`: *"Representation fidelity — an implementation MUST NOT silently transform one valid normative value into another representation..."*). Si se quiere completar la serie, hace falta recuperar el texto íntegro de esos documentos desde la conversación original (probablemente artifacts/archivos generados allí, no solo el resumen que quedó en el chat).

## Roadmap declarado (según la última decisión tomada en la conversación de origen)

1. ~~IF-SG-CONF-001~~ → 2. ✅ **IF-SG-SCHEMA-000** → 3. ✅ **IF-SG-CANON-001** → 4. ✅ **IF-SG-CONF-DATA-001** (fixtures) → 5. ✅ **IF-SG-SCHEMA-001** (Decision/Failure) → 6. ✅ **IF-SG-SCHEMA-002** (Snapshot) → 7. ✅ **IF-SG-SCHEMA-003** (JustificationGraph) → 8. IF-SG-IFACE-001 → 9. IF-SG-EVAL-001 (Reference Evaluator) → 10. IF-SG-API-001.

Con esto, **los cuatro esquemas normativos centrales quedan completos** (SCHEMA-000/CANON-001 como sustrato, SCHEMA-001/002/003 como los tres artefactos de decisión). El siguiente paso pendiente es **IF-SG-IFACE-001** (interfaces de dominio: `SafetyGateResolver.resolve(ResolutionRequest): ResolutionResult` y los puertos de infraestructura) — y ahí hay un hueco ya señalado en [IF-SG-SCHEMA-002 §10](IF-SG-SCHEMA-002.md#10-relationship-to-resolutionrequest-out-of-scope): `ResolutionRequest` nunca quedó especificado en la conversación original, ni siquiera en prosa. Ese sí sería el primer punto de la serie donde continuar exigiría diseño nuevo, no reconstrucción fiel.

## Nota aparte: material filosófico/personal

La conversación de origen incluye, al final, una reflexión sobre neurociencia/interocepción/autorregulación aplicando metáforas del Safety Gate a la experiencia personal, con una crítica que señala cuatro sobre-extensiones de la metáfora (interocepción ≠ verdad, utilidad ≠ corrección, pausa ≠ bloqueo, coherencia ≠ integridad). Es contenido reflexivo, no arquitectura de software — no se traslada a este repo por no ser código ni documentación técnica del proyecto.
