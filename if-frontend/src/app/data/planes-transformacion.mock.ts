import { PlanTransformacion } from '../core/models/plan-transformacion.model';

/**
 * PLANES_MOCK — 4 dimensiones × 6 misiones exactas por plan:
 *   [0] Clínica 1   [1] Clínica 2
 *   [2] IA-1 (fija) [3] IA-2 (contenido adaptado al diagnóstico por el componente)
 *   [4] Iniciativa 1 [5] Iniciativa 2
 */
export const PLANES_MOCK: PlanTransformacion[] = [
  // ─────────────────────────────────────────────────────────────────
  // EMOCIONES
  // ─────────────────────────────────────────────────────────────────
  {
    id: 'plan-emociones',
    pilar: 'EMOCIONES',
    titulo: 'Plan de Transformación en EMOCIONES',
    visionFamiliar: 'Cultivar un entorno seguro de validación emocional, empatía activa y autorregulación colectiva en el hogar.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 6,
    misiones: [
      // ── Clínica 1
      {
        id: 'mis-emociones-1',
        titulo: 'Semáforo del Ánimo Familiar',
        estado: 'Pendiente',
        descripcionGeneral: 'Implementar un espacio diario para reconocer y validar los estados emocionales de cada miembro al finalizar el día.',
        queBusca: 'Autopercepción consciente de la tensión familiar y mitigación de la reactividad antes del descanso.',
        pasoAPaso: [
          'Diseñar y ubicar un tablero visual con los colores del semáforo en una zona común del hogar.',
          'Cada integrante coloca de forma individual el color que representa su estado de ánimo antes de la rutina nocturna.',
          'Aplicar una escucha empática de 3 minutos sin juzgar ni dar consejos rápidos a quien esté en "Rojo" o "Amarillo".'
        ],
        microacciones: [
          { id: 'ma-e1', icono: 'palette', descripcion: 'Diseñar y ubicar un tablero visual con los colores del semáforo en la zona común.' },
          { id: 'ma-e2', icono: 'rate_review', descripcion: 'Registrar de forma individual el color del estado de ánimo al finalizar el día.' },
          { id: 'ma-e3', icono: 'psychology', descripcion: 'Aplicar una técnica de escucha empática (3 minutos) sin juzgar al miembro en "Rojo".' }
        ]
      },
      // ── Clínica 2
      {
        id: 'mis-emociones-2',
        titulo: 'Regulación Emocional Guiada',
        estado: 'Pendiente',
        descripcionGeneral: 'Práctica asertiva de técnicas de respiración diafragmática y coherencia cardíaca colectiva en situaciones de sobrecarga.',
        queBusca: 'Recuperar la calma fisiológica y corregulación del sistema nervioso ante la reactividad o discusiones del hogar.',
        pasoAPaso: [
          'Detener toda conversación activa cuando la tensión suba de nivel en el hogar.',
          'El Sentinel familiar guía una sesión de respiración 4-7-8 (inhalar en 4s, sostener en 7s, exhalar en 8s).',
          'Repetir durante 5 ciclos continuos hasta estabilizar el pulso y el tono de voz.'
        ],
        microacciones: [
          { id: 'ma-e3-1', icono: 'timer', descripcion: 'Pausar interacciones tensas al detectar reactividad y activar el protocolo de calma.' },
          { id: 'ma-e3-2', icono: 'psychology', descripcion: 'Guiar al equipo familiar en 5 ciclos de respiración profunda 4-7-8.' },
          { id: 'ma-e3-3', icono: 'volunteer_activism', descripcion: 'Verificar la reducción del tono de voz y continuar el diálogo constructivo.' }
        ]
      },
      // ── IA-1 (fija)
      {
        id: 'mis-emociones-ia-1',
        titulo: '[IA] Escucha Activa Contra Reactividad',
        estado: 'Pendiente',
        descripcionGeneral: 'Espacio diario nocturno de escucha activa de 10 minutos al final del día para mitigar el estrés parental y la reactividad emocional.',
        queBusca: 'Generar un amortiguador relacional y de contención emocional asertiva utilizando el diagnóstico de IA.',
        isAi: true,
        pasoAPaso: [
          'Reunirse en círculo en un lugar cómodo del hogar sin dispositivos electrónicos.',
          'Pasar un objeto físico (objeto de habla) que determine a quién le corresponde expresarse.',
          'Cada miembro comparte sus tensiones del día por 3 minutos mientras los demás ofrecen presencia pura y silencio comprensivo.'
        ],
        microacciones: [
          { id: 'ma-eia1', icono: 'psychology', descripcion: 'Pasar un objeto de habla para que cada miembro comparta sin ser juzgado o interrumpido.' },
          { id: 'ma-eia2', icono: 'alarm', descripcion: 'Dedicar 10 minutos exclusivos al final del día protegiendo el espacio de consejos no solicitados.' },
          { id: 'ma-eia3', icono: 'done_all', descripcion: 'Completar la dinámica colectiva al menos 4 noches en la semana.' }
        ]
      },
      // ── IA-2 (contenido adaptado al diagnóstico por el componente)
      {
        id: 'mis-emociones-ia-2',
        titulo: '[IA] Diagnóstico Emocional Adaptativo',
        estado: 'Pendiente',
        descripcionGeneral: 'Análisis Sentinel de los patrones emocionales activos según el hito diagnóstico actual de la familia.',
        queBusca: 'Mapear los patrones emocionales dominantes y diseñar intervenciones específicas según el diagnóstico actual.',
        isAi: true,
        pasoAPaso: [
          'Evaluar el indicador de reactividad familiar en el contexto del hito actual.',
          'Identificar las 2 emociones más frecuentes y disruptivas de la semana.',
          'Registrar los avances hacia el resultado esperado del hito vigente.'
        ],
        microacciones: [
          { id: 'ma-eia2-1', icono: 'analytics', descripcion: 'Completar el autodiagnóstico emocional en el portal Sentinel AI.' },
          { id: 'ma-eia2-2', icono: 'psychology', descripcion: 'Identificar y nombrar los 2 patrones emocionales más urgentes de la semana.' },
          { id: 'ma-eia2-3', icono: 'done_all', descripcion: 'Registrar los avances y enviar la evidencia para validación del hito.' }
        ]
      },
      // ── Iniciativa 1
      {
        id: 'mis-emociones-ini-1',
        titulo: 'Reconocimiento Emocional Mutuo (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Espacio personalizado para validar verbalmente las virtudes y esfuerzos emocionales de los integrantes.',
        queBusca: 'Consolidar el hábito del aprecio, disminuyendo la queja y potenciando el autorreconocimiento positivo.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Cada miembro escribe en una nota adhesiva un logro emocional que haya observado en otro miembro del hogar.',
          'Pegar las notas adhesivas en un "Muro de Agradecimientos" en el refrigerador o comedor.',
          'Leerlas colectivamente durante la cena dominical.'
        ],
        microacciones: [
          { id: 'ma-e5-1', icono: 'rate_review', descripcion: 'Escribir a mano un reconocimiento sincero enfocado en el esfuerzo emocional de otro miembro.' },
          { id: 'ma-e5-2', icono: 'volunteer_activism', descripcion: 'Compartir verbalmente o fijar la nota en el mural de luz visible de la casa.' },
          { id: 'ma-e5-3', icono: 'done_all', descripcion: 'Registrar la foto de las notas en la bitácora familiar para análisis de Sentinel AI.' }
        ]
      },
      // ── Iniciativa 2
      {
        id: 'mis-emociones-ini-2',
        titulo: 'Ritual Emocional Semanal (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Dinámica creativa semanal diseñada por la familia para anclar momentos de risa y afecto.',
        queBusca: 'Crear memoria afectiva a largo plazo e inmunizar al núcleo familiar ante factores estresantes externos.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Consensuar una actividad recreativa libre de 15 minutos enfocada al humor (chistes, juego de mesa rápido, baile).',
          'Prohibir de forma estricta todo reclamo o conversación sobre responsabilidades durante el ritual.',
          'Expresar en una palabra al final cómo esta dinámica nutre los lazos del hogar.'
        ],
        microacciones: [
          { id: 'ma-e6-1', icono: 'sports_esports', descripcion: 'Elegir por consenso la dinámica lúdica/emocional de la semana.' },
          { id: 'ma-e6-2', icono: 'timer', descripcion: 'Disfrutar y blindar el espacio recreativo libre de tensiones externas.' },
          { id: 'ma-e6-3', icono: 'photo_camera', descripcion: 'Capturar una evidencia fotográfica del momento e ICF resultante para validación.' }
        ]
      }
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // COMUNICACION
  // ─────────────────────────────────────────────────────────────────
  {
    id: 'plan-comunicacion',
    pilar: 'COMUNICACION',
    titulo: 'Plan de Transformación en COMUNICACIÓN',
    visionFamiliar: 'Establecer canales de diálogo honesto, asertividad empática y reducción activa de fricciones lingüísticas en el hogar.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 6,
    misiones: [
      // ── Clínica 1
      {
        id: 'mis-comunicacion-1',
        titulo: 'Cena sin Celulares',
        estado: 'Pendiente',
        descripcionGeneral: 'Establecer una cena familiar de 15 minutos donde todos guarden sus dispositivos móviles para dialogar cara a cara.',
        queBusca: 'Recuperar la comunicación presencial y asertiva libre de interferencias tecnológicas durante la alimentación.',
        pasoAPaso: [
          'Decorar juntos una caja de cartón bautizándola como "La Caja de Presencia".',
          'Depositar todos los celulares en modo silencioso dentro de la caja antes de sentarse a cenar.',
          'Establecer un diálogo abierto partiendo de un tema asertivo e interesante del día.'
        ],
        microacciones: [
          { id: 'ma-c1', icono: 'settings', descripcion: 'Implementar una caja recolectora de celulares decorada en la mesa del comedor.' },
          { id: 'ma-c2', icono: 'assignment', descripcion: 'Subir una nota corta detallando las risas o temas de conversación de la cena.' },
          { id: 'ma-c3', icono: 'forum', descripcion: 'Introducir una "pregunta rompehielos" aleatoria por noche para guiar la conversación activa.' }
        ]
      },
      // ── Clínica 2
      {
        id: 'mis-comunicacion-2',
        titulo: 'El Mensaje en Primera Persona',
        estado: 'Pendiente',
        descripcionGeneral: 'Entrenar el lenguaje asertivo usando la técnica del "Mensaje Yo" en lugar de acusaciones en segunda persona.',
        queBusca: 'Disminuir la actitud defensiva del núcleo familiar transformando reproches en necesidades emocionales claras.',
        pasoAPaso: [
          'Detectar expresiones automáticas de ataque (ej: "Tú siempre haces...").',
          'Reemplazarlas por la fórmula asertiva: "Me siento [Emoción] cuando sucede [Hecho] y necesito [Acuerdo]".',
          'Sostener y corregirse mutuamente de forma amable cuando alguien incurra en lenguaje reactivo.'
        ],
        microacciones: [
          { id: 'ma-c3-1', icono: 'psychology', descripcion: 'Aprender y dialogar en familia la estructura de la comunicación no violenta ("Mensaje Yo").' },
          { id: 'ma-c3-2', icono: 'rate_review', descripcion: 'Aplicar y validar la reformulación asertiva ante un desacuerdo real en casa.' },
          { id: 'ma-c3-3', icono: 'done_all', descripcion: 'Registrar la micro-victoria comunicativa en la libreta o bitácora de evolución.' }
        ]
      },
      // ── IA-1 (fija)
      {
        id: 'mis-comunicacion-ia-1',
        titulo: '[IA] Caja de Diálogos Complejos',
        estado: 'Pendiente',
        descripcionGeneral: 'Escribir preguntas o temas difíciles de forma anónima para discutirlos en un entorno familiar asertivo y seguro.',
        queBusca: 'Canalizar tensiones subyacentes e implementar diálogos explicativos e inteligentes asistidos por IA.',
        isAi: true,
        pasoAPaso: [
          'Colocar una urna o sobre rotulado en la sala.',
          'Cada integrante deposita dudas, quejas o temas difíciles de abordar directamente durante la semana.',
          'Abrir la urna en la sesión familiar del sábado y dialogar asertivamente usando turnos estructurados.'
        ],
        microacciones: [
          { id: 'ma-c2-1', icono: 'rate_review', descripcion: 'Instalar la urna física de diálogos complejos en el área común del hogar.' },
          { id: 'ma-c2-2', icono: 'forum', descripcion: 'Abrir de forma cooperativa los sobres de dudas o fricciones acumuladas.' },
          { id: 'ma-c2-3', icono: 'psychology', descripcion: 'Resolver los incidentes con escucha activa de 4 minutos por cada participante.' }
        ]
      },
      // ── IA-2 (contenido adaptado al diagnóstico por el componente)
      {
        id: 'mis-comunicacion-ia-2',
        titulo: '[IA] Mapa de Fricciones Comunicativas',
        estado: 'Pendiente',
        descripcionGeneral: 'Diagnóstico Sentinel de los patrones comunicativos críticos según el hito diagnóstico actual de la familia.',
        queBusca: 'Localizar los bloqueos lingüísticos repetitivos y generar un plan de intervención personalizado.',
        isAi: true,
        pasoAPaso: [
          'Identificar las 3 fricciones comunicativas más frecuentes de esta semana.',
          'Aplicar la técnica diagnóstica de reformulación asertiva en los conflictos detectados.',
          'Registrar el progreso esperado hacia el resultado del hito vigente.'
        ],
        microacciones: [
          { id: 'ma-cia2-1', icono: 'analytics', descripcion: 'Completar el diagnóstico comunicativo en el portal Sentinel AI.' },
          { id: 'ma-cia2-2', icono: 'forum', descripcion: 'Identificar y reformular las 3 fricciones lingüísticas más urgentes.' },
          { id: 'ma-cia2-3', icono: 'done_all', descripcion: 'Enviar la evidencia de progreso comunicativo para validación del hito.' }
        ]
      },
      // ── Iniciativa 1
      {
        id: 'mis-comunicacion-ini-1',
        titulo: 'El Espejo del Diálogo (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Dinámica creativa de reformulación recíproca para garantizar que se ha comprendido el mensaje del otro.',
        queBusca: 'Erradicar malentendidos y fortalecer el músculo cognitivo de la empatía comunicativa.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Durante una discusión o charla ordinaria, cada miembro debe repetir con sus propias palabras lo que acaba de oír de su contraparte.',
          'Validar con la pregunta: "¿Es eso exactamente lo que querías transmitir?".',
          'Si la respuesta es afirmativa, proponer la solución cooperativa.'
        ],
        microacciones: [
          { id: 'ma-c5-1', icono: 'psychology', descripcion: 'Implementar el paso a paso de la reformulación en espejo en un diálogo cotidiano.' },
          { id: 'ma-c5-2', icono: 'forum', descripcion: 'Verificar la comprensión de la contraparte antes de responder con un argumento.' },
          { id: 'ma-c5-3', icono: 'done_all', descripcion: 'Registrar en la bitácora familiar el nivel de disminución de malentendidos.' }
        ]
      },
      // ── Iniciativa 2
      {
        id: 'mis-comunicacion-ini-2',
        titulo: 'Pregunta Rompehielos Creativa (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Generar dinámicas divertidas de diálogo profundo a través de cuestionamientos curiosos e interesantes.',
        queBusca: 'Expandir la intimidad y el conocimiento mutuo más allá de los temas logísticos u obligatorios del día.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Cada integrante escribe una pregunta curiosa en una tarjeta (ej: "¿Qué superpoder tendrías hoy?").',
          'Mezclar las tarjetas en un frasco durante el desayuno o almuerzo.',
          'Extraer una tarjeta al azar y turnarse para responderla de manera libre y asertiva.'
        ],
        microacciones: [
          { id: 'ma-c6-1', icono: 'palette', descripcion: 'Elaborar el tarjetero o frasco creativo de preguntas curiosas.' },
          { id: 'ma-c6-2', icono: 'forum', descripcion: 'Desplegar y responder la pregunta rompehielos en equipo familiar.' },
          { id: 'ma-c6-3', icono: 'rate_review', descripcion: 'Anotar en la bitácora la pregunta más divertida y la respuesta de los niños.' }
        ]
      }
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // HABITOS
  // ─────────────────────────────────────────────────────────────────
  {
    id: 'plan-habitos',
    pilar: 'HABITOS',
    titulo: 'Plan de Transformación en HÁBITOS',
    visionFamiliar: 'Coordinar rutinas saludables y sostenibles que promuevan la corresponsabilidad y el bienestar físico-mental.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 6,
    misiones: [
      // ── Clínica 1
      {
        id: 'mis-habitos-1',
        titulo: 'Sincronización del Descanso',
        estado: 'Pendiente',
        descripcionGeneral: 'Establecer un ritual de desconexión digital cooperativo previo a las horas del sueño reparador.',
        queBusca: 'Restaurar la calidad del sueño familiar mitigando la luz azul y recuperando espacio de introspección.',
        pasoAPaso: [
          'Reducir la iluminación artificial del hogar 30 minutos antes de la hora de dormir.',
          'Apagar de forma concertada pantallas de TV, consolas de videojuegos y computadoras.',
          'Reemplazar dispositivos móviles en cama por lectura individual o música relajante.'
        ],
        microacciones: [
          { id: 'ma-h1', icono: 'light_mode', descripcion: 'Reducir la intensidad de la iluminación del hogar 30 minutos antes de dormir.' },
          { id: 'ma-h2', icono: 'auto_stories', descripcion: 'Reemplazar el uso de pantallas en la cama por lectura individual o música ambiental.' },
          { id: 'ma-h3', icono: 'alarm', descripcion: 'Fijar una alarma unificada de "Apagón Tecnológico" para todos los miembros de la casa.' }
        ]
      },
      // ── Clínica 2
      {
        id: 'mis-habitos-2',
        titulo: 'Corresponsabilidad Doméstica Activa',
        estado: 'Pendiente',
        descripcionGeneral: 'Distribuir y validar de manera transparente las tareas y roles cotidianos de orden en el hogar.',
        queBusca: 'Aliviar la sobrecarga de los cuidadores, promoviendo la autonomía y empatía activa en los hijos.',
        pasoAPaso: [
          'Listar las 10 micro-tareas diarias de limpieza y orden comunes.',
          'Asignar cooperativamente 2 tareas fijas para cada miembro de la casa según su rango de edad.',
          'Chequear al final del día la culminación armónica de las tareas sin reclamos verbales.'
        ],
        microacciones: [
          { id: 'ma-h3-1', icono: 'assignment', descripcion: 'Listar de forma abierta las micro-tareas domésticas diarias del hogar.' },
          { id: 'ma-h3-2', icono: 'settings', descripcion: 'Repartir con equidad los roles de orden y cuidado compartido.' },
          { id: 'ma-h3-3', icono: 'done_all', descripcion: 'Asegurar la corresponsabilidad activa al cierre de la jornada nocturna.' }
        ]
      },
      // ── IA-1 (fija)
      {
        id: 'mis-habitos-ia-1',
        titulo: '[IA] Receso Digital y Conexión Activa',
        estado: 'Pendiente',
        descripcionGeneral: 'Desconexión colectiva de pantallas 45 minutos antes de dormir, depositando dispositivos en una cesta común fuera de las habitaciones.',
        queBusca: 'Consolidar límites asertivos con el consumo tecnológico nocturno basado en métricas conductuales de IA.',
        isAi: true,
        pasoAPaso: [
          'Fijar una cesta común en la sala de estar a las 9:30 PM.',
          'Todos los miembros depositan sus dispositivos apagados dentro de la cesta.',
          'Dedicar los 45 minutos siguientes a dialogar brevemente o leer libros físicos antes de conciliar el sueño.'
        ],
        microacciones: [
          { id: 'ma-hia1', icono: 'phonelink_off', descripcion: 'Depositar todos los celulares en la Caja de Presencia familiar fuera de las habitaciones.' },
          { id: 'ma-hia2', icono: 'alarm', descripcion: 'Fijar una alarma familiar unificada 45 minutos antes de la hora de dormir.' },
          { id: 'ma-hia3', icono: 'rate_review', descripcion: 'Registrar la asimilación del hábito y la calidad del descanso en la bitácora familiar.' }
        ]
      },
      // ── IA-2 (contenido adaptado al diagnóstico por el componente)
      {
        id: 'mis-habitos-ia-2',
        titulo: '[IA] Diagnóstico de Adherencia de Hábitos',
        estado: 'Pendiente',
        descripcionGeneral: 'Evaluación Sentinel del nivel de sostenibilidad de rutinas según el hito diagnóstico actual de la familia.',
        queBusca: 'Determinar qué hábitos están consolidados y cuáles necesitan refuerzo inmediato según el avance clínico.',
        isAi: true,
        pasoAPaso: [
          'Revisar el cumplimiento de las rutinas establecidas en el hito actual.',
          'Identificar los 2 hábitos con menor adherencia y su causa raíz.',
          'Proponer un plan de refuerzo de 7 días hacia el resultado del hito vigente.'
        ],
        microacciones: [
          { id: 'ma-hia2-1', icono: 'analytics', descripcion: 'Completar el diagnóstico de adherencia en el portal Sentinel AI.' },
          { id: 'ma-hia2-2', icono: 'settings', descripcion: 'Identificar y priorizar los 2 hábitos con menor constancia.' },
          { id: 'ma-hia2-3', icono: 'done_all', descripcion: 'Enviar el plan de refuerzo como evidencia para validación del hito.' }
        ]
      },
      // ── Iniciativa 1
      {
        id: 'mis-habitos-ini-1',
        titulo: 'Alimentación Consciente y Conectada (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Convertir al menos un almuerzo o desayuno semanal en un espacio ritual de plena conciencia.',
        queBusca: 'Desacelerar el ritmo del día, saborear y agradecer los alimentos promoviendo una sana relación conductual.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Servir los platos y guardar silencio consciente durante los primeros 2 minutos de la comida.',
          'Saborear con atención identificando los ingredientes de forma atenta.',
          'Compartir al final un agradecimiento por el esfuerzo de quien preparó los alimentos.'
        ],
        microacciones: [
          { id: 'ma-h5-1', icono: 'settings', descripcion: 'Establecer el ritual de alimentación libre de pantallas u afanes.' },
          { id: 'ma-h5-2', icono: 'forum', descripcion: 'Expresar gratitud verbal al facilitador u elaborador del alimento.' },
          { id: 'ma-h5-3', icono: 'done_all', descripcion: 'Evaluar y registrar la asimilación del hábito y la paz resultante.' }
        ]
      },
      // ── Iniciativa 2
      {
        id: 'mis-habitos-ini-2',
        titulo: 'Orden Cooperativo Express (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Micro-rutina diaria dinámica y lúdica de orden rápido en equipo acompañada de música.',
        queBusca: 'Reconfigurar la pesadez de las tareas domésticas en una experiencia alegre y colaborativa.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Definir la canción enérgica familiar favorita.',
          'Iniciar la canción y ordenar la mayor cantidad de juguetes u objetos en desuso durante los 5 minutos que dure.',
          'Celebrar con un choque de manos cooperativo al culminar la canción.'
        ],
        microacciones: [
          { id: 'ma-h6-1', icono: 'sports_esports', descripcion: 'Seleccionar por consenso la canción enérgica y fijar la zona común a ordenar.' },
          { id: 'ma-h6-2', icono: 'timer', descripcion: 'Desplegar la dinámica express de orden en equipo al sonar el tema musical.' },
          { id: 'ma-h6-3', icono: 'rate_review', descripcion: 'Subir bitácora corta documentando las risas e impacto en la limpieza.' }
        ]
      }
    ]
  },

  // ─────────────────────────────────────────────────────────────────
  // TIEMPOS
  // ─────────────────────────────────────────────────────────────────
  {
    id: 'plan-tiempos',
    pilar: 'TIEMPOS',
    titulo: 'Plan de Transformación en TIEMPOS',
    visionFamiliar: 'Optimizar la distribución del tiempo para equilibrar las obligaciones individuales con espacios de calidad colectiva.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 6,
    misiones: [
      // ── Clínica 1
      {
        id: 'mis-tiempos-1',
        titulo: 'El Bloque Familiar Dorado',
        estado: 'Pendiente',
        descripcionGeneral: 'Agendar y blindar de forma estricta 1 hora semanal dedicada exclusivamente a actividades lúdicas o recreativas en equipo.',
        queBusca: 'Garantizar el espacio sagrado de homeostasis del hogar y diversión pura para recargar las reservas afectivas.',
        pasoAPaso: [
          'Reunirse el domingo por la tarde para planificar y bloquear la agenda semanal.',
          'Consensuar y agendar de manera fija 1 hora específica del fin de semana para jugar o salir juntos.',
          'Blindar el bloque apagando celulares y declinando compromisos externos o laborales.'
        ],
        microacciones: [
          { id: 'ma-t1', icono: 'calendar_today', descripcion: 'Bloquear los calendarios individuales el domingo fijando la cita del hogar.' },
          { id: 'ma-t2', icono: 'sports_esports', descripcion: 'Alternar democráticamente la selección de la actividad de ocio familiar cada semana.' },
          { id: 'ma-t3', icono: 'photo_camera', descripcion: 'Capturar una evidencia fotográfica del bloque dorado y subirla para desbloquear el hito.' }
        ]
      },
      // ── Clínica 2
      {
        id: 'mis-tiempos-2',
        titulo: 'Ritual de Bienvenida y Despedida',
        estado: 'Pendiente',
        descripcionGeneral: 'Fijar 2 minutos sagrados de conexión ocular y abrazo afectivo al iniciar y cerrar el día.',
        queBusca: 'Anclar la seguridad emocional y recordar que el hogar es un refugio seguro a través del contacto físico.',
        pasoAPaso: [
          'Detener afanes matutinos y dar un abrazo de 20 segundos antes de salir a la escuela o trabajo.',
          'Saludar mirándose a los ojos y con un contacto físico sincero al retornar en la tarde.',
          'Evitar reprender o interrogar sobre responsabilidades en los primeros 10 minutos de reencuentro.'
        ],
        microacciones: [
          { id: 'ma-t3-1', icono: 'volunteer_activism', descripcion: 'Implementar el abrazo matutino de 20 segundos de recarga afectiva.' },
          { id: 'ma-t3-2', icono: 'psychology', descripcion: 'Garantizar el recibimiento ocular asertivo y cálido al volver a casa.' },
          { id: 'ma-t3-3', icono: 'done_all', descripcion: 'Validar la asimilación del ritual y reportar el índice de tranquilidad.' }
        ]
      },
      // ── IA-1 (fija)
      {
        id: 'mis-tiempos-ia-1',
        titulo: '[IA] Optimización de Transiciones Diarias',
        estado: 'Pendiente',
        descripcionGeneral: 'Diseñar márgenes de tiempo inteligentes de 10 minutos entre el trabajo, escuela y vida familiar.',
        queBusca: 'Prevenir que el cansancio exterior contamine el clima emocional del hogar mediante transiciones asertivas.',
        isAi: true,
        pasoAPaso: [
          'Establecer una "zona de transición" de 10 minutos al ingresar a la casa.',
          'Cada miembro se cambia de ropa, se lava las manos y se sienta en silencio para descargar el estrés del exterior.',
          'Saludarse cálidamente y comenzar las actividades del hogar en un estado de presencia renovada.'
        ],
        microacciones: [
          { id: 'ma-t2-1', icono: 'timer', descripcion: 'Implementar el margen de transición de 10 minutos al retornar a casa.' },
          { id: 'ma-t2-2', icono: 'psychology', descripcion: 'Practicar un autoregistro de calma antes de entablar conversaciones.' },
          { id: 'ma-t2-3', icono: 'done_all', descripcion: 'Completar el ritual de transiciones asertivas durante la semana.' }
        ]
      },
      // ── IA-2 (contenido adaptado al diagnóstico por el componente)
      {
        id: 'mis-tiempos-ia-2',
        titulo: '[IA] Auditoría de Distribución de Tiempos',
        estado: 'Pendiente',
        descripcionGeneral: 'Análisis Sentinel del uso del tiempo relacional según el hito diagnóstico actual de la familia.',
        queBusca: 'Detectar fugas de tiempo vincular y redistribuir la agenda familiar hacia espacios de alto impacto afectivo.',
        isAi: true,
        pasoAPaso: [
          'Mapear la distribución actual del tiempo familiar en el hito vigente.',
          'Identificar los 3 bloques de tiempo con mayor valor relacional no aprovechado.',
          'Rediseñar la agenda semanal adaptada al objetivo del hito actual.'
        ],
        microacciones: [
          { id: 'ma-tia2-1', icono: 'analytics', descripcion: 'Completar el diagnóstico de distribución temporal en el portal Sentinel AI.' },
          { id: 'ma-tia2-2', icono: 'calendar_today', descripcion: 'Identificar y priorizar los 3 bloques de tiempo relacionales subutilizados.' },
          { id: 'ma-tia2-3', icono: 'done_all', descripcion: 'Registrar el plan de redistribución como evidencia para validación del hito.' }
        ]
      },
      // ── Iniciativa 1
      {
        id: 'mis-tiempos-ini-1',
        titulo: 'Espacio de Autocuidado Parental (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Establecer márgenes de tiempo blindados para que los cuidadores descansen o practiquen su propio bienestar.',
        queBusca: 'Prevenir el síndrome de desgaste o burnout parental, recargando las reservas de paciencia.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Pactar cooperativamente un bloque de 30 minutos semanales donde un cuidador releva al otro de toda tarea.',
          'El cuidador libre practica una actividad individual (ejercicio, lectura, siesta).',
          'Agradecer el respaldo mutuo al cerrar el bloque.'
        ],
        microacciones: [
          { id: 'ma-t5-1', icono: 'settings', descripcion: 'Acordar el calendario del relevo de autocuidado parental.' },
          { id: 'ma-t5-2', icono: 'timer', descripcion: 'Ejecutar el bloque de recarga individual de manera blindada.' },
          { id: 'ma-t5-3', icono: 'volunteer_activism', descripcion: 'Compartir palabras de aprecio y reportar la tranquilidad resultante.' }
        ]
      },
      // ── Iniciativa 2
      {
        id: 'mis-tiempos-ini-2',
        titulo: 'Inventario de Tiempos Perdidos (Iniciativa)',
        estado: 'Pendiente',
        descripcionGeneral: 'Análisis dinámico cooperativo para detectar las fugas de tiempo familiares provocadas por pantallas.',
        queBusca: 'Recuperar horas de valor relacional en el hogar reorganizando hábitos de consumo multimedia.',
        esIniciativaFamiliar: true,
        pasoAPaso: [
          'Cada integrante anota el tiempo de uso diario reportado por sus celulares al final de la semana.',
          'Sumar los tiempos familiares colectivos identificando el nivel de fuga relacional.',
          'Proponer una reestructuración de 30 minutos de pantalla por 30 minutos de diversión presencial.'
        ],
        microacciones: [
          { id: 'ma-t6-1', icono: 'rate_review', descripcion: 'Registrar cooperativamente el reporte semanal de tiempo en pantalla.' },
          { id: 'ma-t6-2', icono: 'psychology', descripcion: 'Co-crear la alternativa presencial de juego u diálogo para sustituir la fuga.' },
          { id: 'ma-t6-3', icono: 'done_all', descripcion: 'Reportar la recuperación de tiempo de calidad e incremento de sintonía.' }
        ]
      }
    ]
  }
];
