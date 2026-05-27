import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AuthService } from '../../core/services/auth.service';
import { FamilyGratitudeService } from '../family-gratitude/family-gratitude.service';
import { DashboardDataService } from '../dashboard/services/dashboard-data.service';
import { FamilyGratitude } from '../family-gratitude/family-gratitude.model';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import { GuardianPanelComponent } from '../guardian/guardian-panel.component';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-portal-familiar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NarrativeCompanionComponent, GuardianPanelComponent],
  templateUrl: './portal-familiar.component.html',
  styleUrl: './portal-familiar.component.css'
})
export class PortalFamiliarComponent implements OnInit, OnDestroy {
  private readonly http           = inject(HttpClient);
  private readonly api            = inject(ApiService);
  private readonly familyState    = inject(FamilyStateService);
  private readonly authService    = inject(AuthService);
  private readonly gratitudeService = inject(FamilyGratitudeService);
  private readonly dashboardService = inject(DashboardDataService);
  private readonly router         = inject(Router);

  // ── Identidad ─────────────────────────────────────────────────────────────
  familyId     = 0;
  userFullName = '';
  roleLabel    = '';
  familyName   = '';

  // ── Reloj en vivo ─────────────────────────────────────────────────────────
  currentTime  = '';
  private clockInterval: any;

  // ── Datos de la familia ───────────────────────────────────────────────────
  members:          any[]           = [];
  checklistItems:   any[]           = [];
  gratitudeEntries: FamilyGratitude[] = [];
  behavioralEvents: any[]           = [];
  ivrSummary:       any             = null;
  stats:            any             = null;

  // ── Formulario rápido de gratitud ─────────────────────────────────────────
  gratitudeForm = { toMember: '', description: '' };

  // ── Modal de reparación conductual ───────────────────────────────────────
  selectedEventForRepair: any  = null;
  showRepairModal              = false;
  submittingRepair             = false;
  repairForm                   = { description: '' };

  // ── Modal de nueva fricción ───────────────────────────────────────────────
  showFrictionModal    = false;
  submittingFriction   = false;
  frictionForm         = { description: '', severity: 3 };

  // ── Estados de UI ─────────────────────────────────────────────────────────
  loading             = false;
  submittingGratitude = false;
  gratitudeSuccess    = false;
  errorMessage        = '';

  // ── Getters para el panel del Guardián ───────────────────────────────────
  get guardianFamilyId(): number {
    return this.familyId ?? 0;
  }

  get guardianMemberId(): number | undefined {
    return this.familyState.currentMemberId() ?? undefined;
  }

  // ── Etiqueta de rol legible ───────────────────────────────────────────────
  get roleBadge(): string {
    if (this.roleLabel === 'ADMIN') return 'Gestor';
    if (this.roleLabel === 'SENTINEL') return 'Sentinel';
    return 'Miembro';
  }

  // ── Progreso del checklist ────────────────────────────────────────────────
  get completedCount(): number {
    return this.checklistItems.filter(i => i.completed).length;
  }

  get completionPercentage(): number {
    if (!this.checklistItems.length) return 0;
    return Math.round((this.completedCount / this.checklistItems.length) * 100);
  }

  // ── Horas con 1 decimal ───────────────────────────────────────────────────
  get ivrRepairHours(): string {
    const h = this.ivrSummary?.averageRepairTimeHours ?? 0;
    return Number(h).toFixed(1);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 60_000);

    const currentUser = this.authService.user();
    if (currentUser) {
      this.userFullName = currentUser.fullName;
      this.roleLabel    = currentUser.role;
    }
    this.familyId   = this.familyState.getSelectedFamilyId();
    this.familyName = this.familyState.currentFamilyName() || 'Familia';

