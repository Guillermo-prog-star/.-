import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ChecklistItem, Plan } from '../../core/models/models';
@Component({
  selector: 'app-checklist-page', standalone: true, imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header"><div><h1>Checklist operativo</h1><p>Convierte el plan en acciones diarias concretas</p></div></div>
    <!-- Progreso -->
    <div class="card" style="margin-bottom:20px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
        <strong>Progreso semanal</strong>
        <span style="font-weight:700;color:var(--green);font-size:16px;">{{ done }}/{{ items.length }}</span>
      </div>
      <div class="progress-track"><div class="progress-fill" [style.width]="pct+'%'"></div></div>
      <div class="muted" style="margin-top:6px;font-size:12px;">{{ pct }}% completado</div>
    </div>
    <!-- Generar desde plan -->
    <div class="card" style="margin-bottom:20px;">
      <h3 style="margin-bottom:12px;">Generar checklist desde plan</h3>
      <div style="display:flex;gap:10px;flex-wrap:wrap;">
        <select [(ngModel)]="selectedPlan" name="sp" style="flex:1;min-width:200px;">
          <option [ngValue]="null">Selecciona un plan...</option>
          @for (p of plans; track p.id) { <option [ngValue]="p.id">{{ p.title }}</option> }
        </select>
        <button class="btn btn-primary" (click)="fromPlan()" [disabled]="!selectedPlan || genLoading">
          {{ genLoading ? 'Generando...' : 'Generar checklist' }}
        </button>
      </div>
    </div>
    <!-- Agregar manual -->
    <div class="card" style="margin-bottom:20px;display:flex;gap:10px;">
      <input [(ngModel)]="newTitle" name="nt" placeholder="Agregar nueva acción..." (keyup.enter)="addItem()" style="flex:1;"/>
      <button class="btn btn-primary" (click)="addItem()" [disabled]="!newTitle.trim()">+ Agregar</button>
    </div>
    <!-- Lista -->
    @if (loading) { <div class="loading">Cargando...</div> }
    @else if (items.length === 0) {
      <div class="card" style="text-align:center;padding:32px;color:var(--muted);">No hay ítems aún. Genera el checklist desde un plan o agrega uno manual.</div>
    }
    @else {
      <div class="card">
        <table>
          <thead><tr><th>Acción</th><th>Origen</th><th>Estado</th></tr></thead>
          <tbody>
            @for (item of items; track item.id) {
              <tr [style.opacity]="item.completed ? '.6' : '1'">
                <td [style.text-decoration]="item.completed ? 'line-through' : 'none'">{{ item.title }}</td>
                <td><span class="badge" [class]="item.planId ? 'badge-blue' : 'badge-gray'">{{ item.planId ? 'Del plan' : 'Manual' }}</span></td>
                <td>
                  <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:13px;">
                    <input type="checkbox" [checked]="item.completed"
                           (change)="toggle(item.id, $any($event.target).checked)"
                           style="width:16px;height:16px;accent-color:var(--green);"/>
                    {{ item.completed ? 'Listo' : 'Pendiente' }}
                  </label>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }`
})
export class ChecklistPageComponent implements OnInit {
  private http = inject(HttpClient); private api = inject(ApiService);
  items: ChecklistItem[] = []; plans: Plan[] = [];
  selectedPlan: number | null = null; newTitle = '';
  loading = false; genLoading = false;
  familyId = Number(localStorage.getItem('selectedFamilyId') ?? 1);
  get done() { return this.items.filter(i => i.completed).length; }
  get pct()  { return this.items.length ? Math.round(this.done / this.items.length * 100) : 0; }
  ngOnInit() { this.load(); this.loadPlans(); }
  load() {
    this.loading = true;
    this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.items = data; this.loading = false; }, error: () => this.loading = false });
  }
  loadPlans() {
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ next: ({ data }) => { this.plans = data; if (data.length && !this.selectedPlan) this.selectedPlan = data[0].id; } });
  }
  fromPlan() {
    if (!this.selectedPlan) return;
    this.genLoading = true;
    this.http.post<any>(`${this.api.base}/checklist/generate-from-plan`, { planId: this.selectedPlan })
      .subscribe({ next: () => { this.genLoading = false; this.load(); }, error: () => this.genLoading = false });
  }
  addItem() {
    if (!this.newTitle.trim()) return;
    this.http.post<any>(`${this.api.base}/checklist/items`, { familyId: this.familyId, title: this.newTitle.trim() })
      .subscribe({ next: () => { this.newTitle = ''; this.load(); } });
  }
  toggle(id: number, completed: boolean) {
    this.http.put<any>(`${this.api.base}/checklist/items/${id}/complete`, { completed })
      .subscribe({ next: () => this.load() });
  }
}
