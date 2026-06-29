import {
  Component, OnInit, ChangeDetectionStrategy,
  signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  SprintService, Sprint, CreateSprintRequest,
  CreateDailyRequest, CloseSprintRequest
} from '../../core/services/sprint.service';
import { FamilyStateService } from '../../core/services/family-state.service';

type View = 'board' | 'daily-form' | 'close-form' | 'create-form' | 'history' | 'retro';

@Component({
  selector: 'app-sprint-board-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sprint-board-page.component.html',
  styleUrls: ['./sprint-board-page.component.css']
})
export class SprintBoardPageComponent implements OnInit {
  private sprintSvc  = inject(SprintService);
  private familySvc  = inject(FamilyStateService);
  router = inject(Router);

  // ── State ──────────────────────────────────────────────────────────────
  sprint    = signal<Sprint | null>(null);
  history   = signal<Sprint[]>([]);
  loading   = signal(true);
  view      = signal<View>('board');
  actionMsg = signal('');
  actionErr = signal('');
  busy      = signal(false);

  // ── Computed ───────────────────────────────────────────────────────────
  familyId = computed(() => this.familySvc.getSelectedFamilyId());

  completedMissions = computed(() =>
    (this.sprint()?.missions ?? []).filter(m => m.status === 'COMPLETADA').length
  );
  totalMissions = computed(() => (this.sprint()?.missions ?? []).length);
  missionPct    = computed(() =>
    this.totalMissions() === 0
      ? 0
      : Math.round((this.completedMissions() / this.totalMissions()) * 100)
  );

  daysLeft = computed(() => {
    const s = this.sprint();
    if (!s?.endDate) return null;
    const diff = Math.ceil(
      (new Date(s.endDate).getTime() - Date.now()) / 86_400_000
    );
    return Math.max(0, diff);
  });

  todayCheckin = computed(() => {
    const today = new Date().toISOString().slice(0, 10);
    return (this.sprint()?.dailies ?? []).some(d => d.checkinDate === today);
  });

  recentDailies = computed(() =>
    [...(this.sprint()?.dailies ?? [])]
      .sort((a, b) => b.checkinDate.localeCompare(a.checkinDate))
      .slice(0, 5)
  );

  // ── Forms ──────────────────────────────────────────────────────────────
  // Create sprint
  createObj      = '';
  createRisk     = 'emociones';
  createDuration = 14;
  createMissions = ['', '', ''];

  // Daily
  dailyYesterday = '';
  dailyToday     = '';
  dailyBlockage  = '';
  dailyResolution = '';
  dailyEmotion   = 'BIEN';
  dailyMember    = '';

  // Close sprint
  closeWell       = '';
  closeDifficult  = '';
  closeLearn      = '';
  closeAdjust     = '';
  closeTension    = 5;
  closeMindful    = 5;
  closeShared     = 5;
  closePositive   = 5;
  closePersist    = 5;

  // ── Lifecycle ──────────────────────────────────────────────────────────
  ngOnInit() {
    const fid = this.familyId();
    if (!fid) { this.router.navigate(['/families']); return; }
    this.loadSprint(fid);
  }

  private loadSprint(fid: number) {
    this.loading.set(true);
    this.sprintSvc.getActive(fid).subscribe({
      next: s => { this.sprint.set(s); this.loading.set(false); },
      error: () => { this.sprint.set(null); this.loading.set(false); }
    });
  }

  // ── Actions ────────────────────────────────────────────────────────────
  toggle(missionId: number) {
    const s = this.sprint();
    if (!s || this.busy()) return;
    this.busy.set(true);
    this.sprintSvc.toggleMission(s.id, missionId).subscribe({
      next: updated => { this.sprint.set(updated); this.busy.set(false); },
      error: () => { this.busy.set(false); }
    });
  }

