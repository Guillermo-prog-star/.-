# Integrity Family — Guía para Claude Code

Proyecto de transformación familiar con IA adaptativa. Backend Spring Boot + Frontend Angular.
18+ meses de desarrollo. Leer esta guía antes de cualquier tarea.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.4.3 · Maven |
| Frontend | Angular 17.3 · TypeScript 5.4 · NGINX |
| Base de datos | MySQL 8.4 · Flyway (V1→V112, huecos en V61 y V88) |
| Mensajería | RabbitMQ 3 (CloudAMQP en prod) |
| IA | Claude API (Anthropic) vía `ClaudeAiService` |
| Auth | JWT (jjwt 0.12.6) + Spring Security |
| Despliegue | Backend → Railway · Frontend → Vercel |
| Local | Docker Compose (`docker-compose.yml`) |
| CI | GitHub Actions: `quality.yml` (tests + JaCoCo + SonarCloud) |

---

## Estructura del repositorio

```
if-full/
├── backend/                  # Spring Boot API (Puerto 8080)
│   ├── src/main/java/com/integrityfamily/
│   │   ├── domain/           # Entidades JPA + Repositorios (NO tocar sin migración)
│   │   ├── config/           # Spring Security, RabbitMQ, WebSocket, OpenAPI
│   │   ├── common/           # SecurityValidator, EventPublisher, FamilyEventListener
│   │   └── [44 módulos]/     # Ver mapa de módulos abajo
│   ├── src/test/             # ~199 clases de test (JUnit 5 + Mockito strict)
│   └── src/main/resources/
│       └── db/migration/     # Migraciones Flyway V1–V112 (110 archivos; faltan V61, V88)
├── if-frontend/              # Angular SPA (Puerto 4200)
├── scripts/
│   ├── backup-mysql.sh       # Backup con rotación
│   └── restore-mysql.sh      # Restore interactivo
├── docker-compose.yml        # MySQL + RabbitMQ + Backend local
├── sonar-project.properties  # SonarCloud: org=guillermo-prog-star
└── .github/workflows/
    ├── quality.yml           # CI: tests + JaCoCo + SonarCloud (en PRs)
    └── deploy-backend.yml    # Deploy a Railway (en push a main)
```

---

## Mapa de módulos backend (44 módulos)

### Núcleo familiar
| Módulo | Responsabilidad principal |
|---|---|
| `family` | CRUD de núcleos familiares, ICF score, risk_level |
| `member` | Miembros, invitaciones, perfiles, eventos RabbitMQ |
| `auth` | JWT, refresh tokens, account lock, AuditService |
| `security` | SecurityWatchdog, validaciones de acceso |

### Evaluación e ICF
| Módulo | Responsabilidad principal |
|---|---|
| `evaluation` | Evaluaciones, EvaluationScoringService, ICF calculado |
| `assessment` | Banco de preguntas, AssessmentAnswerService |
| `risk` | RiskEvaluationService, RiskSnapshotService, AlertEngine |
| `scanner` | Análisis de señales de riesgo en tiempo real |

### Transformación
| Módulo | Responsabilidad principal |
|---|---|
| `plan` | ImprovementPlan, PlanTask, PlanGenerationService (IA) |
| `checklist` | ChecklistItem, TaskEvidence, validación de evidencias |
| `bitacora` | FamilySprint, SprintMission, SprintDaily, SprintRetrospective |
| `adaptive` | AdaptivePlanService — ajusta planes según contexto |
| `weeklyplan` | WeeklyPlanService — planificación semanal |
| `milestone` | Hitos, MilestoneAwarePlanEngine |

### IA y Cognición
| Módulo | Responsabilidad principal |
|---|---|
| `ai` | ClaudeAiService, AiProviderSelector, PromptGenerator, AiInferenceService |
| `cognitive` | CopilotService, EmotionalStateTracker, ConversationSessionService |
| `context` | FamilyContextEngine — síntesis de contexto para prompts |
| `chat` | WebSocket chat, mensajes, sesiones conversacionales |

