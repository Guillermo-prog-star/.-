import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AuthService } from '../../core/services/auth.service';
import { FamilyGratitudeService } from '../family-gratitude/family-gratitude.service';
import { DashboardDataService } from '../dashboard/services/dashboard-data.service';
import { FamilyGratitude } from '../family-gratitude/family-gratitude.model';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-portal-familiar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portal-familiar.component.html',
  styleUrl: './portal-familiar.component.css'
})
export class PortalFamiliarComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly familyState = inject(FamilyStateService);
  private readonly authService = inject(AuthService);
  private readonly gratitudeService = inject(FamilyGratitudeService);
  private readonly dashboardService = inject(DashboardDataService);

  // Estados locales
  familyId = 0;
  userFullName = '';
  roleLabel = '';
  familyName = '';

  checklistItems: any[] = [];
  gratitudeEntries: FamilyGratitude[] = [];
  behavioralEvents: any[] = [];
  ivrSummary: any = null;
  stats: any = null;

  // Formulario rápido de gratitud
  gratitudeForm = {
    toMember: '',
    description: ''
  };

  // Modal y formulario de reparación conductual
  selectedEventForRepair: any = null;
  showRepairModal = false;
  submittingRepair = false;
  repairForm = {
    description: ''
  };

  // Modal y formulario de nueva fricción conductual
  showFrictionModal = false;
  submittingFriction = false;
  frictionForm = {
    description: '',
    severity: 3
  };

  loading = false;
  submittingGratitude = false;
  gratitudeSuccess = false;
  errorMessage = '';

  ngOnInit(): void {
    const currentUser = this.authService.user();
    if (currentUser) {
      this.userFullName = currentUser.fullName;
      this.roleLabel = currentUser.role;
    }
    this.familyId = this.familyState.getSelectedFamilyId();
    this.familyName = localStorage.getItem('selectedFamilyName') || 'Familia';

    if (this.familyId) {
      this.loadAllData();
    } else {
      this.errorMessage = 'No se ha detectado ningún nodo familiar activo.';
    }
  }

  loadAllData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      checklist: this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`).pipe(
        catchError(() => of({ data: [] }))
      ),
      gratitudes: this.gratitudeService.findByFamily(this.familyId).pipe(
        catchError(() => of([]))
      ),
      events: this.http.get<any>(`${this.api.base}/family-behavioral-events/family/${this.familyId}`).pipe(
        catchError(() => of({ data: [] }))
      ),
      ivr: this.http.get<any>(`${this.api.base}/family-behavioral-events/family/${this.familyId}/ivr`).pipe(
        catchError(() => of({ data: null }))
      ),
      dashboard: this.dashboardService.fetchData(this.familyId).pipe(
        catchError(() => of(null))
      )
    }).subscribe({
      next: ({ checklist, gratitudes, events, ivr, dashboard }) => {
        this.checklistItems = checklist?.data || [];
        this.gratitudeEntries = gratitudes.slice(0, 3);
        this.behavioralEvents = events?.data || [];
        this.ivrSummary = ivr?.data || { familyId: this.familyId, totalConflicts: 0, repairedConflicts: 0, averageRepairTimeHours: 0.0, ivrScore: 100.0 };
        this.stats = dashboard;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Ocurrió un error sincronizando el portal familiar.';
        this.loading = false;
      }
    });
  }

  toggleChecklistItem(item: any): void {
    if (item.completed) return;

    this.http.put<any>(`${this.api.base}/checklist/${item.id}/complete`, { completedBy: this.userFullName })
      .subscribe({
        next: () => {
          item.completed = true;
          item.completedBy = this.userFullName;
          // Volver a cargar el dashboard silenciosamente para refrescar progreso
          this.dashboardService.fetchData(this.familyId).subscribe(data => {
            this.stats = data;
          });
        },
        error: () => {
          this.errorMessage = 'Fallo al completar la tarea diaria.';
        }
      });
  }

  submitQuickGratitude(): void {
    if (!this.gratitudeForm.toMember.trim() || !this.gratitudeForm.description.trim()) {
      return;
    }

    this.submittingGratitude = true;

    const request = {
      familyId: this.familyId,
      fromMember: this.userFullName,
      toMember: this.gratitudeForm.toMember,
      description: this.gratitudeForm.description
    };

    this.gratitudeService.create(request).subscribe({
      next: (newEntry) => {
        this.submittingGratitude = false;
        this.gratitudeSuccess = true;
        this.gratitudeForm.toMember = '';
        this.gratitudeForm.description = '';
        
        // Agregar al inicio del feed localmente
        this.gratitudeEntries.unshift(newEntry);
        if (this.gratitudeEntries.length > 3) {
          this.gratitudeEntries.pop();
        }

        setTimeout(() => {
          this.gratitudeSuccess = false;
        }, 3000);
      },
      error: () => {
        this.submittingGratitude = false;
        this.errorMessage = 'No se pudo enviar el agradecimiento.';
      }
    });
  }

  // Métodos del Módulo de Eventos Conductuales (IVR)
  openRepairModal(event: any): void {
    if (event.repairedAt) return;
    this.selectedEventForRepair = event;
    this.repairForm.description = '';
    this.showRepairModal = true;
  }

  closeRepairModal(): void {
    this.showRepairModal = false;
    this.selectedEventForRepair = null;
  }

  submitRepair(): void {
    if (!this.repairForm.description.trim()) return;

    this.submittingRepair = true;
    const body = {
      repairDescription: this.repairForm.description,
      repairedAt: new Date().toISOString()
    };

    this.http.put<any>(`${this.api.base}/family-behavioral-events/${this.selectedEventForRepair.id}/repair`, body)
      .subscribe({
        next: () => {
          this.submittingRepair = false;
          this.closeRepairModal();
          this.loadAllData();
        },
        error: () => {
          this.submittingRepair = false;
          this.errorMessage = 'Fallo al reportar la reparación conductual.';
        }
      });
  }

  openFrictionModal(): void {
    this.frictionForm.description = '';
    this.frictionForm.severity = 3;
    this.showFrictionModal = true;
  }

  closeFrictionModal(): void {
    this.showFrictionModal = false;
  }

  submitFriction(): void {
    if (!this.frictionForm.description.trim()) return;

    this.submittingFriction = true;
    const body = {
      familyId: this.familyId,
      description: this.frictionForm.description,
      severity: this.frictionForm.severity,
      occurredAt: new Date().toISOString()
    };

    this.http.post<any>(`${this.api.base}/family-behavioral-events`, body)
      .subscribe({
        next: () => {
          this.submittingFriction = false;
          this.closeFrictionModal();
          this.loadAllData();
        },
        error: () => {
          this.submittingFriction = false;
          this.errorMessage = 'No fue posible registrar el incidente de fricción.';
        }
      });
  }

  get completedCount(): number {
    return this.checklistItems.filter(i => i.completed).length;
  }

  get completionPercentage(): number {
    if (!this.checklistItems.length) return 0;
    return Math.round((this.completedCount / this.checklistItems.length) * 100);
  }
}
