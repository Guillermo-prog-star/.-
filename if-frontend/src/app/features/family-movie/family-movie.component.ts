import { Component, OnInit, OnDestroy, AfterViewInit, inject, signal, computed, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FamilyMovieService, FamilyMovieDto } from '../../core/services/family-movie.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-family-movie',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fm-page">

      <!-- ═══ CARGANDO ═══ -->
      @if (loading()) {
        <div class="fm-loading">
          <div class="film-reel">
            <div class="reel-circle">
              <div class="reel-spoke"></div><div class="reel-spoke"></div>
              <div class="reel-spoke"></div><div class="reel-spoke"></div>
            </div>
          </div>
          <p class="loading-text">Cargando la historia de tu familia...</p>
        </div>
      }

      <!-- ═══ SIN PELÍCULA ═══ -->
      @if (!loading() && !movie() && !generating()) {
        <div class="fm-empty">
          <div class="empty-cinema">
            <div class="cinema-screen">
              <div class="screen-scanline"></div>
              <div class="screen-text">🎞️</div>
            </div>
            <div class="cinema-seats">
              <div class="seat"></div><div class="seat"></div>
              <div class="seat"></div><div class="seat"></div>
            </div>
          </div>
          <h2 class="empty-title">Tu primera Película está por nacer</h2>
          <p class="empty-sub">La IA analizará evidencias, gratitudes, misiones y rituales del trimestre para crear la historia de tu familia.</p>
          <button class="btn-generate" (click)="generate()">
            <span class="btn-icon">🎬</span> Crear Película del Trimestre
          </button>
        </div>
      }

      <!-- ═══ GENERANDO ═══ -->
      @if (generating()) {
        <div class="fm-generating">
          <div class="gen-projector">
            <div class="projector-body">📽️</div>
            <div class="projector-beam"></div>
          </div>
          <div class="gen-title">La IA está componiendo tu historia...</div>
          <div class="gen-steps">
            <div class="gs-step" [class.active]="genStep() >= 1" [class.done]="genStep() > 1">
              <span class="gs-dot"></span> Analizando evidencias y gratitudes
            </div>
            <div class="gs-step" [class.active]="genStep() >= 2" [class.done]="genStep() > 2">
              <span class="gs-dot"></span> Identificando momentos clave
            </div>
            <div class="gs-step" [class.active]="genStep() >= 3" [class.done]="genStep() > 3">
              <span class="gs-dot"></span> Componiendo la narrativa
            </div>
            <div class="gs-step" [class.active]="genStep() >= 4">
              <span class="gs-dot"></span> Escribiendo la carta del Mentor
            </div>
          </div>
        </div>
      }

      <!-- ═══ PELÍCULA DISPONIBLE ═══ -->
      @if (movie() && !generating()) {
        <div class="fm-movie" #movieContainer>

          <!-- PÓSTER ANIMADO -->
          <div class="fm-poster" [class.visible]="posterVisible()">
            <div class="poster-bg">
              <div class="poster-particle p1"></div>
              <div class="poster-particle p2"></div>
              <div class="poster-particle p3"></div>
              <div class="poster-particle p4"></div>
              <div class="poster-particle p5"></div>
              <div class="poster-particle p6"></div>
              <div class="poster-rays"></div>
            </div>
            <div class="poster-content">
              <div class="poster-period" [class.animate-in]="posterVisible()">{{ movie()!.periodLabel }}</div>
              <div class="poster-family" [class.animate-in]="posterVisible()">{{ movie()!.familyName }}</div>
              @if (movie()!.openingLine) {
                <div class="poster-opening" [class.animate-in]="posterVisible()">{{ movie()!.openingLine }}</div>
              }
              <div class="poster-clapperboard">
                <div class="clapper-top"></div>
                <div class="clapper-body">🎬</div>
              </div>
            </div>
          </div>

          <!-- TIRA DE FILM separadora -->
          <div class="film-strip-divider">
            <div class="fsd-track">
              @for (f of filmFrames; track f) {
                <div class="fsd-frame"></div>
              }
            </div>
          </div>

          <!-- ESTADÍSTICAS — contadores animados -->
          <div class="stats-section" #statsSection>
            <div class="stats-grid" [class.visible]="statsVisible()">
              <div class="stat-card" style="--delay:0ms">
                <div class="stat-num">{{ displayCounts().evidences }}</div>
                <div class="stat-label">📸 Evidencias</div>
              </div>
              <div class="stat-card" style="--delay:80ms">
                <div class="stat-num">{{ displayCounts().gratitudes }}</div>
                <div class="stat-label">💖 Gratitudes</div>
              </div>
              <div class="stat-card" style="--delay:160ms">
                <div class="stat-num">{{ displayCounts().missions }}</div>
                <div class="stat-label">🏆 Misiones</div>
              </div>
              <div class="stat-card" style="--delay:240ms">
                <div class="stat-num">{{ displayCounts().days }}</div>
                <div class="stat-label">⚡ Días activos</div>
              </div>
              <div class="stat-card" style="--delay:320ms">
                <div class="stat-num">{{ displayCounts().streak }}</div>
                <div class="stat-label">🔥 Mejor racha</div>
              </div>
              <div class="stat-card" style="--delay:400ms">
                <div class="stat-num">{{ displayCounts().rituals }}</div>
                <div class="stat-label">🕯️ Rituales</div>
              </div>
            </div>
          </div>

          @if (movie()!.icfDelta != null) {
            <div class="icf-bar" [class.positive]="movie()!.icfDelta! > 0" [class.negative]="movie()!.icfDelta! < 0">
              <span class="icf-icon">{{ movie()!.icfDelta! > 0 ? '📈' : movie()!.icfDelta! < 0 ? '📉' : '➡️' }}</span>
              <span>ICF {{ movie()!.icfDelta! > 0 ? 'subió' : movie()!.icfDelta! < 0 ? 'bajó' : 'se mantuvo' }}
                {{ movie()!.icfDelta! > 0 ? '+' : '' }}{{ movie()!.icfDelta!.toFixed(1) }} puntos este período</span>
            </div>
          }

          <!-- CAPÍTULOS — reveal al hacer scroll -->
          @if (movie()!.chapter1) {
            <div class="chapter reveal-chapter" #ch1>
              <div class="ch-badge">CAPÍTULO I</div>
              <div class="ch-title">Los momentos que los conectaron</div>
              <p class="ch-text">{{ movie()!.chapter1 }}</p>
            </div>
          }

          @if (movie()!.chapter2) {
            <div class="chapter reveal-chapter" #ch2>
              <div class="ch-badge">CAPÍTULO II</div>
              <div class="ch-title">Los desafíos que enfrentaron</div>
              <p class="ch-text">{{ movie()!.chapter2 }}</p>
            </div>
          }

          @if (movie()!.chapter3) {
            <div class="chapter reveal-chapter" #ch3>
              <div class="ch-badge">CAPÍTULO III</div>
              <div class="ch-title">Lo que construyeron juntos</div>
              <p class="ch-text">{{ movie()!.chapter3 }}</p>
            </div>
          }

          <!-- CITA DESTACADA -->
          @if (movie()!.highlightQuote) {
            <div class="highlight-quote reveal-chapter" #hq>
              <div class="hq-marks">"</div>
              <div class="hq-text">{{ movie()!.highlightQuote }}</div>
              <div class="hq-line"></div>
            </div>
          }

          <!-- TIRA DE FILM separadora -->
          <div class="film-strip-divider film-strip-divider--bottom">
            <div class="fsd-track fsd-track--reverse">
              @for (f of filmFrames; track f) {
                <div class="fsd-frame"></div>
              }
            </div>
          </div>

          <!-- CARTA DEL MENTOR — typewriter -->
          @if (movie()!.mentorLetter) {
            <div class="mentor-letter reveal-chapter" #mentorEl>
              <div class="ml-header">
                <div class="ml-wax-seal">✉️</div>
                <div>
                  <div class="ml-label">CARTA DEL MENTOR</div>
                  <div class="ml-family">para la Familia {{ movie()!.familyName }}</div>
                </div>
              </div>
              <p class="ml-text">{{ typewriterText() }}<span class="cursor" [class.blink]="typewriterDone()">|</span></p>
              <div class="ml-sign">— El Mentor de Integridad</div>
            </div>
          }

          <!-- FOOTER -->
          <div class="fm-footer">
            <div class="fm-meta">Generada el {{ formatDate(movie()!.generatedAt) }}</div>
            <div class="fm-actions">
              @if (allMovies().length > 1) {
                <button class="btn-history" (click)="showHistory.set(!showHistory())">
                  📚 {{ showHistory() ? 'Ocultar' : 'Ver' }} historial ({{ allMovies().length }})
                </button>
              }
              <button class="btn-new-movie" (click)="generate()">🔄 Nueva película</button>
            </div>
          </div>

          @if (showHistory() && allMovies().length > 1) {
            <div class="history-list">
              @for (m of allMovies(); track m.id) {
                @if (m.id !== movie()!.id) {
                  <div class="history-item" (click)="selectMovie(m)">
                    <span class="hi-icon">🎬</span>
                    <div class="hi-info">
                      <div class="hi-period">{{ m.periodLabel }}</div>
                      <div class="hi-stats">{{ m.evidencesCount }} evidencias · {{ m.gratitudesCount }} gratitudes · {{ m.missionsCompleted }} misiones</div>
                    </div>
                    <span class="hi-arrow">→</span>
                  </div>
                }
              }
            </div>
          }

        </div>
      }

      @if (error()) {
        <div class="fm-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    :host { display: block; }

    .fm-page {
      max-width: 780px; margin: 0 auto;
      padding: 24px 20px 80px;
      color: #e0e0e0;
    }

    /* ── CARGANDO ── */
    .fm-loading {
      display: flex; flex-direction: column; align-items: center;
      gap: 24px; padding: 80px 20px; color: #888;
    }
    .film-reel {
      width: 64px; height: 64px;
      animation: reel-spin 1.2s linear infinite;
    }
    .reel-circle {
      width: 64px; height: 64px; border-radius: 50%;
      border: 3px solid rgba(124,58,237,0.6);
      position: relative; display: flex; align-items: center; justify-content: center;
    }
    .reel-circle::before {
      content: ''; position: absolute;
      width: 20px; height: 20px; border-radius: 50%;
      background: rgba(124,58,237,0.4); border: 2px solid rgba(124,58,237,0.6);
    }
    .reel-spoke {
      position: absolute; width: 3px; height: 22px;
      background: rgba(124,58,237,0.5); border-radius: 2px;
      top: 50%; left: 50%; transform-origin: bottom center;
    }
    .reel-spoke:nth-child(1) { transform: translateX(-50%) translateY(-100%) rotate(0deg); }
    .reel-spoke:nth-child(2) { transform: translateX(-50%) translateY(-100%) rotate(90deg); }
    .reel-spoke:nth-child(3) { transform: translateX(-50%) translateY(-100%) rotate(180deg); }
    .reel-spoke:nth-child(4) { transform: translateX(-50%) translateY(-100%) rotate(270deg); }
    @keyframes reel-spin { to { transform: rotate(360deg); } }
    .loading-text { font-size: 14px; letter-spacing: 0.04em; }

    /* ── VACÍO ── */
    .fm-empty { text-align: center; padding: 60px 20px; }
    .empty-cinema { margin: 0 auto 28px; width: 160px; }
    .cinema-screen {
      width: 140px; height: 90px; margin: 0 auto;
      background: #111; border: 2px solid #333; border-radius: 6px;
      position: relative; overflow: hidden; display: flex;
      align-items: center; justify-content: center;
    }
    .screen-scanline {
      position: absolute; inset: 0;
      background: repeating-linear-gradient(0deg, transparent, transparent 3px, rgba(255,255,255,0.02) 3px, rgba(255,255,255,0.02) 4px);
      animation: scanline 4s linear infinite;
    }
    @keyframes scanline { from { background-position: 0 0; } to { background-position: 0 100px; } }
    .screen-text { font-size: 36px; position: relative; z-index: 1; }
    .cinema-seats { display: flex; justify-content: center; gap: 8px; margin-top: 10px; }
    .seat {
      width: 24px; height: 18px;
      background: #2a2a3a; border-radius: 4px 4px 0 0;
      border: 1px solid #444;
    }
    .empty-title { font-size: 20px; font-weight: 800; margin: 0 0 10px; }
    .empty-sub { font-size: 14px; color: #888; max-width: 400px; margin: 0 auto 28px; line-height: 1.6; }

    .btn-generate {
      display: inline-flex; align-items: center; gap: 8px;
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: white;
      padding: 14px 32px; border-radius: 12px;
      font-size: 15px; font-weight: 800; cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 4px 24px rgba(124,58,237,0.35);
    }
    .btn-generate:hover { transform: translateY(-2px); box-shadow: 0 8px 32px rgba(124,58,237,0.45); }
    .btn-icon { font-size: 18px; }

    /* ── GENERANDO ── */
    .fm-generating { text-align: center; padding: 64px 20px; }
    .gen-projector { position: relative; display: inline-block; margin-bottom: 20px; }
    .projector-body { font-size: 60px; animation: proj-shake 0.5s ease-in-out infinite; display: block; }
    @keyframes proj-shake {
      0%,100% { transform: rotate(0deg); }
      25% { transform: rotate(-1deg) scale(1.02); }
      75% { transform: rotate(1deg) scale(1.02); }
    }
    .projector-beam {
      position: absolute; bottom: -4px; right: -20px;
      width: 80px; height: 20px;
      background: linear-gradient(90deg, rgba(255,220,100,0.6), transparent);
      clip-path: polygon(0 40%, 100% 0, 100% 100%, 0 60%);
      animation: beam-flicker 0.8s ease-in-out infinite;
    }
    @keyframes beam-flicker { 0%,100%{opacity:0.7} 50%{opacity:1} }
    .gen-title { font-size: 18px; font-weight: 700; color: #a78bfa; margin-bottom: 24px; }
    .gen-steps { display: flex; flex-direction: column; gap: 10px; max-width: 340px; margin: 0 auto; }
    .gs-step {
      display: flex; align-items: center; gap: 10px;
      font-size: 13px; color: #555; padding: 10px 14px;
      border-radius: 10px; background: rgba(255,255,255,0.02);
      transition: all 0.5s ease;
    }
    .gs-step.active { color: #c4b5fd; background: rgba(139,92,246,0.1); border: 1px solid rgba(139,92,246,0.2); }
    .gs-step.done   { color: #86efac; background: rgba(34,197,94,0.06); border: 1px solid rgba(34,197,94,0.15); }
    .gs-dot {
      width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
      background: currentColor; opacity: 0.5;
      transition: all 0.4s;
    }
    .gs-step.active .gs-dot { opacity: 1; animation: dot-pulse 0.8s ease-in-out infinite; }
    .gs-step.done   .gs-dot { opacity: 1; animation: none; }
    @keyframes dot-pulse { 0%,100%{transform:scale(1)} 50%{transform:scale(1.4)} }

    /* ── PÓSTER ── */
    .fm-poster {
      position: relative; border-radius: 24px; overflow: hidden;
      height: 280px; margin-bottom: 0;
      opacity: 0; transform: scale(0.96);
      transition: opacity 0.8s ease, transform 0.8s ease;
    }
    .fm-poster.visible { opacity: 1; transform: scale(1); }

    .poster-bg {
      position: absolute; inset: 0;
      background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
      background-size: 300% 300%;
      animation: poster-gradient 8s ease infinite;
    }
    @keyframes poster-gradient {
      0%   { background-position: 0% 50%; }
      50%  { background-position: 100% 50%; }
      100% { background-position: 0% 50%; }
    }

    .poster-particle {
      position: absolute; border-radius: 50%;
      background: rgba(196,181,253,0.6);
      animation: float-particle linear infinite;
    }
    .p1 { width:3px;height:3px; left:10%; top:20%; animation-duration:7s; animation-delay:0s; }
    .p2 { width:2px;height:2px; left:25%; top:70%; animation-duration:9s; animation-delay:1s; }
    .p3 { width:4px;height:4px; left:60%; top:15%; animation-duration:11s; animation-delay:2s; }
    .p4 { width:2px;height:2px; left:75%; top:80%; animation-duration:8s; animation-delay:0.5s; }
    .p5 { width:3px;height:3px; left:85%; top:40%; animation-duration:10s; animation-delay:3s; }
    .p6 { width:2px;height:2px; left:45%; top:55%; animation-duration:6s; animation-delay:1.5s; }
    @keyframes float-particle {
      0%   { transform: translateY(0) translateX(0) scale(1); opacity: 0.8; }
      33%  { transform: translateY(-18px) translateX(8px) scale(1.3); opacity: 1; }
      66%  { transform: translateY(-8px) translateX(-10px) scale(0.8); opacity: 0.6; }
      100% { transform: translateY(0) translateX(0) scale(1); opacity: 0.8; }
    }

    .poster-rays {
      position: absolute; inset: 0;
      background: conic-gradient(from 0deg at 50% 50%,
        transparent 0deg, rgba(124,58,237,0.06) 30deg,
        transparent 60deg, rgba(79,70,229,0.06) 90deg,
        transparent 120deg, rgba(124,58,237,0.06) 150deg,
        transparent 180deg, rgba(79,70,229,0.06) 210deg,
        transparent 240deg, rgba(124,58,237,0.06) 270deg,
        transparent 300deg, rgba(79,70,229,0.06) 330deg, transparent 360deg);
      animation: rays-rotate 30s linear infinite;
    }
    @keyframes rays-rotate { to { transform: rotate(360deg); } }

    .poster-content {
      position: relative; z-index: 2;
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      height: 100%; padding: 28px; text-align: center; gap: 8px;
    }

    .poster-period {
      font-size: 11px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.2em; color: #a78bfa;
      opacity: 0; transform: translateY(12px);
      transition: opacity 0.6s ease 0.3s, transform 0.6s ease 0.3s;
    }
    .poster-period.animate-in { opacity: 1; transform: translateY(0); }

    .poster-family {
      font-size: 34px; font-weight: 900; color: white;
      letter-spacing: -0.02em; line-height: 1.1;
      text-shadow: 0 2px 20px rgba(124,58,237,0.5);
      opacity: 0; transform: translateY(16px) scale(0.95);
      transition: opacity 0.7s ease 0.5s, transform 0.7s ease 0.5s;
    }
    .poster-family.animate-in { opacity: 1; transform: translateY(0) scale(1); }

    .poster-opening {
      font-size: 14px; color: rgba(255,255,255,0.65);
      font-style: italic; max-width: 460px; line-height: 1.5;
      opacity: 0; transform: translateY(10px);
      transition: opacity 0.6s ease 0.8s, transform 0.6s ease 0.8s;
    }
    .poster-opening.animate-in { opacity: 1; transform: translateY(0); }

    .poster-clapperboard {
      position: absolute; top: 16px; right: 20px;
      opacity: 0.3; animation: clapper-tap 4s ease-in-out infinite;
    }
    .clapper-top {
      width: 36px; height: 8px; background: repeating-linear-gradient(
        90deg, #fff 0, #fff 4px, #333 4px, #333 8px);
      border-radius: 2px 2px 0 0; transform-origin: left center;
    }
    .clapper-body { font-size: 22px; margin-top: -2px; }
    @keyframes clapper-tap {
      0%,85%,100% { transform: rotate(0deg); }
      90% { transform: rotate(-15deg); }
      95% { transform: rotate(0deg); }
    }

    /* ── TIRA DE FILM ── */
    .film-strip-divider {
      overflow: hidden; margin: 20px -20px;
      height: 40px; position: relative;
    }
    .fsd-track {
      display: flex; gap: 4px;
      animation: strip-scroll 8s linear infinite;
      width: max-content;
    }
    .fsd-track--reverse { animation-direction: reverse; }
    .fsd-frame {
      width: 30px; height: 40px; flex-shrink: 0;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 2px;
      position: relative;
    }
    .fsd-frame::before, .fsd-frame::after {
      content: ''; position: absolute;
      left: 50%; transform: translateX(-50%);
      width: 6px; height: 6px; border-radius: 50%;
      background: rgba(255,255,255,0.12);
      border: 1px solid rgba(255,255,255,0.08);
    }
    .fsd-frame::before { top: 4px; }
    .fsd-frame::after  { bottom: 4px; }
    @keyframes strip-scroll { from { transform: translateX(0); } to { transform: translateX(-50%); } }

    /* ── ESTADÍSTICAS ── */
    .stats-section { margin-bottom: 20px; }
    .stats-grid {
      display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;
    }
    @media (max-width: 480px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }

    .stat-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 16px; padding: 20px 12px;
      text-align: center;
      opacity: 0; transform: translateY(20px) scale(0.95);
      transition: opacity 0.5s ease var(--delay, 0ms),
                  transform 0.5s ease var(--delay, 0ms),
                  background 0.2s, border-color 0.2s;
    }
    .stats-grid.visible .stat-card {
      opacity: 1; transform: translateY(0) scale(1);
    }
    .stat-card:hover {
      background: rgba(124,58,237,0.08);
      border-color: rgba(124,58,237,0.2);
    }
    .stat-num {
      font-size: 36px; font-weight: 900;
      font-variant-numeric: tabular-nums;
      color: #c4b5fd;
      line-height: 1;
    }
    .stat-label { font-size: 12px; color: #777; margin-top: 6px; }

    /* ── ICF BAR ── */
    .icf-bar {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 18px; border-radius: 12px; margin-bottom: 20px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      font-size: 14px; font-weight: 600;
    }
    .icf-bar.positive { background: rgba(34,197,94,0.08); border-color: rgba(34,197,94,0.2); color: #86efac; }
    .icf-bar.negative { background: rgba(239,68,68,0.08); border-color: rgba(239,68,68,0.2); color: #fca5a5; }
    .icf-icon { font-size: 20px; }

    /* ── CAPÍTULOS ── */
    .chapter {
      background: rgba(255,255,255,0.025);
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 3px solid transparent;
      border-radius: 16px; padding: 22px 24px;
      margin-bottom: 14px; position: relative; overflow: hidden;
    }
    .chapter::before {
      content: ''; position: absolute; inset: 0;
      background: linear-gradient(90deg, rgba(124,58,237,0.04), transparent);
      opacity: 0; transition: opacity 0.4s;
    }
    .chapter:hover::before { opacity: 1; }
    .chapter:hover { border-left-color: #7c3aed; }

    .ch-badge {
      font-size: 9px; font-weight: 800; text-transform: uppercase;
      letter-spacing: 0.15em; color: #7c3aed;
      background: rgba(124,58,237,0.1); border: 1px solid rgba(124,58,237,0.2);
      display: inline-block; padding: 3px 10px; border-radius: 99px;
      margin-bottom: 10px;
    }
    .ch-title { font-size: 17px; font-weight: 800; margin-bottom: 12px; color: #e0e0e0; }
    .ch-text  { font-size: 14px; line-height: 1.8; color: #aaa; margin: 0; }

    .reveal-chapter {
      opacity: 0; transform: translateX(-24px);
      transition: opacity 0.7s ease, transform 0.7s ease;
    }
    .reveal-chapter.visible {
      opacity: 1; transform: translateX(0);
    }

    /* ── CITA DESTACADA ── */
    .highlight-quote {
      padding: 32px 24px; margin: 8px 0; text-align: center;
      position: relative;
    }
    .hq-marks {
      font-size: 80px; line-height: 0.4; color: rgba(124,58,237,0.25);
      font-family: Georgia, serif; margin-bottom: 16px;
      display: block;
    }
    .hq-text {
      font-size: 19px; font-weight: 700; font-style: italic;
      color: #c4b5fd; line-height: 1.6; max-width: 520px; margin: 0 auto 16px;
    }
    .hq-line {
      width: 40px; height: 2px; background: rgba(124,58,237,0.4);
      margin: 0 auto; border-radius: 2px;
    }

    /* ── CARTA DEL MENTOR ── */
    .mentor-letter {
      background: linear-gradient(135deg, rgba(124,58,237,0.07), rgba(79,70,229,0.05));
      border: 1px solid rgba(124,58,237,0.18);
      border-radius: 20px; padding: 28px;
      margin-top: 8px; position: relative; overflow: hidden;
    }
    .mentor-letter::before {
      content: ''; position: absolute;
      top: -30px; right: -30px;
      width: 140px; height: 140px; border-radius: 50%;
      background: rgba(124,58,237,0.05);
    }
    .ml-header { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 18px; }
    .ml-wax-seal {
      font-size: 32px; flex-shrink: 0;
      animation: wax-pulse 3s ease-in-out infinite;
    }
    @keyframes wax-pulse { 0%,100%{transform:scale(1) rotate(0deg)} 50%{transform:scale(1.05) rotate(2deg)} }
    .ml-label  { font-size: 10px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; color: #7c3aed; }
    .ml-family { font-size: 14px; font-weight: 700; color: #a78bfa; }
    .ml-text   {
      font-size: 15px; line-height: 1.85; color: #ccc;
      font-style: italic; margin: 0 0 16px; position: relative; z-index: 1;
      min-height: 1em;
    }
    .cursor { color: #a78bfa; font-style: normal; }
    .cursor.blink { animation: blink 1s step-end infinite; }
    @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
    .ml-sign   { font-size: 13px; color: #666; text-align: right; font-style: normal; }

    /* ── FOOTER ── */
    .fm-footer {
      display: flex; justify-content: space-between; align-items: center;
      margin-top: 28px; flex-wrap: wrap; gap: 10px;
    }
    .fm-meta   { font-size: 11px; color: #555; }
    .fm-actions { display: flex; gap: 8px; }
    .btn-history, .btn-new-movie {
      padding: 8px 16px; border-radius: 9px; font-size: 12px; font-weight: 600;
      cursor: pointer; transition: all 0.18s;
    }
    .btn-history   { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #999; }
    .btn-new-movie { background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.3); color: #c4b5fd; }
    .btn-history:hover   { background: rgba(255,255,255,0.1); }
    .btn-new-movie:hover { background: rgba(124,58,237,0.2); }

    /* ── HISTORIAL ── */
    .history-list { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
    .history-item {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 16px;
      background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07);
      border-radius: 10px; cursor: pointer; transition: all 0.18s;
    }
    .history-item:hover { background: rgba(255,255,255,0.07); }
    .hi-icon   { font-size: 20px; flex-shrink: 0; }
    .hi-info   { flex: 1; }
    .hi-period { font-size: 13px; font-weight: 700; }
    .hi-stats  { font-size: 11px; color: #777; margin-top: 2px; }
    .hi-arrow  { color: #666; }

    .fm-error {
      background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5;
    }
  `]
})
export class FamilyMovieComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly movieSvc    = inject(FamilyMovieService);
  private readonly familyState = inject(FamilyStateService);
  private readonly route       = inject(ActivatedRoute);
  private readonly el          = inject(ElementRef);
  private readonly zone        = inject(NgZone);

  readonly filmFrames = Array(30).fill(0);

  readonly movie       = signal<FamilyMovieDto | null>(null);
  readonly allMovies   = signal<FamilyMovieDto[]>([]);
  readonly loading     = signal(false);
  readonly generating  = signal(false);
  readonly error       = signal<string | null>(null);
  readonly showHistory = signal(false);
  readonly genStep     = signal(0);

  // Animación póster
  readonly posterVisible = signal(false);
  // Animación stats (contadores)
  readonly statsVisible  = signal(false);
  readonly displayCounts = signal({ evidences:0, gratitudes:0, missions:0, days:0, streak:0, rituals:0 });
  // Typewriter carta del mentor
  readonly typewriterText = signal('');
  readonly typewriterDone = signal(false);

  private observers: IntersectionObserver[] = [];
  private countInterval: ReturnType<typeof setInterval> | null = null;
  private typewriterInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    const id = this.familyState.currentFamilyId();
    if (!id) return;
    const autoGenerate = this.route.snapshot.queryParamMap.get('autoGenerate') === '1';
    if (autoGenerate) { this.generate(); } else { this.load(id); }
  }

  ngAfterViewInit(): void {
    // El póster entra inmediatamente si ya hay película
    if (this.movie()) { setTimeout(() => this.triggerAnimations(), 100); }
  }

  ngOnDestroy(): void {
    this.observers.forEach(o => o.disconnect());
    if (this.countInterval) clearInterval(this.countInterval);
    if (this.typewriterInterval) clearInterval(this.typewriterInterval);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.movieSvc.list(id).pipe(
      catchError(() => { this.loading.set(false); return of([]); })
    ).subscribe(movies => {
      this.allMovies.set(movies);
      if (movies.length) this.movie.set(movies[0]);
      this.loading.set(false);
      setTimeout(() => this.triggerAnimations(), 150);
    });
  }

  generate(): void {
    const id = this.familyState.currentFamilyId();
    if (!id) return;
    this.generating.set(true);
    this.error.set(null);
    this.genStep.set(0);
    // Reset animaciones
    this.posterVisible.set(false);
    this.statsVisible.set(false);
    this.displayCounts.set({ evidences:0, gratitudes:0, missions:0, days:0, streak:0, rituals:0 });
    this.typewriterText.set('');
    this.typewriterDone.set(false);

    const stepInterval = setInterval(() => {
      this.genStep.update(s => Math.min(s + 1, 4));
    }, 2500);

    this.movieSvc.generateQuarter(id).pipe(
      catchError(() => {
        this.error.set('No se pudo generar la película. Verifica que la familia tenga actividad registrada.');
        this.generating.set(false);
        clearInterval(stepInterval);
        return of(null);
      })
    ).subscribe(m => {
      clearInterval(stepInterval);
      if (m) {
        this.movie.set(m);
        this.allMovies.update(list => [m, ...list.filter(x => x.id !== m.id)]);
      }
      this.generating.set(false);
      setTimeout(() => this.triggerAnimations(), 200);
    });
  }

  private triggerAnimations(): void {
    // 1. Póster
    this.posterVisible.set(true);

    // 2. Stats: IntersectionObserver para contadores
    const statsEl = this.el.nativeElement.querySelector('.stats-section');
    if (statsEl) {
      const obs = new IntersectionObserver(entries => {
        if (entries[0].isIntersecting) {
          this.statsVisible.set(true);
          this.animateCounts();
          obs.disconnect();
        }
      }, { threshold: 0.3 });
      obs.observe(statsEl);
      this.observers.push(obs);
    } else {
      // Si no hay IntersectionObserver, activar directamente
      this.statsVisible.set(true);
      this.animateCounts();
    }

    // 3. Capítulos y cita: reveal al scroll
    setTimeout(() => {
      const revealEls = this.el.nativeElement.querySelectorAll('.reveal-chapter');
      revealEls.forEach((el: Element) => {
        const obs = new IntersectionObserver(entries => {
          if (entries[0].isIntersecting) {
            el.classList.add('visible');
            obs.disconnect();
          }
        }, { threshold: 0.15 });
        obs.observe(el);
        this.observers.push(obs);
      });
    }, 100);

    // 4. Typewriter carta del mentor
    const mentorEl = this.el.nativeElement.querySelector('.mentor-letter');
    if (mentorEl) {
      const mentorObs = new IntersectionObserver(entries => {
        if (entries[0].isIntersecting) {
          this.startTypewriter();
          mentorObs.disconnect();
        }
      }, { threshold: 0.2 });
      mentorObs.observe(mentorEl);
      this.observers.push(mentorObs);
    }
  }

  private animateCounts(): void {
    const m = this.movie();
    if (!m) return;
    const targets = {
      evidences: m.evidencesCount, gratitudes: m.gratitudesCount,
      missions: m.missionsCompleted, days: m.daysActive,
      streak: m.bestStreak, rituals: m.ritualsCompleted
    };
    const duration = 1600;
    const steps = 60;
    let step = 0;

    if (this.countInterval) clearInterval(this.countInterval);
    this.countInterval = setInterval(() => {
      step++;
      const t = Math.min(step / steps, 1);
      // ease-out cubic
      const ease = 1 - Math.pow(1 - t, 3);
      this.zone.run(() => {
        this.displayCounts.set({
          evidences:  Math.round(targets.evidences  * ease),
          gratitudes: Math.round(targets.gratitudes * ease),
          missions:   Math.round(targets.missions   * ease),
          days:       Math.round(targets.days       * ease),
          streak:     Math.round(targets.streak     * ease),
          rituals:    Math.round(targets.rituals    * ease),
        });
      });
      if (step >= steps) clearInterval(this.countInterval!);
    }, duration / steps);
  }

  private startTypewriter(): void {
    const full = this.movie()?.mentorLetter ?? '';
    if (!full) return;
    let i = 0;
    const speed = 22; // ms por carácter

    if (this.typewriterInterval) clearInterval(this.typewriterInterval);
    this.typewriterInterval = setInterval(() => {
      i++;
      this.zone.run(() => {
        this.typewriterText.set(full.substring(0, i));
        if (i >= full.length) {
          this.typewriterDone.set(true);
          clearInterval(this.typewriterInterval!);
        }
      });
    }, speed);
  }

  selectMovie(m: FamilyMovieDto): void {
    this.movie.set(m);
    this.showHistory.set(false);
    // Reset y re-animar
    this.posterVisible.set(false);
    this.statsVisible.set(false);
    this.typewriterText.set('');
    this.typewriterDone.set(false);
    this.displayCounts.set({ evidences:0, gratitudes:0, missions:0, days:0, streak:0, rituals:0 });
    this.observers.forEach(o => o.disconnect());
    this.observers = [];
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => this.triggerAnimations(), 300);
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('es-CO', {
        day: '2-digit', month: 'long', year: 'numeric'
      });
    } catch { return iso; }
  }
}
