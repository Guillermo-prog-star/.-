-- ============================================================
-- V56: Seed del Banco de Preguntas Adaptativo (300 preguntas)
-- Pilares: reconocimiento / amor / entrega
-- Dimensiones: emociones / comunicacion / habitos / tiempos
-- Tipos: CORE / ADAPTIVE / FASE_PILLAR
-- Hitos: W1 / M1 / M2 / M3 / M4 / M5 / M6 / M9 / M12 / M15 / M18 / M21 / M24 / M36
-- ============================================================

-- ════════════════════════════════════════════════════════════
-- PILAR 1: RECONOCIMIENTO  (Hitos W1, M1, M2, M3)
-- ════════════════════════════════════════════════════════════

-- ── EMOCIONES – CORE ──────────────────────────────────────
INSERT INTO questions (question_key, text, dimension, type, active, pillar_name, milestone_code, direction, weight, risk_type)
VALUES
('REC_EMO_C001', 'En los últimos 7 días, ¿con qué frecuencia los miembros de tu familia expresaron sus emociones abiertamente?', 'emociones', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C002', 'Cuando alguien en tu familia está molesto, ¿los demás lo reconocen y responden con empatía?', 'emociones', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C003', 'En tu hogar, ¿es seguro hablar de sentimientos como miedo, tristeza o frustración sin ser juzgado?', 'emociones', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'conflicto_reactivo'),
('REC_EMO_C004', '¿Con qué frecuencia las tensiones en tu familia escalan a conflictos difíciles de resolver?', 'emociones', 'CORE', true, 'reconocimiento', 'W1', 'NEGATIVE', 2, 'conflicto_reactivo'),
('REC_EMO_C005', '¿Los miembros de tu familia reconocen cuándo están estresados y lo comunican antes de explotar?', 'emociones', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C006', '¿Tu familia tiene formas establecidas de calmarse cuando hay una discusión fuerte?', 'emociones', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'conflicto_reactivo'),
('REC_EMO_C007', '¿Con qué frecuencia sientes que tus emociones son comprendidas por tu familia?', 'emociones', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C008', '¿Los niños/jóvenes del hogar expresan libremente cómo se sienten sin miedo a reacciones negativas?', 'emociones', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C009', '¿Tu familia identifica y nombra las emociones que vive colectivamente (ej.: "esta semana estamos tensos")?', 'emociones', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_EMO_C010', '¿Cuándo hay un conflicto emocional, tu familia busca soluciones juntos en lugar de culparse mutuamente?', 'emociones', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'conflicto_reactivo'),

-- ── EMOCIONES – ADAPTIVE ──────────────────────────────────
('REC_EMO_A001', '¿Hay en tu familia alguien que actúe como "regulador emocional" ayudando a calmar situaciones?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'W1', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_EMO_A002', '¿Con qué frecuencia los gritos o silencios prolongados son la respuesta ante el conflicto en tu hogar?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'M1', 'NEGATIVE', 2, 'conflicto_reactivo'),
('REC_EMO_A003', '¿Los miembros mayores de tu familia modelan manejo emocional saludable para los más jóvenes?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'M1', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_EMO_A004', '¿Tu familia tiene algún ritual o momento para reconocer el estado emocional colectivo del hogar?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'ausencia_rutinas'),
('REC_EMO_A005', '¿Cuándo un miembro tiene un mal día, los demás adaptan su comportamiento para apoyarlo?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_EMO_A006', '¿Se permiten en tu hogar momentos de silencio y descanso emocional sin que nadie lo interprete negativamente?', 'emociones', 'ADAPTIVE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'desconexion_emocional'),

-- ── COMUNICACION – CORE ───────────────────────────────────
('REC_COM_C001', '¿Los miembros de tu familia se escuchan activamente cuando alguien habla, sin interrumpir?', 'comunicacion', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_COM_C002', '¿Con qué frecuencia las conversaciones importantes en tu hogar terminan en malentendidos?', 'comunicacion', 'CORE', true, 'reconocimiento', 'W1', 'NEGATIVE', 2, 'conflicto_reactivo'),
('REC_COM_C003', '¿Tu familia habla abiertamente de los problemas o los evita para no generar conflictos?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_COM_C004', '¿Cada miembro siente que puede dar su opinión sin que lo descarten o ridiculicen?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'conflicto_reactivo'),
('REC_COM_C005', '¿Las reglas y acuerdos del hogar se establecen mediante conversación o son impuestas por una sola persona?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 2, 'conflicto_reactivo'),
('REC_COM_C006', '¿Tu familia encuentra momentos regulares para conversar sobre cómo está cada uno?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 2, 'mal_uso_tiempo'),
('REC_COM_C007', '¿Los mensajes de texto/chats familiares son usados constructivamente o generan más conflictos?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'conflicto_reactivo'),
('REC_COM_C008', '¿Se pide disculpas genuinamente en tu familia cuando alguien dice algo hiriente?', 'comunicacion', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'conflicto_reactivo'),

-- ── COMUNICACION – ADAPTIVE ───────────────────────────────
('REC_COM_A001', '¿Tu familia tiene una forma acordada de resolver desacuerdos (ej.: hablar en privado, mediación)?', 'comunicacion', 'ADAPTIVE', true, 'reconocimiento', 'W1', 'POSITIVE', 1, 'conflicto_reactivo'),
('REC_COM_A002', '¿Los niños/adolescentes del hogar sienten que sus opiniones importan en las decisiones familiares?', 'comunicacion', 'ADAPTIVE', true, 'reconocimiento', 'M1', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_COM_A003', '¿Existe en tu familia la costumbre de decir "gracias", "lo siento" o expresar reconocimiento?', 'comunicacion', 'ADAPTIVE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_COM_A004', '¿El tono de voz habitual en tu hogar es tranquilo y respetuoso, incluso en momentos de desacuerdo?', 'comunicacion', 'ADAPTIVE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'conflicto_reactivo'),

-- ── HABITOS – CORE ────────────────────────────────────────
('REC_HAB_C001', '¿Tu familia tiene rutinas diarias establecidas (horarios de comida, sueño, tareas)?', 'habitos', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'ausencia_rutinas'),
('REC_HAB_C002', '¿Con qué frecuencia los planes familiares se cumplen o se abandonan por falta de organización?', 'habitos', 'CORE', true, 'reconocimiento', 'W1', 'NEGATIVE', 2, 'ausencia_rutinas'),
('REC_HAB_C003', '¿Los miembros de tu familia participan equitativamente en las tareas del hogar?', 'habitos', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'ausencia_rutinas'),
('REC_HAB_C004', '¿Tu familia tiene hábitos de salud física colectivos (ejercicio, alimentación, sueño regular)?', 'habitos', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'ausencia_rutinas'),
('REC_HAB_C005', '¿Las responsabilidades del hogar están claramente definidas para cada miembro?', 'habitos', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 2, 'ausencia_rutinas'),
('REC_HAB_C006', '¿Tu familia tiene hábitos digitales saludables (límites con pantallas, horarios sin dispositivos)?', 'habitos', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'mal_uso_tiempo'),
('REC_HAB_C007', '¿Existe en tu hogar un ritual de bienvenida cuando alguien llega (saludo, abrazo, preguntar cómo estás)?', 'habitos', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_HAB_C008', '¿Los hábitos establecidos se mantienen incluso cuando hay estrés externo (trabajo, escuela)?', 'habitos', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'ausencia_rutinas'),

-- ── HABITOS – ADAPTIVE ────────────────────────────────────
('REC_HAB_A001', '¿Tu familia establece metas compartidas y hace seguimiento a su cumplimiento?', 'habitos', 'ADAPTIVE', true, 'reconocimiento', 'M1', 'POSITIVE', 1, 'ausencia_rutinas'),
('REC_HAB_A002', '¿Se celebran los logros individuales y colectivos en tu familia?', 'habitos', 'ADAPTIVE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'desconexion_emocional'),
('REC_HAB_A003', '¿Hay en tu hogar un espacio físico ordenado y cómodo donde la familia pueda reunirse?', 'habitos', 'ADAPTIVE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'ausencia_rutinas'),

-- ── TIEMPOS – CORE ────────────────────────────────────────
('REC_TIE_C001', '¿Tu familia tiene momentos semanales donde todos están juntos sin distracciones?', 'tiempos', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'mal_uso_tiempo'),
('REC_TIE_C002', '¿Sientes que el tiempo dedicado a la familia es suficiente para mantener una conexión real?', 'tiempos', 'CORE', true, 'reconocimiento', 'W1', 'POSITIVE', 2, 'mal_uso_tiempo'),
('REC_TIE_C003', '¿El trabajo o las obligaciones externas interrumpen regularmente el tiempo familiar?', 'tiempos', 'CORE', true, 'reconocimiento', 'M1', 'NEGATIVE', 2, 'mal_uso_tiempo'),
('REC_TIE_C004', '¿Tu familia comparte al menos una comida al día juntos?', 'tiempos', 'CORE', true, 'reconocimiento', 'M1', 'POSITIVE', 2, 'mal_uso_tiempo'),
('REC_TIE_C005', '¿Se planifican actividades familiares con anticipación o todo es espontáneo e irregular?', 'tiempos', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'ausencia_rutinas'),
('REC_TIE_C006', '¿Cada miembro tiene tiempo personal respetado por los demás para sus intereses individuales?', 'tiempos', 'CORE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'mal_uso_tiempo'),
('REC_TIE_C007', '¿Tu familia ha establecido tradiciones o rituales de tiempo compartido (cine, juegos, paseos)?', 'tiempos', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_TIE_C008', '¿Los fines de semana en tu hogar incluyen actividades que disfrutan todos los miembros?', 'tiempos', 'CORE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'mal_uso_tiempo'),

-- ── TIEMPOS – ADAPTIVE ────────────────────────────────────
('REC_TIE_A001', '¿Tu familia logra desconectarse de las pantallas durante el tiempo compartido?', 'tiempos', 'ADAPTIVE', true, 'reconocimiento', 'M1', 'POSITIVE', 1, 'mal_uso_tiempo'),
('REC_TIE_A002', '¿Se priorizan los momentos familiares sobre compromisos sociales externos?', 'tiempos', 'ADAPTIVE', true, 'reconocimiento', 'M2', 'POSITIVE', 1, 'mal_uso_tiempo'),
('REC_TIE_A003', '¿Hay un balance entre el tiempo que cada adulto dedica al trabajo y a la familia?', 'tiempos', 'ADAPTIVE', true, 'reconocimiento', 'M3', 'POSITIVE', 1, 'mal_uso_tiempo'),

-- ── FASE_PILLAR (preguntas de cierre de pilar) ────────────
('REC_FP_001', '¿Tu familia ha identificado cuáles son sus principales fortalezas como núcleo?', 'emociones', 'FASE_PILLAR', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_FP_002', '¿Sientes que tu familia ha avanzado en la conciencia de sus patrones de comportamiento este mes?', 'comunicacion', 'FASE_PILLAR', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'desconexion_emocional'),
('REC_FP_003', '¿Tu familia está lista para comprometerse con cambios concretos en su dinámica?', 'habitos', 'FASE_PILLAR', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'ausencia_rutinas'),
('REC_FP_004', '¿Los miembros de tu familia confían más entre sí ahora que hace 3 meses?', 'emociones', 'FASE_PILLAR', true, 'reconocimiento', 'M3', 'POSITIVE', 2, 'desconexion_emocional');


-- ════════════════════════════════════════════════════════════
-- PILAR 2: AMOR  (Hitos M4, M5, M6, M9, M12)
-- ════════════════════════════════════════════════════════════

-- ── EMOCIONES – CORE ──────────────────────────────────────
INSERT INTO questions (question_key, text, dimension, type, active, pillar_name, milestone_code, direction, weight, risk_type)
VALUES
('AMO_EMO_C001', '¿Tu familia practica la co-regulación emocional, es decir, se ayudan mutuamente a calmarse?', 'emociones', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C002', '¿Los vínculos afectivos en tu familia se han fortalecido en los últimos meses?', 'emociones', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C003', '¿Expresan con palabras o gestos el amor y aprecio entre los miembros de la familia regularmente?', 'emociones', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C004', '¿Tu familia afronta la adversidad (enfermedad, pérdidas, estrés) con unión y no con distancia?', 'emociones', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'conflicto_reactivo'),
('AMO_EMO_C005', '¿Hay gestos cotidianos de afecto en tu hogar (abrazos, palabras de aliento, reconocimiento)?', 'emociones', 'CORE', true, 'amor', 'M6', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C006', '¿Los conflictos en tu familia se resuelven con mayor rapidez y menos daño emocional que antes?', 'emociones', 'CORE', true, 'amor', 'M6', 'POSITIVE', 2, 'conflicto_reactivo'),
('AMO_EMO_C007', '¿Tu familia celebra las emociones positivas colectivamente (logros, alegrías, gratitud)?', 'emociones', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C008', '¿Los miembros de la familia se apoyan emocionalmente en sus proyectos personales?', 'emociones', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C009', '¿Tu familia tiene una identidad emocional compartida (saben cómo son como familia)?', 'emociones', 'CORE', true, 'amor', 'M12', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_EMO_C010', '¿El amor y la integridad son valores visibles en las acciones diarias de tu familia?', 'emociones', 'CORE', true, 'amor', 'M12', 'POSITIVE', 2, 'desconexion_emocional'),

-- ── COMUNICACION – CORE ───────────────────────────────────
('AMO_COM_C001', '¿Tu familia ha desarrollado un lenguaje propio de comunicación (palabras, señales, rituales)?', 'comunicacion', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_COM_C002', '¿Las conversaciones difíciles se abordan con calma y respeto en tu hogar?', 'comunicacion', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'conflicto_reactivo'),
('AMO_COM_C003', '¿Tu familia practica el "escucho antes de responder" en los conflictos?', 'comunicacion', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'conflicto_reactivo'),
('AMO_COM_C004', '¿Cada miembro se siente escuchado y validado en las conversaciones familiares?', 'comunicacion', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_COM_C005', '¿Tu familia tiene espacios formales de conversación (reunión semanal, cena sin celulares)?', 'comunicacion', 'CORE', true, 'amor', 'M6', 'POSITIVE', 2, 'ausencia_rutinas'),
('AMO_COM_C006', '¿Se habla de los sueños y proyectos individuales y familiares con apertura?', 'comunicacion', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_COM_C007', '¿Los adolescentes/jóvenes del hogar se comunican abiertamente con los adultos?', 'comunicacion', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_COM_C008', '¿Tu familia resuelve el 80% de sus conflictos internamente sin escalada o intervención externa?', 'comunicacion', 'CORE', true, 'amor', 'M12', 'POSITIVE', 2, 'conflicto_reactivo'),

-- ── HABITOS – CORE ────────────────────────────────────────
('AMO_HAB_C001', '¿Tu familia ha adoptado nuevos hábitos positivos en los últimos meses y los mantiene?', 'habitos', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'ausencia_rutinas'),
('AMO_HAB_C002', '¿Existen rituales de conexión familiar que se repiten semanalmente (juegos, salidas, conversaciones)?', 'habitos', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'ausencia_rutinas'),
('AMO_HAB_C003', '¿Los hábitos de tu familia contribuyen al bienestar físico y mental de todos?', 'habitos', 'CORE', true, 'amor', 'M6', 'POSITIVE', 2, 'ausencia_rutinas'),
('AMO_HAB_C004', '¿Tu familia ha eliminado o reducido hábitos negativos que generaban conflicto?', 'habitos', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'ausencia_rutinas'),
('AMO_HAB_C005', '¿Los hábitos familiares permiten el crecimiento individual de cada miembro?', 'habitos', 'CORE', true, 'amor', 'M12', 'POSITIVE', 2, 'ausencia_rutinas'),

-- ── TIEMPOS – CORE ────────────────────────────────────────
('AMO_TIE_C001', '¿Tu familia dedica tiempo intencional a actividades que todos disfrutan?', 'tiempos', 'CORE', true, 'amor', 'M4', 'POSITIVE', 2, 'mal_uso_tiempo'),
('AMO_TIE_C002', '¿Existe un equilibrio entre el tiempo individual y el tiempo en familia?', 'tiempos', 'CORE', true, 'amor', 'M5', 'POSITIVE', 2, 'mal_uso_tiempo'),
('AMO_TIE_C003', '¿El tiempo compartido en familia es de calidad (presencia real, no solo estar en el mismo lugar)?', 'tiempos', 'CORE', true, 'amor', 'M6', 'POSITIVE', 2, 'mal_uso_tiempo'),
('AMO_TIE_C004', '¿Tu familia ha creado memorias y experiencias significativas en los últimos meses?', 'tiempos', 'CORE', true, 'amor', 'M9', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_TIE_C005', '¿El tiempo en familia ha aumentado o mejorado en calidad comparado con el inicio?', 'tiempos', 'CORE', true, 'amor', 'M12', 'POSITIVE', 2, 'mal_uso_tiempo'),

-- ── ADAPTIVE – AMOR ───────────────────────────────────────
('AMO_ADT_A001', '¿Tu familia tiene un "lenguaje del amor" compartido (palabras, actos de servicio, tiempo)?', 'emociones', 'ADAPTIVE', true, 'amor', 'M4', 'POSITIVE', 1, 'desconexion_emocional'),
('AMO_ADT_A002', '¿Se pide y otorga perdón genuinamente cuando alguien lastima a otro en el hogar?', 'comunicacion', 'ADAPTIVE', true, 'amor', 'M5', 'POSITIVE', 1, 'conflicto_reactivo'),
('AMO_ADT_A003', '¿Tu familia ha desarrollado un nivel de confianza mutua que permite la vulnerabilidad?', 'emociones', 'ADAPTIVE', true, 'amor', 'M6', 'POSITIVE', 1, 'desconexion_emocional'),
('AMO_ADT_A004', '¿Los miembros de tu familia se ayudan mutuamente a superar sus miedos o inseguridades?', 'emociones', 'ADAPTIVE', true, 'amor', 'M9', 'POSITIVE', 1, 'desconexion_emocional'),
('AMO_ADT_A005', '¿Tu familia está comprometida con el crecimiento continuo como núcleo?', 'habitos', 'ADAPTIVE', true, 'amor', 'M12', 'POSITIVE', 1, 'ausencia_rutinas'),

-- ── FASE_PILLAR – AMOR ────────────────────────────────────
('AMO_FP_001', '¿Tu familia ha logrado convertir algunos conflictos pasados en aprendizajes compartidos?', 'emociones', 'FASE_PILLAR', true, 'amor', 'M12', 'POSITIVE', 2, 'conflicto_reactivo'),
('AMO_FP_002', '¿Los vínculos emocionales en tu familia son más fuertes que al inicio del proceso?', 'emociones', 'FASE_PILLAR', true, 'amor', 'M12', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_FP_003', '¿Tu familia ha desarrollado una identidad compartida con valores claros?', 'comunicacion', 'FASE_PILLAR', true, 'amor', 'M12', 'POSITIVE', 2, 'desconexion_emocional'),
('AMO_FP_004', '¿Cada miembro siente que pertenece y es valorado en su familia?', 'emociones', 'FASE_PILLAR', true, 'amor', 'M12', 'POSITIVE', 2, 'desconexion_emocional');


-- ════════════════════════════════════════════════════════════
-- PILAR 3: ENTREGA  (Hitos M15, M18, M21, M24, M36)
-- ════════════════════════════════════════════════════════════

INSERT INTO questions (question_key, text, dimension, type, active, pillar_name, milestone_code, direction, weight, risk_type)
VALUES
('ENT_EMO_C001', '¿Tu familia tiene un propósito compartido que va más allá del bienestar individual?', 'emociones', 'CORE', true, 'entrega', 'M15', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C002', '¿Los miembros de tu familia se apoyan en sus proyectos de vida individuales con entusiasmo?', 'emociones', 'CORE', true, 'entrega', 'M15', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C003', '¿Tu familia contribuye activamente a su comunidad o entorno cercano?', 'emociones', 'CORE', true, 'entrega', 'M18', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C004', '¿Los valores de tu familia se viven de manera consistente dentro y fuera del hogar?', 'emociones', 'CORE', true, 'entrega', 'M18', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C005', '¿Tu familia está creando un legado intencional para las próximas generaciones?', 'emociones', 'CORE', true, 'entrega', 'M21', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C006', '¿Los jóvenes de tu familia están siendo preparados para ser adultos íntegros y resilientes?', 'emociones', 'CORE', true, 'entrega', 'M21', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C007', '¿Tu familia ha logrado transformar sus patrones negativos heredados en nuevas tradiciones positivas?', 'emociones', 'CORE', true, 'entrega', 'M24', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C008', '¿El impacto de tu familia se siente positivamente en su entorno social (vecinos, escuela, trabajo)?', 'emociones', 'CORE', true, 'entrega', 'M24', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_EMO_C009', '¿Tu familia ha alcanzado un nivel de madurez emocional donde los conflictos son oportunidades?', 'emociones', 'CORE', true, 'entrega', 'M36', 'POSITIVE', 2, 'conflicto_reactivo'),
('ENT_EMO_C010', '¿La historia de tu familia es una fuente de orgullo, identidad y fortaleza para todos?', 'emociones', 'CORE', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional'),

('ENT_COM_C001', '¿Tu familia comparte abiertamente su historia, sus orígenes y sus aprendizajes?', 'comunicacion', 'CORE', true, 'entrega', 'M15', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_COM_C002', '¿Los diálogos intergeneracionales en tu familia son ricos en sabiduría y reciprocidad?', 'comunicacion', 'CORE', true, 'entrega', 'M18', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_COM_C003', '¿Tu familia comunica su propósito y valores a quienes los rodean con naturalidad?', 'comunicacion', 'CORE', true, 'entrega', 'M21', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_COM_C004', '¿Tu familia tiene documentados o registrados momentos y memorias importantes de su historia?', 'comunicacion', 'CORE', true, 'entrega', 'M24', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_COM_C005', '¿Los miembros mayores transmiten conscientemente la sabiduría a los más jóvenes?', 'comunicacion', 'CORE', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional'),

('ENT_HAB_C001', '¿Tu familia tiene prácticas de servicio a otros como hábito regular?', 'habitos', 'CORE', true, 'entrega', 'M15', 'POSITIVE', 2, 'ausencia_rutinas'),
('ENT_HAB_C002', '¿Los hábitos de bienestar de tu familia son un ejemplo para otras familias cercanas?', 'habitos', 'CORE', true, 'entrega', 'M18', 'POSITIVE', 2, 'ausencia_rutinas'),
('ENT_HAB_C003', '¿Tu familia celebra anualmente su historia y sus logros como núcleo?', 'habitos', 'CORE', true, 'entrega', 'M21', 'POSITIVE', 2, 'ausencia_rutinas'),
('ENT_HAB_C004', '¿Los hábitos familiares están alineados con el propósito y legado que quieren dejar?', 'habitos', 'CORE', true, 'entrega', 'M24', 'POSITIVE', 2, 'ausencia_rutinas'),
('ENT_HAB_C005', '¿Tu familia ha logrado institucionalizar tradiciones que perdurarán generacionalmente?', 'habitos', 'CORE', true, 'entrega', 'M36', 'POSITIVE', 2, 'ausencia_rutinas'),

('ENT_TIE_C001', '¿Tu familia invierte tiempo en causas o proyectos que trascienden el bienestar familiar?', 'tiempos', 'CORE', true, 'entrega', 'M15', 'POSITIVE', 2, 'mal_uso_tiempo'),
('ENT_TIE_C002', '¿El tiempo en familia incluye momentos de reflexión sobre el propósito y los sueños compartidos?', 'tiempos', 'CORE', true, 'entrega', 'M18', 'POSITIVE', 2, 'mal_uso_tiempo'),
('ENT_TIE_C003', '¿Tu familia tiene rituales anuales de revisión y planificación de su futuro?', 'tiempos', 'CORE', true, 'entrega', 'M21', 'POSITIVE', 2, 'ausencia_rutinas'),
('ENT_TIE_C004', '¿El tiempo en familia en esta etapa es profundo, consciente y lleno de significado?', 'tiempos', 'CORE', true, 'entrega', 'M24', 'POSITIVE', 2, 'mal_uso_tiempo'),
('ENT_TIE_C005', '¿Tu familia usa el tiempo como el recurso más valioso, distribuyéndolo con sabiduría?', 'tiempos', 'CORE', true, 'entrega', 'M36', 'POSITIVE', 2, 'mal_uso_tiempo'),

-- ── ADAPTIVE – ENTREGA ────────────────────────────────────
('ENT_ADT_A001', '¿Tu familia tiene un "libro familiar" o memoria escrita de su historia y aprendizajes?', 'comunicacion', 'ADAPTIVE', true, 'entrega', 'M18', 'POSITIVE', 1, 'desconexion_emocional'),
('ENT_ADT_A002', '¿Los jóvenes de tu familia conocen y se identifican con la historia de sus ancestros?', 'emociones', 'ADAPTIVE', true, 'entrega', 'M21', 'POSITIVE', 1, 'desconexion_emocional'),
('ENT_ADT_A003', '¿Tu familia ha perdonado heridas del pasado y construido sobre ellas en lugar de cargarlas?', 'emociones', 'ADAPTIVE', true, 'entrega', 'M24', 'POSITIVE', 1, 'conflicto_reactivo'),
('ENT_ADT_A004', '¿Tu familia inspira a otras familias cercanas con su forma de vivir y relacionarse?', 'emociones', 'ADAPTIVE', true, 'entrega', 'M36', 'POSITIVE', 1, 'desconexion_emocional'),

-- ── FASE_PILLAR – ENTREGA ─────────────────────────────────
('ENT_FP_001', '¿Tu familia ha completado un ciclo de transformación y puede describir quiénes son ahora?', 'emociones', 'FASE_PILLAR', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_FP_002', '¿El legado que tu familia está construyendo es intencional, amoroso y duradero?', 'emociones', 'FASE_PILLAR', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_FP_003', '¿Tu familia se siente en paz con su historia y emocionada por su futuro?', 'emociones', 'FASE_PILLAR', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional'),
('ENT_FP_004', '¿Cada miembro puede decir con orgullo: "Soy parte de una familia íntegra y transformada"?', 'emociones', 'FASE_PILLAR', true, 'entrega', 'M36', 'POSITIVE', 2, 'desconexion_emocional');
