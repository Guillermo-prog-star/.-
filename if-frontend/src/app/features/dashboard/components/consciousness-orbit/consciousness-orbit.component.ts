import { Component, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DimensionScore {
  emociones: number;
  comunicacion: number;
  habitos: number;
  tiempos: number;
}

@Component({
  selector: 'app-consciousness-orbit',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="orbit-card glass-premium p-6 rounded-[2.5rem] border border-white/10 flex flex-col h-full overflow-hidden">
      <!-- Encabezado de la Órbita -->
      <div class="flex justify-between items-start mb-4">
        <div>
          <h3 class="text-xl font-extrabold text-white tracking-tight">Órbita Familiar</h3>
          <p class="text-white/40 text-[10px] font-black uppercase tracking-widest mt-1">Sintonía Emocional en Tiempo Real</p>
        </div>
        <div class="px-3 py-1 bg-white/[0.04] border border-white/6 rounded-full text-[10px] font-extrabold text-indigo-400 uppercase tracking-wider">
          {{ status || 'Consciente' }}
        </div>
      </div>

      <!-- Lienzo SVG del Sistema Concéntrico -->
      <div class="flex justify-center items-center py-4 relative">
        <svg width="350" height="350" viewBox="0 0 350 350" class="orbit-canvas select-none">
          <defs>
            <!-- Filtro de Resplandor Premium -->
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="6" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
            <!-- Degradado Lineal del Polígono de Aura -->
            <linearGradient id="aura-grad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="hsla(350, 89%, 60%, 0.25)" />
              <stop offset="50%" stop-color="hsla(217, 91%, 60%, 0.25)" />
              <stop offset="100%" stop-color="hsla(142, 70%, 45%, 0.25)" />
            </linearGradient>
          </defs>

          <!-- 5 Órbitas Concéntricas de Conciencia -->
          <circle cx="175" cy="175" r="30" class="orbit-ring line-inconsciente" />
          <circle cx="175" cy="175" r="60" class="orbit-ring line-reactivo" />
          <circle cx="175" cy="175" r="90" class="orbit-ring line-consciente" />
          <circle cx="175" cy="175" r="120" class="orbit-ring line-intencional" />
          <circle cx="175" cy="175" r="150" class="orbit-ring line-pleno" />

          <!-- Ejes Cardinales de la Estructura Familiar -->
          <line x1="175" y1="25" x2="175" y2="325" class="cardinal-axis" />
          <line x1="25" y1="175" x2="325" y2="175" class="cardinal-axis" />

          <!-- Etiquetas del Sistema Concéntrico -->
          <text x="175" y="171" class="orbit-label">INCONSCIENTE</text>
          <text x="175" y="141" class="orbit-label">REACTIVO</text>
          <text x="175" y="111" class="orbit-label">CONSCIENTE</text>
          <text x="175" y="81" class="orbit-label">INTENCIONAL</text>
          <text x="175" y="51" class="orbit-label">PLENO</text>

          <!-- Red Poligonal (Aura de Sintonía) -->
          <polygon [attr.points]="getPolygonPoints()" class="aura-mesh animate-pulse-slow" fill="url(#aura-grad)" />

          <!-- Trazados y Nodos de las 4 Dimensiones -->
          <!-- 1. EMOCIONES (Arriba) -->
          <g class="node-group" (click)="selectDimension('emociones')" [class.active]="activeDimension === 'emociones'">
            <line x1="175" y1="175" [attr.x2]="nodes.emociones.x" [attr.y2]="nodes.emociones.y" class="dimension-line line-emo" />
            <circle [attr.cx]="nodes.emociones.x" [attr.cy]="nodes.emociones.y" r="9" class="dimension-node fill-emo" filter="url(#glow)" />
            <circle [attr.cx]="nodes.emociones.x" [attr.cy]="nodes.emociones.y" r="15" class="dimension-ring ring-emo" />
          </g>

          <!-- 2. COMUNICACIÓN (Derecha) -->
          <g class="node-group" (click)="selectDimension('comunicacion')" [class.active]="activeDimension === 'comunicacion'">
            <line x1="175" y1="175" [attr.x2]="nodes.comunicacion.x" [attr.y2]="nodes.comunicacion.y" class="dimension-line line-com" />
            <circle [attr.cx]="nodes.comunicacion.x" [attr.cy]="nodes.comunicacion.y" r="9" class="dimension-node fill-com" filter="url(#glow)" />
            <circle [attr.cx]="nodes.comunicacion.x" [attr.cy]="nodes.comunicacion.y" r="15" class="dimension-ring ring-com" />
          </g>

          <!-- 3. HÁBITOS (Abajo) -->
          <g class="node-group" (click)="selectDimension('habitos')" [class.active]="activeDimension === 'habitos'">
            <line x1="175" y1="175" [attr.x2]="nodes.habitos.x" [attr.y2]="nodes.habitos.y" class="dimension-line line-hab" />
            <circle [attr.cx]="nodes.habitos.x" [attr.cy]="nodes.habitos.y" r="9" class="dimension-node fill-hab" filter="url(#glow)" />
            <circle [attr.cx]="nodes.habitos.x" [attr.cy]="nodes.habitos.y" r="15" class="dimension-ring ring-hab" />
          </g>

          <!-- 4. TIEMPOS (Izquierda) -->
          <g class="node-group" (click)="selectDimension('tiempos')" [class.active]="activeDimension === 'tiempos'">
            <line x1="175" y1="175" [attr.x2]="nodes.tiempos.x" [attr.y2]="nodes.tiempos.y" class="dimension-line line-tie" />
            <circle [attr.cx]="nodes.tiempos.x" [attr.cy]="nodes.tiempos.y" r="9" class="dimension-node fill-tie" filter="url(#glow)" />
            <circle [attr.cx]="nodes.tiempos.x" [attr.cy]="nodes.tiempos.y" r="15" class="dimension-ring ring-tie" />
          </g>
        </svg>

        <!-- Indicador Rápido del Eje Activo -->
        <span class="absolute top-2 left-2 text-[10px] font-black text-white/20 select-none">PULSA UN ORBITAL PARA DETALLES</span>
      </div>

      <!-- Tarjeta Interactiva de Detalle - Sabiduría de Convi -->
      <div class="interactive-detail-box bg-white/[0.02] border border-white/5 p-4 rounded-3xl mt-auto space-y-3 relative overflow-hidden transition-all duration-300">
        <!-- Fondo de color dinámico muy difuminado -->
        <div class="absolute inset-0 opacity-[0.03] pointer-events-none transition-all duration-500"
             [style.background-color]="activeDetails.color">
        </div>

        <div class="flex justify-between items-center relative z-2">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full shadow-lg shadow-white/20 animate-pulse" [style.background-color]="activeDetails.color"></span>
            <h4 class="text-sm font-extrabold text-white capitalize tracking-wide">{{ activeDetails.name }}</h4>
          </div>
          <div class="text-xs font-black" [style.color]="activeDetails.color">
            {{ activeDetails.score }} / 5.0
          </div>
        </div>

        <div class="flex justify-between items-center bg-white/[0.03] px-3 py-1.5 rounded-xl border border-white/6 relative z-2">
          <span class="text-[9px] font-black text-white/30 tracking-wider uppercase">Fase de Conciencia</span>
          <span class="text-[10px] font-extrabold uppercase tracking-wide" [style.color]="activeDetails.color">
            {{ activeDetails.level }}
          </span>
        </div>

        <blockquote class="text-white/80 text-xs italic font-medium leading-relaxed bg-white/[0.01] p-3 rounded-2xl border border-white/4 relative z-2">
          "{{ activeDetails.statement }}"
        </blockquote>

        <div class="pt-2 border-t border-white/5 relative z-2">
          <div class="flex items-start gap-3">
            <span class="text-base select-none leading-none mt-0.5">💬</span>
            <div>
              <strong class="text-[9px] uppercase tracking-widest text-indigo-400 font-black block mb-0.5">Consejo de Convi</strong>
              <p class="text-white/60 text-[11px] leading-relaxed font-semibold">
                {{ activeDetails.tip }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .glass-premium {
      background: rgba(10, 15, 30, 0.55);
      backdrop-filter: blur(25px);
      -webkit-backdrop-filter: blur(25px);
      box-shadow: 
        0 20px 50px rgba(0, 0, 0, 0.4),
        inset 0 1px 0 rgba(255, 255, 255, 0.05);
    }

    /* Estilos del Lienzo SVG */
    .orbit-canvas {
      filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.3));
    }

    .orbit-ring {
      fill: none;
      stroke-width: 1;
      stroke-dasharray: 4 6;
      transition: stroke 0.3s ease, opacity 0.3s ease;
    }

    .line-inconsciente { stroke: rgba(100, 116, 139, 0.15); }
    .line-reactivo { stroke: rgba(248, 113, 113, 0.15); }
    .line-consciente { stroke: rgba(245, 158, 11, 0.15); }
    .line-intencional { stroke: rgba(59, 130, 246, 0.15); }
    .line-pleno { stroke: rgba(16, 185, 129, 0.15); }

    .cardinal-axis {
      stroke: rgba(255, 255, 255, 0.03);
      stroke-width: 1;
    }

    .orbit-label {
      font-size: 7px;
      font-weight: 900;
      letter-spacing: 0.1em;
      fill: rgba(255, 255, 255, 0.12);
      text-anchor: middle;
      font-family: 'Outfit', sans-serif;
    }

    /* Red de Sintonía (Aura) */
    .aura-mesh {
      stroke-width: 2.5;
      stroke: rgba(99, 102, 241, 0.45);
      stroke-linejoin: round;
      filter: drop-shadow(0 0 10px rgba(99, 102, 241, 0.2));
      transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .animate-pulse-slow {
      animation: pulseAura 4s infinite alternate ease-in-out;
    }

    @keyframes pulseAura {
      from { opacity: 0.85; filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.15)); }
      to { opacity: 1; filter: drop-shadow(0 0 14px rgba(99, 102, 241, 0.35)); }
    }

    /* Estilos del Nodo */
    .node-group {
      cursor: pointer;
    }

    .dimension-line {
      stroke-width: 1.5;
      stroke-dasharray: 2 4;
      opacity: 0.25;
      transition: opacity 0.3s ease, stroke-width 0.3s ease;
    }

    .dimension-node {
      transition: r 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), filter 0.3s ease;
    }

    .dimension-ring {
      fill: none;
      stroke-width: 1.5;
      opacity: 0;
      transform-origin: center;
      transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    }

    /* Colores por Dimensión */
    /* Emociones (Rosa/Rojo HSL 350, 89%, 60%) */
    .line-emo { stroke: rgba(244, 63, 94, 0.4); }
    .fill-emo { fill: #f43f5e; color: #f43f5e; }
    .ring-emo { stroke: rgba(244, 63, 94, 0.5); }

    /* Comunicación (Azul HSL 217, 91%, 60%) */
    .line-com { stroke: rgba(59, 130, 246, 0.4); }
    .fill-com { fill: #3b82f6; color: #3b82f6; }
    .ring-com { stroke: rgba(59, 130, 246, 0.5); }

    /* Hábitos (Verde HSL 142, 70%, 45%) */
    .line-hab { stroke: rgba(16, 185, 129, 0.4); }
    .fill-hab { fill: #10b981; color: #10b981; }
    .ring-hab { stroke: rgba(16, 185, 129, 0.5); }

    /* Tiempos (Naranja HSL 38, 92%, 50%) */
    .line-tie { stroke: rgba(245, 158, 11, 0.4); }
    .fill-tie { fill: #f59e0b; color: #f59e0b; }
    .ring-tie { stroke: rgba(245, 158, 11, 0.5); }

    /* Estado Activo del Nodo */
    .node-group:hover .dimension-node,
    .node-group.active .dimension-node {
      r: 12;
      filter: drop-shadow(0 0 15px currentColor);
    }

    .node-group:hover .dimension-ring,
    .node-group.active .dimension-ring {
      opacity: 1;
      transform: scale(1.2);
    }

    .node-group:hover .dimension-line,
    .node-group.active .dimension-line {
      opacity: 0.7;
      stroke-width: 2;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConsciousnessOrbitComponent implements OnInit {
  @Input() scores!: DimensionScore | null;
  @Input() status!: string;

  activeDimension: string = 'comunicacion';

  // Coordenadas calculadas en tiempo real para el SVG
  nodes: {
    emociones: { x: number; y: number };
    comunicacion: { x: number; y: number };
    habitos: { x: number; y: number };
    tiempos: { x: number; y: number };
  } = {
    emociones: { x: 175, y: 85 },
    comunicacion: { x: 265, y: 175 },
    habitos: { x: 175, y: 265 },
    tiempos: { x: 85, y: 175 }
  };

  ngOnInit() {
    this.calculateCoordinates();
  }

  ngOnChanges() {
    this.calculateCoordinates();
  }

  private calculateCoordinates() {
    const defaultScore = 3.0;
    
    // Obtener los puntajes reales (escalados de 1.0 a 5.0).
    // Si viene en escala 1-100 (legado), lo dividimos por 20.
    const getEffectiveScore = (val: any): number => {
      if (val === undefined || val === null) return defaultScore;
      const num = Number(val);
      if (num > 5.0) return num / 20.0;
      return num < 1.0 ? 1.0 : num;
    };

    const s = {
      emociones: getEffectiveScore(this.scores?.emociones),
      comunicacion: getEffectiveScore(this.scores?.comunicacion),
      habitos: getEffectiveScore(this.scores?.habitos),
      tiempos: getEffectiveScore(this.scores?.tiempos)
    };

    // Centro del SVG = 175, 175.
    // Cada nivel de conciencia avanza exactamente 30px (Radio total = 150px para score=5).
    const radiusMultiplier = 30;

    // Emociones: Dirección Arriba (Ángulo -Math.PI / 2)
    const distEmo = s.emociones * radiusMultiplier;
    this.nodes.emociones = {
      x: 175,
      y: 175 - distEmo
    };

    // Comunicación: Dirección Derecha (Ángulo 0)
    const distCom = s.comunicacion * radiusMultiplier;
    this.nodes.comunicacion = {
      x: 175 + distCom,
      y: 175
    };

    // Hábitos: Dirección Abajo (Ángulo Math.PI / 2)
    const distHab = s.habitos * radiusMultiplier;
    this.nodes.habitos = {
      x: 175,
      y: 175 + distHab
    };

    // Tiempos: Dirección Izquierda (Ángulo Math.PI)
    const distTie = s.tiempos * radiusMultiplier;
    this.nodes.tiempos = {
      x: 175 - distTie,
      y: 175
    };
  }

  getPolygonPoints(): string {
    return `${this.nodes.emociones.x},${this.nodes.emociones.y} ` +
           `${this.nodes.comunicacion.x},${this.nodes.comunicacion.y} ` +
           `${this.nodes.habitos.x},${this.nodes.habitos.y} ` +
           `${this.nodes.tiempos.x},${this.nodes.tiempos.y}`;
  }

  selectDimension(dim: string) {
    this.activeDimension = dim;
  }

  get activeDetails() {
    const rawScore = this.scores ? (this.scores as any)[this.activeDimension] : null;
    
    const getEffectiveScore = (val: any): number => {
      if (val === undefined || val === null) return 3.0;
      const num = Number(val);
      if (num > 5.0) return num / 20.0;
      return num < 1.0 ? 1.0 : num;
    };

    const score = getEffectiveScore(rawScore);

    // Mapeo del nivel de conciencia y HSL
    let level = 'Consciente';
    let color = '#f59e0b';
    let statement = 'A veces logramos organizarnos, pero aún nos dejamos llevar por impulsos.';
    let tip = 'Establece un pequeño acuerdo visible y celébralo en el día.';

    if (score < 1.8) {
      level = 'Inconsciente';
      color = '#64748b';
    } else if (score < 2.8) {
      level = 'Reactivo';
      color = '#f87171';
    } else if (score < 3.8) {
      level = 'Consciente';
      color = '#f59e0b';
    } else if (score < 4.8) {
      level = 'Intencional';
      color = '#3b82f6';
    } else {
      level = 'Plena';
      color = '#10b981';
    }

    const detailsMap: { [key: string]: { name: string; statement: { [key: string]: string }; tip: { [key: string]: string } } } = {
      emociones: {
        name: 'Gestión Emocional',
        statement: {
          'Inconsciente': 'No nos percatamos del impacto de nuestro estrés individual en el hogar.',
          'Reactivo': 'Cualquier roce detona discusiones y descargamos el cansancio en el otro.',
          'Consciente': 'Nos damos cuenta de los momentos de tensión familiar, pero nos cuesta calmarnos.',
          'Intencional': 'Hacemos un esfuerzo voluntario de detenernos y respirar antes de reaccionar.',
          'Plena': 'Manejamos los momentos difíciles en el hogar con paciencia, empatía y calma natural.'
        },
        tip: {
          'Inconsciente': 'Identifica una alarma silenciosa en tu cuerpo cuando te estresas y compártela.',
          'Reactivo': 'Cuando sientas tensión, haz una pausa de 3 minutos antes de responder.',
          'Consciente': 'Crea un semáforo emocional en el refrigerador para avisar cómo te sientes.',
          'Intencional': 'Comparte un elogio sincero con otro miembro de la familia hoy en la tarde.',
          'Plena': 'Actúa como un ancla de serenidad en el hogar y apoya con un abrazo cuando haya tensión.'
        }
      },
      comunicacion: {
        name: 'Comunicación Asertiva',
        statement: {
          'Inconsciente': 'Hablamos por inercia y rara vez nos escuchamos con verdadera atención.',
          'Reactivo': 'Interrumpimos para defendernos de inmediato, elevando la voz al conversar.',
          'Consciente': 'Intentamos dialogar, pero caemos rápidamente en debates de quién tiene la razón.',
          'Intencional': 'Estamos aprendiendo a validar las emociones y a escuchar con empatía.',
          'Plena': 'Dialogamos con absoluta apertura, respeto mutuo y validación constante del otro.'
        },
        tip: {
          'Inconsciente': 'Mira a los ojos a tu familia al saludarlos hoy por lo menos durante 3 segundos.',
          'Reactivo': 'Escucha sin interrumpir a los demás. Tu meta hoy es validar antes de dar tu opinión.',
          'Consciente': 'Pregunta hoy en la cena: "¿Qué fue lo más bonito que te pasó hoy?" y escucha con sintonía.',
          'Intencional': 'Evita el uso de celulares al entablar una conversación importante en la sala.',
          'Plena': 'Fomenta un espacio de palabra libre en el hogar donde todos expresen sus ideas sin temor.'
        }
      },
      habitos: {
        name: 'Rutinas e Integridad',
        statement: {
          'Inconsciente': 'Vivimos al día y las responsabilidades del hogar se asumen de forma caótica.',
          'Reactivo': 'Solo cumplimos con los acuerdos familiares cuando hay reclamos o presiones.',
          'Consciente': 'Conocemos los acuerdos de convivencia, pero nos cuesta mantener la disciplina.',
          'Intencional': 'Trabajamos deliberadamente en crear hábitos colaborativos y asumir roles.',
          'Plena': 'La corresponsabilidad y el orden colaborativo fluyen de manera integrada y natural.'
        },
        tip: {
          'Inconsciente': 'Define una sola tarea sencilla del hogar en la que te comprometas a colaborar hoy.',
          'Reactivo': 'Cumple con tu responsabilidad del día antes de que te lo recuerden o reclamen.',
          'Consciente': 'Diseñen juntos una pequeña lista visual de hábitos de la mañana y déjenla en la sala.',
          'Intencional': 'Asume una tarea colaborativa invisible sin esperar reconocimiento alguno.',
          'Plena': 'Comparte con otros tu método para mantener tus hábitos y apóyalos a consolidar los suyos.'
        }
      },
      tiempos: {
        name: 'Tiempo en Presencia',
        statement: {
          'Inconsciente': 'Compartimos el mismo techo pero cada uno está absorto en su pantalla.',
          'Reactivo': 'Las distracciones digitales interrumpen constantemente nuestros momentos de sintonía.',
          'Consciente': 'A veces programamos momentos juntos, pero nos cuesta desconectarnos del celular.',
          'Intencional': 'Estamos priorizando e instalando rituales familiares libres de tecnologías.',
          'Plena': 'Disfrutamos de verdaderos momentos de sintonía familiar con absoluta presencia física.'
        },
        tip: {
          'Inconsciente': 'Cena hoy sin ninguna pantalla encendida. Lo importante es compartir el momento.',
          'Reactivo': 'Define una "zona libre de pantallas" en el comedor y respétenla todos hoy.',
          'Consciente': 'Dediquen 15 minutos en la tarde para dar un paseo familiar corto y conversar.',
          'Intencional': 'Instalen una "canasta de celulares" a la entrada para desconectarse al llegar.',
          'Plena': 'Diseña un ritual de fin de semana que les permita reconectar con la naturaleza o un juego.'
        }
      }
    };

    const dict = detailsMap[this.activeDimension];
    statement = dict.statement[level] || statement;
    tip = dict.tip[level] || tip;

    return {
      name: dict.name,
      score: score.toFixed(1),
      level,
      color,
      statement,
      tip
    };
  }
}
