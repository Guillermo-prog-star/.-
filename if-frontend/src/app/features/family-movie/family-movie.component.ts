import { Component, OnInit, inject, signal } from '@angular/core';
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

      <!-- Header cinematográfico -->
      <div class="fm-header">
        <div class="fm-film">🎬</div>
        <div>
          <h1 class="fm-title">Película Familiar</h1>
          <p class="fm-sub">El resumen de tu historia — generado por la IA a partir de todo lo que han vivido</p>
        </div>
      </div>

      <!-- Cargando -->
      @if (loading()) {
        <div class="fm-loading">
          <div class="film-strip">
            <div class="fs-frame"></div>
            <div class="fs-frame"></div>
            <div class="fs-frame"></div>
          </div>
          <p>Cargando la historia de tu familia...</p>
        </div>
      }

      <!-- Sin película aún -->
      @if (!loading() && !movie() && !generating()) {
        <div class="fm-empty">
          <div class="ei">🎞️</div>
          <h2>Tu primera Película está por crearse</h2>
          <p>La IA analizará todas las evidencias, gratitudes, misiones y rituales del trimestre actual para crear la historia de tu familia.</p>
          <button class="btn-generate" (click)="generate()">
            🎬 Crear Película del Trimestre
          </button>
        </div>
      }

      <!-- Generando -->
      @if (generating()) {
        <div class="fm-generating">
          <div class="gen-projector">📽️</div>
          <div class="gen-title">La IA está componiendo tu historia...</div>
          <div class="gen-steps">
            <div class="gs-step" [class.active]="genStep() >= 1">Analizando evidencias y gratitudes</div>
            <div class="gs-step" [class.active]="genStep() >= 2">Identificando momentos clave</div>
            <div class="gs-step" [class.active]="genStep() >= 3">Componiendo la narrativa</div>
            <div class="gs-step" [class.active]="genStep() >= 4">Escribiendo la carta del Mentor</div>
          </div>
        </div>
      }

      <!-- Película disponible -->
      @if (movie() && !generating()) {
        <div class="fm-movie">

          <!-- Póster de apertura -->
          <div class="fm-poster">
            <div class="poster-bg"></div>
            <div class="poster-content">
              <div class="poster-period">{{ movie()!.periodLabel }}</div>
              <div class="poster-family">{{ movie()!.familyName }}</div>
              @if (movie()!.openingLine) {
                <div class="poster-opening">{{ movie()!.openingLine }}</div>
              }
            </div>
          </div>

          <!-- Estadísticas tipo Wrapped -->
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.evidencesCount }}</div>
              <div class="stat-label">📸 Evidencias</div>
            </div>
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.gratitudesCount }}</div>
              <div class="stat-label">💖 Gratitudes</div>
            </div>
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.missionsCompleted }}</div>
              <div class="stat-label">🏆 Misiones</div>
            </div>
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.daysActive }}</div>
              <div class="stat-label">⚡ Días activos</div>
            </div>
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.bestStreak }}</div>
              <div class="stat-label">🔥 Mejor racha</div>
            </div>
            <div class="stat-card">
              <div class="stat-num">{{ movie()!.ritualsCompleted }}</div>
              <div class="stat-label">🕯️ Rituales</div>
            </div>
          </div>

          @if (movie()!.icfDelta != null) {
            <div class="icf-bar" [class.positive]="movie()!.icfDelta! > 0" [class.negative]="movie()!.icfDelta! < 0">
              <span class="icf-icon">{{ movie()!.icfDelta! > 0 ? '📈' : movie()!.icfDelta! < 0 ? '📉' : '➡️' }}</span>
              <span>ICF {{ movie()!.icfDelta! > 0 ? 'subió' : movie()!.icfDelta! < 0 ? 'bajó' : 'se mantuvo' }}
                {{ movie()!.icfDelta! > 0 ? '+' : '' }}{{ movie()!.icfDelta!.toFixed(1) }} puntos este período</span>
            </div>
          }

          <!-- Capítulos narrativos -->
          @if (movie()!.chapter1) {
            <div class="chapter">
              <div class="ch-number">Capítulo I</div>
              <div class="ch-title">Los momentos que los conectaron</div>
              <p class="ch-text">{{ movie()!.chapter1 }}</p>
            </div>
          }

          @if (movie()!.chapter2) {
            <div class="chapter">
              <div class="ch-number">Capítulo II</div>
              <div class="ch-title">Los desafíos que enfrentaron</div>
              <p class="ch-text">{{ movie()!.chapter2 }}</p>
            </div>
          }

          @if (movie()!.chapter3) {
            <div class="chapter">
              <div class="ch-number">Capítulo III</div>
              <div class="ch-title">Lo que construyeron juntos</div>
              <p class="ch-text">{{ movie()!.chapter3 }}</p>
            </div>
          }

          <!-- Cita destacada -->
          @if (movie()!.highlightQuote) {
            <div class="highlight-quote">
              <div class="hq-mark">"</div>
              <div class="hq-text">{{ movie()!.highlightQuote }}</div>
            </div>
          }

          <!-- Carta del Mentor -->
          @if (movie()!.mentorLetter) {
            <div class="mentor-letter">
              <div class="ml-header">
                <span class="ml-icon">✉️</span>
                <span class="ml-title">Carta del Mentor a la Familia {{ movie()!.familyName }}</span>
              </div>
              <p class="ml-text">{{ movie()!.mentorLetter }}</p>
              <div class="ml-sign">— El Mentor de Integridad</div>
            </div>
          }

          <!-- Historial y nueva generación -->
          <div class="fm-footer">
            <div class="fm-meta">Generada el {{ formatDate(movie()!.generatedAt) }}</div>
            <div class="fm-actions">
              @if (allMovies().length > 1) {
                <button class="btn-history" (click)="showHistory.set(!showHistory())">
                  📚 {{ showHistory() ? 'Ocultar' : 'Ver' }} historial ({{ allMovies().length }})
                </button>
              }
              <button class="btn-new-movie" (click)="generate()">
                🔄 Nueva película
              </button>
            </div>
          </div>

          <!-- Historial de películas -->
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
    .fm-page {
      max-width: 760px; margin: 0 auto;
      padding: 24px 20px 60px;
      font-family: inherit;
      color: var(--if-text-primary, #e0e0e0);
    }

    /* Header */
    .fm-header { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
    .fm-film   { font-size: 40px; }
    .fm-title  { font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .fm-sub    { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }

    /* Loading */
    .fm-loading { display: flex; flex-direction: column; align-items: center; gap: 20px; padding: 52px 20px; color: var(--if-text-secondary, #888); }
    .film-strip { display: flex; gap: 6px; }
    .fs-frame {
      width: 40px; height: 54px; border-radius: 4px;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      animation: flicker 1.2s ease-in-out infinite;
    }
    .fs-frame:nth-child(2) { animation-delay: 0.2s; }
    .fs-frame:nth-child(3) { animation-delay: 0.4s; }
    @keyframes flicker { 0%,100%{opacity:0.4} 50%{opacity:1} }

    /* Vacío */
    .fm-empty { text-align: center; padding: 52px 20px; }
    .ei { font-size: 52px; margin-bottom: 14px; }
    .fm-empty h2 { font-size: 20px; font-weight: 700; margin: 0 0 10px; }
    .fm-empty p  { font-size: 14px; color: var(--if-text-secondary, #999); max-width: 420px; margin: 0 auto 24px; line-height: 1.6; }

    .btn-generate {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border: none; color: white;
      padding: 14px 32px; border-radius: 12px;
      font-size: 15px; font-weight: 800; cursor: pointer;
      transition: opacity 0.2s; box-shadow: 0 4px 20px rgba(124,58,237,0.3);
    }
    .btn-generate:hover { opacity: 0.88; }

    /* Generando */
    .fm-generating { text-align: center; padding: 52px 20px; }
    .gen-projector { font-size: 56px; margin-bottom: 16px; animation: proj 1.5s ease-in-out infinite; }
    @keyframes proj { 0%,100%{transform:scale(1)} 50%{transform:scale(1.06)} }
    .gen-title { font-size: 18px; font-weight: 700; color: #a78bfa; margin-bottom: 20px; }
    .gen-steps { display: flex; flex-direction: column; gap: 8px; max-width: 320px; margin: 0 auto; }
    .gs-step { font-size: 13px; color: var(--if-text-secondary, #777); padding: 8px 12px; border-radius: 8px; background: rgba(255,255,255,0.03); transition: all 0.4s; }
    .gs-step.active { color: #c4b5fd; background: rgba(139,92,246,0.12); border: 1px solid rgba(139,92,246,0.25); }

    /* Póster */
    .fm-poster {
      position: relative; border-radius: 20px; overflow: hidden;
      height: 220px; margin-bottom: 24px;
    }
    .poster-bg {
      position: absolute; inset: 0;
      background: linear-gradient(135deg, #1e1b4b 0%, #312e81 40%, #4c1d95 100%);
      opacity: 0.95;
    }
    .poster-content {
      position: relative; z-index: 1;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      height: 100%; padding: 24px; text-align: center;
    }
    .poster-period { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.18em; color: #a78bfa; margin-bottom: 8px; }
    .poster-family { font-size: 28px; font-weight: 900; color: white; margin-bottom: 12px; letter-spacing: -0.02em; }
    .poster-opening { font-size: 15px; color: rgba(255,255,255,0.7); font-style: italic; max-width: 500px; line-height: 1.5; }

    /* Stats */
    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
    @media (max-width: 480px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
    .stat-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 14px; padding: 18px;
      text-align: center;
    }
    .stat-num   { font-size: 32px; font-weight: 900; font-variant-numeric: tabular-nums; color: #c4b5fd; }
    .stat-label { font-size: 12px; color: var(--if-text-secondary, #888); margin-top: 4px; }

    /* ICF bar */
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

    /* Capítulos */
    .chapter {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 4px solid #7c3aed;
      border-radius: 14px; padding: 20px 22px;
      margin-bottom: 14px;
    }
    .ch-number { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.12em; color: #a78bfa; margin-bottom: 4px; }
    .ch-title  { font-size: 16px; font-weight: 800; margin-bottom: 12px; color: var(--if-text-primary, #ddd); }
    .ch-text   { font-size: 14px; line-height: 1.75; color: var(--if-text-secondary, #bbb); margin: 0; }

    /* Cita destacada */
    .highlight-quote {
      position: relative;
      padding: 24px 28px;
      margin: 20px 0;
      text-align: center;
    }
    .hq-mark { font-size: 64px; line-height: 0.5; color: rgba(124,58,237,0.3); margin-bottom: 12px; }
    .hq-text { font-size: 18px; font-weight: 700; font-style: italic; color: #c4b5fd; line-height: 1.5; }

    /* Carta del Mentor */
    .mentor-letter {
      background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(79,70,229,0.06));
      border: 1px solid rgba(124,58,237,0.2);
      border-radius: 16px; padding: 24px;
      margin-top: 20px;
    }
    .ml-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
    .ml-icon   { font-size: 24px; }
    .ml-title  { font-size: 13px; font-weight: 700; color: #a78bfa; }
    .ml-text   { font-size: 14px; line-height: 1.8; color: var(--if-text-primary, #ddd); margin: 0 0 14px; font-style: italic; }
    .ml-sign   { font-size: 12px; color: var(--if-text-secondary, #888); text-align: right; }

    /* Footer y acciones */
    .fm-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; flex-wrap: wrap; gap: 10px; }
    .fm-meta   { font-size: 11px; color: var(--if-text-secondary, #666); }
    .fm-actions { display: flex; gap: 8px; }
    .btn-history, .btn-new-movie {
      padding: 8px 16px; border-radius: 9px; font-size: 12px; font-weight: 600;
      cursor: pointer; transition: all 0.18s;
    }
    .btn-history { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: var(--if-text-secondary, #aaa); }
    .btn-new-movie { background: rgba(124,58,237,0.12); border: 1px solid rgba(124,58,237,0.3); color: #c4b5fd; }

    /* Historial */
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
    .hi-stats  { font-size: 11px; color: var(--if-text-secondary, #888); margin-top: 2px; }
    .hi-arrow  { color: var(--if-text-secondary, #777); }

    .fm-error { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; padding: 12px 16px; font-size: 13px; color: #fca5a5; }
  `]
})
export class FamilyMovieComponent implements OnInit {
  private readonly movieSvc    = inject(FamilyMovieService);
  private readonly familyState = inject(FamilyStateService);
  private readonly route       = inject(ActivatedRoute);

  readonly familyId   = this.familyState.currentFamilyId;
  readonly movie      = signal<FamilyMovieDto | null>(null);
  readonly allMovies  = signal<FamilyMovieDto[]>([]);
  readonly loading    = signal(false);
  readonly generating = signal(false);
  readonly error      = signal<string | null>(null);
  readonly showHistory = signal(false);
  readonly genStep     = signal(0);

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    const autoGenerate = this.route.snapshot.queryParamMap.get('autoGenerate') === '1';
    if (autoGenerate) {
      // Vino de pilar completado — generar directamente sin esperar lista
      this.generate();
    } else {
      this.load(id);
    }
  }

  private load(id: number): void {
    this.loading.set(true);
    this.movieSvc.list(id).pipe(
      catchError(() => { this.loading.set(false); return of([]); })
    ).subscribe(movies => {
      this.allMovies.set(movies);
      if (movies.length) this.movie.set(movies[0]);
      this.loading.set(false);
    });
  }

  generate(): void {
    const id = this.familyId();
    if (!id) return;
    this.generating.set(true);
    this.error.set(null);
    this.genStep.set(0);

    // Simula pasos visuales mientras espera la IA
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
    });
  }

  selectMovie(m: FamilyMovieDto): void {
    this.movie.set(m);
    this.showHistory.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('es-CO', {
        day: '2-digit', month: 'long', year: 'numeric'
      });
    } catch { return iso; }
  }
}
