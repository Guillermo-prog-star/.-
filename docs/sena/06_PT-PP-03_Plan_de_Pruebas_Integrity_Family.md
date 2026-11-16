# PT-PP-03. Plan de Pruebas Integrity Family

El presente documento organiza la validación de Integrity Family. El código oficial 2026 de la plantilla es PT-PP-03. Las pruebas que aquí se listan son las que ya existen en el repositorio. Esta entrega documental no ejecutó `mvn test` ni `ng test`; el resultado de una corrida concreta queda pendiente.

**HISTORIAL DE REVISIÓN**

| Versión | Fecha Elaboración | Responsable Elaboración | Fecha Aprobación | Responsable Aprobación |
| --- | --- | --- | --- | --- |
| 1.0 | 16/11/2026 | William Lopez | [Pendiente: fecha de aprobación del instructor] | [Pendiente: nombre del instructor] |

**CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR**

| **VERSIÓN** | **MODIFICACIÓN RESPECTO VERSIÓN ANTERIOR** |
| --- | --- |
| 1.0 | Creación del plan a partir de `backend/src/test/java` (201 clases `*Test.java`) y de 31 especificaciones Angular. |

## TABLA DE CONTENIDO

1. Introducción
2. Justificación
3. Objetivo
4. Alcance
5. Plan de pruebas
6. Casos de prueba del sistema por módulos

## 1. INTRODUCCIÓN

Integrity Family se prueba con JUnit 5, Mockito en modo estricto y AssertJ en el backend, y con especificaciones Angular en el frontend. El perfil `test` usa H2 en memoria, Flyway apagado y `ddl-auto=create-drop` (`application-test.yml`). El perfil `integration-test` usa MySQL real y RabbitMQ simulado. La integración continua corre el perfil Maven `ci`, publica JaCoCo y exige al menos 65 % de cobertura de líneas (`quality.yml`, `pom.xml`).

## 2. JUSTIFICACION.

El sistema guarda datos sensibles de convivencia y puede registrar señales de riesgo vital. Una regresión en el acceso a la familia, en el cálculo del ICF o en el protocolo de seguridad tendría efecto sobre personas reales. Las pruebas automatizadas fijan esas reglas: el administrador pasa, el extraño no; la IA comenta la evidencia y no la califica; completar una tarea no fabrica evidencia; el protocolo no se abre solo. Ejecutarlas antes de un despliegue reduce el riesgo de romper ese contrato. [Pendiente: actas de prueba con el instructor y pruebas de campo con familias.]

## 3. OBJETIVO

Comprobar, con las clases de prueba ya escritas, que los módulos esenciales cumplen los requisitos del PT-ERS-02: acceso, pertenencia al núcleo, evaluación, plan, sprint, evidencia, protocolo de seguridad y consentimiento.

## 4. ALCANCE

Entran las pruebas unitarias del backend, las tres pruebas de ciclo con perfil `integration-test` y las especificaciones Angular de acceso, hogar, evaluación, plan, checklist y bitácora.

No entra en esta corrida documental:

- La ejecución misma de Maven o de Karma. [Pendiente: pegar el informe de `mvn verify -P ci` y de las pruebas de frontend.]
- Pruebas de carga, de penetración y de usabilidad con usuarios. No hay guiones de ese tipo en el repositorio.
- `SonicIntegrationTest`, atado al perfil `sonic-test` y excluido del circuito normal.
- El cifrado en reposo y el borrado a 30 días, porque no hay prueba que los demuestre.

## 5. PLAN DE PRUEBAS

