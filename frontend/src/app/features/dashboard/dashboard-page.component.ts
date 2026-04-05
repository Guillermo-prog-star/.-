import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { DashboardSummary } from '../../core/models/models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>Dashboard ejecutivo</h1>
        <p>Estado actual de {{ summary?.familyName ?? 'la familia' }}</p>
      </div>
      <div class="header-actions">
        <span class="badge" [class]="systemHealthy ? 'bg-success' : 'bg-warning'">
          {{ totalQuestions }} Reactivos en Sistema
        </span>
        @if (!summary) { <a routerLink="/families" class="btn">Seleccionar familia →</a> }
      </div>
    </div>

    @if (loading) { <div class="loading">Cargando datos del Nodo Armenia...</div> }
    
    @else if (!familyId) {
      <div class="card" style="text-align:center;padding:48px;">
        <div style="font-size:48px;margin-bottom:16px;">⌂</div>
        <h2>Selecciona una familia para comenzar</h2>
        <p style="margin:8px 0 24px;">Elige el núcleo familiar para visualizar su nivel de riesgo y planes.</p>
        <a routerLink="/families" class="btn btn-primary">Ir a Gestión de Familias</a>
      </div>
    }

    @else if (summary) {
      <div class="grid-4">
        <div class="card">
          <div class="stat-label">Miembros</div>
          <div class="stat-value">{{ summary.totalMembers }}</div>
        </div>
        <div class="card">
          <div class="stat-label">Evaluaciones</div>
          <div class="stat-value">{{ summary.totalEvaluations }}</div>
        </div>
        <div class="card">
          <div class="stat-label">Planes Activos</div>
          <div class="stat-value">{{ summary.totalPlans }}</div>
        </div>
        <div class="card">
          <div class="stat-label">Cumplimiento Checklist</div>
          <div class="stat-value">{{ summary.completedChecklistItems }}/{{ summary.totalChecklistItems }}</div>
        </div>
      </div>

      <div class="grid-2">
        <div class="card">
          <h3 style="margin-bottom:16px;">Riesgo familiar</h3>
          @if (summary.latestRiskLevel) {
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px;">
              <span class="badge" [class]="'risk-' + summary.latestRiskLevel">
                Riesgo {{ riskLabel(summary.latestRiskLevel) }}
              </span>
              <span style="font-size:28px;font-weight:800;">{{ summary.latestGlobalScore }}</span>
              <span class="muted">/ 100</span>
            </div>
            
            <div class="progress-section">
              <div class="progress-info"><span>Tareas del plan</span><strong>{{ taskPct }}%</strong></div>
              <div class="progress-track"><div class="progress-fill" [style.width.%]="taskPct"></div></div>
            </div>

            <div class="progress-section">
              <div class="progress-info"><span>Checklist Diario</span><strong>{{ chkPct }}%</strong></div>
              <div class="progress-track"><div class="progress-fill" [style.width.%]="chkPct" style="background:var(--amber);"></div></div>
            </div>
          } @else {
            <div style="text-align:center;padding:24px;color:var(--muted);">
              <p>No hay evaluaciones recientes para esta familia.</p>
              <a routerLink="/evaluations/start" class="btn btn-primary">Iniciar Diagnóstico</a>
            </div>
          }
        </div>

        <div class="card">
          <h3 style="margin-bottom:16px;">Acciones rápidas</h3>
          <div class="action-grid">
            <a routerLink="/evaluations/start" class="btn-action">◈ Nueva evaluación</a>
            <a routerLink="/plans" class="btn-action">▦ Ver planes activos</a>
            <a routerLink="/checklist" class="btn-action">☑ Checklist diario</a>
            <a routerLink="/chat" class="btn-action">◉ Consultar IA</a>
            <a routerLink="/members" class="btn-action">◑ Gestionar miembros</a>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .stat-label { font-size:12px; color:var(--muted); font-weight:600; text-transform:uppercase; letter-spacing:.05em; margin-bottom:8px; }
    .stat-value { font-size:28px; font-weight:700; }
    .progress-section { margin-bottom:15px; }
    .progress-info { display:flex; justify-content:space-between; font-size:13px; margin-bottom:4px; }
    .btn-action { display: flex; justify-content: flex-start; padding: 10px; border: 1px solid #eee; border-radius: 8px; text-decoration: none; color: inherit; transition: background 0.2s; }
    .btn-action:hover { background: #f8f9fa; }
  `]
})
export class DashboardPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private assessmentService = inject(AssessmentService);

  summary: DashboardSummary | null = null;
  loading = false;
  totalQuestions = 0;
  systemHealthy = false;
  familyId = Number(localStorage.getItem('selectedFamilyId') ?? 0);

  get taskPct() { return this.summary?.totalPlanTasks ? Math.round((this.summary.completedPlanTasks / this.summary.totalPlanTasks) * 100) : 0; }
  get chkPct() { return this.summary?.totalChecklistItems ? Math.round((this.summary.completedChecklistItems / this.summary.totalChecklistItems) * 100) : 0; }

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading = true;
    
    // Ejecutamos ambas peticiones en paralelo para optimizar la carga del Nodo Armenia
    const requests = {
      stats: this.assessmentService.getQuestionStats()
    };

    if (this.familyId) {
      (requests as any).family = this.http.get<any>(`${this.api.base}/analytics/dashboard/family/${this.familyId}`);
    }

    forkJoin(requests).subscribe({
      next: (res: any) => {
        // 1. Procesar estadísticas del banco
        this.totalQuestions = res.stats.reduce((acc: number, curr: any) => acc + curr.count, 0);
        this.systemHealthy = this.totalQuestions >= 1000;

        // 2. Procesar datos de la familia si existen
        if (res.family) {
          this.summary = res.family.data;
        }
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  riskLabel(r: string) { return { LOW: 'Bajo', MEDIUM: 'Medio', HIGH: 'Alto' }[r] ?? r; }
}