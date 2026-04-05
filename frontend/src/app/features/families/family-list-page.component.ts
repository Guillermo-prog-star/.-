import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/api-response.model';
import { Family } from '../../core/models/models';
@Component({
  selector: 'app-family-list-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div><h1>Familias</h1><p>Selecciona el núcleo familiar de trabajo</p></div>
      <a routerLink="/families/create" class="btn btn-primary">+ Crear familia</a>
    </div>
    @if (loading) { <div class="loading">Cargando...</div> }
    @else {
      <div class="grid-3">
        @for (f of families; track f.id) {
          <div class="card" style="cursor:pointer" (click)="select(f)">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px;">
              <div style="width:44px;height:44px;background:var(--greenLt);border-radius:10px;display:grid;place-items:center;font-weight:700;color:var(--green);font-size:16px;">
                {{ f.name[0] }}
              </div>
              @if (isSelected(f)) { <span class="badge badge-green">Activa</span> }
            </div>
            <h3 style="margin-bottom:4px;">{{ f.name }}</h3>
            <p style="font-size:13px;margin-bottom:12px;">{{ f.description }}</p>
            <div style="font-size:11px;color:var(--muted);margin-bottom:4px;">{{ f.familyCode }}</div>
            <div style="font-size:12px;color:var(--muted);">{{ f.municipio }} · Hito: {{ f.currentMilestone }}</div>
            <button class="btn" style="width:100%;margin-top:14px;justify-content:center;font-size:13px;"
                    [style.background]="isSelected(f) ? 'var(--greenLt)' : ''"
                    [style.color]="isSelected(f) ? 'var(--green)' : ''">
              {{ isSelected(f) ? '✓ Familia seleccionada' : 'Usar esta familia' }}
            </button>
          </div>
        }
      </div>
    }`
})
export class FamilyListPageComponent implements OnInit {
  private http = inject(HttpClient); private api = inject(ApiService); private router = inject(Router);
  families: Family[] = []; loading = false;
  ngOnInit() {
    this.loading = true;
    this.http.get<ApiResponse<Family[]>>(`${this.api.base}/families`).subscribe({
      next: ({ data }) => { this.families = data; this.loading = false; },
      error: () => this.loading = false
    });
  }
  select(f: Family) {
    localStorage.setItem('selectedFamilyId', String(f.id));
    localStorage.setItem('selectedFamilyName', f.name);
    localStorage.setItem('selectedFamilyCode', f.familyCode ?? '');
    localStorage.setItem('currentMilestone', f.currentMilestone ?? 'inicio');
    this.router.navigate(['/dashboard']);
  }
  isSelected(f: Family) { return localStorage.getItem('selectedFamilyId') === String(f.id); }
}
