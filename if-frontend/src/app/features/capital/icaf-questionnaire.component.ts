import {
  Component, OnInit, OnDestroy, inject,
  ChangeDetectionStrategy, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { IcafDataService } from './services/icaf-data.service';
import { IcafQuestion } from '../../core/models/icaf.model';
import { environment } from '../../../environments/environment';

// ── Dominios disponibles ────────────────────────────────────────────────────

interface DomainMeta {
  key:   string;
  label: string;
  icon:  string;
  color: string;
  desc:  string;
}

const DOMAINS: DomainMeta[] = [
  {
    key:   'confianza',
    label: 'Confianza',
    icon:  '🤝',
    color: '#0ea5e9',
    desc:  'Percepción de apoyo, escucha y apertura emocional en la familia.'
  },
  {
    key:   'bienestar_emocional',
    label: 'Bienestar Emocional',
    icon:  '💛',
    color: '#ec4899',
    desc:  'Estado emocional individual y colectivo dentro del núcleo familiar.'
  },
  {
    key:   'autonomia',
    label: 'Autonomía',
    icon:  '🦋',
    color: '#f59e0b',
    desc:  'Capacidad de cada miembro de actuar con independencia y ser respetado en sus decisiones.'
  },
  {
    key:   'proposito',
    label: 'Propósito',
    icon:  '🧭',
    color: '#14b8a6',
    desc:  'Sentido de dirección compartida, valores comunes y visión de futuro familiar.'
  },
  {
    key:   'emprendimiento',
    label: 'Emprendimiento',
    icon:  '🚀',
    color: '#f97316',
    desc:  'Capacidad de la familia de adaptarse, innovar y tomar iniciativa ante los retos.'
  },
  {
    key:   'legado',
    label: 'Legado',
    icon:  '🏺',
    color: '#d4af37',
    desc:  'Conciencia de la historia familiar, transmisión de valores y conexión intergeneracional.'
  },
];

const LIKERT_LABELS: Record<number, string> = {
  1: 'Totalmente en desacuerdo',
  2: 'En desacuerdo',
  3: 'Neutral',
  4: 'De acuerdo',
  5: 'Totalmente de acuerdo',
};

type Phase = 'selector' | 'questions' | 'result';

@Component({
  selector: 'app-icaf-questionnaire',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
<div class="q-root">

  <!-- ── CABECERA ─────────────────────────────────────────────── -->
  <div class="q-header">
    <a routerLink="/capital" class="q-back">← Capital Familiar</a>
    <h1 class="q-title">Cuestionario ICaF</h1>
    <p class="q-subtitle">Responde para obtener datos reales en lugar de estimaciones</p>
  </div>

  <!-- ── FASE 1: SELECTOR DE DOMINIO ──────────────────────────── -->
  @if (phase() === 'selector') {
    <div class="domain-grid">
      @for (d of domains; track d.key) {
        <button class="domain-card" [style.--c]="d.color" (click)="selectDomain(d)">
          <span class="domain-icon">{{ d.icon }}</span>
          <span class="domain-label">{{ d.label }}</span>
          <span class="domain-desc">{{ d.desc }}</span>
          <span class="domain-cta">Responder →</span>
        </button>
      }
    </div>
  }

  <!-- ── FASE 2: PREGUNTAS ─────────────────────────────────────── -->
  @if (phase() === 'questions') {
    <div class="q-form-wrap">

      <!-- encabezado del dominio -->
      <div class="domain-header" [style.--c]="activeDomain()!.color">
        <span class="domain-icon-lg">{{ activeDomain()!.icon }}</span>
        <div>
          <div class="domain-name">{{ activeDomain()!.label }}</div>
          <div class="domain-desc-sm">{{ activeDomain()!.desc }}</div>
        </div>
      </div>

      <!-- progreso -->
      <div class="q-progress-row">
        <span class="q-progress-label">{{ answeredCount() }} / {{ questions().length }} respondidas</span>
        <div class="q-progress-track">
          <div class="q-progress-fill"
               [style.width.%]="progressPct()"
               [style.background]="activeDomain()!.color">
          </div>
        </div>
      </div>

      <!-- lista de preguntas -->
      <div class="q-list">
        @for (q of questions(); track q.questionKey; let i = $index) {
          <div class="q-item" [class.answered]="answers()[q.questionKey]">
            <div class="q-num">{{ i + 1 }}</div>
            <div class="q-body">
              <p class="q-text">{{ q.text }}</p>
              <div class="likert-row">
                @for (v of [1,2,3,4,5]; track v) {
                  <button class="likert-btn"
                          [class.selected]="answers()[q.questionKey] === v"
                          [style.--c]="activeDomain()!.color"
                          [title]="likertLabel(v)"
                          (click)="setAnswer(q.questionKey, v)">
                    {{ v }}
                  </button>
                }
              </div>
              <div class="likert-labels">
                <span>En desacuerdo</span>
                <span>De acuerdo</span>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- acciones -->
      <div class="q-actions">
        <button class="btn-back" (click)="phase.set('selector')">← Volver</button>
        <button class="btn-submit"
                [disabled]="!allAnswered() || submitting()"
                [style.background]="activeDomain()!.color"
                (click)="submit()">
          {{ submitting() ? 'Guardando…' : 'Enviar respuestas' }}
        </button>
      </div>

    </div>
  }

  <!-- ── FASE 3: RESULTADO ─────────────────────────────────────── -->
  @if (phase() === 'result') {
    <div class="result-wrap">
      <div class="result-icon">✅</div>
      <h2 class="result-title">¡Respuestas guardadas!</h2>
      <p class="result-domain">{{ activeDomain()!.label }}</p>

      @if (result()) {
        <div class="result-score-card" [style.--c]="activeDomain()!.color">
          <div class="result-score">{{ result()!.score | number:'1.0-0' }}</div>
          <div class="result-score-label">puntuación del dominio (0 – 100)</div>
          <div class="result-detail">
            {{ result()!.savedCount }} preguntas guardadas ·
            {{ result()!.totalAnswered }} respuestas totales en el sistema
          </div>
        </div>
      }

      <p class="result-hint">El ICaF ha sido recalculado automáticamente con los nuevos datos.</p>

      <div class="result-actions">
        <button class="btn-back" (click)="goOther()">Responder otro dominio</button>
        <a routerLink="/capital" class="btn-dashboard">Ver dashboard ICaF →</a>
        <a routerLink="/smff"    class="btn-smff">Ver SMFF →</a>
      </div>
    </div>
  }

</div>
  `,
  styles: [`
    .q-root {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      color: #e8eaf0;
      max-width: 720px;
      margin: 0 auto;
      padding: 24px 16px 48px;
    }

    /* ── header ── */
    .q-header { margin-bottom: 28px; }
    .q-back { color: #6b8cff; font-size: 12px; text-decoration: none; display: inline-block; margin-bottom: 10px; }
    .q-back:hover { text-decoration: underline; }
    .q-title { font-size: 26px; font-weight: 800; margin: 0 0 4px; }
    .q-subtitle { color: #8b8fa8; font-size: 13px; margin: 0; }

    /* ── domain selector ── */
    .domain-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 14px;
    }
    .domain-card {
      background: rgba(255,255,255,.04);
      border: 1.5px solid rgba(255,255,255,.08);
      border-radius: 14px; padding: 20px;
      cursor: pointer; text-align: left;
      display: flex; flex-direction: column; gap: 6px;
      transition: all .2s; color: #e8eaf0;
    }
    .domain-card:hover {
      background: color-mix(in srgb, var(--c) 10%, rgba(255,255,255,.04));
      border-color: var(--c);
      transform: translateY(-2px);
    }
    .domain-icon { font-size: 28px; }
    .domain-label { font-size: 16px; font-weight: 700; color: var(--c); }
    .domain-desc { font-size: 12px; color: #8b8fa8; line-height: 1.5; }
    .domain-cta { font-size: 11px; font-weight: 600; color: var(--c); margin-top: 6px; }

    /* ── form wrap ── */
    .q-form-wrap { display: flex; flex-direction: column; gap: 20px; }

    .domain-header {
      display: flex; align-items: center; gap: 14px;
      background: color-mix(in srgb, var(--c) 8%, rgba(255,255,255,.03));
      border: 1px solid color-mix(in srgb, var(--c) 30%, transparent);
      border-radius: 12px; padding: 14px 16px;
    }
    .domain-icon-lg { font-size: 32px; }
    .domain-name { font-size: 16px; font-weight: 700; color: var(--c); }
    .domain-desc-sm { font-size: 11px; color: #8b8fa8; margin-top: 2px; }

    /* progreso */
    .q-progress-row { display: flex; align-items: center; gap: 10px; }
    .q-progress-label { font-size: 11px; color: #8b8fa8; white-space: nowrap; }
    .q-progress-track { flex: 1; height: 4px; background: rgba(255,255,255,.08); border-radius: 2px; overflow: hidden; }
    .q-progress-fill { height: 100%; border-radius: 2px; transition: width .3s; }

    /* preguntas */
    .q-list { display: flex; flex-direction: column; gap: 16px; }

    .q-item {
      display: flex; gap: 12px;
      background: rgba(255,255,255,.03);
      border: 1px solid rgba(255,255,255,.07);
      border-radius: 10px; padding: 14px;
      transition: border-color .2s;
    }
    .q-item.answered { border-color: rgba(255,255,255,.15); }

    .q-num {
      width: 24px; height: 24px; border-radius: 50%;
      background: rgba(255,255,255,.08);
      font-size: 10px; font-weight: 700; color: #8b8fa8;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0; margin-top: 2px;
    }
    .q-body { flex: 1; min-width: 0; }
    .q-text { font-size: 13px; line-height: 1.6; margin: 0 0 12px; color: #d8dae8; }

    .likert-row { display: flex; gap: 8px; }
    .likert-btn {
      width: 40px; height: 40px; border-radius: 8px;
      border: 1.5px solid rgba(255,255,255,.12);
      background: rgba(255,255,255,.04);
      color: rgba(255,255,255,.6); font-size: 14px; font-weight: 700;
      cursor: pointer; transition: all .15s;
    }
    .likert-btn:hover {
      background: color-mix(in srgb, var(--c) 20%, rgba(255,255,255,.04));
      border-color: var(--c); color: #fff;
    }
    .likert-btn.selected {
      background: color-mix(in srgb, var(--c) 25%, rgba(255,255,255,.06));
      border-color: var(--c); color: #fff;
      box-shadow: 0 0 0 2px color-mix(in srgb, var(--c) 30%, transparent);
    }

    .likert-labels {
      display: flex; justify-content: space-between;
      font-size: 9px; color: #8b8fa8; margin-top: 5px; padding: 0 2px;
    }

    /* acciones */
    .q-actions { display: flex; gap: 10px; justify-content: flex-end; padding-top: 8px; }

    .btn-back {
      padding: 10px 18px; border-radius: 9px;
      background: rgba(255,255,255,.05);
      border: 1px solid rgba(255,255,255,.1);
      color: rgba(255,255,255,.6); font-size: 13px; font-weight: 600;
      cursor: pointer; transition: all .15s;
    }
    .btn-back:hover { background: rgba(255,255,255,.09); color: #fff; }

    .btn-submit {
      padding: 10px 22px; border-radius: 9px;
      border: none; color: #fff; font-size: 13px; font-weight: 700;
      cursor: pointer; transition: opacity .15s;
    }
    .btn-submit:disabled { opacity: .4; cursor: not-allowed; }

    /* ── resultado ── */
    .result-wrap {
      display: flex; flex-direction: column; align-items: center; gap: 14px;
      padding: 40px 20px; text-align: center;
    }
    .result-icon { font-size: 48px; }
    .result-title { font-size: 22px; font-weight: 800; margin: 0; }
    .result-domain { font-size: 14px; color: #8b8fa8; margin: 0; }

    .result-score-card {
      background: color-mix(in srgb, var(--c) 10%, rgba(255,255,255,.04));
      border: 1.5px solid color-mix(in srgb, var(--c) 40%, transparent);
      border-radius: 14px; padding: 24px 32px;
      display: flex; flex-direction: column; align-items: center; gap: 6px;
    }
    .result-score { font-size: 54px; font-weight: 900; color: var(--c); line-height: 1; }
    .result-score-label { font-size: 11px; color: #8b8fa8; }
    .result-detail { font-size: 11px; color: rgba(255,255,255,.4); margin-top: 4px; }

    .result-hint { font-size: 12px; color: #8b8fa8; max-width: 380px; line-height: 1.6; margin: 0; }

    .result-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-top: 8px; }
    .btn-dashboard, .btn-smff {
      padding: 10px 20px; border-radius: 9px; font-size: 13px; font-weight: 600;
      text-decoration: none; transition: all .15s;
    }
    .btn-dashboard {
      background: rgba(99,102,241,.15); border: 1px solid rgba(99,102,241,.35); color: #a5b4fc;
    }
    .btn-dashboard:hover { background: rgba(99,102,241,.25); }
    .btn-smff {
      background: rgba(107,140,255,.12); border: 1px solid rgba(107,140,255,.3); color: #93c5fd;
    }
    .btn-smff:hover { background: rgba(107,140,255,.22); }

    @media (max-width: 520px) {
      .likert-btn { width: 34px; height: 34px; font-size: 12px; }
      .q-actions { flex-direction: column; }
    }
  `]
})
export class IcafQuestionnaireComponent implements OnInit, OnDestroy {

  private readonly svc     = inject(IcafDataService);
  private readonly http    = inject(HttpClient);
  private readonly router  = inject(Router);
  private readonly route   = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();

  readonly domains = DOMAINS;

  // ── Estado ───────────────────────────────────────────────────────────────

  readonly phase         = signal<Phase>('selector');
  readonly activeDomain  = signal<DomainMeta | null>(null);
  readonly questions     = signal<IcafQuestion[]>([]);
  readonly answers       = signal<Record<string, number>>({});
  readonly submitting    = signal(false);
  readonly result        = signal<{ score: number; savedCount: number; totalAnswered: number } | null>(null);

  familyId = 0;

  // ── Computed ─────────────────────────────────────────────────────────────

  readonly answeredCount = computed(() =>
    Object.keys(this.answers()).filter(k => this.answers()[k] > 0).length
  );

  readonly progressPct = computed(() => {
    const total = this.questions().length;
    return total ? (this.answeredCount() / total) * 100 : 0;
  });

  readonly allAnswered = computed(() =>
    this.questions().length > 0 &&
    this.answeredCount() === this.questions().length
  );

  // ── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.http
      .get<any>(`${environment.apiBaseUrl}/families/mine`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => { this.familyId = res?.data?.id ?? res?.id ?? 0; },
        error: () => {}
      });

    // Soporte para /capital/questionnaire?domain=confianza
    const domain = this.route.snapshot.queryParamMap.get('domain');
    if (domain) {
      const meta = DOMAINS.find(d => d.key === domain);
      if (meta) this.selectDomain(meta);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Acciones ─────────────────────────────────────────────────────────────

  selectDomain(d: DomainMeta): void {
    this.activeDomain.set(d);
    this.answers.set({});
    this.result.set(null);
    this.phase.set('questions');

    this.svc.getQuestions(d.key)
      .pipe(takeUntil(this.destroy$))
      .subscribe(qs => this.questions.set(qs));
  }

  setAnswer(key: string, value: number): void {
    this.answers.update(current => ({ ...current, [key]: value }));
  }

  likertLabel(v: number): string {
    return LIKERT_LABELS[v] ?? '';
  }

  submit(): void {
    if (!this.allAnswered() || !this.familyId) return;
    this.submitting.set(true);

    this.svc.saveAnswers(this.familyId, this.activeDomain()!.key, this.answers())
      .pipe(takeUntil(this.destroy$))
      .subscribe(res => {
        this.submitting.set(false);
        if (res) {
          this.result.set(res);
          this.phase.set('result');
        }
      });
  }

  goOther(): void {
    this.phase.set('selector');
    this.activeDomain.set(null);
    this.questions.set([]);
    this.answers.set({});
    this.result.set(null);
  }
}
