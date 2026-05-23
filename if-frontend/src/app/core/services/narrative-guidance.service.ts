import { Injectable, signal, computed } from '@angular/core';

export type UserRole = 'PADRE' | 'MADRE' | 'HIJO' | 'ADOLESCENTE' | 'FAMILIA';
export type EmotionalState = 'CALMA' | 'ESTRES' | 'AGOTAMIENTO' | 'CERCANIA';

export interface NarrativeContent {
  badge: string;      // Exactly 10 characters spark badge
  message: string;    // 60-140 characters colloquial, warm paragraph
}

@Injectable({
  providedIn: 'root'
})
export class NarrativeGuidanceService {
  // Reactive states initialized from localStorage (or defaults)
  private readonly activeRoleSignal = signal<UserRole>(this.loadRole());
  private readonly activeEmotionSignal = signal<EmotionalState>(this.loadEmotion());

  // Public readonly views for components
  public readonly activeRole = this.activeRoleSignal.asReadonly();
  public readonly activeEmotion = this.activeEmotionSignal.asReadonly();

  // Full detailed matrix mapping [Module] -> [Role] -> [Emotion] -> NarrativeContent
  private readonly matrix: Record<string, Record<UserRole, Record<EmotionalState, NarrativeContent>>> = {
    login: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Hoy el hogar respira calma. Buen momento para liderar con el ejemplo y paciencia.' },
        ESTRES: { badge: 'Respira ya', message: 'Los ánimos están intensos hoy. Respira hondo antes de entrar, vamos paso a paso.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Sabemos que vienes cansado. Hoy no tienes que resolver todo, solo estar presente.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Se siente una linda vibra en el aire. Entra y comparte un momento cálido con tus hijos.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Qué bueno verte. El hogar se siente sereno hoy. Tómate el ingreso a tu ritmo.' },
        ESTRES: { badge: 'Respira ya', message: 'Carga pesada hoy, ¿verdad? No tienes que sostener sola toda la energía de la casa.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Batería baja en casa. Hoy se vale dejar los pendientes para mañana y descansar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hay mucha luz en el hogar hoy. Qué lindo verte entrar para multiplicar esa calidez.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: '¡Hola! Todo está tranquilo hoy en casa. Entra a ver los retos divertidos que hay.' },
        ESTRES: { badge: 'Respira ya', message: 'Si sientes tensión en casa hoy, no pasa nada. Este es tu espacio seguro.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Ha sido un día largo de escuela. Entra a relajarte, hoy solo queremos que descanses.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Hola! Hay risas en el aire. Entra rápido a compartir tus aventuras y alegrías.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Qué onda. Cero dramas hoy en casa. Entra a tu bola y con tranquilidad.' },
        ESTRES: { badge: 'Respira ya', message: 'Si el día viene cruzado, no te enrolles. Este rincón es para desconectar de todo.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Batería social en cero. Entra tranquilo, hoy nadie te va a pedir explicaciones.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hay buena onda en el aire hoy. Aprovecha para conectar con los tuyos sin discusiones.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Hola 👋 Qué bueno vernos otra vez. Hoy no tienen que hacerlo perfecto, solo empezar.' },
        ESTRES: { badge: 'Respira ya', message: 'Parece que han sido días intensos en casa. Entren tranquilos, vamos despacio.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'El cansancio ronda hoy. No intenten solucionar todo hoy. Empiecen por algo posible.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Qué gran día! Hay momentos hermosos creciendo en casa. Entren a compartirlos.' }
      }
    },
    'portal-familiar': {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Hoy es un gran día para charlar con los chicos. ¡Usa la rueda de conexión y asómbralos hoy!' },
        ESTRES: { badge: 'Respira ya', message: 'El día estuvo pesado, pero conectar con tus hijos en la app es divertido. ¡Prueba una misión simple!' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Cero estrés hoy. Solo entra, ve tu destello de amor del día y deja que la app cuide del hogar.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Qué gran sintonía! Entra a proponer una micro-misión loca y haz reír a todos en casa hoy.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Tu luz guía el hogar. Entra al portal y mira los pequeños logros del equipo familiar hoy.' },
        ESTRES: { badge: 'Respira ya', message: 'Baja las revoluciones. Un juego rápido en la app o un abrazo cura cualquier fricción. ¡Tú puedes!' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy se vale delegar. Deja que los chicos cumplan las misiones en la app y regálate un respiro.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'La risa familiar es mágica. Entra y comparte un destello de gratitud que alegre el día de todos.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: '¡Tu casita mágica está abierta! Entra a marcar tus misiones del día y gana estrellas de felicidad.' },
        ESTRES: { badge: 'Respira ya', message: 'Si el día fue aburrido o difícil, ¡no te preocupes! Entra a jugar y a conectar con tus papás.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy los superhéroes descansan. Entra a ver los dibujos bonitos y relájate con tu familia.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Escribe tu aventura favorita de hoy! Guarda este momento feliz para siempre.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Tu espacio, tu ritmo. Entra a ver qué onda con las misiones rápidas y conecta sin dramas.' },
        ESTRES: { badge: 'Respira ya', message: 'Si hay ruido o tensión, desconecta un rato aquí. La app es tu espacio seguro para relajarte.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Batería social baja. No te exijas hoy; solo entra a dejar una reacción rápida y a tu bola.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Buena onda hoy! Entra a proponer un plan divertido o sorprender a todos con un destello.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Aquí comienzan los pequeños momentos que ayudan a transformar el ambiente del hogar.' },
        ESTRES: { badge: 'Respira ya', message: 'Hoy puede ser un buen día para volver a escucharse un poco más en casa de forma divertida.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Un hogar también se fortalece en los minutos simples del día. ¡Tómense un descanso y jueguen!' },
        CERCANIA: { badge: 'Juntos hoy', message: 'No necesitan hacerlo perfecto. Solo empezar a conectar otra vez en equipo. ¡A disfrutar!' }
      }
    },
    dashboard: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Buen día. Así se ve el hogar cuando la calma guía las decisiones de todos.' },
        ESTRES: { badge: 'Respira ya', message: 'Las métricas muestran tensión en casa. Hoy tu serenidad es el mejor escudo.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'No presiones por resultados hoy. El hogar necesita descanso más que planes duros.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Excelente clima familiar! Tus esfuerzos por conectar con el corazón están dando frutos.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Aquí puedes contemplar la armonía que con tanto amor sostienes día a día.' },
        ESTRES: { badge: 'Respira ya', message: 'Hoy el hogar parece necesitar más abrazos silenciosos que respuestas rápidas.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Mira los datos con calma. Delegar hoy no es fallar, es cuidar de ti también.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'La cercanía familiar brilla en el panel. Disfruta de esta hermosa conexión hoy.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: 'Mira cómo brilla el tablero de la casa. ¡Tu alegría ayuda a que todo esté bien!' },
        ESTRES: { badge: 'Respira ya', message: 'El ambiente está algo cargado. No te preocupes por los números, todo mejorará.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy las tareas pueden esperar. Tómate el día para recargar tu energía y sonreír.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Wow! Mira lo felices que están todos en casa hoy. ¡Sigue contagiando esa luz!' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'El panel está en verde y sin ruido. Un gran día para disfrutar de tu propio espacio.' },
        ESTRES: { badge: 'Respira ya', message: 'Si ves tensión en las gráficas, recuerda: no todo tiene que terminar en discusión.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'La energía general está baja. Ve a tu ritmo hoy, no te exijas de más.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hay buena sintonía en casa. Buen momento para proponer ese plan que tanto querías.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Aquí van a poder ver cómo se está sintiendo el hogar. Disfruten de este momento.' },
        ESTRES: { badge: 'Respira ya', message: 'Tensión detectada en el ambiente. Tómense un momento para respirar juntos antes de actuar.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy el cansancio colectivo es alto. Un día para bajar las revoluciones en casa.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'También hay momentos espectaculares creciendo en casa hoy. ¡A celebrarlos!' }
      }
    },
    families: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Aquí cada miembro aporta algo importante al equilibrio del hogar.' },
        ESTRES: { badge: 'Respira ya', message: 'Tus hijos necesitan hoy tu atención y contención, no una solución reactiva.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Vayan paso a paso hoy. El equipo familiar avanza más lento pero seguro.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Una familia fuerte no piensa igual siempre. Aprende a avanzar unida.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Este núcleo familiar evoluciona paso a paso con decisiones conscientes.' },
        ESTRES: { badge: 'Respira ya', message: 'La convivencia mejora cuando todos participan en construirla.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'No sostengas la energía de todos sola hoy. Apóyate en tu equipo familiar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'La complicidad familiar es hermosa. Disfruta de la magia de estar unidos hoy.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: 'Todos en casa son un gran equipo. ¡Mira lo divertido que es estar juntos!' },
        ESTRES: { badge: 'Respira ya', message: 'A veces los papás también se estresan. Tenles paciencia hoy, te quieren mucho.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy es día de abrazos suaves. Demuéstrale a los tuyos cuánto te importan.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Eres una pieza súper importante de esta familia! Gracias por tu alegría de hoy.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Cada uno tiene su rollo en casa, pero hoy se respira respeto y libertad.' },
        ESTRES: { badge: 'Respira ya', message: 'No todo tiene que terminar en pelea. Escuchar un ratito hoy puede cambiar el día.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Si el grupo familiar está cansado, un poco de silencio compartido ayuda mucho.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué buena sintonía hay hoy en el grupo. Es genial sentirse parte del equipo.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Cada familia tiene su propia historia. Aquí construyen la suya juntos.' },
        ESTRES: { badge: 'Respira ya', message: 'La empatía familiar cura cualquier roce. Pónganse en los zapatos del otro hoy.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Bájenle al ritmo familiar hoy. Un descanso en equipo es el mejor plan.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Las conversaciones pequeñas cambian hogares. Sigan tejiendo lazos fuertes.' }
      }
    },
    evaluations: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Responde con sinceridad y sin juzgar. Este test no califica, solo nos orienta.' },
        ESTRES: { badge: 'Respira ya', message: 'Deja de lado la tensión del trabajo. Responde pensando en el bienestar de tus hijos.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Si estás muy cansado, responde las preguntas simples primero. Sin presiones.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué buen momento para evaluar cómo crecemos. Tus respuestas nacen del corazón.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Responde tranquila. Aquí no hay respuestas perfectas, solo amor por tu hogar.' },
        ESTRES: { badge: 'Respira ya', message: 'No te culpes por los resultados. El diagnóstico es el mapa para sanar, no para juzgar.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Si te agobia pensar las respuestas hoy, respira y ve al ritmo que puedas.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hoy las respuestas fluirán hermosas. Es fácil ver el progreso cuando hay cercanía.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: 'No pienses mucho las respuestas. Lo importante es lo que sientes de verdad.' },
        ESTRES: { badge: 'Respira ya', message: 'Aquí no hay exámenes ni notas. Solo queremos saber cómo te sientes en casa.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Contesta a tu ritmo, despacito. Tu opinión vale oro para toda la familia.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Diviértete respondiendo! Cuéntanos las cosas más lindas que vives en casa hoy.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Responde con la verdad. Nadie te va a juzgar, tu perspectiva es vital aquí.' },
        ESTRES: { badge: 'Respira ya', message: 'Sé honesto con lo que te molesta o estresa. El primer paso para cambiar es decirlo.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Son pocas preguntas. Tómalo con calma y responde con lo primero que sientas.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Buen momento para plasmar lo bien que te sientes hoy en tu núcleo familiar.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'No piensen mucho las respuestas. Lo importante es que sean sinceras y tranquilas.' },
        ESTRES: { badge: 'Respira ya', message: 'Si el ambiente está tenso, respondan sin culparse. El diagnóstico es el inicio.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Vayan despacio con la encuesta. Un par de preguntas hoy y completamos mañana.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué lindo evaluar juntos nuestro camino. Aquí celebramos la verdad en equipo.' }
      }
    },
    plans: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Perfecto. Vamos con acciones pequeñas que sí puedan cumplir en casa sin presiones.' },
        ESTRES: { badge: 'Respira ya', message: 'No intentes cambiar todo hoy. Empiecen por una sola misión que traiga calma.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy la mejor misión del plan es descansar juntos. Tómense una pausa.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Qué motivador! Con esta sintonía, cumplir las misiones familiares será un juego.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Diseñemos metas realistas. La paz de la casa vale más que cualquier plan perfecto.' },
        ESTRES: { badge: 'Respira ya', message: 'Prioriza la calma emocional del hogar hoy antes que las tareas y horarios.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'No asumas todas las misiones. Asigna tareas sencillas a los demás y aligera la carga.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Es hermoso ver cómo el plan de acción se llena de misiones divertidas en equipo.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: '¡Tienes misiones divertidas hoy! Vamos a lograrlas con una gran sonrisa.' },
        ESTRES: { badge: 'Respira ya', message: 'Si una misión te resulta difícil hoy, pide ayuda a tus papás. Todo es un juego.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy puedes hacer solo una cosita pequeña. Los superhéroes también descansan.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Vamos a cumplir misiones en equipo hoy! Juntos es mucho más divertido.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Metas sencillas para hoy. A tu propio ritmo y sin que nadie te esté encima.' },
        ESTRES: { badge: 'Respira ya', message: 'Si el plan te abruma, elije la misión más simple. Una pequeña victoria hoy sirve.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy tómalo con calma. No pasa nada si postergas una misión para estar tranquilo.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hay buena sintonía hoy para colaborar en casa. ¡Sorpréndelos con un detalle!' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Perfecto. Ya entendimos algunas cosas. Ahora vamos con acciones pequeñas.' },
        ESTRES: { badge: 'Respira ya', message: 'No intenten cambiar todo hoy. Empiecen por algo posible y de mutuo acuerdo.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy el plan familiar es mimarse y descansar. Mañana seguiremos adelante.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Con esta alegría familiar, no hay misión que se nos resista. ¡A divertirse juntos!' }
      }
    },
    logbook: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Escribe tranquilo. Escribir ayuda a consolidar las alegrías que a veces olvidamos celebrar.' },
        ESTRES: { badge: 'Respira ya', message: 'A veces escribir ayuda a soltar el enojo y entender cosas que hablando no salen tan fácil.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy solo deja una frase corta de lo que sentiste en el día. Alivia la mente.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué hermoso día para dejar registrado. Tu gratitud hoy inspirará a toda tu familia.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Este cuaderno guarda la historia de tu hogar. Deja un rayito de luz escrito hoy.' },
        ESTRES: { badge: 'Respira ya', message: 'Desahoga tu corazón aquí. Escribir drena el estrés y trae claridad a tus pensamientos.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Si estás muy cansada, solo escribe una palabra que resuma tu sentir hoy. Libérate.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Plasma la calidez de hoy en la bitácora. Será hermoso volver a leerlo después.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: 'Cuéntale a tu diario lo feliz o tranquilo que te sentiste hoy en casa.' },
        ESTRES: { badge: 'Respira ya', message: 'Si algo te molestó hoy, escríbelo o dibújalo aquí. Te sentirás mucho más ligero.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Dibuja una carita de cómo te sientes hoy. Tu diario familiar te acompaña siempre.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Escribe tu aventura favorita de hoy! Guarda este momento feliz para siempre.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Escribe lo que pienses sin filtros. Este es tu rincón personal de la bitácora.' },
        ESTRES: { badge: 'Respira ya', message: 'Escribir ordena el caos mental. Suelta la rabia o el estrés sobre el papel hoy.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'No te enrolles escribiendo testamentos. Pon una sola frase de cómo fue el día.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Un gran día para dejar constancia de que hoy todo fluyó en perfecta calma.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'A veces escribir ayuda a entender cosas que hablando no salen tan fácil.' },
        ESTRES: { badge: 'Respira ya', message: 'Escriban lo que sintieron sin culpar al otro. El papel ayuda a sanar los roces.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Ahora guarda lo que sentiste hoy de forma simple. Después será valioso releerlo.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Registremos la alegría de hoy en la memoria del hogar. ¡Qué linda familia somos!' }
      }
    },
    'my-space': {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Disfruta de este rincón de desconexión. Te lo has ganado por cuidar tanto a los tuyos.' },
        ESTRES: { badge: 'Respira ya', message: 'Este espacio es solo tuyo. Aquí puedes pensar tranquilo antes de reaccionar en casa.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Cierra los ojos un momento. Aquí no hay demandas ni tareas, solo descanso puro.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué buena vibra llevas en el corazón hoy. Disfruta tu paz para seguir guiando.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Un momento a solas contigo misma. Abraza este silencio y recarga tu hermosa luz.' },
        ESTRES: { badge: 'Respira ya', message: 'Suelta todas las cargas aquí. Respira profundo y regálate calma antes de continuar.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Recuéstate un ratito en tu espacio. Hoy el mundo puede esperar, tú eres lo primero.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué lindo es disfrutar de ti misma cuando hay paz en casa. Sonríe y respira.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: '¡Bienvenido a tu casita secreta! Aquí puedes relajarte y soñar despierto.' },
        ESTRES: { badge: 'Respira ya', message: 'Si estás enojado o triste, quédate un ratito aquí respirando suavemente. Todo pasa.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Túmbate a descansar aquí. Tu rincón seguro te abraza y te cuida hoy.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Qué divertido es tener un espacio propio! Disfruta de tu alegría aquí hoy.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Tu zona, tus reglas. Relájate, escucha música mental y disfruta de no hacer nada.' },
        ESTRES: { badge: 'Respira ya', message: 'Pausa el mundo exterior. Respira hondo, limpia tu mente y vuelve a tu centro aquí.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Batería en ahorro de energía. Cierra los ojos y aíslate un rato para recargar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué bien se siente estar en paz con uno mismo y con los demás. Disfruta el momento.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Este espacio es solo tuyo. Aquí puedes pensar tranquilo antes de reaccionar.' },
        ESTRES: { badge: 'Respira ya', message: 'Respiren primero a solas. La mejor solución nace de una mente en calma.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Un rincón para que cada uno respire y descanse hoy. La paz empieza en ti.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué hermoso es recargar el alma en el espacio personal para dar lo mejor al hogar.' }
      }
    },
    cognitive: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Te ayudaré a detectar patrones de bienestar en casa que a veces pasan desapercibidos.' },
        ESTRES: { badge: 'Respira ya', message: 'Tranquilo. Analicemos juntos esa tensión para encontrar una ruta pacífica.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Deja que yo procese las métricas complejas hoy. Tú solo enfócate en descansar.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Increíble conexión familiar! Te muestro los pilares de amor que están creciendo hoy.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Veo patrones hermosos de armonía hoy. Tu paciencia está guiando al hogar.' },
        ESTRES: { badge: 'Respira ya', message: 'No te angusties por los roces cotidianos. Te daré ideas prácticas para aliviar la carga.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy no pienses en problemas difíciles. Te sugiero descansar y delegar responsabilidades.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'La empatía está en su punto más alto. Te muestro las dinámicas hermosas del hogar.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: '¡Hola! Te ayudaré a descubrir cosas súper bonitas sobre ti y tu familia hoy.' },
        ESTRES: { badge: 'Respira ya', message: 'Si sientes que las cosas están difíciles, aquí estoy para ayudarte a entender tus emociones.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy vamos a hablar cortito y a jugar a relajarnos. ¡Te lo mereces!' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Wow! Hoy todos se entienden de maravilla. Vamos a ver por qué se quieren tanto hoy.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Analicemos lo que funciona bien hoy. Tus ideas y tu paz marcan la pauta.' },
        ESTRES: { badge: 'Respira ya', message: 'Si sientes que no te entienden, busquemos juntos formas de expresarlo sin discutir.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy no hay análisis densos. Charlemos de algo ligero que te relaje.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Qué gran sintonía familiar hoy. Te muestro cómo influye tu buena onda en casa.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Voy a ayudarte a detectar patrones que a veces pasan desapercibidos en casa.' },
        ESTRES: { badge: 'Respira ya', message: 'Analicemos las causas de la tensión sin juzgarnos. La IA nos ayuda a comprender.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Hoy la mejor recomendación cognitiva es el reposo. Bájenle al ritmo familiar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Los datos confirman que están más unidos. Sigan cultivando estas hermosas rutinas.' }
      }
    },
    crisis: {
      PADRE: {
        CALMA: { badge: 'Calma aquí', message: 'Aunque haya un problema, tu centro es la brújula de casa. Mantén la serenidad.' },
        ESTRES: { badge: 'Respira ya', message: 'Respira primero. No intentes resolver toda la situación de una vez. Calma.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'El cansancio nubla la mente. Deja la discusión difícil para cuando hayan dormido.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Apóyate en el amor de los tuyos para superar este bache. Todo saldrá bien.' }
      },
      MADRE: {
        CALMA: { badge: 'Calma aquí', message: 'La paz es tu superpoder. Irradia serenidad en este momento difícil.' },
        ESTRES: { badge: 'Respira ya', message: 'Suelta la urgencia. No tienes que arreglar la crisis sola hoy. Respira profundo.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Estás al límite. Pide un tiempo fuera para respirar a solas antes de hablar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'El cariño de tu familia es fuerte. Abrazarse hoy disuelve cualquier tormenta.' }
      },
      HIJO: {
        CALMA: { badge: 'Calma aquí', message: 'Todo va a estar bien. Quédate cerquita de tus papás y respira tranquilo.' },
        ESTRES: { badge: 'Respira ya', message: 'Respira suavecito. No te asustes si hay discusiones. Todo se va a solucionar.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Cierra los ojitos y descansa. La tormenta pasará pronto, estás seguro.' },
        CERCANIA: { badge: 'Juntos hoy', message: '¡Abrázalos fuerte! Tu cariño es la mejor medicina cuando hay un problema.' }
      },
      ADOLESCENTE: {
        CALMA: { badge: 'Calma aquí', message: 'Mantén la cabeza fría. La calma es la mejor forma de no agrandar los problemas.' },
        ESTRES: { badge: 'Respira ya', message: 'Aléjate del conflicto un momento. Respira hondo y habla solo cuando estés frío.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Si estás sobrepasado, pide espacio para calmarte solo. Es totalmente válido.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'Hoy el cariño familiar es más fuerte que cualquier malentendido. Vamos a hablarlo.' }
      },
      FAMILIA: {
        CALMA: { badge: 'Calma aquí', message: 'Respiren primero. Mantener la paz en equipo es el mejor escudo familiar.' },
        ESTRES: { badge: 'Respira ya', message: 'Respiren primero. No intenten resolver todo de una vez. Vamos con calma.' },
        AGOTAMIENTO: { badge: 'Da un paso', message: 'Pausa colectiva obligatoria. Una mente cansada no resuelve crisis. A descansar.' },
        CERCANIA: { badge: 'Juntos hoy', message: 'El amor familiar es más fuerte que este problema. Juntos saldremos adelante.' }
      }
    }
  };

  constructor() {}

  // Active NarrativeContent computed reactive signal
  public readonly currentContent = computed<NarrativeContent>(() => {
    const role = this.activeRoleSignal();
    const emotion = this.activeEmotionSignal();
    // Default module is dashboard if not matched, but narrative component passes it
    return {
      badge: 'Calma aquí',
      message: 'Estamos aquí para acompañarte paso a paso en tu bienestar.'
    };
  });

  /**
   * Main query to retrieve the dynamic narrative based on:
   * @param module Active Integrity Family module name
   */
  public getContent(module: string): NarrativeContent {
    const role = this.activeRoleSignal();
    const emotion = this.activeEmotionSignal();
    
    // Normalize module key
    let modKey = module.toLowerCase();
    if (modKey.includes('login')) modKey = 'login';
    else if (modKey.includes('dash')) modKey = 'dashboard';
    else if (modKey.includes('portal')) modKey = 'portal-familiar';
    else if (modKey.includes('famil')) modKey = 'families';
    else if (modKey.includes('eval') || modKey.includes('start') || modKey.includes('form') || modKey.includes('result')) modKey = 'evaluations';
    else if (modKey.includes('plan')) modKey = 'plans';
    else if (modKey.includes('log') || modKey.includes('bitacora')) modKey = 'logbook';
    else if (modKey.includes('my-space') || modKey.includes('espacio')) modKey = 'my-space';
    else if (modKey.includes('cogni') || modKey.includes('chat') || modKey.includes('consultor')) modKey = 'cognitive';
    else if (modKey.includes('cris')) modKey = 'crisis';

    const moduleData = this.matrix[modKey] || this.matrix['dashboard'];
    const roleData = moduleData[role] || moduleData['FAMILIA'];
    const emotionData = roleData[emotion] || roleData['CALMA'];

    return emotionData;
  }

  /**
   * Set user role globally
   */
  public setRole(role: UserRole): void {
    this.activeRoleSignal.set(role);
    localStorage.setItem('nge_active_role', role);
  }

  /**
   * Set emotional state globally
   */
  public setEmotion(emotion: EmotionalState): void {
    this.activeEmotionSignal.set(emotion);
    localStorage.setItem('nge_active_emotion', emotion);
  }

  // Load active role from localStorage
  private loadRole(): UserRole {
    const saved = localStorage.getItem('nge_active_role') as UserRole;
    return saved && ['PADRE', 'MADRE', 'HIJO', 'ADOLESCENTE', 'FAMILIA'].includes(saved) 
      ? saved 
      : 'FAMILIA';
  }

  // Load active emotion from localStorage
  private loadEmotion(): EmotionalState {
    const saved = localStorage.getItem('nge_active_emotion') as EmotionalState;
    return saved && ['CALMA', 'ESTRES', 'AGOTAMIENTO', 'CERCANIA'].includes(saved) 
      ? saved 
      : 'CALMA';
  }
}
