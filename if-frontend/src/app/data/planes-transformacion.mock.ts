import { PlanTransformacion } from '../core/models/plan-transformacion.model';

/**
 * PLANES_MOCK — 4 dimensiones × 3 misiones exactas por plan:
 *   [0] IA-1 (obligatoria)
 *   [1] IA-2 (obligatoria, se desbloquea al completar IA-1)
 *   [2] CREATIVA (opcional, se desbloquea al completar IA-2)
 *
 * Flujo: Misión → Sprint 7 días → Daily → Evidencia → Cápsula → Película
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
    misionesTotales: 3,
    misiones: [
      // ── IA-1 (obligatoria)
      {
        id: 'mis-emociones-1',
        titulo: 'Semáforo del Ánimo Familiar',
        estado: 'Pendiente',
        isAi: true,
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
      // ── IA-2 (obligatoria, se desbloquea al completar IA-1)
      {
        id: 'mis-emociones-ia-1',
        titulo: '[IA] Escucha Activa Contra Reactividad',
        estado: 'Pendiente',
        isAi: true,
        descripcionGeneral: 'Espacio diario nocturno de escucha activa de 10 minutos al final del día para mitigar el estrés parental y la reactividad emocional.',
        queBusca: 'Generar un amortiguador relacional y de contención emocional asertiva utilizando el diagnóstico de IA.',
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
      // ── CREATIVA (opcional, se desbloquea al completar IA-2)
      {
        id: 'mis-emociones-creativa',
        titulo: 'Ritual Emocional Semanal',
        estado: 'Pendiente',
        esIniciativaFamiliar: true,
        descripcionGeneral: 'Dinámica creativa semanal diseñada por la familia para anclar momentos de risa y afecto compartido.',
        queBusca: 'Crear memoria afectiva a largo plazo e inmunizar al núcleo familiar ante factores estresantes externos.',
        pasoAPaso: [
          'Consensuar una actividad recreativa libre de 15 minutos enfocada al humor (chistes, juego de mesa rápido, baile).',
          'Prohibir de forma estricta todo reclamo o conversación sobre responsabilidades durante el ritual.',
          'Expresar en una palabra al final cómo esta dinámica nutre los lazos del hogar.'
        ],
        microacciones: [
          { id: 'ma-ec1', icono: 'sports_esports', descripcion: 'Elegir por consenso la dinámica lúdica/emocional de la semana.' },
          { id: 'ma-ec2', icono: 'timer', descripcion: 'Disfrutar y blindar el espacio recreativo libre de tensiones externas.' },
          { id: 'ma-ec3', icono: 'photo_camera', descripcion: 'Capturar una evidencia del momento para la Cápsula Familiar.' }
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
    misionesTotales: 3,
    misiones: [
      // ── IA-1
      {
        id: 'mis-comunicacion-1',
        titulo: 'Cena sin Celulares',
        estado: 'Pendiente',
        isAi: true,
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
      // ── IA-2
      {
        id: 'mis-comunicacion-ia-1',
        titulo: '[IA] Caja de Diálogos Complejos',
        estado: 'Pendiente',
        isAi: true,
        descripcionGeneral: 'Escribir preguntas o temas difíciles de forma anónima para discutirlos en un entorno familiar asertivo y seguro.',
        queBusca: 'Canalizar tensiones subyacentes e implementar diálogos explicativos e inteligentes asistidos por IA.',
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
      // ── CREATIVA
      {
        id: 'mis-comunicacion-creativa',
        titulo: 'El Espejo del Diálogo',
        estado: 'Pendiente',
        esIniciativaFamiliar: true,
        descripcionGeneral: 'Dinámica creativa de reformulación recíproca para garantizar que se ha comprendido el mensaje del otro.',
        queBusca: 'Erradicar malentendidos y fortalecer el músculo cognitivo de la empatía comunicativa.',
        pasoAPaso: [
          'Durante una discusión o charla ordinaria, cada miembro repite con sus propias palabras lo que acaba de oír.',
          'Validar con la pregunta: "¿Es eso exactamente lo que querías transmitir?".',
          'Si la respuesta es afirmativa, proponer la solución cooperativa.'
        ],
        microacciones: [
          { id: 'ma-cc1', icono: 'psychology', descripcion: 'Implementar la reformulación en espejo en un diálogo cotidiano.' },
          { id: 'ma-cc2', icono: 'forum', descripcion: 'Verificar la comprensión de la contraparte antes de responder.' },
          { id: 'ma-cc3', icono: 'done_all', descripcion: 'Registrar la micro-victoria comunicativa en la bitácora familiar.' }
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
    misionesTotales: 3,
    misiones: [
      // ── IA-1
      {
        id: 'mis-habitos-1',
        titulo: 'Sincronización del Descanso',
        estado: 'Pendiente',
        isAi: true,
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
      // ── IA-2
      {
        id: 'mis-habitos-ia-1',
        titulo: '[IA] Receso Digital y Conexión Activa',
        estado: 'Pendiente',
        isAi: true,
        descripcionGeneral: 'Desconexión colectiva de pantallas 45 minutos antes de dormir, depositando dispositivos en una cesta común fuera de las habitaciones.',
        queBusca: 'Consolidar límites asertivos con el consumo tecnológico nocturno basado en métricas conductuales de IA.',
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
      // ── CREATIVA
      {
        id: 'mis-habitos-creativa',
        titulo: 'Alimentación Consciente y Conectada',
        estado: 'Pendiente',
        esIniciativaFamiliar: true,
        descripcionGeneral: 'Convertir al menos un almuerzo o desayuno semanal en un espacio ritual de plena conciencia compartida.',
        queBusca: 'Desacelerar el ritmo del día, saborear y agradecer los alimentos promoviendo una sana relación conductual.',
        pasoAPaso: [
          'Servir los platos y guardar silencio consciente durante los primeros 2 minutos de la comida.',
          'Saborear con atención identificando los ingredientes de forma atenta.',
          'Compartir al final un agradecimiento por el esfuerzo de quien preparó los alimentos.'
        ],
        microacciones: [
          { id: 'ma-hc1', icono: 'settings', descripcion: 'Establecer el ritual de alimentación libre de pantallas u afanes.' },
          { id: 'ma-hc2', icono: 'forum', descripcion: 'Expresar gratitud verbal al facilitador u elaborador del alimento.' },
          { id: 'ma-hc3', icono: 'done_all', descripcion: 'Evaluar y registrar la asimilación del hábito y la paz resultante.' }
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
    misionesTotales: 3,
    misiones: [
      // ── IA-1 (Llave del Bloque Dorado)
      {
        id: 'mis-tiempos-1',
        titulo: 'El Bloque Familiar Dorado',
        estado: 'Pendiente',
        isAi: true,
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
      // ── IA-2
      {
        id: 'mis-tiempos-2',
        titulo: 'Ritual de Bienvenida y Despedida',
        estado: 'Pendiente',
        isAi: true,
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
      // ── CREATIVA
      {
        id: 'mis-tiempos-creativa',
        titulo: 'Espacio de Autocuidado Parental',
        estado: 'Pendiente',
        esIniciativaFamiliar: true,
        descripcionGeneral: 'Establecer márgenes de tiempo blindados para que los cuidadores descansen o practiquen su propio bienestar.',
        queBusca: 'Prevenir el síndrome de desgaste o burnout parental, recargando las reservas de paciencia y amor.',
        pasoAPaso: [
          'Pactar cooperativamente un bloque de 30 minutos semanales donde un cuidador releva al otro de toda tarea.',
          'El cuidador libre practica una actividad individual (ejercicio, lectura, siesta).',
          'Agradecer el respaldo mutuo al cerrar el bloque con una palabra de reconocimiento.'
        ],
        microacciones: [
          { id: 'ma-tc1', icono: 'settings', descripcion: 'Acordar el calendario del relevo de autocuidado parental.' },
          { id: 'ma-tc2', icono: 'timer', descripcion: 'Ejecutar el bloque de recarga individual de manera blindada.' },
          { id: 'ma-tc3', icono: 'volunteer_activism', descripcion: 'Compartir palabras de aprecio y reportar la tranquilidad resultante.' }
        ]
      }
    ]
  }
];
