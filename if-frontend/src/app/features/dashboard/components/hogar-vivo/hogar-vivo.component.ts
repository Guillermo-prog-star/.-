import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

export interface HogarDimension {
  key: 'comunicacion' | 'habitos' | 'tiempos' | 'emociones';
  score: number;   // 0–5
  label: string;
  room: string;
  icon: string;
  route: string;
  narrativa: string;
}

@Component({
  selector: 'app-hogar-vivo',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="hogar-container">

  <!-- Título -->
  <div class="hogar-header">
    <div class="hh-icon">🏠</div>
    <div>
      <h2 class="hh-title">El Hogar Vivo</h2>
      <p class="hh-sub">Así está la energía de tu familia hoy</p>
    </div>
    <div class="hh-badge" [class]="overallClass()">
      {{ overallEmoji() }} {{ overallLabel() }}
    </div>
  </div>

  <!-- Casa SVG interactiva -->
  <div class="casa-wrap">
    <svg class="casa-svg" viewBox="0 0 520 380" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <!-- Filtros de resplandor por habitación -->
        @for (dim of dimensions; track dim.key) {
          <filter [attr.id]="'glow-' + dim.key" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="4" result="blur"/>
            <feFlood [attr.flood-color]="roomColor(dim)" flood-opacity="0.6" result="color"/>
            <feComposite in="color" in2="blur" operator="in" result="glow"/>
            <feMerge><feMergeNode in="glow"/><feMergeNode in="SourceGraphic"/></feMerge>
          </filter>
        }
        <!-- Gradiente del cielo -->
        <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" [attr.stop-color]="skyTop()"/>
          <stop offset="100%" [attr.stop-color]="skyBottom()"/>
        </linearGradient>
        <!-- Gradiente del suelo -->
        <linearGradient id="ground" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" [attr.stop-color]="groundTop()"/>
          <stop offset="100%" stop-color="#0d1117"/>
        </linearGradient>
      </defs>

      <!-- Fondo cielo -->
      <rect width="520" height="380" fill="url(#sky)"/>

      <!-- Estrellas/partículas si la familia está en buen estado -->
      @if (overallScore() >= 3) {
        <circle cx="50" cy="30" r="1.5" fill="white" opacity="0.6"/>
        <circle cx="120" cy="15" r="1" fill="white" opacity="0.5"/>
        <circle cx="400" cy="25" r="1.5" fill="white" opacity="0.7"/>
        <circle cx="470" cy="40" r="1" fill="white" opacity="0.4"/>
        <circle cx="200" cy="10" r="1" fill="white" opacity="0.6"/>
        <circle cx="340" cy="18" r="1.5" fill="white" opacity="0.5"/>
      }

      <!-- Luna/Sol según estado -->
      @if (overallScore() >= 4) {
        <!-- Sol cálido -->
        <circle cx="450" cy="50" r="22" fill="#fbbf24" opacity="0.25"/>
        <circle cx="450" cy="50" r="14" fill="#f59e0b" opacity="0.4"/>
      } @else {
        <!-- Luna -->
        <circle cx="450" cy="50" r="18" fill="#e2e8f0" opacity="0.15"/>
        <circle cx="460" cy="44" r="14" fill="url(#sky)" opacity="0.9"/>
      }

      <!-- Suelo -->
      <rect x="0" y="310" width="520" height="70" fill="url(#ground)"/>

      <!-- Jardín (si tiempos score es alto) -->
      @if (getScore('tiempos') >= 3) {
        <ellipse cx="80" cy="315" rx="30" ry="8" [attr.fill]="gardenColor()" opacity="0.4"/>
        <ellipse cx="440" cy="315" rx="28" ry="7" [attr.fill]="gardenColor()" opacity="0.35"/>
        <!-- Árbol izquierdo -->
        <rect x="72" y="280" width="5" height="35" fill="#6b7280" opacity="0.5"/>
        <circle cx="74" cy="270" r="18" [attr.fill]="gardenColor()" [attr.opacity]="treeOpacity()"/>
        <!-- Árbol derecho -->
        <rect x="437" y="283" width="5" height="32" fill="#6b7280" opacity="0.5"/>
        <circle cx="439" cy="274" r="15" [attr.fill]="gardenColor()" [attr.opacity]="treeOpacity()"/>
      }

      <!-- ── ESTRUCTURA DE LA CASA ──────────────────── -->

      <!-- Techo -->
      <polygon points="90,145 260,50 430,145"
               fill="#1e1b4b" stroke="rgba(255,255,255,0.08)" stroke-width="1.5"/>
      <!-- Cumbrera decorativa -->
      <polygon points="90,145 260,50 430,145 430,155 260,60 90,155"
               fill="rgba(255,255,255,0.03)"/>
      <!-- Chimenea -->
      <rect x="310" y="70" width="22" height="55" rx="3"
            fill="#1e1b4b" stroke="rgba(255,255,255,0.1)" stroke-width="1"/>
      <!-- Humo (si hay actividad) -->
      @if (overallScore() >= 2) {
        <ellipse cx="321" cy="62" rx="5" ry="7"
                 fill="rgba(255,255,255,0.06)" [attr.opacity]="smokeOpacity()"/>
        <ellipse cx="316" cy="50" rx="4" ry="6"
                 fill="rgba(255,255,255,0.04)" [attr.opacity]="smokeOpacity()"/>
      }

      <!-- Fachada principal -->
      <rect x="90" y="145" width="340" height="165" rx="2"
            fill="#111827" stroke="rgba(255,255,255,0.07)" stroke-width="1.5"/>

      <!-- ── HABITACIONES ─────────────────────────── -->

      <!-- Sala (arriba izquierda) — Comunicación -->
      <g class="room-group" (click)="navigateTo('comunicacion')" style="cursor:pointer">
        <rect x="98" y="153" width="152" height="72" rx="4"
              [attr.fill]="roomFill('comunicacion')"
              [attr.filter]="activeRoom() === 'comunicacion' ? 'url(#glow-comunicacion)' : ''"
              (mouseenter)="activeRoom.set('comunicacion')"
              (mouseleave)="activeRoom.set(null)"/>
        <!-- Ventana sala -->
        <rect x="108" y="163" width="60" height="42" rx="3"
              fill="rgba(0,0,0,0.4)" stroke="rgba(255,255,255,0.1)" stroke-width="0.8"/>
        <!-- Cruz ventana -->
        <line x1="138" y1="163" x2="138" y2="205" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <line x1="108" y1="184" x2="168" y2="184" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <!-- Luz interior ventana -->
        <rect x="109" y="164" width="28" height="19" rx="2"
              [attr.fill]="windowGlow('comunicacion')" [attr.opacity]="windowOpacity('comunicacion')"/>
        <rect x="109" y="185" width="28" height="18" rx="2"
              [attr.fill]="windowGlow('comunicacion')" [attr.opacity]="windowOpacity('comunicacion') * 0.7"/>
        <!-- Ícono y label -->
        <text x="185" y="185" text-anchor="middle" font-size="16" fill="white" opacity="0.9">🛋️</text>
        <text x="185" y="200" text-anchor="middle" font-size="8" font-weight="600"
              [attr.fill]="roomLabelColor('comunicacion')" font-family="sans-serif">Sala</text>
        <text x="185" y="212" text-anchor="middle" font-size="7"
              fill="rgba(255,255,255,0.5)" font-family="sans-serif">Comunicación</text>
        <!-- Score bar -->
        <rect x="115" y="218" width="120" height="3" rx="1.5" fill="rgba(255,255,255,0.08)"/>
        <rect x="115" y="218" [attr.width]="scoreWidth('comunicacion', 120)" height="3" rx="1.5"
              [attr.fill]="roomColor(getDim('comunicacion'))"/>
      </g>

      <!-- Cocina (arriba derecha) — Hábitos -->
      <g class="room-group" (click)="navigateTo('habitos')" style="cursor:pointer">
        <rect x="258" y="153" width="164" height="72" rx="4"
              [attr.fill]="roomFill('habitos')"
              [attr.filter]="activeRoom() === 'habitos' ? 'url(#glow-habitos)' : ''"
              (mouseenter)="activeRoom.set('habitos')"
              (mouseleave)="activeRoom.set(null)"/>
        <!-- Ventana cocina -->
        <rect x="268" y="163" width="60" height="42" rx="3"
              fill="rgba(0,0,0,0.4)" stroke="rgba(255,255,255,0.1)" stroke-width="0.8"/>
        <line x1="298" y1="163" x2="298" y2="205" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <line x1="268" y1="184" x2="328" y2="184" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <rect x="269" y="164" width="28" height="19" rx="2"
              [attr.fill]="windowGlow('habitos')" [attr.opacity]="windowOpacity('habitos')"/>
        <rect x="269" y="185" width="28" height="18" rx="2"
              [attr.fill]="windowGlow('habitos')" [attr.opacity]="windowOpacity('habitos') * 0.7"/>
        <text x="365" y="185" text-anchor="middle" font-size="16" fill="white" opacity="0.9">🍳</text>
        <text x="365" y="200" text-anchor="middle" font-size="8" font-weight="600"
              [attr.fill]="roomLabelColor('habitos')" font-family="sans-serif">Cocina</text>
        <text x="365" y="212" text-anchor="middle" font-size="7"
              fill="rgba(255,255,255,0.5)" font-family="sans-serif">Hábitos</text>
        <rect x="275" y="218" width="120" height="3" rx="1.5" fill="rgba(255,255,255,0.08)"/>
        <rect x="275" y="218" [attr.width]="scoreWidth('habitos', 120)" height="3" rx="1.5"
              [attr.fill]="roomColor(getDim('habitos'))"/>
      </g>

      <!-- Jardín / Patio (abajo izquierda) — Tiempos -->
      <g class="room-group" (click)="navigateTo('tiempos')" style="cursor:pointer">
        <rect x="98" y="233" width="152" height="70" rx="4"
              [attr.fill]="roomFill('tiempos')"
              [attr.filter]="activeRoom() === 'tiempos' ? 'url(#glow-tiempos)' : ''"
              (mouseenter)="activeRoom.set('tiempos')"
              (mouseleave)="activeRoom.set(null)"/>
        <rect x="108" y="243" width="60" height="42" rx="3"
              fill="rgba(0,0,0,0.4)" stroke="rgba(255,255,255,0.1)" stroke-width="0.8"/>
        <line x1="138" y1="243" x2="138" y2="285" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <line x1="108" y1="264" x2="168" y2="264" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
        <rect x="109" y="244" width="28" height="19" rx="2"
              [attr.fill]="windowGlow('tiempos')" [attr.opacity]="windowOpacity('tiempos')"/>
        <text x="185" y="265" text-anchor="middle" font-size="16" fill="white" opacity="0.9">🌿</text>
        <text x="185" y="279" text-anchor="middle" font-size="8" font-weight="600"
              [attr.fill]="roomLabelColor('tiempos')" font-family="sans-serif">Jardín</text>
        <text x="185" y="291" text-anchor="middle" font-size="7"
              fill="rgba(255,255,255,0.5)" font-family="sans-serif">Tiempos juntos</text>
        <rect x="115" y="297" width="120" height="3" rx="1.5" fill="rgba(255,255,255,0.08)"/>
        <rect x="115" y="297" [attr.width]="scoreWidth('tiempos', 120)" height="3" rx="1.5"
              [attr.fill]="roomColor(getDim('tiempos'))"/>
      </g>

      <!-- Corazón del Hogar (abajo derecha) — Emociones + Puerta -->
      <g class="room-group" (click)="navigateTo('emociones')" style="cursor:pointer">
        <rect x="258" y="233" width="164" height="70" rx="4"
              [attr.fill]="roomFill('emociones')"
              [attr.filter]="activeRoom() === 'emociones' ? 'url(#glow-emociones)' : ''"
              (mouseenter)="activeRoom.set('emociones')"
              (mouseleave)="activeRoom.set(null)"/>
        <!-- Puerta central -->
        <rect x="332" y="255" width="32" height="48" rx="4"
              fill="rgba(0,0,0,0.5)" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
        <circle cx="358" cy="280" r="2.5" fill="rgba(255,255,255,0.4)"/>
        <!-- Arco de puerta -->
        <path d="M332,270 Q348,252 364,270" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="1"/>
        <!-- Luz bajo la puerta -->
        <rect x="333" y="300" width="30" height="2" rx="1"
              [attr.fill]="windowGlow('emociones')" [attr.opacity]="windowOpacity('emociones')"/>
        <rect x="268" y="243" width="55" height="35" rx="3"
              fill="rgba(0,0,0,0.4)" stroke="rgba(255,255,255,0.1)" stroke-width="0.8"/>
        <rect x="269" y="244" width="25" height="16" rx="2"
              [attr.fill]="windowGlow('emociones')" [attr.opacity]="windowOpacity('emociones')"/>
        <text x="400" y="265" text-anchor="middle" font-size="16" fill="white" opacity="0.9">❤️</text>
        <text x="400" y="279" text-anchor="middle" font-size="8" font-weight="600"
              [attr.fill]="roomLabelColor('emociones')" font-family="sans-serif">Corazón</text>
        <text x="400" y="291" text-anchor="middle" font-size="7"
              fill="rgba(255,255,255,0.5)" font-family="sans-serif">Emociones</text>
        <rect x="275" y="297" width="120" height="3" rx="1.5" fill="rgba(255,255,255,0.08)"/>
        <rect x="275" y="297" [attr.width]="scoreWidth('emociones', 120)" height="3" rx="1.5"
              [attr.fill]="roomColor(getDim('emociones'))"/>
      </g>

      <!-- Divisores entre habitaciones -->
      <line x1="250" y1="153" x2="250" y2="303" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>
      <line x1="98" y1="225" x2="422" y2="225" stroke="rgba(255,255,255,0.06)" stroke-width="1.5"/>

      <!-- Número de la casa -->
      <text x="260" y="340" text-anchor="middle" font-size="10" font-weight="700"
            fill="rgba(255,255,255,0.2)" font-family="sans-serif" letter-spacing="2">FAMILIA</text>

    </svg>
  </div>

  <!-- ── Tooltip de habitación activa ─────────────── -->
  @if (activeRoom() && getActiveDim(); as dim) {
    <div class="room-tooltip" [class]="'rt-' + dim.key">
      <div class="rt-header">
        <span class="rt-icon">{{ dim.icon }}</span>
        <div>
          <span class="rt-room">{{ dim.room }}</span>
          <span class="rt-dimension">{{ dim.label }}</span>
        </div>
        <div class="rt-score">
          <span class="rt-score-num" [style.color]="roomColor(dim)">{{ dim.score }}</span>
          <span class="rt-score-max">/5</span>
        </div>
      </div>
      <p class="rt-narrativa">{{ dim.narrativa }}</p>
      <div class="rt-level-bar">
        @for (i of [1,2,3,4,5]; track i) {
          <div class="rtl-dot" [class.active]="i <= dim.score"
               [style.background]="i <= dim.score ? roomColor(dim) : 'rgba(255,255,255,0.1)'"></div>
        }
      </div>
    </div>
  }

  <!-- ── Cards de estado por dimensión ─────────────── -->
  <div class="dims-grid">
    @for (dim of dimensions; track dim.key) {
      <div class="dim-card" [class.active]="activeRoom() === dim.key"
           (click)="navigateTo(dim.key)"
           (mouseenter)="activeRoom.set(dim.key)"
           (mouseleave)="activeRoom.set(null)">
        <div class="dc-icon">{{ dim.icon }}</div>
        <div class="dc-info">
          <div class="dc-room">{{ dim.room }}</div>
          <div class="dc-label">{{ dim.label }}</div>
        </div>
        <div class="dc-score-wrap">
          <div class="dc-score" [style.color]="roomColor(dim)">{{ dim.score }}</div>
          <div class="dc-level-label" [style.color]="roomColor(dim)">{{ levelLabel(dim.score) }}</div>
        </div>
        <div class="dc-bar">
          <div class="dc-bar-fill" [style.width.%]="(dim.score/5)*100" [style.background]="roomColor(dim)"></div>
        </div>
      </div>
    }
  </div>

</div>
  `,
  styles: [`
    .hogar-container {
      display: flex; flex-direction: column; gap: 16px;
      background: rgba(255,255,255,0.02);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 24px; padding: 24px;
      font-family: inherit;
    }

    /* Header */
    .hogar-header {
      display: flex; align-items: center; gap: 12px;
      flex-wrap: wrap;
    }
    .hh-icon { font-size: 28px; }
    .hh-title { font-size: 18px; font-weight: 800; color: #fff; margin: 0; }
    .hh-sub   { font-size: 12px; color: rgba(255,255,255,0.5); margin: 0; }
    .hh-badge {
      margin-left: auto;
      padding: 5px 14px; border-radius: 20px;
      font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em;
    }
    .hh-badge.level-bajo     { background:rgba(239,68,68,.15);border:1px solid rgba(239,68,68,.3);color:#fca5a5; }
    .hh-badge.level-medio    { background:rgba(249,115,22,.15);border:1px solid rgba(249,115,22,.3);color:#fb923c; }
    .hh-badge.level-bueno    { background:rgba(250,204,21,.12);border:1px solid rgba(250,204,21,.3);color:#fde047; }
    .hh-badge.level-pleno    { background:rgba(16,185,129,.15);border:1px solid rgba(16,185,129,.3);color:#6ee7b7; }

    /* SVG casa */
    .casa-wrap {
      width: 100%; border-radius: 18px; overflow: hidden;
      background: #0d1117;
      border: 1px solid rgba(255,255,255,0.06);
    }
    .casa-svg { width: 100%; height: auto; display: block; cursor: default; }
    .room-group { transition: opacity 0.2s; }
    .room-group:hover { opacity: 0.92; }

    /* Tooltip */
    .room-tooltip {
      border-radius: 14px; padding: 14px 16px;
      background: rgba(13,17,28,0.95);
      border: 1px solid rgba(255,255,255,0.1);
      animation: fadeIn 0.2s ease;
    }
    @keyframes fadeIn { from{opacity:0;transform:translateY(4px)} to{opacity:1;transform:none} }
    .rt-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .rt-icon   { font-size: 22px; }
    .rt-room   { font-size: 14px; font-weight: 800; color: #fff; display: block; }
    .rt-dimension { font-size: 11px; color: rgba(255,255,255,.5); display: block; }
    .rt-score  { margin-left: auto; text-align: right; }
    .rt-score-num { font-size: 22px; font-weight: 900; }
    .rt-score-max { font-size: 12px; color: rgba(255,255,255,.4); }
    .rt-narrativa { font-size: 13px; color: rgba(255,255,255,.7); line-height: 1.6; margin: 0 0 10px; font-style: italic; }
    .rt-level-bar { display: flex; gap: 5px; }
    .rtl-dot { width: 28px; height: 5px; border-radius: 3px; transition: background 0.3s; }

    /* Cards de dimensiones */
    .dims-grid {
      display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px;
    }
    @media (min-width: 640px) { .dims-grid { grid-template-columns: repeat(4, 1fr); } }

    .dim-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 14px; padding: 14px 12px;
      cursor: pointer; transition: all 0.22s;
      display: flex; flex-direction: column; gap: 6px;
    }
    .dim-card:hover, .dim-card.active {
      background: rgba(255,255,255,0.06);
      border-color: rgba(255,255,255,0.14);
      transform: translateY(-2px);
    }
    .dc-icon  { font-size: 22px; }
    .dc-room  { font-size: 13px; font-weight: 700; color: #fff; }
    .dc-label { font-size: 10px; color: rgba(255,255,255,.5); }
    .dc-score-wrap { display: flex; align-items: baseline; gap: 5px; }
    .dc-score { font-size: 22px; font-weight: 900; }
    .dc-level-label { font-size: 10px; font-weight: 700; }
    .dc-bar {
      height: 4px; background: rgba(255,255,255,0.08); border-radius: 2px; overflow: hidden;
    }
    .dc-bar-fill { height: 100%; border-radius: 2px; transition: width 0.8s ease; }
  `]
})
export class HogarVivoComponent {
  private router = inject(Router);

  @Input() set scores(val: Partial<Record<string, number>>) {
    this.dimensions = this.dimensions.map(d => ({
      ...d,
      score: val[d.key] ?? d.score
    }));
  }

  readonly activeRoom = signal<string | null>(null);

  dimensions: HogarDimension[] = [
    {
      key: 'comunicacion',
      score: 3,
      label: 'Comunicación',
      room: 'La Sala',
      icon: '🛋️',
      route: '/evaluations/history',
      narrativa: 'El espacio donde la familia se reúne a escucharse, a dialogar, a construir acuerdos.'
    },
    {
      key: 'habitos',
      score: 2,
      label: 'Hábitos',
      room: 'La Cocina',
      icon: '🍳',
      route: '/checklist',
      narrativa: 'Los rituales cotidianos que nutren a la familia. Lo que se hace juntos, día a día.'
    },
    {
      key: 'tiempos',
      score: 4,
      label: 'Tiempos juntos',
      room: 'El Jardín',
      icon: '🌿',
      route: '/plans',
      narrativa: 'El lugar donde la familia respira, juega y crea memorias que duran generaciones.'
    },
    {
      key: 'emociones',
      score: 2,
      label: 'Emociones',
      room: 'Corazón del Hogar',
      icon: '❤️',
      route: '/my-space',
      narrativa: 'La dimensión más profunda. Cuando las emociones fluyen, todo lo demás mejora.'
    }
  ];

  // ── Computed ──────────────────────────────────────

  overallScore = computed(() => {
    const sum = this.dimensions.reduce((a, d) => a + d.score, 0);
    return sum / this.dimensions.length;
  });

  overallClass = computed(() => {
    const s = this.overallScore();
    if (s < 2)  return 'level-bajo';
    if (s < 3)  return 'level-medio';
    if (s < 4)  return 'level-bueno';
    return 'level-pleno';
  });

  overallLabel = computed(() => {
    const s = this.overallScore();
    if (s < 2)  return 'Hogar en construcción';
    if (s < 3)  return 'Hogar despertando';
    if (s < 4)  return 'Hogar creciendo';
    return 'Hogar pleno';
  });

  overallEmoji = computed(() => {
    const s = this.overallScore();
    if (s < 2)  return '🌑';
    if (s < 3)  return '🌒';
    if (s < 4)  return '🌔';
    return '🌕';
  });

  // Colores del cielo según estado general
  skyTop    = computed(() => {
    const s = this.overallScore();
    if (s < 2)  return '#0d1117';
    if (s < 3)  return '#111827';
    if (s < 4)  return '#1e1b4b';
    return '#1a2744';
  });
  skyBottom = computed(() => {
    const s = this.overallScore();
    if (s < 2)  return '#0d1117';
    if (s < 3)  return '#1f2937';
    if (s < 4)  return '#312e81';
    return '#1e3a6e';
  });
  groundTop = computed(() => {
    const s = this.overallScore();
    if (s >= 3) return '#1a2e1a';
    return '#111827';
  });
  gardenColor = computed(() => {
    const s = this.getScore('tiempos');
    if (s >= 4) return '#22c55e';
    if (s >= 3) return '#16a34a';
    return '#15803d';
  });
  treeOpacity = computed(() => Math.min(0.9, 0.3 + this.getScore('tiempos') * 0.15));
  smokeOpacity = computed(() => Math.min(0.8, this.overallScore() * 0.2));

  // ── Color de cada habitación según score ──────────

  roomColor(dim: HogarDimension | undefined): string {
    if (!dim) return '#6b7280';
    const s = dim.score;
    if (s <= 1) return '#6b7280';
    if (s === 2) return '#f97316';
    if (s === 3) return '#f59e0b';
    if (s === 4) return '#22c55e';
    return '#10b981';
  }

  roomFill(key: string): string {
    const dim = this.getDim(key);
    if (!dim) return 'rgba(255,255,255,0.02)';
    const s = dim.score;
    if (s <= 1) return 'rgba(107,114,128,0.06)';
    if (s === 2) return 'rgba(249,115,22,0.08)';
    if (s === 3) return 'rgba(245,158,11,0.1)';
    if (s === 4) return 'rgba(34,197,94,0.1)';
    return 'rgba(16,185,129,0.14)';
  }

  windowGlow(key: string): string {
    const dim = this.getDim(key);
    if (!dim || dim.score <= 1) return '#374151';
    return this.roomColor(dim);
  }

  windowOpacity(key: string): number {
    const dim = this.getDim(key);
    if (!dim) return 0;
    return Math.min(0.85, dim.score * 0.18);
  }

  roomLabelColor(key: string): string {
    const dim = this.getDim(key);
    if (!dim || dim.score <= 1) return 'rgba(255,255,255,0.5)';
    return this.roomColor(dim);
  }

  scoreWidth(key: string, maxWidth: number): number {
    const dim = this.getDim(key);
    if (!dim) return 0;
    return (dim.score / 5) * maxWidth;
  }

  levelLabel(score: number): string {
    if (score <= 1) return 'Dormido';
    if (score === 2) return 'Despertando';
    if (score === 3) return 'Consciente';
    if (score === 4) return 'Intencional';
    return 'Pleno';
  }

  // ── Helpers ────────────────────────────────────────

  getDim(key: string): HogarDimension | undefined {
    return this.dimensions.find(d => d.key === key);
  }

  getScore(key: string): number {
    return this.getDim(key)?.score ?? 0;
  }

  getActiveDim(): HogarDimension | undefined {
    const key = this.activeRoom();
    return key ? this.getDim(key) : undefined;
  }

  navigateTo(key: string): void {
    const dim = this.getDim(key);
    if (dim) this.router.navigate([dim.route]);
  }
}

// Needed for inject() outside constructor
import { inject } from '@angular/core';
