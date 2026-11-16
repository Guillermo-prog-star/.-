# Documentación SENA — Integrity Family

Documentos de proyecto formativo tipo 1 (plantillas 2026) diligenciados por ingeniería inversa de la rama `feature/v1.2-micro-simulations`, commit `0d3cfef` (2026-11-15). La redacción es del 16/11/2026. No se modificó código de aplicación.

Los códigos usados son los oficiales de 2026: PT-ERS-02, PT-ECU-02 y PT-PP-03. Lo que el repositorio no permite afirmar queda como `[Pendiente: …]`. La lista única para completar antes de la entrega al instructor está al final de este índice.

## Índice

| Código | Documento | Qué cubre |
| --- | --- | --- |
| — | [00 Principal](00_Principal_Integrity_Family.md) | Portada y enlaces a los formatos. |
| PT-PP-01 | [Planteamiento del problema](01_PT-PP-01_Planteamiento_del_Problema_Integrity_Family.md) | Problema, situación actual y esperada, justificación y normas, con cita de archivo. |
| PT-ERS-02 | [Especificación de requisitos](02_PT-ERS-02_Especificacion_de_Requisitos_Integrity_Family.md) | Actores, módulos M01–M11, 68 requisitos funcionales y requisitos no funcionales. |
| PT-ECU-02 | [Casos de uso](03_PT-ECU-02_Especificacion_Casos_de_Uso_Integrity_Family.md) | Actores, diagramas Mermaid y especificación de CU-01 a CU-12. |
| PT-IGS-01 | [Informe general](04_PT-IGS-01_Informe_General_del_Sistema_Integrity_Family.md) | Objetivo, alcance, roles e historias de usuario. |
| PT-PS-01 | [Prototipado](05_PT-PS-01_Prototipado_Integrity_Family.md) | Arquitectura de información y pantallas reales. |
| PT-PP-03 | [Plan de pruebas](06_PT-PP-03_Plan_de_Pruebas_Integrity_Family.md) | Casos CP-01 a CP-38 ligados a clases de prueba existentes. |
| PT-MCBD-01 | [Manual de base de datos](07_PT-MCBD-01_Manual_Configuracion_BD_Integrity_Family.md) | MER, diccionario del núcleo, catálogo de 122 entidades y arranque MySQL. |
| PT-MTC-01 | [Manual técnico](08_PT-MTC-01_Manual_Tecnico_Configuracion_Integrity_Family.md) | Arquitectura, patrones, variables y despliegue. |
| PT-MU-01 | [Manual de usuario](09_PT-MU-01_Manual_de_Usuario_Integrity_Family.md) | Navegación por rol y mensajes literales de error, aviso, confirmación e información. |
| Anexos | [Anexos](10_Anexos_Integrity_Family.md) | Backlogs, actas, investigación y arquitectura. Hijos en `anexos/`. |

## Matriz de trazabilidad

Requisito, caso de uso, módulo o clase, endpoint o ruta, y caso de prueba. La clase de prueba es la que existe en el repositorio; el resultado de ejecutarla no se registró en esta entrega.

