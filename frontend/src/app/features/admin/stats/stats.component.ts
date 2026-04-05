import { Component, OnInit } from '@angular/core';
import { AssessmentService, QuestionStat } from '../../../core/services/assessment.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container mt-4">
      <h2 class="text-primary mb-4 text-center">📊 Auditoría de Datos: Nodo Armenia</h2>
      
      <div class="card shadow">
        <div class="card-header bg-dark text-white d-flex justify-content-between">
          <span>Resumen de Reactivos Cargados</span>
          <span class="badge bg-info">Meta: 100 por Área</span>
        </div>
        
        <div class="card-body p-0">
          <table class="table table-hover mb-0 text-center">
            <thead class="table-light text-uppercase small">
              <tr>
                <th>Dimensión</th>
                <th>Área de Evaluación</th>
                <th>Total</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let stat of stats">
                <td class="fw-bold">{{ stat.dimension }}</td>
                <td>{{ stat.area }}</td>
                <td>
                  <span class="badge" [ngClass]="getBadgeClass(stat.count)">
                    {{ stat.count }}
                  </span>
                </td>
                <td [ngStyle]="{'color': getStatusColor(stat.count)}">
                  <i class="fas" [ngClass]="stat.count >= 100 ? 'fa-check-circle' : 'fa-exclamation-triangle'"></i>
                  {{ stat.count >= 100 ? ' Completo' : ' Incompleto' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="mt-3 text-muted small">
        <p>* Los colores indican el cumplimiento de la meta de sembrado (Verde: ≥ 100, Amarillo: 50-99, Rojo: < 50).</p>
      </div>
    </div>
  `,
  styles: [`
    .table th { letter-spacing: 1px; }
    .badge { font-size: 0.9rem; padding: 0.5em 1em; min-width: 50px; }
  `]
})
export class StatsComponent implements OnInit {
  stats: QuestionStat[] = [];

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit(): void {
    this.assessmentService.getQuestionStats().subscribe({
      next: (data) => this.stats = data,
      error: (err) => console.error('Error en auditoría:', err)
    });
  }

  getBadgeClass(count: number): string {
    if (count >= 100) return 'bg-success text-white';
    if (count >= 50) return 'bg-warning text-dark';
    return 'bg-danger text-white';
  }

  getStatusColor(count: number): string {
    return count >= 100 ? '#198754' : (count >= 50 ? '#ffc107' : '#dc3545');
  }
}