  submitDaily() {
    const s = this.sprint();
    if (!s || this.busy()) return;
    this.busy.set(true);
    this.actionErr.set('');
    const req: CreateDailyRequest = {
      yesterdayText:    this.dailyYesterday,
      todayText:        this.dailyToday,
      blockagesText:    this.dailyBlockage,
      resolutionText:   this.dailyResolution,
      emotionalIndicator: this.dailyEmotion,
      memberName:       this.dailyMember
    };
    this.sprintSvc.submitDaily(s.id, req).subscribe({
      next: () => {
        this.actionMsg.set('Daily registrado. ¡Gracias por tu conciencia!');
        this.busy.set(false);
        this.view.set('board');
        this.loadSprint(this.familyId()!);
        this.resetDailyForm();
        setTimeout(() => this.actionMsg.set(''), 4000);
      },
      error: e => {
        this.actionErr.set(e?.error?.message ?? 'Error al guardar el daily.');
        this.busy.set(false);
      }
    });
  }

  submitClose() {
    const s = this.sprint();
    if (!s || this.busy()) return;
    this.busy.set(true);
    this.actionErr.set('');
    const req: CloseSprintRequest = {
      whatWentWell:         this.closeWell,
      whatWasDifficult:     this.closeDifficult,
      whatLearned:          this.closeLearn,
      whatToAdjust:         this.closeAdjust,
      tensionLevel:         this.closeTension,
      mindfulCompliance:    this.closeMindful,
      sharedTime:           this.closeShared,
      positiveInteractions: this.closePositive,
      emotionalPersistence: this.closePersist
    };
    this.sprintSvc.closeSprint(s.id, req).subscribe({
      next: updated => {
        this.sprint.set(updated);
        this.actionMsg.set('Sprint cerrado. ¡Excelente trabajo en equipo!');
        this.busy.set(false);
        this.view.set('retro');
        setTimeout(() => this.actionMsg.set(''), 5000);
      },
      error: e => {
        this.actionErr.set(e?.error?.message ?? 'Error al cerrar el sprint.');
        this.busy.set(false);
      }
    });
  }

  submitCreate() {
    const fid = this.familyId();
    if (!fid || this.busy()) return;
    this.busy.set(true);
    this.actionErr.set('');
    const missions = this.createMissions.map(m => m.trim()).filter(Boolean);
    const req: CreateSprintRequest = {
      objective:    this.createObj,
      riskDimension: this.createRisk,
      durationDays: this.createDuration,
      missions
    };
    this.sprintSvc.create(fid, req).subscribe({
      next: s => {
        this.sprint.set(s);
        this.actionMsg.set('Sprint creado. ¡El equipo está en marcha!');
        this.busy.set(false);
        this.view.set('board');
        setTimeout(() => this.actionMsg.set(''), 4000);
      },
      error: e => {
        this.actionErr.set(e?.error?.message ?? 'Error al crear el sprint.');
        this.busy.set(false);
      }
    });
  }

  loadHistory() {
    const fid = this.familyId();
    if (!fid) return;
    this.view.set('history');
    this.sprintSvc.getHistory(fid).subscribe({
      next: h => this.history.set(h),
      error: () => this.history.set([])
    });
  }

  addMissionField()    { this.createMissions.push(''); }
  removeMission(i: number) { this.createMissions.splice(i, 1); }

  private resetDailyForm() {
    this.dailyYesterday = ''; this.dailyToday = '';
    this.dailyBlockage  = ''; this.dailyResolution = '';
    this.dailyEmotion = 'BIEN'; this.dailyMember = '';
  }

  statusLabel(status: string): string {
    return ({ ACTIVE: 'Activo', CLOSED: 'Cerrado', CANCELLED: 'Cancelado' } as any)[status] ?? status;
  }

  emotionEmoji(e: string): string {
    return ({ BIEN: '😊', REGULAR: '😐', MAL: '😔', EXCELENTE: '🤩', MUY_MAL: '😢' } as any)[e] ?? '•';
  }

  trackByMission(_: number, m: any) { return m.id; }
  trackByDaily(_: number, d: any)   { return d.id; }
  trackBySprint(_: number, s: any)  { return s.id; }
}