### Memoria y Herencia
| Módulo | Responsabilidad principal |
|---|---|
| `documentary` | FamilyDocumentary, DocumentaryProductionService (DRAFT→PUBLISHED) |
| `timeline` | FamilyTimelineService — línea de tiempo familiar |
| `legado` | LegacyService — legado intergeneracional |
| `lineage` | LineageService — árbol genealógico extendido |
| `dna` | FamilyDnaService — ADN cultural/emocional de la familia |
| `tree` | FamilyTreeService — árbol familiar visual |
| `lts` | LongitudinalStateService — estado histórico de la familia |

### Experiencia y Relacional
| Módulo | Responsabilidad principal |
|---|---|
| `ritual` | RitualEngineService — rituales familiares |
| `council` | FamilyCouncilService — sesiones de consejo familiar |
| `guardian` | GuardianService, GuardianBriefingService — cuidadores |
| `movie` | FamilyMovieService — película narrativa de la familia |
| `participation` | ParticipationService — gamificación y participación |
| `feedback` | FeedbackService — feedback de miembros |
| `myspace` | Espacio personal de cada miembro |

### Analytics e Inteligencia
| Módulo | Responsabilidad principal |
|---|---|
| `analytics` | AnalyticsService, AdminAnalyticsService, ConvivenceAnalytics |
| `report` / `reports` | ExcelExport, PdfExport, ExecutiveReport, AutomatedReporting |
| `twin` | DigitalTwinService — gemelo digital de la familia |
| `simulation` | CrisisSimulationService, SentinelSimulationService, TrendSimulation |
| `transformation` | TransformationStateService — estado de cambio |

### Infraestructura
| Módulo | Responsabilidad principal |
|---|---|
| `common` | SecurityValidator, EventPublisher, FamilyEventListener, excepciones |
| `config` | SecurityConfig, RabbitMQConfig, WebSocketConfig, OpenApiConfig |
| `errorprotocol` | ErrorProtocolService — manejo de crisis sistémicas |
| `admin` | AdminAnalyticsService, BackupService, BetaLauncherService |
| `trajectory` | TrajectoryService, TrajectorySuggestionService — Banco de Trayectorias de Riesgo (35 tipos, 9 macrodominios), sugerencias automáticas por señales LTS, integrado en PromptGenerator |

---

## Entidades de dominio críticas

```
families
  └── family_members (family_id FK)
  └── evaluations (family_id FK)
      └── improvement_plans (family_id + evaluation_id nullable FK)
          └── plan_tasks (plan_id + family_id FK)
              └── task_evidences (task_id NULLABLE desde V65, family_id FK)
                  └── ← documentary_id nullable FK desde V67
  └── family_sprints (family_id FK)
      └── sprint_missions / sprint_dailies / sprint_retrospectives
  └── family_documentaries (family_id FK) ← V66
  └── critical_days / risk_snapshots / ai_inferences / audit_events
```

**Migraciones estructurales recientes:**
- `V65` — `task_evidences.task_id` pasó a NULLABLE (evidencias sin tarea)
- `V66` — nueva tabla `family_documentaries`
- `V67` — `task_evidences.documentary_id` FK nullable
- `V68` — formaliza 11 columnas de `families` que solo existían vía ddl-auto=update
- `V69` — snapshot idempotente del schema completo de producción 2026-06-16 (99 tablas)
- `V70`–`V74` — alexa OAuth, chapter progress, project_documents, ICaF schema, ICaF questionnaires
- `V75` — Banco de Trayectorias de Riesgo (4 tablas + seed 35 trayectorias + 2 docs técnicos)
- `V76`–`V112` — ver el detalle en "Convenciones de migración Flyway" más abajo

---

## Convenciones de testing

**Framework:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) + AssertJ

