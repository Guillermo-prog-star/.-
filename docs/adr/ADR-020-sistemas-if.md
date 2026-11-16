# ADR-020: Sistemas IF — capa de práctica sobre lo que ya existe

## Estado

Aceptado (2026-11-16). Migración **V116**.

## Contexto

Se organizaron diez sistemas de organización personal y familiar en dos niveles:

- **Nivel 1, Estructura:** Captura, Calendario, Tareas, Conocimiento, Comunicación.
- **Nivel 2, Funcionamiento:** revisión diaria, planificación semanal, ajuste por cambio, revisión mensual y revisión anual.

El mapeo contra el código mostró que cada sistema ya tiene un lugar en Integrity Family (diario, sprint y planeación, PlanTask/SprintMission, Centro de Documentación, chat y consejo, check-in con `attempt_status`, retrospectiva, hitos). Construir otro diario, otro calendario o otra lista de tareas duplicaría esos modelos.

Lo que no existe es el criterio que distingue revisar de mirar: **cada revisión deja una salida mínima escrita**, con límites (de 1 a 3 acciones, como máximo 3 prioridades, una decisión por proyecto, objetivos con criterio y fecha, y el ajuste por cambio escrito). El sistema 8 no es periódico: se dispara cuando cambia la situación.

ADR-019 (propuesto, sin migración) fija que la IA propone y la familia decide, y que la IA no escribe en repositorios. La tabla que ese ADR imaginó para `sprint_mission_participants` no se creó; el número libre al implementar este módulo es V116. Cuando ADR-019 se apruebe, su migración será la siguiente libre.

## Decisión

### 1. Un módulo de práctica, no un segundo sistema de información

`sistemas` muestra los diez sistemas, su función o regla, y enlaza a las pantallas que ya existen. No copia el texto del diario, no crea tareas ni eventos, y no cierra sprints.

La captura sigue en el diario (`JournalEntry`, bitácora y Mi Espacio). El calendario sigue en el sprint, la planeación y los días críticos. Las tareas siguen en `PlanTask` y `SprintMission`, con el responsable del plan como propuesta (ADR-019): esta pantalla no asigna compromisos.

### 2. La salida escrita vive en tablas nuevas

`if_system_reviews` + `if_system_review_items` guardan solo esa salida, con autor resuelto del principal (nunca del cliente) y visibilidad de familia: quien pasa `familySecurity` la lee; otra familia no.

| Revisión | Límite que el servidor rechaza si no se cumple |
|---|---|
| Diaria | Captura vacía, agenda comprobada, de 1 a 3 acciones |
| Semanal | Calendario revisado, tareas depuradas, compromisos de mensajes trasladados, de 1 a 3 prioridades |
| Mensual | Al menos un proyecto, cada uno con una decisión entre continuar, corregir, pausar o cerrar, y respuesta explícita a si un cambio de etapa pasó desapercibido |
| Anual | De 1 a 5 objetivos («pocos»), cada uno con criterio de logro y fecha de revisión |
| Ajuste por cambio | Qué ya no cabe, y al menos un ítem que se deja, se reduce o se pospone |

No hay efecto sobre ICF, riesgo ni Ruta de Conciencia.

### 3. Enlace al ritmo que ya existe, sin crearlo

- **Diaria:** si hay sprint activo y un check-in de esa fecha a nombre de quien revisa, se guarda `sprint_daily_id`. Si no hay check-in, la revisión se guarda igual y lo dice. No se fabrica un `SprintDaily` (exigiría textos que la persona no escribió).
- **Semanal:** si ya hay una retrospectiva de un sprint cerrado en la vecindad de esa semana, se guarda `retrospective_id`. No se cierra el sprint, no se reescribe `what_to_adjust` y no se llama a la IA.

Así se respeta ADR-019: la familia escribe las prioridades; el sistema no decide ni ejecuta.

### 4. La prueba de 4 semanas

Sobre los 28 días que terminan hoy, el módulo calcula dos de las tres preguntas:

1. Semanas con revisión semanal (la prueba pide al menos 3 de 4).
2. Si la captura se vació al menos una vez en cada una de esas semanas.

La pregunta «¿llegó algún compromiso a la fecha sin estar registrado?» no se puede inferir: lo que no se registró no está en los datos. La responde la familia y queda en `if_system_checks`, con el recuento de ese momento. Lo `PRIVATE` de otro miembro no entra en este cálculo.

## Consecuencias

- La navegación gana la entrada «Sistemas IF». Las pantallas de sprint, bitácora, plan y documentación siguen siendo el lugar de cada cosa.
- Un administrador sin perfil de miembro puede leer (como el resto de módulos con `familySecurity`) y no puede guardar: no hay autor humano que resolver.
- Riesgo: si la familia marca «captura vacía» sin haberla procesado, el registro no lo detecta. La casilla es una afirmación de quien revisa, no una lectura del diario.
