-- V97: Criterio de protocolo de seguridad para el Banco de Trayectorias de Riesgo (V75)
-- Puertas: A) riesgo vital/daño irreversible en corto plazo, B) sujeto de especial
-- protección en indefensión, C) obligación legal de reporte, D) excede la capacidad
-- familiar y requiere red externa de forma inmediata.
-- Una trayectoria activa protocolo obligatorio si cumple >= 1 puerta sin condición.
-- Si solo la cumple bajo un contexto (edad, dependencia, tipo de contenido), queda
-- como condicional en contextual_criticality_rule, evaluable caso por caso.

ALTER TABLE risk_trajectories
    ADD COLUMN requires_safety_protocol    BOOLEAN NOT NULL DEFAULT FALSE AFTER severity_default,
    ADD COLUMN contextual_criticality_rule TEXT NULL AFTER requires_safety_protocol;

-- ─── (1) Confirmadas críticas — cumplen >= 1 puerta sin condición ────────────
UPDATE risk_trajectories SET requires_safety_protocol = TRUE
WHERE code IN (
    'VIOLENCIA_INTRAFAMILIAR',
    'DELINCUENCIA_JUVENIL',
    'IDEACION_SUICIDA',
    'AUTOLESIONES',
    'TRASTORNO_ALIMENTACION',
    'CONSUMO_ALCOHOL_ADULTO',
    'CONSUMO_OTRAS_SUSTANCIAS'
);

-- ─── (2) Condicionales — críticas solo bajo un modificador de contexto ───────
UPDATE risk_trajectories
SET contextual_criticality_rule = 'Activa protocolo de seguridad si la edad de la persona menor es inferior a 14 años (acceso carnal abusivo — denuncia obligatoria, independiente del consentimiento).'
WHERE code = 'EMBARAZO_ADOLESCENTE';

UPDATE risk_trajectories
SET contextual_criticality_rule = 'Activa protocolo de seguridad si coexiste con dependencia funcional confirmada del adulto mayor (indefensión real, no solo distanciamiento afectivo).'
WHERE code = 'ABANDONO_ADULTO_MAYOR';

UPDATE risk_trajectories
SET contextual_criticality_rule = 'Activa protocolo de seguridad si la dependencia es avanzada y el cuidador reporta agotamiento severo (excede la capacidad familiar, requiere red externa de inmediato).'
WHERE code = 'DEPENDENCIA_ADULTO_MAYOR';

UPDATE risk_trajectories
SET contextual_criticality_rule = 'Activa protocolo de seguridad si involucra distribución o solicitud de contenido sexual de una persona menor de edad (delito de reporte obligatorio, distinto del ciberacoso genérico).'
WHERE code = 'CIBERACOSO';

-- ─── (3) Reencuadre: la identidad/orientación no es una trayectoria de riesgo ─
-- El objeto de riesgo real es el conflicto o rechazo familiar ante la diversidad,
-- no la identidad de la persona. Se corrige código, nombre, descripción y señales.
UPDATE risk_trajectories
SET code = 'CONFLICTO_FAMILIAR_POR_DIVERSIDAD',
    name = 'Conflicto familiar por diversidad de identidad u orientación',
    description = 'Conflicto o rechazo dentro de la familia ante la expresión de identidad de género u orientación sexual de un integrante, con impacto en la dinámica familiar.',
    early_signals = '["Conflictos familiares por expectativas de género u orientación","Rechazo o distanciamiento hacia el integrante","Silenciamiento del tema en el hogar","Búsqueda de apoyo fuera de la familia"]',
    potential_evolution = 'Ruptura del vínculo familiar, aislamiento del integrante, riesgo emocional si no hay acompañamiento y apertura al diálogo.'
WHERE code = 'IDENTIDAD_GENERO';

-- ─── (4) DOC-TRAY-002: corregir lista de protocolo (faltaban 3 CRITICAL) ─────
UPDATE project_documents
SET content = '# Guía de Uso Clínico: Trayectorias de Riesgo Familiar\n\n## Principios orientadores\n- No se emiten diagnósticos clínicos; se documentan señales, evolución e intervenciones\n- El foco siempre es la familia como sistema, no el individuo como problema\n- Se mide lo que la plataforma puede influir: comunicación, apoyo, detección temprana, seguimiento\n\n## Cuándo asignar una trayectoria\n- Cuando el Guardián o un profesional identifica una situación de riesgo sostenida\n- Cuando hay señales tempranas confirmadas en más de una fuente (conversación, evaluación, bitácora)\n- Cuando la familia lo solicita explícitamente\n\n## Clasificación de estado\n- DETECTED: Se identifican señales, aún sin intervención activa\n- IN_PROGRESS: La familia está en proceso de intervención y seguimiento\n- RESOLVED: La situación mejoró de forma sostenida (mínimo 3 meses)\n- RELAPSED: Hubo mejora pero la situación reaparecio\n- CLOSED: Caso cerrado sin resolución o por cierre administrativo\n\n## Indicadores recomendados\nRegistrar siempre: baseline al inicio, medición cada 30-60 días.\nUsar unidades observables: frecuencias, porcentajes, horas, días.\n\n## Criterio de protocolo de seguridad (4 puertas)\nUna trayectoria activa protocolo de seguridad obligatorio si cumple al menos una:\n- A. Riesgo vital o daño irreversible en el corto plazo (días-semanas)\n- B. Sujeto de especial protección (menor, adulto mayor dependiente, persona con discapacidad) en indefensión\n- C. Obligación legal de reporte o denuncia\n- D. Excede la capacidad familiar, requiere red externa de forma inmediata\n\n## Trayectorias con protocolo de seguridad obligatorio (requires_safety_protocol = true)\nIDEACION_SUICIDA, AUTOLESIONES, VIOLENCIA_INTRAFAMILIAR, DELINCUENCIA_JUVENIL, TRASTORNO_ALIMENTACION, CONSUMO_ALCOHOL_ADULTO, CONSUMO_OTRAS_SUSTANCIAS.\nSiempre documentar: fecha de detección, acción realizada, persona responsable del seguimiento.\nEl Mentor de IA incluirá estas trayectorias críticas como contexto prioritario en cada sesión.\n\n## Trayectorias con criticidad condicional (contextual_criticality_rule no nulo)\nEMBARAZO_ADOLESCENTE, ABANDONO_ADULTO_MAYOR, DEPENDENCIA_ADULTO_MAYOR, CIBERACOSO: solo activan protocolo de seguridad si se cumple la condición descrita en contextual_criticality_rule. Se evalúa caso por caso, no por defecto del catálogo.',
    summary = 'Guía clínica de uso del Banco de Trayectorias: cuándo asignar, cómo medir, estados, criterio de 4 puertas para protocolo de seguridad y casos condicionales.',
    version = '1.1'
WHERE code = 'DOC-TRAY-002';
