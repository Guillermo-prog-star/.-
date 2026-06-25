package com.integrityfamily.documentation.initializer;

import com.integrityfamily.documentation.domain.DocumentCategory;
import com.integrityfamily.documentation.domain.ProjectDocument;
import com.integrityfamily.documentation.repository.ProjectDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DocumentationDataInitializer implements ApplicationRunner {

    private final ProjectDocumentRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (repository.count() > 0) return;
        } catch (Exception e) {
            log.warn("[DOCS] Tabla project_documents no disponible aún, se omite carga inicial: {}", e.getMessage());
            return;
        }

        log.info("[DOCS] Cargando documentos base del Centro de Documentación...");

        List<ProjectDocument> docs = List.of(

            new ProjectDocument(
                "PT-ERS-01",
                "Especificación de Requisitos del Sistema (ERS)",
                DocumentCategory.PROJECT,
                """
                # ERS — Integrity Family v1.2

                ## 1. Introducción
                Integrity Family es una plataforma de acompañamiento familiar con IA que calcula el ICF (Índice de Cohesión Familiar), genera planes de mejora personalizados y conduce sprints familiares.

                ## 2. Objetivos
                - Medir la cohesión familiar en 4 dimensiones: emociones, comunicación, hábitos y tiempos compartidos.
                - Generar planes de mejora personalizados usando IA (Claude).
                - Facilitar sprints familiares de 7–21 días con misiones y dailies.
                - Detectar días críticos y activar protocolos de resiliencia.
                - Construir documentales y legado familiar.

                ## 3. Requisitos Funcionales
                - RF-01: Registro y autenticación de familias (JWT).
                - RF-02: Evaluación ICF con 4 dimensiones (0–100 por dimensión).
                - RF-03: Generación automática de plan de mejora con IA.
                - RF-04: Gestión de sprints familiares.
                - RF-05: Sistema de checklist y evidencias fotográficas.
                - RF-06: Módulo de documental familiar.
                - RF-07: Dashboard con histórico ICF y tendencias.
                - RF-08: Modo voz con integración Alexa/Whisper.
                - RF-09: Chat con Copilot IA en tiempo real (WebSocket).
                - RF-10: Exportación de reportes en PDF y Excel.

                ## 4. Requisitos No Funcionales
                - RNF-01: Tiempo de respuesta < 2s para consultas sin IA.
                - RNF-02: Respuesta IA < 10s en condiciones normales.
                - RNF-03: Disponibilidad 99.5% en producción.
                - RNF-04: Datos cifrados en tránsito (HTTPS) y en reposo.
                - RNF-05: Compatibilidad con navegadores modernos (Angular 18+).
                """,
                "Requisitos funcionales y no funcionales del sistema Integrity Family v1.2",
                "1.2",
                "requisitos,ERS,funcional,no-funcional,ICF,sprint,plan"
            ),

            new ProjectDocument(
                "PT-ARQ-01",
                "Arquitectura del Sistema",
                DocumentCategory.PROJECT,
                """
                # Arquitectura — Integrity Family

                ## Stack Tecnológico
                | Capa | Tecnología |
                |---|---|
                | Backend | Java 17 · Spring Boot 3.4.3 · Maven |
                | Frontend | Angular 18 · TypeScript · NGINX |
                | Base de datos | MySQL 8.4 · Flyway (migraciones V1→V72) |
                | Mensajería | RabbitMQ 3 (CloudAMQP en producción) |
                | IA | Claude API (Anthropic) · Gemini · DeepSeek |
                | Auth | JWT (jjwt 0.12.6) + Spring Security |
                | Despliegue | Backend → Railway · Frontend → Vercel |
                | Local | Docker Compose |
                | CI | GitHub Actions (quality.yml + deploy) |

                ## Módulos Backend (44 módulos)
                - **Núcleo**: family, member, auth, security
                - **Evaluación e ICF**: evaluation, assessment, risk, scanner
                - **Transformación**: plan, checklist, bitacora, adaptive, weeklyplan, milestone
                - **IA y Cognición**: ai, cognitive, context, chat
                - **Memoria y Herencia**: documentary, timeline, legado, lineage, dna, tree, lts
                - **Experiencia**: ritual, council, guardian, movie, participation, feedback, myspace
                - **Analytics**: analytics, report, reports, twin, simulation, transformation
                - **Infraestructura**: common, config, errorprotocol, admin

                ## Flujo de Datos Principal
                ```
                Angular → Spring Boot API → MySQL
                                         → RabbitMQ → Consumers
                                         → Claude API → Respuesta IA
                ```

                ## Despliegue en Producción
                - Backend: Railway (auto-deploy desde main)
                - Frontend: Vercel (auto-deploy desde main)
                - BD: MySQL gestionado por Railway
                - RabbitMQ: CloudAMQP
                """,
                "Arquitectura técnica completa: stack, módulos, flujo de datos y despliegue",
                "2.0",
                "arquitectura,spring-boot,angular,mysql,rabbitmq,railway,vercel,docker"
            ),

            new ProjectDocument(
                "PT-ICF-01",
                "Marco Conceptual — ICF (Índice de Cohesión Familiar)",
                DocumentCategory.PROJECT,
                """
                # Marco Conceptual — ICF

                ## Definición
                El ICF (Índice de Cohesión Familiar) es una métrica compuesta que mide la salud integral de una familia en una escala de 0 a 100.

                ## Las 4 Dimensiones
                1. **Emociones** — Capacidad de reconocer, expresar y regular emociones en el núcleo familiar.
                2. **Comunicación** — Calidad de los intercambios verbales y no verbales entre miembros.
                3. **Hábitos** — Rutinas saludables compartidas (alimentación, sueño, actividad física).
                4. **Tiempos Compartidos** — Frecuencia y calidad del tiempo familiar sin dispositivos.

                ## Cálculo
                - Cada dimensión puntúa de 0 a 100 según las respuestas de la evaluación.
                - El ICF total es el promedio ponderado de las 4 dimensiones.
                - Niveles: Crítico (0–39), En Riesgo (40–59), En Desarrollo (60–74), Fuerte (75–89), Excepcional (90–100).

                ## Risk Level
                Basado en el ICF se asigna un nivel de riesgo:
                - CRITICAL: ICF < 40
                - HIGH: ICF 40–59
                - MEDIUM: ICF 60–74
                - LOW: ICF 75–89
                - NONE: ICF 90+

                ## Recalculación
                El ICF se recalcula automáticamente al completar una evaluación o al registrar evidencias significativas.
                """,
                "Definición y metodología del ICF — métrica central de Integrity Family",
                "1.0",
                "ICF,cohesión,dimensiones,emociones,comunicación,hábitos,tiempos,riesgo"
            ),

            new ProjectDocument(
                "PT-API-01",
                "Documentación API REST",
                DocumentCategory.PROJECT,
                """
                # API REST — Integrity Family

                ## Base URL
                - Producción: `https://api.integrityfamily.app`
                - Local: `http://localhost:8080`

                ## Autenticación
                Todos los endpoints requieren JWT en el header:
                ```
                Authorization: Bearer {token}
                ```

                ## Endpoints Principales

                ### Auth
                - POST /api/auth/register — Registro de familia
                - POST /api/auth/login — Login (retorna JWT + refresh token)
                - POST /api/auth/refresh — Renovar token

                ### Familia
                - GET  /api/families/{id} — Obtener familia
                - PUT  /api/families/{id} — Actualizar datos familiares

                ### Evaluación ICF
                - POST /api/evaluations — Crear evaluación
                - GET  /api/evaluations/family/{familyId} — Historial de evaluaciones
                - GET  /api/evaluations/{id}/result — Resultado con ICF calculado

                ### Plan de Mejora
                - POST /api/plans/generate — Generar plan con IA
                - GET  /api/plans/family/{familyId} — Planes de la familia
                - PUT  /api/plans/{id}/tasks/{taskId}/complete — Completar tarea

                ### Sprint Familiar
                - POST /api/sprints — Crear sprint
                - GET  /api/sprints/family/{familyId}/active — Sprint activo
                - POST /api/sprints/{id}/daily — Registrar daily

                ### Documental
                - POST /api/documentaries — Crear documental
                - GET  /api/documentaries/family/{familyId} — Documentales de la familia

                ### Chat IA (WebSocket)
                - STOMP ws://localhost:8080/ws
                - Subscribe: /topic/family/{familyId}
                - Send: /app/chat.send

                ### Documentación
                - GET  /api/documentation — Listar todos los documentos
                - GET  /api/documentation/category/{category} — Por categoría
                - GET  /api/documentation/{code} — Documento específico
                - GET  /api/documentation/search?q={query} — Búsqueda
                - POST /api/documentation/query — Consulta con IA
                """,
                "Referencia completa de la API REST de Integrity Family",
                "1.5",
                "API,REST,endpoints,JWT,autenticación,evaluación,plan,sprint,chat"
            ),

            new ProjectDocument(
                "PT-BD-01",
                "Modelo de Datos",
                DocumentCategory.PROJECT,
                """
                # Modelo de Datos — Integrity Family

                ## Jerarquía Principal
                ```
                families
                  └── family_members (family_id FK)
                  └── evaluations (family_id FK)
                      └── improvement_plans (family_id + evaluation_id FK)
                          └── plan_tasks (plan_id + family_id FK)
                              └── task_evidences (task_id NULLABLE desde V65)
                                  └── documentary_id nullable FK desde V67
                  └── family_sprints (family_id FK)
                      └── sprint_missions / sprint_dailies / sprint_retrospectives
                  └── family_documentaries (family_id FK) ← V66
                  └── critical_days / risk_snapshots / ai_inferences / audit_events
                ```

                ## Tabla Principal: families
                - id, name, created_by (email), icf_score (0–100), risk_level
                - created_at, updated_at

                ## Migraciones Flyway
                Actualmente en V72. Convenciones:
                - NUNCA modificar una migración ejecutada (rompe checksum).
                - Columnas nuevas: siempre con DEFAULT o NULL.
                - FK nullable cuando la relación es opcional (V65, V67).
                - Próximo número disponible: V73.

                ## Hitos Estructurales
                - V65: task_evidences.task_id → NULLABLE
                - V66: tabla family_documentaries
                - V67: task_evidences.documentary_id FK nullable
                - V68: formaliza columnas en families
                - V69: snapshot idempotente producción 2026-06-16 (99 tablas)
                - V70: tablas Alexa OAuth
                - V71: family_chapter_progress
                - V72: project_documents (Centro de Documentación)
                """,
                "Modelo entidad-relación, jerarquía de tablas y convenciones de migración Flyway",
                "1.3",
                "base-de-datos,modelo,flyway,migraciones,tablas,entidades,MySQL"
            ),

            new ProjectDocument(
                "PT-IA-01",
                "Arquitectura IA — Módulo ai",
                DocumentCategory.AI,
                """
                # Arquitectura IA — Integrity Family

                ## Proveedores
                | Proveedor | Uso Principal | Clase |
                |---|---|---|
                | Claude (Anthropic) | Respuestas empáticas, análisis cualitativo | ClaudeAiProvider (@Primary) |
                | Gemini (Google) | Análisis de tendencias, resúmenes | GeminiAiProvider |
                | DeepSeek | Análisis técnico / costos reducidos | DeepSeekAiProvider |

                ## Componentes Clave
                - **AiProvider** (interfaz): contrato único `generateResponse(prompt, context)`
                - **AiProviderSelector**: selecciona proveedor según tipo de tarea (TaskType)
                - **PromptGenerator**: construye prompts estructurados con contexto familiar
                - **ContextSynthesizer**: sintetiza ICF, miembros, historia → AiContext
                - **ClaudeAiService**: bridge especializado para interacción por voz
                - **CopilotService**: copiloto familiar — conversación continua con memoria
                - **AiInferenceService**: inferencia asíncrona con persistencia en ai_inferences
                - **ConversationSessionService**: gestión de sesiones de chat por WebSocket

                ## Flujo de una Consulta IA
                1. Usuario envía mensaje (HTTP o WebSocket)
                2. ContextSynthesizer construye AiContext con estado familiar
                3. PromptGenerator arma el prompt con instrucciones sistémicas
                4. AiProviderSelector elige proveedor según TaskType
                5. Proveedor llama a la API (Claude/Gemini/DeepSeek)
                6. Respuesta persistida en ai_inferences y enviada al cliente

                ## Motor Emocional (Scanner)
                El módulo `scanner` analiza señales de riesgo en tiempo real:
                - Detecta patrones en dailies, evaluaciones y chat
                - Genera alertas via AlertEngine
                - Actualiza el risk_level de la familia
                """,
                "Arquitectura del módulo IA: proveedores, componentes y flujo de inferencia",
                "1.1",
                "IA,Claude,Gemini,DeepSeek,CopilotService,AiProvider,motor-emocional,scanner"
            ),

            new ProjectDocument(
                "PT-DEV-01",
                "Guía de Desarrollo Local",
                DocumentCategory.DEVELOPMENT,
                """
                # Guía de Desarrollo Local

                ## Prerequisitos
                - Java 17+, Maven 3.9+
                - Docker Desktop (para MySQL y RabbitMQ)
                - Node.js 20+ (para Angular)

                ## Levantar el Entorno
                ```bash
                # BD + RabbitMQ
                docker compose up -d db rabbitmq

                # Backend (desde /backend)
                mvn spring-boot:run

                # Frontend (desde /if-frontend)
                ng serve
                ```

                ## Puertos
                - Backend: http://localhost:8080
                - Frontend: http://localhost:4200
                - MySQL: localhost:3306 (usuario: root, pwd: en docker-compose.yml)
                - RabbitMQ UI: http://localhost:15672

                ## Tests
                ```bash
                cd backend
                mvn test                          # todos los tests unitarios
                mvn test -Dtest=NombreTest        # test específico
                mvn verify -P ci                  # tests + JaCoCo (igual que CI)
                ```

                ## CI/CD
                | Evento | Workflow | Acción |
                |---|---|---|
                | PR a main | quality.yml | Tests + JaCoCo (40%) + SonarCloud |
                | Push a main (backend) | deploy-backend.yml | Deploy a Railway |
                | Push a main (frontend) | deploy-frontend.yml | Deploy a Vercel |

                ## Convenciones Git
                - Rama activa de desarrollo: `principal`
                - Rama de producción: `main`
                - Commits: `tipo(scope): descripción` (feat, fix, ci, docs, refactor)
                """,
                "Cómo configurar y ejecutar el proyecto en local: Docker, Maven, Angular, tests y CI",
                "1.0",
                "desarrollo,local,docker,maven,angular,tests,CI,git,Railway,Vercel"
            ),

            new ProjectDocument(
                "PT-SEG-01",
                "Seguridad y Autenticación",
                DocumentCategory.DEVELOPMENT,
                """
                # Seguridad — Integrity Family

                ## JWT
                - Librería: jjwt 0.12.6
                - Access token: 24 horas
                - Refresh token: 7 días
                - Almacenamiento cliente: localStorage (Angular)

                ## Roles
                - ROLE_USER: acceso a su propia familia
                - ROLE_ADMIN: acceso total (bypass en SecurityValidator)

                ## SecurityValidator
                Valida propiedad de familia en cada operación:
                1. Principal null → AccessDeniedException("No autenticado")
                2. ROLE_ADMIN → acceso inmediato
                3. Email == family.createdBy.email → acceso como creador
                4. Email == member.email && member.active && member.familyId == familyId → acceso como miembro
                5. Cualquier otro caso → AccessDeniedException

                ## Account Lock
                - 5 intentos fallidos → cuenta bloqueada 15 minutos
                - AuditService registra todos los intentos en audit_events

                ## Headers de Seguridad
                - CORS configurado en SecurityConfig
                - HTTPS obligatorio en producción (Railway + Cloudflare)
                """,
                "Modelo de seguridad: JWT, roles, SecurityValidator y bloqueo de cuentas",
                "1.0",
                "seguridad,JWT,autenticación,roles,ADMIN,SecurityValidator,CORS"
            )
        );

        try {
            repository.saveAll(docs);
            log.info("[DOCS] {} documentos base cargados correctamente.", docs.size());
        } catch (Exception e) {
            log.error("[DOCS] Error al cargar documentos base (la app sigue funcionando): {}", e.getMessage());
        }
    }
}
