import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';

@Component({
  selector: 'app-evaluation-start-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header"><div><h1>Nueva evaluación</h1><p>Hito actual: {{ milestone }}</p></div></div>
    <div class="card" style="max-width:480px;">
      <h3 style="margin-bottom:16px;">¿Quién responde esta evaluación?</h3>
      <div style="display:grid;gap:10px;margin-bottom:24px;">
        <label style="display:flex;align-items:center;gap:10px;padding:12px;border:2px solid var(--border);border-radius:10px;cursor:pointer;"
               [style.border-color]="!selectedMember ? 'var(--green)' : 'var(--border)'"
               [style.background]="!selectedMember ? 'var(--greenLt)' : ''">
          <input type="radio" [value]="null" [(ngModel)]="selectedMember" name="member"/>
          <div><div style="font-weight:600;">Evaluación familiar general</div><div class="muted">Todos los miembros participan</div></div>
        </label>
        @for (m of members; track m.id) {
          <label style="display:flex;align-items:center;gap:10px;padding:12px;border:2px solid var(--border);border-radius:10px;cursor:pointer;"
                 [style.border-color]="selectedMember===m.id ? 'var(--green)' : 'var(--border)'"
                 [style.background]="selectedMember===m.id ? 'var(--greenLt)' : ''">
            <input type="radio" [value]="m.id" [(ngModel)]="selectedMember" name="member"/>
            <div><div style="font-weight:600;">{{ m.fullName }}</div><div class="muted">{{ m.roleType }} · {{ m.age }} años</div></div>
          </label>
        }
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center;padding:13px;" (click)="start()" [disabled]="loading">
        {{ loading ? 'Iniciando...' : 'Iniciar evaluación →' }}
      </button>
    </div>`
})
export class EvaluationStartPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private router = inject(Router);

  members: Member[] = [];
  selectedMember: number | null = null;
  loading = false;

  get familyId() { return Number(localStorage.getItem('selectedFamilyId') ?? 0); }
  get milestone() { return localStorage.getItem('currentMilestone') ?? 'inicio'; }

  ngOnInit() {
    if (this.familyId > 0) {
      this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
        .subscribe({ 
          next: ({ data }) => this.members = data,
          error: (err) => console.error('Error cargando miembros:', err)
        });
    }
  }

  start() {
    this.loading = true;
    const payload = { 
      familyId: this.familyId,
      memberId: this.selectedMember 
    };

    console.log('Enviando petición de inicio:', payload);

    this.http.post<any>(`${this.api.base}/evaluations/start`, payload)
      .subscribe({
        next: (response) => {
          this.loading = false;
          console.log('Respuesta del servidor:', response);

          // Extraemos el ID dinámicamente
          const evalId = response.data ? response.data.id : response.id;

          if (evalId) {
            // ⚠️ VERIFICACIÓN DE RUTA: 
            // Según tu app.routes.ts, la ruta es 'evaluations/:id/form'
            console.log('Navegando a:', `/evaluations/${evalId}/form`);
            this.router.navigate(['/evaluations', evalId, 'form']);
          } else {
            console.error('No se recibió ID de evaluación');
          }
        },
        error: (err) => {
          this.loading = false;
          console.error('Error en POST /evaluations/start:', err);
          alert('No se pudo iniciar la evaluación. Revisa la consola.');
        }
      });
  }
}