| **ID** | **MÓDULO** | **DESCRIPCIÓN** | OBJETIVO |
| --- | --- | --- | --- |
| M01 | Acceso, sesión y textos legales | Login, registro, bloqueo, JWT y guardas. | Validar credenciales, bloqueo, redirección y rechazo sin token. |
| M02 | Núcleo familiar, miembros y guardián | Pertenencia, miembros e invitación. | Verificar que solo creador, miembro activo o administrador tocan la familia. |
| M03 | Evaluación, ICF e ICaF | Cálculo, respuestas y ciclo de evaluación. | Verificar el cierre de la evaluación y la persistencia del riesgo asociado. |
| M04 | Riesgo, trayectorias y protocolo | Banco, sugerencia y activación manual. | Verificar que el protocolo exige datos humanos y no se dispara solo. |
| M05 | Plan de mejora | Generación, tareas y aceptación. | Verificar que la tarea completada no crea evidencia. |
| M06 | Sprint, bitácora y diario | Duración, estados de misión y daily. | Verificar duraciones 7 y 15, estados que no cuentan como incumplimiento y el daily. |
| M07 | Evidencias y documental | Alta de evidencia y comentario de IA. | Verificar el comentario sin calificación. |
| M08 | Hogar digital y vitalidad | Contrato del hogar y registro parcial. | Verificar el hogar autenticado y los campos nulos de vitalidad. |
| M09 | Memoria y legado | Biografía y perspectivas. | Verificar autoría del principal y campos de interpretación. |
| M10 | Consentimiento, profesional e interoperabilidad | Consentimiento, borrador y mapeadores. | Verificar otorgar, revocar y el bundle sin códigos clínicos inventados. |
| M11 | Analítica y administración | Reportes y seguridad de admin. | Verificar exportación y que `/api/admin/**` no queda abierto. |

Orden sugerido de ejecución: M01 y M02 antes que el resto, porque casi todos los casos incluyen la sesión y la pertenencia. Luego M03, M05, M06 y M07, que son el ciclo que cubre `FamilyLifecycleIntegrationTest`. M04 y M10 se corren con sus clases dedicadas. M11 cierra.

Comando de referencia, desde `backend/`:

```bash
mvn test
mvn verify -P ci
mvn test -Dtest=FamilyLifecycleIntegrationTest
```

El tercer comando necesita MySQL de compose en el puerto 3307 (`CLAUDE.md`). En el frontend, el script de pruebas es el de Angular sobre los `*.spec.ts`. [Pendiente: versión exacta del script npm de pruebas si el equipo usa un comando distinto de `ng test`.]

## 6. CASOS DE PRUEBA DEL SISTEMA POR MÓDULOS

La base de Notion de la plantilla no trae columnas en el Markdown exportado. Cada caso usa: identificador, requisito, clase de prueba que ya existe, precondición, qué se ejercita y resultado que la prueba protege. El estado de la última ejecución es pendiente porque esta entrega no lanzó la suite.

### M01 Acceso

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-01 | RF02 | `AuthServiceTest`, `AuthControllerTest` | Usuario sembrado en el doble de prueba | Login con clave correcta e incorrecta | Token solo si el hash coincide; mensaje de credenciales inválidas si no |
| CP-02 | RNF10 | `AccountLockServiceTest` | Intentos fallidos acumulados | Nuevo intento con la cuenta bloqueada | Se rechaza el acceso mientras `accountLockedUntil` está en el futuro |
| CP-03 | RF08 | `if-frontend/src/app/core/guards/auth.guard.spec.ts` | Sin token en el almacenamiento | Navegación a una ruta del shell | Redirección a `/auth/login` |
| CP-04 | RF04 | `register-page` spec y pruebas de `registerFamily` | Correo libre y datos completos | Alta de familia | Usuario con `ROLE_FAMILY_ADMIN` |
| CP-05 | RF67 | `admin.guard.spec.ts` | Rol distinto de `ADMIN` y correo distinto del institucional | Apertura de `/admin` | Navegación a `/dashboard` |

