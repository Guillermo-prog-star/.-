import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface AccessScope {
  canViewIcfScore: boolean;
  canViewRiskLevel: boolean;
  canViewPlanSummary: boolean;
  canViewSprintProgress: boolean;
  canViewCrisisHistory: boolean;
  canLeaveNotes: boolean;
}

export interface AssignedFamily {
  id: number;       // assignmentId
  familyId: number;
  familyName: string;
  specialty: string;
  status: string;
  accessScope: AccessScope;
  invitedAt: string;
  consentedAt: string | null;
  professional?: { fullName: string; email: string; specialty: string };
}

export interface FamilyDataView {
  familyId: number | null;
  familyName: string;
  assignmentId: number;
  specialty: string;
  accessLevel: number;
  icfScore: number | null;
  icfLabel: string | null;
  icfDirection: string | null;
  riskLevel: string | null;
  sentinelActive: boolean | null;
  planSummaryAvailable: boolean | null;
  hasActiveSprint: boolean | null;
  activeSprintStatus: string | null;
  crisisHistoryAvailable: boolean | null;
}

export interface ProfessionalProfile {
  id: number;
  fullName: string;
  email: string;
  specialty: string;
  licenseNumber: string | null;
  institutionName: string | null;
  bio: string | null;
}

type MainTab = 'families' | 'profile';
type FamilyTab = 'data' | 'notes' | 'audit';

@Component({
  selector: 'app-professional-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './professional-dashboard.component.html',
  styleUrls: ['./professional-dashboard.component.css']
})
export class ProfessionalDashboardComponent implements OnInit {
  private api = inject(ApiService);
  private http = inject(HttpClient);

  readonly mainTab        = signal<MainTab>('families');
  readonly families       = signal<AssignedFamily[]>([]);
  readonly selected       = signal<AssignedFamily | null>(null);
  readonly dataView       = signal<FamilyDataView | null>(null);
  readonly auditLog       = signal<any[]>([]);
  readonly profile        = signal<ProfessionalProfile | null>(null);
  readonly loading        = signal(true);
  readonly loadingView    = signal(false);
  readonly loadingProfile = signal(false);
  readonly profileSaving  = signal(false);
  readonly noteLoading    = signal(false);
  readonly error          = signal<string | null>(null);
  readonly profileError   = signal<string | null>(null);
  readonly noteSuccess    = signal(false);
  readonly profileSuccess = signal(false);
  readonly familyTab      = signal<FamilyTab>('data');
  readonly noProfile      = signal(false);

  readonly noteContent    = signal('');
  readonly noteVisible    = signal(true);

  readonly editBio          = signal('');
  readonly editPhone        = signal('');
  readonly editInstitution  = signal('');
  readonly editLicense      = signal('');
  readonly editFullName     = signal('');

  readonly icfColor = computed(() => {
    const s = this.dataView()?.icfScore;
    if (!s) return '#94a3b8';
    if (s >= 80) return '#10b981';
    if (s >= 60) return '#3b82f6';
    if (s >= 40) return '#f59e0b';
    return '#ef4444';
  });

  readonly specialtyLabel = computed(() => {
    return this.specialtyMap()[this.profile()?.specialty ?? ''] ?? (this.profile()?.specialty ?? '—');
  });

  ngOnInit() { this.loadFamilies(); this.loadProfile(); }

  private get base() { return this.api.base; }

  // ── Cargar familias ───────────────────────────────────────────────────

  loadFamilies() {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<any[]>(`${this.base}/support/my-families`).pipe(
      catchError(() => this.http.get<any[]>(`${this.base}/support/my-assignments`).pipe(
        catchError(() => of([]))
      ))
    ).subscribe(data => {
      // normaliza: el backend retorna AssignmentResponse con familyId + professional
      const mapped: AssignedFamily[] = data.map(a => ({
        id: a.id,
        familyId: a.familyId,
        familyName: a.familyName ?? `Familia #${a.familyId}`,
        specialty: a.specialty ?? a.professional?.specialty ?? '',
        status: a.status,
        accessScope: a.accessScope ?? {},
        invitedAt: a.invitedAt,
        consentedAt: a.consentedAt,
        professional: a.professional
      }));
      this.families.set(mapped);
      this.loading.set(false);
    });
  }

  selectFamily(f: AssignedFamily) {
    this.selected.set(f);
    this.familyTab.set('data');
    this.noteSuccess.set(false);
    this.noteContent.set('');
    this.loadDataView(f);
  }

