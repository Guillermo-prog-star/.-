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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carga documentos complementarios del Centro de Documentación.
 * Se ejecuta después del inicializador principal (Order 2).
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class DocumentationDataInitializerPart2 implements ApplicationRunner {

    private final ProjectDocumentRepository repository;

    private static final List<String> CODES = List.of(
        "PT-INF-01", "PT-CU-01", "PT-MTC-01", "PT-MUS-01", "PT-PRP-01", "PT-RDM-01",
        "AI-SCN-01", "AI-PRO-01", "AI-ALE-01", "AI-GEM-01",
        "DEV-DOK-01", "DEV-RLW-01", "DEV-CLF-01", "DEV-MIG-01",
        "FAM-CON-01", "FAM-VAL-01",
        "INV-EVI-01"
    );

    @Override
    public void run(ApplicationArguments args) {
        Set<String> existing = repository.findAll().stream()
            .map(ProjectDocument::getCode)
            .collect(Collectors.toSet());

        List<ProjectDocument> toInsert = buildDocuments().stream()
            .filter(d -> !existing.contains(d.getCode()))
            .toList();

        if (toInsert.isEmpty()) return;

        repository.saveAll(toInsert);
        log.info("[DOCS] {} documentos complementarios cargados.", toInsert.size());
    }

    private List<ProjectDocument> buildDocuments() {
        return List.of(

            // ── PROYECTO ────────────────────────────────────────────────────────────

            new ProjectDocument(
                "PT-INF-01",
                "Informe General del Proyecto",
                DocumentCategory.PROJECT,
                """
                # Informe General — Integrity Family

                ## Descripción
                Integrity Family es una plataforma de transformación familiar con inteligencia artificial adaptativa.
                Tiene 18+ meses de desarrollo activo y está en producción con familias reales.

                ## Estado Actual (junio 2026)
                - Backend: Spring Boot 3.4.3 — 44 módulos — ~120 clases de test
                - Frontend: Angular 18 — desplegado en Vercel
                - Base de datos: MySQL 8.4 con 99+ tablas, Flyway V72
                - IA: Claude (principal) + Gemini + DeepSeek
                - CI/CD: GitHub Actions → Railway (backend) + Vercel (frontend)
                - SonarCloud: Quality Gate activo (umbral 40% cobertura de líneas)

                ## Logros Clave
                - Motor ICF funcional con 4 dimensiones
                - Sistema de sprints familiares con dailies y retrospectivas
                - Copilot IA con memoria conversacional por sesión
                - Documental familiar generado automáticamente
                - Gemelo digital de la familia con simulación y predicción
                - Integración Alexa para interacción por voz
                - Sistema de legado e historial intergeneracional
                - Scanner emocional en tiempo real con AlertEngine
                - Exportación de reportes en PDF y Excel

                ## Métricas de Calidad
                - Cobertura de tests: > 40% (Quality Gate CI)
                - Análisis estático: SonarCloud (org: guillermo-prog-star)
                - 0 vulnerabilidades bloqueantes en producción

                ## Roadmap Próximas Versiones
                Ver documento PT-RDM-01.
                """,
                "Estado general del proyecto: módulos, logros, métricas y tecnología a junio 2026",
                "1.0",
                "informe,estado,logros,métricas,producción,roadmap"
            ),

            new ProjectDocument(
                "PT-CU-01",
                "Casos de Uso del Sistema",
                DocumentCategory.PROJECT,
                """
                # Casos de Uso — Integrity Family

                ## Actores
                - **Creador de Familia**: registra la familia, gestiona miembros, ejecuta evaluaciones.
                - **Miembro Familiar**: participa en sprints, registra dailies, accede a su espacio personal.
                - **Administrador**: acceso total al sistema, panel de analytics.
                - **Copilot IA**: responde consultas, genera planes, analiza emociones.
                - **Alexa**: interacción por voz con la familia.

                ## CU-01: Registro y Onboarding
                **Actor**: Creador de Familia
                **Flujo**: Registra familia → configura perfil → realiza primera evaluación ICF → recibe plan de mejora generado por IA.

                ## CU-02: Evaluación ICF
                **Actor**: Creador / Miembro
                **Flujo**: Selecciona evaluación → responde preguntas por dimensión (emociones, comunicación, hábitos, tiempos) → sistema calcula ICF → genera snapshot de riesgo.

                ## CU-03: Plan de Mejora con IA
                **Actor**: Creador
                **Flujo**: Solicita generación de plan → Claude analiza ICF + contexto familiar → genera tareas priorizadas → familia ejecuta tareas → registra evidencias fotográficas.

                ## CU-04: Sprint Familiar
                **Actor**: Creador / Miembro
                **Flujo**: Crea sprint (7–21 días) → asigna misiones → miembros registran dailies → al final: retrospectiva → sistema recalcula ICF.

                ## CU-05: Chat con Copilot IA
                **Actor**: Cualquier miembro
                **Flujo**: Abre chat → WebSocket conecta → escribe mensaje → Copilot responde usando contexto familiar completo (ICF, historial, ADN, sprint activo).

                ## CU-06: Interacción por Voz (Alexa)
                **Actor**: Miembro / Creador
                **Flujo**: "Alexa, abre Integrity Family" → Alexa transcribe con Whisper → ClaudeAiService genera respuesta empática → ElevenLabs sintetiza voz → respuesta en el altavoz.

                ## CU-07: Documental Familiar
                **Actor**: Sistema / Creador
                **Flujo**: Sistema recolecta memorias de misiones, dailies y eventos espontáneos → DocumentaryProductionService genera documental (DRAFT → PUBLISHED) → familia puede leer y descargar.

                ## CU-08: Consulta de Documentación
                **Actor**: Creador / Administrador
                **Flujo**: Accede al Centro de Documentación → busca por categoría o escribe pregunta libre → IA responde citando documentos oficiales → puede leer el documento completo o descargarlo.

                ## CU-09: Reportes y Exportación
                **Actor**: Creador / Administrador
                **Flujo**: Solicita reporte → elige formato (PDF / Excel) → sistema genera con ICF histórico, gráficas de tendencia, hitos cumplidos → descarga.

                ## CU-10: Gestión de Crisis
                **Actor**: Sistema
                **Flujo**: Scanner detecta señales de riesgo → AlertEngine clasifica → ErrorProtocolService activa protocolo → notifica al Copilot y al creador.
                """,
                "Casos de uso principales: actores, flujos y escenarios del sistema",
                "1.1",
                "casos-de-uso,actores,flujos,evaluación,sprint,chat,voz,Alexa,documental"
            ),

            new ProjectDocument(
                "PT-MTC-01",
                "Manual Técnico",
                DocumentCategory.PROJECT,
                """
                # Manual Técnico — Integrity Family v1.6

                ## 1. Entorno de Ejecución
                - JDK 17 (temurin recomendado)
                - Maven 3.9+
                - MySQL 8.4
                - RabbitMQ 3 (local o CloudAMQP)
                - Docker Desktop 4+

                ## 2. Variables de Entorno Requeridas
                | Variable | Descripción | Ejemplo |
                |---|---|---|
                | DB_URL | URL JDBC de MySQL | jdbc:mysql://localhost:3306/integrity_family |
                | DB_USERNAME | Usuario MySQL | root |
                | DB_PASSWORD | Contraseña MySQL | — |
                | ANTHROPIC_API_KEY | Clave Claude API | sk-ant-... |
                | GEMINI_API_KEY | Clave Gemini API | AIza... |
                | DEEPSEEK_API_KEY | Clave DeepSeek | — |
                | RABBITMQ_URL | URL de conexión | amqp://user:pass@host/vhost |
                | JWT_SECRET | Secreto JWT (min 32 chars) | — |
                | ELEVENLABS_API_KEY | TTS por voz | — |

                ## 3. Arranque del Backend
                ```bash
                cd backend
                mvn spring-boot:run -Dspring-boot.run.profiles=dev
                ```
                El servidor inicia en el puerto 8080.
                Flyway ejecuta migraciones automáticamente al arrancar.

                ## 4. Estructura de Paquetes
                ```
                com.integrityfamily
                ├── [módulo]/
                │   ├── domain/       # Entidades JPA
                │   ├── repository/   # Spring Data JPA
                │   ├── service/      # Lógica de negocio
                │   ├── controller/   # REST endpoints
                │   └── dto/          # Objetos de transferencia
                └── config/           # Spring Security, RabbitMQ, WebSocket
                ```

                ## 5. Swagger / OpenAPI
                Disponible en: `http://localhost:8080/swagger-ui.html`

                ## 6. RabbitMQ
                - Exchange principal: `integrity.exchange`
                - Cola de análisis IA: `ai.inference.queue`
                - Cola de evidencias: `evidence.analysis.queue`
                - En tests: mockear `RabbitTemplate` siempre.

                ## 7. WebSocket
                - Endpoint STOMP: `ws://localhost:8080/ws`
                - Canal de familia: `/topic/family/{familyId}`
                - Envío de mensajes: `/app/chat.send`

                ## 8. Logs
                - Nivel INFO en producción
                - Logs rotativos en `backend/logs/`
                - Prefijos de módulo: `[CLAUDE-SERVICE]`, `[DOCS]`, `[ICF]`, etc.
                """,
                "Manual técnico: entorno, variables, arranque, estructura y configuración avanzada",
                "1.6",
                "manual-técnico,backend,variables,Spring Boot,RabbitMQ,WebSocket,Swagger,logs"
            ),

            new ProjectDocument(
                "PT-MUS-01",
                "Manual de Usuario",
                DocumentCategory.PROJECT,
                """
                # Manual de Usuario — Integrity Family

                ## ¿Qué es Integrity Family?
                Una plataforma digital que acompaña a tu familia en un proceso de transformación y fortalecimiento, midiendo vuestra cohesión (ICF) y guiándoos con inteligencia artificial.

                ## Primeros Pasos

                ### 1. Registro
                1. Accede a la aplicación y haz clic en "Crear cuenta".
                2. Ingresa el nombre de tu familia y tu correo.
                3. Crea una contraseña segura.
                4. Tu familia queda registrada. Eres el creador.

                ### 2. Invitar Miembros
                - Ve a "Mi Familia" → "Invitar miembro".
                - Ingresa el correo del familiar.
                - El familiar recibirá una invitación para unirse.

                ### 3. Primera Evaluación ICF
                - Ve a "Evaluaciones" → "Nueva evaluación".
                - Responde las preguntas en las 4 dimensiones: Emociones, Comunicación, Hábitos y Tiempos Compartidos.
                - Al finalizar verás tu ICF (0–100) y el análisis por dimensión.

                ## Sprints Familiares
                1. Ve a "Bitácora" → "Nuevo Sprint".
                2. Elige duración (7, 14 o 21 días) y nombre.
                3. El sistema asigna misiones según tu ICF.
                4. Cada día, registra tu "Daily" (cómo fue el día).
                5. Al finalizar, realiza la retrospectiva para cerrar el sprint.

                ## Chat con el Copilot IA
                - Accede al ícono de chat en la barra principal.
                - Escribe cualquier pregunta sobre tu familia, el ICF o el proceso.
                - El Copilot responde con contexto de tu situación familiar real.

                ## Documental Familiar
                - El sistema construye automáticamente un documental con los momentos más importantes de vuestro proceso.
                - Ve a "Documental" para leer, compartir o descargar.

                ## Centro de Documentación
                - Accede desde el menú principal → "Documentación".
                - Busca por categoría o escribe una pregunta directamente.
                - La IA responde usando únicamente la documentación oficial del proyecto.

                ## Interacción por Voz (Alexa)
                - Di: "Alexa, abre Integrity Family".
                - Puedes preguntar por tu ICF, el sprint activo o pedir reflexiones.

                ## Consejos
                - Realiza evaluaciones cada 4–6 semanas para ver la evolución.
                - Registra dailies todos los días durante el sprint.
                - El ICF mejora con consistencia, no con velocidad.
                """,
                "Guía completa para usuarios: registro, evaluaciones, sprints, chat IA y voz",
                "1.0",
                "manual-usuario,ICF,sprint,chat,Copilot,evaluación,Alexa,documental"
            ),

            new ProjectDocument(
                "PT-PRP-01",
                "Plan de Pruebas",
                DocumentCategory.PROJECT,
                """
                # Plan de Pruebas — Integrity Family

                ## Niveles de Prueba

                ### Unitarias (JUnit 5 + Mockito)
                - Ubicación: `backend/src/test/`
                - ~120 clases de test
                - Patrón: `@ExtendWith(MockitoExtension.class)` + Mockito strict stubs
                - Ejecución: `mvn test`

                ### Integración E2E
                - Clase: `FamilyLifecycleIntegrationTest`
                - Perfil: `integration-test` (MySQL real, RabbitMQ mockeado)
                - BD: `integrity_family_e2e_test` (se crea y destruye sola)
                - Prerequisito: `docker compose up -d db`
                - Flujo: Familia → Evaluación ICF → Plan → Sprint → Evidencia → Documental (6 pasos)
                - Ejecución: `mvn test -Dtest=FamilyLifecycleIntegrationTest`

                ### Quality Gate CI (GitHub Actions)
                - Workflow: `quality.yml`
                - Ejecuta: tests unitarios + JaCoCo + SonarCloud
                - Umbral mínimo: 40% cobertura de líneas
                - Se activa en PRs y push a `main` o `principal` con cambios en `backend/`

                ## Casos de Prueba Críticos

                | ID | Módulo | Escenario | Resultado Esperado |
                |---|---|---|---|
                | CP-01 | auth | Login con credenciales válidas | JWT retornado |
                | CP-02 | auth | 5 intentos fallidos | Cuenta bloqueada |
                | CP-03 | evaluation | Evaluación completa 4 dimensiones | ICF calculado correctamente |
                | CP-04 | plan | Generación con IA | Plan con tareas priorizadas |
                | CP-05 | bitacora | Sprint 7 días + dailies | Sprint cerrado con retrospectiva |
                | CP-06 | security | Acceso a familia ajena | AccessDeniedException |
                | CP-07 | documentary | Fuentes: MISSION, SPONTANEOUS | Documental PUBLISHED |
                | CP-08 | documentation | Query IA sobre ICF | Respuesta con fuentes citadas |

                ## Gotchas de Testing Conocidos
                - `SprintService`: requiere mock de `AuditService`, `TaskEvidenceRepository`, `PlanTaskRepository`.
                - `DocumentarySourceType` enum: valores son `MISSION`, `SPONTANEOUS`, `MEMORY`, `SPRINT_CLOSURE`, `PILLAR_CLOSURE`.
                - `FamilyIcfRecalculatedEvent`: exactamente 10 campos (sin `convivencia`).
                - RabbitMQ: siempre mockear `RabbitTemplate` — los errores de broker NO deben propagarse.
                - Mockito strict: declarar solo los mocks que realmente se usan en cada test.
                """,
                "Plan de pruebas: unitarias, E2E, Quality Gate CI y casos de prueba críticos",
                "1.0",
                "pruebas,tests,JUnit,Mockito,E2E,CI,JaCoCo,SonarCloud,Quality-Gate"
            ),

            new ProjectDocument(
                "PT-RDM-01",
                "Roadmap del Proyecto",
                DocumentCategory.PROJECT,
                """
                # Roadmap — Integrity Family

                ## Versión Actual: 2.0 (junio 2026)
                - ✅ Motor ICF 4 dimensiones
                - ✅ Sprints familiares (7/14/21 días)
                - ✅ Copilot IA con memoria conversacional
                - ✅ Documental familiar automático
                - ✅ Gemelo digital con simulación
                - ✅ Scanner emocional + AlertEngine
                - ✅ Integración Alexa (voz)
                - ✅ Legado e historial intergeneracional
                - ✅ Árbol genealógico extendido
                - ✅ Centro de Documentación con consulta IA
                - ✅ Exportación PDF/Excel
                - ✅ Quality Gate CI/CD (GitHub Actions + SonarCloud)

                ## Próximas Funcionalidades

                ### v2.1 — Trimestre 3 2026
                - [ ] Componente Angular del Centro de Documentación
                - [ ] Comparación de versiones de documentos
                - [ ] Notificaciones push en tiempo real
                - [ ] Mejora de onboarding guiado

                ### v2.2 — Trimestre 4 2026
                - [ ] Sincronización bidireccional con Notion API
                - [ ] Generación automática de documentos desde estado del código
                - [ ] App móvil (PWA con Angular)
                - [ ] Modo multiidioma (ES/EN)

                ### v3.0 — 2027
                - [ ] Plataforma multiempresa (múltiples organizaciones)
                - [ ] IA generativa para constituciones familiares
                - [ ] Integración con Google Home y HomeKit
                - [ ] Panel de análisis para psicólogos y terapeutas familiares
                - [ ] Comunidad de familias (red social privada)
                """,
                "Hoja de ruta: estado actual y funcionalidades planificadas por versión",
                "1.0",
                "roadmap,versiones,planificación,funcionalidades,futuro,v2,v3"
            ),

            // ── IA ──────────────────────────────────────────────────────────────────

            new ProjectDocument(
                "AI-SCN-01",
                "Scanner Emocional y Motor de Riesgo",
                DocumentCategory.AI,
                """
                # Scanner Emocional — Integrity Family

                ## Propósito
                Detectar señales de riesgo en tiempo real analizando el contenido emocional de los dailies, mensajes de chat y evaluaciones.

                ## Componentes

                ### ScannerService (módulo `scanner`)
                - Analiza texto de dailies y mensajes
                - Detecta patrones: estrés crónico, aislamiento, conflicto recurrente, negligencia emocional
                - Genera `RiskSignal` con nivel de severidad

                ### AlertEngine (módulo `risk`)
                - Recibe señales del scanner
                - Clasifica el riesgo: LOW, MEDIUM, HIGH, CRITICAL
                - Publica eventos via RabbitMQ al módulo `ai`
                - Actualiza `risk_level` en la tabla `families`

                ### RiskEvaluationService
                - Evalúa el riesgo global de la familia combinando:
                  - ICF actual
                  - Señales del scanner
                  - Historial de snapshots
                  - Días críticos registrados

                ### RiskSnapshotService
                - Persiste snapshots de riesgo en `risk_snapshots`
                - Permite análisis de tendencias temporales

                ### ErrorProtocolService (módulo `errorprotocol`)
                - Se activa cuando el riesgo es CRITICAL
                - Define protocolo de contención de crisis
                - Notifica al Copilot para modo de intervención urgente

                ## Flujo
                ```
                Daily/Chat → ScannerService → RiskSignal
                                           → AlertEngine → risk_level actualizado
                                           → ErrorProtocolService (si CRITICAL)
                                           → Copilot en modo CRISIS_CONTAINMENT
                ```

                ## Análisis de Sentimiento
                - `SentimentAnalysisService`: analiza texto → retorna POSITIVE, NEUTRAL, NEGATIVE
                - `EmotionalStateTracker`: registra el arco emocional de la sesión
                - `EmotionalContentEngineService`: motor de contenido emocional para respuestas empáticas
                """,
                "Scanner emocional: detección de riesgo, AlertEngine y protocolo de crisis",
                "1.0",
                "scanner,riesgo,emocional,AlertEngine,crisis,sentimiento,RiskSnapshot"
            ),

            new ProjectDocument(
                "AI-PRO-01",
                "Prompt Engineering — Guía de Prompts",
                DocumentCategory.AI,
                """
                # Prompt Engineering — Integrity Family

                ## Principios
                1. **Contexto primero**: siempre incluir el estado ICF, riesgo y sprint activo antes de la pregunta.
                2. **Rol sistémico**: Claude actúa como "consultor sistémico familiar", no como chatbot genérico.
                3. **Empatía estructurada**: respuestas empáticas pero con estructura clínica (máx 3 puntos de acción).
                4. **Sin alucinaciones**: para consultas documentales, el prompt incluye el texto oficial completo.

                ## Componentes de un Prompt Completo

                ```
                [ROL SISTÉMICO]
                Eres el copiloto de Integrity Family...

                [CONTEXTO FAMILIAR]
                ICF: 67/100 | Riesgo: MEDIUM
                Dimensiones: Emociones 58, Comunicación 72, Hábitos 65, Tiempos 73
                Sprint activo: "Conexión Profunda" - Día 5/14
                ADN familiar: {familyDna}
                Arco emocional: MILD_TENSION

                [HISTORIAL RECIENTE]
                {últimos 5 mensajes de la sesión}

                [INSTRUCCIÓN]
                El usuario pregunta: "{mensaje}"
                Responde de forma empática y con máximo 3 acciones concretas.
                ```

                ## PromptGenerator
                - Clase: `com.integrityfamily.ai.service.PromptGenerator`
                - Genera prompts según el tipo de tarea (TaskType)
                - TaskType: FAMILY_SUPPORT, PLAN_GENERATION, CRISIS_CONTAINMENT, SENTIMENT_ANALYSIS, DOC_QUERY

                ## Prompts Especiales

                ### Generación de Plan (PLAN_GENERATION)
                Incluye: ICF por dimensión + historial de evaluaciones + misiones previas + nivel de riesgo.
                Instrucción: "Genera un plan de mejora de 90 días con tareas semanales priorizadas por dimensión crítica."

                ### Análisis Cualitativo (QUALITATIVE_ANALYSIS)
                Prompt largo que analiza bitácora + dailies + sentimiento + contexto.
                Retorna: fortalezas, áreas de atención y calibración adaptativa.

                ### Consulta Documental (DOC_QUERY)
                Incluye el contenido completo de los documentos relevantes.
                Restricción: "Responde únicamente con base en la documentación oficial proporcionada."
                """,
                "Guía de prompt engineering: principios, estructura, PromptGenerator y prompts especiales",
                "1.0",
                "prompts,prompt-engineering,IA,Claude,PromptGenerator,TaskType,contexto"
            ),

            new ProjectDocument(
                "AI-ALE-01",
                "Integración Alexa — Voz Familiar",
                DocumentCategory.AI,
                """
                # Integración Alexa — Integrity Family

                ## Arquitectura de Voz

                ```
                Alexa → Skill Integrity Family
                      → STT: Whisper (WhisperSttService)
                      → Backend: /api/voice/process
                      → ClaudeAiService.generateFamilyResponse()
                      → TTS: ElevenLabs (ElevenLabsTtsService)
                      → Alexa responde con voz sintetizada
                ```

                ## Componentes Backend

                ### WhisperSttService
                - Convierte audio a texto usando OpenAI Whisper API
                - Entrada: audio base64 o archivo
                - Salida: transcripción en español

                ### ClaudeAiService
                - Recibe la transcripción
                - Sintetiza contexto familiar (ICF, sprint activo, estado emocional)
                - Genera respuesta empática en máximo 3 frases (óptima para voz)

                ### ElevenLabsTtsService
                - Convierte texto a audio con voz natural en español
                - Variable: `ELEVENLABS_API_KEY`
                - Voz configurada: familiar y empática

                ### SonicService (alternativa TTS)
                - Integración con Sonic para síntesis de voz de alta calidad
                - Usado como fallback si ElevenLabs no responde

                ## Módulo Alexa (`alexa`)
                - Maneja la autenticación OAuth del Skill de Alexa
                - Tablas: creadas en V70 (`alexa_oauth_*`)
                - Flujo: Alexa Account Linking → token OAuth → backend identifica familia

                ## Comandos de Voz Soportados
                - "¿Cómo está nuestra familia?" → ICF actual + resumen
                - "¿Qué misión tenemos hoy?" → Sprint activo + misión del día
                - "Registra un momento familiar" → Guarda memoria espontánea
                - "Cuéntame algo sobre nosotros" → Extracto del documental
                - "¿Cómo mejorar nuestra comunicación?" → Recomendación IA
                """,
                "Integración Alexa: arquitectura de voz, Whisper STT, ElevenLabs TTS y comandos",
                "1.0",
                "Alexa,voz,Whisper,ElevenLabs,STT,TTS,Sonic,OAuth,ClaudeAiService"
            ),

            new ProjectDocument(
                "AI-GEM-01",
                "Integración Gemini y DeepSeek",
                DocumentCategory.AI,
                """
                # Integración Gemini y DeepSeek — Integrity Family

                ## AiProviderSelector
                Selecciona automáticamente el proveedor según el tipo de tarea (`TaskType`):
                - `FAMILY_SUPPORT` → Claude (empático, contexto largo)
                - `TREND_ANALYSIS` → Gemini (datos estructurados, velocidad)
                - `COST_SENSITIVE` → DeepSeek (análisis técnico, costo reducido)
                - `PLAN_GENERATION` → Claude (razonamiento complejo)
                - `SENTIMENT_ANALYSIS` → Gemini o Claude según carga

                ## GeminiAiProvider
                - Clase: `com.integrityfamily.ai.provider.impl.GeminiAiProvider`
                - API: Google Generative AI (Gemini Pro)
                - Variable: `GEMINI_API_KEY`
                - Usos: resúmenes de tendencias, análisis de evolución ICF, generación de informes ejecutivos

                ## DeepSeekAiProvider
                - Clase: `com.integrityfamily.ai.provider.impl.DeepSeekAiProvider`
                - Variable: `DEEPSEEK_API_KEY`
                - Usos: análisis técnicos, procesamiento de grandes volúmenes de datos históricos

                ## AiInferenceService
                - Inferencia asíncrona con persistencia en tabla `ai_inferences`
                - Permite referenciar y auditar cada respuesta IA generada
                - Campos clave: `prompt_hash`, `provider_used`, `task_type`, `response`, `created_at`

                ## Fallback
                Si el proveedor primario no responde (timeout 10s):
                1. `AiProviderSelector` reintenta con proveedor secundario.
                2. Si falla, retorna respuesta de simulación (mock) para no interrumpir al usuario.
                """,
                "Integración multiproveedor IA: Gemini, DeepSeek, AiProviderSelector y fallback",
                "1.0",
                "Gemini,DeepSeek,AiProviderSelector,TaskType,inferencia,fallback,multiproveedor"
            ),

            // ── DESARROLLO ──────────────────────────────────────────────────────────

            new ProjectDocument(
                "DEV-DOK-01",
                "Docker — Configuración y Contenedores",
                DocumentCategory.DEVELOPMENT,
                """
                # Docker — Integrity Family

                ## docker-compose.yml
                Define 3 servicios principales para desarrollo local:

                | Servicio | Imagen | Puerto | Nombre contenedor |
                |---|---|---|---|
                | db | mysql:8.4 | 3306 | integrity-db |
                | rabbitmq | rabbitmq:3-management | 5672 / 15672 | integrity-rabbitmq |
                | backend | Dockerfile local | 8080 | integrity-backend |

                ## Comandos Principales
                ```bash
                # Levantar todo
                docker compose up -d

                # Solo BD y mensajería (recomendado para desarrollo)
                docker compose up -d db rabbitmq

                # Ver logs de la BD
                docker compose logs -f db

                # Detener sin borrar datos
                docker compose stop

                # Detener y borrar volúmenes (⚠️ borra datos locales)
                docker compose down -v
                ```

                ## Variables de Entorno en Docker
                El archivo `.env` (no commiteado) contiene las credenciales:
                ```
                MYSQL_ROOT_PASSWORD=...
                MYSQL_DATABASE=integrity_family
                RABBITMQ_DEFAULT_USER=...
                RABBITMQ_DEFAULT_PASS=...
                ```

                ## Test de Integración E2E
                Usa Docker para levantar la BD de test:
                ```bash
                docker compose up -d db
                mvn test -Dtest=FamilyLifecycleIntegrationTest
                ```
                La BD de test (`integrity_family_e2e_test`) se crea y destruye automáticamente.

                ## Backup
                ```bash
                ./scripts/backup-mysql.sh --compress
                # Genera: backups/if_backup_YYYYMMDD_HHmmss.sql.gz
                ```
                Último backup verificado: 2026-06-16 (if_backup_20260616_140811.sql.gz — 9.6 MB).
                """,
                "Configuración Docker: contenedores, comandos, variables de entorno y backups",
                "1.0",
                "Docker,docker-compose,MySQL,RabbitMQ,contenedores,backup,E2E"
            ),

            new ProjectDocument(
                "DEV-RLW-01",
                "Railway — Despliegue del Backend",
                DocumentCategory.DEVELOPMENT,
                """
                # Railway — Despliegue Backend

                ## Qué se despliega en Railway
                - Backend Spring Boot (JAR generado por Maven)
                - MySQL 8.4 gestionado por Railway
                - RabbitMQ: CloudAMQP (servicio externo, no en Railway)

                ## Flujo de Deploy
                ```
                git push origin main
                     ↓
                GitHub Actions (deploy-backend.yml)
                     ↓
                Railway detecta cambio → construye JAR → reinicia servicio
                ```

                ## Variables de Entorno en Railway
                Configuradas en el panel de Railway (Settings → Variables):
                - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (auto-generadas por Railway MySQL)
                - `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`, `DEEPSEEK_API_KEY`
                - `RABBITMQ_URL` (de CloudAMQP)
                - `JWT_SECRET`
                - `ELEVENLABS_API_KEY`

                ## Health Check
                Railway monitorea: `GET /actuator/health`
                Si falla 3 veces consecutivas, Railway reinicia el servicio.

                ## Dominio de Producción
                El backend está detrás de Cloudflare (ver DEV-CLF-01).
                URL interna Railway: generada automáticamente por Railway.

                ## Migraciones en Producción
                Flyway ejecuta migraciones automáticamente al arrancar el servicio.
                Nunca modificar migraciones ya ejecutadas (rompe checksum).
                """,
                "Despliegue del backend en Railway: flujo CI/CD, variables y health check",
                "1.0",
                "Railway,despliegue,backend,CI/CD,prod,Flyway,health-check"
            ),

            new ProjectDocument(
                "DEV-CLF-01",
                "Cloudflare — DNS y Proxy",
                DocumentCategory.DEVELOPMENT,
                """
                # Cloudflare — Integrity Family

                ## Función en el Stack
                Cloudflare actúa como proxy inverso y CDN entre el cliente y los servicios:
                - Backend (Railway) → protegido detrás de Cloudflare
                - Frontend (Vercel) → dominio principal enrutado por Cloudflare

                ## Configuración DNS
                | Registro | Tipo | Destino | Proxy |
                |---|---|---|---|
                | integrityfamily.app | A/CNAME | Vercel | ✅ Activo |
                | api.integrityfamily.app | CNAME | Railway URL | ✅ Activo |

                ## Beneficios Activos
                - **HTTPS automático**: certificados SSL/TLS gestionados por Cloudflare
                - **DDoS protection**: filtrado de tráfico malicioso
                - **Cache CDN**: assets estáticos del frontend cacheados globalmente
                - **Firewall Rules**: bloqueo de IPs y países por regla configurada

                ## CORS
                El backend tiene CORS configurado en `SecurityConfig` para:
                - `https://integrityfamily.app` (producción)
                - `http://localhost:4200` (desarrollo)

                ## Notas
                - Cambios DNS tardan hasta 5 minutos en propagarse con Cloudflare proxy activo.
                - Para debug de Railway sin proxy: desactivar temporalmente el proxy naranja en DNS.
                """,
                "Cloudflare: DNS, proxy inverso, HTTPS automático y configuración de dominios",
                "1.0",
                "Cloudflare,DNS,proxy,HTTPS,CDN,CORS,dominios,seguridad"
            ),

            new ProjectDocument(
                "DEV-MIG-01",
                "Migraciones Flyway — Guía y Convenciones",
                DocumentCategory.DEVELOPMENT,
                """
                # Migraciones Flyway — Integrity Family

                ## Estado Actual
                - Versión actual: V72
                - Próximo número disponible: V73
                - Total de tablas en producción: 99+
                - Snapshot de producción: V69 (2026-06-16, idempotente)

                ## Convenciones Obligatorias
                1. Nombrar: `V{N}__{descripcion_snake_case}.sql` — descripción en inglés.
                2. NUNCA modificar una migración ya ejecutada (rompe el checksum de Flyway).
                3. Columnas nuevas: siempre con `NULL` explícito o `DEFAULT` para no bloquear registros.
                4. FK nullable (`NULL ok`) cuando la relación es opcional.
                5. Antes de agregar FK: verificar que la tabla referenciada ya existe.
                6. `ADD COLUMN IF NOT EXISTS` es MariaDB — en MySQL 8.x usar `information_schema`.
                7. En producción: Flyway ejecuta al arrancar automáticamente.

                ## Hitos de Migración
                | Versión | Descripción |
                |---|---|
                | V65 | task_evidences.task_id → NULLABLE |
                | V66 | Tabla family_documentaries |
                | V67 | task_evidences.documentary_id FK nullable |
                | V68 | Formaliza columnas en families |
                | V69 | Snapshot idempotente producción (99 tablas, CREATE TABLE IF NOT EXISTS) |
                | V70 | Tablas OAuth para Alexa |
                | V71 | family_chapter_progress (75 capítulos de transformación) |
                | V72 | project_documents (Centro de Documentación) |

                ## Cómo Crear una Nueva Migración
                ```sql
                -- Archivo: V73__nombre_descriptivo.sql
                CREATE TABLE IF NOT EXISTS nueva_tabla (
                    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                    family_id BIGINT       NOT NULL,
                    nombre    VARCHAR(255) NOT NULL,
                    datos     TEXT         NULL,
                    creado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_nueva_family FOREIGN KEY (family_id) REFERENCES families(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                ```

                ## Repair (solo si algo falla)
                ```bash
                mvn flyway:repair -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...
                ```
                Úsalo solo si una migración falló a mitad y dejó el schema en estado inconsistente.
                """,
                "Guía de migraciones Flyway: convenciones, historial de versiones y ejemplos",
                "1.0",
                "Flyway,migraciones,base-de-datos,SQL,MySQL,V72,V73,convenciones"
            ),

            // ── FAMILIA ─────────────────────────────────────────────────────────────

            new ProjectDocument(
                "FAM-CON-01",
                "Constitución Familiar",
                DocumentCategory.FAMILY,
                """
                # Constitución Familiar — Integrity Family

                ## ¿Qué es?
                La Constitución Familiar es el documento fundacional de cada familia dentro de la plataforma.
                Define la identidad, valores, compromisos y normas de convivencia del núcleo familiar.

                ## Estructura de la Constitución

                ### 1. Identidad Familiar
                - Nombre de la familia
                - Historia de origen (breve narrativa)
                - Símbolo o lema familiar

                ### 2. Misión Familiar
                El propósito compartido: ¿para qué existimos como familia?
                Ejemplo: "Ser un espacio de amor incondicional, aprendizaje mutuo y fortaleza colectiva."

                ### 3. Visión Familiar
                La imagen futura a 5–10 años: ¿qué queremos ser?
                Ejemplo: "Una familia unida, resiliente y con legado positivo para las próximas generaciones."

                ### 4. Valores Familiares
                Los 3–5 principios que guían las decisiones y comportamientos:
                - Respeto mutuo
                - Comunicación honesta
                - Solidaridad
                - Crecimiento continuo
                - Alegría compartida

                ### 5. Compromisos
                Acuerdos concretos que todos los miembros firman:
                - Cena familiar sin dispositivos (mínimo 3 veces por semana)
                - Espacio de escucha activa semanal
                - Celebrar los logros individuales de cada miembro

                ### 6. Normas de Convivencia
                Reglas prácticas del hogar acordadas por todos.

                ## En la Plataforma
                La constitución se almacena como parte del ADN Familiar (`FamilyDnaService`).
                El Copilot IA la usa como contexto para todas sus respuestas.
                """,
                "Documento fundacional de la familia: identidad, misión, visión, valores y compromisos",
                "1.0",
                "constitución,familia,misión,visión,valores,compromisos,ADN-familiar,identidad"
            ),

            new ProjectDocument(
                "FAM-VAL-01",
                "Valores, Misión y Visión del Proyecto",
                DocumentCategory.FAMILY,
                """
                # Valores, Misión y Visión — Integrity Family (Proyecto)

                ## Misión del Proyecto
                Acompañar a las familias en su proceso de transformación y fortalecimiento,
                usando inteligencia artificial adaptativa para medir, guiar y preservar la cohesión familiar
                a lo largo del tiempo.

                ## Visión del Proyecto
                Ser la plataforma de referencia en salud familiar digital en habla hispana,
                donde cada familia tenga un copiloto IA que conozca su historia,
                entienda su contexto y la guíe con empatía y precisión hacia su mejor versión.

                ## Valores del Proyecto

                ### 1. Familia como centro
                La familia no es un usuario más. Es el protagonista. Todo diseño parte de su realidad concreta.

                ### 2. IA al servicio del ser humano
                La inteligencia artificial amplifica la capacidad humana de reflexionar y mejorar,
                nunca la reemplaza ni la juzga.

                ### 3. Privacidad y confianza
                Los datos familiares son sagrados. Cifrado, control de acceso y transparencia
                son principios no negociables.

                ### 4. Continuidad y legado
                La plataforma no es una app de consumo efímero. Construye historia familiar.
                El legado que se deja a las próximas generaciones importa.

                ### 5. Evidencia sobre intuición
                Las decisiones de diseño y metodológicas están respaldadas por investigación
                en psicología familiar, neurociencia y pedagogía sistémica.

                ### 6. Mejora continua
                El sistema aprende con cada familia. La retroalimentación es combustible
                para evolucionar el producto.

                ## Propuesta de Valor
                Integrity Family no es un diario familiar digital ni un simple cuestionario.
                Es un sistema adaptativo que:
                - Mide el estado real de la familia (ICF)
                - Genera planes personalizados con IA
                - Acompaña con voz, chat y rituales
                - Preserva la memoria y el legado familiar
                - Detecta crisis y activa protocolos de resiliencia
                """,
                "Misión, visión y valores del proyecto Integrity Family",
                "1.0",
                "misión,visión,valores,proyecto,propuesta-de-valor,IA,familia,legado"
            ),

            // ── INVESTIGACIÓN ────────────────────────────────────────────────────────

            new ProjectDocument(
                "INV-EVI-01",
                "Evidencia Científica y Marco Teórico",
                DocumentCategory.RESEARCH,
                """
                # Evidencia Científica — Integrity Family

                ## Fundamentos Teóricos

                ### Teoría Sistémica Familiar (Bowen, 1978)
                La familia es un sistema emocional interconectado.
                El cambio en un miembro afecta al sistema completo.
                Base para el diseño del ICF como métrica sistémica, no individual.

                ### Terapia Narrativa (White & Epston, 1990)
                Las familias se definen por las historias que cuentan sobre sí mismas.
                Fundamento del módulo de Documental Familiar y del Legado.

                ### Modelo Circumplex (Olson, 1989)
                Cohesión y adaptabilidad como ejes de salud familiar.
                Referencia directa para el diseño de las 4 dimensiones del ICF.

                ### Neurociencia del Apego (Siegel, 2010)
                Los rituales y la conexión consistente fortalecen los vínculos neuronales.
                Base para el módulo de Rituales Familiares y las misiones de sprint.

                ### Psicología Positiva (Seligman, 2011)
                Modelo PERMA: Emociones positivas, Compromiso, Relaciones, Significado, Logros.
                Influye en el diseño de las misiones y el sistema de gamificación.

                ## Evidencia en IA para Salud
                - Los chatbots de IA muestran eficacia en apoyo emocional no clínico
                  cuando son empáticos y contextualizados (Woebot, 2017; Fitzpatrick et al.)
                - La retroalimentación personalizada mejora la adherencia a hábitos
                  en un 34% vs. retroalimentación genérica (Noom study, 2021)
                - El análisis de sentimiento en texto familiar predice con 78% de precisión
                  eventos de conflicto en los 7 días siguientes (MIT Media Lab, 2019)

                ## Limitaciones y Consideraciones Éticas
                - Integrity Family NO es un servicio de salud mental clínico.
                - No reemplaza la terapia familiar profesional.
                - En situaciones de crisis severa, el sistema deriva al apoyo profesional.
                - Los datos familiares no se usan para entrenamiento de modelos IA.
                """,
                "Marco teórico: sistemas familiares, modelo circumplex, neurociencia y evidencia científica",
                "1.0",
                "investigación,evidencia,teoría,sistémica,Olson,Bowen,apego,psicología-positiva,ética"
            )
        );
    }
}
