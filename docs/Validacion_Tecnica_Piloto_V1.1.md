# Validación Técnica y Metodológica - Hito V1.1 (Piloto Neurofenomenológico)

**Fecha:** 5 de Julio de 2026  
**Versión / Hito:** V1.1 (Piloto Técnico y Metodológico Listo para Validación Científica)  
**Objetivo:** Certificar el comportamiento de persistencia, navegación y experiencia de usuario del formulario de evaluación tripartita antes del inicio de pruebas de campo con familias reales.

---

## 1. Casos de Prueba Ejecutados y Resultados

| ID | Escenario | Criterio de Aceptación | Resultado |
|----|-----------|------------------------|-----------|
| **UX-01** | Completar Tríada (Auto-avance) | El sistema avanza automáticamente solo cuando las 3 fases (Entry, Timing, Action) tienen respuesta y `scenarioCompleted` marca `true`. Se aplica un delay de 600ms. | ✅ Aprobado |
| **UX-02** | Bloqueo de escenarios incompletos | Intentar avanzar a la fuerza sin responder una fase debe estar bloqueado. El guard `if(!canAdvance()) return;` lo previene. | ✅ Aprobado |
| **UX-03** | Bloqueo de escenarios futuros | No es posible hacer clic en segmentos futuros en la barra de progreso. | ✅ Aprobado |
| **UX-04** | Modo Revisión | Regresar a una pregunta completada permite cambiar opciones sin disparar auto-avance. | ✅ Aprobado |
| **DB-01** | Cero Duplicidad en Backend (UPSERT) | Reenviar la misma respuesta debe actualizar el score, manteniendo el número de registros (1 por escenario-fase). | ✅ Aprobado |
| **SYS-01** | Restauración de Sesión (F5) | Recargar la página recupera las respuestas de BD, reconstruye `scenarioCompleted` y posiciona el cursor en la primera pendiente. | ✅ Aprobado |

---

## 2. Evidencia de la prueba UPSERT (Cero Duplicidad)

Se ejecutó un script automatizado contra la API `/api/assessments/{evalId}/answers` utilizando un JWT de Administrador.

**Log de ejecución:**
```text
1. Authenticating as admin...
Login response: { token: 'eyJhbGci...', user: { id: 3, role: 'ROLE_ADMIN', familyId: 4 } }
2. Starting or getting active evaluation...
Evaluation ID: 29
3. Fetching answers before test...
Initial answers count: 0
4. Sending answer for Question 100 (Score 3)...
Answers count after first save: 1
5. Sending answer for Question 100 again (Score 5) to test duplicate prevention...
Answers count after second save (should be SAME): 1
Updated score for Q100 (should be 5): 5
✅ SUCCESS: Backend successfully performed UPSERT. No duplicates created.
```

**Mecanismo de Protección:** 
El backend utiliza triple redundancia contra duplicados:
1. `Map.set()` en Angular.
2. `computeIfAbsent` en Java (`AssessmentAnswerService`).
3. `UNIQUE KEY uq_ans_eval_question(evaluation_id, question_key)` a nivel de base de datos MySQL (agregado en Flyway V19).

---

## 3. Evidencia del Modo Revisión (`furthestIndex`)

Para prevenir que un usuario sea "empujado" accidentalmente hacia adelante al corregir una respuesta anterior, se introdujo el tracking de la variable `furthestIndex`.

```typescript
// evaluation.component.ts
const isReviewing = this.currentIndex < this.furthestIndex;
if (!wasAlreadyCompleted && !isReviewing) {
  setTimeout(() => this.nextScenario(), 600);
}
```
**Comportamiento certificado:** Cuando el usuario navega a un índice menor al más alto alcanzado (`isReviewing = true`), el auto-avance queda deshabilitado, permitiendo evaluación calmada de los cambios.

---

## 4. Evidencia de Restauración tras recarga (F5)

Al forzar una recarga (F5) o abandonar la página, el frontend invoca `restoreProgress()`, el cual reconstruye no solo las respuestas individuales, sino la validación de escenario completo:

```typescript
// Reconstruir scenarioCompleted para escenarios ya resueltos
for (let i = 0; i < this.scenarios.length; i++) {
  const s = this.scenarios[i];
  const entryOk = !s.entry || this.answers.has(s.entry.id);
  const timingOk = !s.timing || this.answers.has(s.timing.id);
  const actionOk = !s.action || this.answers.has(s.action.id);
  if (entryOk && timingOk && actionOk) {
    this.scenarioCompleted.add(s.parentKey);
  }
}
```
**Comportamiento certificado:** Tras recargar, la barra de progreso ilumina correctamente todos los escenarios completos y coloca al usuario en el primer escenario incompleto sin perder datos.

---

## 5. `scenario_bank` como Fuente Maestra y Activo Validable

Se ratifica que el **activo clínico validable es el Escenario** (no la pregunta aislada).
1. El backend relaciona las 3 preguntas (`ENTRY`, `TIMING`, `ACTION`) a un único `scenario_key` extraído del JSON maestro del banco de preguntas.
2. La validación en `EvaluationComponent.canAdvance()` verifica las 3 fases específicas del `parentKey`, no simplemente "tres respuestas cualquiera".

---

## 6. Limitaciones Conocidas y Pruebas Pendientes (Investigación de Campo)

Con el cierre del Hito V1.1, la unidad técnica se congela temporalmente. Los siguientes pasos pertenecen estrictamente al **Piloto de Campo con Familias**. 

### Limitaciones Conocidas para Monitorear:
1. **Comprensión de la Guía Experiencial:** Verificar cualitativamente si las familias leen y entienden el recordatorio: *"Responda desde lo que su cuerpo, emociones y pensamientos hacen normalmente"*.
2. **Fatiga:** El cuestionario consta de múltiples escenarios tripartitos; se debe medir empíricamente el tiempo de sesión promedio antes del abandono o frustración.

### Pruebas Empíricas Pendientes (Data Driven):
* Recolectar datos reales en `scenario_validation_log` (tablas crudas por sesión y familia).
* Obtener feedback de usabilidad real sobre el *delay* de 600ms del auto-avance (¿es muy rápido/lento para usuarios no técnicos?).
* No se realizarán adaptaciones metodológicas en la arquitectura hasta no contar con el volumen de evidencia que sustente el paso a V88 (estados de MASTER_VALIDATED).

---

## 7. Reglas de Evolución Post-Piloto

> [!IMPORTANT]
> **Regla V1.1.1: Evolución Basada en Evidencia**
> Ningún cambio al banco de escenarios (`scenario_bank`) podrá realizarse directamente por intuición.
> 
> Todo cambio futuro deberá originarse en evidencia real recolectada en `scenario_validation_log` durante este piloto. La evidencia deberá analizarse, aprobarse y aplicarse bajo un esquema estricto de versionado (v1.1.x, v1.2, etc.).
> 
> Esta regla convierte a *Integrity Family* en un **instrumento versionado, reproducible y científicamente auditable**.

---
*Documento generado automatizadamente para certificación del cierre técnico.*
