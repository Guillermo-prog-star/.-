import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { FamilyStateService } from '../../core/services/family-state.service';

interface Milestone {
  month: number;
  pillar: 'reconocimiento' | 'amor' | 'entrega';
  pillarColor: string;
  phase: string;
  missionExample: string;
  sprintDays: number;
  indicator: string;
  status: 'done' | 'active' | 'upcoming';
}

@Component({
  selector: 'app-transformation-route',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="route-page">

      <!-- Header -->
      <div class="route-header">
        <div class="rh-left">
          <div class="rh-title">Ruta de Transformación Familiar</div>
          <div class="rh-sub">36 meses · 3 pilares · Misión a misión</div>
        </div>
        <div class="rh-right">
          <div class="rh-family">{{ familyState.currentFamilyCode() || 'Sin familia' }}</div>
          <div class="rh-progress-wrap">
            <div class="rh-bar">
              <div class="rh-bar-fill" [style.width.%]="flow.progressPercent()"></div>
            </div>
            <span class="rh-pct">{{ flow.progressPercent() }}%</span>
          </div>
        </div>
      </div>

      <!-- Pillar tabs -->
      <div class="pillar-tabs">
        <button class="ptab" [class.active]="activePillar() === 'reconocimiento'" (click)="setPillar('reconocimiento')">
          <span class="ptab-icon">💛</span>
          <div>
            <div class="ptab-name">Reconocimiento</div>
            <div class="ptab-range">Meses 1–6</div>
          </div>
        </button>
        <button class="ptab" [class.active]="activePillar() === 'amor'" (click)="setPillar('amor')">
          <span class="ptab-icon">❤️</span>
          <div>
            <div class="ptab-name">Amor</div>
            <div class="ptab-range">Meses 7–18</div>
          </div>
        </button>
        <button class="ptab" [class.active]="activePillar() === 'entrega'" (click)="setPillar('entrega')">
          <span class="ptab-icon">💙</span>
          <div>
            <div class="ptab-name">Entrega</div>
            <div class="ptab-range">Meses 19–36</div>
          </div>
        </button>
      </div>

      <!-- Milestone timeline -->
      <div class="timeline">
        @for (m of visibleMilestones(); track m.month) {
          <div class="milestone-card" [class]="'ms-' + m.status" (click)="openMission(m)">
            <!-- connector line -->
            <div class="ms-connector"></div>

            <!-- Left: month badge -->
            <div class="ms-month-badge" [style.background]="m.status === 'done' ? 'rgba(16,185,129,0.15)' : m.status === 'active' ? 'rgba(99,102,241,0.2)' : 'rgba(255,255,255,0.04)'">
              <span class="ms-month-num">M{{ m.month }}</span>
              @if (m.status === 'done') { <span class="ms-check">✓</span> }
              @if (m.status === 'active') { <span class="ms-pulse"></span> }
            </div>

            <!-- Content -->
            <div class="ms-content">
              <div class="ms-phase">{{ m.phase }}</div>
              <div class="ms-mission">🎯 {{ m.missionExample }}</div>
              <div class="ms-meta">
                <span class="ms-sprint">Sprint {{ m.sprintDays }}d</span>
                <span class="ms-indicator">{{ m.indicator }}</span>
              </div>
            </div>

            <!-- Action -->
            <div class="ms-action">
              @if (m.status === 'active') {
                <button class="btn-active" [routerLink]="['/logbook']">
                  Ir a Bitácora →
                </button>
              } @else if (m.status === 'done') {
                <span class="badge-done">Completado</span>
              } @else {
                <span class="badge-upcoming">Próximamente</span>
              }
            </div>
          </div>
        }
      </div>

      <!-- Legend -->
      <div class="legend">
        <div class="leg-item"><span class="leg-dot done"></span> Completado</div>
        <div class="leg-item"><span class="leg-dot active"></span> Mes activo</div>
        <div class="leg-item"><span class="leg-dot upcoming"></span> Próximo</div>
      </div>

    </div>
  `,
  styles: [`
    .route-page { max-width: 860px; margin: 0 auto; }

    .route-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; gap: 20px; flex-wrap: wrap; }
    .rh-title { font-size: 22px; font-weight: 800; color: #fff; margin-bottom: 4px; }
    .rh-sub   { font-size: 13px; color: rgba(255,255,255,0.4); }
    .rh-family { font-size: 11px; color: #818cf8; font-weight: 700; text-align: right; margin-bottom: 6px; letter-spacing: 0.06em; }
    .rh-progress-wrap { display: flex; align-items: center; gap: 8px; }
    .rh-bar   { width: 160px; height: 6px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
    .rh-bar-fill { height: 100%; background: linear-gradient(90deg, #6366f1, #818cf8); border-radius: 3px; transition: width 0.5s; }
    .rh-pct   { font-size: 12px; color: rgba(255,255,255,0.5); font-weight: 700; }

    .pillar-tabs { display: flex; gap: 12px; margin-bottom: 28px; flex-wrap: wrap; }
    .ptab { display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-radius: 12px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07); color: rgba(255,255,255,0.5); cursor: pointer; transition: all 0.2s; flex: 1; min-width: 160px; }
    .ptab:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.15); }
    .ptab.active { background: rgba(99,102,241,0.12); border-color: rgba(99,102,241,0.35); color: #fff; }
    .ptab-icon { font-size: 22px; }
    .ptab-name { font-size: 13px; font-weight: 700; }
    .ptab-range { font-size: 11px; color: rgba(255,255,255,0.35); }
    .ptab.active .ptab-range { color: rgba(255,255,255,0.55); }

    .timeline { display: flex; flex-direction: column; gap: 0; position: relative; }

    .milestone-card {
      display: flex; align-items: center; gap: 16px; padding: 16px 20px;
      background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06);
      border-radius: 12px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; position: relative;
    }
    .milestone-card:hover { background: rgba(255,255,255,0.04); border-color: rgba(255,255,255,0.1); transform: translateX(4px); }
    .ms-active { background: rgba(99,102,241,0.06) !important; border-color: rgba(99,102,241,0.25) !important; }
    .ms-done   { opacity: 0.65; }

    .ms-month-badge { width: 52px; height: 52px; border-radius: 12px; display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; position: relative; }
    .ms-month-num { font-size: 12px; font-weight: 800; color: rgba(255,255,255,0.7); }
    .ms-check { font-size: 16px; color: #10b981; }
    .ms-pulse { width: 8px; height: 8px; border-radius: 50%; background: #6366f1; animation: pulse 1.5s infinite; margin-top: 2px; }
    @keyframes pulse { 0%,100% { box-shadow: 0 0 0 0 rgba(99,102,241,0.6); } 50% { box-shadow: 0 0 0 6px rgba(99,102,241,0); } }

    .ms-content { flex: 1; min-width: 0; }
    .ms-phase   { font-size: 10px; color: rgba(255,255,255,0.35); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 3px; }
    .ms-mission { font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.85); margin-bottom: 5px; }
    .ms-meta    { display: flex; gap: 10px; }
    .ms-sprint, .ms-indicator { font-size: 11px; color: rgba(255,255,255,0.35); background: rgba(255,255,255,0.04); padding: 2px 8px; border-radius: 4px; }

    .ms-action { flex-shrink: 0; }
    .btn-active  { background: rgba(99,102,241,0.2); border: 1px solid rgba(99,102,241,0.4); color: #818cf8; padding: 8px 14px; border-radius: 8px; font-size: 12px; font-weight: 700; cursor: pointer; white-space: nowrap; }
    .btn-active:hover { background: rgba(99,102,241,0.35); }
    .badge-done     { font-size: 11px; color: #10b981; background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.2); padding: 4px 10px; border-radius: 6px; }
    .badge-upcoming { font-size: 11px; color: rgba(255,255,255,0.25); background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07); padding: 4px 10px; border-radius: 6px; }

    .legend { display: flex; gap: 20px; margin-top: 24px; padding: 12px 0; border-top: 1px solid rgba(255,255,255,0.05); }
    .leg-item { display: flex; align-items: center; gap: 6px; font-size: 11px; color: rgba(255,255,255,0.4); }
    .leg-dot  { width: 8px; height: 8px; border-radius: 50%; }
    .leg-dot.done     { background: #10b981; }
    .leg-dot.active   { background: #6366f1; }
    .leg-dot.upcoming { background: rgba(255,255,255,0.15); }
  `]
})
export class TransformationRouteComponent {
  protected flow        = inject(TransformationFlowService);
  protected familyState = inject(FamilyStateService);
  private router        = inject(Router);

  readonly activePillar = signal<'reconocimiento' | 'amor' | 'entrega'>(
    this.flow.currentPillar()
  );

  private readonly ALL_MILESTONES: Milestone[] = this.buildMilestones();

  readonly visibleMilestones = computed(() => {
    const p = this.activePillar();
    return this.ALL_MILESTONES.filter(m => m.pillar === p);
  });

  setPillar(p: 'reconocimiento' | 'amor' | 'entrega') {
    this.activePillar.set(p);
  }

  openMission(m: Milestone) {
    if (m.status === 'active') {
      this.router.navigate(['/logbook']);
    }
  }

  private buildMilestones(): Milestone[] {
    const currentMonth = this.flow.currentMonth();

    const data: Array<[number, string, string, number, string]> = [
      // [mes, fase, misiónEjemplo, sprintDays, indicador]
      [1,  'Estabilización',           'Cena Familiar Consciente',        7,  '5 cenas/semana'],
      [2,  'Estabilización',           'Acuerdo de Pantallas',            7,  'Reglas definidas'],
      [3,  'Conciencia Inicial',       'Mapa de Emociones',               15, '10 registros/semana'],
      [4,  'Conciencia Inicial',       'Ritual de Buenos Días',           7,  '7 días consecutivos'],
      [5,  'Cimentación de Vínculos',  'Noche de Juegos en Familia',      15, '3 noches/mes'],
      [6,  'Cimentación de Vínculos',  'Carta de Reconocimiento',         7,  '1 carta/persona'],
      [7,  'Transformación Profunda',  'Conversación Sin Pantallas',      15, '2 conv/semana'],
      [8,  'Transformación Profunda',  'Proyecto Compartido',             15, 'Proyecto iniciado'],
      [9,  'Transformación Profunda',  'Acuerdo de Conflictos',           7,  'Protocolo documentado'],
      [10, 'Consolidación de Hábitos', 'Hábito de Gratitud',              7,  '21 días continuos'],
      [11, 'Consolidación de Hábitos', 'Lectura Familiar',                30, '1 libro/mes'],
      [12, 'Consolidación de Hábitos', 'Revisión de 6 meses',             7,  'Informe completado'],
      [13, 'Integridad Familiar',      'Misión Comunitaria',              30, 'Servicio realizado'],
      [14, 'Integridad Familiar',      'Finanzas Familiares',             15, 'Presupuesto definido'],
      [15, 'Integridad Familiar',      'Metas Personales de Cada Uno',    15, '1 meta/persona'],
      [16, 'Integridad Familiar',      'Viaje o Retiro Familiar',         30, 'Viaje planificado'],
      [17, 'Integridad Familiar',      'Evaluación de Hábitos',           7,  '80% sostenido'],
      [18, 'Integridad Familiar',      'Renovación de Compromisos',       7,  'Ceremonia realizada'],
      [19, 'Crecimiento Generacional', 'Historia Familiar Escrita',       30, 'Doc publicado'],
      [20, 'Crecimiento Generacional', 'Abuelos en el Proceso',           15, '1 sesión/abuelos'],
      [21, 'Crecimiento Generacional', 'Tradiciones Documentadas',        15, 'Lista de tradiciones'],
      [22, 'Crecimiento Generacional', 'Habilidades Transmitidas',        30, '1 habilidad/hijo'],
      [24, 'Legado Familiar',          'Constitución Familiar v1',        30, 'Doc firmado'],
      [27, 'Legado Familiar',          'Misión Familiar Declarada',       15, 'Declaración pública'],
      [30, 'Legado Familiar',          'Árbol Genealógico Digital',       30, 'Árbol completado'],
      [33, 'Trascendencia',            'Carta a Generaciones Futuras',    30, 'Cartas escritas'],
      [36, 'Trascendencia',            'Celebración de Transformación',   7,  'Ceremonia de cierre'],
    ];

    return data.map(([month, phase, mission, sprint, indicator]) => {
      const pillar: Milestone['pillar'] =
        month <= 6  ? 'reconocimiento' :
        month <= 18 ? 'amor' : 'entrega';

      const status: Milestone['status'] =
        month < currentMonth  ? 'done' :
        month === currentMonth ? 'active' : 'upcoming';

      return {
        month, pillar, phase,
        pillarColor: pillar === 'reconocimiento' ? '#fbbf24' : pillar === 'amor' ? '#ef4444' : '#3b82f6',
        missionExample: mission,
        sprintDays: sprint,
        indicator,
        status,
      };
    });
  }
}
