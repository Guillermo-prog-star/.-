import {
  Component, OnInit, inject, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CrisisService } from '../../core/services/crisis.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import { ApiService } from '../../core/services/api.service';

export interface ErrorProtocol {
  id: number;
  familyId: number;
  missionFailed: string;
  feelings: string;
  whatHappened: string;
  correctiveAction: string;
  whoHelps: string;
  agreement: string;
  followupDate: string | null;
  learning: string;
  closed: boolean;
  currentStep: string;
  createdAt: string;
  closedAt: string | null;
}

export interface GuardianStatus {
  familyId: number;
  hasGuardian: boolean;
  guardianMemberId: number | null;
  guardianFullName: string | null;
  guardianSince: string | null;
}

@Component({
  selector: 'app-crisis-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NarrativeCompanionComponent],
  templateUrl: './crisis-page.component.html',
  styleUrls: ['./crisis-page.component.css']
})
export class CrisisPageComponent implements OnInit {
  private crisisService = inject(CrisisService);
  private familyState   = inject(FamilyStateService);
  private flow          = inject(TransformationFlowService);
  private http          = inject(HttpClient);
  private api           = inject(ApiService);
  router = inject(Router);

  // ── Estado nuevo ────────────────────────────────────────────────────────
  isUnderCrisis    = signal(false);
  openProtocols    = signal<ErrorProtocol[]>([]);
  guardian         = signal<GuardianStatus | null>(null);
  activatingProto  = signal(false);
  protocolActivated = signal(false);
  protocolError    = signal('');

  openProtocolCount = computed(() => this.openProtocols().length);
  hasGuardian       = computed(() => this.guardian()?.hasGuardian ?? false);

  // ── Estado existente ────────────────────────────────────────────────────
  crisis = { category: 'Conflicto de Convivencia', emotion: '', description: '' };
  loading = false;
  lastResponse: any = null;
  history: any[] = [];
  pausedMissionId: string | null = null;

  emotionTags = ['Ira ⚡', 'Tristeza 💧', 'Frustración 🌀', 'Ansiedad 🌪️', 'Distanciamiento 🔇'];

  step1Ticked = false;
  step2Ticked = false;
  step3Ticked = false;

  // ── Lifecycle ────────────────────────────────────────────────────────────
  ngOnInit() {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid) return;
    this.loadAll(fid);
  }

  private loadAll(fid: number) {
    forkJoin({
      status:    this.crisisService.getCrisisStatus(fid).pipe(catchError(() => of(false))),
      protocols: this.http.get<ErrorProtocol[]>(
                   `${this.api.base}/families/${fid}/error-protocols/open`
                 ).pipe(catchError(() => of([]))),
      guardian:  this.http.get<any>(
                   `${this.api.base}/families/${fid}/guardian`
                 ).pipe(catchError(() => of(null)))
    }).subscribe(({ status, protocols, guardian }) => {
      this.isUnderCrisis.set(!!status);
      this.openProtocols.set(protocols as ErrorProtocol[]);
      const g = guardian?.data ?? guardian;
      this.guardian.set(g);
    });

    this.loadHistory();
  }

  // ── Acciones nuevas ──────────────────────────────────────────────────────
  activateProtocol() {
    const fid = this.familyState.getSelectedFamilyId();
    if (!fid || this.activatingProto()) return;
    this.activatingProto.set(true);
    this.protocolError.set('');
    this.http.post<any>(`${this.api.base}/crisis/protocol/activate`, {
      familyId: fid,
      reason: 'Activación manual desde panel de crisis'
    }).subscribe({
      next: () => {
        this.protocolActivated.set(true);
        this.isUnderCrisis.set(true);
        this.activatingProto.set(false);
        setTimeout(() => this.protocolActivated.set(false), 5000);
      },
      error: (e) => {
        this.protocolError.set(e?.error?.message ?? 'Error al activar el protocolo.');
        this.activatingProto.set(false);
      }
    });
  }

  stepLabel(step: string): string {
    const map: Record<string, string> = {
      DETECT: 'Detectar', FEEL: 'Sentir', UNDERSTAND: 'Comprender',
      ACTION: 'Accionar', AGREEMENT: 'Acordar', FOLLOWUP: 'Seguimiento',
      LEARNING: 'Aprender'
    };
    return map[step] ?? step;
  }

  isStepDone(currentStep: string, checkStep: string): boolean {
    const order = ['DETECT','FEEL','UNDERSTAND','ACTION','AGREEMENT','FOLLOWUP','LEARNING'];
    return order.indexOf(checkStep) < order.indexOf(currentStep);
  }

  // ── Métodos existentes ───────────────────────────────────────────────────
  loadHistory() {
    const familyId = this.familyState.getSelectedFamilyId();
    if (familyId) {
      this.crisisService.getHistory(familyId).subscribe(res => this.history = res);
    }
  }

  selectEmotion(emotion: string) { this.crisis.emotion = emotion; }

  resetChecklist() {
    this.step1Ticked = false;
    this.step2Ticked = false;
    this.step3Ticked = false;
  }

  selectHistoryItem(item: any) {
    this.lastResponse = item;
    this.resetChecklist();
    window.scrollTo({ top: 120, behavior: 'smooth' });
  }

  getCategoryColor(category: string): string {
    const colors: Record<string, string> = {
      'Emergencia Emocional': 'var(--if-crisis)',
      'Crisis de Autoridad':  '#a855f7',
      'Tensión Financiera':   'var(--if-family)',
      'Ruptura de Diálogo':   '#3b82f6',
      'Conflicto de Convivencia': '#0ea5e9'
    };
    return colors[category] ?? '#6366f1';
  }

  submitCrisis() {
    const familyId = this.familyState.getSelectedFamilyId();
    if (!familyId) return;

    const activeMission = this.flow.activeMissionId();
    if (activeMission) {
      this.pausedMissionId = activeMission;
      this.flow.setActiveMission(null);
    }

    this.loading = true;
    const payload = { ...this.crisis, familyId };

    this.crisisService.reportCrisis(payload).subscribe({
      next: (res) => {
        this.lastResponse = res;
        this.loading = false;
        this.isUnderCrisis.set(true);
        this.crisis = { category: 'Conflicto de Convivencia', emotion: '', description: '' };
        this.resetChecklist();
        this.loadHistory();
      },
      error: () => this.loading = false
    });
  }

  resumeMission() {
    if (this.pausedMissionId) {
      this.flow.setActiveMission(this.pausedMissionId);
      this.pausedMissionId = null;
    }
  }

  formatAiResponse(text: string): string {
    if (!text) return '';
    let html = text
      .replace(/^### (.*)/gm, '<h4>$1</h4>')
      .replace(/^## (.*)/gm, '<h3>$1</h3>')
      .replace(/^# (.*)/gm, '<h2>$1</h2>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/^\s*[\*\-]\s*(.*)/gm, '<li style="list-style-type:disc;margin-left:20px">$1</li>')
      .replace(/^\s*\d+\.\s*(.*)/gm, '<li style="list-style-type:decimal;margin-left:20px">$1</li>')
      .replace(/\n/g, '<br>');
    return html;
  }
}
