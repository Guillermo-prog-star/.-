import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  ADJUSTMENT_KINDS,
  PROJECT_DECISIONS,
  REVIEW_KINDS,
  ReviewKind,
  ReviewResponse,
  apiErrorMessage,
  todayIso
} from './sistemas.model';
import { SistemasService } from './sistemas.service';

interface NamedRow {
  name: string;
  decision: string;
}

interface ObjectiveRow {
  objective: string;
  achievementCriterion: string;
  reviewOn: string;
}

@Component({
  selector: 'app-sistemas-review',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './sistemas-review.component.html',
  styleUrl: './sistemas.css'
})
export class SistemasReviewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly sistemas = inject(SistemasService);

  readonly kinds = REVIEW_KINDS;
  readonly decisions = PROJECT_DECISIONS;
  readonly adjustmentKinds = ADJUSTMENT_KINDS;
  readonly kind = signal<ReviewKind | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly saved = signal<ReviewResponse | null>(null);

  reviewDate = todayIso();
  captureEmptied = false;
  agendaChecked = false;
  actions: string[] = [''];
  calendarReviewed = false;
  tasksPruned = false;
  commitmentsTransferred = false;
  priorities: string[] = [''];
  projects: NamedRow[] = [{ name: '', decision: '' }];
  stageChange: 'si' | 'no' | '' = '';
  objectives: ObjectiveRow[] = [{ objective: '', achievementCriterion: '', reviewOn: '' }];
  changeDescription = '';
  whatNoLongerFits = '';
  adjustments: NamedRow[] = [{ name: '', decision: '' }];

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const slug = params.get('tipo') ?? '';
      this.kind.set(this.kinds.find(item => item.slug === slug) ?? null);
      this.saved.set(null);
      this.error.set('');
      this.resetDraft();
    });
  }

  addLine(list: string[], max: number): void {
    if (list.length < max) list.push('');
  }

  removeLine(list: string[], index: number): void {
    if (list.length > 1) list.splice(index, 1);
  }

  addNamed(list: NamedRow[], max: number): void {
    if (list.length < max) list.push({ name: '', decision: '' });
  }

  removeNamed(list: NamedRow[], index: number): void {
    if (list.length > 1) list.splice(index, 1);
  }

  addObjective(): void {
    if (this.objectives.length < 5) {
      this.objectives.push({ objective: '', achievementCriterion: '', reviewOn: '' });
    }
  }

  removeObjective(index: number): void {
    if (this.objectives.length > 1) this.objectives.splice(index, 1);
  }

  localError(): string | null {
    const kind = this.kind();
    if (!kind) return 'Esa revisión no existe.';
    const dateError = this.dateError();
    if (dateError) return dateError;
    return this.outputError(kind.code);
  }

  private dateError(): string | null {
    if (!this.reviewDate) return 'La fecha de la revisión es obligatoria.';
    if (this.reviewDate > todayIso()) return 'La revisión no puede quedar en una fecha futura.';
    return null;
  }

  private outputError(code: ReviewKind['code']): string | null {
    switch (code) {
      case 'DAILY': return this.dailyError();
      case 'WEEKLY': return this.weeklyError();
      case 'MONTHLY': return this.monthlyError();
      case 'ANNUAL': return this.annualError();
      case 'CHANGE': return this.changeError();
    }
  }

  private dailyError(): string | null {
    if (!this.captureEmptied) return 'La revisión diaria exige dejar la captura vacía.';
    if (!this.agendaChecked) return 'La revisión diaria exige comprobar la agenda de hoy.';
    return this.countError(this.filled(this.actions), 1, 3, 'La revisión diaria deja de 1 a 3 acciones para hoy.');
  }

  private weeklyError(): string | null {
    if (!this.calendarReviewed) return 'La revisión semanal exige marcar el calendario como revisado.';
    if (!this.tasksPruned) return 'La revisión semanal exige depurar las tareas.';
    if (!this.commitmentsTransferred) return 'La revisión semanal exige pasar los compromisos de correo o chat a tarea o evento.';
    return this.countError(this.filled(this.priorities), 1, 3, 'La revisión semanal deja como máximo 3 prioridades, y al menos una.');
  }

  private monthlyError(): string | null {
    if (!this.stageChange) return 'Responde si algún cambio de etapa pasó desapercibido.';
    const rows = this.projects.filter(row => row.name.trim() || row.decision);
    if (rows.length === 0) return 'La revisión mensual exige al menos un proyecto.';
    if (rows.some(row => !row.name.trim() || !row.decision)) {
      return 'Cada proyecto necesita un nombre y una decisión: continuar, corregir, pausar o cerrar.';
    }
    return null;
  }

  private annualError(): string | null {
    const rows = this.objectives.filter(row => row.objective.trim() || row.achievementCriterion.trim() || row.reviewOn);
    if (rows.length < 1 || rows.length > 5) return 'La revisión anual guarda pocos objetivos: entre 1 y 5.';
    if (rows.some(row => !row.objective.trim() || !row.achievementCriterion.trim() || !row.reviewOn)) {
      return 'Cada objetivo necesita su texto, su criterio de logro y la fecha en que se revisa.';
    }
    return null;
  }

  private changeError(): string | null {
    if (!this.changeDescription.trim()) return 'Describe el cambio que disparó el ajuste.';
    if (!this.whatNoLongerFits.trim()) return 'Escribe qué compromisos ya no caben.';
    const rows = this.adjustments.filter(row => row.name.trim() || row.decision);
    if (rows.length === 0) return 'Indica al menos una cosa que dejas, reduces o pospones.';
    if (rows.some(row => !row.name.trim() || !row.decision)) {
      return 'Cada ajuste debe decir qué cambias y si lo dejas, lo reduces o lo pospones.';
    }
    return null;
  }

  private filled(lines: string[]): string[] {
    return lines.map(line => line.trim()).filter(Boolean);
  }

  private countError(values: string[], min: number, max: number, message: string): string | null {
    if (values.length < min || values.length > max) return message;
    return null;
  }

  save(): void {
    const problem = this.localError();
    this.error.set(problem ?? '');
    if (problem || !this.kind()) return;
    this.saving.set(true);
    this.sistemas.ensureFamily().subscribe({
      next: familyId => {
        this.sistemas.saveReview(familyId, this.payload()).subscribe({
          next: review => {
            this.saved.set(review);
            this.saving.set(false);
          },
          error: err => {
            this.error.set(apiErrorMessage(err, 'No fue posible guardar la revisión.'));
            this.saving.set(false);
          }
        });
      },
      error: err => {
        this.error.set(err?.message || 'No se encontró una familia activa.');
        this.saving.set(false);
      }
    });
  }

  private payload(): unknown {
    const kind = this.kind()!;
    const base = { type: kind.code, reviewDate: this.reviewDate };
    const filled = (lines: string[]) => lines.map(line => line.trim()).filter(Boolean);
    switch (kind.code) {
      case 'DAILY':
        return { ...base, captureEmptied: true, agendaChecked: true, actions: filled(this.actions) };
      case 'WEEKLY':
        return {
          ...base,
          calendarReviewed: true,
          tasksPruned: true,
          commitmentsTransferred: true,
          priorities: filled(this.priorities)
        };
      case 'MONTHLY':
        return {
          ...base,
          stageChangeUnnoticed: this.stageChange === 'si',
          projects: this.projects
            .filter(row => row.name.trim() && row.decision)
            .map(row => ({ name: row.name.trim(), decision: row.decision }))
        };
      case 'ANNUAL':
        return {
          ...base,
          objectives: this.objectives
            .filter(row => row.objective.trim() && row.achievementCriterion.trim() && row.reviewOn)
            .map(row => ({
              objective: row.objective.trim(),
              achievementCriterion: row.achievementCriterion.trim(),
              reviewOn: row.reviewOn
            }))
        };
      case 'CHANGE':
        return {
          ...base,
          changeDescription: this.changeDescription.trim(),
          whatNoLongerFits: this.whatNoLongerFits.trim(),
          adjustments: this.adjustments
            .filter(row => row.name.trim() && row.decision)
            .map(row => ({ description: row.name.trim(), kind: row.decision }))
        };
    }
  }

  private resetDraft(): void {
    this.reviewDate = todayIso();
    this.captureEmptied = false;
    this.agendaChecked = false;
    this.actions = [''];
    this.calendarReviewed = false;
    this.tasksPruned = false;
    this.commitmentsTransferred = false;
    this.priorities = [''];
    this.projects = [{ name: '', decision: '' }];
    this.stageChange = '';
    this.objectives = [{ objective: '', achievementCriterion: '', reviewOn: '' }];
    this.changeDescription = '';
    this.whatNoLongerFits = '';
    this.adjustments = [{ name: '', decision: '' }];
  }
}
