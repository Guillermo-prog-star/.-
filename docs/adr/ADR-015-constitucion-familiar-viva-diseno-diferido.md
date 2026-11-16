# ADR-015: Constitución Familiar Viva — diseño candidato, explícitamente diferido; línea roja ética sobre "juicio moral automatizado"

**Status:** Proposed (diferido)
**Date:** 2026-09-15
**Deciders:** William Lopez

## Context

Una sesión de lectura sobre los maestros de Simón Bolívar (Rodríguez, Bello, Andújar, Pelgrón/Vides/Carrasco, Palacios y Sojo, Marqués de Ustáriz) y sobre el pensamiento político de Bolívar (Carta de Jamaica, Discurso de Angostura) plantea una pregunta trasladable a Integrity Family: ¿cómo distinguir que una familia *mejoró su estado* (ICF/ICaF) de que *ganó capacidad de gobernarse sin el sistema*? Bolívar enfrentó el mismo problema a escala de nación — independencia (liberarse) no garantiza libertad sostenida (permanecer libre) sin instituciones propias.

Esta pregunta **no es nueva en este repo**: es, en sustancia, la Decisión 5 de [ADR-013](ADR-013-ciclo-if7-marco-conceptual.md#L53-L57) ("Autonomía Familiar" como hipótesis candidata, sin constructo ni métrica, condicionada a evidencia real del piloto V1.2 de familias dependientes de sugerencias IA pese a ICF estable). Este ADR no reabre ni duplica esa decisión. Su aporte es más angosto: (a) registrar un diseño de producto concreto para el día en que ese disparador ocurra — la "Constitución Familiar Viva" — siguiendo el mismo criterio de ADR-014 (registrar sin construir), y (b) fijar por escrito una línea roja ética que la lectura de Bolívar hace visible: el riesgo de que un mecanismo de "virtud" o "moral pública" —su propio Poder Moral en Angostura— se traduzca en un sistema que puntúe o vigile la moral de una familia.

### Verificado en este repo, no asumido

- **La hipótesis de autonomía ya existe y sigue sin disparador.** `hypothesis_evidence` (ADR-004) tiene registradas `PAF`, `DELIBERATIVE_INTERRUPTION_HYPOTHESIS`, `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS`, `RECOVERY_INDEX_HYPOTHESIS` — ninguna fila `AUTONOMY_HYPOTHESIS` (o equivalente). Confirma que ADR-013 Decisión 5 sigue en el mismo estado: candidata, no activada. Este ADR no cambia eso.
- **Ya existe un germen literal, pequeño, de "la familia declara su propia intención".** `plans.acceptance_status` / `accepted_at` / `accepted_by` / `intention_statement` (V111, [ADR-010](ADR-010-declaracion-intencion-fase0b-plan-acceptance.md)) permite que la familia acepte un plan generado por IA con una declaración de intención en texto libre. Es un campo único, ligado al ciclo de vida de un plan (`PROPOSED → ACCEPTED`), no un documento versionado con secciones propias (propósito, valores, derechos, responsabilidades, reglas de conflicto, reglas de decisión, crisis, revisión) como plantea la idea de "Constitución Familiar Viva". No hay colisión — hay una semilla mucho más estrecha que ya resuelve la pieza más básica ("la familia puede decir algo propio junto a lo que la IA propuso").
- **No existe ningún constructo de "capacidad familiar" o "gobernanza familiar" separado del ICF.** Búsqueda dirigida (`graduaci|retirada|constitucion.familiar|capacidad.familiar|soberania`) en todo el repo: sin resultados relevantes de dominio. Confirma que no hay nada parcialmente construido que este ADR deba reconciliar.
- **El vector de Goodhart ya documentado (ADR-013 Decisión 4) es el precedente correcto para juzgar cualquier "índice de autonomía" futuro.** El único punto verificado donde completar tareas mueve una aguja real del sistema sin pasar por el ICF es `MilestoneService.evaluate()` (umbral de ICF **y** % de tareas). Cualquier futura métrica de "capacidad familiar" tendría que evitar el mismo error: medir el proceso, no convertirse en el objetivo que la familia persigue para "aprobar".
- **El módulo `milestone` es el punto de extensión más cercano, no uno nuevo.** `MilestoneAwarePlanEngine`/`MilestoneService` ya formalizan avance por hitos condicionado a evidencia (ICF + tareas). Si algún día se decide medir progresión de autonomía, el precedente de ADR-014 Decisión 3 aplica igual aquí: extender `milestone`/`plan` antes que diseñar un bounded context nuevo.

## Decision

### Decisión 1 — no se reabre ni se duplica la hipótesis de Autonomía Familiar

Sigue vigente ADR-013 Decisión 5 tal como está escrita: sin constructo, sin métrica, sin fila en `hypothesis_evidence`, condicionada a evidencia real del piloto V1.2. La convergencia entre el problema político de Bolívar (libertad sin instituciones se pierde) y esta hipótesis es una coincidencia conceptual interesante — **no es evidencia empírica** y no adelanta el disparador que ADR-013 exige.

### Decisión 2 — "Constitución Familiar Viva" se registra como diseño candidato, sin construir nada

Idea de producto: un objeto versionado por familia, con secciones explícitas —propósito, valores declarados, derechos, responsabilidades por edad/rol, reglas de manejo de conflicto, reglas de reparación, matriz de quién decide qué, protocolo de crisis, condición de revisión— que evolucione por versiones (v1.0 → v1.1 → v2.0) a medida que la familia prueba y ajusta sus propios acuerdos.

Se registra aquí como referencia futura, igual que EF-Feynman en ADR-014, **sin agregarse a `vision.md`** (no es una lectura de un mecanismo que ya existe; es un instrumento nuevo sin construir). Si algún día se retoma, la primera opción a evaluar no es una entidad nueva: es extender `plans.intention_statement` (hoy un campo de texto libre, único, ligado a un plan) hacia algo estructurado y versionado — reutilizando el mismo lugar donde la familia ya declara intención, en vez de crear un dominio `constitution` paralelo.

### Decisión 3 — línea roja ética: ningún "Poder Moral" automatizado

Angostura propuso una institución para vigilar educación, costumbres y moral pública — Bolívar mismo reconoció que esa idea era ambiciosa y arriesgada incluso en su contexto. Trasladada a software, esa misma lógica ("el sistema determina si esta familia es moralmente buena") degenera en vigilancia, puntuación moral y presión normalizadora.

Se fija como principio de diseño, sin fecha de vencimiento ni condición de reapertura: Integrity Family no implementará ningún mecanismo que produzca un juicio de valor moral sobre una familia o sus integrantes (del tipo "familia buena/mala", "padre presente/ausente" como score). El límite operacional aceptable es exclusivamente descriptivo y siempre remite a lo que la propia familia declaró: *"ustedes declararon X; ocurrió Y; la evidencia disponible muestra Z; ¿quieren revisar su hipótesis, conducta o acuerdo?"* — nunca una calificación emitida por el sistema sobre el carácter de las personas. Este principio complementa, sin sustituir, el de ADR-013 Decisión 4 (métrica ≠ propósito): aquél protege contra optimizar el indicador equivocado; éste protege contra que el indicador sea, directamente, un juicio sobre las personas.

### Decisión 4 — explícitamente NO se construye ahora

- Ninguna migración, entidad, endpoint ni vista.
- Ningún campo nuevo en `plans` ni tabla satélite para "Constitución Familiar".
- Ninguna métrica de "capacidad familiar" ni de "autonomía" — sigue bloqueado por ADR-013 Decisión 5 hasta que exista el disparador que esa decisión exige.

## Trade-off Analysis

Frente a **construir la Constitución Familiar Viva ahora**: es una idea de producto atractiva y con un punto de extensión real (`intention_statement`), pero no tiene disparador — nadie ha pedido esto todavía, y el piloto V1.2 sigue siendo la prioridad de evidencia pendiente antes de sumar superficie nueva.

Frente a **no registrar nada**: se perdería el vínculo ya verificado con `intention_statement` como punto de extensión correcto, y una futura sesión redescubriría desde cero tanto la idea como la corrección de no duplicar ADR-013 Decisión 5.

Frente a **fijar la línea roja ética solo informalmente (sin ADR)**: más barato hoy, pero un principio de "nunca calificar moralmente a una familia" vale más escrito de forma permanente que disperso en el historial de conversación — es exactamente el tipo de decisión que ADR-013 Decisión 4 ya demostró que conviene registrar aunque no accione código.

## Consequences

- **Más fácil:** existe un registro único de "Constitución Familiar Viva" ligado a su punto de extensión real (`intention_statement`), evita que se reproponga como dominio nuevo, y queda escrita una línea roja ética explícita y permanente.
- **Más difícil:** nada — este ADR no añade superficie de código.
- **Habrá que revisitar:** si y solo si ADR-013 Decisión 5 se activa (evidencia real de dependencia en el piloto V1.2), empezar por extender `plans.intention_statement`, no por diseñar un dominio `constitution` nuevo.

## Action Items

1. [x] Registrar "Constitución Familiar Viva" como diseño candidato, sin construir nada.
2. [x] Fijar la línea roja ética contra juicio moral automatizado, sin condición de reapertura.
3. [ ] (Condicional) Retomar el diseño solo si ADR-013 Decisión 5 se activa — empezando por extender `plans.intention_statement`, no por una entidad nueva.
