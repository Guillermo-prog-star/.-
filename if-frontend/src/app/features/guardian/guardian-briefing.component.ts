import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GuardianService } from '../../core/services/guardian.service';
import { GuardianBriefingResponse } from '../../core/models/models';

@Component({
  selector: 'app-guardian-briefing',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="briefing-card">

  <!-- Header -->
  <div class="briefing-header">
    <div class="header-left">
      <span class="briefing-icon">{{ fatigueIcon }}</span>
      <div>
        <span class="briefing-title">Tu briefing diario</span>
        <span class="fatigue-badge" [class]="'fatigue-' + (briefing?.fatigueSignal ?? 'NONE').toLowerCase()">
          {{ fatigueLabel }}
        </span>
      </div>
    </div>
    <button class="refresh-btn" (click)="load()" [disabled]="loading" title="Actualizar">
      <span [class.spinning]="loading">↻</span>
    </button>
  </div>

  <!-- Loading -->
  <div class="loading-block" *ngIf="loading && !briefing">
    <div class="pulse-dot"></div>
    <span>El Mentor está analizando tu familia...</span>
  </div>

  <!-- Error -->
  <div class="error-block" *ngIf="error && !loading">
    <span>No se pudo cargar el briefing. Intenta más tarde.</span>
  </div>

  <!-- Contenido principal -->
  <ng-container *ngIf="briefing && !loading">

    <!-- Participación -->
    <div class="participation-bar">
      <div class="part-stat active">
        <span class="stat-num">{{ briefing.activeParticipants }}</span>
        <span class="stat-label">Activos esta semana</span>
      </div>
      <div class="part-divider"></div>
      <div class="part-stat inactive">
        <span class="stat-num">{{ briefing.inactiveParticipants }}</span>
        <span class="stat-label">Sin actividad</span>
      </div>
      <div class="part-divider"></div>
      <div class="part-stat plan">
        <span class="stat-num">{{ (briefing.planCompletionRate * 100) | number:'1.0-0' }}%</span>
        <span class="stat-label">Plan completado</span>
      </div>
    </div>

    <!-- Lista de miembros -->
    <div class="members-list">
      <div class="member-row" *ngFor="let m of briefing.members"
           [class.member-inactive]="!m.activeThisWeek">
        <div class="member-dot" [class.dot-active]="m.activeThisWeek" [class.dot-inactive]="!m.activeThisWeek"></div>
        <span class="member-name">{{ m.name }}</span>
        <span class="member-days" *ngIf="!m.activeThisWeek && m.daysSinceLastActivity < 999">
          {{ m.daysSinceLastActivity }}d sin actividad
        </span>
        <span class="member-days first-time" *ngIf="!m.activeThisWeek && m.daysSinceLastActivity >= 999">
          Sin actividad registrada
        </span>
        <span class="member-active" *ngIf="m.activeThisWeek">Activo ✓</span>
      </div>
    </div>

    <!-- Hito del plan -->
    <div class="milestone-row" *ngIf="briefing.currentMilestone">
      <span class="milestone-icon">🗺️</span>
      <span class="milestone-text">Hito actual: <strong>{{ briefing.currentMilestone }}</strong></span>
    </div>

    <!-- Mensaje IA -->
    <div class="ai-message">
      <div class="ai-badge">Mentor de Integridad</div>
      <p class="ai-text">{{ briefing.aiMessage }}</p>
    </div>

  </ng-container>

</div>
  `,
  styles: [`
    .briefing-card {
      background: linear-gradient(135deg, rgba(15,23,42,0.8) 0%, rgba(30,27,75,0.6) 100%);
      border: 1px solid rgba(139,92,246,0.25);
      border-radius: 16px;
      padding: 1.25rem;
      color: #e2e8f0;
    }

    /* Header */
    .briefing-header {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 1rem;
    }
    .header-left { display: flex; align-items: center; gap: 0.6rem; }
    .briefing-icon { font-size: 1.4rem; }
    .briefing-title { display: block; font-size: 0.7rem; text-transform: uppercase;
                      letter-spacing: 1px; color: #8b5cf6; margin-bottom: 0.2rem; }
    .fatigue-badge {
      font-size: 0.68rem; font-weight: 600; padding: 2px 8px;
      border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px;
    }
    .fatigue-none { background: rgba(16,185,129,0.15); color: #34d399; border: 1px solid rgba(16,185,129,0.3); }
    .fatigue-mild { background: rgba(251,191,36,0.15); color: #fbbf24; border: 1px solid rgba(251,191,36,0.3); }
    .fatigue-high { background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }

    .refresh-btn {
      background: transparent; border: 1px solid rgba(99,102,241,0.3);
      color: #818cf8; border-radius: 8px; width: 32px; height: 32px;
      cursor: pointer; font-size: 1rem; display: flex; align-items: center; justify-content: center;
      transition: all 0.15s;
    }
    .refresh-btn:hover { border-color: #818cf8; color: #c7d2fe; }
    .refresh-btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .spinning { display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Loading */
    .loading-block {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 1rem 0; color: #64748b; font-size: 0.85rem;
    }
    .pulse-dot {
      width: 10px; height: 10px; border-radius: 50%;
      background: #8b5cf6; flex-shrink: 0;
      animation: pulse 1.4s ease-in-out infinite;
    }
    @keyframes pulse { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.7); } }

    /* Error */
    .error-block { color: #f87171; font-size: 0.82rem; padding: 0.5rem 0; }

    /* Participación */
    .participation-bar {
      display: flex; align-items: center; gap: 0;
      background: rgba(15,23,42,0.5); border-radius: 12px;
      padding: 0.75rem; margin-bottom: 1rem;
    }
    .part-stat { flex: 1; text-align: center; }
    .part-divider { width: 1px; height: 36px; background: rgba(99,102,241,0.2); }
    .stat-num {
      display: block; font-size: 1.5rem; font-weight: 700; line-height: 1;
      margin-bottom: 0.2rem;
    }
    .active .stat-num   { color: #34d399; }
    .inactive .stat-num { color: #f87171; }
    .plan .stat-num     { color: #fbbf24; }
    .stat-label { font-size: 0.65rem; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; }

    /* Miembros */
    .members-list { margin-bottom: 0.85rem; display: flex; flex-direction: column; gap: 0.45rem; }
    .member-row {
      display: flex; align-items: center; gap: 0.5rem;
      padding: 0.4rem 0.6rem; border-radius: 8px;
      background: rgba(15,23,42,0.4);
      transition: background 0.15s;
    }
    .member-inactive { opacity: 0.75; }
    .member-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .dot-active   { background: #34d399; box-shadow: 0 0 6px rgba(52,211,153,0.5); }
    .dot-inactive { background: #475569; }
    .member-name  { flex: 1; font-size: 0.85rem; color: #cbd5e1; }
    .member-days  { font-size: 0.73rem; color: #f87171; white-space: nowrap; }
    .first-time   { color: #64748b; }
    .member-active{ font-size: 0.73rem; color: #34d399; white-space: nowrap; }

    /* Hito */
    .milestone-row {
      display: flex; align-items: center; gap: 0.5rem;
      font-size: 0.8rem; color: #94a3b8;
      margin-bottom: 0.85rem;
    }
    .milestone-row strong { color: #c7d2fe; }

    /* Mensaje IA */
    .ai-message {
      background: rgba(139,92,246,0.08);
      border: 1px solid rgba(139,92,246,0.2);
      border-radius: 12px;
      padding: 0.85rem 1rem;
    }
    .ai-badge {
      font-size: 0.65rem; text-transform: uppercase; letter-spacing: 1px;
      color: #8b5cf6; font-weight: 600; margin-bottom: 0.5rem;
    }
    .ai-text { margin: 0; font-size: 0.87rem; color: #cbd5e1; line-height: 1.6; white-space: pre-wrap; }
  `]
})
export class GuardianBriefingComponent implements OnChanges {

  @Input() familyId!: number;

  briefing?: GuardianBriefingResponse;
  loading = false;
  error = false;

  constructor(private guardianSvc: GuardianService) {}

  ngOnChanges() {
    if (this.familyId) this.load();
  }

  load() {
    if (!this.familyId) return;
    this.loading = true;
    this.error = false;
    this.guardianSvc.getBriefing(this.familyId).subscribe({
      next: b  => { this.briefing = b; this.loading = false; },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  get fatigueIcon(): string {
    if (!this.briefing) return '📊';
    return { NONE: '🌿', MILD: '⚡', HIGH: '🔴' }[this.briefing.fatigueSignal] ?? '📊';
  }

  get fatigueLabel(): string {
    if (!this.briefing) return '';
    return { NONE: 'Equilibrado', MILD: 'Carga moderada', HIGH: 'Fatiga detectada' }[this.briefing.fatigueSignal] ?? '';
  }
}
