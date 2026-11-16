# PT-PP-01. Planteamiento del Problema Integrity Family

- **Código oficial 2026:** PT-PP-01
- **Proyecto:** Integrity Family
- **Versión del documento:** 1.0
- **Fuente de ingeniería inversa:** rama `feature/v1.2-micro-simulations`, commit `0d3cfef` (2026-11-15)
- **Elaboración documental:** 16/11/2026

Este documento da una visión general del problema que aborda Integrity Family y de la solución de software ya construida en el repositorio.

**HISTORIAL DE REVISIÓN**

| Versión | Fecha Elaboración | Responsable Elaboración | Fecha Aprobación | Responsable Aprobación |
| --- | --- | --- | --- | --- |
| 1.0 | 16/11/2026 | William Lopez (autor registrado en `CLAUDE.md` y usuario administrador sembrado en `MasterDataInitializer`) | [Pendiente: fecha de aprobación del instructor] | [Pendiente: nombre del instructor] |

**CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR**

| **VERSIÓN** | **MODIFICACIÓN RESPECTO VERSIÓN ANTERIOR** |
| --- | --- |
| 1.0 | Creación del documento a partir del código, de `README.md`, de `docs/vision.md` y de los ADR en `docs/adr/`. |

## Tabla de Contenido.

1. Introducción
2. Descripción General
3. Situación Actual
4. Situación Esperada
5. Justificación
6. Aspectos Legales (normas o leyes)

## 1. Introducción

Integrity Family es una plataforma web de acompañamiento familiar. El repositorio la describe como un sistema de bienestar familiar con inteligencia artificial, backend Spring Boot, frontend Angular y base de datos MySQL (`README.md`, párrafo inicial). La visión del producto precisa que combina evaluación diagnóstica, IA adaptativa y narrativa generacional en un proceso de transformación sostenida, y que se concibe como un sistema operativo familiar y no como una aplicación de tareas (`docs/vision.md`, secciones «Qué es Integrity Family» y «Las cuatro capas del proyecto»).

Este documento presenta el problema que el software pretende atender, la situación que el propio repositorio documenta como punto de partida, la situación que el sistema ya implementa y la justificación técnica de esa solución. Cada afirmación de contexto se apoya en un archivo del repositorio. Lo que no está en el código ni en la documentación interna queda marcado como pendiente.

### 1.1 Responsables e Involucrados

| **Nombre** | **Tipo (Responsable/ Involucrado)** | **Rol** | Cargo |
| --- | --- | --- | --- |
| William Lopez | Responsable | Autor del repositorio (`CLAUDE.md`: «Autor: William Lopez / Guillermo-prog-star»). El inicializador crea el administrador `william@integrity.family` con nombre «William Lopez» (`MasterDataInitializer.java`). El guard de administración del frontend acepta además el correo `william_lopezb@soy.sena.edu.co` (`admin.guard.ts`). | [Pendiente: cargo o rol dentro del equipo de formación] |
| [Pendiente: nombres de los demás aprendices del equipo] | Responsable | [Pendiente: rol en el proyecto formativo] | [Pendiente: cargo] |
| [Pendiente: nombre del instructor] | Involucrado | Aprobación de la documentación de proyecto tipo 1 | Instructor |
| Familia usuaria | Involucrado | Núcleo que se registra, evalúa y ejecuta el plan. No hay un cliente institucional nombrado en el código. | [Pendiente: familia o institución piloto, si existe] |
| Profesional de apoyo (terapeuta, orientador, trabajador social) | Involucrado | Rol de sistema sembrado en `V78__professional_roles.sql` y asignado en `SupportNetworkService` | [Pendiente: profesionales reales del piloto] |

### 1.2 Referencias (Bibliografía o web Grafía)

Las referencias siguientes están en el repositorio. No se encontró un levantamiento de requisitos con entrevistas o encuestas a un cliente externo.

