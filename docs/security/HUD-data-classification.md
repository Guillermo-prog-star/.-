# IFA-HUD Data Classification & Governance

Estado real de la clasificación de datos y control de acceso en el IFA-HUD, verificado contra el código (2026-07). Reemplaza una versión anterior de este documento que describía un modelo de 3 niveles con enforcement en runtime (`HudEvidencePolicyGate`, clasificaciones `SHADOW_ONLY`/`RESEARCH_ONLY`/`RESTRICTED`, telemetría `HUD_RESOURCE_CONCEALED`) que nunca llegó a cablearse — la clase vivía sin ningún llamador real y fue eliminada.

## Lo que sí existe y está cableado

1. **Separación por endpoint/rol** (`HudAuthorizationPolicy`, `hud.permissions`): decide quién puede llamar `GET /hud/family` vs `GET /hud/professional` según `ViewerRole` (`ADULT_MEMBER`, `YOUNG_MEMBER`, `SUPPORT_PERSON`) o permisos explícitos (`VIEW_FAMILY_HUD`, `VIEW_PROFESSIONAL_HUD`).
2. **Separación por módulo** (`HudModulePolicy`, `hud.policy`): fija qué bloques de contenido puede devolver cada tipo de HUD (p. ej. `NOTES`/`INTERVENTIONS`/`ASSESSMENT` solo en `PROFESSIONAL`).
3. **Evidencia narrativa de la familia** (`EvidencePolicy`, `EvidencePolicyGate`, `dto.home` / `familyhome.policy`): único mecanismo real de "clasificación de contenido" hoy. Tiene **un solo valor posible: `FAMILY_APPROVED`** — el constructor de `NarrativeProvenance` rechaza cualquier otro valor. `EvidencePolicyGate.isAllowed()` es deny-by-default: solo dejar pasar contenido `FAMILY_APPROVED`.

## Lo que no existe todavía

No hay ningún dato en el sistema clasificado como clínico-restringido o de investigación. La fuente actual de narrativas (`FamilyNarrativeQueryPortAdapter`) fija `"FAMILY_APPROVED"` como string literal para todo lo que produce — el comentario del propio archivo explica por qué: `FamilyDocumentary` (la entidad real) no persiste todavía ningún metadato de procedencia/clasificación. El panel profesional (`ProfessionalHudProjectionResolver`) tampoco filtra nada por clasificación: sus notas clínicas e intervenciones son literales fijos, no contenido clasificado dinámicamente.

Antes de construir un tercer nivel (investigación) o un filtrado real Tier 2, el primer paso es que algún módulo (`documentary`, `assessment`, `ai`) empiece a persistir una clasificación real. Sin esa fuente de datos, cualquier gate adicional sería, otra vez, código sin nada que proteger.
