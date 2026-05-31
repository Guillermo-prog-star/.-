import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type ErrorStep = 'detect' | 'feel' | 'understand' | 'action' | 'agreement' | 'followup' | 'learning';

interface ErrorRecord {
  id: string;
  missionFailed: string;
  step: ErrorStep;
  // feel
  feelings: string;
  // understand
  whatHappened: string;
  // action
  correctiveAction: string;
  whoHelps: string;
  // agreement
  agreement: string;
  followupDate: string;
  // learning
  learning: string;
  closed: boolean;
  createdAt: Date;
}

const STEPS: Array<{ key: ErrorStep; label: string; icon: string; question: string; color: string }> = [
  { key: 'detect',    label: 'Detectar',   icon: '🔍', question: '¿Qué misión o compromiso no se cumplió?', color: '#ef4444' },
  { key: 'feel',      label: 'Sentir',     icon: '💬', question: '¿Cómo se sintieron cuando ocurrió esto?', color: '#f59e0b' },
  { key: 'understand',label: 'Comprender', icon: '🧠', question: '¿Qué ocurrió realmente? ¿Por qué pasó?', color: '#6366f1' },
  { key: 'action',    label: 'Accionar',   icon: '⚡', question: '¿Qué podemos cambiar? ¿Quién ayudará?', color: '#10b981' },
  { key: 'agreement', label: 'Acordar',    icon: '🤝', question: 'Define el acuerdo familiar para superar esto.', color: '#818cf8' },
  { key: 'followup',  label: 'Seguimiento',icon: '📅', question: '¿Cuándo revisamos el cumplimiento del acuerdo?', color: '#3b82f6' },
  { key: 'learning',  label: 'Aprender',   icon: '✨', question: '¿Qué aprendimos como familia de este error?', color: '#fbbf24' },
];

