# ADR-011: IF-CAM — arquitectura cognitiva como marco conceptual, sin nuevos instrumentos ni fases durante el piloto

**Status:** Accepted
**Date:** 2026-07-25
**Deciders:** William Lopez

## Context

Un documento externo propone que las autoevaluaciones de un libro de referencia no se incorporen como cuestionarios independientes, sino que se descompongan en un modelo cognitivo unificado — **IF-CAM** (Integrity Family – Cognitive Architecture Model): un ciclo de 10 pasos (Percibir → Atender → Interpretar → Relacionar → Imaginar → Evaluar → Decidir → Actuar → Reflexionar → Aprender → Transformación), 8 dominios cognitivos (percepción, curiosidad, pensamiento crítico, flexibilidad cognitiva, creatividad, pensamiento analítico, pensamiento sistémico, autorregulación corporal) sintetizados en un dominio transversal **CCTF** (Competencias Cognitivas para la Transformación Familiar), y una propuesta concreta de refinar las fases de las microsimulaciones `SCENARIO_V1_2` de `NOTICE/THINK/ACT/AFTERMATH/EFFECT` (5 pasos) a `PERCEIVE/ATTEND/INTERPRET/CONNECT/IMAGINE/EVALUATE/DECIDE/ACT/OBSERVE/LEARN/TRANSFORM` (11 pasos).

### Verificado en este repo, no asumido

- Las fases de `SCENARIO_V1_2` (`NOTICE/THINK/ACT/AFTERMATH/EFFECT`) están **congeladas explícitamente** por [`Directriz_Operativa_Piloto_V1.2.md`](../Directriz_Operativa_Piloto_V1.2.md) (2026-07-25, Decisión 1): *"No se modificarán escalas (`rubric_level`/`score_value`) ni las fases de cada escenario... No se alterará la lógica diagnóstica (`RiskAlgoV1Engine`)."* Ese documento se comprometió (`ad11e8a`) apenas horas antes de esta propuesta, específicamente para proteger un piloto en curso con familias reales tras detectar que la Directriz V1.1 anterior no se sostuvo en la práctica.
- No existe en el repo ningún rastro previo de `IF-MI`, `IF-CAM` ni `CCTF` — es terminología enteramente nueva del documento externo, no continuación de trabajo ya empezado.
- `V89__poc_v1_2_parallel.sql` ya separa la situación (`questions.text`) del prompt clínico por fase (`questions.phase_prompt`) — el modelo de 5 fases ya tiene una estructura extensible, pero cae bajo el mismo freeze.
- El propio documento externo concluye que **no** deben construirse cuestionarios independientes — coincide con la Regla V1.1.1 (no capturar antes de tener evidencia de necesidad), ya aplicada repetidamente: ADR-008 pospuso IUC/ICP y la predicción prospectiva real; la Directriz V1.2 pospuso reconstruir `scenario_validation_log`.

## Decision

### Decisión 1 — el ciclo cognitivo se adopta como marco narrativo, no como schema nuevo

Se documenta el ciclo IF-CAM en `docs/vision.md` como una **lectura conceptual de las 5 fases ya existentes** de `SCENARIO_V1_2` — no como fases nuevas ni columnas nuevas:

| Fase actual (`SCENARIO_V1_2`, congelada) | Etapas cognitivas IF-CAM que ya cubre |
|---|---|
| `NOTICE` | Percibir + Atender |
| `THINK` | Interpretar + Relacionar + Imaginar |
| `ACT` | Evaluar + Decidir + Actuar |
| `AFTERMATH` | Insumo crudo de Observar (desenlace declarado) |
| `EFFECT` | Reflexionar + Aprender |

Esto muestra que el ciclo de 10 pasos no es incompatible con el instrumento actual — ya está implícito en 5 fases más gruesas. Refinar a 11 fases explícitas queda pospuesto a una eventual V1.3, después del cierre del piloto — exactamente el mecanismo que la propia Directriz V1.2 ya prevé para la evolución del instrumento ("...decisión de qué escenarios... ameritan una V1.3").

### Decisión 2 — los 8 dominios cognitivos (+ CCTF) se registran como hipótesis futura, no como instrumento

No se crean cuestionarios, tablas ni entidades nuevas para percepción/curiosidad/pensamiento crítico/flexibilidad cognitiva/creatividad/pensamiento analítico/sistémico/autorregulación corporal. Se documenta la intención en `vision.md` como candidata futura a `hypothesis_evidence` (ADR-004) — mismo patrón que la fila "Convivencia" ya usa en la tabla de `vision.md` ("Sin hipótesis propia todavía"). Si tras el piloto la evidencia de `PROXY_PREDICTIVE_ACCURACY_HYPOTHESIS` (ADR-008) muestra divergencias sistemáticas entre `THINK` y `AFTERMATH` no explicadas por el proxy actual, ahí hay motivo real para instrumentar un dominio cognitivo específico — no antes.

### Decisión 3 — por qué queda `Accepted` de inmediato, sin Action Items de construcción

A diferencia de ADR-006/009/010 (que dejaron entidades, migraciones y endpoints por construir), esta decisión es puramente de **alcance**: qué sí y qué no se construye ahora. El resultado completo es la documentación en sí — no hay pipeline de código pendiente que deje al ADR en estado intermedio.

## Trade-off Analysis

Frente a implementar el modelo de 11 fases ahora: se gana fidelidad conceptual inmediata, pero se rompe un freeze fijado explícitamente horas antes para proteger un piloto activo con familias reales — el mismo tipo de costo que la Directriz V1.2 ya pagó una vez (un banco "congelado" que en la práctica no lo estuvo). No se repite el error.

Frente a no integrar nada de la propuesta: se perdería un marco narrativo legítimo que ya conecta con trabajo real — ADR-007 (`pauseCapacity`) ya mide un fragmento de "Percibir/Atender antes de reaccionar", ADR-008 (`THINK` vs `AFTERMATH`) ya mide un fragmento de "Interpretar" vs "Observar". Documentarlo cuesta poco y deja el terreno preparado para V1.3.

## Consequences

- **Más fácil:** `vision.md` gana un marco unificador que explica por qué ADR-007/008 miden lo que miden, sin inventar instrumento nuevo ni tocar el piloto en curso.
- **Más difícil:** ninguna — cero cambios de código, cero migraciones.
- **Habrá que revisitar:** después del cierre del piloto V1.2, si la evidencia acumulada en `hypothesis_evidence` justifica (a) refinar las 5 fases a las 11 propuestas, o (b) instrumentar alguno de los 8 dominios cognitivos como cuestionario real — cada uno con su propio ADR, evidencia primero, igual que todo lo demás en este proyecto.

## Action Items

1. [x] ADR-011 documentado, verificado contra `Directriz_Operativa_Piloto_V1.2.md` y el código real (`V89__poc_v1_2_parallel.sql`, ausencia total de `IF-CAM`/`CCTF` previos).
2. [x] Sección nueva en `docs/vision.md` con el mapeo de 5 fases → ciclo cognitivo IF-CAM, y los 8 dominios cognitivos como hipótesis diferida.
3. [ ] (Diferido a V1.3, post-piloto) Refinar `SCENARIO_V1_2` a fases explícitas de 11 pasos — requiere levantar el freeze de la Directriz V1.2 primero, con evidencia que lo justifique.
4. [ ] (Diferido, post-piloto) Evaluar si algún dominio cognitivo (CCTF) amerita instrumento propio, conectado a `hypothesis_evidence` — no antes de tener evidencia de que el proxy actual (ADR-008) no explica las divergencias observadas.
