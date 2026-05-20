import { PlanTransformacion } from '../core/models/plan-transformacion.model';

export const PLANES_MOCK: PlanTransformacion[] = [
  {
    id: 'plan-emociones',
    pilar: 'EMOCIONES',
    titulo: 'Plan de Transformación en EMOCIONES',
    visionFamiliar: 'Cultivar un entorno seguro de validación emocional, empatía activa y autorregulación colectiva en el hogar.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 2,
    misiones: [
      {
        id: 'mis-emociones-1',
        titulo: 'Semáforo del Ánimo Familiar',
        estado: 'Pendiente',
        descripcionGeneral: 'Implementar un espacio diario para reconocer y validar los estados emocionales de cada miembro antes de iniciar la rutina nocturna.',
        microacciones: [
          { id: 'ma-e1', icono: 'palette', descripcion: 'Diseñar y ubicar un tablero visual con los colores del semáforo en la zona común.' },
          { id: 'ma-e2', icono: 'rate_review', descripcion: 'Registrar de forma individual el color del estado de ánimo al finalizar el día.' },
          { id: 'ma-e3', icono: 'psychology', descripcion: 'Aplicar una técnica de escucha empática (3 minutos) sin juzgar ni dar consejos inmediatos al miembro en "Rojo".' },
          { id: 'ma-e4', icono: 'volunteer_activism', descripcion: 'Registrar en la bitácora familiar una palabra de apoyo o agradecimiento hacia otro miembro del hogar.' }
        ]
      },
      {
        id: 'mis-emociones-ia',
        titulo: '[IA] Escucha Activa Contra Reactividad',
        estado: 'Pendiente',
        descripcionGeneral: 'Espacio diario nocturno de escucha activa de 10 minutos al final del día para mitigar el estrés parental y la reactividad emocional.',
        isAi: true,
        microacciones: [
          { id: 'ma-eia1', icono: 'psychology', descripcion: 'Pasar un objeto de habla para que cada miembro comparta sin ser juzgado o interrumpido.' },
          { id: 'ma-eia2', icono: 'alarm', descripcion: 'Dedicar 10 minutos exclusivos al final del día protegiendo el espacio de consejos no solicitados.' },
          { id: 'ma-eia3', icono: 'done_all', descripcion: 'Completar la dinámica colectiva al menos 4 noches en la semana.' },
          { id: 'ma-eia4', icono: 'rate_review', descripcion: 'Registrar en la bitácora familiar el nivel de tranquilidad colectiva alcanzado (1 al 5).' }
        ]
      }
    ]
  },
  {
    id: 'plan-comunicacion',
    pilar: 'COMUNICACION',
    titulo: 'Plan de Transformación en COMUNICACION',
    visionFamiliar: 'Consolidar un hogar íntegro, consciente y con comunicación plena en un horizonte longitudinal de 36 meses.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 1,
    misiones: [
      {
        id: 'mis-comunicacion-1',
        titulo: 'Cena sin celulares',
        estado: 'Pendiente',
        descripcionGeneral: 'Establecer una cena familiar de 15 minutos donde todos guarden sus dispositivos móviles para dialogar cara a cara.',
        microacciones: [
          { id: 'ma-c1', icono: 'settings', descripcion: 'Implementar una caja recolectora de celulares decorada en la mesa del comedor.' },
          { id: 'ma-c2', icono: 'assignment', descripcion: 'Subir una nota corta detallando las risas o temas de conversación de la cena.' },
          { id: 'ma-c3', icono: 'forum', descripcion: 'Introducir una "pregunta rompehielos" aleatoria por noche para guiar la conversación activa.' },
          { id: 'ma-c4', icono: 'phonelink_off', descripcion: 'Activar el modo "No molestar" programado en los dispositivos antes de sentarse a la mesa.' }
        ]
      }
    ]
  },
  {
    id: 'plan-habitos',
    pilar: 'HABITOS',
    titulo: 'Plan de Transformación en HÁBITOS',
    visionFamiliar: 'Coordinar rutinas saludables y sostenibles que promuevan la corresponsabilidad y el bienestar físico-mental.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 2,
    misiones: [
      {
        id: 'mis-habitos-1',
        titulo: 'Sincronización del Descanso',
        estado: 'Pendiente',
        descripcionGeneral: 'Establecer un ritual de desconexión digital cooperativo previo a las horas del sueño reparador.',
        microacciones: [
          { id: 'ma-h1', icono: 'light_mode', descripcion: 'Reducir la intensidad de la iluminación del hogar 30 minutos antes de dormir.' },
          { id: 'ma-h2', icono: 'auto_stories', descripcion: 'Reemplazar el uso de pantallas en la cama por lectura individual o música ambiental.' },
          { id: 'ma-h3', icono: 'alarm', descripcion: 'Fijar una alarma unificada de "Apagón Tecnológico" para todos los miembros de la casa.' },
          { id: 'ma-h4', icono: 'done_all', descripcion: 'Verificar el cumplimiento del orden de los espacios comunes compartidos antes de ir a descansar.' }
        ]
      },
      {
        id: 'mis-habitos-ia',
        titulo: '[IA] Receso Digital y Conexión Activa',
        estado: 'Pendiente',
        descripcionGeneral: 'Desconexión colectiva de pantallas 45 minutos antes de dormir, depositando dispositivos en una cesta común fuera de las habitaciones.',
        isAi: true,
        microacciones: [
          { id: 'ma-hia1', icono: 'phonelink_off', descripcion: 'Depositar todos los celulares en la Caja de Presencia familiar fuera de las habitaciones.' },
          { id: 'ma-hia2', icono: 'alarm', descripcion: 'Fijar una alarma familiar unificada 45 minutos antes de la hora de dormir.' },
          { id: 'ma-hia3', icono: 'done_all', descripcion: 'Mantener la desconexión colectiva de forma exitosa por 5 días consecutivos.' },
          { id: 'ma-hia4', icono: 'rate_review', descripcion: 'Registrar la asimilación del hábito y la calidad del descanso en la bitácora familiar.' }
        ]
      }
    ]
  },
  {
    id: 'plan-tiempos',
    pilar: 'TIEMPOS',
    titulo: 'Plan de Transformación en TIEMPOS',
    visionFamiliar: 'Optimizar la distribución del tiempo para equilibrar las obligaciones individuales con espacios de calidad colectiva.',
    progresoPilar: 0,
    misionesLogradas: 0,
    misionesTotales: 1,
    misiones: [
      {
        id: 'mis-tiempos-1',
        titulo: 'El Bloque Familiar Dorado',
        estado: 'Pendiente',
        descripcionGeneral: 'Agendar y blindar de forma estricta 1 hora semanal dedicada exclusivamente a actividades recreativas o lúdicas en equipo.',
        microacciones: [
          { id: 'ma-t1', icono: 'calendar_today', descripcion: 'Bloquear los calendarios laborales/académicos individuales el domingo por la tarde.' },
          { id: 'ma-t2', icono: 'sports_esports', descripcion: 'Alternar democráticamente la selección de la actividad de ocio familiar cada semana.' },
          { id: 'ma-t3', icono: 'timer', descripcion: 'Evaluar al final del bloque si se presentaron interrupciones externas y registrarlas.' },
          { id: 'ma-t4', icono: 'photo_camera', descripcion: 'Capturar una evidencia fotográfica del momento para el mural de la app como registro histórico.' }
        ]
      }
    ]
  }
];
