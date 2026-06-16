# Integrity Family — Guía para Claude Code

Proyecto de transformación familiar con IA adaptativa. Backend Spring Boot + Frontend Angular.
18+ meses de desarrollo. Leer esta guía antes de cualquier tarea.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.4.3 · Maven |
| Frontend | Angular 18 · TypeScript · NGINX |
| Base de datos | MySQL 8.4 · Flyway (V1→V67) |
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
│   ├── src/test/             # ~120 clases de test (JUnit 5 + Mockito strict)
│   └── src/main/resources/
│       └── db/migration/     # Migraciones Flyway V1–V67
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
mvn verify -P ci                  # tests + JaCoCo quality gate (igual que CI)
```

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
| Push/PR → `main` o `principal` con cambios en `backend/` | `quality.yml` | Tests + JaCoCo (umbral 40% líneas) + SonarCloud |
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
- Próximo número disponible: **V68**

---

## Git

**Rama activa:** `principal` (40+ commits adelante de `origin/principal`)
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
