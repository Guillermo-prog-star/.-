-- V75: Banco de Trayectorias de Riesgo Familiar
-- Sistema de seguimiento longitudinal de trayectorias de riesgo (35+ tipos, 9 macrodominios)

CREATE TABLE IF NOT EXISTS risk_trajectories (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(100) NOT NULL UNIQUE,
    name               VARCHAR(200) NOT NULL,
    macrodomain        VARCHAR(50)  NOT NULL,
    description        TEXT,
    early_signals      JSON,
    potential_evolution TEXT,
    severity_default   VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS family_risk_trajectories (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT       NOT NULL,
    trajectory_id    BIGINT       NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'DETECTED',
    detected_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at      DATETIME,
    notes            TEXT,
    assigned_by      VARCHAR(255),
    CONSTRAINT fk_frt_family     FOREIGN KEY (family_id)     REFERENCES families(id)           ON DELETE CASCADE,
    CONSTRAINT fk_frt_trajectory FOREIGN KEY (trajectory_id) REFERENCES risk_trajectories(id)  ON DELETE CASCADE,
    INDEX idx_frt_family_id (family_id),
    INDEX idx_frt_status    (status)
);

CREATE TABLE IF NOT EXISTS trajectory_timeline_events (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_trajectory_id BIGINT       NOT NULL,
    event_date           DATE         NOT NULL,
    age_at_event         SMALLINT,
    event_description    TEXT         NOT NULL,
    risk_level           VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    action_taken         TEXT,
    result               TEXT,
    recorded_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by          VARCHAR(255),
    CONSTRAINT fk_tte_family_trajectory FOREIGN KEY (family_trajectory_id) REFERENCES family_risk_trajectories(id) ON DELETE CASCADE,
    INDEX idx_tte_family_trajectory_id (family_trajectory_id),
    INDEX idx_tte_event_date (event_date)
);

CREATE TABLE IF NOT EXISTS trajectory_impact_indicators (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_trajectory_id BIGINT          NOT NULL,
    indicator_name       VARCHAR(200)    NOT NULL,
    indicator_key        VARCHAR(100)    NOT NULL,
    baseline_value       DECIMAL(10,2),
    current_value        DECIMAL(10,2),
    unit                 VARCHAR(50),
    higher_is_better     BOOLEAN         NOT NULL DEFAULT TRUE,
    measured_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes                TEXT,
    CONSTRAINT fk_tii_family_trajectory FOREIGN KEY (family_trajectory_id) REFERENCES family_risk_trajectories(id) ON DELETE CASCADE,
    INDEX idx_tii_family_trajectory_id (family_trajectory_id),
    UNIQUE KEY uq_tii_trajectory_key (family_trajectory_id, indicator_key)
);

-- ─── Seed: Catálogo de trayectorias ──────────────────────────────────────────

INSERT INTO risk_trajectories (code, name, macrodomain, description, early_signals, potential_evolution, severity_default) VALUES

-- RELACIONES DE PAREJA
('DIVORCIO_SEPARACION', 'Divorcio o separación', 'RELACIONES_PAREJA',
 'Proceso de ruptura de la pareja con impacto en el núcleo familiar.',
 '["Disminución de comunicación","Discusiones frecuentes","Ausencia de proyectos comunes","Distanciamiento emocional"]',
 'Separación definitiva con impacto en hijos, economía y estabilidad emocional de todos los miembros.',
 'HIGH'),

('INFIDELIDAD', 'Infidelidad', 'RELACIONES_PAREJA',
 'Ruptura de confianza en la relación de pareja.',
 '["Distanciamiento emocional","Ocultamiento","Pérdida de confianza","Cambios de comportamiento inexplicables"]',
 'Ruptura de la relación o proceso largo de reconstrucción de confianza.',
 'HIGH'),

('VIOLENCIA_INTRAFAMILIAR', 'Violencia intrafamiliar', 'RELACIONES_PAREJA',
 'Episodios de violencia verbal, psicológica o física dentro del hogar.',
 '["Insultos","Amenazas","Agresiones verbales","Control excesivo","Aislamiento de la víctima"]',
 'Escalada a violencia física, ruptura o daño psicológico severo en los miembros.',
 'CRITICAL'),

('VIOLENCIA_ECONOMICA', 'Violencia económica', 'RELACIONES_PAREJA',
 'Control abusivo de los recursos económicos por un miembro de la pareja.',
 '["Control total del dinero","Impedimento para trabajar","Dependencia económica forzada"]',
 'Dependencia total, imposibilidad de tomar decisiones, deterioro de la autoestima.',
 'HIGH'),

('CRISIS_PAREJA', 'Crisis de pareja', 'RELACIONES_PAREJA',
 'Deterioro de la relación de pareja sin llegar aún a ruptura.',
 '["Pérdida de afecto","Comunicación reducida al mínimo","Irritabilidad persistente","Falta de proyectos comunes"]',
 'Separación o estancamiento si no se interviene a tiempo.',
 'MEDIUM'),

-- CRIANZA Y ADOLESCENCIA
('CONSUMO_MARIHUANA', 'Consumo de marihuana', 'CRIANZA_ADOLESCENCIA',
 'Consumo recreativo o habitual de marihuana por parte de un adolescente o joven.',
 '["Cambio de amistades","Aislamiento familiar","Descenso del rendimiento escolar","Cambios de humor"]',
 'Dependencia, conflictos familiares recurrentes, deserción escolar, exposición a otras sustancias.',
 'HIGH'),

('CONSUMO_ALCOHOL_ADOLESCENTE', 'Consumo de alcohol en adolescente', 'CRIANZA_ADOLESCENCIA',
 'Consumo de alcohol por parte de un menor de edad.',
 '["Llegadas tarde","Olor a alcohol","Cambios conductuales","Nuevas amistades de riesgo"]',
 'Hábito sostenido, riñas, accidentes, deserción escolar.',
 'HIGH'),

('DELINCUENCIA_JUVENIL', 'Delincuencia juvenil', 'CRIANZA_ADOLESCENCIA',
 'Participación del adolescente en actividades delictivas.',
 '["Ausentismo escolar","Nuevas amistades de riesgo","Objetos sin explicación","Actitud desafiante"]',
 'Judicialización, privación de libertad, deterioro del proyecto de vida.',
 'CRITICAL'),

('DESERCION_ESCOLAR', 'Deserción escolar', 'CRIANZA_ADOLESCENCIA',
 'Abandono definitivo o intermitente del sistema educativo.',
 '["Inasistencias repetidas","Bajo rendimiento","Desinterés","Conflictos con docentes"]',
 'Abandono definitivo, dificultades de inserción laboral, mayor vulnerabilidad.',
 'HIGH'),

('BAJO_RENDIMIENTO_ACADEMICO', 'Bajo rendimiento académico', 'CRIANZA_ADOLESCENCIA',
 'Calificaciones persistentemente bajas con riesgo de pérdida del año escolar.',
 '["Falta de hábitos de estudio","No entrega tareas","Dificultades de concentración"]',
 'Repetición del año, desmotivación, deserción.',
 'MEDIUM'),

('EMBARAZO_ADOLESCENTE', 'Embarazo adolescente', 'CRIANZA_ADOLESCENCIA',
 'Embarazo en una menor de edad con impacto en su proyecto de vida y en la dinámica familiar.',
 '["Relaciones sin educación sexual","Ausencia de diálogo familiar sobre sexualidad"]',
 'Deserción escolar, dificultades económicas, impacto en el proyecto de vida.',
 'HIGH'),

('ACOSO_ESCOLAR', 'Acoso escolar (bullying)', 'CRIANZA_ADOLESCENCIA',
 'Situación de acoso o victimización dentro del entorno escolar.',
 '["Tristeza inexplicable","Rechazo a asistir al colegio","Cambios en el comportamiento","Pérdida de pertenencias"]',
 'Depresión, aislamiento, abandono escolar, daño en la autoestima.',
 'HIGH'),

('CIBERACOSO', 'Ciberacoso', 'CRIANZA_ADOLESCENCIA',
 'Acoso a través de medios digitales, redes sociales o mensajería.',
 '["Ansiedad tras usar el celular","Ocultar la pantalla","Tristeza después de conectarse","Cambios emocionales abruptos"]',
 'Aislamiento, ansiedad severa, daño en la imagen personal.',
 'HIGH'),

('USO_PROBLEMATICO_VIDEOJUEGOS', 'Uso problemático de videojuegos', 'CRIANZA_ADOLESCENCIA',
 'Tiempo excesivo en videojuegos con impacto en la vida familiar, escolar y social.',
 '["Más de 8 horas diarias","Irritabilidad al interrumpir","Descuido de tareas y obligaciones"]',
 'Aislamiento, fracaso escolar, trastornos del sueño.',
 'MEDIUM'),

('USO_PROBLEMATICO_REDES', 'Uso problemático de redes sociales', 'CRIANZA_ADOLESCENCIA',
 'Dependencia de redes sociales con impacto en el bienestar emocional.',
 '["Más de 6 horas de pantalla","Comparación constante","Alteraciones del sueño","Reducción de interacción familiar"]',
 'Ansiedad, baja autoestima, aislamiento.',
 'MEDIUM'),

-- SALUD MENTAL
('IDEACION_SUICIDA', 'Ideación suicida', 'SALUD_MENTAL',
 'Expresiones o pensamientos sobre hacerse daño o no querer vivir.',
 '["Expresiones de desesperanza","Aislamiento","Regalar objetos queridos","Despedidas inusuales","Cambios abruptos de humor"]',
 'Crisis suicida si no se interviene de forma oportuna y profesional.',
 'CRITICAL'),

('AUTOLESIONES', 'Autolesiones', 'SALUD_MENTAL',
 'Conductas de daño físico intencional como mecanismo de regulación emocional.',
 '["Marcas o cortes en el cuerpo","Uso de ropa de manga larga en verano","Aislamiento","Objetos cortantes ocultos"]',
 'Escalada en la frecuencia o gravedad de las lesiones, riesgo de daño severo.',
 'CRITICAL'),

('TRASTORNO_ALIMENTACION', 'Trastorno de la conducta alimentaria', 'SALUD_MENTAL',
 'Restricción, atracones o purgas con impacto en la salud física y emocional.',
 '["Restricción alimentaria severa","Atracones seguidos de culpa","Miedo extremo a subir de peso","Preocupación constante por la imagen"]',
 'Deterioro físico y emocional, hospitalización, riesgo vital.',
 'CRITICAL'),

('DUELO_COMPLICADO', 'Duelo complicado', 'SALUD_MENTAL',
 'Proceso de duelo que no evoluciona y genera deterioro funcional prolongado.',
 '["Aislamiento persistente tras una pérdida","Incapacidad para retomar rutinas","Tristeza que no cede"]',
 'Depresión prolongada, deterioro de relaciones y funcionamiento.',
 'HIGH'),

('AISLAMIENTO_SOCIAL', 'Aislamiento social', 'SALUD_MENTAL',
 'Retirada progresiva de las relaciones sociales y familiares.',
 '["Pérdida de amistades","Rechazo a salir","Comunicación mínima en casa","Cambios en el sueño y apetito"]',
 'Depresión, deterioro cognitivo y emocional.',
 'HIGH'),

('IDENTIDAD_GENERO', 'Identidad de género y diversidad familiar', 'SALUD_MENTAL',
 'Proceso de un integrante que expresa inconformidad con su género asignado o diversidad de orientación sexual, con impacto en la dinámica familiar.',
 '["Expresiones de incomodidad con el propio cuerpo","Distanciamiento","Búsqueda de referentes diversos","Conflictos familiares por expectativas de género"]',
 'Conflictos familiares sostenidos, rechazo, riesgo emocional para el joven si no hay apoyo.',
 'HIGH'),

-- ADICCIONES
('CONSUMO_ALCOHOL_ADULTO', 'Consumo problemático de alcohol en adulto', 'ADICCIONES',
 'Consumo excesivo o dependiente de alcohol por parte de un adulto del núcleo.',
 '["Consumo diario","Incumplimiento de responsabilidades","Conflictos asociados al consumo","Irritabilidad sin alcohol"]',
 'Dependencia, violencia, pérdida laboral, deterioro familiar.',
 'CRITICAL'),

('CONSUMO_OTRAS_SUSTANCIAS', 'Consumo de otras sustancias psicoactivas', 'ADICCIONES',
 'Uso de sustancias ilegales o medicamentos sin prescripción con impacto familiar.',
 '["Cambios bruscos de conducta","Problemas económicos inexplicables","Ocultamiento","Cambios físicos"]',
 'Trastorno por consumo, deterioro familiar y laboral.',
 'CRITICAL'),

('LUDOPATIA', 'Ludopatía (adicción al juego)', 'ADICCIONES',
 'Conducta compulsiva de apuestas con impacto económico y familiar.',
 '["Apuestas frecuentes","Mentiras sobre el dinero","Endeudamiento","Irritabilidad"]',
 'Endeudamiento severo, crisis económica, ruptura familiar.',
 'HIGH'),

('CONSUMO_TABACO', 'Consumo de tabaco', 'ADICCIONES',
 'Hábito tabáquico con impacto en la salud y el ejemplo para los hijos.',
 '["Consumo visible en casa","Intentos de dejar fallidos"]',
 'Daño a la salud, normalización en los hijos.',
 'LOW'),

-- EDUCACIÓN Y DESARROLLO
('JOVEN_SIN_PROYECTO', 'Joven sin proyecto de vida', 'EDUCACION_DESARROLLO',
 'Joven que no estudia ni trabaja y carece de metas claras.',
 '["Sin actividad formal","Desmotivación","Aislamiento","Dependencia económica total"]',
 'Exclusión social, vulnerabilidad, mayor riesgo de conductas problemáticas.',
 'HIGH'),

-- ECONOMÍA FAMILIAR
('ENDEUDAMIENTO_FAMILIAR', 'Endeudamiento familiar', 'ECONOMIA_FAMILIAR',
 'Gastos sistemáticamente superiores a los ingresos con deudas acumuladas.',
 '["Préstamos frecuentes","Mora en servicios","Conflictos por dinero","Ocultamiento de gastos"]',
 'Crisis económica, pérdida del hogar, deterioro de relaciones.',
 'HIGH'),

('DESEMPLEO_PROLONGADO', 'Desempleo prolongado', 'ECONOMIA_FAMILIAR',
 'Pérdida de empleo sostenida en el tiempo con impacto económico y emocional.',
 '["Ingresos detenidos","Deterioro del estado de ánimo","Tensión familiar","Pérdida de rutinas"]',
 'Deterioro económico y emocional, pérdida de la vivienda, crisis familiar.',
 'HIGH'),

('EMPRENDIMIENTO_FAMILIAR', 'Emprendimiento familiar en dificultad', 'ECONOMIA_FAMILIAR',
 'Negocio familiar que atraviesa dificultades económicas o de gestión.',
 '["Pérdidas recurrentes","Conflictos familiares por el negocio","Mezcla de roles familiar y laboral"]',
 'Quiebra, conflictos familiares sostenidos, pérdida del patrimonio.',
 'MEDIUM'),

-- GOBERNANZA FAMILIAR
('CRIANZA_PERMISIVA', 'Crianza permisiva', 'GOBERNANZA',
 'Ausencia de normas, límites y estructura en la crianza.',
 '["No hay reglas claras","Los hijos imponen su voluntad","Consecuencias sin seguimiento"]',
 'Conductas desafiantes, dificultades en la escuela y en la vida social.',
 'MEDIUM'),

('CRIANZA_AUTORITARIA', 'Crianza autoritaria', 'GOBERNANZA',
 'Imposición de normas sin diálogo ni participación de los hijos.',
 '["Castigos frecuentes y desproporcionados","Sin espacio para la opinión del hijo","Obediencia por miedo"]',
 'Rebeldía, ruptura del vínculo, baja autoestima, conductas de riesgo.',
 'MEDIUM'),

('CONFLICTOS_HERENCIAS', 'Conflictos por herencias', 'GOBERNANZA',
 'Disputas familiares por distribución de bienes o patrimonio.',
 '["Desacuerdos sobre bienes","Favoritismos percibidos","Exclusión de algún familiar"]',
 'Ruptura permanente de relaciones familiares.',
 'HIGH'),

-- ADULTO MAYOR
('ABANDONO_ADULTO_MAYOR', 'Abandono del adulto mayor', 'ADULTO_MAYOR',
 'Escasa atención y acompañamiento al adulto mayor del núcleo familiar.',
 '["Pocas visitas o llamadas","Delegación total del cuidado","Desconocimiento del estado de salud"]',
 'Soledad, deterioro funcional y emocional acelerado.',
 'HIGH'),

('DEPENDENCIA_ADULTO_MAYOR', 'Dependencia funcional del adulto mayor', 'ADULTO_MAYOR',
 'Situación de dependencia física o cognitiva del adulto mayor con impacto en el cuidador.',
 '["Limitación para actividades básicas","Carga elevada para el cuidador","Agotamiento familiar"]',
 'Agotamiento del cuidador, deterioro de la calidad de vida de todos.',
 'MEDIUM'),

-- LEGADO FAMILIAR
('RUPTURA_GENERACIONAL', 'Ruptura generacional', 'LEGADO',
 'Desconexión entre generaciones: los jóvenes desconocen o rechazan la historia familiar.',
 '["Nueva generación sin conocimiento de sus raíces","Pérdida de tradiciones","Desinterés por el legado"]',
 'Pérdida de identidad familiar, fragilidad ante crisis, ausencia de referentes.',
 'LOW'),

('MIGRACION_INTEGRANTE', 'Migración de un integrante', 'LEGADO',
 'Separación física de un miembro del núcleo por migración.',
 '["Separación geográfica","Reducción de comunicación","Duelo migratorio"]',
 'Debilitamiento del vínculo, cambios en los roles familiares.',
 'MEDIUM');

-- Documentación técnica en project_documents
INSERT INTO project_documents (code, title, category, content, summary, version, status, tags) VALUES
('DOC-TRAY-001', 'Banco de Trayectorias de Riesgo Familiar — Especificación',
 'TECHNICAL',
 '# Banco de Trayectorias de Riesgo Familiar\n\n## Propósito\nSistema de seguimiento longitudinal de trayectorias de riesgo que permite documentar señales tempranas, evolución y resultados de intervención familiar.\n\n## Macrodominios (9)\n1. RELACIONES_PAREJA — Divorcio, infidelidad, violencia, crisis de pareja, violencia económica\n2. CRIANZA_ADOLESCENCIA — Consumo de sustancias, delincuencia, deserción, acoso, embarazo adolescente\n3. SALUD_MENTAL — Ideación suicida, autolesiones, trastornos de alimentación, duelo complicado, aislamiento\n4. ADICCIONES — Alcohol, otras sustancias, ludopatía, tabaco\n5. EDUCACION_DESARROLLO — Joven sin proyecto de vida, bajo rendimiento, deserción\n6. ECONOMIA_FAMILIAR — Endeudamiento, desempleo, emprendimiento en dificultad\n7. GOBERNANZA — Crianza permisiva/autoritaria, conflictos por herencias\n8. ADULTO_MAYOR — Abandono, dependencia funcional\n9. LEGADO — Ruptura generacional, migración\n\n## Flujo de uso\n1. Guardiá o profesional asigna trayectoria a la familia (DETECTED)\n2. Se registran eventos en la línea de tiempo\n3. Se miden indicadores de impacto (baseline → current)\n4. Se actualiza el estado (IN_PROGRESS → RESOLVED | RELAPSED)\n5. El Mentor de IA recibe el contexto de trayectorias activas\n\n## Indicadores de impacto sugeridos por trayectoria\n- Conflictos/semana, cumplimiento de acuerdos (%), asistencia escolar (%)\n- Tiempo compartido en familia (horas/semana), percepción de apoyo familiar\n- Días libres de la conducta de riesgo, participación familiar (%)',
 'Especificación técnica del Banco de Trayectorias: 35+ trayectorias en 9 macrodominios con señales, evolución e indicadores.',
 '1.0', 'PUBLISHED',
 '["trayectorias","riesgo","macrodominios","impacto","longitudinal"]'),

('DOC-TRAY-002', 'Guía de Uso Clínico — Trayectorias de Riesgo',
 'GUIDE',
 '# Guía de Uso Clínico: Trayectorias de Riesgo Familiar\n\n## Principios orientadores\n- No se emiten diagnósticos clínicos; se documentan señales, evolución e intervenciones\n- El foco siempre es la familia como sistema, no el individuo como problema\n- Se mide lo que la plataforma puede influir: comunicación, apoyo, detección temprana, seguimiento\n\n## Cuándo asignar una trayectoria\n- Cuando el Guardián o un profesional identifica una situación de riesgo sostenida\n- Cuando hay señales tempranas confirmadas en más de una fuente (conversación, evaluación, bitácora)\n- Cuando la familia lo solicita explícitamente\n\n## Clasificación de estado\n- DETECTED: Se identifican señales, aún sin intervención activa\n- IN_PROGRESS: La familia está en proceso de intervención y seguimiento\n- RESOLVED: La situación mejoró de forma sostenida (mínimo 3 meses)\n- RELAPSED: Hubo mejora pero la situación reaparecio\n- CLOSED: Caso cerrado sin resolución o por cierre administrativo\n\n## Indicadores recomendados\nRegistrar siempre: baseline al inicio, medición cada 30-60 días.\nUsar unidades observables: frecuencias, porcentajes, horas, días.\n\n## Casos especiales: riesgo alto y crítico\n- IDEACION_SUICIDA, AUTOLESIONES, VIOLENCIA_INTRAFAMILIAR, DELINCUENCIA_JUVENIL: activar protocolo de contención inmediata\n- Siempre documentar: fecha de detección, acción realizada, persona responsable del seguimiento\n- El Mentor de IA incluirá estas trayectorias críticas como contexto prioritario en cada sesión',
 'Guía clínica de uso del Banco de Trayectorias: cuándo asignar, cómo medir, estados y casos especiales.',
 '1.0', 'PUBLISHED',
 '["guia","clinico","trayectorias","protocolo","indicadores"]');