**Patrón estándar:**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("NombreServicio — Unit Tests")
class NombreServicioTest {
    @Mock DependenciaRepository repo;
    @Mock AuditService auditService;  // ← siempre presente desde sprint estabilización
    @InjectMocks NombreServicio service;

    @Nested @DisplayName("metodo()")
    class Metodo { ... }
}
```

**Gotchas conocidos:**
- `SprintService` tiene 3 dependencias nuevas: `AuditService`, `TaskEvidenceRepository`, `PlanTaskRepository` — todos deben mockearse
- `TaskEvidenceService` requiere mock de `AuditService`
- `DocumentarySourceType` enum: valores son `MISSION`, `SPONTANEOUS`, `MEMORY`, `SPRINT_CLOSURE`, `PILLAR_CLOSURE` (NO `MISSION_BASED`)
- `FamilyDocumentaryDTO` es clase `@Data` de Lombok, usar `getTitle()` (NO record con `title()`)
- `FamilyIcfRecalculatedEvent` tiene exactamente 10 campos (sin `convivencia`)
- Tests se ejecutan con perfil `test` — Mockito strict stubs: declarar solo los mocks que se usan

**Comandos de test:**
```bash
# Desde /backend
mvn test                          # todos los tests
mvn test -Dtest=SprintServiceTest # test específico
mvn verify -P ci                  # tests + JaCoCo quality gate (igual que CI); rompe si cobertura de líneas < 65%

# Test de integración E2E (requiere Docker integrity-db en puerto 3307)
mvn test -Dtest=FamilyLifecycleIntegrationTest
```

**Test E2E — `FamilyLifecycleIntegrationTest`:**
- Perfil: `integration-test` (MySQL real, RabbitMQ mockeado, ddl-auto=update)
- BD: `integrity_family_e2e_test` — se crea y destruye automáticamente
- Flujo: Familia → Evaluación ICF → Plan → Sprint → Evidencia → Documental (6 pasos)
- Prerrequisito: `docker compose up -d db` (MySQL en localhost:3307)

---

## RabbitMQ — módulos que publican/consumen

Producen eventos: `ai`, `analytics`, `checklist`, `common`, `evaluation`, `member`, `plan`

Exchanges/queues: en `config/RabbitMQConfig.java`

En tests: mockear `RabbitTemplate` — los errores de broker NO deben propagarse al caller (patrón try/catch interno validado en `TaskEvidenceServiceTest`).

---

## Seguridad — flujo de validación

`SecurityValidator.validateFamilyOwnership(familyId, principal)`:
1. Principal null → `AccessDeniedException("No autenticado")`
2. `ROLE_ADMIN` → acceso inmediato (bypass)
3. Email == `family.createdBy.email` → acceso como creador
4. Email == `familyMember.email` && `member.active` && `member.familyId == familyId` → acceso como miembro
5. Cualquier otro caso → `AccessDeniedException`

---

## Infraestructura local

```bash
# Levantar MySQL + RabbitMQ + Backend
docker compose up -d

# Solo BD y rabbit (para desarrollo local del backend)
docker compose up -d db rabbitmq

# Backup
./scripts/backup-mysql.sh --compress

