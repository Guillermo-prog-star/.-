import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

export interface AssignedFamily {
  id: number;
  linkId: number;
  familyName: string;
  networkType: string;
  status: string;
  accessScope: {
    canViewIcfScore: boolean;
    canViewRiskLevel: boolean;
    canViewPlanSummary: boolean;
    canViewSprintProgress: boolean;
    canViewCrisisHistory: boolean;
    canLeaveNotes: boolean;
  };
  invitedAt: string;
  consentedAt: string | null;
}

export interface FamilyDataView {
  familyId: number | null;
  networkType: string;
  accessLevel: number;
  participantName: string;
  icfScore: number | null;
  icfLabel: string | null;
  icfDirection: string | null;
  riskLevel: string | null;
  sentinelActive: boolean | null;
  planSummaryAvailable: boolean | null;
  hasActiveSprint: boolean | null;
  activeSprintStatus: string | null;
  crisisHistoryAvailable: boolean | null;
  aggregatedOnly: boolean | null;
}

export interface NoteForm {
  assignmentId: number;
  content: string;
  visibleToFamily: boolean;
}

@Component({
  selector: 'app-professional-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './professional-dashboard.component.html',
  styleUrls: ['./professional-dashboard.component.css']
})
export class ProfessionalDashboardComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private http   = inject(HttpClient);

  readonly families      = signal<AssignedFamily[]>([]);
  readonly selectedFamily = signal<AssignedFamily | null>(null);
  readonly dataView       = signal<FamilyDataView | null>(null);
  readonly auditLog       = signal<any[]>([]);
  readonly loading        = signal(true);
  readonly loadingView    = signal(false);
  readonly noteLoading    = signal(false);
  readonly error          = signal<string | null>(null);
  readonly noteSuccess    = signal(false);
  readonly activeTab      = signal<'data'|'notes'|'audit'>('data');

  readonly noteForm = signal<NoteForm>({ assignmentId: 0, content: '', visibleToFamily: true });

  readonly icfColor = computed(() => {
    const score = this.dataView()?.icfScore;
    if (!score) return '#94a3b8';
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#3b82f6';
    if (score >= 40) return '#f59e0b';
    return '#ef4444';
  });

  ngOnInit() {
    this.loadAssignedFamilies();
  }

  private loadAssignedFamilies() {
    this.loading.set(true);
    this.error.set(null);
    // El terapeuta consulta sus familias activas a través del endpoint de support
    this.http.get<any[]>(`${this.api.base}/support/my-families`).pipe(
      catchError(() => {
        // Fallback: intentar con el endpoint del ecosistema
        return this.http.get<any[]>(`${this.api.base}/support/my-assignments`).pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe(data => {
      this.families.set(data);
      this.loading.set(false);
    });
  }

  selectFamily(family: AssignedFamily) {
    this.selectedFamily.set(family);
    this.noteForm.set({ assignmentId: family.linkId, content: '', visibleToFamily: true });
    this.noteSuccess.set(false);
    this.activeTab.set('data');
    this.loadDataView(family);
  }

  private loadDataView(family: AssignedFamily) {
    this.loadingView.set(true);
    this.dataView.set(null);
    // El terapeuta ve los datos de la familia según su scope autorizado
    this.http.get<FamilyDataView>(
      `${this.api.base}/families/${family.id}/support/data-view?linkId=${family.linkId}`
    ).pipe(catchError(() => of(null))).subscribe(view => {
      this.dataView.set(view);
      this.loadingView.set(false);
    });
  }

  loadAuditLog(family: AssignedFamily) {
    this.activeTab.set('audit');
    this.http.get<any[]>(`${this.api.base}/families/${family.id}/ecosystem/links/${family.linkId}/audit`)
      .pipe(catchError(() => of([])))
      .subscribe(log => this.auditLog.set(log));
  }

  submitNote() {
    const family = this.selectedFamily();
    if (!family) return;
    const form = this.noteForm();
    if (!form.content.trim()) return;

    this.noteLoading.set(true);
    this.noteSuccess.set(false);
    this.http.post<any>(
      `${this.api.base}/families/${family.id}/support/notes`,
      { assignmentId: family.linkId, content: form.content, visibleToFamily: form.visibleToFamily }
    ).pipe(catchError(() => of(null))).subscribe(resp => {
      this.noteLoading.set(false);
      if (resp) {
        this.noteSuccess.set(true);
        this.noteForm.update(f => ({ ...f, content: '' }));
      }
    });
  }

  setTab(tab: 'data'|'notes'|'audit') {
    this.activeTab.set(tab);
    if (tab === 'audit' && this.selectedFamily()) {
      this.loadAuditLog(this.selectedFamily()!);
    }
  }

  icfLabel(score: number | null | undefined): string {
    if (!score) return 'Sin datos';
    if (score >= 80) return 'Fortaleza';
    if (score >= 60) return 'Creciendo';
    if (score >= 40) return 'Atención';
    return 'Crítico';
  }

  directionIcon(dir: string | null | undefined): string {
    const map: Record<string, string> = {
      IMPROVING: '↑', STABLE: '→', DECLINING: '↓',
      CRITICAL_DECLINE: '↓↓', NO_DATA: '—'
    };
    return map[dir ?? 'NO_DATA'] ?? '—';
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  updateNoteContent(value: string) {
    this.noteForm.update(f => ({ ...f, content: value }));
  }

  toggleNoteVisibility() {
    this.noteForm.update(f => ({ ...f, visibleToFamily: !f.visibleToFamily }));
  }
}