### M02 Núcleo

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-06 | RF10 | `SecurityValidatorTest`, `FamilySecurityEvaluatorTest` | Principal nulo, admin, creador, miembro y extraño | `validateFamilyOwnership` | Mensajes «No autenticado», paso del admin, y denegación del extraño |
| CP-07 | RF11 | `MemberServiceTest`, `MemberControllerSecurityTest` | Familia del actor | Alta y listado | El miembro queda en esa familia; otro actor no lista |
| CP-08 | RF12 | `InvitationServiceTest` | Miembro invitable | Invitación | El flujo de invitación no entrega la familia a un correo ajeno |
| CP-09 | RF14 | `GuardianIntegrationTest` (perfil `integration-test`) | MySQL de prueba | Votación y misión de guardián | Se persiste el guardián elegido |

### M03 Evaluación

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-10 | RF20, RF24 | `FamilyLifecycleIntegrationTest` | Perfil `integration-test` y MySQL | Familia, evaluación, plan, sprint, evidencia y documental | El ciclo de seis pasos termina con documental |
| CP-11 | RF24 | `EvaluationRiskIntegrationTest` | Perfil `integration-test` | Cálculo dinámico de riesgo | El snapshot persiste junto a la evaluación |
| CP-12 | RF18 | `evaluation-start-page` y `evaluation-history-page` specs | Formulario de inicio | Validación de pilar | No arranca la evaluación sin pilar |
| CP-13 | RF19 | `RiskAlgoV1EngineTest`, `NeuroAwarenessInfrastructureTest` | Respuestas de escenario | Motor de riesgo y pausa | El cálculo no depende de un texto libre sin estructura |

### M04 Protocolo y trayectorias

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-14 | RF27, RF28 | `SafetyProtocolServiceTest` | Trayectoria y miembro responsable | Alta y cierre | Sin responsable, acción o fecha de seguimiento no hay activación válida |
| CP-15 | RF26 | `TrajectoryServiceTest`, `TrajectorySuggestionServiceTest` | Señales del estado longitudinal | Sugerencia | La sugerencia no activa el protocolo |
| CP-16 | RF29 | `SubtleSignalRadarServiceTest`, `RadarAlertServiceTest` | Familia con señales | Lectura del radar | La lectura no escribe una activación de seguridad |

### M05 Plan

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-17 | RF30 | `PlanGenerationServiceTest`, `PlanServiceTest` | Evaluación o contexto mínimo | Generación | Existe plan y tareas sin marcarlas aceptadas por el solo hecho de generarse |
| CP-18 | RF33 | `PlanServiceTest`, método `completeTask()` | Tarea abierta | Completar la tarea | El caso «completed=true» comprueba que no se fabrica entrada de bitácora. El comentario del servicio dice que tampoco se genera evidencia: la escribe la familia (`PlanService`, junto a la línea que documenta el cierre de la tarea). |
| CP-19 | RF34 | `AdaptivePlanServiceTest` (paquete `adaptive` y paquete `plan`) | Ajuste propuesto | Aprobar, rechazar, aplicar | El ajuste aplicado es uno que alguien aprobó |
| CP-20 | RF32 | `plan-list-page.component.spec.ts` | Plan en el doble HTTP | Render del listado | La pantalla muestra las misiones devueltas por el API |

### M06 Sprint y diario

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-21 | RF36, RF37 | `SprintServiceTest` | Familia con permiso | Duración y estados de misión | Solo 7 o 15 días; `CHANGED_BY_AGREEMENT` y `POSTPONED` no cuentan como incumplimiento |
| CP-22 | RF38 | Especificación `missionStatusDomain.spec.ts` y pruebas de bitácora | Estados de misión | Cálculo de porcentaje | El denominador excluye los estados acordados |
| CP-23 | RF40 | `FamilyLogbookComponent` spec | Familia ausente o campos vacíos | Guardado | Mensaje de familia no asociada o de campos obligatorios |
| CP-24 | RF41 | `JournalServiceTest`, `JournalServiceInterpretationTest`, `UserJournalServiceTest` | Entrada con interpretación | Persistencia | Interpretación y hecho quedan en campos distintos |