| Requisito | Caso de uso | Módulo / clase | Endpoint o ruta | Caso de prueba |
| --- | --- | --- | --- | --- |
| RF01 | CU-02 | `PrivacyPolicyComponent`, `TermsOfUseComponent` | `/legal/privacy`, `/legal/terms` | NA. No hay spec de las páginas legales. |
| RF02 | CU-01 | `AuthService`, `AuthController` | `POST /api/auth/login` | CP-01 `AuthServiceTest` |
| RF03 | CU-01, CU-10 | `login-page.component.ts` | Navegación a `/professional` o al hogar | CP-01 |
| RF04 | CU-02 | `AuthService.registerFamily` | `POST /api/auth/register-family` | CP-04 `AuthServiceTest` |
| RF05 | CU-02 (alterno) | Registro en modo voucher | `POST /api/auth/register` | CP-04 |
| RF06 | CU-02b | `AuthService`, `RefreshTokenService` no; sí el flujo de reset | `POST /api/auth/forgot-password`, `POST /api/auth/reset-password` | `EmailServiceTest`, `AuthServiceTest` |
| RF07 | CU-01 | `AuthController`, menú del shell | `POST /api/auth/logout` | NA de interfaz. [Pendiente: spec del cierre de sesión.] |
| RF08 | CU-01 | `authGuard`, `AuthMeController` | `GET /api/auth/me` | CP-03 `auth.guard.spec.ts`, `AuthMeControllerTest` |
| RF09 | CU-02, CU-03 | `Family`, `FamilyController` | `/families`, `/families/create` | Ciclo en CP-10 |
| RF10 | Todos los CU familiares | `SecurityValidator`, `FamilySecurityEvaluator` | Comprobación en servicios | CP-06 `SecurityValidatorTest` |
| RF11 | CU-03 | `MemberService`, `FamilyMember` | API de miembros, `/members` | CP-07 `MemberServiceTest` |
| RF12 | CU-03 | `InvitationService` | Invitación de miembro | CP-08 `InvitationServiceTest` |
| RF13 | CU-01 | `ProfilePageComponent` | `/profile` | NA |
| RF14 | — | `GuardianService` | `/guardian/:familyId/election` | CP-09 `GuardianIntegrationTest` |
| RF15 | — | `adminGuard`, controlador de familias | `/admin/families` | CP-05 `admin.guard.spec.ts` |
| RF16 | — | `TerritorialReportComponent` | `/families/:id/report` | NA |
| RF17 | CU-04 | `AssessmentController`, `EvaluationController` | `POST /api/assessments/start`, `/evaluations/start` | CP-12 |
| RF18 | CU-04 | `rutaConcienciaDomain.ts`, formulario de evaluación | `/evaluations/:id/form` | CP-12 |
| RF19 | CU-04 | Banco `SCENARIO_V1_2`, `RiskAlgoV1Engine` | Respuestas de evaluación | CP-13 `RiskAlgoV1EngineTest` |
| RF20 | CU-04 | `EvaluationScoringService` | `POST` de finalización | CP-10 `FamilyLifecycleIntegrationTest` |
| RF21 | CU-04, CU-10 | Historial y evolución | `/evaluations/history`, `/evaluations/evolution` | CP-12 |
| RF22 | CU-10 | `IcafController`, `ObservatoryController` | `/capital`, `/capital/observatory` | NA de pantalla. Servicios de capital si existen en test. |
| RF23 | — | `AssessmentController` | `GET /api/assessments/resilience-check` | NA |
| RF24 | CU-04 | `RiskService` | Historial de riesgo | CP-11 `EvaluationRiskIntegrationTest` |
| RF25 | CU-08 | `RiskTrajectory`, `TrajectoryService` | `/trajectory` | CP-15 `TrajectoryServiceTest` |
| RF26 | CU-08 | `FamilyRiskTrajectory` | Asignación de trayectoria | CP-15 |
| RF27 | CU-08 | `SafetyProtocolService` | `POST /api/trajectories/family/{id}/safety-protocol` | CP-14 `SafetyProtocolServiceTest` |
| RF28 | CU-08 | `SafetyProtocolService` | `POST .../safety-protocol/{id}/close` | CP-14 |
| RF29 | — | `SubtleSignalRadarService` | `GET /api/families/{id}/radar`, `/radar` | CP-16 |
| RF30 | CU-05 | `PlanGenerationService` | Generación de `plans` | CP-17 `PlanGenerationServiceTest` |
| RF31 | CU-05 | `ImprovementPlan`, `V111` | Aceptación del plan, `/plans` | CP-17, CP-20 |
| RF32 | CU-05 | `PlanTaskService` | `/plans`, `/plans/mission/:taskId` | CP-20 |
| RF33 | CU-05, CU-07 | `PlanService.completeTask` | Completar tarea | CP-18 `PlanServiceTest` |
| RF34 | CU-05 | `AdaptivePlanService`, `AdaptiveController` | `/api/families/{id}/adaptive/*` | CP-19 |
| RF35 | CU-05 | `PlanController` | Generación determinista | CP-17 |
| RF36 | CU-06 | `SprintService` | `/sprint`, alta de sprint | CP-21 `SprintServiceTest` |
| RF37 | CU-06 | `SprintService` | `PUT /api/sprints/{sprintId}/missions/{missionId}/status` | CP-21, CP-22 |
| RF38 | CU-06 | `SprintDaily`, `V114` | Daily del sprint | CP-21, CP-23 |
| RF39 | CU-06 | `SprintRetrospective` | Cierre del sprint | CP-21 |
| RF40 | CU-12 | `FamilyLogbookComponent` | `/logbook` | CP-23 |
| RF41 | CU-12 | `JournalService` | Diario y `/my-space` | CP-24 `JournalServiceInterpretationTest` |
| RF42 | CU-07 | `TaskEvidenceService` | `/checklist`, `/evidence/capture` | CP-26 `TaskEvidenceServiceTest` |
| RF43 | CU-07 | `EvidenceAnalysisConsumer` | Comentario de IA sobre la evidencia | CP-25 `EvidenceAnalysisConsumerTest` |
| RF44 | CU-07 | `DocumentaryProductionService` | `/documentary-maker` | CP-27, paso de CP-10 |
| RF45 | CU-07 | Checklist | `POST /api/checklist/generate-from-plan` | NA específica |
| RF46 | HU-14 | `FamilyHomeController` | `/family-home`, `GET /api/v1/families/{id}/home` | CP-28 |
| RF47 | — | `TransformationRouteComponent`, hitos sembrados | `/transformation/route` | NA |
| RF48 | — | `WeeklyPlanService` | `/transformation/weekly-plan` | `WeeklyPlanServiceTest` |
| RF49 | — | `ErrorProtocolService` | `/transformation/error-protocol` | Pruebas del paquete `errorprotocol` |
| RF50 | CU de vitalidad | `VitalityService` | `/vitality` | CP-29 `VitalityServiceTest` |
| RF51 | CU-11 | `BiographyService` | `/biography` | CP-30 `BiographyServiceTest` |
| RF52 | — | `LineageService` | `/lineage` | CP-31 `LineageServiceTest` |
| RF53 | — | `LegacyService`, `FamilyDnaService` | `/legado`, `/family-dna` | NA unificada en esta matriz |
| RF54 | — | Timeline, árbol, película | `/family-timeline`, `/family-tree`, `/family-movie` | `FamilyTimelineServiceTest`, `FamilyMovieServiceTest` |
| RF55 | — | Ritual, gratitud, pulso, consejo | Rutas del bloque legado y sistema | `RitualEngineServiceTest`, `ParticipationServiceTest` |
| RF56 | — | `DigitalTwinService` | `/digital-twin` | NA en el plan numerado |
| RF57 | CU-10 | `ProfessionalDashboardComponent` | `/professional` | CP-32 |
| RF58 | CU-10 | `ProfessionalFollowUpDraftService` | Borrador de seguimiento | CP-32 |
| RF59 | CU-09 | `ConsentService` | `POST/GET /api/families/{id}/consents` | CP-33 `ConsentServiceTest` |
| RF60 | CU-09 | `CanonicalFamilyRecordAssembler` | `GET .../interop/canonical-record` | Pruebas de mapeadores en `interop` |
| RF61 | CU-09 | `FhirBundleAssembler`, `ObservationFhirMapper` | `GET .../interop/fhir-bundle` | CP-34 |
| RF62 | — | `EcosystemService` | `/ecosystem` | NA de pantalla |
| RF63 | — | `SupportNetworkService` | `POST /api/support/professionals` | CP-35 `SupportNetworkServiceTest` |
| RF64 | — | `AnalyticsService` | `/dashboard` | NA de pantalla |
| RF65 | — | `PdfExportService`, `ExcelExportService` | `/reports` | CP-36 |
| RF66 | — | `ChatController` | `/chat`, `POST /api/chat` | CP-38 `ChatControllerTest` |
| RF67 | — | `SecurityConfig` regla `/api/admin/**` | `/admin/*` | CP-05, CP-37 |
| RF68 | — | `AlexaOAuthController`, `AlexaSkillController` | `/oauth/alexa/**`, `POST /alexa` | `AlexaOAuthServiceTest` |
| RNF07–RNF10 | CU-01 | `SecurityConfig`, `AccountLockService` | Cadena JWT | CP-02 `AccountLockServiceTest` |
| RNF16 | — | Perfil Maven `ci`, JaCoCo 65 % | `mvn verify -P ci` | [Pendiente: informe de la corrida] |

