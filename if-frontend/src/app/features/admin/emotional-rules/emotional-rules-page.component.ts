import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ScannerService } from '../../../core/services/scanner.service';
import { EmotionalRuleDto, EmotionalRuleRequest } from '../../../core/models/models';

type FilterStatus = 'all' | 'active' | 'inactive';

const EMPTY_FORM: EmotionalRuleRequest = {
  ruleKey: '', milestoneScope: '*', memberRole: '*',
  requiredSignals: [], temporalWindowDays: 14,
  projectionLabel: '', confidenceBase: 0.70, riskOutput: 'MODERADO'
};

@Component({
  selector: 'app-emotional-rules-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './emotional-rules-page.component.html',
  styleUrls: ['./emotional-rules-page.component.css']
})
export class EmotionalRulesPageComponent implements OnInit {
  private scanner = inject(ScannerService);

  readonly rules        = signal<EmotionalRuleDto[]>([]);
  readonly loading      = signal(true);
  readonly saving       = signal(false);
  readonly expandedId   = signal<number | null>(null);
  readonly showForm     = signal(false);
  readonly editingId    = signal<number | null>(null);
  readonly filterStatus = signal<FilterStatus>('all');
  readonly filterRole   = signal<string>('*');
  readonly signalInput  = signal('');

  // Form state
  form: EmotionalRuleRequest = { ...EMPTY_FORM, requiredSignals: [] };

  readonly filtered = computed(() => {
    let list = this.rules();
    const status = this.filterStatus();
    const role   = this.filterRole();
    if (status === 'active')   list = list.filter(r => r.active);
    if (status === 'inactive') list = list.filter(r => !r.active);
    if (role !== '*')          list = list.filter(r => r.memberRole === role || r.memberRole === '*');
    return list;
  });

  readonly activeCount   = computed(() => this.rules().filter(r =>  r.active).length);
  readonly inactiveCount = computed(() => this.rules().filter(r => !r.active).length);

  readonly roles = ['*', 'PADRE', 'MADRE', 'ADOLESCENTE', 'NINO'];
  readonly riskOptions = ['BAJO', 'MODERADO', 'ALTO', 'CRITICO'];

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.scanner.getRules().pipe(catchError(() => of([]))).subscribe(data => {
      this.rules.set(data);
      this.loading.set(false);
    });
  }

  toggle(rule: EmotionalRuleDto): void {
    const action  = rule.active ? 'desactivar' : 'activar';
    const impact  = rule.active
      ? 'Dejará de aplicarse en nuevas evaluaciones.'
      : 'Comenzará a aplicarse en nuevas evaluaciones.';
    if (!confirm(`¿${action.charAt(0).toUpperCase() + action.slice(1)} la regla "${rule.ruleKey}"?\n${impact}`)) return;

    this.scanner.toggleRule(rule.id).pipe(catchError(() => of(null))).subscribe(updated => {
      if (updated) {
        this.rules.update(list => list.map(r => r.id === updated.id ? updated : r));
      }
    });
  }

  toggleExpand(id: number): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
    if (this.editingId() !== null && this.editingId() !== id) this.cancelEdit();
  }

  openCreate(): void {
    this.form = { ...EMPTY_FORM, requiredSignals: [] };
    this.editingId.set(null);
    this.showForm.set(true);
    this.signalInput.set('');
  }

  openEdit(rule: EmotionalRuleDto): void {
    this.form = {
      ruleKey:            rule.ruleKey,
      milestoneScope:     rule.milestoneScope,
      memberRole:         rule.memberRole,
      requiredSignals:    [...rule.requiredSignals],
      temporalWindowDays: rule.temporalWindowDays,
      projectionLabel:    rule.projectionLabel ?? '',
      confidenceBase:     rule.confidenceBase,
      riskOutput:         rule.riskOutput ?? 'MODERADO'
    };
    this.editingId.set(rule.id);
    this.showForm.set(false);
    this.signalInput.set('');
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form = { ...EMPTY_FORM, requiredSignals: [] };
  }

  cancelCreate(): void {
    this.showForm.set(false);
    this.form = { ...EMPTY_FORM, requiredSignals: [] };
  }

  save(): void {
    if (!this.form.ruleKey?.trim()) return;
    this.saving.set(true);

    const editing = this.editingId();
    const req$ = editing
      ? this.scanner.updateRule(editing, this.form)
      : this.scanner.createRule(this.form);

    req$.pipe(catchError(() => of(null))).subscribe(result => {
      this.saving.set(false);
      if (!result) return;
      if (editing) {
        this.rules.update(list => list.map(r => r.id === result.id ? result : r));
        this.cancelEdit();
      } else {
        this.rules.update(list => [result, ...list]);
        this.cancelCreate();
      }
    });
  }

  addSignal(): void {
    const s = this.signalInput().trim().toLowerCase().replace(/\s+/g, '_');
    if (!s || this.form.requiredSignals?.includes(s)) return;
    this.form.requiredSignals = [...(this.form.requiredSignals ?? []), s];
    this.signalInput.set('');
  }

  removeSignal(s: string): void {
    this.form.requiredSignals = (this.form.requiredSignals ?? []).filter(x => x !== s);
  }

  onSignalKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); this.addSignal(); }
  }

  // ── Helpers visuales ──────────────────────────────────────────────────────

  riskClass(r: string | null | undefined): string {
    switch (r) {
      case 'BAJO':     return 'badge-bajo';
      case 'MODERADO': return 'badge-moderado';
      case 'ALTO':     return 'badge-alto';
      case 'CRITICO':  return 'badge-critico';
      default:         return 'badge-default';
    }
  }

  roleLabel(r: string): string {
    const map: Record<string,string> = {
      '*': 'Todos', PADRE: 'Padre', MADRE: 'Madre',
      ADOLESCENTE: 'Adolescente', NINO: 'Niño'
    };
    return map[r] ?? r;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  confidencePct(v: number): string {
    return (v * 100).toFixed(0) + '%';
  }

  confidenceColor(v: number): string {
    if (v >= 0.80) return '#34d399';
    if (v >= 0.60) return '#fbbf24';
    return '#f87171';
  }

  createdByClass(c: string): string {
    if (c === 'ADMIN') return 'by-admin';
    if (c === 'CLINICIAN_OVERRIDE') return 'by-clinician';
    return 'by-algo';
  }
}