    if (this.familyId) {
      this.loadAllData();
    } else {
      this.errorMessage = 'No se ha detectado ningún nodo familiar activo.';
    }
  }

  ngOnDestroy(): void {
    clearInterval(this.clockInterval);
  }

  private updateClock(): void {
    const now = new Date();
    this.currentTime = now.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  // ── Carga de datos ────────────────────────────────────────────────────────
  loadAllData(): void {
    this.loading      = true;
    this.errorMessage = '';

    forkJoin({
      family: this.http.get<any>(`${this.api.base}/families/${this.familyId}`).pipe(catchError(() => of(null))),
      checklist: this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`).pipe(catchError(() => of({ data: [] }))),
      gratitudes: this.gratitudeService.findByFamily(this.familyId).pipe(catchError(() => of([]))),
      events: this.http.get<any>(`${this.api.base}/family-behavioral-events/family/${this.familyId}`).pipe(catchError(() => of({ data: [] }))),
      ivr: this.http.get<any>(`${this.api.base}/family-behavioral-events/family/${this.familyId}/ivr`).pipe(catchError(() => of({ data: null }))),
      dashboard: this.dashboardService.fetchData(this.familyId).pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ family, checklist, gratitudes, events, ivr, dashboard }) => {
        // Miembros del núcleo familiar para selector
        const familyData = family?.data ?? family;
        this.members = familyData?.members ?? [];

        // Pre-seleccionar currentMemberId en gratitud si ya se conoce
        if (!this.gratitudeForm.toMember && this.members.length > 0) {
          const authUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
          const matched  = this.members.find((m: any) => m.email === authUser.email);
          // resolver currentMemberId si aún no está
          if (matched?.id && !this.familyState.currentMemberId()) {
            this.familyState.setMemberId(matched.id);
          }
        }

        this.checklistItems   = checklist?.data || [];
        this.gratitudeEntries = gratitudes.slice(0, 3);
        this.behavioralEvents = events?.data || [];
        this.ivrSummary       = ivr?.data ?? {
          familyId: this.familyId, totalConflicts: 0, repairedConflicts: 0,
          averageRepairTimeHours: 0.0, ivrScore: 100.0
        };
        this.stats   = dashboard;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Ocurrió un error sincronizando el portal familiar.';
        this.loading      = false;
      }
    });
  }

  // ── Checklist ─────────────────────────────────────────────────────────────
  toggleChecklistItem(item: any): void {
    if (item.completed) return;

    this.http.put<any>(`${this.api.base}/checklist/${item.id}/complete`, { completedBy: this.userFullName })
      .subscribe({
        next: () => {
          item.completed   = true;
          item.completedBy = this.userFullName;
          this.dashboardService.fetchData(this.familyId).subscribe(data => { this.stats = data; });
        },
        error: () => { this.errorMessage = 'Fallo al completar la tarea diaria.'; }
      });
  }

  injectSentinelMission(): void {
    const demoMissions = [
      { description: 'Cena sin celulares: Cenar juntos durante 15 minutos sin ningún dispositivo móvil.', dimension: 'Comunicación', source: 'SENTINEL' },
      { description: 'Reconocimiento sincero: Dedicar 5 minutos al final del día para agradecerse mutuamente.', dimension: 'Amor', source: 'SENTINEL' },
      { description: 'Cartel de responsabilidades: Diseñar juntos un cartel visual de roles y tareas domésticas.', dimension: 'Entrega', source: 'SENTINEL' }
    ];
    const body = demoMissions[Math.floor(Math.random() * demoMissions.length)];
    this.http.post<any>(`${this.api.base}/checklist/family/${this.familyId}`, body)
      .subscribe({
        next: () => this.loadAllData(),
        error: () => { this.errorMessage = 'No se pudo inyectar la micro-misión interactiva.'; }
      });
  }

  // ── Gratitud ──────────────────────────────────────────────────────────────
  selectGratitudeMember(name: string): void {
    this.gratitudeForm.toMember = this.gratitudeForm.toMember === name ? '' : name;
  }

  submitQuickGratitude(): void {
    if (!this.gratitudeForm.toMember.trim() || !this.gratitudeForm.description.trim()) return;

    this.submittingGratitude = true;

    this.gratitudeService.create({
      familyId:    this.familyId,
      fromMember:  this.userFullName,
      toMember:    this.gratitudeForm.toMember,
      description: this.gratitudeForm.description
    }).subscribe({
      next: (newEntry) => {
        this.submittingGratitude        = false;
        this.gratitudeSuccess           = true;
        this.gratitudeForm.toMember     = '';
        this.gratitudeForm.description  = '';
        this.gratitudeEntries.unshift(newEntry);
        if (this.gratitudeEntries.length > 3) this.gratitudeEntries.pop();
        setTimeout(() => { this.gratitudeSuccess = false; }, 3000);
      },
      error: () => {
        this.submittingGratitude = false;
        this.errorMessage = 'No se pudo enviar el agradecimiento.';
      }
    });
  }

  // ── IVR — reparación ─────────────────────────────────────────────────────
  openRepairModal(event: any): void {
    if (event.repairedAt) return;
    this.selectedEventForRepair = event;
    this.repairForm.description = '';
    this.showRepairModal        = true;
  }

  closeRepairModal(): void {
    this.showRepairModal        = false;
    this.selectedEventForRepair = null;
  }

  submitRepair(): void {
    if (!this.repairForm.description.trim()) return;
    this.submittingRepair = true;
    const body = {
      repairDescription: this.repairForm.description,
      repairedAt: new Date().toISOString().replace('Z', '')
    };
    this.http.put<any>(`${this.api.base}/family-behavioral-events/${this.selectedEventForRepair.id}/repair`, body)
      .subscribe({
        next: () => { this.submittingRepair = false; this.closeRepairModal(); this.loadAllData(); },
        error: () => { this.submittingRepair = false; this.errorMessage = 'Fallo al reportar la reparación.'; }
      });
  }

  // ── IVR — fricción ────────────────────────────────────────────────────────
  openFrictionModal(): void {
    this.frictionForm.description = '';
    this.frictionForm.severity    = 3;
    this.showFrictionModal        = true;
  }

  closeFrictionModal(): void { this.showFrictionModal = false; }

  submitFriction(): void {
    if (!this.frictionForm.description.trim()) return;
    this.submittingFriction = true;
    const body = {
      familyId:    this.familyId,
      description: this.frictionForm.description,
      severity:    this.frictionForm.severity,
      occurredAt:  new Date().toISOString().replace('Z', '')
    };
    this.http.post<any>(`${this.api.base}/family-behavioral-events`, body)
      .subscribe({
        next: () => { this.submittingFriction = false; this.closeFrictionModal(); this.loadAllData(); },
        error: () => { this.submittingFriction = false; this.errorMessage = 'No fue posible registrar la fricción.'; }
      });
  }

  // ── Navegación del nav bar ────────────────────────────────────────────────
  navTo(route: string): void {
    this.router.navigate([route]);
  }
}
