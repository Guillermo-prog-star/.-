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
  templateUrl: './evaluation-result-page.component.html',
  styleUrls: ['./evaluation-result-page.component.css']
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
      next: ({ data }: any) => {
        this.result = data;
        this.loading = false;
      },
      error: (err: any) => this.loading = false
    });
  }

  getScore(key: string): number {
    return (this.result as any)?.[key] ?? 0;
  }

  riskLabel(r: string) { return { LOW:'Bajo', MEDIUM:'Medio', HIGH:'Alto' }[r] ?? r; }
  riskBg(r: string)    { return { LOW:'#D1FAE5', MEDIUM:'#FEF3C7', HIGH:'#FEE2E2' }[r] ?? '#F3F4F6'; }
  riskColor(r: string) { return { LOW:'#065F46', MEDIUM:'#92400E', HIGH:'#991B1B' }[r] ?? '#374151'; }
}