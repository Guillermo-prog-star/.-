import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

export interface ArbolStats {
  mesesActivos: number;       // 0–36
  misionesCumplidas: number;
  evidencias: number;
  capsulas: number;           // mini documentales
  aprendizajes: number;       // reflexiones
  icfActual: number;          // 0–100
}

interface Elemento {
  tipo: 'hoja' | 'flor' | 'fruto' | 'capsula';
  x: number; y: number;
  color: string;
  escala: number;
  etiqueta?: string;
}

@Component({
  selector: 'app-arbol-vivo',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="av-container">

  <!-- Encabezado -->
  <div class="av-header">
    <div class="av-header-icon">🌳</div>
    <div>
      <h2 class="av-title">El Árbol Vivo</h2>
      <p class="av-sub">
        Mes {{ stats.mesesActivos }} de 36 ·
        <span [style.color]="etapaColor()">{{ etapaLabel() }}</span>
      </p>
    </div>
    <div class="av-progress-ring" [title]="stats.mesesActivos + ' de 36 meses'">
      <svg width="52" height="52" viewBox="0 0 52 52">
        <circle cx="26" cy="26" r="22" fill="none" stroke="rgba(255,255,255,0.07)" stroke-width="4"/>
        <circle cx="26" cy="26" r="22" fill="none"
          [attr.stroke]="etapaColor()"
          stroke-width="4"
          stroke-linecap="round"
          [attr.stroke-dasharray]="138"
          [attr.stroke-dashoffset]="138 - (138 * stats.mesesActivos / 36)"
          transform="rotate(-90 26 26)"
          style="transition: stroke-dashoffset 1.2s ease"/>
        <text x="26" y="31" text-anchor="middle"
              font-size="11" font-weight="800" fill="white" font-family="sans-serif">
          {{ pct() }}%
        </text>
      </svg>
    </div>
  </div>

  <!-- El Árbol SVG -->
  <div class="av-tree-wrap">
    <svg class="av-svg" viewBox="0 0 500 480" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="skyAvGrad" cx="50%" cy="100%" r="80%">
          <stop offset="0%" [attr.stop-color]="skyBase()" stop-opacity="1"/>
          <stop offset="100%" stop-color="#07052a" stop-opacity="1"/>
        </radialGradient>
        <radialGradient id="suelo" cx="50%" cy="0%" r="80%">
          <stop offset="0%" [attr.stop-color]="sueloColor()" stop-opacity="1"/>
          <stop offset="100%" stop-color="#0a1a0e" stop-opacity="1"/>
        </radialGradient>
        <filter id="leafGlow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <filter id="trunkShadow">
          <feDropShadow dx="2" dy="4" stdDeviation="4" flood-color="rgba(0,0,0,0.4)"/>
        </filter>
      </defs>

      <!-- Cielo -->
      <rect width="500" height="480" fill="url(#skyAvGrad)"/>

      <!-- Estrellas/partículas según estado -->
      @if (stats.mesesActivos >= 6) {
        <circle cx="60"  cy="40"  r="1.2" fill="white" opacity=".5"/>
        <circle cx="140" cy="20"  r="1"   fill="white" opacity=".4"/>
        <circle cx="380" cy="35"  r="1.5" fill="white" opacity=".55"/>
        <circle cx="450" cy="15"  r="1"   fill="white" opacity=".4"/>
        <circle cx="250" cy="12"  r="1.2" fill="white" opacity=".45"/>
      }

      <!-- Sol/Luna según progreso -->
      @if (stats.mesesActivos >= 24) {
        <!-- Sol brillante (familia madura) -->
        <circle cx="430" cy="55" r="30" [attr.fill]="etapaColor()" opacity=".12"/>
        <circle cx="430" cy="55" r="20" [attr.fill]="etapaColor()" opacity=".18"/>
        <circle cx="430" cy="55" r="13" [attr.fill]="etapaColor()" opacity=".28"/>
      } @else if (stats.mesesActivos >= 6) {
        <!-- Luna media -->
        <circle cx="430" cy="55" r="22" fill="#fef3c7" opacity=".14"/>
        <circle cx="442" cy="48" r="16" fill="#07052a" opacity=".92"/>
      }

      <!-- Suelo -->
      <ellipse cx="250" cy="445" rx="200" ry="28" fill="url(#suelo)"/>
      <ellipse cx="250" cy="442" rx="160" ry="18" [attr.fill]="sueloColor()" opacity=".5"/>

      <!-- Raíces visibles (crecen con el tiempo) -->
      @if (stats.mesesActivos >= 3) {
        <path [attr.d]="'M250 440 Q220 445 195 452'" fill="none" [attr.stroke]="trunkColor()" stroke-width="5" stroke-linecap="round" opacity=".6"/>
        <path [attr.d]="'M250 440 Q280 445 305 452'" fill="none" [attr.stroke]="trunkColor()" stroke-width="5" stroke-linecap="round" opacity=".6"/>
      }
      @if (stats.mesesActivos >= 12) {
        <path [attr.d]="'M245 442 Q210 450 178 458'" fill="none" [attr.stroke]="trunkColor()" stroke-width="4" stroke-linecap="round" opacity=".45"/>
        <path [attr.d]="'M255 442 Q290 450 320 458'" fill="none" [attr.stroke]="trunkColor()" stroke-width="4" stroke-linecap="round" opacity=".45"/>
      }
      @if (stats.mesesActivos >= 24) {
        <path [attr.d]="'M240 444 Q195 455 165 462'" fill="none" [attr.stroke]="trunkColor()" stroke-width="3" stroke-linecap="round" opacity=".35"/>
        <path [attr.d]="'M260 444 Q305 455 335 462'" fill="none" [attr.stroke]="trunkColor()" stroke-width="3" stroke-linecap="round" opacity=".35"/>
      }

      <!-- TRONCO — crece con mesesActivos -->
      <path [attr.d]="trunkPath()" [attr.stroke]="trunkColor()"
            [attr.stroke-width]="trunkWidth()" fill="none"
            stroke-linecap="round" filter="url(#trunkShadow)"
            style="transition: all 0.8s ease"/>

      <!-- RAMAS principales — aparecen por etapa -->
      @for (rama of ramas(); track rama.id) {
        <path [attr.d]="rama.path" [attr.stroke]="rama.color"
              [attr.stroke-width]="rama.w" fill="none"
              stroke-linecap="round" [attr.opacity]="rama.opacity"
              style="transition: all 0.6s ease"/>
      }

      <!-- COPA/FOLLAJE base (forma orgánica) -->
      @if (stats.mesesActivos >= 2) {
        <ellipse [attr.cx]="250" [attr.cy]="copaY()"
                 [attr.rx]="copaRx()" [attr.ry]="copaRy()"
                 [attr.fill]="copaColor()" [attr.opacity]="copaOpacity()"
                 filter="url(#leafGlow)"
                 style="transition: all 0.8s ease"/>
      }
      <!-- Copas secundarias orgánicas -->
      @if (stats.mesesActivos >= 8) {
        <ellipse [attr.cx]="210" [attr.cy]="copaY() + 20"
                 [attr.rx]="copaRx() * 0.65" [attr.ry]="copaRy() * 0.7"
                 [attr.fill]="copa2Color()" opacity=".7"
                 style="transition: all 0.8s ease"/>
        <ellipse [attr.cx]="290" [attr.cy]="copaY() + 25"
                 [attr.rx]="copaRx() * 0.60" [attr.ry]="copaRy() * 0.65"
                 [attr.fill]="copa3Color()" opacity=".65"
                 style="transition: all 0.8s ease"/>
      }
      @if (stats.mesesActivos >= 18) {
        <ellipse [attr.cx]="185" [attr.cy]="copaY() + 45"
                 [attr.rx]="copaRx() * 0.5" [attr.ry]="copaRy() * 0.55"
                 [attr.fill]="copaColor()" opacity=".55"/>
        <ellipse [attr.cx]="315" [attr.cy]="copaY() + 50"
                 [attr.rx]="copaRx() * 0.48" [attr.ry]="copaRy() * 0.52"
                 [attr.fill]="copa2Color()" opacity=".5"/>
      }

      <!-- ELEMENTOS individuales (hojas, flores, frutos, cápsulas) -->
      @for (el of elementos(); track el.x + '-' + el.y) {
        @if (el.tipo === 'hoja') {
          <ellipse [attr.cx]="el.x" [attr.cy]="el.y"
                   rx="7" ry="4"
                   [attr.fill]="el.color" [attr.opacity]="0.85 * el.escala"
                   [attr.transform]="'rotate(' + (el.x * 37 % 360) + ' ' + el.x + ' ' + el.y + ')'"/>
        }
        @if (el.tipo === 'flor') {
          <circle [attr.cx]="el.x" [attr.cy]="el.y"
                  [attr.r]="5 * el.escala"
                  [attr.fill]="el.color" opacity=".9"
                  filter="url(#leafGlow)"/>
          <circle [attr.cx]="el.x" [attr.cy]="el.y"
                  [attr.r]="2.5 * el.escala"
                  fill="white" opacity=".8"/>
        }
        @if (el.tipo === 'fruto') {
          <circle [attr.cx]="el.x" [attr.cy]="el.y"
                  [attr.r]="6 * el.escala"
                  [attr.fill]="el.color" opacity=".88"
                  filter="url(#leafGlow)"/>
          <ellipse [attr.cx]="el.x + 1" [attr.cy]="el.y - 2"
                   rx="2" ry="1.5"
                   fill="white" opacity=".3"/>
        }
        @if (el.tipo === 'capsula') {
          <!-- Pequeño mini-documental (rama especial) -->
          <rect [attr.x]="el.x - 7" [attr.y]="el.y - 5"
                width="14" height="10" rx="3"
                [attr.fill]="el.color" opacity=".8"
                filter="url(#leafGlow)"/>
          <text [attr.x]="el.x" [attr.y]="el.y + 3.5"
                text-anchor="middle" font-size="6" fill="white" opacity=".9"
                font-family="sans-serif">🎬</text>
        }
      }

      <!-- SEMILLA inicial (mes 0-1) -->
      @if (stats.mesesActivos <= 1) {
        <ellipse cx="250" cy="435" rx="12" ry="8"
                 fill="#84cc16" opacity=".8" filter="url(#leafGlow)"/>
        <path d="M250 427 Q248 415 250 408 Q252 415 250 427"
              fill="none" stroke="#4ade80" stroke-width="1.5" stroke-linecap="round"/>
        <text x="250" y="455" text-anchor="middle"
              font-size="10" fill="rgba(255,255,255,.45)" font-family="sans-serif">
          Semilla plantada
        </text>
      }

      <!-- SEMILLA GENERACIONAL (mes 36) -->
      @if (stats.mesesActivos >= 36) {
        <circle cx="250" cy="455" r="10"
                fill="#f59e0b" opacity=".9" filter="url(#leafGlow)">
          <animate attributeName="r" values="10;13;10" dur="2s" repeatCount="indefinite"/>
        </circle>
        <text x="250" y="478" text-anchor="middle"
              font-size="9" fill="#fde68a" font-family="sans-serif" font-weight="700">
          Semilla generacional
        </text>
      }

      <!-- LEYENDA flotante en el árbol -->
      @if (stats.mesesActivos >= 4 && stats.misionesCumplidas > 0) {
        <text [attr.x]="250" y="32"
              text-anchor="middle" font-size="9" fill="rgba(255,255,255,.35)"
              font-family="sans-serif">
          {{ stats.misionesCumplidas }} misiones · {{ stats.evidencias }} evidencias · {{ stats.capsulas }} cápsulas
        </text>
      }

    </svg>
  </div>

  <!-- Leyenda de elementos -->
  <div class="av-legend">
    <div class="av-leg-item">
      <div class="av-leg-dot" style="background:#22c55e;"></div>
      <span>Hoja = misión cumplida ({{ stats.misionesCumplidas }})</span>
    </div>
    <div class="av-leg-item">
      <div class="av-leg-dot" style="background:#f472b6;"></div>
      <span>Flor = aprendizaje registrado ({{ stats.aprendizajes }})</span>
    </div>
    <div class="av-leg-item">
      <div class="av-leg-dot" style="background:#f97316;border-radius:50%;"></div>
      <span>Fruto = evidencia validada ({{ stats.evidencias }})</span>
    </div>
    <div class="av-leg-item">
      <div class="av-leg-dot" style="background:#a78bfa;border-radius:4px;"></div>
      <span>Rama especial = mini documental ({{ stats.capsulas }})</span>
    </div>
  </div>

  <!-- Frase de impacto según etapa -->
  <div class="av-phrase" [style.border-left-color]="etapaColor()">
    <span class="av-phrase-text">{{ etapaFrase() }}</span>
  </div>

  <!-- Llamada a acción -->
  <div class="av-cta" (click)="irAMisiones()">
    <span>{{ ctaLabel() }}</span>
    <span class="av-cta-arrow">→</span>
  </div>

</div>
  `,
  styles: [`
    .av-container {
      display: flex; flex-direction: column; gap: 16px;
      font-family: inherit; color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .av-header {
      display: flex; align-items: center; gap: 14px;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 16px; padding: 16px 18px;
    }
    .av-header-icon { font-size: 30px; }
    .av-title { font-size: 18px; font-weight: 800; color: #fff; margin: 0 0 3px; }
    .av-sub   { font-size: 12px; color: rgba(255,255,255,.55); margin: 0; }
    .av-progress-ring { margin-left: auto; flex-shrink: 0; }

    /* Árbol */
    .av-tree-wrap {
      border-radius: 18px; overflow: hidden;
      border: 1px solid rgba(255,255,255,.06);
    }
    .av-svg { width: 100%; display: block; }

    /* Leyenda */
    .av-legend {
      display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
      background: rgba(255,255,255,0.02);
      border: 1px solid rgba(255,255,255,0.06);
      border-radius: 14px; padding: 14px 16px;
    }
    .av-leg-item {
      display: flex; align-items: center; gap: 8px;
      font-size: 12px; color: rgba(255,255,255,.6);
    }
    .av-leg-dot {
      width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0;
    }

    /* Frase */
    .av-phrase {
      border-left: 3px solid #22c55e;
      padding: 12px 16px;
      background: rgba(255,255,255,0.02);
      border-radius: 0 12px 12px 0;
    }
    .av-phrase-text { font-size: 13px; color: rgba(255,255,255,.7); font-style: italic; line-height: 1.6; }

    /* CTA */
    .av-cta {
      display: flex; justify-content: space-between; align-items: center;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.09);
      border-radius: 12px; padding: 13px 18px;
      cursor: pointer; transition: all 0.2s;
      font-size: 13px; font-weight: 700; color: rgba(255,255,255,.8);
    }
    .av-cta:hover { background: rgba(255,255,255,0.08); border-color: rgba(255,255,255,0.15); }
    .av-cta-arrow { font-size: 16px; color: rgba(255,255,255,.4); }
  `]
})
export class ArbolVivoComponent implements OnInit {
  private router = inject(Router);

  @Input() stats: ArbolStats = {
    mesesActivos: 8,
    misionesCumplidas: 12,
    evidencias: 28,
    capsulas: 3,
    aprendizajes: 9,
    icfActual: 52
  };

  pct = computed(() => Math.round((this.stats.mesesActivos / 36) * 100));

  // ── Etapa del viaje ──────────────────────────────────

  etapaLabel = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m === 0)  return 'Semilla plantada';
    if (m <= 12)  return 'Reconocimiento';
    if (m <= 24)  return 'Amor';
    if (m < 36)   return 'Entrega';
    return 'Legado completo';
  });

  etapaColor = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m === 0)  return '#84cc16';
    if (m <= 12)  return '#22c55e';
    if (m <= 24)  return '#f97316';
    return '#f59e0b';
  });

  etapaFrase = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m <= 1)   return 'Toda historia grande empieza con una semilla. La suya ya fue plantada.';
    if (m <= 6)   return 'Las primeras raíces sostienen todo lo que vendrá. Su árbol está arraigando.';
    if (m <= 12)  return 'Cada misión cumplida es una hoja que no caerá. Su árbol está vivo.';
    if (m <= 18)  return 'El árbol que cuidan hoy, les dará sombra a ellos y frutos a sus hijos.';
    if (m <= 24)  return 'Más de la mitad del camino. Lo que construyeron ya no puede perderse.';
    if (m <= 30)  return 'Un árbol así tarda una generación en crecer. El suyo está a punto de madurar.';
    if (m < 36)   return 'La semilla generacional está formándose. La siguiente generación ya la recibirá.';
    return 'Su árbol está completo. Una semilla nueva espera a la siguiente generación.';
  });

  ctaLabel = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m === 0)  return 'Comenzar la primera misión';
    if (m <= 12)  return `Continuar — quedan ${12 - m} meses de Reconocimiento`;
    if (m <= 24)  return `En etapa Amor — mes ${m - 12} de 12`;
    if (m < 36)   return `Etapa Entrega — mes ${m - 24} de 12`;
    return 'Ver el legado generacional completo';
  });

  // ── Visuales del árbol ───────────────────────────────

  skyBase = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m === 0)  return '#0d1117';
    if (m <= 6)   return '#0f1a12';
    if (m <= 18)  return '#0d1f14';
    if (m <= 30)  return '#0f2010';
    return '#122612';
  });

  trunkColor = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m <= 3)  return '#6b7280';
    if (m <= 12) return '#78350f';
    if (m <= 24) return '#92400e';
    return '#a16207';
  });

  trunkWidth = computed((): number => {
    const m = this.stats.mesesActivos;
    if (m <= 1)  return 6;
    if (m <= 6)  return 10;
    if (m <= 12) return 16;
    if (m <= 24) return 22;
    return 28;
  });

  trunkPath = computed((): string => {
    const m = this.stats.mesesActivos;
    const base  = 440;
    const top   = Math.max(100, 440 - (m / 36) * 300);
    return `M250 ${base} Q248 ${(base + top) / 2 + 20} 250 ${top}`;
  });

  sueloColor = computed((): string => {
    const m = this.stats.mesesActivos;
    if (m === 0) return '#1f2937';
    if (m <= 6)  return '#14532d';
    if (m <= 18) return '#166534';
    return '#15803d';
  });

  copaY = computed((): number => {
    const m = this.stats.mesesActivos;
    return Math.max(110, 380 - (m / 36) * 240);
  });

  copaRx = computed((): number => {
    return Math.min(140, 20 + (this.stats.mesesActivos / 36) * 140);
  });

  copaRy = computed((): number => {
    return Math.min(110, 15 + (this.stats.mesesActivos / 36) * 110);
  });

  copaOpacity = computed((): number => {
    return Math.min(0.72, 0.15 + (this.stats.mesesActivos / 36) * 0.57);
  });

  copaColor  = computed(() => this.stats.mesesActivos <= 12 ? '#166534' : this.stats.mesesActivos <= 24 ? '#15803d' : '#16a34a');
  copa2Color = computed(() => this.stats.mesesActivos <= 12 ? '#14532d' : '#166534');
  copa3Color = computed(() => this.stats.mesesActivos <= 18 ? '#1a3a10' : '#166534');

  ramas = computed(() => {
    const m = this.stats.mesesActivos;
    const tc = this.trunkColor();
    const ramas: { id: number; path: string; color: string; w: number; opacity: number }[] = [];
    const topY = Math.max(100, 440 - (m / 36) * 300);

    if (m >= 2) {
      ramas.push({ id: 1, path: `M250 ${topY + 60} Q225 ${topY + 40} 190 ${topY + 20}`, color: tc, w: Math.min(10, m / 4), opacity: .85 });
      ramas.push({ id: 2, path: `M250 ${topY + 60} Q275 ${topY + 40} 310 ${topY + 20}`, color: tc, w: Math.min(10, m / 4), opacity: .85 });
    }
    if (m >= 6) {
      ramas.push({ id: 3, path: `M250 ${topY + 100} Q215 ${topY + 85} 170 ${topY + 70}`, color: tc, w: Math.min(8, m / 5), opacity: .8 });
      ramas.push({ id: 4, path: `M250 ${topY + 100} Q285 ${topY + 85} 330 ${topY + 70}`, color: tc, w: Math.min(8, m / 5), opacity: .8 });
    }
    if (m >= 12) {
      ramas.push({ id: 5, path: `M250 ${topY + 140} Q205 ${topY + 125} 155 ${topY + 110}`, color: tc, w: Math.min(7, m / 6), opacity: .75 });
      ramas.push({ id: 6, path: `M250 ${topY + 140} Q295 ${topY + 125} 345 ${topY + 110}`, color: tc, w: Math.min(7, m / 6), opacity: .75 });
    }
    if (m >= 20) {
      ramas.push({ id: 7, path: `M190 ${topY + 20} Q165 ${topY + 5} 145 ${topY - 10}`, color: tc, w: 5, opacity: .7 });
      ramas.push({ id: 8, path: `M310 ${topY + 20} Q335 ${topY + 5} 355 ${topY - 10}`, color: tc, w: 5, opacity: .7 });
    }
    if (m >= 28) {
      ramas.push({ id: 9,  path: `M170 ${topY + 70} Q145 ${topY + 55} 120 ${topY + 40}`, color: tc, w: 4, opacity: .65 });
      ramas.push({ id: 10, path: `M330 ${topY + 70} Q355 ${topY + 55} 380 ${topY + 40}`, color: tc, w: 4, opacity: .65 });
    }
    return ramas;
  });

  elementos = computed((): Elemento[] => {
    const items: Elemento[] = [];
    const topY = Math.max(100, 440 - (this.stats.mesesActivos / 36) * 300);
    const cx = 250;

    // Hojas por misiones cumplidas
    const nHojas = Math.min(this.stats.misionesCumplidas, 30);
    for (let i = 0; i < nHojas; i++) {
      const angle = (i / nHojas) * Math.PI * 2;
      const r = 40 + (i % 3) * 25 + (this.stats.mesesActivos / 36) * 50;
      items.push({
        tipo: 'hoja',
        x: cx + Math.cos(angle) * r,
        y: topY + this.copaRy() * 0.4 + Math.sin(angle) * (r * 0.6),
        color: i % 3 === 0 ? '#22c55e' : i % 3 === 1 ? '#16a34a' : '#15803d',
        escala: 0.8 + Math.random() * 0.4
      });
    }

    // Flores por aprendizajes
    const nFlores = Math.min(this.stats.aprendizajes, 12);
    for (let i = 0; i < nFlores; i++) {
      const angle = (i / nFlores) * Math.PI * 1.8 + 0.5;
      const r = 50 + (i % 2) * 30;
      items.push({
        tipo: 'flor',
        x: cx + Math.cos(angle) * r * 0.9,
        y: topY + this.copaRy() * 0.3 + Math.sin(angle) * r * 0.5,
        color: i % 2 === 0 ? '#f472b6' : '#fb7185',
        escala: 0.9 + (i % 3) * 0.1
      });
    }

    // Frutos por evidencias
    const nFrutos = Math.min(Math.floor(this.stats.evidencias / 4), 10);
    for (let i = 0; i < nFrutos; i++) {
      const angle = (i / nFrutos) * Math.PI * 1.6 + 1;
      const r = 55 + (i % 3) * 20;
      items.push({
        tipo: 'fruto',
        x: cx + Math.cos(angle) * r,
        y: topY + this.copaRy() * 0.5 + Math.sin(angle) * r * 0.6,
        color: i % 2 === 0 ? '#f97316' : '#ef4444',
        escala: 0.9
      });
    }

    // Cápsulas (mini documentales)
    const nCaps = Math.min(this.stats.capsulas, 5);
    for (let i = 0; i < nCaps; i++) {
      const angle = (i / Math.max(nCaps, 1)) * Math.PI * 1.4 + 0.8;
      const r = 70 + i * 20;
      items.push({
        tipo: 'capsula',
        x: cx + Math.cos(angle) * r * 0.85,
        y: topY + this.copaRy() * 0.6 + Math.sin(angle) * r * 0.5,
        color: '#7c3aed',
        escala: 1
      });
    }

    return items;
  });

  ngOnInit(): void {}

  irAMisiones(): void {
    this.router.navigate(['/plans']);
  }
}

import { inject as injectCore } from '@angular/core';
