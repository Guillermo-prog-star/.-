import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { GuardianService } from '../../core/services/guardian.service';
import { GuardianStatusResponse, MissionDto, MissionTemplate, MissionCategory } from '../../core/models/models';
import { GuardianBriefingComponent } from './guardian-briefing.component';

const MISSION_TEMPLATES: MissionTemplate[] = [
  { title: 'Cena sin pantallas',       description: 'Una comida juntos donde nadie usa el celular.', category: 'CONEXION',      durationMinutes: 60,  difficulty: 'FACIL', emoji: '🍽️' },
  { title: '15 minutos de escucha',    description: 'Cada miembro habla 2 minutos sin ser interrumpido.', category: 'COMUNICACION', durationMinutes: 20,  difficulty: 'FACIL', emoji: '👂' },
  { title: 'Día de gratitud',          description: 'Cada miembro dice algo por lo que agradece a otro.', category: 'GRATITUD',     durationMinutes: 30,  difficulty: 'FACIL', emoji: '🙏' },
  { title: 'Caminar juntos',           description: 'Una caminata familiar de 30 minutos.', category: 'HABITOS',      durationMinutes: 30,  difficulty: 'FACIL', emoji: '🚶' },
  { title: 'Historia familiar',        description: 'Alguien cuenta un recuerdo familiar especial.', category: 'MEMORIA',      durationMinutes: 30,  difficulty: 'FACIL', emoji: '📖' },
  { title: 'Abrazo colectivo',         description: 'Todos se abrazan durante 10 segundos.', category: 'CONEXION',      durationMinutes: 5,   difficulty: 'FACIL', emoji: '🤗' },
  { title: 'Apagón digital 2h',        description: 'Dos horas sin internet ni pantallas.', category: 'HABITOS',      durationMinutes: 120, difficulty: 'MEDIO', emoji: '📵' },
  { title: 'Acuerdo familiar',         description: 'Definir una norma de convivencia entre todos.', category: 'COMUNICACION', durationMinutes: 60,  difficulty: 'MEDIO', emoji: '🤝' },
];