| Nombre | Descripción | Link Referencia |
| --- | --- | --- |
| README del repositorio | Describe el producto, el stack y el flujo principal declarado por el equipo. | `README.md` |
| Visión y principios | Define familia, hogar, cuidado, legado, ICF y la regla de que ninguna IA es fuente única de verdad. | `docs/vision.md` |
| Arquitectura | Describe el despliegue (Angular, Spring Boot, MySQL, RabbitMQ, Claude) y el flujo de evaluación. | `docs/architecture.md` |
| ADR-002 | El Hogar Digital Familiar es la raíz de la experiencia. | `docs/adr/ADR-002-hogar-digital-como-raiz.md` |
| ADR-018 | Separa lo ocurrido de lo interpretado en el diario (`V115`). | `docs/adr/ADR-018-separar-ocurrido-de-interpretado.md` |
| ADR-019 | La IA propone y la familia decide; la IA comenta la evidencia y no la califica. | `docs/adr/ADR-019-ia-propone-familia-decide.md` |
| Directriz operativa del piloto V1.2 | Congela las fases del instrumento `SCENARIO_V1_2` durante el piloto. | `docs/Directriz_Operativa_Piloto_V1.2.md` |
| Política de privacidad de la aplicación | Texto legal publicado en el frontend. Cita la Ley 1581 de 2012 y trata como sensibles los datos emocionales. | `if-frontend/src/app/features/legal/privacy-policy.component.html` |
| Ley 1581 de 2012 | Protección de datos personales. La política de la aplicación la invoca de forma explícita. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981 |
| Ley 1098 de 2006 | Código de Infancia y Adolescencia. Aplica porque el modelo de miembro guarda edad y fecha de nacimiento y el comentario de `document_type` contempla TI y RC. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=22106 |
| [Pendiente: encuestas o entrevistas de levantamiento] | No hay instrumentos de campo en el repositorio. | NA |

## 2. Descripción General

El problema que el proyecto formula, según su propia visión, es que una familia no se transforma por acumular tareas ni por un diagnóstico que la etiquete. La transformación ocurre cuando las experiencias individuales se convierten, por comunicación, reflexión y memoria compartida, en una comprensión colectiva (`docs/vision.md`, Principio de Construcción Compartida de Significado Familiar). El software responde con un acompañamiento de hasta 36 meses, un Índice de Cohesión Familiar (ICF) de 0 a 100 en cuatro dimensiones (emociones, comunicación, hábitos y tiempos) y planes, sprints, evidencias y memoria familiar (`README.md`, sección «Hitos del sistema»; `docs/vision.md`, «Modelo de transformación familiar»).

La solución construida es una aplicación web de una sola página (Angular 17) que consume una API REST (Spring Boot 3.4.3) con autenticación JWT, persistencia en MySQL 8.4 mediante migraciones Flyway y mensajería opcional con RabbitMQ. La generación de planes y el chat usan un proveedor de IA intercambiable; Claude es el proveedor descrito como activo (`docs/architecture.md`, «Independencia de IA»). La familia acepta el plan: la migración `V111` agrega estado de aceptación sobre `plans` y el ADR-019 fija que la IA no valida la evidencia.

## 3. Situación Actual

Esta sección describe el contexto del problema tal como lo escriben los documentos del proyecto, y el estado del software en la rama analizada. No describe un proceso administrativo del SENA: el repositorio no documenta un trámite institucional previo.

Según `docs/vision.md`, una familia puede compartir techo sin construir significado compartido, y el cuidado puede fallar aunque exista afecto. El ICF se define como diagnóstico de orientación y no como juicio. Las trayectorias de riesgo y el protocolo de seguridad existen para cuando la familia pierde temporalmente la capacidad de cuidarse; la tecnología no sustituye ese cuidado (`docs/vision.md`, «¿Qué es el cuidado?»).

En el código, esa situación ya tiene un sistema en operación de desarrollo, no un prototipo vacío:

