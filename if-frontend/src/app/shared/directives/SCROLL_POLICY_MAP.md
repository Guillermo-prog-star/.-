# Mapa de políticas de scroll — Integrity Family

## Directiva: `ScrollPolicyDirective`
Importar desde `../../shared/directives/scroll-policy.directive`
Uso: `<div scrollPolicy="POLÍTICA">` sobre el contenedor con `overflow-y-auto`

---

## Políticas disponibles

| Política           | Comportamiento                                              |
|--------------------|-------------------------------------------------------------|
| `auto-bottom`      | Siempre baja al último elemento (chat, consultor IA)        |
| `scroll-to-new`    | Baja solo si el usuario está cerca del final (< 120px)      |
| `preserve-position`| Nunca mueve — formularios, paneles clínicos, diagnóstico    |
| `critical-alert`   | Solo mueve cuando `[criticalAlert]="true"` cambia a true    |
| `manual`           | Sin lógica automática (default)                             |

---

## Asignación por módulo

### Configuración
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| family-create-page        | formulario              | `preserve-position`|
| member-list-page          | lista                   | `scroll-to-new`    |
| guardian-panel            | panel                   | `preserve-position`|

### Diagnóstico
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| evaluation-start-page     | formulario              | `preserve-position`|
| evaluation-history-page   | lista de evaluaciones   | `scroll-to-new`    |
| evaluation-evolution-page | gráficas                | `preserve-position`|
| evaluation-result-page    | panel clínico           | `preserve-position`|

### Plan & Ruta
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| plan-list-page            | lista de planes         | `scroll-to-new`    |
| mission-detail-page       | detalle de misión       | `preserve-position`|
| transformation-route      | ruta 36 meses           | `preserve-position`|

### Transformación Diaria
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| family-logbook            | lista de entradas       | `scroll-to-new`    |
| weekly-plan               | formulario semanal      | `preserve-position`|
| checklist-page            | lista de tareas         | `scroll-to-new`    |
| error-protocol            | pasos del protocolo     | `preserve-position`|

### Apoyo
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| **chat-page** ✅           | `.chat-messages`        | `auto-bottom`      |
| crisis-page               | checklist de crisis     | `preserve-position`|
| cognitive-page            | historial cognitivo     | `scroll-to-new`    |

### Legado
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| family-gratitude          | lista de gratitudes     | `scroll-to-new`    |
| my-space                  | contenido personal      | `preserve-position`|
| legado                    | línea de tiempo         | `scroll-to-new`    |

### Sistema
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| dashboard-page            | wrapper principal       | `critical-alert`   |
| scanner-analytics-page    | historial              | `scroll-to-new`    |
| inference-history-page    | historial              | `scroll-to-new`    |
| portal-familiar           | feed                    | `critical-alert`   |
| profile-page              | formulario              | `preserve-position`|

### Administración
| Componente                | Contenedor              | Política           |
|---------------------------|-------------------------|--------------------|
| stats                     | panel analítico         | `preserve-position`|
| emotional-rules-page      | reglas EEDSL            | `preserve-position`|
| sandbox                   | área de pruebas         | `manual`           |
| voice-monitor             | log de voz en tiempo real | `auto-bottom`    |

---

## Estado de implementación

### auto-bottom
- ✅ `chat-page` — directiva en `.chat-messages` + servicio eliminado (método `scroll()` removido)
- ✅ `voice-monitor` — directiva en `.max-h-[500px]` + servicio

### scroll-to-new
- ✅ `member-list-page`
- ✅ `evaluation-history-page`
- ✅ `checklist-page`
- ✅ `inference-history-page`
- ✅ `scanner-analytics-page`
- ✅ `family-gratitude`
- ✅ `plan-list-page`
- ✅ `family-logbook`
- ✅ `cognitive-page`
- ✅ `legado`

### critical-alert
- ✅ `dashboard-page` — conectado a `SentinelCoreService` vía efecto en el servicio
- ✅ `portal-familiar`

### preserve-position / manual (default)
- No requieren cambio — el shell resetea la política en cada cambio de ruta via `(activate)="scrollPolicy.reset()"`
