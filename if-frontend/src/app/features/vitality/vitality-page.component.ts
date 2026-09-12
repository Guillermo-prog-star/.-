import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';

interface FamilyMemberOption {
  id: number;
  fullName: string;
}

interface RecoveryIndexResponse {
  familyMemberId: number;
  windowDays: number;
  recoveryIndex: number | null;
  semaphore: 'GREEN' | 'YELLOW' | 'RED' | null;
}

/**
 * Fase 4 (base biológica) — ADR-009 / ADR-010.
 * Única interfaz de captura para DailyVitalityLog: el backend (VitalityController)
 * existía desde ADR-009 sin ninguna pantalla que lo consumiera.
 */
@Component({
  selector: 'app-vitality-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vitality-page.component.html',
  styleUrls: ['./vitality-page.component.css']
})
export class VitalityPageComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);

  get familyId() { return this.familyState.currentFamilyId(); }

  familyMembers: FamilyMemberOption[] = [];
  selectedMemberId: number | null = null;

  loadingMembers = false;
  loadingToday = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  /** Todos los campos son opcionales — se puede registrar solo uno (ADR-009, Decisión 1). */
  form: {
    sleepHours: number | null;
    sleepQuality: number | null;
    exerciseMinutes: number | null;
    nutritionQuality: number | null;
    screenTimeBeforeBedMinutes: number | null;
    fatigueLevel: number | null;
  } = {
    sleepHours: null,
    sleepQuality: null,
    exerciseMinutes: null,
    nutritionQuality: null,
    screenTimeBeforeBedMinutes: null,
    fatigueLevel: null
  };

  recovery: RecoveryIndexResponse | null = null;
  loadingRecovery = false;

  scaleOptions = [
    { value: 1, label: '1 — Muy baja' },
    { value: 2, label: '2 — Baja' },
    { value: 3, label: '3 — Media' },
    { value: 4, label: '4 — Buena' },
    { value: 5, label: '5 — Muy buena' }
  ];

  fatigueOptions = [
    { value: 1, label: '1 — Nada cansado' },
    { value: 2, label: '2 — Poco cansado' },
    { value: 3, label: '3 — Cansancio moderado' },
    { value: 4, label: '4 — Muy cansado' },
    { value: 5, label: '5 — Agotado' }
  ];

  ngOnInit(): void {
    this.loadMembers();
  }

  loadMembers(): void {
    if (!this.familyId) return;
    this.loadingMembers = true;
    this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`).subscribe({
      next: (res) => {
        this.familyMembers = res?.data ?? [];
        this.loadingMembers = false;
        const savedMemberId = this.familyState.currentMemberId?.();
        this.selectedMemberId = this.familyMembers.find(m => m.id === savedMemberId)?.id
          ?? this.familyMembers[0]?.id
          ?? null;
        if (this.selectedMemberId) this.onMemberChange();
      },
      error: () => { this.loadingMembers = false; }
    });
  }

  onMemberChange(): void {
    this.resetForm();
    this.loadTodayLog();
    this.loadRecoveryIndex();
  }

  private resetForm(): void {
    this.form = {
      sleepHours: null,
      sleepQuality: null,
      exerciseMinutes: null,
      nutritionQuality: null,
      screenTimeBeforeBedMinutes: null,
      fatigueLevel: null
    };
    this.successMessage = '';
    this.errorMessage = '';
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadTodayLog(): void {
    if (!this.familyId || !this.selectedMemberId) return;
    const today = this.todayIso();
    this.loadingToday = true;
    this.http.get<any>(
      `${this.api.base}/families/${this.familyId}/members/${this.selectedMemberId}/vitality`,
      { params: { from: today, to: today } }
    ).subscribe({
      next: (res) => {
        this.loadingToday = false;
        const log = (res?.data ?? [])[0];
        if (log) {
          this.form = {
            sleepHours: log.sleepHours,
            sleepQuality: log.sleepQuality,
            exerciseMinutes: log.exerciseMinutes,
            nutritionQuality: log.nutritionQuality,
            screenTimeBeforeBedMinutes: log.screenTimeBeforeBedMinutes,
            fatigueLevel: log.fatigueLevel
          };
        }
      },
      error: () => { this.loadingToday = false; }
    });
  }

  loadRecoveryIndex(): void {
    if (!this.familyId || !this.selectedMemberId) return;
    this.loadingRecovery = true;
    this.http.get<any>(
      `${this.api.base}/families/${this.familyId}/members/${this.selectedMemberId}/vitality/recovery-index`,
      { params: { windowDays: '7' } }
    ).subscribe({
      next: (res) => { this.recovery = res?.data ?? null; this.loadingRecovery = false; },
      error: () => { this.loadingRecovery = false; }
    });
  }

  hasAnyValue(): boolean {
    return Object.values(this.form).some(v => v !== null && v !== undefined);
  }

  saveLog(): void {
    if (!this.familyId || !this.selectedMemberId || !this.hasAnyValue()) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post<any>(
      `${this.api.base}/families/${this.familyId}/members/${this.selectedMemberId}/vitality`,
      this.form
    ).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = '✅ Registro de hoy guardado.';
        this.loadRecoveryIndex();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || 'No se pudo guardar el registro. Intenta de nuevo.';
      }
    });
  }

  semaphoreColor(s: string | null | undefined): string {
    switch (s) {
      case 'GREEN': return '#10b981';
      case 'YELLOW': return '#fbbf24';
      case 'RED': return '#f87171';
      default: return '#64748b';
    }
  }

  semaphoreBg(s: string | null | undefined): string {
    switch (s) {
      case 'GREEN': return 'rgba(16,185,129,0.1)';
      case 'YELLOW': return 'rgba(251,191,36,0.1)';
      case 'RED': return 'rgba(248,113,113,0.1)';
      default: return 'rgba(148,163,184,0.08)';
    }
  }

  semaphoreLabel(s: string | null | undefined): string {
    switch (s) {
      case 'GREEN': return 'Recuperación sólida';
      case 'YELLOW': return 'Recuperación moderada';
      case 'RED': return 'Recuperación baja';
      default: return 'Sin datos suficientes esta semana';
    }
  }
}
