import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';

import { FamilyStateService } from '../../core/services/family-state.service';

@Component({
  selector: 'app-evaluation-start-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluation-start-page.component.html',
  styleUrls: ['./evaluation-start-page.component.css']
})
export class EvaluationStartPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private router = inject(Router);
  private familyState = inject(FamilyStateService);

  members: Member[] = [];
  selectedMember: number | null = null;
  loading = false;

  get familyId() { return this.familyState.currentFamilyId(); }
  get milestone() { return localStorage.getItem('currentMilestone') ?? 'inicio'; }

  ngOnInit() {
    if (this.familyId > 0) {
      this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
        .subscribe({ 
          next: ({ data }: any) => this.members = data,
          error: (err: any) => console.error('Error cargando miembros:', err)
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
        next: (response: any) => {
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
        error: (err: any) => {
          this.loading = false;
          console.error('Error en POST /evaluations/start:', err);
          alert('No se pudo iniciar la evaluación. Revisa la consola.');
        }
      });
  }
}