@Component({
  selector: 'app-error-protocol',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ep-page">

      <div class="ep-header">
        <div class="ep-title">Gestión del Error Familiar</div>
        <div class="ep-sub">Detectar → Sentir → Comprender → Accionar → Acordar → Aprender</div>
        <button class="btn-new" (click)="startNew()">+ Nuevo protocolo de error</button>
      </div>

      <!-- Active protocol wizard -->
      @if (activeRecord()) {
        <div class="wizard">
          <!-- Step progress -->
          <div class="step-track">
            @for (s of steps; track s.key; let i = $index) {
              <div class="st-item" [class.active]="currentStepIndex() === i" [class.done]="isStepDone(i)">
                <div class="st-dot" [style.border-color]="isStepDone(i) ? '#10b981' : currentStepIndex() === i ? s.color : 'rgba(255,255,255,0.1)'">
                  {{ isStepDone(i) ? '✓' : (i + 1) }}
                </div>
                <div class="st-label">{{ s.label }}</div>
              </div>
              @if (i < steps.length - 1) {
                <div class="st-line" [class.done]="isStepDone(i)"></div>
              }
            }
          </div>

          <!-- Step content -->
          <div class="step-content" [style.border-color]="currentStep()?.color + '33'">
            <div class="sc-icon">{{ currentStep()?.icon }}</div>
            <div class="sc-question">{{ currentStep()?.question }}</div>

            @if (currentStep()?.key === 'detect') {
              <input class="sc-input" [(ngModel)]="activeRecord()!.missionFailed"
                placeholder="Ej: No cumplimos la cena familiar esta semana…" />
            }
            @if (currentStep()?.key === 'feel') {
              <textarea class="sc-textarea" rows="3" [(ngModel)]="activeRecord()!.feelings"
                placeholder="Ej: Me sentí frustrado, mi hijo se sintió ignorado…"></textarea>
            }
            @if (currentStep()?.key === 'understand') {
              <textarea class="sc-textarea" rows="3" [(ngModel)]="activeRecord()!.whatHappened"
                placeholder="Ej: El trabajo llegó tarde y nadie avisó a tiempo…"></textarea>
            }
            @if (currentStep()?.key === 'action') {
              <input class="sc-input" [(ngModel)]="activeRecord()!.correctiveAction"
                placeholder="¿Qué cambiaremos?" style="margin-bottom:10px" />
              <input class="sc-input" [(ngModel)]="activeRecord()!.whoHelps"
                placeholder="¿Quién será responsable de liderar el cambio?" />
            }
            @if (currentStep()?.key === 'agreement') {
              <textarea class="sc-textarea" rows="4" [(ngModel)]="activeRecord()!.agreement"
                placeholder="Redacta el acuerdo familiar: Nosotros, como familia, nos comprometemos a…"></textarea>
            }
            @if (currentStep()?.key === 'followup') {
              <input type="date" class="sc-input" [(ngModel)]="activeRecord()!.followupDate" />
            }
            @if (currentStep()?.key === 'learning') {
              <textarea class="sc-textarea" rows="3" [(ngModel)]="activeRecord()!.learning"
                placeholder="Ej: Aprendimos que la comunicación a tiempo evita muchos conflictos…"></textarea>
            }

            <div class="sc-actions">
              @if (currentStepIndex() > 0) {
                <button class="btn-back" (click)="prevStep()">← Atrás</button>
              }
              @if (currentStepIndex() < steps.length - 1) {
                <button class="btn-next" (click)="nextStep()">Siguiente →</button>
              } @else {
                <button class="btn-close" (click)="closeProtocol()">✓ Cerrar protocolo</button>
              }
            </div>
          </div>
        </div>
      }

      <!-- History -->
      @if (closedRecords().length > 0) {
        <div class="history-section">
          <div class="hs-title">📚 Historial de Protocolos Cerrados</div>
          @for (r of closedRecords(); track r.id) {
            <div class="history-card">
              <div class="hc-left">
                <div class="hc-mission">{{ r.missionFailed || 'Error sin título' }}</div>
                <div class="hc-date">{{ r.createdAt | date:'d MMM yyyy' }}</div>
              </div>
              <div class="hc-learning">{{ r.learning || 'Aprendizaje pendiente' }}</div>
              <span class="hc-badge">Cerrado</span>
            </div>
          }
        </div>
      }

    </div>
  `,
  styles: [`
    .ep-page  { max-width: 800px; margin: 0 auto; }
    .ep-header { margin-bottom: 28px; }
    .ep-title  { font-size: 22px; font-weight: 800; color: #fff; margin-bottom: 4px; }
    .ep-sub    { font-size: 13px; color: rgba(255,255,255,0.4); margin-bottom: 16px; }
    .btn-new   { font-size: 13px; font-weight: 700; padding: 10px 20px; background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.25); color: #f87171; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
    .btn-new:hover { background: rgba(239,68,68,0.22); }

    .wizard { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.07); border-radius: 16px; padding: 24px; margin-bottom: 28px; }

    .step-track { display: flex; align-items: center; margin-bottom: 28px; overflow-x: auto; padding-bottom: 4px; }
    .st-item  { display: flex; flex-direction: column; align-items: center; gap: 5px; flex-shrink: 0; }
    .st-dot   { width: 28px; height: 28px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 800; color: rgba(255,255,255,0.4); transition: all 0.3s; }
    .st-item.active .st-dot { color: #fff; background: rgba(99,102,241,0.2); }
    .st-item.done .st-dot   { color: #10b981; background: rgba(16,185,129,0.12); }
    .st-label { font-size: 9px; color: rgba(255,255,255,0.3); font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; white-space: nowrap; }
    .st-item.active .st-label { color: rgba(255,255,255,0.7); }
    .st-line  { flex: 1; height: 1px; background: rgba(255,255,255,0.07); min-width: 20px; margin: 0 4px; margin-bottom: 14px; }
    .st-line.done { background: rgba(16,185,129,0.35); }

    .step-content { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 24px; text-align: center; }
    .sc-icon     { font-size: 36px; margin-bottom: 12px; }
    .sc-question { font-size: 16px; font-weight: 700; color: rgba(255,255,255,0.85); margin-bottom: 20px; }
    .sc-input    { width: 100%; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; color: #fff; font-size: 14px; padding: 12px 16px; outline: none; font-family: inherit; box-sizing: border-box; }
    .sc-textarea { width: 100%; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; color: #fff; font-size: 14px; padding: 12px 16px; outline: none; resize: none; font-family: inherit; box-sizing: border-box; line-height: 1.5; }
    .sc-actions  { display: flex; justify-content: center; gap: 12px; margin-top: 20px; }
    .btn-back  { padding: 10px 20px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.5); border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 600; }
    .btn-next  { padding: 10px 24px; background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.3); color: #818cf8; border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 700; }
    .btn-next:hover  { background: rgba(99,102,241,0.25); }
    .btn-close { padding: 10px 24px; background: rgba(16,185,129,0.15); border: 1px solid rgba(16,185,129,0.3); color: #10b981; border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 700; }
    .btn-close:hover { background: rgba(16,185,129,0.25); }

    .history-section { margin-top: 8px; }
    .hs-title  { font-size: 14px; font-weight: 700; color: rgba(255,255,255,0.6); margin-bottom: 12px; }
    .history-card { display: flex; align-items: center; gap: 16px; padding: 14px 18px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; margin-bottom: 8px; }
    .hc-left   { flex-shrink: 0; }
    .hc-mission { font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.7); }
    .hc-date   { font-size: 11px; color: rgba(255,255,255,0.3); }
    .hc-learning { flex: 1; font-size: 12px; color: rgba(255,255,255,0.4); font-style: italic; }
    .hc-badge  { font-size: 10px; color: #10b981; background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.2); padding: 3px 8px; border-radius: 5px; flex-shrink: 0; }
  `]
})
export class ErrorProtocolComponent {
  readonly steps = STEPS;
  private _records    = signal<ErrorRecord[]>([]);
  private _activeId   = signal<string | null>(null);
  readonly _stepIndex = signal(0);

  readonly activeRecord  = computed(() =>
    this._records().find(r => r.id === this._activeId() && !r.closed) ?? null
  );
  readonly closedRecords = computed(() => this._records().filter(r => r.closed));
  readonly currentStepIndex = this._stepIndex.asReadonly();
  readonly currentStep = computed(() => STEPS[this._stepIndex()]);

  isStepDone(i: number): boolean {
    const r = this.activeRecord();
    if (!r) return false;
    const stepKey = STEPS[i].key;
    if (stepKey === 'detect')     return !!r.missionFailed;
    if (stepKey === 'feel')       return !!r.feelings;
    if (stepKey === 'understand') return !!r.whatHappened;
    if (stepKey === 'action')     return !!r.correctiveAction;
    if (stepKey === 'agreement')  return !!r.agreement;
    if (stepKey === 'followup')   return !!r.followupDate;
    return false;
  }

  startNew() {
    const id = Date.now().toString();
    const record: ErrorRecord = {
      id, missionFailed: '', step: 'detect', feelings: '',
      whatHappened: '', correctiveAction: '', whoHelps: '',
      agreement: '', followupDate: '', learning: '',
      closed: false, createdAt: new Date(),
    };
    this._records.update(r => [...r, record]);
    this._activeId.set(id);
    this._stepIndex.set(0);
  }

  nextStep() {
    if (this._stepIndex() < STEPS.length - 1) this._stepIndex.update(i => i + 1);
  }

  prevStep() {
    if (this._stepIndex() > 0) this._stepIndex.update(i => i - 1);
  }

  closeProtocol() {
    const id = this._activeId();
    if (!id) return;
    this._records.update(records =>
      records.map(r => r.id === id ? { ...r, closed: true } : r)
    );
    this._activeId.set(null);
    this._stepIndex.set(0);
  }
}
