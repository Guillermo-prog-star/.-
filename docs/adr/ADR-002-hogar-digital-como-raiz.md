# ADR-002: Hogar Digital Familiar como raíz de la experiencia (no el HUD, no el tiempo)

**Status:** Accepted
**Date:** 2026-07-14
**Deciders:** William Lopez
**Supersede a:** [ADR-001](./ADR-001-divulgacion-progresiva.md)

## Context

ADR-001 diagnosticó bien el problema (sidebar completo desde el día 0, dashboard con lenguaje de riesgo para familias sin datos, banco de 1000 preguntas anunciado de una vez) pero se equivocó en la solución de fondo en dos puntos, señalados por William:

1. **Mezcló navegación con estado.** Proponía el HUD Adaptativo (un panel de estado) como "casa base" (una estructura de navegación). Un HUD representa *qué necesita la familia hoy*; no debería ser también *cómo se organiza el sistema*. Si el HUD se vuelve la puerta de entrada, el dashboard y el sistema colapsan en la misma cosa.
2. **Usó `currentMilestone` sin verificar cómo avanza realmente.** ADR-001 lo trató como tiempo transcurrido. Investigación posterior (ver "Corrección" abajo) mostró que esto era incorrecto: `currentMilestone` ya es un motor de preparación real, no un calendario.

### Lo que la investigación de seguimiento encontró (y ADR-001 pasó por alto)

El concepto correcto **ya está construido**, solo desconectado — el mismo patrón repetido durante toda esta sesión de trabajo (HUD Adaptativo, `isSetupDone()`, y ahora esto):

- **`/family-home` (`FamilyHomePageComponent`)** ya se titula literalmente *"Hogar Digital Familiar"*. Comentario en el código: *"Contenedor del Hogar Digital Familiar (IFRM-D, Hito 7)... no reemplaza /dashboard"* — nació como alternativa y nunca se conectó como entrada real.
- Ya tiene **4 vistas que cambian por estado real de la familia**, no por tiempo: `OnboardingHomeView`, `AssessmentHomeView`, `ReturnStageHomeView`, `ActiveHomeView`.
- Ese estado es `JourneyStage` (`NEW_FAMILY → PROFILE_IN_PROGRESS → ASSESSMENT_IN_PROGRESS → ASSESSMENT_COMPLETED → RETURN_AVAILABLE → FIRST_SPRINT_PENDING → ACTIVE_HOME → PAUSED_HOME → RESUMING_HOME`), calculado en `FamilyJourneyQueryPortAdapter` a partir de **comportamiento real**: evaluaciones finalizadas/en curso, sprint activo, tareas completadas — no de fecha de registro.
- El propio comentario de ese archivo admite la brecha: *"No existe todavía una máquina de estados real para JourneyStage... Esta es una heurística conservadora sobre datos reales mientras se define el modelo formal de etapas."* — es decir, el código ya pedía, por escrito, exactamente lo que William está proponiendo.
- Ya existen señales de comportamiento sin usar para esto: `participation_score`, `plan_adherence_percent`, `adherence_percentage` (BD), y un `ParticipationService` con ventana de actividad de 7 días / umbral de inactividad de 10 días, con eventos ya registrados por tipo.

El HUD Adaptativo (construido hoy en esta misma sesión) también encaja aquí — pero como **panel de estado dentro** del Hogar Digital, no como la estructura que lo contiene.

### Corrección (misma sesión, tras investigar `MilestoneService`)

La primera versión de este ADR proponía `FamilyReadinessLevel` como un score nuevo a construir, y trataba `currentMilestone` como simple tiempo transcurrido. Ambas cosas eran incorrectas.

`MilestoneService.evaluate()` (`milestone/service/MilestoneService.java`) ya avanza `currentMilestone` combinando **tres criterios a la vez**, no solo calendario:

1. **Tiempo** — al menos 85% de la duración esperada del hito.
2. **ICF** — promedio de ICF de evaluaciones finalizadas ≥ umbral específico por hito (40 en W1 → 70 en M36, progresivo).
3. **Tareas** — % de tareas completadas ≥ umbral **adaptativo según nivel de riesgo** (30% si la familia está en riesgo CRÍTICO, 70% si está en BAJO).

Corre automáticamente cada noche (`@Scheduled(cron = "0 0 3 * * *")`) sobre todas las familias, y solo avanza si los tres criterios se cumplen juntos. Esto es, en sustancia, `FamilyReadinessLevel` — ya construido, con 22 archivos del backend dependiendo de `currentMilestone` (`PlanGenerationService`, `PromptGenerator`/`ContextSynthesizer` para IA, `GuardianBriefingService`, `AnalyticsServiceImpl`, reportes PDF, entre otros).

