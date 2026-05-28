import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GuardianService } from '../../../../core/services/guardian.service';
import { ParticipationPulseResponse, MemberPulse, DayActivity } from '../../../../core/models/models';

@Component({
  selector: 'app-participation-pulse',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="pulse-card" *ngIf="pulse">

  <!-- Header -->
  <div class="pulse-header">
    <div class="header-left">
      <span class="pulse-title">Pulso Familiar</span>
      <span class="pulse-subtitle">Últimos 7 días</span>
    </div>
    <div class="pulse-rate" [class.rate-low]="pulse.participationRate < 0.5">
      <span class="rate-num">{{ pulse.activeThisWeek }}</span>
      <span class="rate-sep">/</span>
      <span class="rate-total">{{ pulse.totalMembers }}</span>
      <span class="rate-label">activos</span>
    </div>
  </div>

  <!-- Barra de progreso de participación -->
  <div class="participation-track">
    <div class="participation-fill"
         [style.width.%]="pulse.participationRate * 100"
         [class.fill-low]="pulse.participationRate < 0.5"
         [class.fill-mid]="pulse.participationRate >= 0.5 && pulse.participationRate < 0.8"
         [class.fill-high]="pulse.participationRate >= 0.8">
    </div>
  </div>

  <!-- Avatares de miembros -->
  <div class="members-row">
    <div class="member-avatar-wrap" *ngFor="let m of pulse.members" [title]="memberTooltip(m)">
      <div class="member-avatar" [class.avatar-active]="m.activeThisWeek" [class.avatar-inactive]="!m.activeThisWeek">
        {{ m.initials }}
      </div>
      <div class="avatar-pulse-dot" [class.dot-on]="m.activeThisWeek" [class.dot-off]="!m.activeThisWeek"></div>
      <span class="avatar-name">{{ firstName(m.name) }}</span>
    </div>
  </div>

  <!-- Sparkline semanal -->
  <div class="sparkline-section">
    <div class="sparkline-bars">
      <div class="bar-col" *ngFor="let day of pulse.weeklyActivity">
        <div class="bar-wrap">
          <div class="bar-fill" [style.height.%]="barHeight(day)" [class.bar-active]="day.eventCount > 0"></div>
        </div>
        <span class="bar-label">{{ day.dayLabel }}</span>
      </div>
    </div>
  </div>

</div>

<!-- Skeleton mientras carga -->
<div class="pulse-card skeleton" *ngIf="!pulse && !error">
  <div class="sk-header"></div>
  <div class="sk-track"></div>
  <div class="sk-row">
    <div class="sk-avatar" *ngFor="let i of [1,2,3,4]"></div>
  </div>
  <div class="sk-bars">
    <div class="sk-bar" *ngFor="let i of [1,2,3,4,5,6,7]" [style.height.px]="[20,35,15,40,25,30,10][i-1]"></div>
  </div>
</div>
  `,
  styles: [`
    .pulse-card {
      background: rgba(15,23,42,0.6);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 16px;
      padding: 1.25rem;
      color: #e2e8f0;
    }

    /* Header */
    .pulse-header {
      display: flex; align-items: flex-start; justify-content: space-between;
      margin-bottom: 0.85rem;
    }
    .pulse-title {
      display: block; font-weight: 700; font-size: 0.95rem; color: #e2e8f0;
      margin-bottom: 0.1rem;
    }
    .pulse-subtitle { font-size: 0.7rem; color: #475569; text-transform: uppercase; letter-spacing: 1px; }
    .pulse-rate {
      display: flex; align-items: baseline; gap: 0.2rem;
      background: rgba(99,102,241,0.1); border-radius: 10px;
      padding: 0.3rem 0.7rem; border: 1px solid rgba(99,102,241,0.2);
    }
    .rate-num  { font-size: 1.4rem; font-weight: 800; color: #34d399; line-height: 1; }
    .rate-sep  { color: #334155; font-size: 1.1rem; }
    .rate-total{ font-size: 1.1rem; font-weight: 600; color: #475569; }
    .rate-label{ font-size: 0.65rem; color: #475569; margin-left: 0.3rem; text-transform: uppercase; }
    .pulse-rate.rate-low .rate-num { color: #f87171; }

    /* Track */
    .participation-track {
      height: 4px; background: rgba(99,102,241,0.1); border-radius: 2px;
      margin-bottom: 1.1rem; overflow: hidden;
    }
    .participation-fill {
      height: 100%; border-radius: 2px; transition: width 0.6s ease;
    }
    .fill-high { background: linear-gradient(90deg, #10b981, #34d399); }
    .fill-mid  { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
    .fill-low  { background: linear-gradient(90deg, #ef4444, #f87171); }

    /* Avatares */
    .members-row {
      display: flex; gap: 0.85rem; flex-wrap: wrap;
      margin-bottom: 1.1rem;
    }
    .member-avatar-wrap {
      display: flex; flex-direction: column; align-items: center; gap: 0.25rem;
      position: relative;
    }
    .member-avatar {
      width: 40px; height: 40px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.82rem;
      transition: all 0.2s;
    }
    .avatar-active {
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      color: white; box-shadow: 0 0 12px rgba(99,102,241,0.4);
    }
    .avatar-inactive {
      background: rgba(30,41,59,0.8); color: #475569;
      border: 1px solid rgba(71,85,105,0.3);
    }
    .avatar-pulse-dot {
      width: 8px; height: 8px; border-radius: 50%;
      position: absolute; top: -2px; right: -2px;
      border: 2px solid #0f172a;
    }
    .dot-on  { background: #34d399; }
    .dot-off { background: #475569; }
    .avatar-name {
      font-size: 0.65rem; color: #64748b; max-width: 44px;
      text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }

    /* Sparkline */
    .sparkline-section { border-top: 1px solid rgba(99,102,241,0.1); padding-top: 0.85rem; }
    .sparkline-bars {
      display: flex; gap: 0.35rem; align-items: flex-end; height: 48px;
    }
    .bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 0.25rem; }
    .bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; }
    .bar-fill {
      width: 100%; border-radius: 3px 3px 0 0; min-height: 3px;
      background: rgba(99,102,241,0.2); transition: height 0.4s ease;
    }
    .bar-fill.bar-active {
      background: linear-gradient(180deg, #6366f1, #4f46e5);
      box-shadow: 0 0 6px rgba(99,102,241,0.3);
    }
    .bar-label { font-size: 0.6rem; color: #334155; text-transform: uppercase; }

    /* Skeleton */
    .skeleton { animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0%,100% { opacity: 0.5; } 50% { opacity: 0.8; } }
    .sk-header  { height: 36px; background: rgba(99,102,241,0.08); border-radius: 8px; margin-bottom: 0.75rem; }
    .sk-track   { height: 4px;  background: rgba(99,102,241,0.08); border-radius: 2px; margin-bottom: 1rem; }
    .sk-row     { display: flex; gap: 0.75rem; margin-bottom: 1rem; }
    .sk-avatar  { width: 40px; height: 40px; border-radius: 12px; background: rgba(99,102,241,0.08); }
    .sk-bars    { display: flex; gap: 0.35rem; align-items: flex-end; height: 48px; }
    .sk-bar     { flex: 1; border-radius: 3px 3px 0 0; background: rgba(99,102,241,0.08); }
  `]
})
export class ParticipationPulseComponent implements OnChanges {

  @Input() familyId!: number;

  pulse?: ParticipationPulseResponse;
  error = false;

  private get maxEvents(): number {
    return Math.max(1, ...( this.pulse?.weeklyActivity.map(d => d.eventCount) ?? [1]));
  }

  constructor(private guardianSvc: GuardianService) {}

  ngOnChanges() {
    if (this.familyId && this.familyId > 0) this.load();
  }

  load() {
    this.guardianSvc.getParticipationPulse(this.familyId).subscribe({
      next: p  => this.pulse = p,
      error: () => this.error = true
    });
  }

  barHeight(day: DayActivity): number {
    return this.maxEvents > 0 ? (day.eventCount / this.maxEvents) * 100 : 0;
  }

  firstName(fullName: string): string {
    return fullName?.split(' ')[0] ?? '';
  }

  memberTooltip(m: MemberPulse): string {
    if (m.activeThisWeek) return `${m.name} — Activo esta semana`;
    if (m.daysSinceLastActivity >= 999) return `${m.name} — Sin actividad registrada`;
    return `${m.name} — ${m.daysSinceLastActivity} días sin actividad`;
  }
}
