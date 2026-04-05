import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { EvaluationResultResponse } from '../../core/models/models';

@Component({
  selector: 'app-evaluation-result-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>Resultado de evaluación</h1>
        <p>Análisis generado por Integrity Family para el hito: {{ currentMilestone }}</p>
      </div>
    </div>

    @if (loading) { 
      <div class="loading">Calculando métricas en Armenia...</div> 
    }
    @else if (result) {
      <div class="grid-2" style="margin-bottom:20px;">
        
        <div class="card">
          <h3 style="margin-bottom:16px;">Estado General</h3>
          <div style="display:flex;align-items:center;gap:20px;margin-bottom:24px;">
            <div [style.background]="riskBg(result.riskLevel)" 
                 [style.border]="'4px solid ' + riskColor(result.riskLevel)"
                 style="width:90px;height:90px;border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center;">
              <span [style.color]="riskColor(result.riskLevel)" style="font-size:24px;font-weight:800;">{{ result.globalScore }}</span>
              <span [style.color]="riskColor(result.riskLevel)" style="font-size:10px;font-weight:600;">/100</span>
            </div>
            <div>
              <span class="badge" [class]="'risk-' + result.riskLevel" style="font-size:14px;padding:6px 14px;">
                Riesgo {{ riskLabel(result.riskLevel) }}
              </span>
              <p style="font-size:13px; margin-top:8px;">Basado en 408 puntos de control.</p>
            </div>
          </div>

          <div class="stack" style="gap:12px;">
            @for (dim of dims; track dim.key) {
              <div>
                <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;">
                  <span style="font-weight:700;padding:2px 8px;border-radius:12px;" [style.background]="dim.bg" [style.color]="dim.text">
                    {{ dim.label }}
                  </span>
                  <strong>{{ getScore(dim.key) }} / 5.0</strong>
                </div>
                <div class="progress-track" style="height:8px; background:#eee; border-radius:4px; overflow:hidden;">
                  <div class="progress-fill" 
                       [style.width.%]="(getScore(dim.key) / 5) * 100" 
                       [style.background]="dim.dot"
                       style="height:100%; transition: width 1s ease;">
                  </div>
                </div>
              </div>
            }
          </div>
        </div>

        <div class="card" style="border-left:4px solid #3B82F6; background: #F8FAFC;">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:14px;">
            <span class="badge" style="background:#DBEAFE; color:#1E40AF;">◉ Consultor IA</span>
            <span class="muted" style="font-size:11px;">Análisis de tendencia familiar</span>
          </div>
          @if (result.aiReport) {
            <p style="font-size:14px; color:#334155; line-height:1.7; font-style: italic;">"{{ result.aiReport }}"</p>
          } @else {
            <p class="muted">El análisis detallado está siendo procesado por Claude...</p>
          }
        </div>
      </div>

      <div style="display:flex;gap:12px;flex-wrap:wrap;margin-top:20px;">
        <a routerLink="/plans" class="btn btn-primary">Ver Plan de Acción →</a>
        <a routerLink="/dashboard" class="btn">Panel Principal</a>
      </div>
    }
  `
})
export class EvaluationResultPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  result: EvaluationResultResponse | null = null;
  loading = true;
  currentMilestone = localStorage.getItem('currentMilestone') || 'Inicio';

  dims = [
    { key:'scoreEmotions',      label:'Emociones',     bg:'#FDF2F8', text:'#9D174D', dot:'#EC4899' },
    { key:'scoreCommunication', label:'Comunicación',  bg:'#EFF6FF', text:'#1E40AF', dot:'#3B82F6' },
    { key:'scoreHabits',        label:'Hábitos',       bg:'#F0FDF4', text:'#166534', dot:'#22C55E' },
    { key:'scoreTimes',         label:'Tiempos',       bg:'#FFFBEB', text:'#92400E', dot:'#F59E0B' },
  ];

  ngOnInit() {
    // 1. Prioridad: Intentar obtener desde el estado de navegación (más rápido)
    const nav = window.history.state;
    if (nav?.result) {
      this.result = nav.result;
      this.loading = false;
    } else {
      // 2. Fallback: Cargar desde API si el usuario refresca la página (F5)
      const id = this.route.snapshot.paramMap.get('id');
      if (id) this.loadFromApi(id);
    }
  }

  loadFromApi(id: string) {
    this.http.get<any>(`${this.api.base}/analytics/results/${id}`).subscribe({
      next: ({ data }) => {
        this.result = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getScore(key: string): number {
    return (this.result as any)?.[key] ?? 0;
  }

  riskLabel(r: string) { return { LOW:'Bajo', MEDIUM:'Medio', HIGH:'Alto' }[r] ?? r; }
  riskBg(r: string)    { return { LOW:'#D1FAE5', MEDIUM:'#FEF3C7', HIGH:'#FEE2E2' }[r] ?? '#F3F4F6'; }
  riskColor(r: string) { return { LOW:'#065F46', MEDIUM:'#92400E', HIGH:'#991B1B' }[r] ?? '#374151'; }
}