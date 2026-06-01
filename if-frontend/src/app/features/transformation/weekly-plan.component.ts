import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { ApiService } from '../../core/services/api.service';
import { SprintService } from '../family-logbook/sprint.service';
import { catchError, of } from 'rxjs';

interface WeeklyTask {
  id: string;
  description: string;
  responsible: string;
  when: string;
  indicator: string;
  done: boolean;
  phase: 'prepare' | 'execute' | 'evaluate' | 'consolidate';
}

type WeekPhase = 'prepare' | 'execute' | 'evaluate' | 'consolidate';

const WEEK_PHASES: Array<{ key: WeekPhase; label: string; icon: string; desc: string; color: string }> = [
  { key: 'prepare',    label: 'Semana 1 — Preparar',    icon: '📋', desc: 'Definir qué hacer, quién lo hace, cuándo y el indicador de éxito.', color: '#6366f1' },
  { key: 'execute',    label: 'Semana 2 — Ejecutar',    icon: '⚡', desc: 'Poner en marcha lo planeado. Registrar en la bitácora diariamente.', color: '#f59e0b' },
  { key: 'evaluate',   label: 'Semana 3 — Evaluar',     icon: '🔍', desc: 'Revisar qué funcionó, qué no y por qué. Recoger evidencias.', color: '#10b981' },
  { key: 'consolidate',label: 'Semana 4 — Consolidar',  icon: '🏆', desc: 'Cerrar el sprint, celebrar logros y definir el siguiente paso.', color: '#818cf8' },
];

