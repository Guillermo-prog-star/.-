# PT-ERS-02. Especificación de Requisitos Integrity Family

## Nombre del Proyecto

Integrity Family

## Número de versión

1.0 — ingeniería inversa de la rama `feature/v1.2-micro-simulations` (commit `0d3cfef`), 16/11/2026.

Este documento define el sistema en cuanto a características, actores, módulos y requisitos funcionales y no funcionales observados en el código. El código oficial 2026 de la plantilla es PT-ERS-02.

**HISTORIAL DE REVISIÓN**

| Versión | Fecha Elaboración | Responsable Elaboración | Fecha Aprobación | Responsable Aprobación |
| --- | --- | --- | --- | --- |
| 1.0 | 16/11/2026 | William Lopez | [Pendiente: fecha de aprobación del instructor] | [Pendiente: nombre del instructor] |

**CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR**

| **VERSIÓN** | **MODIFICACIÓN RESPECTO VERSIÓN ANTERIOR** |
| --- | --- |
| 1.0 | Creación del documento a partir de controladores, entidades, rutas Angular, seguridad y migraciones Flyway. |

## Tabla de Contenido.

1. Introducción
2. Características del producto
3. Características del usuario o actores
4. Módulos y funciones del producto
5. Especificación de requisitos
6. Restricciones del software
7. Anexos

## 1. Introducción

Integrity Family es una plataforma web de acompañamiento familiar. Este documento traduce a requisitos lo que el software ya hace: autenticar personas, administrar un núcleo, evaluar la cohesión, proponer un plan, sostener sprints, registrar evidencia y memoria, y abrir un protocolo de seguridad solo con confirmación humana. La fuente es el código de `backend/` e `if-frontend/`, no un taller de requisitos con el cliente. Donde el código no permite afirmar un comportamiento, el requisito no se inventa y el vacío queda en la sección de pendientes del índice.

### 1.1 Responsables e Involucrados

| **Nombre** | **Tipo (Responsable/ Involucrado)** | **Rol** | Cargo |
| --- | --- | --- | --- |
| William Lopez | Responsable | Autor registrado y administrador sembrado | [Pendiente: cargo] |
| [Pendiente: equipo de aprendices] | Responsable | [Pendiente: rol] | [Pendiente: cargo] |
| [Pendiente: instructor] | Involucrado | Aprobación | Instructor |

### 1.2 Referencias (Bibliografía o web Grafía)

| Nombre | Descripción | Link Referencia |
| --- | --- | --- |
| Visión del producto | Definiciones de ICF, familia, hogar y regla de la IA. | `docs/vision.md` |
| Seguridad HTTP y JWT | Rutas públicas, roles y filtro JWT. | `backend/src/main/java/com/integrityfamily/security/config/SecurityConfig.java` |
| Propiedad del núcleo | Reglas de acceso a una familia. | `backend/src/main/java/com/integrityfamily/common/security/SecurityValidator.java` |
| Rutas de pantalla | Mapa de navegación real. | `if-frontend/src/app/app.routes.ts` |
| Escala de respuesta | Ruta de Conciencia. | `if-frontend/src/domain/constants/rutaConcienciaDomain.ts` |
| Migraciones | Esquema V1 a V115, con huecos V61 y V88. | `backend/src/main/resources/db/migration/` |
| ADR-019 | La IA propone; la familia decide. | `docs/adr/ADR-019-ia-propone-familia-decide.md` |

## 2. Características del producto