`JourneyStage`, en cambio, es un concepto más chico y de otro nivel: no persiste, se recalcula por request, y solo vive dentro del paquete `familyhome`. No responde "qué tan preparada está la familia" — responde "qué pantalla debo renderizar ahora mismo dentro del Hogar Digital" (¿está a mitad de una evaluación? ¿tiene un sprint activo hoy?). Es routing de sesión/vista, no una escala de madurez, y no debería tratarse como tal.

## Decision

**El Hogar Digital Familiar (`/family-home`, ya construido) es la raíz conceptual y la pantalla de entrada.** El HUD Adaptativo es uno de sus componentes — el panel de "estado actual" — no el contenedor.

**El eje de divulgación progresiva es `currentMilestone`, respaldado por `MilestoneService` — no un sistema nuevo.** Ya combina tiempo + ICF + adherencia de tareas (adaptativo por riesgo), corre automáticamente, y ya es la fuente de verdad que usan 22 archivos del backend. No se construye `FamilyReadinessLevel` como sistema aparte — donde se necesite más granularidad que un hito binario, se expone lo que `AdvancementEvaluation` ya calcula (`canAdvance`, `timeMet`, `icfMet`, `tasksMet`) como "les falta esto para el siguiente nivel".

`JourneyStage` se mantiene, pero con su alcance real: decide qué vista de Hogar Digital renderizar en esta sesión (Onboarding/Assessment/ReturnStage/Active), no la profundidad de los espacios permanentes. No compite con `currentMilestone` — opera en una capa distinta (momento a momento, no macro-progreso).

**Los espacios de navegación son permanentes; lo que cambia es su profundidad, no su existencia.** Ningún espacio se oculta — se muestra con un mensaje de "esto llegará con ustedes" hasta que la familia esté lista. Esto reemplaza el modelo "no puedes ver esto" de ADR-001 por "esto existe, todavía no es su momento".

**Regla dura heredada de ADR-001, sin cambios:** Crisis Familiar, Protocolo de Seguridad y Consultor IA nunca se gatean, en ningún estado, para nadie.

### Arquitectura

```
Integrity Family
       │
Hogar Digital Familiar  (raíz — /family-home, ya construido)
       │
   ┌───┴────────────────────────────┐
   │                                │
Estado actual                Espacios permanentes
(HUD Adaptativo)              (navegación fija; profundidad variable)
   │                                │
resumen del día,              Familia · Nuestro Camino · Aprender ·
ICF/riesgo si profesional,    Conversar · Recordar · Crecer · Ayuda
5 accesos rápidos             (nombres exactos: ver tabla abajo, abierta a ajuste)
```

### Mapa de espacios (propuesta inicial, sobre `currentMilestone` — no es definitiva)

| Espacio | Módulos reales que agrupa | Profundidad mínima (hito, vía `MilestoneService`) |
|---|---|---|
| **Familia** | Familia, Miembros, Guardián, ADN Familiar | `W1` — siempre con contenido completo, es lo primero que se llena |
| **Nuestro Camino** | Diagnóstico, Plan Familiar, Ruta de 36 Meses, Capital (ICaF), SMFF, Motor Adaptativo | `W1` (visible, "esto irá creciendo con ustedes") → contenido real desde `M1` |
| **Aprender** | Sistema Cognitivo, Documentación, Trayectorias de Riesgo* | Disponible siempre (Trayectorias por regla de seguridad) |
| **Conversar** | Consultor IA*, Consejo Familiar, Crisis Familiar* | Disponible siempre (regla de seguridad) |
| **Recordar** | Historia Familiar, Árbol Generacional, Documental, Película Familiar, Legado, Linaje | Contenido real desde `M18` |
| **Crecer** | Sprint Familiar, Bitácora, Planeación Mensual, Evidencias, Cápsula, Gestión de Errores, Pulso Familiar, Gratitud, Mi Espacio | Contenido real desde `M1` (una vez hay plan) |
| **Ayuda** | Ecosistema de Apoyo, Salud Familiar, Reportes, Panel Profesional | Disponible siempre (apoyo no debería esperar madurez) |

`*` = regla de seguridad, nunca se gatea. Dentro de cada espacio, `JourneyStage` sigue decidiendo el detalle de la vista activa (ej. si "Nuestro Camino" muestra la vista de Onboarding o la de Assessment), independientemente del hito.

### Diagnóstico — narrativa antes que mecánica

En vez de "1000 preguntas" (ADR-001 original) o solo "20 preguntas · 8-12 min" (mi primera corrección), mostrar primero el propósito:

> *"Durante los próximos meses iremos conociendo mejor a su familia. Hoy solo les proponemos una primera conversación. No es un examen, no hay respuestas correctas ni incorrectas."*

y solo después el detalle mecánico ("Sesión 1 · 20 preguntas · 10 minutos").

## Resolución del riesgo de los "tres ejes" (antes abierto, ahora cerrado)

La versión anterior de este ADR dejaba abierto el riesgo de tener `currentMilestone`, `JourneyStage` y un `FamilyReadinessLevel` nuevo compitiendo como fuente de verdad. Investigación de `MilestoneService` lo resuelve:

- **`currentMilestone`** = fuente de verdad para preparación/madurez macro. Ya es un motor de tres criterios (tiempo + ICF + tareas adaptativo por riesgo), no un calendario. Gobierna la profundidad de los espacios permanentes.
- **`JourneyStage`** = routing de vista dentro de una sesión. No compite con lo anterior — responde una pregunta distinta ("qué pantalla ahora"), no ("qué tan lista está la familia").
- **`FamilyReadinessLevel`** = no se construye. Ya existe con otro nombre (`MilestoneService.AdvancementEvaluation`).

No hay dos sistemas que reconciliar — hay uno macro (`currentMilestone`) y uno de sesión (`JourneyStage`), en capas distintas por diseño.

## Trade-off Analysis

Frente a ADR-001: este enfoque no cuesta más construir — de hecho cuesta *menos*, porque tanto el eje de profundidad (`currentMilestone` + `MilestoneService`, ya con lógica de tres criterios) como el Hogar Digital (`/family-home`, `JourneyStage`) ya existen y ya funcionan. El trabajo real es exponer y conectar, no diseñar un algoritmo nuevo.

## Consequences

- **Más fácil:** el Hogar Digital por fin cumple el propósito con el que se construyó (Hito 7, nunca conectado); no hace falta diseñar ningún algoritmo de madurez — `MilestoneService` ya lo resuelve, con sensibilidad al riesgo incluida; la experiencia deja de sentirse como "software incompleto" (espacios permanentes vs. módulos que aparecen/desaparecen).
- **Más difícil:** el frontend (sidebar, Hogar Digital) hoy no consume `AdvancementEvaluation` en ningún lado — hay que exponerlo vía API y diseñar cómo comunicar "les falta esto para el siguiente nivel" sin caer otra vez en lenguaje de riesgo/urgencia (la misma trampa que motivó ADR-001).
- **Habrá que revisitar:** nada de `MilestoneAwarePlanEngine` — ya usa `currentMilestone` correctamente y no necesita cambiar de eje. Esto simplifica la implementación respecto a la versión anterior de este ADR.

## Action Items