@Component({
  selector: 'app-weekly-plan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="wp-page">
      <div class="wp-header">
        <div>
          <div class="wp-title">Planeación Semanal Familiar</div>
          <div class="wp-sub">4 fases · Preparar → Ejecutar → Evaluar → Consolidar</div>
        </div>
        <div class="wp-sprint">
          <span class="sprint-label">Sprint activo</span>
          <span class="sprint-num">#{{ currentSprint() }}</span>
        </div>
      </div>

      <!-- Phase selector -->
      <div class="phases">
        @for (p of phases; track p.key) {
          <button class="phase-card" [class.active]="activePhase() === p.key" (click)="activePhase.set(p.key)">
            <span class="phase-icon">{{ p.icon }}</span>
            <div>
              <div class="phase-name">{{ p.label }}</div>
              <div class="phase-desc">{{ p.desc }}</div>
            </div>
          </button>
        }
      </div>

      <!-- Task list for current phase -->
      <div class="task-section">
        <div class="ts-header">
          <span class="ts-title">{{ currentPhase()?.label }}</span>
          <button class="btn-add" (click)="addTask()">+ Agregar tarea</button>
        </div>

        @if (phaseTasks().length === 0) {
          <div class="empty-state">
            <div class="es-icon">{{ currentPhase()?.icon }}</div>
            <div class="es-text">No hay tareas en esta fase aún.</div>
            <div class="es-hint">Haz clic en "+ Agregar tarea" para comenzar.</div>
          </div>
        }

        <div class="task-list">
          @for (task of phaseTasks(); track task.id) {
            <div class="task-row" [class.done]="task.done">
              <input type="checkbox"
                [ngModel]="task.done"
                (ngModelChange)="updateTask(task.id, 'done', $event)"
                class="task-check" />
              <div class="task-body">
                <input class="task-desc"
                  [ngModel]="task.description"
                  (ngModelChange)="updateTask(task.id, 'description', $event)"
                  placeholder="¿Qué hay que hacer?" />
                <div class="task-meta">
                  <input class="task-input"
                    [ngModel]="task.responsible"
                    (ngModelChange)="updateTask(task.id, 'responsible', $event)"
                    placeholder="¿Quién lo hace?" />
                  <input class="task-input"
                    [ngModel]="task.when"
                    (ngModelChange)="updateTask(task.id, 'when', $event)"
                    placeholder="¿Cuándo?" />
                  <input class="task-input"
                    [ngModel]="task.indicator"
                    (ngModelChange)="updateTask(task.id, 'indicator', $event)"
                    placeholder="Indicador de éxito" />
                </div>
              </div>
              <button class="btn-remove" (click)="removeTask(task.id)">✕</button>
            </div>
          }
        </div>

        <!-- Summary row -->
        @if (phaseTasks().length > 0) {
          <div class="task-summary">
            <span>{{ doneCount() }} / {{ phaseTasks().length }} completadas</span>
            <div class="summary-bar">
              <div class="summary-fill" [style.width.%]="donePercent()"></div>
            </div>
            <span class="summary-pct">{{ donePercent() }}%</span>
          </div>
        }
      </div>

      <!-- Daily questions (always visible) -->
      <div class="daily-section">
        <div class="daily-header">
          <div class="daily-title">📔 Daily Familiar (2 min)</div>
          @if (activeSprintId()) {
            <button class="btn-goto-sprint" (click)="goToSprint()">Ver Sprint activo →</button>
          }
        </div>
        <div class="daily-grid">
          @for (q of dailyQuestions; track q) {
            <div class="daily-q">
              <div class="dq-label">{{ q }}</div>
              <textarea class="dq-input" rows="2"
                placeholder="Tu respuesta…"
                [value]="dailyAnswers()[q] ?? ''"
                (input)="setDailyAnswer(q, $any($event.target).value)">
              </textarea>
            </div>
          }
        </div>
        <button class="btn-save-daily" (click)="savePhase()">
          {{ saving() ? '⏳ Guardando…' : saved() ? '✓ Guardado' : '💾 Guardar fase en plan semanal' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .wp-page { max-width: 900px; margin: 0 auto; }
    .wp-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
    .wp-title { font-size: 22px; font-weight: 800; color: #fff; margin-bottom: 4px; }
    .wp-sub   { font-size: 13px; color: rgba(255,255,255,0.4); }
    .wp-sprint { text-align: right; }
    .sprint-label { display: block; font-size: 10px; color: rgba(255,255,255,0.35); letter-spacing: 0.06em; text-transform: uppercase; }
    .sprint-num   { font-size: 28px; font-weight: 900; color: #6366f1; }

    .phases { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin-bottom: 24px; }
    .phase-card { display: flex; gap: 12px; align-items: flex-start; padding: 14px; border-radius: 12px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07); cursor: pointer; text-align: left; color: rgba(255,255,255,0.5); transition: all 0.2s; }
    .phase-card:hover { background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.12); }
    .phase-card.active { background: rgba(99,102,241,0.1); border-color: rgba(99,102,241,0.3); color: #fff; }
    .phase-icon { font-size: 20px; flex-shrink: 0; }
    .phase-name { font-size: 12px; font-weight: 700; margin-bottom: 3px; }
    .phase-desc { font-size: 11px; color: rgba(255,255,255,0.35); line-height: 1.4; }
    .phase-card.active .phase-desc { color: rgba(255,255,255,0.5); }

    .task-section { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 14px; padding: 20px; margin-bottom: 24px; }
    .ts-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .ts-title { font-size: 15px; font-weight: 700; color: #fff; }
    .btn-add  { font-size: 12px; font-weight: 700; padding: 7px 14px; background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.3); color: #818cf8; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .btn-add:hover { background: rgba(99,102,241,0.25); }

    .empty-state { text-align: center; padding: 32px 0; }
    .es-icon { font-size: 32px; margin-bottom: 8px; }
    .es-text { font-size: 14px; color: rgba(255,255,255,0.5); font-weight: 600; }
    .es-hint { font-size: 12px; color: rgba(255,255,255,0.25); margin-top: 4px; }

    .task-list { display: flex; flex-direction: column; gap: 8px; }
    .task-row { display: flex; align-items: flex-start; gap: 10px; padding: 12px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; transition: all 0.2s; }
    .task-row.done { opacity: 0.5; }
    .task-check { margin-top: 6px; accent-color: #6366f1; width: 16px; height: 16px; flex-shrink: 0; }
    .task-body  { flex: 1; }
    .task-desc  { width: 100%; background: none; border: none; color: #fff; font-size: 13px; font-weight: 600; outline: none; padding: 2px 0; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 8px; }
    .task-meta  { display: flex; gap: 8px; flex-wrap: wrap; }
    .task-input { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.07); border-radius: 6px; color: rgba(255,255,255,0.6); font-size: 11px; padding: 4px 8px; outline: none; min-width: 100px; }
    .btn-remove { color: rgba(255,255,255,0.2); background: none; border: none; cursor: pointer; font-size: 13px; padding: 0 4px; }
    .btn-remove:hover { color: #ef4444; }

    .task-summary { display: flex; align-items: center; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.05); font-size: 12px; color: rgba(255,255,255,0.4); }
    .summary-bar  { flex: 1; height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; }
    .summary-fill { height: 100%; background: #6366f1; border-radius: 2px; transition: width 0.3s; }
    .summary-pct  { font-weight: 700; color: #6366f1; }

    .daily-section { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 14px; padding: 20px; }
    .daily-header  { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .daily-title   { font-size: 15px; font-weight: 700; color: #fff; }
    .btn-goto-sprint { font-size: 11px; font-weight: 600; padding: 5px 12px; background: rgba(99,102,241,0.1); border: 1px solid rgba(99,102,241,0.25); color: #818cf8; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .btn-goto-sprint:hover { background: rgba(99,102,241,0.2); }
    .daily-grid    { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 12px; margin-bottom: 16px; }
    .daily-q       { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; padding: 12px; }
    .dq-label      { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.6); margin-bottom: 6px; }
    .dq-input      { width: 100%; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.07); border-radius: 6px; color: rgba(255,255,255,0.7); font-size: 12px; padding: 6px 8px; resize: none; outline: none; font-family: inherit; }
    .btn-save-daily { font-size: 13px; font-weight: 700; padding: 10px 20px; background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.3); color: #818cf8; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
    .btn-save-daily:hover { background: rgba(99,102,241,0.25); }
  `]
})
export class WeeklyPlanComponent implements OnInit {
  protected familyState = inject(FamilyStateService);
  private flow          = inject(TransformationFlowService);
  private http          = inject(HttpClient);
  private api           = inject(ApiService);
  private sprintService = inject(SprintService);
  private router        = inject(Router);

  readonly phases        = WEEK_PHASES;
  readonly activePhase   = signal<WeekPhase>('prepare');
  readonly currentSprint = signal(1);
  readonly activeSprintId = signal<number | null>(null);
  readonly saving        = signal(false);
  readonly saved         = signal(false);
  readonly loadingSprint = signal(true);

  readonly dailyQuestions = [
    '¿Qué logré hoy?', '¿Qué aprendí hoy?', '¿Qué dificultad encontré?',
    '¿Cómo me sentí?',  '¿Qué haré mañana?',
  ];
  readonly dailyAnswers = signal<Record<string, string>>({});

  private tasks = signal<WeeklyTask[]>([]);

  readonly currentPhase = computed(() => WEEK_PHASES.find(p => p.key === this.activePhase()));
  readonly phaseTasks   = computed(() => this.tasks().filter(t => t.phase === this.activePhase()));
  readonly doneCount    = computed(() => this.phaseTasks().filter(t => t.done).length);
  readonly donePercent  = computed(() => {
    const total = this.phaseTasks().length;
    return total ? Math.round((this.doneCount() / total) * 100) : 0;
  });

  get familyId() { return this.familyState.currentFamilyId(); }
  get apiBase()  { return `${this.api.base}/families/${this.familyId}/weekly-plans`; }

  ngOnInit() {
    const fallback = this.flow.currentSprintNumber();
    this.currentSprint.set(fallback);

    if (this.familyId > 0) {
      this.sprintService.getActiveSprint(this.familyId)
        .pipe(catchError(() => of(null)))
        .subscribe(sprint => {
          if (sprint) {
            this.activeSprintId.set(sprint.id ?? null);
          }
          this.loadingSprint.set(false);
          this.loadAll();
        });
    } else {
      this.loadingSprint.set(false);
    }
  }

  goToSprint() {
    this.router.navigate(['/family-logbook']);
  }

  private loadAll() {
    this.http.get<any[]>(this.apiBase).pipe(catchError(() => of([]))).subscribe(plans => {
      if (!plans?.length) return;
      const allTasks: WeeklyTask[] = [];
      plans.forEach((plan: any) => {
        const phase = plan.phase?.toLowerCase() as WeekPhase;
        (plan.tasks ?? []).forEach((t: any, i: number) => {
          allTasks.push({
            id:          String(t.id ?? `${phase}-${i}`),
            description: t.description ?? '',
            responsible: t.responsible ?? '',
            when:        t.when ?? '',
            indicator:   t.indicator ?? '',
            done:        !!t.done,
            phase,
          });
        });
      });
      this.tasks.set(allTasks);
    });
  }

  savePhase() {
    if (this.familyId <= 0) return;
    const phase   = this.activePhase().toUpperCase();
    const tasks   = this.phaseTasks().map((t, i) => ({
      description: t.description, responsible: t.responsible,
      when: t.when, indicator: t.indicator, done: t.done, sortOrder: i,
    }));
    const body = { sprintNumber: this.currentSprint(), phase, tasks, dailyAnswers: this.dailyAnswers() };

    this.saving.set(true);
    this.http.put<any>(`${this.apiBase}/${phase}`, body)
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 2500);
      });
  }

  setDailyAnswer(question: string, value: string) {
    this.dailyAnswers.update(answers => ({ ...answers, [question]: value }));
  }

  updateTask(id: string, field: keyof WeeklyTask, value: any) {
    this.tasks.update(tasks => tasks.map(t => t.id === id ? { ...t, [field]: value } : t));
  }

  addTask() {
    this.tasks.update(tasks => [...tasks, {
      id: Date.now().toString(), description: '', responsible: '',
      when: '', indicator: '', done: false, phase: this.activePhase(),
    }]);
  }

  removeTask(id: string) {
    this.tasks.update(tasks => tasks.filter(t => t.id !== id));
  }
}