@Component({
  selector: 'app-guardian-panel',
  standalone: true,
  imports: [CommonModule, GuardianBriefingComponent],
  template: `
<div class="guardian-panel" *ngIf="status">

  <!-- Sin Guardián -->
  <ng-container *ngIf="!status.hasGuardian">
    <div class="no-guardian">
      <span class="no-guardian-icon">🌱</span>
      <div class="no-guardian-text">
        <strong>Guardián Familiar no elegido</strong>
        <p>Elige quién guiará la evolución de tu familia</p>
      </div>
      <button class="btn-elect" (click)="goToElection()">Elegir →</button>
    </div>
  </ng-container>

  <!-- Con Guardián -->
  <ng-container *ngIf="status.hasGuardian">
    <div class="guardian-header">
      <div class="guardian-avatar">{{ getInitials(status.guardianFullName || '') }}</div>
      <div class="guardian-info">
        <span class="role-label">🌱 Guardián Familiar</span>
        <span class="guardian-name">{{ status.guardianFullName }}</span>
        <span class="guardian-since" *ngIf="status.guardianSince">
          Desde {{ status.guardianSince | date:'mediumDate' }}
        </span>
      </div>
      <div class="score-badge">
        <span class="score-value">{{ status.participationScore }}</span>
        <span class="score-label">pts</span>
      </div>
    </div>

    <!-- Misión activa -->
    <div class="mission-block" *ngIf="status.activeMission">
      <div class="mission-header">
        <span class="mission-emoji">🎯</span>
        <span class="mission-tag">Misión activa</span>
      </div>
      <div class="mission-title">{{ status.activeMission.title }}</div>
      <div class="mission-desc">{{ status.activeMission.description }}</div>
      <div class="mission-meta">
        <span class="mission-cat">{{ categoryLabel(status.activeMission.category) }}</span>
        <span class="mission-dur">⏱ {{ status.activeMission.durationMinutes }} min</span>
      </div>
      <button
        class="btn-complete"
        *ngIf="isGuardian && status.activeMission"
        [disabled]="completing"
        (click)="completeMission()">
        {{ completing ? '...' : '✅ Marcar completada (+10 pts)' }}
      </button>
    </div>

    <!-- Sin misión: sugerir una -->
    <div class="no-mission" *ngIf="!status.activeMission && isGuardian">
      <p>No hay misión activa. ¿Activamos una?</p>
      <div class="templates-grid">
        <button
          class="template-btn"
          *ngFor="let t of visibleTemplates"
          (click)="activateTemplate(t)">
          {{ t.emoji }} {{ t.title }}
        </button>
        <button class="template-btn see-more" (click)="showMore = !showMore">
          {{ showMore ? 'Ver menos' : 'Ver más...' }}
        </button>
      </div>
    </div>

    <!-- Progreso familiar -->
    <div class="progress-block">
      <div class="progress-header">
        <span>Evolución familiar</span>
        <span class="missions-count">{{ status.completedMissions }} misiones completadas</span>
      </div>
      <div class="evolution-bar">
        <div class="evolution-step" *ngFor="let step of evolutionSteps; let i = index"
             [class.reached]="i <= currentStep">
          <div class="step-dot"></div>
          <span class="step-label">{{ step }}</span>
        </div>
      </div>
    </div>

    <!-- Briefing diario: solo visible para el propio Guardián -->
    <div class="briefing-section" *ngIf="isGuardian">
      <app-guardian-briefing [familyId]="familyId"></app-guardian-briefing>
    </div>

  </ng-container>

</div>
  `,
  styles: [`
    .guardian-panel {
      background: rgba(15,23,42,0.6);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 16px;
      padding: 1.25rem;
      color: #e2e8f0;
    }

    /* Sin guardián */
    .no-guardian {
      display: flex; align-items: center; gap: 1rem;
    }
    .no-guardian-icon { font-size: 2rem; }
    .no-guardian-text { flex: 1; }
    .no-guardian-text strong { color: #c7d2fe; display: block; margin-bottom: 0.2rem; }
    .no-guardian-text p  { color: #64748b; font-size: 0.82rem; margin: 0; }
    .btn-elect {
      background: linear-gradient(135deg,#6366f1,#8b5cf6);
      color: white; border: none; border-radius: 8px;
      padding: 0.5rem 1rem; font-size: 0.85rem; cursor: pointer;
      white-space: nowrap;
    }

    /* Header del guardián */
    .guardian-header {
      display: flex; align-items: center; gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .guardian-avatar {
      width: 44px; height: 44px;
      background: linear-gradient(135deg,#6366f1,#8b5cf6);
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.9rem; color: white;
      flex-shrink: 0;
    }
    .guardian-info { flex: 1; display: flex; flex-direction: column; gap: 0.1rem; }
    .role-label    { font-size: 0.7rem; color: #818cf8; text-transform: uppercase; letter-spacing: 1px; }
    .guardian-name { font-weight: 600; color: #e2e8f0; font-size: 0.95rem; }
    .guardian-since{ font-size: 0.75rem; color: #475569; }
    .score-badge   { text-align: center; }
    .score-value   { display: block; font-size: 1.4rem; font-weight: 700; color: #fbbf24; line-height: 1; }
    .score-label   { font-size: 0.65rem; color: #64748b; text-transform: uppercase; }

    /* Misión activa */
    .mission-block {
      background: rgba(99,102,241,0.08);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 12px;
      padding: 1rem;
      margin-bottom: 1rem;
    }
    .mission-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
    .mission-emoji  { font-size: 1.1rem; }
    .mission-tag    { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px; color: #818cf8; }
    .mission-title  { font-weight: 600; color: #c7d2fe; margin-bottom: 0.3rem; }
    .mission-desc   { color: #94a3b8; font-size: 0.82rem; margin-bottom: 0.5rem; }
    .mission-meta   { display: flex; gap: 0.75rem; font-size: 0.75rem; margin-bottom: 0.75rem; }
    .mission-cat    { background: rgba(99,102,241,0.2); padding: 2px 8px; border-radius: 20px; color: #a5b4fc; }
    .mission-dur    { color: #64748b; }
    .btn-complete {
      width: 100%; padding: 0.6rem;
      background: linear-gradient(135deg,#10b981,#059669);
      color: white; border: none; border-radius: 8px;
      font-size: 0.85rem; font-weight: 600; cursor: pointer;
    }
    .btn-complete:disabled { opacity: 0.5; cursor: not-allowed; }

    /* Sugerencia de misiones */
    .no-mission p { color: #64748b; font-size: 0.85rem; margin-bottom: 0.75rem; }
    .templates-grid {
      display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1rem;
    }
    .template-btn {
      background: rgba(15,23,42,0.8);
      border: 1px solid rgba(99,102,241,0.25);
      color: #94a3b8; border-radius: 20px;
      padding: 0.35rem 0.85rem; font-size: 0.8rem;
      cursor: pointer; transition: all 0.15s;
    }
    .template-btn:hover { border-color: #818cf8; color: #c7d2fe; }
    .see-more { color: #475569; }

    /* Barra de evolución */
    .progress-block { margin-top: 0.5rem; }
    .progress-header {
      display: flex; justify-content: space-between;
      font-size: 0.75rem; color: #64748b; margin-bottom: 0.6rem;
    }
    .missions-count { color: #818cf8; }
    .evolution-bar  { display: flex; align-items: center; gap: 0; }
    .evolution-step {
      flex: 1; text-align: center; position: relative;
    }
    .evolution-step:not(:last-child)::after {
      content: '';
      position: absolute;
      top: 6px; right: -50%;
      width: 100%; height: 2px;
      background: rgba(99,102,241,0.15);
    }
    .evolution-step.reached:not(:last-child)::after { background: #6366f1; }
    .step-dot {
      width: 12px; height: 12px;
      border-radius: 50%;
      background: rgba(99,102,241,0.2);
      border: 2px solid rgba(99,102,241,0.3);
      margin: 0 auto 0.3rem;
    }
    .evolution-step.reached .step-dot {
      background: #6366f1;
      border-color: #818cf8;
      box-shadow: 0 0 8px rgba(99,102,241,0.5);
    }
    .step-label { font-size: 0.6rem; color: #475569; white-space: nowrap; }
    .evolution-step.reached .step-label { color: #818cf8; }

    /* Briefing */
    .briefing-section { margin-top: 1rem; border-top: 1px solid rgba(99,102,241,0.15); padding-top: 1rem; }
  `]
})
export class GuardianPanelComponent implements OnInit, OnChanges {