1. [x] Decidir la fuente de verdad — resuelto arriba: `currentMilestone`/`MilestoneService` para profundidad, `JourneyStage` para vista de sesión
2. [x] Conectar `/family-home` como pantalla de entrada real. Hallazgo en el camino: `FamilyMembershipQueryPortAdapter` comparaba `family.getCreatedBy().getEmail()` sobre un proxy Hibernate lazy sin sesión activa — rompía con `LazyInitializationException` para cualquier familia no-admin (el admin nunca lo sufría por su bypass). Corregido comparando por `.getId()`; verificado en vivo con familia autoregistrada.
3. [x] Mover el HUD Adaptativo de "pantalla independiente" a "panel de estado dentro de Hogar Digital". Hallazgo: `FamilyHudProjectionResolver` (variante familiar) usaba el mismo `familyHomeService.project()` que Hogar Digital, pero su mensaje de estado principal era **hardcodeado** (`formatICaF(63)`, `formatAdaptiveCapacity(0.61)`, `formatRisk("MEDIUM")` fijos, sin relación con la familia real) — no se trasladó esa fachada. Se integraron los 5 accesos rápidos (únicos, reales) como menú permanente dentro de `/family-home`; `/hud` ahora redirige ahí para cualquier usuario no-profesional. El HUD Profesional sigue en `/hud`, sin cambios (fuera del alcance de este ADR).
4. [x] Implementar el mapa de espacios permanentes sobre `currentMilestone`. Alcance acotado deliberadamente (decisión explícita): gating solo a nivel de navegación del sidebar actual (22 enlaces, mapeados 1:1 a la tabla de arriba vía `MilestoneDepthService`), **sin** renombrar las 8 secciones existentes a los 7 espacios nombrados del ADR ni reescribir el contenido interno de cada página — eso queda para una iteración futura si se decide. `FamilyStateService.setFamily()` ahora sincroniza `currentMilestone` desde `FamilyResponse` en cualquier flujo (antes solo `FamilyListPage` lo hacía explícitamente). Ningún enlace se oculta ni se bloquea — se navega igual, solo con un indicador 🌱 + tooltip. La regla de "el profesional ve todo" se respeta (`isGrowing()` siempre `false` si `isProUser()`). Verificado en vivo: familia en W1 ve 21/22 indicadores (el 22º vive en un submenú colapsado), navegación a un enlace "creciendo" funciona sin bloqueo.
5. [x] Exponer `AdvancementEvaluation` al frontend. El endpoint (`GET /api/milestones/family/{familyId}/advancement-status`) ya existía — otro caso de "construido, nunca conectado". Se agregó `MilestoneAdvancementService` y una tarjeta "Camino hacia el hito X" en Hogar Digital (no en el sidebar — son preguntas de distinto nivel: el 🌱 del punto 4 responde "¿cuándo se activa este enlace puntual?", esta tarjeta responde "¿qué le falta a toda la familia para el siguiente hito?"). Copy sin lenguaje de riesgo: checklist de 3 filas (tiempo/evaluación/tareas) con lenguaje neutro ("15 de 5 días en este hito", no "les faltan X días") y cierre "No es una carrera — cada familia avanza a su propio ritmo". Bug propio encontrado y corregido en el camino: el primer intento no desempaquetaba `response.data` del `ApiResponse<T>` del backend — solo hacía un cast de tipos (`as unknown as Observable<...>`) que no transforma nada en runtime, dejando todos los campos `undefined` en el template. Corregido con `map(res => res.data)`. Verificado en vivo con datos reales.
6. [x] Copy de "esto irá creciendo con ustedes" — implementado como tooltip (`title`) en el indicador 🌱 del punto 4, con el hito exacto de desbloqueo. Sigue siendo solo on-hover, no un mensaje siempre visible dentro de cada página — un tratamiento más prominente queda abierto si se considera necesario.
7. [x] Corregir el tono del dashboard. Dos causas raíz encontradas y corregidas en `FamilyContextEngine`: (a) `computeDaysWithoutActivity()` usaba `999` como centinela cuando no había actividad — alimentaba directo la alerta "999 días sin actividad" para cualquier familia nueva; ahora usa la fecha de creación de la familia como base real. (b) `computeConnectionLevel()` devolvía `"BAJA"` para cualquier familia con menos de 3 eventos en 7 días, sin distinguir "familia nueva que no ha tenido ni una semana" de "familia establecida que dejó de participar" — ahora exige que la familia tenga ≥7 días para calificar como BAJA. También corregido en `PromptGenerator.buildDashboardInsightPrompt()`: sin rama para "familia sin evaluaciones", el modelo generaba lenguaje de "ceguera diagnóstica...urgente" con persona "Guardián Sentinel" incluso con Riesgo=LOW; ahora hay una rama de bienvenida cálida cuando `dimensions` está vacío. Verificado en vivo registrando una familia nueva: `daysWithoutActivity=0`, `connectionLevel=MEDIA`, `alerts=[]` (antes: 999 días, BAJA, 2 alertas). 12 tests nuevos/actualizados, 99/99 pasando.
8. [x] Reescribir el copy del Diagnóstico con narrativa antes que mecánica. `evaluation-start-page.component.ts`: cuando `completedSessions === 0`, el header muestra la narrativa acordada ("Durante los próximos meses iremos conociendo mejor a su familia...") en vez de "1000 preguntas · 3 pilares · 20 por sesión". Para familias que ya tienen sesiones completadas, se mantiene el resumen técnico (ya conocen el proceso). Verificado en vivo.
9. [x] `FamilyHudProjectionResolver` — se corrigió en vez de eliminarse (el endpoint sigue siendo un contrato de API real, no solo consumido por nuestro frontend). Ahora lee `FamilyLongitudinalState` real (ICF, nivel de riesgo) cuando existe; si la familia no tiene evaluaciones, muestra un mensaje honesto de "aún no hay datos" en vez de fabricar cifras. Verificado en vivo con familia sin datos (mensaje honesto) y con familia con datos reales (ICaF=25 → mensaje correctamente derivado, ya no el "63" fijo). De paso se encontró (no corregido aquí, anotado como tarea aparte) que `FamilyPresentationPolicy.formatRisk()` compara contra `"HIGH"/"MEDIUM"` en inglés mientras el resto del sistema usa español (`"CRITICO"/"ALTO"/"BAJO"`) — el mensaje urgente nunca se dispara con datos reales.