### M07 Evidencia

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-25 | RF43 | `EvidenceAnalysisConsumerTest` | Evidencia ya guardada | Consumo del mensaje | El texto de IA es comentario, no una nota de validación |
| CP-26 | RF42 | `TaskEvidenceServiceTest` | Fallo simulado de `RabbitTemplate` | Alta de evidencia | El error del broker no se propaga al caller |
| CP-27 | RF44 | `DocumentaryProductionService` y el paso final de `FamilyLifecycleIntegrationTest` | Evidencias de la familia | Ensamble | El documental pasa de borrador a publicado solo por el flujo de producción |

### M08 Hogar y vitalidad

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-28 | RF46 | `FamilyHomeControllerIntegrationTest`, `FamilyHomeControllerJwtIntegrationTest`, specs de `family-home` | JWT de un miembro | `GET` del hogar | Sin token no hay hogar; con token se ve la etapa |
| CP-29 | RF50 | `VitalityServiceTest`, `RecoveryIndexServiceTest` | Registro parcial | Inserción con campos nulos y cálculo del índice | El servicio acepta el día incompleto que define `V110` |

### M09 Memoria

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-30 | RF51 | `BiographyServiceTest` | Catálogo y principal | Alta de respuesta | El autor es el usuario autenticado |
| CP-31 | RF52 | `LineageServiceTest` | Familia del actor | Alta de linaje | El linaje queda ligado a esa familia |

### M10 Consentimiento y profesional

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-32 | RF58 | `ProfessionalFollowUpDraftServiceTest`, `ProfessionalFollowUpDraftControllerIntegrationTest` | Asignación polimórfica | Alta del borrador | No exige llave foránea a una sola tabla de asignaciones (`V108`) |
| CP-33 | RF59 | `ConsentServiceTest` | Familia del actor | Otorgar y revocar | Queda el estado de consentimiento y el evento de auditoría que el servicio publica |
| CP-34 | RF61 | `FhirBundleAssemblerTest`, `ObservationFhirMapperTest`, `FhirSerializationServiceTest` | Registro canónico sin mapa clínico | Serialización | El bundle conserva el código de Integrity y no inventa un segundo coding |
| CP-35 | RF63 | `SupportNetworkServiceTest` | Especialidad del profesional | Alta | El rol Spring coincide con terapeuta, orientador o trabajador social |

### M11 Reportes y administración

| ID | Requisito | Clase existente | Precondición | Qué se ejercita | Resultado que la prueba protege |
| --- | --- | --- | --- | --- | --- |
| CP-36 | RF65 | `PdfExportServiceTest`, `ExcelExportServiceTest`, `PdfReportServiceTest` | Datos de familia en memoria | Generación del archivo | El exportador produce bytes sin lanzar en el caso feliz cubierto |
| CP-37 | RF67 | `SecurityWatchdogServiceTest` y pruebas de controladores bajo `admin` | Usuario sin `ROLE_ADMIN` | Llamado a ruta de administración | La cadena de seguridad niega el acceso |
| CP-38 | RF66 | `ChatControllerTest` | Proveedor o servicio simulado | Envío de mensaje | El controlador no exige la red de Anthropic en la prueba |

### Registro de ejecución

| ID | Fecha | Ejecutor | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| CP-01 a CP-38 | [Pendiente: fecha de la corrida] | [Pendiente: quien ejecuta] | [Pendiente: pasó o falló] | [Pendiente: ruta del informe JaCoCo o de la consola] |

Conteo de soporte, sin abrir cada clase: 201 archivos `*Test.java`, 1 clase `IntegrityFamilyApplicationTests`, 0 archivos con sufijo `*IT.java`, 3 clases de integración con perfil `integration-test` (`FamilyLifecycleIntegrationTest`, `EvaluationRiskIntegrationTest`, `GuardianIntegrationTest`) y 31 `*.spec.ts`.