  @Input() familyId!: number;
  @Input() currentMemberId?: number;

  status?: GuardianStatusResponse;
  showMore = false;
  completing = false;

  evolutionSteps = ['Fragmentada', 'Conectando', 'Estable', 'Unida', 'Consciente'];

  constructor(private guardianSvc: GuardianService, private router: Router) {}

  ngOnInit() { this.load(); }
  ngOnChanges() { if (this.familyId) this.load(); }

  load() {
    if (!this.familyId || this.familyId === 0) return;
    this.guardianSvc.getStatus(this.familyId, this.currentMemberId).subscribe({
      next: s => this.status = s,
      error: () => {}
    });
  }

  get isGuardian(): boolean {
    return !!this.currentMemberId &&
           !!this.status?.hasGuardian &&
           this.status.guardianMemberId === this.currentMemberId;
  }

  get currentStep(): number {
    const pts = this.status?.participationScore ?? 0;
    if (pts >= 80) return 4;
    if (pts >= 50) return 3;
    if (pts >= 30) return 2;
    if (pts >= 10) return 1;
    return 0;
  }

  get visibleTemplates(): MissionTemplate[] {
    return this.showMore ? MISSION_TEMPLATES : MISSION_TEMPLATES.slice(0, 4);
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(p => p[0] ?? '').join('').toUpperCase();
  }

  categoryLabel(cat: MissionCategory): string {
    const map: Record<MissionCategory, string> = {
      CONEXION: '🔗 Conexión', COMUNICACION: '💬 Comunicación',
      GRATITUD: '🙏 Gratitud', HABITOS: '💪 Hábitos',
      MEMORIA: '📖 Memoria', REFLEXION: '🧠 Reflexión', BIENESTAR: '🧘 Bienestar'
    };
    return map[cat] ?? cat;
  }

  activateTemplate(t: MissionTemplate) {
    if (!this.familyId || !this.status?.guardianMemberId) return;
    this.guardianSvc.activateMission(this.familyId, {
      title: t.title,
      description: t.description,
      category: t.category,
      durationMinutes: t.durationMinutes,
      guardianMemberId: this.status.guardianMemberId
    }).subscribe({ next: () => this.load() });
  }

  completeMission() {
    if (!this.status?.activeMission || !this.status.guardianMemberId) return;
    this.completing = true;
    this.guardianSvc.completeMission(
      this.familyId,
      this.status.activeMission.id,
      this.status.guardianMemberId
    ).subscribe({
      next: () => { this.completing = false; this.load(); },
      error: () => { this.completing = false; }
    });
  }

  goToElection() {
    const params = this.currentMemberId ? { memberId: this.currentMemberId } : {};
    this.router.navigate(['/guardian', this.familyId, 'election'], { queryParams: params });
  }
}
