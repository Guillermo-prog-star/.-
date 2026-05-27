import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Member } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

@Component({
  selector: 'app-evaluation-start-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NarrativeCompanionComponent],
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
  errorMessage = '';

  /** ID de la evaluación en estado STARTED (si existe). Permite reanudar. */
  pendingEvalId: number | null = null;

  get familyId() { return this.familyState.currentFamilyId(); }
  get milestone() { return this.familyState.currentMilestone() || 'inicio'; }

  ngOnInit() {
    if (this.familyId > 0) {
      // Cargar miembros
      this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
        .subscribe({
          next: ({ data }: any) => this.members = data,
          error: (err: any) => console.error('Error cargando miembros:', err)
        });

      // Verificar si existe una evaluación sin finalizar (STARTED)
      this.checkPendingEvaluation();
    }
  }

  /** Consulta el historial y detecta una evaluación en estado STARTED. */
  private checkPendingEvaluation(): void {
    this.http.get<any>(`${this.api.base}/assessments/family/${this.familyId}/history`).pipe(
      catchError(() => of(null))
    ).subscribe(response => {
      if (!response?.data) return;
      const history: any[] = response.data;
      const pending = history.find((e: any) => e.status === 'STARTED');
      if (pending) {
        this.pendingEvalId = pending.id;
        console.log(`[ASSESSMENT] Evaluación pendiente detectada: ID ${pending.id}`);
      }
    });
  }

  /** Reanuda la evaluación en curso sin crear una nueva. */
  resume(): void {
    if (this.pendingEvalId) {
      this.router.navigate(['/evaluations', this.pendingEvalId, 'form']);
    }
  }

  /** Descarta la evaluación pendiente y arranca una nueva. */
  discardAndStart(): void {
    this.pendingEvalId = null;
    this.start();
  }

  goToFamilies() {
    this.router.navigate(['/families']);
  }

  start() {
    this.errorMessage = '';
    if (this.familyId <= 0) {
      this.goToFamilies();
      return;
    }

    this.loading = true;
    const payload = {
      familyId: this.familyId,
      memberId: this.selectedMember
    };

    this.http.post<any>(`${this.api.base}/assessments/start`, payload)
      .subscribe({
        next: (response: any) => {
          this.loading = false;
          const evalId = response?.data?.id ?? response?.id;
          if (evalId) {
            this.router.navigate(['/evaluations', evalId, 'form']);
          } else {
            console.error('No se recibió ID de evaluación');
          }
        },
        error: (err: any) => {
          this.loading = false;
          this.errorMessage = err?.error?.message ?? 'No se pudo iniciar la evaluación. Intenta de nuevo.';
        }
      });
  }
}
