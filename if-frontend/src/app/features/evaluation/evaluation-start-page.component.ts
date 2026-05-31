import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { PillarProgressResult } from '../../core/models/question.model';
import { catchError, of, forkJoin } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

type Pillar = 'reconocimiento' | 'amor' | 'entrega';

interface PillarCard {
  key: Pillar;
  icon: string;
  name: string;
  monthRange: string;
  description: string;
  color: string;
  totalQuestions: number;
  sessionsOf20: number;
  isActive: boolean;
}

@Component({
  selector: 'app-evaluation-start-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="eval-start">

      <!-- Header -->
      <div class="es-header">
        <div class="es-icon">◈</div>
        <div>
          <h1 class="es-title">Diagnóstico Familiar</h1>
          <p class="es-sub">
            1000 preguntas · 3 pilares · 20 por sesión ·
            <span class="es-sessions">{{ pillarProgress()?.completedSessions ?? 0 }} sesiones completadas</span>
          </p>
        </div>
      </div>

      <!-- Sin familia -->
      @if (familyId <= 0) {
        <div class="no-family">
          <div class="nf-icon">👨‍👩‍👧‍👦</div>
          <p>Selecciona una familia antes de iniciar el diagnóstico.</p>
          <button class="btn-primary" (click)="goToFamilies()">Ir a Familias</button>
        </div>
      }

      @if (familyId > 0) {

        <!-- Evaluación pendiente de reanudar -->
        @if (pendingEvalId) {
          <div class="pending-banner">
            <span class="pb-icon">⚡</span>
            <div>
              <strong>Tienes una sesión en curso</strong>
              <p>Puedes retomar donde te quedaste o iniciar una nueva sesión.</p>
            </div>
            <div class="pb-actions">
              <button class="btn-resume" (click)="resume()">Continuar sesión →</button>
              <button class="btn-discard" (click)="discardAndStart()">Nueva sesión</button>
            </div>
          </div>
        }

        <!-- Seleccionar pilar -->
        <div class="pillar-section">
          <div class="ps-label">Elige el pilar que vas a diagnosticar hoy:</div>
          <div class="pillar-grid">
            @for (p of pillars; track p.key) {
              <button
                class="pillar-card"
                [class.selected]="selectedPillar() === p.key"
                [class.is-active]="p.isActive"
                (click)="selectPillar(p.key)"
                [style.--pillar-color]="p.color">
                <div class="pc-top">
                  <span class="pc-icon">{{ p.icon }}</span>
                  @if (p.isActive) { <span class="pc-active-badge">Activo</span> }
                </div>
                <div class="pc-name">{{ p.name }}</div>
                <div class="pc-range">{{ p.monthRange }}</div>
                <div class="pc-desc">{{ p.description }}</div>
                <div class="pc-stats">
                  <span class="pc-stat">{{ p.totalQuestions }} preguntas</span>
                  <span class="pc-stat">~{{ p.sessionsOf20 }} sesiones</span>
                </div>
                <!-- progress bar (sesiones del pilar completadas - aproximación) -->
                <div class="pc-bar-wrap">
                  <div class="pc-bar" [style.width.%]="pillarSessionPercent(p.key)"></div>
                </div>
                <div class="pc-pct">{{ pillarSessionPercent(p.key) }}% explorado</div>
              </button>
            }
          </div>
        </div>

        <!-- Seleccionar respondiente -->
        @if (members.length > 0) {
        <div class="member-section">
          <div class="ms-label">¿Quién responde esta sesión?</div>
          <div class="member-chips">
            <button
              class="member-chip"
              [class.selected]="selectedMember === null"
              (click)="selectedMember = null">
              👨‍👩‍👧‍👦 Familia completa
            </button>
            @for (m of members; track m.id) {
              <button
                class="member-chip"
                [class.selected]="selectedMember === m.id"
                (click)="selectedMember = m.id">
                {{ getInitial(m.fullName) }} {{ m.fullName }}
              </button>
            }
          </div>
        </div>
        } <!-- end members -->

        <!-- Info sesión -->
        <div class="session-info">
          <div class="si-item">
            <span class="si-icon">📋</span>
            <span><strong>20 preguntas</strong> adaptadas al pilar seleccionado</span>
          </div>
          <div class="si-item">
            <span class="si-icon">⏱️</span>
            <span>Duración estimada: <strong>8–12 minutos</strong></span>
          </div>
          <div class="si-item">
            <span class="si-icon">💾</span>
            <span>Guardado automático — puedes continuar luego</span>
          </div>
          <div class="si-item">
            <span class="si-icon">🔄</span>
            <span>Cada sesión activa preguntas distintas del banco</span>
          </div>
        </div>

        <!-- Error -->
        @if (errorMessage) {
          <div class="error-msg">⚠️ {{ errorMessage }}</div>
        }

        <!-- Botón iniciar -->
        <button
          class="btn-start"
          [disabled]="loading || !selectedPillar()"
          (click)="start()">
          @if (loading) {
            <span class="spinner"></span> Preparando sesión…
          } @else if (!selectedPillar()) {
            Elige un pilar para comenzar
          } @else {
            🚀 Iniciar diagnóstico · Pilar {{ getPillarLabel(selectedPillar()!) }}
          }
        </button>

      } <!-- end familyId > 0 -->

    </div>
  `,
  styles: [`
    .eval-start { max-width: 860px; margin: 0 auto; }

    .es-header   { display: flex; align-items: center; gap: 16px; margin-bottom: 28px; }
    .es-icon     { font-size: 40px; color: #818cf8; }
    .es-title    { font-size: 24px; font-weight: 800; color: #fff; margin: 0 0 4px; }
    .es-sub      { font-size: 13px; color: rgba(255,255,255,0.4); margin: 0; }
    .es-sessions { color: #6366f1; font-weight: 700; }

    .no-family   { text-align: center; padding: 60px 20px; }
    .nf-icon     { font-size: 48px; margin-bottom: 16px; }

    .pending-banner {
      display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
      padding: 16px 20px; margin-bottom: 24px;
      background: rgba(251,191,36,0.07); border: 1px solid rgba(251,191,36,0.25);
      border-radius: 14px;
    }
    .pb-icon  { font-size: 24px; }
    .pending-banner > div { flex: 1; }
    .pending-banner strong { color: #fbbf24; font-size: 14px; }
    .pending-banner p { font-size: 12px; color: rgba(255,255,255,0.5); margin: 3px 0 0; }
    .pb-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-resume  { padding: 9px 18px; background: rgba(251,191,36,0.15); border: 1px solid rgba(251,191,36,0.35); color: #fbbf24; border-radius: 9px; cursor: pointer; font-size: 13px; font-weight: 700; }
    .btn-discard { padding: 9px 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.4); border-radius: 9px; cursor: pointer; font-size: 12px; }

    /* ── Pilar grid ──────────────────────────────────────────── */
    .pillar-section { margin-bottom: 24px; }
    .ps-label { font-size: 12px; font-weight: 700; color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 12px; }

    .pillar-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }

    .pillar-card {
      background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 16px; padding: 18px; text-align: left; cursor: pointer;
      transition: all 0.2s; color: rgba(255,255,255,0.6); position: relative; overflow: hidden;
    }
    .pillar-card:hover { background: rgba(255,255,255,0.04); border-color: rgba(255,255,255,0.15); transform: translateY(-2px); }
    .pillar-card.selected {
      background: color-mix(in srgb, var(--pillar-color) 10%, transparent);
      border-color: var(--pillar-color);
      color: #fff;
      box-shadow: 0 0 24px color-mix(in srgb, var(--pillar-color) 15%, transparent);
    }
    .pillar-card.is-active::before {
      content: '';
      position: absolute; top: 0; left: 0; right: 0; height: 2px;
      background: var(--pillar-color);
    }

    .pc-top    { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
    .pc-icon   { font-size: 28px; }
    .pc-active-badge { font-size: 9px; font-weight: 800; background: color-mix(in srgb, var(--pillar-color) 20%, transparent); border: 1px solid var(--pillar-color); color: var(--pillar-color); padding: 2px 7px; border-radius: 999px; text-transform: uppercase; letter-spacing: 0.06em; }
    .pc-name   { font-size: 15px; font-weight: 800; color: #fff; margin-bottom: 2px; }
    .pc-range  { font-size: 10px; color: rgba(255,255,255,0.35); margin-bottom: 8px; }
    .pc-desc   { font-size: 12px; color: rgba(255,255,255,0.5); line-height: 1.5; margin-bottom: 10px; }
    .pc-stats  { display: flex; gap: 8px; margin-bottom: 8px; }
    .pc-stat   { font-size: 10px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.08); padding: 2px 7px; border-radius: 5px; color: rgba(255,255,255,0.5); }
    .pillar-card.selected .pc-stat { background: color-mix(in srgb, var(--pillar-color) 12%, transparent); border-color: color-mix(in srgb, var(--pillar-color) 30%, transparent); color: rgba(255,255,255,0.7); }
    .pc-bar-wrap { height: 3px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; margin-bottom: 3px; }
    .pc-bar  { height: 100%; background: var(--pillar-color); border-radius: 2px; transition: width 0.5s; }
    .pc-pct  { font-size: 9px; color: rgba(255,255,255,0.3); text-align: right; }

    /* ── Member chips ───────────────────────────────────────────── */
    .member-section { margin-bottom: 20px; }
    .ms-label { font-size: 12px; font-weight: 700; color: rgba(255,255,255,0.4); text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 10px; }
    .member-chips { display: flex; gap: 8px; flex-wrap: wrap; }
    .member-chip { padding: 7px 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 999px; color: rgba(255,255,255,0.5); font-size: 12px; cursor: pointer; transition: all 0.2s; }
    .member-chip:hover { background: rgba(255,255,255,0.07); border-color: rgba(255,255,255,0.15); }
    .member-chip.selected { background: rgba(99,102,241,0.15); border-color: rgba(99,102,241,0.4); color: #818cf8; font-weight: 700; }

    /* ── Session info ───────────────────────────────────────────── */
    .session-info { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 24px; }
    @media(max-width:600px) { .session-info { grid-template-columns: 1fr; } }
    .si-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; font-size: 13px; color: rgba(255,255,255,0.55); }
    .si-icon { font-size: 18px; flex-shrink: 0; }

    .error-msg { padding: 12px 16px; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: 10px; color: #fca5a5; font-size: 13px; margin-bottom: 16px; }

    /* ── Start button ───────────────────────────────────────────── */
    .btn-start {
      width: 100%; padding: 16px; font-size: 15px; font-weight: 800;
      background: linear-gradient(135deg, #6366f1, #818cf8);
      border: none; border-radius: 14px; color: #fff; cursor: pointer;
      transition: all 0.2s; letter-spacing: 0.02em;
      display: flex; align-items: center; justify-content: center; gap: 8px;
    }
    .btn-start:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(99,102,241,0.35); }
    .btn-start:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

    .spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .btn-primary { padding: 12px 24px; background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.3); color: #818cf8; border-radius: 10px; cursor: pointer; font-size: 14px; font-weight: 700; }
  `]
})
export class EvaluationStartPageComponent implements OnInit {
  private http          = inject(HttpClient);
  private api           = inject(ApiService);
  private router        = inject(Router);
  private route         = inject(ActivatedRoute);
  private familyState   = inject(FamilyStateService);
  private assessment    = inject(AssessmentService);
  private flow          = inject(TransformationFlowService);

  members: Member[]  = [];
  selectedMember: number | null = null;
  loading            = false;
  errorMessage       = '';
  pendingEvalId: number | null = null;

  readonly selectedPillar   = signal<Pillar | null>(null);
  readonly pillarProgress   = signal<PillarProgressResult | null>(null);

  get familyId() { return this.familyState.currentFamilyId(); }

  readonly pillars: PillarCard[] = [
    {
      key: 'reconocimiento', icon: '💛', name: 'Reconocimiento',
      monthRange: 'Meses 1–6', color: '#fbbf24',
      description: 'Entender qué está ocurriendo en nuestra familia. Estabilización y conciencia.',
      totalQuestions: 0, sessionsOf20: 0, isActive: false,
    },
    {
      key: 'amor', icon: '❤️', name: 'Amor',
      monthRange: 'Meses 7–18', color: '#ef4444',
      description: 'Transformar comportamientos. Consolidar hábitos e integridad familiar.',
      totalQuestions: 0, sessionsOf20: 0, isActive: false,
    },
    {
      key: 'entrega', icon: '💙', name: 'Entrega',
      monthRange: 'Meses 19–36', color: '#3b82f6',
      description: 'Crear legado generacional. Trascendencia y misión familiar.',
      totalQuestions: 0, sessionsOf20: 0, isActive: false,
    },
  ];

  ngOnInit() {
    // Prioridad: query param (viene del botón "20 más" en result page) → pilar activo del flujo
    const queryPillar = this.route.snapshot.queryParamMap.get('pillar') as Pillar | null;
    const currentPillar = (queryPillar ?? this.flow.currentPillar()) as Pillar;
    this.selectedPillar.set(currentPillar);

    // Marcar qué pilar está activo en el viaje
    this.pillars.forEach(p => p.isActive = p.key === currentPillar);

    if (this.familyId > 0) {
      forkJoin({
        members:  this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`),
        history:  this.http.get<any>(`${this.api.base}/assessments/family/${this.familyId}/history`).pipe(catchError(() => of(null))),
        progress: this.assessment.getPillarProgress(this.familyId).pipe(catchError(() => of(null))),
      }).subscribe(({ members, history, progress }) => {
        this.members = members?.data ?? [];

        // Evaluación pendiente
        if (history?.data) {
          const pending = history.data.find((e: any) => e.status === 'STARTED');
          if (pending) this.pendingEvalId = pending.id;
        }

        // Progreso por pilar
        if (progress) {
          this.pillarProgress.set(progress);
          progress.pillars.forEach((info: any) => {
            const card = this.pillars.find(p => p.key === info.pillar);
            if (card) {
              card.totalQuestions = info.totalQuestions;
              card.sessionsOf20   = info.sessionsOf20;
            }
          });
        }
      });
    }
  }

  selectPillar(p: Pillar) { this.selectedPillar.set(p); }

  /** % aproximado de sesiones completadas para un pilar */
  pillarSessionPercent(pillar: Pillar): number {
    const prog = this.pillarProgress();
    if (!prog || prog.totalSessions === 0) return 0;
    const completedTotal = prog.completedSessions;
    const card = this.pillars.find(p => p.key === pillar);
    if (!card || card.sessionsOf20 === 0) return 0;
    // Distribuir sesiones completadas entre pilares proporcionalmente
    const ratio = card.sessionsOf20 / Math.max(prog.totalSessions, 1);
    return Math.min(100, Math.round((completedTotal * ratio / card.sessionsOf20) * 100));
  }

  getPillarLabel(p: Pillar): string {
    return { reconocimiento: '💛 Reconocimiento', amor: '❤️ Amor', entrega: '💙 Entrega' }[p] ?? p;
  }

  getInitial(name: string): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }

  resume() {
    if (this.pendingEvalId) {
      this.router.navigate(['/evaluations', this.pendingEvalId, 'form']);
    }
  }

  discardAndStart() {
    this.pendingEvalId = null;
    this.start();
  }

  goToFamilies() { this.router.navigate(['/families']); }

  start() {
    this.errorMessage = '';
    const pillar = this.selectedPillar();

    if (this.familyId <= 0) { this.goToFamilies(); return; }
    if (!pillar) { this.errorMessage = 'Selecciona un pilar para comenzar.'; return; }

    this.loading = true;
    const payload = {
      familyId:   this.familyId,
      memberId:   this.selectedMember,
      pillarName: pillar,
    };

    this.http.post<any>(`${this.api.base}/assessments/start`, payload).subscribe({
      next: (response: any) => {
        this.loading = false;
        const evalId = response?.data?.id ?? response?.id;
        if (evalId) {
          // Pasar pillarName como query param para que el form use preguntas del pilar
          this.router.navigate(['/evaluations', evalId, 'form'], {
            queryParams: { pillar }
          });
        } else {
          this.errorMessage = 'No se recibió ID de evaluación.';
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err?.error?.message ?? 'No se pudo iniciar la evaluación. Intenta de nuevo.';
      }
    });
  }
}