  private loadDataView(f: AssignedFamily) {
    this.loadingView.set(true);
    this.dataView.set(null);
    this.http.get<FamilyDataView>(
      `${this.base}/families/${f.familyId}/support/data-view?assignmentId=${f.id}`
    ).pipe(catchError(() => of(null))).subscribe(v => {
      this.dataView.set(v);
      this.loadingView.set(false);
    });
  }

  setFamilyTab(tab: FamilyTab) {
    this.familyTab.set(tab);
    if (tab === 'audit' && this.selected()) this.loadAudit();
  }

  private loadAudit() {
    const f = this.selected()!;
    this.http.get<any[]>(`${this.base}/families/${f.familyId}/ecosystem/links/${f.id}/audit`)
      .pipe(catchError(() => of([])))
      .subscribe(log => this.auditLog.set(log));
  }

  submitNote() {
    const f = this.selected();
    if (!f || !this.noteContent().trim()) return;
    this.noteLoading.set(true);
    this.noteSuccess.set(false);
    this.http.post<any>(`${this.base}/families/${f.familyId}/support/notes`, {
      assignmentId: f.id,
      content: this.noteContent(),
      visibleToFamily: this.noteVisible()
    }).pipe(catchError(() => of(null))).subscribe(r => {
      this.noteLoading.set(false);
      if (r) { this.noteSuccess.set(true); this.noteContent.set(''); }
    });
  }

  // ── Perfil del profesional ────────────────────────────────────────────

  loadProfile() {
    this.loadingProfile.set(true);
    this.http.get<ProfessionalProfile>(`${this.base}/support/my-profile`)
      .pipe(catchError(() => { this.noProfile.set(true); return of(null); }))
      .subscribe(p => {
        this.profile.set(p);
        if (p) {
          this.editFullName.set(p.fullName ?? '');
          this.editBio.set(p.bio ?? '');
          this.editPhone.set('');
          this.editInstitution.set(p.institutionName ?? '');
          this.editLicense.set(p.licenseNumber ?? '');
        }
        this.loadingProfile.set(false);
      });
  }

  saveProfile() {
    this.profileSaving.set(true);
    this.profileError.set(null);
    this.profileSuccess.set(false);
    this.http.put<ProfessionalProfile>(`${this.base}/support/my-profile`, {
      fullName: this.editFullName(),
      phone: this.editPhone() || undefined,
      bio: this.editBio(),
      institutionName: this.editInstitution(),
      licenseNumber: this.editLicense()
    }).pipe(catchError(e => {
      this.profileError.set(e?.error?.message ?? 'Error al guardar el perfil.');
      return of(null);
    })).subscribe(p => {
      this.profileSaving.set(false);
      if (p) { this.profile.set(p); this.profileSuccess.set(true); }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  setMainTab(t: MainTab) { this.mainTab.set(t); }

  specialtyMap(): Record<string, string> {
    return {
      THERAPIST: 'Terapeuta familiar', ORIENTADOR: 'Orientador familiar',
      SOCIAL_WORKER: 'Trabajador social', DOCTOR: 'Médico',
      TEACHER: 'Docente', COMMUNITY_LEADER: 'Líder comunitario',
      COACH: 'Coach familiar', INSTITUTION: 'Institución'
    };
  }

  specialtyIcon(s: string): string {
    const m: Record<string, string> = {
      THERAPIST: '🧠', ORIENTADOR: '🧭', SOCIAL_WORKER: '🤝',
      DOCTOR: '🩺', TEACHER: '📚', COMMUNITY_LEADER: '🏘️',
      COACH: '🎯', INSTITUTION: '🏛️'
    };
    return m[s] ?? '👤';
  }

  directionIcon(d: string | null | undefined): string {
    return ({ IMPROVING: '↑', STABLE: '→', DECLINING: '↓', CRITICAL_DECLINE: '↓↓' } as any)[d ?? ''] ?? '—';
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  scopeDots(scope: AccessScope): { key: string; label: string; on: boolean }[] {
    return [
      { key: 'icf',     label: 'ICF',     on: scope?.canViewIcfScore },
      { key: 'risk',    label: 'Riesgo',  on: scope?.canViewRiskLevel },
      { key: 'plan',    label: 'Plan',    on: scope?.canViewPlanSummary },
      { key: 'sprint',  label: 'Sprint',  on: scope?.canViewSprintProgress },
      { key: 'crisis',  label: 'Crisis',  on: scope?.canViewCrisisHistory },
      { key: 'notes',   label: 'Notas',   on: scope?.canLeaveNotes },
    ];
  }

  icfGaugeDash(score: number | null): string {
    return score ? ((score / 100) * 172) + ' 172' : '0 172';
  }
}
