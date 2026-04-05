import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
@Component({
  selector: 'app-plan-list-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div><h1>Planes de acción</h1><p>Acciones coordinadas para avanzar juntos</p></div>
    </div>
    @if (loading) { <div class="loading">Cargando planes...</div> }
    @else if (plans.length === 0) {
      <div class="card" style="text-align:center;padding:48px;">
        <div style="font-size:48px;margin-bottom:16px;">▦</div>
        <h2>No hay planes aún</h2>
        <p style="margin:8px 0 24px;">Los planes se generan automáticamente al finalizar una evaluación.</p>
        <a routerLink="/evaluations/start" class="btn btn-primary">Iniciar evaluación</a>
      </div>
    }
    @else {
      <div class="stack">
        @for (plan of plans; track plan.id) {
          <div class="card">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px;">
              <div>
                <h3 style="margin-bottom:4px;">{{ plan.title }}</h3>
                <span class="badge badge-green">{{ plan.status }}</span>
              </div>
              <div style="text-align:right;">
                <div style="font-size:22px;font-weight:700;color:var(--green);">{{ completedCount(plan) }}/{{ plan.tasks.length }}</div>
                <div class="muted" style="font-size:12px;">tareas</div>
              </div>
            </div>
            @if (plan.aiReport) {
              <div style="background:#EFF6FF;border-radius:10px;padding:12px 16px;margin-bottom:16px;font-size:13px;color:#1E40AF;line-height:1.6;">
                <strong>◉ Análisis IA:</strong> {{ plan.aiReport }}
              </div>
            }
            <div style="margin-bottom:16px;">
              <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:6px;">
                <span>Progreso</span><strong>{{ planPct(plan) }}%</strong>
              </div>
              <div class="progress-track"><div class="progress-fill" [style.width]="planPct(plan)+'%'"></div></div>
            </div>
            <div style="overflow-x:auto;">
              <table>
                <thead><tr><th>Tarea</th><th>Responsable</th><th>Estado</th></tr></thead>
                <tbody>
                  @for (task of plan.tasks; track task.id) {
                    <tr [style.opacity]="task.completed ? '.6' : '1'">
                      <td>
                        <div [style.text-decoration]="task.completed ? 'line-through' : 'none'" style="font-weight:600;font-size:13px;">{{ task.title }}</div>
                        <div class="muted" style="font-size:12px;">{{ task.description }}</div>
                      </td>
                      <td><span class="badge badge-gray">{{ task.assignedMemberName ?? 'Compartido' }}</span></td>
                      <td>
                        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">
                          <input type="checkbox" [checked]="task.completed"
                                 (change)="toggle(task.id, $any($event.target).checked)"
                                 style="width:16px;height:16px;accent-color:var(--green);"/>
                          {{ task.completed ? 'Completada' : 'Pendiente' }}
                        </label>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      </div>
    }`
})
export class PlanListPageComponent implements OnInit {
  private http = inject(HttpClient); private api = inject(ApiService);
  plans: Plan[] = []; loading = false;
  familyId = Number(localStorage.getItem('selectedFamilyId') ?? 1);
  ngOnInit() { this.load(); }
  load() {
    this.loading = true;
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.plans = data; this.loading = false; }, error: () => this.loading = false });
  }
  toggle(taskId: number, completed: boolean) {
    this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed })
      .subscribe({ next: () => this.load() });
  }
  completedCount(p: Plan) { return p.tasks.filter(t => t.completed).length; }
  planPct(p: Plan) { return p.tasks.length ? Math.round(this.completedCount(p) / p.tasks.length * 100) : 0; }
}