- El sistema es una aplicación web. El frontend es Angular 17.3 con componentes standalone (`if-frontend/package.json`) y el backend es una API REST Spring Boot 3.4.3 (`backend/pom.xml`).
- El lenguaje de compilación declarado en Maven es Java 17. La imagen Docker ejecuta Temurin 21 (`backend/Dockerfile`).
- La persistencia es relacional, MySQL 8.4, con Flyway activo en el perfil por defecto (`application.yml`, `docker-compose.yml`).
- La autenticación es JWT sin sesión de servidor. Las contraseñas de usuario se almacenan con BCrypt (`SecurityConfig.passwordEncoder`).
- Hay siete nombres de rol sembrados: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_FAMILY_ADMIN`, `ROLE_FAMILY_MEMBER`, `ROLE_THERAPIST`, `ROLE_ORIENTADOR` y `ROLE_SOCIAL_WORKER`.
- El producto calcula el ICF, genera planes, conduce sprints, registra evidencias, construye memoria familiar y expone un registro canónico y un bundle FHIR de solo lectura.
- La mensajería RabbitMQ existe y en los perfiles `railway` y `render` el listener arranca desactivado por defecto (`RABBITMQ_ENABLED`).
- El proveedor de IA es intercambiable (`AiProviderSelector` en `docs/architecture.md`). Claude es el proveedor descrito como principal.
- Hay integración opcional con Alexa (`/oauth/alexa`, `POST /alexa`), declarada pública en la cadena de seguridad.
- La documentación de API OpenAPI está expuesta fuera del perfil de producción (`springdoc` en `SecurityConfig`).

## 3. Características del usuario o Actores

Un actor es un rol con el que una persona u otro sistema entra a la herramienta. `ROLE_FAMILY_MEMBER` se siembra en base de datos y no se encontró asignación en la lógica Java revisada: el miembro de la familia es la entidad `FamilyMember`, que puede vincularse a un `User`.

| **ACTOR** | **DESCRIPCIÓN** | **INTERACCIONES ESPERADAS** |
| --- | --- | --- |
| Visitante | Persona no autenticada. | Ver política de privacidad y términos; abrir login, registro y recuperación de contraseña. |
| Administrador familiar | Usuario creado en `POST /api/auth/register-family` con `ROLE_FAMILY_ADMIN`. | Crear el núcleo, registrar miembros, evaluar, aceptar el plan, conducir el sprint, registrar evidencia y otorgar consentimientos. |
| Miembro de familia | Persona registrada en `family_members`, con usuario propio cuando completa la invitación. En Spring suele operar como `ROLE_USER` si no recibe otro rol. | Responder evaluación, daily, diario, biografía y evidencias del núcleo al que pertenece. |
| Administrador de plataforma | Usuario con `ROLE_ADMIN`. El sembrado crea `william@integrity.family`. El frontend trata el rol `ADMIN` y el correo `william_lopezb@soy.sena.edu.co` como acceso de administración. | Listar familias, estadísticas, reglas EEDSL, sandbox, monitor de voz y operaciones bajo `/api/admin/**`. El `SecurityValidator` le concede acceso a cualquier familia. |
| Profesional de apoyo | `ROLE_THERAPIST`, `ROLE_ORIENTADOR` o `ROLE_SOCIAL_WORKER`, asignados al registrar un profesional o al consentir un vínculo de ecosistema. El frontend los marca `isProfessional`. | Abrir el panel profesional, observar una familia asignada y redactar notas o borradores de seguimiento. |
| Sistema de IA | Componente interno (`ClaudeAiService` y otros proveedores). No es un usuario de pantalla. | Proponer texto de plan, chat e inferencias. No califica evidencias (ADR-019) ni activa el protocolo de seguridad. |
| Alexa | Cliente externo de las rutas públicas `/oauth/alexa/**` y `POST /alexa`. | Autorizar la skill y enviar peticiones de la skill. |
| Consumidor FHIR | Sistema externo que leería el bundle. No hay un cliente del Ministerio implementado. | Consultar `GET /api/families/{id}/interop/fhir-bundle` si tiene acceso a la familia. [Pendiente: consumidor real SISPRO o IPS.] |

## 4. Módulos y Funciones del producto

Los identificadores M01–M11 agrupan paquetes reales. El conteo de requisitos es el de la sección 5.

| **ID** | **MÓDULO** | **DESCRIPCIÓN** | **# RF** |
| --- | --- | --- | --- |
| M01 | Acceso, sesión y textos legales | Login, registro de familia, ingreso con voucher, recuperación de contraseña, cierre de sesión, bloqueo por intentos y páginas legales. Paquete `auth` y rutas `auth/*`, `legal/*`. | 8 |
| M02 | Núcleo familiar, miembros y guardián | Alta y selección de familia, miembros, invitación, perfil y elección de guardián. Paquetes `family`, `member`, `guardian`. | 8 |
| M03 | Evaluación, ICF e ICaF | Inicio, respuesta, cierre, historial, evolución, escenarios `SCENARIO_V1_2` y cuestionario de capital familiar. Paquetes `evaluation`, `assessment`, `capital`. | 7 |
| M04 | Riesgo, trayectorias y protocolo de seguridad | Nivel de riesgo, banco de  trayectorias, asignación, activación manual y radar de señales. Paquetes `risk`, `trajectory`, `scanner`. | 6 |
| M05 | Plan de mejora y ajuste adaptativo | Generación, aceptación, tareas y aprobación de ajustes. Paquete `plan` y `adaptive`. | 6 |
| M06 | Sprint, bitácora y diario | Sprint de 7 o 15 días, misiones, daily, retrospectiva, bitácora, Mi Espacio y diario con interpretación separada. Paquetes `bitacora`, `myspace`. | 6 |
| M07 | Evidencias, checklist y documental | Evidencia, lista de verificación y documental. Paquetes `checklist`, `documentary`. | 4 |
| M08 | Hogar digital, ruta y vitalidad | Hogar como raíz, ruta de 36 meses, planeación, protocolo de error (distinto del de seguridad) y vitalidad diaria. Paquetes `familyhome`, `transformation`, `weeklyplan`, `errorprotocol`, `vitality`. | 5 |
| M09 | Memoria y legado | Biografía, linaje, legado, ADN, línea de tiempo, película, rituales, gratitud, árbol y consejo. | 6 |
| M10 | Acompañamiento, consentimiento e interoperabilidad | Panel profesional, notas, consentimientos, registro canónico, FHIR y ecosistema. | 7 |
| M11 | Analítica, reportes, chat y administración | Tablero, exportación, chat, centro de documentación y herramientas de administración. | 5 |
| **TOTAL** |  |  | 68 |

## 5. Especificación de requisitos

El backend es un monolito modular: cada módulo es un paquete Java dentro del mismo proceso, no un microservicio. Los requisitos dicen lo que el sistema hace hoy. El criterio de tipo es el de la plantilla:

- **Esencial:** sin esa función el flujo familiar principal no opera.
- **Ideal:** mejora el acompañamiento y el sistema base sigue usable si falla.
- **Opcional:** integración o herramienta de valor agregado.

### 5.1 Requisitos funcionales

#### 5.1.1 Requisitos Funcionales **M01 – Acceso, sesión y textos legales**

Controla quién entra. Las rutas públicas de autenticación están en `SecurityConfig`. El filtro de la cadena responde 401 y 403 en inglés (`Unauthorized`, `Access Denied`). El manejador de negocio responde en español.

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF01 | Consultar textos legales sin sesión | El sistema muestra la política de privacidad y los términos en `/legal/privacy` y `/legal/terms` sin token. | Visitante | Esencial |
| RF02 | Iniciar sesión | `POST /api/auth/login` valida correo y contraseña. Si las credenciales fallan, el backend puede responder «Credenciales inválidas» y el login del frontend muestra «Credenciales incorrectas.» Tras varios fallos la cuenta queda bloqueada (`accountLockedUntil`, `FailedLoginAttempt`). | Visitante que ya tiene cuenta | Esencial |
| RF03 | Redirigir según tipo de usuario | Tras el login, si `isProfessional` es verdadero el frontend va a `/professional`; en caso contrario entra al flujo familiar (`login-page.component.ts`). | Administrador familiar, miembro, profesional | Esencial |
| RF04 | Registrar una familia nueva | `POST /api/auth/register-family` crea usuario con `ROLE_FAMILY_ADMIN`. El formulario exige nombre, correo, contraseña con mayúscula, minúscula y dígito (mínimo 8), confirmación, nombre de familia, municipio, departamento y aceptación de términos. | Visitante | Esencial |
| RF05 | Unirse a una familia con voucher | El registro en modo voucher envía el código de ingreso. Si el servidor rechaza la operación, el frontend muestra el mensaje del backend o «Error en el servidor. Verifica el Voucher.» | Visitante | Esencial |
| RF06 | Recuperar la contraseña | `POST /api/auth/forgot-password` y `POST /api/auth/reset-password`. El enlace expira o se invalida al usarse (`AuthService`: «El enlace de recuperación ha expirado o ya fue utilizado»). La nueva contraseña debe cumplir el patrón de 8 caracteres con mayúscula, minúscula y dígito. | Visitante | Esencial |
| RF07 | Cerrar sesión | `POST /api/auth/logout` y la acción «Cerrar Sesión» del menú, con confirmación «¿Finalizar sesión?». | Cualquier autenticado | Esencial |
| RF08 | Consultar la sesión actual | `GET /api/auth/me` devuelve el usuario del token. El guard `authGuard` envía a `/auth/login` si no hay token. | Cualquier autenticado | Esencial |

#### 5.1.2 Requisitos Funcionales **M02 – Núcleo familiar, miembros y guardián**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF09 | Crear y seleccionar familia | Pantallas `/families` y `/families/create`. La familia guarda nombre, código único, PIN, municipio, departamento, país, WhatsApp e hito actual. Al crearse, el hito inicial es `W1` (`Family.prePersist`). | Administrador familiar, administrador de plataforma | Esencial |
| RF10 | Restringir el acceso al núcleo | `SecurityValidator.validateFamilyOwnership`: sin sesión responde «No autenticado»; `ROLE_ADMIN` pasa; el creador pasa; un miembro activo del mismo `familyId` pasa; si no, «No tienes permisos sobre este núcleo familiar», «Acceso denegado» o «Cuenta inactiva». | Todos los autenticados | Esencial |
| RF11 | Registrar un miembro | Alta con nombre obligatorio. Puede guardar correo, teléfono, rol familiar, edad, fecha de nacimiento y documento (`document_type`, `document_number`, únicos cuando ambos vienen). El frontend exige el nombre y, para el teléfono, 10 dígitos. | Administrador familiar | Esencial |
| RF12 | Invitar a un miembro | `MemberController` expone invitación. El detalle de plantilla de correo depende de `SPRING_MAIL_*`. | Administrador familiar | Ideal |
| RF13 | Mantener el perfil propio | Ruta `/profile`. Muestra la cuenta de la sesión. | Cualquier autenticado | Esencial |
| RF14 | Elegir guardián familiar | Ruta `/guardian/:familyId/election`. La familia guarda `guardianMemberId` y `guardianSince`. Hay votos en `guardian_votes`. | Miembros del núcleo | Ideal |
| RF15 | Listar familias para administración | `/admin/families` protegido por `adminGuard`. `GET` de familias con rol de administrador en el controlador de familia. | Administrador de plataforma | Esencial |
| RF16 | Ver el reporte territorial de una familia | Ruta `/families/:id/report`. | Administrador familiar, administrador de plataforma | Ideal |

#### 5.1.3 Requisitos Funcionales **M03 – Evaluación, ICF e ICaF**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF17 | Iniciar una evaluación | `/evaluations/start` exige elegir un pilar. El backend abre la evaluación de la familia (`AssessmentController` `POST /start` con comprobación de familia). Si no hay identificador, el frontend dice «No se recibió ID de evaluación.» | Miembro, administrador familiar | Esencial |
| RF18 | Responder con la Ruta de Conciencia | Una sola escala de cinco textos de conciencia para las cuatro dimensiones del ICF. El estado interno no se muestra. El modo `NEURO_AWARENESS` usa otras cinco opciones centradas en la señal corporal y no se unificó con esta escala (`CLAUDE.md`, `rutaConcienciaDomain.ts`). | Miembro | Esencial |
| RF19 | Responder micro-simulaciones | El banco `SCENARIO_V1_2` (migraciones V89–V95) usa fases `NOTICE`, `THINK`, `ACT`, `AFTERMATH` y `EFFECT`, con `phase_prompt` y `rubric_level`. La directriz V1.2 congela esas fases durante el piloto. | Miembro | Esencial |
| RF20 | Cerrar la evaluación y obtener el ICF | `POST` de finalización calcula el ICF de 0 a 100 y puntajes por dimensión. El frontend muestra «Error al finalizar el diagnóstico. Intenta de nuevo.» si falla. | Miembro, administrador familiar | Esencial |
| RF21 | Consultar historial, evolución e inferencias | Rutas `/evaluations/history`, `/evaluations/evolution`, `/evaluations/inferences` y `/evaluations/analytics`. | Miembro, profesional en modo observación | Esencial |
| RF22 | Consultar capital familiar ICaF | `/capital`, cuestionario `/capital/questionnaire` y observatorio `/capital/observatory`. La generación manual del observatorio exige rol de administración. | Administrador familiar, profesional, administrador de plataforma | Ideal |
| RF23 | Consultar el chequeo de resiliencia público | `GET /api/assessments/resilience-check` es `permitAll`. | Visitante | Opcional |

#### 5.1.4 Requisitos Funcionales **M04 – Riesgo, trayectorias y protocolo de seguridad**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF24 | Calcular y consultar el riesgo | Snapshots de riesgo e historial por familia. El nivel acompaña al ICF y no lo reemplaza. | Administrador familiar, profesional | Esencial |
| RF25 | Consultar el banco de trayectorias | Tabla `risk_trajectories`: código, nombre, macrodominio, señales, evolución y bandera `requires_safety_protocol` (`V75`, `V97`). Pantalla `/trajectory`. | Autenticado con acceso a la familia | Esencial |
| RF26 | Asignar una trayectoria a la familia | `family_risk_trajectories` relaciona el núcleo con una trayectoria del banco, con estado y fechas. | Administrador familiar, profesional | Ideal |
| RF27 | Activar el protocolo de seguridad a mano | `POST /api/trajectories/family/{id}/safety-protocol`. Exige responsable miembro, acción inicial y fecha de seguimiento (`V98`). No hay activación automática. | Persona con acceso a la familia, confirmación humana | Esencial |
| RF28 | Cerrar el protocolo | `POST .../safety-protocol/{activationId}/close` con notas de cierre. | Responsable o quien `canCloseSafetyProtocol` autorice | Esencial |
| RF29 | Consultar el radar de señales | `GET /api/families/{familyId}/radar` y pantalla `/radar`. | Administrador familiar, profesional | Ideal |

#### 5.1.5 Requisitos Funcionales **M05 – Plan de mejora y ajuste adaptativo**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF30 | Proponer un plan | El servicio de generación arma un `ImprovementPlan` ligado a la familia y, si existe, a la evaluación. El informe de IA se guarda en `aiReport`. La IA propone; no decide (ADR-019). | Sistema de IA, administrador familiar | Esencial |
| RF31 | Aceptar el plan | `V111` agrega aceptación sobre `plans` (`acceptedAt`, `acceptedBy`, declaración de intención). Hasta esa migración el plan se activaba sin confirmación. | Administrador familiar | Esencial |
| RF32 | Consultar tareas del plan | `plan_tasks` guarda título, dimensión, responsable miembro, hito y vencimiento. Pantalla `/plans` y detalle `/plans/mission/:taskId`. | Miembro, administrador familiar | Esencial |
| RF33 | Completar una tarea sin fabricar evidencia | Cerrar la tarea no crea una evidencia. La evidencia es un registro aparte (`task_evidences`, ADR-019, commit de corrección del plan). | Miembro | Esencial |
| RF34 | Aprobar, rechazar o aplicar un ajuste | Endpoints de `AdaptiveController`: listar, evaluar, aprobar, aplicar y rechazar. El ajuste no se aplica solo. | Administrador familiar | Ideal |
| RF35 | Generar un plan determinista | Existe generación determinista además de la ruta de IA (`PlanController`, según el inventario de endpoints). | Administrador familiar | Ideal |

#### 5.1.6 Requisitos Funcionales **M06 – Sprint, bitácora y diario**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF36 | Crear un sprint de 7 o 15 días | `SprintService` solo acepta esas dos duraciones (`CLAUDE.md`). Pantalla `/sprint` y bitácora `/logbook`. | Administrador familiar | Esencial |
| RF37 | Actualizar el estado de una misión | Estados `PENDING`, `COMPLETED`, `CHANGED_BY_AGREEMENT` y `POSTPONED`. Cambiada por acuerdo y pospuesta salen del denominador de cumplimiento. Endpoint `PUT /api/sprints/{sprintId}/missions/{missionId}/status`. | Miembro, administrador familiar | Esencial |
| RF38 | Registrar el daily | `sprint_dailies.attempt_status`: `NO_OPPORTUNITY`, `NOT_ATTEMPTED`, `ATTEMPTED_UNFAVORABLE`, `ATTEMPTED_FAVORABLE`; NULL es no respondido (`V114`). El tablero muestra «Daily registrado. ¡Gracias por tu conciencia!» o «Error al guardar el daily.» La bitácora avisa «Ya registraste tu Daily el día de hoy.» | Miembro | Esencial |
| RF39 | Cerrar el sprint con retrospectiva | `sprint_retrospectives` guarda lo que salió bien y el nivel de tensión. El tablero confirma «Sprint cerrado. ¡Excelente trabajo en equipo!» | Administrador familiar | Esencial |
| RF40 | Escribir la bitácora familiar | `/logbook` registra situación, emoción y comprensión (`family_logbook_entries`). Exige familia asociada y campos marcados. | Miembro | Esencial |
| RF41 | Escribir el diario privado y separar interpretación | `/my-space` usa el diario privado. `journal_entries` distingue hecho, interpretación, explicación alternativa, señal corporal y respuestas explícitas (`V115`, ADR-018). La visibilidad por defecto es privada. | Miembro | Esencial |

#### 5.1.7 Requisitos Funcionales **M07 – Evidencias, checklist y documental**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF42 | Registrar una evidencia | Tipos de evidencia con título, texto, archivo, emoción y coordenadas opcionales. `task_id` es nullable desde `V65`. Pantallas `/checklist`, `/evidence/capture` y `/evidence/documentary`. | Miembro | Esencial |
| RF43 | Revisar evidencias | El checklist permite enviar, validar o rechazar según el controlador de evidencias. La IA comenta y no asigna calificación (ADR-019, `EvidenceAnalysisConsumerTest`). | Administrador familiar, sistema de IA | Esencial |
| RF44 | Producir un documental | Estados de producción hacia publicación (`documentary_productions`, `family_documentaries` desde `V66`). Pantalla `/documentary-maker`. | Administrador familiar | Ideal |
| RF45 | Derivar ítems de checklist desde el plan | El README documenta `POST /api/checklist/generate-from-plan`. | Administrador familiar | Ideal |

#### 5.1.8 Requisitos Funcionales **M08 – Hogar digital, ruta y vitalidad**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF46 | Abrir el Hogar Digital | Ruta por defecto tras autenticarse: `/family-home`. `GET /api/v1/families/{familyId}/home`. Etapas de incorporación, evaluación, activo o en pausa. ADR-002 lo define como raíz de la experiencia. | Administrador familiar, miembro | Esencial |
| RF47 | Ver la ruta de 36 meses | `/transformation/route`. Hitos sembrados de `W1` a `M36` con duraciones en `MasterDataInitializer`. | Administrador familiar | Ideal |
| RF48 | Registrar la planeación | `/transformation/weekly-plan` y tablas `weekly_plans`, `weekly_tasks`. | Administrador familiar | Ideal |
| RF49 | Abrir el protocolo de error de una misión | `/transformation/error-protocol` y `family_error_protocols`. Es el ritual ante una misión fallida. No sustituye el protocolo de seguridad de `V98` (`V99`). | Administrador familiar | Ideal |
| RF50 | Registrar vitalidad diaria | `/vitality` y `daily_vitality_logs` (`V110`): horas de sueño, calidad, ejercicio, nutrición, pantallas y fatiga, todos anulables. ADR-009. | Miembro | Ideal |

#### 5.1.9 Requisitos Funcionales **M09 – Memoria y legado**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF51 | Responder la biografía guiada | Catálogo global de preguntas por capítulos (`V113`, sembrado en Java, no en SQL). Una respuesta por miembro y pregunta. Autoría tomada del principal, no del cliente. Rutas `/biography` y `/biography/members/:memberId`. Borrar pide confirmación. | Miembro | Ideal |
| RF52 | Registrar linaje | `/lineage` y tablas `family_lineages`, miembros, eventos y relaciones. | Administrador familiar | Ideal |
| RF53 | Registrar legado y ADN | `/legado`, `/family-dna`. | Administrador familiar | Ideal |
| RF54 | Ver línea de tiempo, árbol y película | `/family-timeline`, `/family-tree`, `/family-movie`. | Miembro | Ideal |
| RF55 | Usar rituales, gratitud, pulso y consejo | `/rituals`, `/gratitude`, `/family-pulse`, `/family-council`. | Miembro | Opcional |
| RF56 | Consultar el gemelo digital | `/digital-twin`. Proyección a partir del estado familiar. No es un dispositivo médico. | Administrador familiar, profesional | Opcional |

#### 5.1.10 Requisitos Funcionales **M10 – Acompañamiento, consentimiento e interoperabilidad**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF57 | Usar el panel profesional | `/professional` lista familias asignadas y el perfil. En observación, el menú cambia a evolución, historial, ICaF, plan, salud, trayectorias y ecosistema. | Profesional | Esencial |
| RF58 | Redactar nota y borrador de seguimiento | `professional_follow_up_drafts` (`V107`) congela el vistazo de datos. `assignment_id` es polimórfico y no tiene llave foránea (`V108`, `V109`). Las notas aceptan terapeuta, orientador o administrador. | Profesional, administrador de plataforma | Ideal |
| RF59 | Otorgar y revocar consentimiento | `POST/GET /api/families/{id}/consents` y revocación. Propósitos: ecosistema, red de apoyo, interoperabilidad en salud e investigación. Cada alta o revocación genera auditoría. | Administrador familiar | Esencial |
| RF60 | Exportar el registro canónico | `GET /api/families/{id}/interop/canonical-record` ensambla personas, hogar, evaluaciones, riesgos y consentimientos. | Quien tenga acceso a la familia | Ideal |
| RF61 | Exportar bundle FHIR y traza de auditoría | `GET .../interop/fhir-bundle` (`application/fhir+json`) y `GET .../interop/fhir-audit-trail`. Los códigos clínicos propios de Integrity no se reemplazan; un segundo coding solo se agrega si `TerminologyService` tiene un mapa. El mapa está vacío a propósito. | Quien tenga acceso a la familia | Ideal |
| RF62 | Administrar el ecosistema de apoyo | `/ecosystem`. Crear participantes institucionales exige `ROLE_ADMIN`. Eliminar pide confirmación. | Administrador de plataforma, administrador familiar | Ideal |
| RF63 | Registrar profesionales de la red | `POST /api/support/professionals` exige `ROLE_ADMIN` y asigna el rol según la especialidad. | Administrador de plataforma | Ideal |

#### 5.1.11 Requisitos Funcionales **M11 – Analítica, reportes, chat y administración**

| **ID** | **NOMBRE** | **DESCRIPCIÓN** | **ROLES** | **TIPO (Esencial, Ideal, Opcional)** |
| --- | --- | --- | --- | --- |
| RF64 | Ver el tablero familiar | `/dashboard` consume analítica (`/api/analytics/...`): resumen, historial de ICF y dimensiones. | Administrador familiar | Ideal |
| RF65 | Exportar reportes | `/reports` y exportadores PDF y Excel (`reports`, iText 8.0.2 y POI 5.2.5). | Administrador familiar, administrador de plataforma | Ideal |
| RF66 | Conversar con el consultor | `/chat` y `POST /api/chat`. El contexto familiar entra al proveedor de IA. | Miembro | Ideal |
| RF67 | Operar herramientas de administración | `/admin/stats`, `/admin/eedsl`, `/admin/sandbox`, `/admin/voice-monitor`, analítica y auditoría bajo `/api/admin/**`. | Administrador de plataforma | Esencial |
| RF68 | Atender Alexa | Rutas públicas de OAuth y skill. Requiere credenciales `ALEXA_*` para operar de verdad. | Alexa | Opcional |

### 5.2 Requisitos no funcionales

Los valores que el repositorio no fija (tiempos de respuesta, usuarios concurrentes) no se inventan.

| **REQUISITO** | **DESCRIPCIÓN** |
| --- | --- |
| **USABILIDAD** |  |
| RNF01 | Los textos de validación de autenticación, miembros, sprint y diario están en español de la interfaz (`LoginRequest`, `RegisterFamilyRequest`, componentes de sprint y bitácora). |
| RNF02 | La Ruta de Conciencia muestra la frase de conciencia y oculta el estado interno. |
| RNF03 | El menú lateral agrupa la navegación familiar en bloques (configuración, diagnóstico, índices, plan, transformación, apoyo, legado y sistema) definidos en `sidebar.component.ts`. El profesional ve un menú reducido. |
| **CONFIABILIDAD** |  |
| RNF04 | Los errores de RabbitMQ no deben tumbar al llamador: los publicadores capturan el fallo (convención verificada en pruebas de evidencia, `CLAUDE.md`). |
| RNF05 | `family_action_executions` guarda la clave de idempotencia para no repetir la misma acción del Hogar Digital (`V102`). |
| RNF06 | Flyway aplica el esquema al arrancar. Hay 113 archivos, versiones V1 a V115, sin V61 ni V88. `ddl-auto` por defecto es `update` si no se define `JPA_DDL_AUTO`. |
| **SEGURIDAD** |  |
| RNF07 | Sesión stateless y JWT. El secreto sale de `JWT_SECRET` y no tiene valor por defecto en el YAML principal. La expiración por defecto es 86400000 ms. |
| RNF08 | Contraseñas de `users.password_hash` con BCrypt. El registro de familia exige complejidad mínima. |
| RNF09 | Autorización por ruta (`/api/admin/**` solo `ROLE_ADMIN`), por `@PreAuthorize` y por `SecurityValidator` en el servicio. |
| RNF10 | Bloqueo temporal de cuenta tras intentos fallidos, con mensaje «La cuenta está bloqueada temporalmente por múltiples intentos fallidos.» El frontend muestra «Tu cuenta ha sido bloqueada temporalmente por seguridad.» |
| **EFICIENCIA Y RENDIMIENTO** |  |
| RNF11 | [Pendiente: no hay un acuerdo de nivel de servicio ni un umbral de tiempo de respuesta en el código.] |
| RNF12 | El contexto FHIR (`FhirContext`) se declara como bean costoso y singleton (`FhirConfig`, descrito en `CLAUDE.md`). |
| RNF13 | La cobertura de líneas exigida en el perfil Maven `ci` es 65 % (`jacoco.line.coverage.min`). Eso es un umbral de prueba, no de rendimiento. |
| **PORTABILIDAD** |  |
| RNF14 | El backend se empaqueta en Docker (`backend/Dockerfile`) y el compose local publica MySQL en el puerto anfitrión 3307, RabbitMQ en 5672 y 15672, y la API en 8080. |
| RNF15 | El frontend se construye como estáticos con reescritura SPA en `if-frontend/vercel.json`. El script `start` usa el puerto 4300. |
| **MANTENIBILIDAD** |  |
| RNF16 | Hay 201 clases `*Test.java` en `backend/src/test/java` y 31 archivos `*.spec.ts` en el frontend, más pruebas de integración con perfil `integration-test`. |
| RNF17 | Las decisiones de diseño viven en `docs/adr/ADR-001` a `ADR-019`. |
| RNF18 | Las migraciones ya aplicadas no se editan. El siguiente número libre, con V115 como última, es V116. |
| **SOPORTABILIDAD** |  |
| RNF19 | Salud del proceso en `/actuator/health` e `/actuator/info`, públicas. |
| RNF20 | Los registros de contenedor del backend rotan a 10 MB por 3 archivos (`docker-compose.yml`). |
| **OPERATIVIDAD** |  |
| RNF21 | Despliegue descrito: API en Railway o Render (workflows `deploy-backend.yml` y `deploy-backend-render.yml` al hacer push a `main` con cambios en `backend/`), frontend en Vercel (`deploy-frontend.yml` al hacer push a `principal` con cambios en `if-frontend/`). |
| RNF22 | Zona horaria de la URL JDBC local: `America/Bogota` (`docker-compose.yml`). |

## 6. Restricciones del software

- Un solo plan activo por familia es la invariante que `V111` reutiliza; no hay historial de planes en otra tabla.
- El sprint solo dura 7 o 15 días.
- El protocolo de seguridad no se dispara solo. El protocolo de error de misión no sirve para un riesgo vital.
- `TerminologyService` no contiene códigos SNOMED, LOINC ni CIE-10. Un mapa inventado está prohibido por la documentación del proyecto.
- `ROLE_FAMILY_MEMBER` existe en el sembrado y no se asigna en el flujo Java revisado.
- La contraseña del administrador sembrado está en el código fuente de `MasterDataInitializer`. Debe rotarse fuera del repositorio antes de cualquier uso real. Este documento no la reproduce.
- `README.md` y `docs/architecture.md` contradicen en versiones de Java y de Angular a `pom.xml` y `package.json`. El comportamiento de compilación lo fijan los archivos de construcción.
- No hay cifrado de columnas verificado para el contenido emocional. La política de privacidad afirma AES-256 y TLS 1.3. [Pendiente: evidencia de cifrado en reposo y de la versión de TLS en el proveedor de hosting.]
- No hay borrado automático a 30 días, aunque la política lo anuncia. [Pendiente: implementar o corregir el texto.]
- El secreto JWT, la clave de Claude y el secreto del Hogar Digital son obligatorios por variable de entorno.
- [Pendiente: restricciones de hosting del centro de formación, presupuesto y calendario académico.]

## 7. Anexos

| **ANEXO** | **DESCRIPCIÓN** |
| --- | --- |
| Anexo A. Índice SENA | `docs/sena/README.md`, con la matriz de trazabilidad. |
| Anexo B. Norma y privacidad | Texto de `privacy-policy.component.html` y sección 6 del PT-PP-01. |
| Anexo C. Decisiones de arquitectura | `docs/adr/` ADR-001 a ADR-019. |
| Anexo D. Modelo de datos | PT-MCBD-01 y migraciones Flyway. |
| [Pendiente: levantamiento con el cliente] | No hay actas, encuestas ni entrevistas en el repositorio. |
| [Pendiente: modelo de negocio Canvas] | La plantilla menciona `PT-MC-01-Canvas.docx`. Ese archivo no está en el repositorio. |
