# ADR-001: Divulgación Progresiva de la Experiencia Familiar

**Status:** Superseded por [ADR-002](./ADR-002-hogar-digital-como-raiz.md)
**Date:** 2026-07-14
**Deciders:** William Lopez

> **Nota de superación (2026-07-14):** este ADR trataba el HUD Adaptativo como la "casa base" del producto y usaba `currentMilestone` (tiempo) como eje de divulgación progresiva. William señaló correctamente que esto mezcla dos conceptos (navegación vs. estado) y que el eje correcto no es el tiempo sino la madurez/comportamiento real de la familia. La investigación de seguimiento confirmó que **ya existe** un concepto de "Hogar Digital Familiar" construido (`/family-home`, 4 vistas por `JourneyStage`) que el diagnóstico original pasó por alto. El contexto, los hallazgos en vivo (dashboard, sidebar, banco de 1000 preguntas) y la restricción de seguridad de este documento siguen siendo válidos y se heredan en ADR-002 — lo que cambia es la decisión de arquitectura de fondo.

## Context

Integrity Family expone hoy toda su superficie (44 módulos backend, ~40 enlaces de sidebar, panel analítico con más de 15 widgets, banco de 1000 preguntas) desde el primer segundo de uso, sin importar cuánto tiempo lleve la familia ni cuántos datos tenga.

Esto se verificó en vivo, registrando una familia nueva de principio a fin (sin datos de prueba preexistentes):

1. **Entrada** — Tras el registro, antes de crear la familia o agregar a nadie, ya está visible el sidebar completo: Configuración, Diagnóstico, Índices, Plan & Ruta, Transformación Diaria, Apoyo, Legado, Sistema — sin ningún orden sugerido ni ocultamiento.
2. **Dashboard (Panel Principal)** — Con una familia de un solo miembro, cero evaluaciones y minutos de existir, la pantalla principal mostró: *"999 días sin actividad ⚠️ 2 señales de atención"*, **"RIESGO DE ABANDONO: ALTO"**, y un análisis de IA en tono clínico: *"esta ceguera diagnóstica es un riesgo operacional que debe resolverse ahora mismo"*, junto con más de 15 widgets (Panóptico Familiar, Radar de Señales Sutiles, Sistema Cognitivo, Matriz de Escenarios, Gemelo Digital, Árbol Generacional, Consejo Familiar, Pulso Familiar…).
3. **Banco de evaluación** — 1000 preguntas totales en 3 pilares. Solo el primer pilar (Reconocimiento, meses 1–6) tiene 884 preguntas → ~45 sesiones de 20 preguntas (8–12 min c/u) → **6–9 horas** solo para el primer pilar de 6 meses.
4. **HUD Adaptativo** — construido en esta misma sesión de trabajo (ver historial de cambios). No es contenido nuevo: es un resumen de estado + 5 accesos directos que apuntan a páginas que ya existen en el sidebar completo (ej. "Trayectoria" → `/trajectory`, "Crecemos" → `/transformation/route`). Sí resuelve el problema de densidad para el día a día, pero **no es la pantalla de entrada** — el login sigue aterrizando en `/dashboard` o `/members`, y el HUD es un enlace más entre 40. Sus 5 accesos son fijos (no varían por hito), lo cual importa para la decisión de abajo.

### Esto contradice un principio ya documentado y marcado como "lo que nunca debe cambiar"

`docs/vision.md`: *"El ICF es diagnóstico, no juicio. El Índice de Cohesión Familiar mide para orientar, nunca para etiquetar."*

Una familia sin datos etiquetada como "RIESGO DE ABANDONO: ALTO" es exactamente el tipo de etiquetado que ese principio prohíbe. No es una preferencia de diseño — es una inconsistencia entre lo que el proyecto dice que es y lo que el producto hace hoy.

### Infraestructura que ya existe y no se está usando para esto

- **`Milestone`** (`milestone_definitions`): W1, M1, M3, M6, M9, M12, M15, M18, M24, M30, M36 — la línea de tiempo ya está modelada.
- **`Family.currentMilestone`**: cada familia ya sabe en qué hito está.
- **`MilestoneAwarePlanEngine`**: ya genera contenido (tareas del plan) congruente con el hito actual — es decir, ya existe el patrón "el contenido varía según dónde está la familia en el tiempo", solo que aplicado al Plan, no a la navegación ni al dashboard.
- **`isSetupDone()` / `step-dot`** en `sidebar.component.ts`: ya existe un patrón visual de progreso (family → members → guardian → diagnosis → plan), pero es puramente decorativo — no oculta ni revela nada, todos los ~40 enlaces están visibles desde el minuto uno independientemente de estos pasos.

Es decir: el sistema ya sabe calcular "dónde está esta familia en el tiempo" — lo que falta es usar ese dato para decidir qué mostrar.

### Restricción que no es negociable