- Existe un núcleo familiar (`families`), miembros con edad, fecha de nacimiento y documento de identidad opcional (`FamilyMember.java`, migración `V103`).
- La evaluación calcula ICF y dimensiones (`Evaluation`, `EvaluationScoringService` referenciado en `docs/architecture.md`).
- Hay planes de mejora, sprints de 7 o 15 días (`CLAUDE.md`, convención de `SprintService`), evidencias y documentales.
- Hay un banco de trayectorias de riesgo con trayectorias que exigen protocolo de seguridad (`V97`) y una activación siempre manual (`V98`, `SafetyProtocolActivation`).
- Hay consentimiento formal para compartir con una institución externa (`V104`, módulo `consent`), separado de los flags de ecosistema.
- El concepto map clínico hacia SNOMED, LOINC o CIE-10 está deliberadamente vacío (`CLAUDE.md`, Fase 5 de interoperabilidad; `TerminologyService`).

Inconsistencias documentales que forman parte de la situación actual del repositorio, no del problema social:

- `README.md` declara Java 21; `backend/pom.xml` fija `java.version` en 17; `backend/Dockerfile` compila y ejecuta con Temurin 21.
- `docs/architecture.md` declara Angular 18; `if-frontend/package.json` declara Angular `^17.3.0`.
- `README.md` describe la respuesta de evaluación como «escala 1–5»; la escala vigente en el frontend es la Ruta de Conciencia (`if-frontend/src/domain/constants/rutaConcienciaDomain.ts`), que no se expone al usuario con los nombres internos `INCONSCIENTE`…`PLENO`.
- `README.md` publica credenciales de demostración distintas del administrador que siembra `MasterDataInitializer`.

## 4. Situación Esperada

La situación que el software busca, según los mismos documentos, es que la familia pueda:

1. Registrarse, constituir un núcleo y registrar miembros con autonomía y responsabilidad (`README.md`, «Filosofía del sistema» y «Flujo principal»).
2. Reconocer su estado mediante el ICF y la Ruta de Conciencia, sin que el estado interno de conciencia se muestre como etiqueta (`rutaConcienciaDomain.ts`; `docs/vision.md`).
3. Recibir un plan propuesto por IA y decidir si lo acepta (`V111`, ADR-010, ADR-019).
4. Sostener la práctica en sprints, dailies y evidencias, separando «no hubo oportunidad», «no se intentó» e «intentado sin resultado favorable» (`V114`, `AttemptStatus`).
5. Separar lo observado de lo interpretado en el diario (`V115`, ADR-018).
6. Conservar memoria: biografía guiada, linaje, legado, documental y línea de tiempo (`V113`, módulos `biography`, `lineage`, `legado`, `documentary`, `timeline`).
7. Activar, con confirmación humana, un protocolo de seguridad cuando una trayectoria lo exige, sin ritual automático ante un riesgo vital (`V98`, `V99`).
8. Compartir datos con un tercero solo con consentimiento revocable y auditable (`V104`).
9. Volver a mirarse en el Hogar Digital como espejo del hogar real, no como sustituto (`docs/vision.md`, «¿Qué es el hogar?»; ADR-002).

El alcance geográfico de operación (municipio, departamento y país) está modelado en `Family` (`municipio`, `departmentCode`, `countryCode`). El valor por defecto de país en el registro del frontend es `CO` (`register-page`, según el formulario de creación de familia). [Pendiente: territorio concreto del piloto y población objetivo cuantificada.]

## 5. Justificación

La solución elegida se justifica con decisiones ya escritas en el repositorio, no con una comparación de mercado que el código no contiene.