# Restore
./scripts/restore-mysql.sh backups/if_backup_YYYYMMDD_HHmmss.sql.gz
```

**Contenedores:** `integrity-db` (MySQL:3306), `integrity-rabbitmq` (5672/15672), `integrity-backend` (8080)

---

## CI/CD

| Evento | Workflow | Qué hace |
|---|---|---|
| Push/PR → `main` o `principal` con cambios en `backend/` | `quality.yml` | Tests + JaCoCo (umbral **65% líneas**, `jacoco:check` con `haltOnFailure`; ver `<jacoco.line.coverage.min>` en `pom.xml`) + SonarCloud |
| Push → `main` con cambios en `backend/` | `deploy-backend.yml` | Deploy a Railway |
| Push → `main` con cambios en `if-frontend/` | `deploy-frontend.yml` | Deploy a Vercel |

**Secret requerido para SonarCloud:** `SONAR_TOKEN` en GitHub → Settings → Secrets.

---

## Backup — estado verificado

- Último backup probado: `2026-06-16` — `if_backup_20260616_140811.sql.gz` (9.6 MB)
- Restore verificado con `COUNT(*)` exacto en 8 tablas críticas: 100% coincidencia
- Método: restore en BD temporal `integrity_family_test_restore`, comparación, eliminación
- Próxima verificación recomendada: mensual
Dashboard: https://sonarcloud.io/project/overview?id=Guillermo-prog-star_if-full

---

## Convenciones de migración Flyway

- Nombrar: `V{N}__{descripcion_snake_case}.sql` — descripción en inglés
- NUNCA modificar una migración ya ejecutada (rompe el checksum de Flyway)
- Siempre agregar `NULL` explícito o `DEFAULT` en columnas nuevas para no bloquear registros existentes
- Antes de agregar FK: verificar que la tabla referenciada ya existe (respetar orden de migraciones)
- FK nullable (`NULL ok`) cuando la relación es opcional (ej: V65, V67)
- `ADD COLUMN IF NOT EXISTS` es MariaDB — en MySQL 8.x usar procedure con `information_schema` (ver V68)
- V69 es un snapshot idempotente de producción 2026-06-16 con `CREATE TABLE IF NOT EXISTS`
- V83–V88 — banco NEURO_AWARENESS (opciones dinámicas, pilotos, banco maestro 60 preguntas)
- V89–V93 — SCENARIO_V1_2 (micro-simulaciones): `questions.phase_prompt` y `question_options.rubric_level`
- V94–V95 — SCENARIO_V1_2 batches 3–4 (más escenarios del Contrato Metodológico Gold Standard)
- V96 — Modelo de Determinantes Transformacionales (borrador/hipótesis): tablas `transformational_determinants` (4 ejes: PATRIMONIO, ENTORNO, DINAMICA, ECOSISTEMA_APOYO) y `risk_trajectory_determinants` (bridge N:N, `role` PRIMARY/SECONDARY) sobre el Banco de Trayectorias (V75). No reemplaza `macrodomain`; el mapeo se hizo por trayectoria individual porque a nivel de macrodominio solo 3 de 9 encajaban 1:1 (los otros son crosscutting, ej. SALUD_MENTAL/ADICCIONES). Pendiente de validación empírica antes de usarse como verdad del modelo.
- V97 — Criterio de protocolo de seguridad (4 puertas) sobre el Banco de Trayectorias (V75): agrega `risk_trajectories.requires_safety_protocol` (7 trayectorias confirmadas: violencia intrafamiliar, delincuencia juvenil, ideación suicida, autolesiones, trastorno de alimentación, consumo problemático de alcohol/otras sustancias en adulto) y `contextual_criticality_rule` (4 condicionales que dependen de edad/dependencia/tipo de contenido: embarazo adolescente, abandono y dependencia de adulto mayor, ciberacoso). También renombra `IDENTIDAD_GENERO` → `CONFLICTO_FAMILIAR_POR_DIVERSIDAD` (el objeto de riesgo es el rechazo familiar, no la identidad de la persona) y corrige `DOC-TRAY-002` que solo documentaba 4 de las 7 trayectorias críticas.
- V98 — `safety_protocol_activations`: activación estructurada del protocolo de seguridad (responsable real vía FK a `family_members`, acción inicial, `follow_up_date` obligatoria, `support_assignment_id` opcional hacia `family_support_assignments`). Deliberadamente separado de `family_error_protocols` (ritual Detectar-Sentir-Comprender para misiones fallidas, módulo `errorprotocol`) — un riesgo vital/legal va directo a acción, no a un ritual emocional de 7 pasos. Activación siempre manual (requiere confirmación humana), nunca automática. Endpoints en `TrajectoryController`: `POST/GET /api/trajectories/family/{id}/safety-protocol`, `POST .../safety-protocol/{activationId}/close`.
- V99 — Corrige `PT-CU-01` (CU-10 Gestión de Crisis), que describía un flujo aspiracional nunca implementado (`ErrorProtocolService activa protocolo` automáticamente ante riesgo CRITICAL). Ahora refleja el mecanismo real de V98 y agrega CU-11 distinguiendo `FamilyErrorProtocol` (misiones fallidas) de `SafetyProtocolActivation` (crisis). También actualiza `DOC-TRAY-002` (v1.2) para referenciar el endpoint real en vez de una instrucción vaga.
- V100–V101 — Corrigen `DOC-TRAY-001`/`DOC-TRAY-002` (sembrados directo por SQL en V75, sin pasar por el enum Java): `category` tenía valores `TECHNICAL`/`GUIDE` que no existen en `DocumentCategory` (solo `PROJECT, RESEARCH, FAMILY, AI, DEVELOPMENT`), lo que hacía fallar `ProjectDocumentRepository.findAll()` — y por ende `DocumentationDataInitializerPart2` — en cada arranque (silenciado por un try/catch amplio, sin romper la app pero bloqueando la carga de documentos complementarios nuevos). Se remapearon a `PROJECT`. Además tenían `status='PUBLISHED'` en vez de `'ACTIVE'` (el único valor que filtra `DocumentationService.listAll()`), por lo que nunca aparecían en el Centro de Documentación pese a ser consultables por código directo — corregido también.
- V102 — `family_action_executions` (Family Action Engine, IFRM-D Hito 5): registro de ejecuciones de comandos semánticos del Hogar Digital, para idempotencia (evita reejecutar la misma acción si el cliente reintenta con el mismo Idempotency-Key) y como rastro auditable mínimo.
- V103 — Fase 0 del programa de interoperabilidad con el ecosistema de salud (ver sección "Interoperabilidad — Ministerio de Salud" más abajo): agrega `family_members.document_type`/`document_number` (nullable, único cuando ambos están presentes) — ancla de identidad necesaria para mapear un miembro a FHIR `Patient.identifier`/un MPI nacional. Antes de esta migración no existía ningún campo de identificación formal en el dominio.
- V104 — Fase 2 del programa de interoperabilidad: tabla `consents` (módulo nuevo `consent`), consentimiento formal separado de los flags `consented_by_email`/`consented_at` de `ecosystem`/`support` — cubre el caso de compartir con una institución externa (Ministerio, IPS) que no es un participante del ecosistema ni un miembro de la red de apoyo. Cada grant/revoke genera `AuditEvent` (`CONSENT_GRANTED`/`CONSENT_REVOKED`, nuevos en `AuditEventType`).
- V105 — Streaks de sostenimiento en PLENO por dimensión ICF (ADR-003, hipótesis): agrega 4 columnas a `family_longitudinal_state` (`emociones_pleno_streak`, `comunicacion_pleno_streak`, `habitos_pleno_streak`, `tiempos_pleno_streak`). Identidad Familiar / Patrimonio Automático Familiar (PAF) no se autoevalúa ni es un sexto nivel de la Ruta de Conciencia — se infiere de sostener `dimScore >= 90` durante `>= 3` ciclos consecutivos, mismo patrón que `consecutiveImprovements`/`consecutiveDeteriorations`. Pendiente de validación empírica (Regla V1.1.1, ver V106).
- V106 — `hypothesis_evidence` (ADR-004): tabla append-only, independiente del estado operacional, para registrar observaciones primarias que respaldan cualquier hipótesis del sistema (PAF, Determinantes Transformacionales, futuras). Generaliza la Regla V1.1.1 (antes acotada a `scenario_bank`/`scenario_validation_log`) como principio transversal: ningún estado operacional (mutable, sobrescribible) constituye evidencia — la evidencia vive aparte, versionada por hipótesis e instrumento. Deliberadamente sin bounded context `research`, roles ni Warehouse todavía — se posponen hasta que exista un segundo consumidor real más allá de PAF (ADR-005, pendiente).
- V107 — `professional_follow_up_drafts` (ADR-006): traslada la generación del "resumen clínico" del Panel Profesional (antes plantilla Angular sin registro ni auditoría) a una entidad versionada en backend. `sourceSnapshot` congela los valores de `FamilyDataView` vistos al generar (estado operacional mutable).
- V108 — corrige `professional_follow_up_drafts.assignment_id` (V107): no debía tener FK a `family_support_assignments` — `assignmentId` es polimórfico (`FamilySupportAssignment` **o** `FamilyEcosystemLink`, espacios de ID separados, ver `SupportNetworkService.getDataView()`).
- V109 — mismo fix que V108 sobre `support_professional_notes.assignment_id` (V77): `addNote()` se extiende para aceptar también profesionales conectados vía `FamilyEcosystemLink` (verificado en vivo 2026-07-18).
- V110 — `daily_vitality_log` (ADR-009, Fase 4 base biológica): única fase del método externo de 8 sin cobertura digital. Todos los campos numéricos NULL-ables (registro parcial por día).
- V111 — `plan_acceptance_status` (ADR-010, Fase 0b): columnas de aceptación sobre `plans` (hoy `ImprovementPlan` se activa sin confirmación de la familia). No tabla nueva: la invariante "1 plan activo por familia" ya garantiza que no hay historial.
- V112 — perspectivas múltiples sobre un mismo evento (H2, ADR-012): agrega autor a `journal_entries` (solo tenía `family_id`) y formaliza `critical_days.member_id` — existía en producción solo vía `ddl-auto=update`, ninguna migración lo declaraba sobre un schema Flyway puro.
- Última migración: **V112**. Próximo número disponible: **V113** (verificar con `ls src/main/resources/db/migration/ | sort -V | tail -1` antes de crear una nueva).

---

## Git

**Rama activa:** `principal` (sincronizada con `origin/principal`)
**Rama de producción:** `main`
**Autor:** William Lopez / Guillermo-prog-star

---

## Contexto de negocio

Integrity Family es una plataforma de acompañamiento familiar que:
- Calcula el **ICF (Índice de Cohesión Familiar)** — 0–100, 4 dimensiones: emociones, comunicación, hábitos, tiempos
- Genera **planes de mejora** personalizados con IA (Claude)
- Conduce **sprints familiares** de 7–21 días con misiones y dailies
- Detecta **días críticos / crisis** y activa protocolos de resiliencia
- Construye **documentales familiares** (fuente: misiones, eventos espontáneos, memorias)
- Mantiene un **gemelo digital** de la familia para simulación y predicción
- Guarda el **linaje, legado y ADN cultural** de cada familia

**Las cuatro capas del proyecto** (computacional, metodológica, matemática, antropológica) están documentadas en [`docs/vision.md`](docs/vision.md) — incluye el Principio de Construcción Compartida de Significado Familiar (CCSF) y las definiciones fundacionales de familia, hogar, conversación familiar, cuidado, amor como práctica y legado. Es un marco conceptual propuesto, no una tesis demostrada — sujeto a la Regla V1.1.1 de evolución basada en evidencia.

---

## Ruta de Conciencia Familiar — escala de respuesta oficial

Componente metodológico central de Integrity Family. Reemplaza las escalas de frecuencia ("Nunca/A veces/Siempre") por una escala de **nivel de conciencia**: no mide cuántas veces ocurre algo, sino qué tan consciente está la familia de esa realidad.

**Fuente única de verdad:** [`rutaConcienciaDomain.ts`](if-frontend/src/domain/constants/rutaConcienciaDomain.ts) → `RUTA_CONCIENCIA_SCALE`. Consumida por `getCustomOptions()` en [`evaluation.component.ts`](if-frontend/src/app/features/evaluation/evaluation.component.ts).

| Nivel interno (`state`) | Respuesta visible para la familia |
|---|---|
| `INCONSCIENTE` | Aún no logro reconocer esta realidad en nuestra familia. |
| `REACTIVO` | Empiezo a darme cuenta, pero normalmente cuando la situación ya pasó. |
| `CONSCIENTE` | Reconozco esta realidad cuando ocurre. |
| `INTENCIONAL` | Cuando la reconozco, procuro actuar para fortalecerla o transformarla. |
| `PLENO` | Esta forma de vivir ya hace parte natural de nuestra familia. |

**Reglas:**
- Es **una sola escala genérica** para las 4 dimensiones del ICF (emociones, comunicación, hábitos, tiempos) y para cualquier escenario evaluado — el texto de la pregunta ya aporta el contexto específico, la respuesta solo describe el estado de conciencia.
- El `state` interno (`INCONSCIENTE`…`PLENO`) **nunca se expone al usuario** — el frontend solo renderiza `text` (ver patrón ya usado en el modo `NEURO_AWARENESS`, que oculta `label` deliberadamente).
- El modelo `NEURO_AWARENESS`/`TRAJECTORY` (Señal Corporal → Conciencia → Acción, mismo archivo `evaluation.component.ts`) es un modelo epistemológico distinto y **no** se unificó con esta escala — usa sus propias 5 opciones centradas en la señal corporal.
- Antes de este refactor existían 5 variantes hardcodeadas casi idénticas (una por dimensión + `PRESENCE_SCALE` para tiempos + fallback). Se consolidaron en una sola constante; no se debe volver a bifurcar por dimensión.

---

## Interoperabilidad — Ministerio de Salud y Protección Social

Programa en curso para que Integrity Family sea interoperable con el ecosistema de salud colombiano (FHIR, SISPRO, HCE) sin acoplar el dominio propio (ICaF, Sprint Familiar, Trayectorias, etc.) a ningún estándar externo. Se ejecuta por fases; el dominio nunca conoce FHIR directamente — solo lo conoce la capa `interop`.

**Fases:**
1. **Fase 0** ✅ — Ancla de identidad: `family_members.document_type`/`document_number` (V103). Sin esto no había forma de mapear una persona a `Patient.identifier`/MPI.
2. **Fase 1** ✅ — Modelo Canónico (`backend/src/main/java/com/integrityfamily/interop/canonical/`): POJOs (`CanonicalFamilyRecord`, `Person`, `Household`, `Observation`, `Assessment`, `Risk`, `Intervention`, `Goal`, `Outcome`, `ProfessionalNote`, `Evidence`, `Consent`, `CanonicalIdentifier`) sin dependencia de FHIR ni persistencia propia — es solo la forma intermedia que usarán los mappers de fases posteriores.
3. **Fase 2** ✅ — Consentimiento real (módulo `consent`, tabla `consents`, V104): entidad propia con propósito (`ConsentPurpose`: ECOSYSTEM_SHARING, SUPPORT_NETWORK_SHARING, HEALTH_INTEROPERABILITY, RESEARCH), alcance (`scope`, texto libre — formalizar en Fase 5), receptor (`granteeReference`) y revocación con auditoría (`AuditEventType.CONSENT_GRANTED`/`CONSENT_REVOKED`). Deliberadamente separado de los flags `consentedByEmail`/`consentedAt` de `ecosystem`/`support` (esos siguen intactos) porque cubre un caso que esos no cubren: compartir con una institución externa que no es un participante del ecosistema ni un miembro de la red de apoyo. Endpoints en `ConsentController`: `POST/GET /api/families/{id}/consents`, `POST .../consents/{id}/revoke`, `GET .../consents/active`.
4. **Fase 3** ✅ — Mappers `Integrity Model → Canonical Model` (`backend/src/main/java/com/integrityfamily/interop/mapper/`): `PersonMapper`, `HouseholdMapper`, `AssessmentMapper` (Evaluation + dimension scores → Observations), `RiskMapper` (FamilyRiskTrajectory + banco → Risk), `ConsentMapper`. Son funciones puras sin I/O; `CanonicalFamilyRecordAssembler` (`interop.service`) es el único punto que toca repositorios JPA para ensamblar el agregado completo de una familia. Expuesto de solo lectura en `GET /api/families/{id}/interop/canonical-record`. Deliberadamente sin mapear aún: interventions/outcomes/professionalNotes/evidences (requieren entrar a plan/checklist/support — fuera de alcance de esta fase).
5. **Fase 4** ✅ — Dependencia `ca.uhn.hapi.fhir:hapi-fhir-structures-r4:8.10.0` (solo modelo R4 + parser JSON, no servidor FHIR completo) + adapter real Canonical → FHIR (`backend/src/main/java/com/integrityfamily/interop/fhir/`): `PatientFhirMapper` (Person), `GroupFhirMapper` (Household — FHIR no tiene "Family"; municipio/departamento/país van como extensiones propias porque Group no tiene `address` en R4), `ObservationFhirMapper` (con `FhirReferences` para resolver subject Group/Patient según el prefijo del canonicalId). `FhirBundleAssembler` arma un `Bundle` tipo COLLECTION con los 3 recursos piloto; `FhirSerializationService` lo serializa vía `FhirContext` (bean singleton en `FhirConfig`, costoso de crear). Expuesto en `GET /api/families/{id}/interop/fhir-bundle` (`application/fhir+json`). Los códigos de Observation.code y los system de Patient.identifier usan namespaces propios de Integrity (`https://integrityfamily.com/fhir/...`) a falta de OID/URI oficial del Ministerio — reemplazar ahí cuando exista, sin tocar el resto del pipeline.
6. **Fase 5** ✅ — `interop.terminology.TerminologyService` (Concept Map código Integrity → SNOMED/LOINC/CIE-10): **deliberadamente vacío**, no poblado con códigos inventados — mapear terminología clínica real requiere validación de un profesional de codificación clínica, y un código mal mapeado en una trayectoria con `requires_safety_protocol` (V97: violencia intrafamiliar, ideación suicida, autolesiones...) tiene consecuencias reales. `register()`/`lookup()` ya están listos: `ObservationFhirMapper` recibe el `ConceptMapping` resuelto y agrega una segunda `Coding` cuando existe, sin reemplazar nunca la de Integrity. Además, wrapper FHIR sobre `AuditService`: `AuditEventFhirMapper` (`domain.AuditEvent` → FHIR `AuditEvent`, esto sí puramente mecánico) y `AuditFhirTrailService`, que resuelve los emails relevantes de una familia (dueño + miembros + el email sintético `family_<id>@integrityfamily.com` que usan los eventos de sistema) reutilizando `AuditEventRepository.findByActorEmailInOrderByOccurredAtDesc` — `AuditEvent` no tiene `family_id`, no se agregó columna nueva para esto. Expuesto en `GET /api/families/{id}/interop/fhir-audit-trail`.

Con esto el programa de interoperabilidad (Fases 0–5) queda funcionalmente completo: identidad, modelo canónico, consentimiento, mappers, adapter FHIR y auditoría. Lo único pendiente de terceros (no de código) es que un profesional de codificación clínica valide y puebla el Concept Map real, y que aparezca un consumidor concreto (SISPRO, una IPS) que defina el perfil FHIR exacto a validar contra.

No se recomienda construir el API Gateway/OAuth2 completo antes de tener un consumidor real (una IPS, SISPRO) del otro lado.