## Cómo se construyó

Se leyeron las plantillas exportadas de Notion, se conservaron sus secciones y sus tablas, y se reemplazó el ejemplo (SIGEF, aprendices, compras) por lo que está en el código. No se ejecutó la suite de pruebas en esta entrega. El frontend sí se sirvió en local el 16/11/2026 y se capturaron las pantallas públicas; las autenticadas siguen pendientes porque no se levantó el backend.

## Pendientes que debe completar William

1. [Pendiente: fecha de aprobación del instructor] y [Pendiente: nombre del instructor], en el historial de cada documento.
2. [Pendiente: cargo o rol dentro del equipo de formación] de William Lopez.
3. [Pendiente: nombres de los demás aprendices del equipo], con rol y cargo.
4. [Pendiente: centro de formación SENA], [Pendiente: programa y código de ficha] y [Pendiente: ciudad y regional].
5. [Pendiente: familia o institución piloto] y [Pendiente: profesionales reales del piloto].
6. [Pendiente: territorio concreto del piloto y población objetivo cuantificada.]
7. [Pendiente: justificación económica, tamaño de la muestra del piloto y comparación con productos del mercado.]
8. [Pendiente: política específica de menores y autorización del representante.]
9. [Pendiente: concepto del área jurídica del centro o de un profesional.]
10. [Pendiente: encuestas o entrevistas de levantamiento], informes escaneados, fotografías y canvas `PT-MC-01`.
11. [Pendiente: actas reales] (fecha, lugar, participantes, orden del día, desarrollo y compromisos) y el sprint o trimestre formativo de cada módulo.
12. [Pendiente: enlace Trello o Jira], [Pendiente: URL pública del repositorio] (el remoto observado es `Guillermo-prog-star/.-`; `CLAUDE.md` dice `if-full`) y [Pendiente: presentación y video de sustentación].
13. [Pendiente: URL pública vigente del frontend y host real de la API] (Render y Railway están ambos declarados).
14. [Pendiente: credencial de aula]. Rotar y sacar del fuente la contraseña de `MasterDataInitializer`. El README publica otro usuario de demostración.
15. [Pendiente: evidencia de cifrado en reposo AES-256 y de TLS 1.3.] El código muestra BCrypt y JWT. Ajustar la política de privacidad si el host no cumple lo que el texto afirma.
16. [Pendiente: proceso de supresión de cuenta a 30 días], o corrección del texto de `privacy-policy.component.html`.
17. [Pendiente: unificar `JPA_DDL_AUTO` de producción] (`update` frente a `validate`).
18. [Pendiente: política de cifrado y custodia de los backups], que contienen datos personales.
19. [Pendiente: codificación SNOMED, LOINC o CIE-10] por un codificador, y [Pendiente: consumidor SISPRO o IPS].
20. [Pendiente: ejecutar `mvn verify -P ci` y las pruebas del frontend], y pegar el resultado en el registro del PT-PP-03.
21. [Pendiente: capturas de las pantallas autenticadas] (hogar, evaluación, plan, sprint, panel profesional). Las públicas —login, registro en «Unirse a familia», recuperación y política— están en `docs/sena/img/`.
22. [Pendiente: acuerdo de nivel de servicio] (tiempos de respuesta y concurrencia), navegadores certificados, RAM mínima y metodología que exija el centro (el repo no declara RUP ni Scrum con actas).
23. [Pendiente: decidir si el correo institucional fijo de `admin.guard.ts` sigue en el producto.]
24. [Pendiente: valores de `.env` que entregue el instructor.] No se documentan secretos.
25. [Pendiente: cuadro comparativo de motores de base de datos] firmado, si la actividad del centro lo exige además de la justificación ya escrita.
26. [Pendiente: accesos a Railway, Render, Vercel y Anthropic.]