- **Acompañamiento sostenido y no un tablero de tareas.** La visión rechaza reducir el producto a una aplicación de tareas y fija un horizonte de 36 meses con hitos (`docs/vision.md`; hitos `W1` a `M36` sembrados en `MasterDataInitializer`).
- **La familia decide.** ADR-019 y el estado de aceptación del plan (`V111`) impiden que la IA califique la evidencia o active el plan sin confirmación.
- **Evidencia separada del estado operacional.** ADR-004 y la tabla `hypothesis_evidence` (`V106`) registran observaciones de hipótesis (identidad familiar, determinantes, recuperación biológica) fuera de las tablas que el sistema sobrescribe.
- **Riesgo vital con acción humana.** `V97` marca trayectorias que exigen protocolo (violencia intrafamiliar, ideación suicida, autolesiones, entre otras, según el comentario de la migración) y `V98` exige responsable, acción inicial y fecha de seguimiento. `V99` corrige la documentación que describía una activación automática inexistente.
- **Datos sensibles y compartición explícita.** La política de privacidad clasifica los datos emocionales como sensibles. El módulo `consent` cubre el caso de una institución que no es miembro de la familia ni de la red de apoyo (`V104`, `CLAUDE.md`).
- **Interoperabilidad sin acoplar el dominio a FHIR.** Las fases 0 a 5 descritas en `CLAUDE.md` introducen identificación, modelo canónico, consentimiento, mapeadores y un bundle FHIR R4, y dejan el diccionario clínico vacío hasta que lo valide un codificador. Eso evita inventar códigos sobre trayectorias críticas.
- **Stack verificable.** Spring Boot, Angular, MySQL, Flyway, JWT y Docker están en `pom.xml`, `package.json`, `docker-compose.yml` y las migraciones. La elección de MySQL se desarrolla en el PT-MCBD-01.

[Pendiente: justificación económica, tamaño de la muestra del piloto y comparación con productos del mercado. No están en el repositorio.]

## 6. Aspectos Legales (normas o leyes)

El código trata datos personales y datos sensibles de núcleos familiares, incluida información de miembros con edad y fecha de nacimiento. La política publicada por la propia aplicación invoca la Ley 1581 de 2012. La inclusión de la Ley 1098 de 2006 se apoya en que el dominio puede registrar menores (campos `age` y `birthDate`, y el comentario de `document_type` que enumera TI, RC, NUIP). No se encontró en el backend un flujo que exija autorización de representante legal ni un job que ejecute la supresión a 30 días anunciada en la política. Esos vacíos quedan pendientes. Este apartado no sustituye un concepto jurídico.

| Norma o Ley | Descripción | Enlace |
| --- | --- | --- |
| Ley 1581 de 2012 | Régimen general de protección de datos personales. La pantalla `legal/privacy` la cita y clasifica como sensibles los datos emocionales, psicológicos y relacionales. El registro exige aceptar la política (`register-page.component.ts`). Hay consentimiento revocable en `consents` (`V104`) y eventos `CONSENT_GRANTED` / `CONSENT_REVOKED`. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981 |
| Decreto 1377 de 2013 | Reglamenta la Ley 1581 (autorización, políticas y derechos). La pantalla de privacidad enumera acceso, rectificación, supresión, portabilidad, oposición y revocatoria. El mecanismo automático de supresión a 30 días no aparece en los controladores revisados. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=53646 |
| Ley 1266 de 2008 | Habeas data financiero y comercial. Se menciona solo porque el dominio guarda tipo y número de documento (`V103`) como ancla de identidad, no como historia crediticia. No hay módulo de centrales de riesgo. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=34488 |
| Ley 1098 de 2006 | Código de Infancia y Adolescencia. `FamilyMember` persiste `age` y `birthDate`. El comentario de `document_type` incluye documentos de menores (TI, RC, NUIP). `V97` documenta trayectorias condicionadas por edad (embarazo adolescente, ciberacoso). El interés superior del niño obliga a tratar esos datos con restricción adicional. [Pendiente: política específica de menores y autorización del representante, no implementada de forma verificable en el código.] | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=22106 |
| Ley 1581 de 2012, artículo 6 (datos sensibles) y trayectorias de salud mental | El banco de trayectorias incluye ideación suicida, autolesiones y consumo problemático (`V97`, comentario de migración). Esos datos son sensibles. El protocolo de seguridad es manual (`V98`). | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981 |
| Ley 527 de 1999 | Comercio electrónico y mensajes de datos. Aplica al registro, al consentimiento electrónico y a los tokens de sesión. No hay firma digital certificada en el código. | https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=4276 |
| [Pendiente: concepto del área jurídica del centro o de un profesional] | La plantilla exige consejo profesional cuando el riesgo legal es material. Este documento no lo reemplaza. | NA |