`V97__risk_trajectory_safety_protocol.sql` (Banco de Trayectorias) define 7 trayectorias con `requires_safety_protocol = TRUE` (violencia intrafamiliar, ideación suicida, autolesiones, trastorno de alimentación, consumo problemático…) más reglas contextuales (embarazo adolescente, abandono de adulto mayor, ciberacoso). **Ningún esquema de divulgación progresiva puede ocultar u ocultar el acceso a Crisis Familiar, el protocolo de seguridad, ni el Consultor IA** — la seguridad no se gana por antigüedad en el sistema.

## Decision

Introducir divulgación progresiva **basada en el hito real de la familia (`currentMilestone`)**, aplicada en dos capas con roles distintos y complementarios:

- **HUD Adaptativo = casa base permanente.** No se gatea por hito. Es la pantalla de entrada para *cualquier* familia en *cualquier* momento del viaje (W1 a M36) — el "modo calmado" constante. Sus 5 accesos siguen siendo fijos; apuntan a páginas que pueden *verse* aunque el módulo destino todavía no esté "activo" para uso diario en el sidebar (mirar la Ruta de 36 Meses una vez no es lo mismo que tenerla como ítem de menú todos los días).
- **Sidebar completo = la capa que crece con el tiempo.** Aquí sí aplica el gating por `currentMilestone`, extendiendo `isSetupDone()` de decorativo a real. Es la profundidad disponible para quien quiere ir más allá del HUD.

Regla dura, sin excepción: **los módulos de seguridad (Crisis Familiar, Protocolo de Seguridad, Consultor IA) están disponibles en todos los hitos, desde W1, en ambas capas.**

Regla de rol: **el profesional (`SUPPORT_PERSON`) ve siempre todo, sin gating por hito** (ya es así hoy vía `HudAuthorizationPolicy`, coherente con lo que arreglamos en la parte 1-4). **El Guardián Familiar (miembro electo, no profesional) ve lo mismo que el resto de la familia** — acompaña el ritmo, no lo adelanta.

## Options Considered

### Option A: Divulgación progresiva por hito (recomendada)

Extender `isSetupDone()`/`currentMilestone` de decorativo a real: el sidebar y la pantalla de entrada muestran solo lo relevante al hito actual; el resto queda visible pero agrupado bajo "Más" o similar, no eliminado. El HUD Adaptativo se vuelve la pantalla de entrada (`redirectTo: 'hud'` en vez de `'dashboard'`) para familias en hitos tempranos (W1–M3).

| Dimensión | Evaluación |
|---|---|
| Complejidad | Media — reutiliza `Milestone`/`currentMilestone` ya modelados; requiere un mapa módulo↔hito y lógica de gating en `app.routes.ts`/`sidebar.component.ts` |
| Costo | Bajo — no hay nueva infraestructura de datos, solo lógica de presentación |
| Consistencia con el proyecto | Alta — es la misma lógica que ya usa `MilestoneAwarePlanEngine`, aplicada a un nuevo lugar |
| Riesgo | Requiere definir bien el mapa módulo↔hito (ver "Preguntas abiertas") — un mapeo mal pensado solo traslada el problema |

**Pros:** usa lo que ya existe; resuelve los 3 hallazgos (entrada, dashboard, HUD-no-es-entrada) con un solo mecanismo; reversible (es una capa de presentación, no borra datos ni funcionalidad).
**Cons:** requiere una decisión de producto no trivial (qué va en cada hito) que no se puede derivar solo del código.

### Option B: Modo Simple / Modo Avanzado (toggle manual, sin lógica temporal)

Un interruptor que la familia activa manualmente, sin relación con `currentMilestone`.

| Dimensión | Evaluación |
|---|---|
| Complejidad | Baja |
| Costo | Bajo |
| Consistencia con el proyecto | Baja — ignora la infraestructura de hitos ya construida |
| Riesgo | La familia nueva tiene que *saber* que debe activar "modo simple" — no resuelve el problema del día 0, que es exactamente cuando la familia menos sabe qué necesita |

**Pros:** implementación trivial.
**Cons:** no usa el `Milestone` ya modelado; pone la carga de decisión en una familia que apenas está llegando — contradice el objetivo mismo.

### Option C: Solo rediseñar el tono/copy del dashboard, sin tocar navegación

Cambiar el lenguaje alarmista ("RIESGO DE ABANDONO: ALTO") por mensajes calibrados a la ausencia de datos, sin ocultar ni reordenar módulos.

| Dimensión | Evaluación |
|---|---|
| Complejidad | Baja |
| Costo | Bajo |
| Consistencia con el proyecto | Media — resuelve la contradicción con "el ICF nunca etiqueta", pero dejaría intacto el sidebar de 40 enlaces día 0 |
| Riesgo | Resuelve solo 1 de los 3 hallazgos; la sobrecarga cognitiva de navegación seguiría intacta |

