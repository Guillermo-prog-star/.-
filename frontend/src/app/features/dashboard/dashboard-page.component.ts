import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { DashboardSummary } from '../../core/models/models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css']
})
export class DashboardPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private assessmentService = inject(AssessmentService);
  private familyState = inject(FamilyStateService);

  summary: DashboardSummary | null = null;
  loading = false;
  totalQuestions = 0;
  systemHealthy = false;
  
  milestones = [
    { key: 'MES_00_DIAGNOSTICO_BASE', label: 'Inicio', icon: '📍' },
    { key: 'MES_03_PRIMEROS_CAMBIOS', label: '3 Meses', icon: '🌱' },
    { key: 'MES_06_CONSOLIDACION_INICIAL', label: '6 Meses', icon: '🌳' },
    { key: 'MES_12_PRIMERA_TRANSFORMACION', label: '12 Meses', icon: '🛡️' },
    { key: 'MES_18_PROFUNDIZACION', label: '18 Meses', icon: '🚀' },
    { key: 'MES_24_MADUREZ_SISTEMA', label: '24 Meses', icon: '💎' },
    { key: 'MES_30_CIERRE_SOSTENIMIENTO', label: '30 Meses', icon: '📊' },
    { key: 'MES_36_TRANSFORMACION_COMPLETA', label: 'Completa', icon: '🌟' }
  ];
  
  get familyId() { return this.familyState.currentFamilyId(); }
  get taskPct() { return this.summary?.totalPlanTasks ? Math.round((this.summary.completedPlanTasks / this.summary.totalPlanTasks) * 100) : 0; }
  get chkPct() { return this.summary?.totalChecklistItems ? Math.round((this.summary.completedChecklistItems / this.summary.totalChecklistItems) * 100) : 0; }

  constructor() {
    effect(() => {
      // Re-cargar datos automáticamente cuando cambie la familia
      if (this.familyId) {
        this.loadDashboardData();
      }
    });
  }

  ngOnInit() {
    // Primera carga garantizada por el effect() si hay ID, 
    // pero si no hay, al menos cargamos estadísticas
    if (!this.familyId) {
      this.loadStatsOnly();
    }
  }

  loadStatsOnly() {
    this.assessmentService.getQuestionStats().subscribe(stats => {
      this.totalQuestions = stats.reduce((acc: number, curr: any) => acc + curr.count, 0);
      this.systemHealthy = this.totalQuestions >= 1000;
    });
  }

  loadDashboardData() {
    this.loading = true;
    
    // Ejecutamos ambas peticiones en paralelo
    const requests = {
      stats: this.assessmentService.getQuestionStats(),
      family: this.http.get<any>(`${this.api.base}/analytics/dashboard/family/${this.familyId}`)
    };

    forkJoin(requests).subscribe({
      next: (res: any) => {
        // 1. Procesar estadísticas del banco
        this.totalQuestions = res.stats.reduce((acc: number, curr: any) => acc + curr.count, 0);
        this.systemHealthy = this.totalQuestions >= 1000;

        // 2. Procesar datos de la familia
        if (res.family) {
          this.summary = res.family.data;
        }
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getChartPath(): string {
    if (!this.summary?.riskHistory || this.summary.riskHistory.length < 2) return '';
    const history = [...this.summary.riskHistory].reverse();
    const width = 400;
    const height = 150;
    const step = width / (history.length - 1);
    
    return history.map((s: any, i: number) => {
      const x = i * step;
      const y = height - (s.globalScore * height / 100);
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ');
  }

  getRadarPoints(): string {
    // Si no hay datos, mostramos un cuadrado base
    if (!this.summary?.riskHistory || this.summary.riskHistory.length === 0) {
      return '100,50 150,100 100,150 50,100'; 
    }
    const latest = this.summary.riskHistory[0];
    const center = 100;
    const radius = 80;

    // 0: Arriba (Emociones), 1: Derecha (Comunicación), 2: Abajo (Hábitos), 3: Izquierda (Tiempos)
    const points = [
      { x: center, y: center - (radius * (latest.scoreEmotions || 50) / 100) },
      { x: center + (radius * (latest.scoreCommunication || 50) / 100), y: center },
      { x: center, y: center + (radius * (latest.scoreHabits || 50) / 100) },
      { x: center - (radius * (latest.scoreTimes || 50) / 100), y: center }
    ];

    return points.map(p => `${p.x},${p.y}`).join(' ');
  }

  isMilestoneActive(key: string): boolean {
    if (!this.summary) return false;
    const currentIdx = this.milestones.findIndex(m => m.key === this.summary?.currentMilestone);
    const itemIdx = this.milestones.findIndex(m => m.key === key);
    return itemIdx <= currentIdx;
  }

  riskLabel(r: string) { return { LOW: 'Bajo', MEDIUM: 'Medio', HIGH: 'Alto' }[r] ?? r; }
}