**Pros:** cambio rápido, bajo riesgo, corrige la violación de principio documentada de inmediato.
**Cons:** parcial — no resuelve la carga de navegación ni el hecho de que el HUD (ya construido) no es la entrada.

## Trade-off Analysis

Option A y Option C no son excluyentes — C es un subconjunto correctivo urgente de A (la violación del principio "nunca etiquetar" es la más grave y la más barata de arreglar). Option B se descarta porque ignora infraestructura ya construida y traslada la carga de decisión a la familia en el peor momento posible para pedírsela.

El verdadero costo de Option A no es técnico — es de producto: requiere decidir, módulo por módulo, en qué hito se revela. Eso no es una decisión que deba tomar yo solo derivándola del código; es exactamente el tipo de decisión que corresponde definir contigo antes de tocar nada.

## Consequences

- **Más fácil:** onboarding de familias nuevas; consistencia con el principio ya documentado de "el ICF nunca etiqueta"; el HUD Adaptativo (ya construido) por fin cumple su propósito como pantalla diaria.
- **Más difícil:** cualquier cambio futuro al mapa módulo↔hito requiere disciplina de producto (documentar por qué algo se mueve de hito) para no degenerar otra vez en "todo visible siempre" por conveniencia de desarrollo.
- **Habrá que revisitar:** el texto del propio Diagnóstico Familiar ("1000 preguntas · 3 pilares") — incluso con navegación gateada, esa pantalla sigue comunicando el volumen total de una vez; probablemente necesite su propio ADR o al menos su propia iteración de copy.

## Decisiones resueltas (2026-07-14, conversación con William Lopez)

1. **Primera victoria en W1:** el HUD Adaptativo (casa base permanente) es la respuesta — bienvenida + estado del día + los 5 accesos, sin exigir Diagnóstico completo primero.
2. **Mapa módulo↔hito para el sidebar** (propuesta inicial, sujeta a ajuste continuo — no es definitiva ni debe tratarse como cerrada en piedra):

   | Hito | Se revela | Razón |
   |---|---|---|
   | **W1** | Familia, Miembros, Guardián Familiar, HUD Adaptativo, Crisis Familiar*, Consultor IA*, Sprint Familiar, Bitácora & Daily | Mínimo para vivir el ritmo diario sin evaluación previa |
   | **M1** | Diagnóstico (solo sesión de hoy), Plan Familiar, ADN Familiar, Planeación Mensual, Evidencias, Cápsula Familiar | Primera sesión de diagnóstico ya ocurrió; el plan empieza a generarse |
   | **M3** | Capital Familiar (ICaF), Fortalecimiento (SMFF), Salud Familiar, Trayectorias de Riesgo*, Ecosistema de Apoyo, Gestión de Errores | Suficiente historia para que estos índices signifiquen algo |
   | **M6** | Ruta de 36 Meses, Motor Adaptativo, Reportes, Sistema Cognitivo, Viaje Familiar, Documentación | Cierre del primer semestre (Reconocimiento) |
   | **M12** | Consejo Familiar, Pulso Familiar, Gratitud Familiar, Mi Espacio, Motor de Rituales | Entrada a "Amor" (vínculos, rituales) |
   | **M18–M24** | Historia Familiar, Árbol Generacional, Ensamblaje Documental, Película Familiar | Memoria acumulada suficiente para documentar |
   | **M24–M36** | Legado Familiar, Linaje Generacional, Gemelo Digital | Fase "Entrega y Legado" de `docs/vision.md` |

   `*` = disponibles siempre desde W1 (regla de seguridad).

3. **Guardián y profesional:** el profesional ve siempre todo (ya es así vía `HudAuthorizationPolicy`); el Guardián Familiar ve lo mismo que la familia — no es una vista experta permanente.
4. **Copy del Diagnóstico:** mostrar solo la sesión de hoy ("20 preguntas · 8–12 minutos"), no el total de 1000/3 pilares de entrada.
5. **Relación HUD ↔ Sidebar:** el HUD no se gatea por hito (casa base constante); el sidebar sí. No son el mismo mecanismo — son dos capas complementarias.

## Action Items

1. [ ] Cambiar el `redirectTo` por defecto de `'dashboard'` a `'hud'` — para **todos** los hitos, no solo los tempranos (el HUD es la entrada permanente, no una rampa que desaparece)
2. [ ] Extender `isSetupDone()`/`sidebar.component.ts` de decorativo a gating real, usando la tabla módulo↔hito de arriba
3. [ ] Corregir el tono/lenguaje del dashboard para familias sin datos (Option C, independiente y urgente — corrige la violación de principio de inmediato incluso antes de que el resto esté listo)
4. [ ] Cambiar el copy de la pantalla de Diagnóstico Familiar para mostrar solo la sesión de hoy
5. [ ] Confirmar en código que Crisis Familiar, Consultor IA y Trayectorias de Riesgo quedan fuera de cualquier gating (ya lo están hoy — verificar que se mantenga así al implementar el punto 2)
