import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { AuthService } from '../../core/services/auth.service';
import { TransformationFlowService } from '../../core/services/transformation-flow.service';
import { TelemetryService } from '../../core/services/telemetry.service';
import { PlanTransformacion, Mision } from '../../core/models/plan-transformacion.model';
import { PLANES_MOCK } from '../../data/planes-transformacion.mock';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';
import { SprintService } from '../family-logbook/sprint.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-plan-list-page', 
  standalone: true, 
  imports: [CommonModule, RouterLink, FormsModule, NarrativeCompanionComponent],
  templateUrl: './plan-list-page.component.html',
  styleUrls: ['./plan-list-page.component.css']
})
export class PlanListPageComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);
  private auth        = inject(AuthService);
  private flow        = inject(TransformationFlowService);
  private router = inject(Router);
  private telemetry    = inject(TelemetryService);
  private scrollPolicy = inject(ScrollPolicyService);
  private sprintService = inject(SprintService);

  get currentUserName(): string { return this.auth.user()?.fullName || 'Familia'; }

  plans: Plan[] = [];
  planes: PlanTransformacion[] = [];
  evidences: any[] = [];
  loading = false;
  isWaitingForPlan = false;

  /** Banner de celebración cuando una dimensión se completa y la siguiente se desbloquea */
  dimensionCompletadaBanner: {
    dimensionCompletada: string;
    dimensionDesbloqueada: string | null;
    emoji: string;
  } | null = null;
  private _bannerTimer: ReturnType<typeof setTimeout> | null = null;
  terminalLogs: string[] = [];
  selectedTaskId: number | null = null;
  isCliCollapsed = false;
  showFullTimeline = false;

  proposedMissions: any[] = [];

  private loadingInterval: ReturnType<typeof setInterval> | null = null;

  pillarProgress = {
    reconocimiento: { completed: 0, total: 0, percentage: 0 },
    amor: { completed: 0, total: 0, percentage: 0 },
    entrega: { completed: 0, total: 0, percentage: 0 }
  };

  // Lightbox Contemplation Modal State
  activeContemplation: any | null = null;
  activeInlineEvidenceId: number | null = null;
  private mapInstance: any = null;
  private inlineMapInstances: { [key: number]: any } = {};

  // Modal State Properties
  isEvidenceModalOpen = false;
  activeModalTask: any = null;
  submittingEvidence = false;
  evidenceForm = {
    title: '',
    description: '',
    textContent: '',
    fileUrl: '',
    evidenceType: 'BITACORA',
    submittedBy: '',
    feelingEmoji: ''
  };

  // Estado para la personalización de Iniciativas Familiares (Misiones 4-6)
  isCreativeModalOpen = false;
  activeCreativePlan: any = null;
  activeCreativeMision: any = null;
  creativeForm = {
    titulo: '',
    queBusca: '',
    accion1: '',
    accion2: '',
    accion3: ''
  };

  // Detalles clínicos y estructurados por hito (36 meses)
  fasesDetalles: { [key: string]: { queSeHace: string[], resultado: string } } = {
    'W1': {
      queSeHace: ['disminuir tensión emocional', 'organizar rutinas mínimas', 'iniciar escucha básica', 'reducir caos relacional', 'establecer seguridad emocional inicial'],
      resultado: 'La familia logra disminuir reactividad y crear estabilidad básica.'
    },
    'M1': {
      queSeHace: ['identificar emociones', 'reconocer hábitos negativos', 'comprender impacto de acciones', 'iniciar autorreflexión'],
      resultado: 'Cada miembro comienza a reconocer cómo afecta al sistema familiar.'
    },
    'M3': {
      queSeHace: ['fortalecer comunicación', 'reconstruir confianza', 'establecer acuerdos', 'promover espacios compartidos'],
      resultado: 'La familia comienza a operar como unidad colaborativa.'
    },
    'M6': {
      queSeHace: ['intervención activa sobre conflictos', 'regulación emocional constante', 'reconstrucción afectiva', 'acompañamiento mutuo'],
      resultado: 'Se reducen patrones destructivos recurrentes.'
    },
    'M9': {
      queSeHace: ['fortalecer rutinas saludables', 'mantener actividades familiares', 'sostener disciplina emocional', 'practicar acuerdos permanentes'],
      resultado: 'Los nuevos hábitos comienzan a estabilizarse.'
    },
    'M12': {
      queSeHace: ['coherencia entre emoción, comunicación y acción', 'autonomía emocional', 'convivencia estable', 'confianza sostenida'],
      resultado: 'La familia alcanza funcionamiento íntegro y consciente.'
    },
    'M18': {
      queSeHace: ['transmisión de valores', 'liderazgo familiar', 'mentoría entre generaciones', 'protección emocional colectiva'],
      resultado: 'La evolución comienza a impactar nuevas generaciones.'
    },
    'M24': {
      queSeHace: ['construcción de cultura familiar', 'consolidación de identidad', 'rituales positivos', 'memoria compartida'],
      resultado: 'La familia desarrolla un legado emocional sostenible.'
    },
    'M30': {
      queSeHace: ['servicio', 'expansión del bienestar', 'plenitud relacional', 'estabilidad intergeneracional'],
      resultado: 'La familia alcanza madurez emocional sostenida y capacidad de inspirar a otros.'
    },
    'M36': {
      queSeHace: ['consolidación de madurez intergeneracional activa', 'cohesión sistémica plena', 'sostenimiento a largo plazo de la autorregulación'],
      resultado: 'El núcleo familiar alcanza la trascendencia y la plenitud total del hogar.'
    }
  };

  // Dynamic Milestone states and family metadata
  milestones: any[] = [
    { code: 'W1', label: 'Estabilización', title: 'Estabilización inicial', orderIndex: 1 },
    { code: 'M1', label: 'Conciencia Inicial', title: 'Conciencia Inicial', orderIndex: 2 },
    { code: 'M3', label: 'Cimentación de Vínculos', title: 'Cimentación de Vínculos', orderIndex: 3 },
    { code: 'M6', label: 'Transformación Profunda', title: 'Transformación Profunda', orderIndex: 4 },
    { code: 'M9', label: 'Consolidación de Hábitos', title: 'Consolidación de Hábitos', orderIndex: 5 },
    { code: 'M12', label: 'Integridad Plena', title: 'Integridad Plena', orderIndex: 6 },
    { code: 'M18', label: 'Crecimiento Generacional', title: 'Crecimiento Generacional', orderIndex: 7 },
    { code: 'M24', label: 'Legado Familiar', title: 'Legado Familiar', orderIndex: 8 },
    { code: 'M30', label: 'Trascendencia', title: 'Trascendencia', orderIndex: 9 },
    { code: 'M36', label: 'Plenitud Total', title: 'Plenitud Total', orderIndex: 10 }
  ];
  familyDashboard: any = null;
  familyMembers: any[] = [];

  
  get familyId()   { return this.familyState.currentFamilyId(); }
  get familyCode() { return this.familyState.currentFamilyCode() || 'IF-CO-QUI-2026-0001'; }

  selectTask(taskId: number, event: Event) {
    event.stopPropagation();
    this.selectedTaskId = this.selectedTaskId === taskId ? null : taskId;
  }

  // ── Desbloqueo secuencial: misión N bloqueada hasta que N-1 esté Completada
  isMisionLocked(plan: PlanTransformacion, mision: Mision): boolean {
    const idx = plan.misiones.indexOf(mision);
    if (idx <= 0) return false;
    return plan.misiones[idx - 1].estado !== 'Completada';
  }

  getMisionSequenceLabel(plan: PlanTransformacion, mision: Mision): string {
    const idx = plan.misiones.indexOf(mision);
    if (idx === 0) return 'Misión 1 · IA';
    if (idx === 1) return 'Misión 2 · IA';
    return 'Misión 3 · Creativa';
  }

  // ── Desbloqueo secuencial entre planes/dimensiones ─────────────────────────
  /** Orden canónico de dimensiones. Un plan se desbloquea cuando el anterior está completo. */
  private readonly DIMENSION_ORDER = ['plan-emociones', 'plan-comunicacion', 'plan-habitos', 'plan-tiempos'];

  isPlanLocked(plan: PlanTransformacion): boolean {
    const idx = this.DIMENSION_ORDER.indexOf(plan.id);
    if (idx <= 0) return false; // EMOCIONES siempre desbloqueado
    const prevId = this.DIMENSION_ORDER[idx - 1];
    const prevPlan = this.planes.find(p => p.id === prevId);
    if (!prevPlan) return false;
    return !this.isPlanCompleted(prevPlan);
  }

  isPlanCompleted(plan: PlanTransformacion): boolean {
    return plan.misiones.length > 0 && plan.misiones.every(m => m.estado === 'Completada');
  }

  getPlanUnlockLabel(plan: PlanTransformacion): string {
    const idx = this.DIMENSION_ORDER.indexOf(plan.id);
    if (idx <= 0) return '';
    const prevId = this.DIMENSION_ORDER[idx - 1];
    const prevPlan = this.planes.find(p => p.id === prevId);
    return prevPlan?.titulo ?? '';
  }

  /** Sincroniza sprint_dias_{misionId} con el backend al cargar la página */
  private syncSprintDesdeBackend(): void {
    if (!this.familyId) return;
    this.sprintService.getActiveSprint(this.familyId)
      .pipe(catchError(() => of(null)))
      .subscribe(sprint => {
        if (!sprint) return;
        try {
          const ctx = JSON.parse(localStorage.getItem('active_sprint_mision') ?? 'null');
          if (!ctx?.misionId) return;
          if (ctx.sprintId && ctx.sprintId !== sprint.id) return;
          const diasReales = sprint.dailies?.length ?? 0;
          localStorage.setItem(`sprint_dias_${ctx.misionId}`, String(diasReales));
          if (diasReales >= sprint.durationDays && !ctx.completado) {
            ctx.completado = true;
            localStorage.setItem('active_sprint_mision', JSON.stringify(ctx));
          }
        } catch { /* ignorar */ }
      });
  }

  // ── Sprint 7 días: persistido en localStorage por misionId
  getSprintDiasCompletados(misionId: string): number {
    try {
      const raw = localStorage.getItem(`sprint_dias_${misionId}`);
      return raw ? parseInt(raw, 10) : 0;
    } catch { return 0; }
  }

  isSprintTerminado(misionId: string): boolean {
    try {
      const ctx = JSON.parse(localStorage.getItem('active_sprint_mision') ?? 'null');
      if (ctx?.misionId !== misionId) return false;
      return ctx?.completado === true || this.getSprintDiasCompletados(misionId) >= 7;
    } catch { return false; }
  }

  isSprintActivo(misionId: string): boolean {
    try { return localStorage.getItem(`sprint_activo_${misionId}`) === '1'; } catch { return false; }
  }

  getPreviousMisionTitulo(plan: PlanTransformacion, mision: Mision): string {
    const idx = plan.misiones.indexOf(mision);
    return idx > 0 ? (plan.misiones[idx - 1]?.titulo ?? '') : '';
  }

  /**
   * Cierra el ciclo Sprint → Cápsula → Misión Completa en un solo clic.
   * Requiere que el sprint de 7 días esté terminado y que la misión tenga backendTaskId.
   */
  completarMisionConCapsula(plan: PlanTransformacion, mision: Mision): void {
    if (!mision.backendTaskId) {
      // Sin task en backend: abrir el modal de evidencia normal pre-relleno
      this.openEvidenceModal({ id: 0, title: mision.titulo }, 'BITACORA');
      return;
    }

    const taskId = mision.backendTaskId;
    this.terminalLogs.push(`🎖️ Completando misión "${mision.titulo}" con la Cápsula del sprint...`);
    this.scrollToBottom();

    // Construir evidencia resumen del sprint desde localStorage
    let textContent = `✅ Sprint 7 días completado — ${mision.titulo}\n\nLa familia completó los 7 días de práctica continua para esta misión.`;
    try {
      const ctx = JSON.parse(localStorage.getItem('active_sprint_mision') ?? 'null');
      if (ctx?.misionTitulo) textContent = `✅ Sprint completado — ${ctx.misionTitulo}\n\nDimensión: ${plan.titulo}\nDías completados: 7/7\n\nLa Cápsula Familiar documenta el logro de esta misión.`;
    } catch { /* ignorar */ }

    const evidencePayload = {
      taskId,
      familyId: this.familyId,
      evidenceType: 'BITACORA',
      title: `🎖️ Cápsula — ${mision.titulo}`,
      description: `Sprint de 7 días completado en la dimensión ${plan.titulo}.`,
      textContent,
      submittedBy: this.currentUserName || 'Familia',
    };

    this.http.post<any>(`${this.api.base}/evidences/submit`, evidencePayload).subscribe({
      next: () => {
        this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed: true })
          .subscribe({
            next: () => {
              this.terminalLogs.push(`✅ Misión "${mision.titulo}" completada. ¡Cápsula archivada!`);
              this.scrollToBottom();
              // Limpiar estado del sprint en localStorage
              try {
                const ctx = JSON.parse(localStorage.getItem('active_sprint_mision') ?? 'null');
                if (ctx?.misionId === mision.id) {
                  localStorage.removeItem('active_sprint_mision');
                  localStorage.removeItem(`sprint_activo_${mision.id}`);
                  localStorage.removeItem(`sprint_dias_${mision.id}`);
                }
              } catch { /* ignorar */ }
              this.load(true);
              this.loadDashboard();
              setTimeout(() => this.avanzarSiguienteMision(taskId), 700);
            },
            error: () => {
              this.terminalLogs.push(`ℹ️ Cápsula guardada. Marca la misión completa manualmente si es necesario.`);
              this.scrollToBottom();
              this.load(true);
            }
          });
      },
      error: () => {
        // Si falla la evidencia, al menos completar la misión
        this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed: true })
          .subscribe({
            next: () => {
              this.terminalLogs.push(`✅ Misión completada.`);
              this.load(true);
              this.loadDashboard();
              setTimeout(() => this.avanzarSiguienteMision(taskId), 700);
            },
            error: () => {
              this.terminalLogs.push(`⚠️ No se pudo completar. Intenta de nuevo.`);
              this.scrollToBottom();
            }
          });
      }
    });
  }

  iniciarSprintMision(plan: PlanTransformacion, mision: Mision) {
    try {
      localStorage.setItem(`sprint_activo_${mision.id}`, '1');
      if (!localStorage.getItem(`sprint_dias_${mision.id}`)) {
        localStorage.setItem(`sprint_dias_${mision.id}`, '0');
      }
      localStorage.setItem('active_sprint_mision', JSON.stringify({
        misionId: mision.id, 
        misionTitulo: mision.titulo, 
        pilar: plan.pilar,
        backendTaskId: mision.backendTaskId
      }));
    } catch {}
    this.router.navigate(['/logbook'], { queryParams: { tab: 'SPRINT', mision: mision.id } });
  }

  toggleCli() {
    this.isCliCollapsed = !this.isCliCollapsed;
    if (!this.isCliCollapsed) {
      setTimeout(() => {
        const input = document.querySelector('.cli-drawer-input') as HTMLInputElement;
        if (input) input.focus();
      }, 150);
    }
  }

  ngOnInit() {
    this.scrollPolicy.set('scroll-to-new');
    if (this.familyId) {
      this.load(false); // Carga inicial
      this.loadMilestones();
      this.loadDashboard();
      this.loadMembers();
      this.syncSprintDesdeBackend(); // Sincronizar contador de días real desde el backend
      
      this.isWaitingForPlan = true;
      let attempts = 0;
      
      this.loadingInterval = setInterval(() => {
        if (this.plans.length > 0 || attempts > 20) {
          this.isWaitingForPlan = false;
          this.clearLoadingInterval();
        } else {
          // Si llegamos al intento 6 (unos 9 segundos) y sigue vacío, activamos diagnóstico de emergencia
          if (attempts === 6 && this.plans.length === 0) {
            console.warn("🛠️ Activando diagnóstico de emergencia...");
            this.http.get(`${this.api.base}/diagnostic/fix-plans/${this.familyId}`)
              .subscribe(() => this.load(true));
          }
          this.load(true);
          attempts++;
        }
      }, 1500);
    }
  }

  ngOnDestroy(): void {
    this.clearLoadingInterval();
    if (this._bannerTimer) { clearTimeout(this._bannerTimer); this._bannerTimer = null; }
    this.closeContemplation();
    Object.keys(this.inlineMapInstances).forEach(id => {
      this.closeInlineContemplation(Number(id));
    });
  }

  private clearLoadingInterval(): void {
    if (this.loadingInterval !== null) {
      clearInterval(this.loadingInterval);
      this.loadingInterval = null;
    }
  }

  loadMilestones() {
    this.http.get<any[]>(`${this.api.base}/milestones`)
      .subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.milestones = data.sort((a, b) => a.orderIndex - b.orderIndex);
          }
        },
        error: (err) => console.error('Error fetching milestones:', err)
      });
  }

  loadDashboard() {
    this.http.get<any>(`${this.api.base}/analytics/dashboard/family/${this.familyId}`)
      .subscribe({
        next: (res) => {
          // FIX: AnalyticsController returns ApiResponse<DashboardSummaryResponse> — extract .data
          this.familyDashboard = res?.data ?? res;
          // Re-adaptar misiones IA-2 al hito real una vez que llega el dashboard
          this.adaptIaMissionsToDiagnostic();
        },
        error: () => {
          // Dashboard falla → usar hito local desde localStorage si está disponible
          const cached = localStorage.getItem('if_family_dashboard');
          if (cached) { try { this.familyDashboard = JSON.parse(cached); } catch (_) {} }
          this.terminalLogs.push(`ℹ️ Dashboard en modo local — continuando con hito ${this.activePillar}`);
          this.scrollToBottom();
        }
      });
  }

  /** Etiqueta legible del hito actual con su mes de progresión */
  get currentMilestoneLabel(): string {
    const code = this.familyDashboard?.currentMilestone || 'W1';
    const ms = this.milestones.find(m => m.code === code);
    const label = ms?.label || code;
    const pilar = this.activePillar;
    return `${code} · ${label} · Pilar ${pilar}`;
  }

  loadMembers() {
    this.http.get<any>(`${this.api.base}/members/family/${this.familyId}`)
      .subscribe({
        next: (res) => {
          if (res && res.data) {
            this.familyMembers = res.data;
          }
        },
        error: (err) => console.error('Error fetching family members:', err)
      });
  }

  loadFamilyEvidences() {
    this.http.get<any>(`${this.api.base}/evidences/family/${this.familyId}`)
      .subscribe({
        next: (res) => {
          if (res && res.data) {
            this.evidences = res.data;
          }
        },
        error: (err) => console.error('Error fetching family evidences:', err)
      });
  }

  getTaskEvidence(taskId: number) {
    return this.evidences.find(e => e.task?.id === taskId);
  }

  getEmotionDetails(emotionKey: string | null | undefined) {
    const emotionsMap: { [key: string]: { label: string; emoji: string; color: string; glow: string } } = {
      alegria:   { label: 'Alegría',     emoji: '😊', color: '#ffb300', glow: 'rgba(255, 179, 0, 0.2)' },
      gratitud:  { label: 'Gratitud',    emoji: '🙏', color: '#a78bfa', glow: 'rgba(167, 139, 250, 0.2)' },
      amor:      { label: 'Amor',        emoji: '❤️', color: '#fb7185', glow: 'rgba(251, 113, 133, 0.2)' },
      orgullo:   { label: 'Orgullo',     emoji: '🌟', color: '#fbbf24', glow: 'rgba(251, 191, 36, 0.2)' },
      calma:     { label: 'Calma',       emoji: '😌', color: '#2dd4bf', glow: 'rgba(45, 212, 191, 0.2)' },
      esperanza: { label: 'Esperanza',   emoji: '🌱', color: '#34d399', glow: 'rgba(52, 211, 153, 0.2)' },
      tristeza:  { label: 'Tristeza',    emoji: '😢', color: '#60a5fa', glow: 'rgba(96, 165, 250, 0.2)' },
      tension:   { label: 'Tensión',     emoji: '😤', color: '#f87171', glow: 'rgba(248, 113, 113, 0.2)' },
    };
    return emotionsMap[emotionKey || ''] || { label: 'Reflexión', emoji: '📸', color: '#58a6ff', glow: 'rgba(88, 166, 255, 0.15)' };
  }

  openContemplation(ev: any) {
    this.activeContemplation = ev;
    if (ev.latitude && ev.longitude) {
      this.initMap(ev.latitude, ev.longitude);
    }
  }

  closeContemplation() {
    if (this.mapInstance) {
      try {
        this.mapInstance.remove();
      } catch (e) {
        console.error('Error removing map instance:', e);
      }
      this.mapInstance = null;
    }
    this.activeContemplation = null;
  }

  private initMap(lat: number, lng: number) {
    // Cargar CSS de Leaflet si no existe
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    // Cargar JS de Leaflet si no existe, luego instanciar
    if (typeof (window as any).L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.setupLeafletMap(lat, lng);
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.setupLeafletMap(lat, lng), 100);
    }
  }

  private setupLeafletMap(lat: number, lng: number) {
    if (!this.activeContemplation) return;

    if (this.mapInstance) {
      try {
        this.mapInstance.remove();
      } catch {}
      this.mapInstance = null;
    }

    setTimeout(() => {
      const container = document.getElementById('contemplation-map');
      if (!container) return;

      try {
        const L = (window as any).L;
        this.mapInstance = L.map('contemplation-map', {
          zoomControl: false,
          attributionControl: false
        }).setView([lat, lng], 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          maxZoom: 20
        }).addTo(this.mapInstance);

        const customIcon = L.divIcon({
          className: 'leaflet-custom-marker',
          html: `<div style="background-color: ${this.getEmotionDetails(this.activeContemplation.emotion).color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        });

        L.marker([lat, lng], { icon: customIcon }).addTo(this.mapInstance);
      } catch (e) {
        console.error('Error setting up Leaflet map:', e);
      }
    }, 150);
  }

  toggleInlineContemplation(ev: any) {
    if (this.activeInlineEvidenceId === ev.id) {
      this.closeInlineContemplation(ev.id);
      this.activeInlineEvidenceId = null;
    } else {
      if (this.activeInlineEvidenceId !== null) {
        this.closeInlineContemplation(this.activeInlineEvidenceId);
      }
      this.activeInlineEvidenceId = ev.id;
      if (ev.latitude && ev.longitude) {
        this.initInlineMap(ev.id, ev.latitude, ev.longitude);
      }
    }
  }

  private closeInlineContemplation(id: number) {
    const map = this.inlineMapInstances[id];
    if (map) {
      try {
        map.remove();
      } catch (e) {
        console.error('Error removing inline map:', e);
      }
      delete this.inlineMapInstances[id];
    }
  }

  private initInlineMap(id: number, lat: number, lng: number) {
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    if (typeof (window as any).L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.setupInlineMap(id, lat, lng);
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.setupInlineMap(id, lat, lng), 150);
    }
  }

  private setupInlineMap(id: number, lat: number, lng: number) {
    if (this.activeInlineEvidenceId !== id) return;

    this.closeInlineContemplation(id);

    setTimeout(() => {
      const mapId = `contemplation-map-inline-${id}`;
      const container = document.getElementById(mapId);
      if (!container) return;

      try {
        const L = (window as any).L;
        const map = L.map(mapId, {
          zoomControl: false,
          attributionControl: false
        }).setView([lat, lng], 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          maxZoom: 20
        }).addTo(map);

        const ev = this.evidences.find((e: any) => e.id === id);
        const emotion = ev ? ev.emotion : null;
        const color = this.getEmotionDetails(emotion).color;

        const customIcon = L.divIcon({
          className: 'leaflet-custom-marker',
          html: `<div style="background-color: ${color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        });

        L.marker([lat, lng], { icon: customIcon }).addTo(map);
        this.inlineMapInstances[id] = map;

        setTimeout(() => {
          map.invalidateSize();
        }, 300);
      } catch (e) {
        console.error('Error setting up inline Leaflet map:', e);
      }
    }, 150);
  }

  openEvidenceModal(task: any, type: string) {
    this.activeModalTask = task;
    this.isEvidenceModalOpen = true;
    this.evidenceForm = {
      title: `Evidencia: ${task.title}`,
      description: '',
      textContent: '',
      fileUrl: type === 'PHOTO' ? 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&q=80&w=800' : '',
      evidenceType: type,
      submittedBy: this.familyMembers[0]?.fullName || '',
      feelingEmoji: ''
    };
  }

  closeEvidenceModal() {
    this.isEvidenceModalOpen = false;
    this.activeModalTask = null;
  }

  isFormValid(): boolean {
    if (!this.evidenceForm.title || !this.evidenceForm.submittedBy) return false;
    if (this.evidenceForm.evidenceType === 'BITACORA' && !this.evidenceForm.textContent) return false;
    if (this.evidenceForm.evidenceType === 'PHOTO' && !this.evidenceForm.fileUrl) return false;
    return true;
  }

  submitEvidence() {
    this.submittingEvidence = true;

    // Acompañar el texto de la evidencia con el sentimiento para análisis asíncrono asertivo de la IA
    let textContentPayload = this.evidenceForm.textContent;
    if (this.evidenceForm.feelingEmoji) {
      textContentPayload = `[Cohesión Emocional Familiar: ${this.evidenceForm.feelingEmoji}] ${textContentPayload}`;
    }

    const payload = {
      taskId: this.activeModalTask.id,
      familyId: this.familyId,
      evidenceType: this.evidenceForm.evidenceType,
      title: this.evidenceForm.title,
      description: this.evidenceForm.description,
      fileUrl: this.evidenceForm.fileUrl,
      textContent: textContentPayload,
      submittedBy: this.evidenceForm.submittedBy
    };

    this.http.post<any>(`${this.api.base}/evidences/submit`, payload)
      .subscribe({
        next: () => {
          this.submittingEvidence = false;
          this.terminalLogs.push(`📥 Evidencia registrada. Completando misión...`);
          this.scrollToBottom();

          const taskId = this.activeModalTask.id;
          this.closeEvidenceModal();

          // Auto-completar la misión inmediatamente tras enviar evidencia
          this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed: true })
            .subscribe({
              next: () => {
                this.terminalLogs.push(`✅ Misión completada. Buscando siguiente misión...`);
                this.scrollToBottom();
                this.load(true);
                this.loadDashboard();
                // Avanzar automáticamente a la siguiente misión
                setTimeout(() => this.avanzarSiguienteMision(taskId), 600);
              },
              error: (err) => {
                // Completar falló — aun así intentar avanzar (evidencia ya quedó registrada)
                const msg = err?.error?.message || err?.message || 'Error del servidor';
                this.terminalLogs.push(`⚠️ Evidencia guardada pero no se pudo marcar completa: ${msg}. Reintentando...`);
                this.scrollToBottom();
                // Reintento único tras 2s
                setTimeout(() => {
                  this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed: true })
                    .subscribe({
                      next: () => {
                        this.terminalLogs.push(`✅ Misión marcada completa. Avanzando...`);
                        this.scrollToBottom();
                        this.load(true);
                        this.loadDashboard();
                        setTimeout(() => this.avanzarSiguienteMision(taskId), 600);
                      },
                      error: () => {
                        this.terminalLogs.push(`ℹ️ La misión se registró. Continúa con la siguiente cuando quieras.`);
                        this.scrollToBottom();
                        this.load(true);
                        this.loadDashboard();
                      }
                    });
                }, 2000);
              }
            });
        },
        error: (err) => {
          this.submittingEvidence = false;
          console.error('Error submitting evidence:', err);
          this.terminalLogs.push(`❌ Error al subir la evidencia: ${err.message || 'Error del servidor'}`);
          this.scrollToBottom();
        }
      });
  }

  load(silent: boolean = false) {
    if (!silent) this.loading = true;
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ 
        next: ({ data }) => { 
          this.plans = data && data.length > 0 ? [data[data.length - 1]] : []; 
          this.loading = false;
          this.loadFamilyEvidences(); // Carga secundaria

          // Inicializar/Reiniciar progreso
          this.pillarProgress = {
            reconocimiento: { completed: 0, total: 0, percentage: 0 },
            amor: { completed: 0, total: 0, percentage: 0 },
            entrega: { completed: 0, total: 0, percentage: 0 }
          };

          // Sincronizar dinámicamente con el PLANES_MOCK (6 misiones fijas: 2 clínicas + 2 IA + 2 iniciativas)
          this.planes = JSON.parse(JSON.stringify(PLANES_MOCK));
          this.adaptIaMissionsToDiagnostic(); // Adaptar misiones IA-2 al hito diagnóstico actual
          
          if (this.plans.length > 0) {
            // Plan cargado → onboarding completado
            if (!this.flow.isOnboardingDone()) {
              this.flow.completeOnboarding();
            }
            const planTasks = this.plans[0].tasks || [];
            
            // Agrupar y calcular progreso por pilar en tiempo real
            planTasks.forEach((t: any) => {
              const pName = (t.pillarName || t.fase || '').toLowerCase().trim();
              if (pName.includes('reconocimiento')) {
                this.pillarProgress.reconocimiento.total++;
                if (t.completed) this.pillarProgress.reconocimiento.completed++;
              } else if (pName.includes('amor')) {
                this.pillarProgress.amor.total++;
                if (t.completed) this.pillarProgress.amor.completed++;
              } else if (pName.includes('entrega')) {
                this.pillarProgress.entrega.total++;
                if (t.completed) this.pillarProgress.entrega.completed++;
              }
            });

            // Calcular porcentajes
            this.pillarProgress.reconocimiento.percentage = this.pillarProgress.reconocimiento.total > 0
              ? Math.round((this.pillarProgress.reconocimiento.completed / this.pillarProgress.reconocimiento.total) * 100)
              : 0;
            this.pillarProgress.amor.percentage = this.pillarProgress.amor.total > 0
              ? Math.round((this.pillarProgress.amor.completed / this.pillarProgress.amor.total) * 100)
              : 0;
            this.pillarProgress.entrega.percentage = this.pillarProgress.entrega.total > 0
              ? Math.round((this.pillarProgress.entrega.completed / this.pillarProgress.entrega.total) * 100)
              : 0;

            
            // Recorrer los planes del Mock y mapear tareas del backend de manera inteligente y multidimensional
            this.planes.forEach(plan => {
              // Ya no filtramos por dimensión porque la IA a veces cataloga la tarea en otro pilar.
              const matchedTasks = planTasks;

              // Mantener un conjunto de IDs de tareas del backend mapeadas/excluidas
              const mappedTaskIds = new Set<number>();

              // PRE-FILTRO: excluir tareas genéricas del backend antes de cualquier paso
              // Así no pueden sobreescribir títulos del mock (paso 1) ni ser inyectadas (paso 2)
              const GENERIC_PATTERNS = ['misión de reconocimiento', 'misión clínica', 'tarea generada', 'task '];
              matchedTasks.forEach((t: any) => {
                if (GENERIC_PATTERNS.some(p => t.title.toLowerCase().startsWith(p))) {
                  mappedTaskIds.add(t.id);
                }
              });

              // 1. Sincronizar misiones predefinidas en el mock con tareas que coincidan por título o contexto de hito
              plan.misiones.forEach(mision => {
                const removeAccents = (str: string) => str.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
                const titleLower = removeAccents(mision.titulo.toLowerCase());
                const matchedTask = matchedTasks.find(t => {
                  if (mappedTaskIds.has(t.id)) return false; // excluye genéricas y ya usadas

                  const tTitle = removeAccents(t.title.toLowerCase());
                  
                  // Si la tarea del backend es la de IA específica, emparejarla con la misión IA del mock
                  if (mision.id.includes('-ia-') && tTitle.includes('[ia]')) {
                    return true;
                  }

                  return tTitle.includes(titleLower) ||
                         titleLower.includes(tTitle) ||
                         (titleLower.includes('semaforo') && tTitle.includes('gratitud')) ||
                         (titleLower.includes('cena') && tTitle.includes('dialogo')) ||
                         (titleLower.includes('descanso') && tTitle.includes('responsabilidades'));
                });

                if (matchedTask) {
                  mision.backendTaskId = matchedTask.id;
                  mision.estado = matchedTask.completed ? 'Completada' : 'En_Progreso';
                  mision.titulo = matchedTask.title;
                  mision.descripcionGeneral = matchedTask.description;
                  
                  // Si el backend tiene paso a paso explícito en 'accion_concreta' o similar, podríamos mapearlo aquí,
                  // pero por ahora mantenemos el paso a paso del mock o de la propuesta.
                  
                  mappedTaskIds.add(matchedTask.id);
                } else {
                  mision.backendTaskId = undefined;
                  mision.estado = 'Pendiente';
                }
              });

              // 2. El mock es la fuente de verdad (6 misiones fijas: 2 clínicas + 2 IA + 2 iniciativas).
              // No se inyectan tareas del backend — el paso 1 ya sincronizó los estados de completado.
              
              // 3. Calcular progreso dinámico del pilar real basado en las misiones mapeadas de este plan
              const completedTasks = plan.misiones.filter(m => m.estado === 'Completada').length;
              plan.misionesLogradas = completedTasks;
              plan.misionesTotales = plan.misiones.length;
              plan.progresoPilar = plan.misionesTotales > 0 ? Math.round((completedTasks / plan.misionesTotales) * 100) : 0;
            });

            // Proponer 2 misiones clínicas proporcionadas por la IA adaptadas al diagnóstico
            this.proposedMissions = [];
            
            const hasReactividadMission = planTasks.some((t: any) => t.title.includes('[IA] Escucha Activa'));
            const hasDigitalMission = planTasks.some((t: any) => t.title.includes('[IA] Receso Digital'));

            if (!hasReactividadMission) {
              this.proposedMissions.push({
                title: '[IA] Escucha Activa Contra Reactividad',
                description: 'Espacio diario nocturno de escucha activa de 10 minutos al final del día para mitigar el estrés parental y la reactividad emocional.',
                dimension: 'emociones',
                objetivo: 'Fomentar la validación y corregulación emocional ante el estrés diario.',
                accion: 'Pasar un objeto de habla para que cada miembro comparta sin ser juzgado o interrumpido.',
                indicador: 'Dinámica completada 4 noches en la semana.',
                evidencia: 'Registrar en la bitácora familiar el nivel de tranquilidad colectiva (1 al 5).',
                impacto: 20,
                isAi: true
              });
            }

            if (!hasDigitalMission) {
              this.proposedMissions.push({
                title: '[IA] Receso Digital y Conexión Activa',
                description: 'Desconexión colectiva de pantallas 45 minutos antes de dormir, depositando dispositivos en una cesta común fuera de las habitaciones.',
                dimension: 'habitos',
                objetivo: 'Restaurar la higiene de sueño familiar y contrarrestar la desconexión por pantallas.',
                accion: 'Fijar una alarma familiar unificada y depositar celulares en la Caja de Presencia.',
                indicador: 'Desconexión colectiva exitosa por 5 días consecutivos.',
                evidencia: 'Cargar una bitácora detallando la asimilación del hábito y la calidad del descanso.',
                impacto: 22,
                isAi: true
              });
            }
          }
        }, 
        error: () => this.loading = false 
      });
  }

  getEmoji(iconName: string): string {
    const map: { [key: string]: string } = {
      'palette': '🎨',
      'rate_review': '📝',
      'psychology': '🧠',
      'volunteer_activism': '💝',
      'settings': '⚙️',
      'assignment': '📋',
      'forum': '💬',
      'phonelink_off': '📴',
      'light_mode': '🔆',
      'auto_stories': '📚',
      'alarm': '⏰',
      'done_all': '✅',
      'calendar_today': '📅',
      'sports_esports': '🎮',
      'timer': '⏱️',
      'photo_camera': '📷'
    };
    return map[iconName] || '✨';
  }

  comenzarMision(plan: PlanTransformacion, mision: Mision) {
    if (mision.estado === 'Completada') return; // ya completada, no hacer nada

    if (mision.backendTaskId) {
      // Ya tiene task en backend → abrir Sprint-Daily directamente
      this.openEvidenceModal({ id: mision.backendTaskId, title: mision.titulo }, 'BITACORA');
      return;
    }

    if (!this.plans || this.plans.length === 0) {
      this.terminalLogs.push(`❌ ERROR: No hay plan clínico activo para asociar esta misión.`);
      this.scrollToBottom();
      return;
    }

    this.terminalLogs.push(`⚡ Iniciando misión "${mision.titulo}"...`);
    this.scrollToBottom();

    const payload = {
      title: mision.titulo,
      description: mision.descripcionGeneral,
      dimension: plan.pilar.toUpperCase(),
      fase: this.activePillar,
      riesgoAsociado: 'BAJO',
      objetivo: plan.visionFamiliar,
      accionConcreta: mision.microacciones[0]?.descripcion || '',
      indicadorCumplimiento: mision.microacciones[1]?.descripcion || '',
      evidenciaRequerida: mision.microacciones[2]?.descripcion || '',
      impactoIcf: 15,
      completed: false,
      plan: { id: this.plans[0].id }
    };

    this.http.post<any>(`${this.api.base}/plans/tasks`, payload).subscribe({
      next: (res) => {
        const taskId = res?.data?.id ?? res?.id;
        this.load(true);
        this.loadDashboard();
        // Abrir Sprint-Daily inmediatamente tras crear la tarea
        if (taskId) {
          setTimeout(() => this.openEvidenceModal({ id: taskId, title: mision.titulo }, 'BITACORA'), 400);
        }
      },
      error: (err) => {
        this.terminalLogs.push(`❌ ERROR al iniciar misión: ${err.message || 'Error del servidor'}`);
        this.scrollToBottom();
      }
    });
  }

  /** Después de completar una misión, avanza automáticamente a la siguiente. */
  private avanzarSiguienteMision(taskIdCompletado: number) {
    // Buscar en qué plan y en qué posición está la misión recién completada
    for (const plan of this.planes) {
      const idx = plan.misiones.findIndex((m: any) => m.backendTaskId === taskIdCompletado);
      if (idx === -1) continue;

      // ¿Hay siguiente misión incompleta en este plan?
      for (let i = idx + 1; i < plan.misiones.length; i++) {
        const siguiente = plan.misiones[i];
        if (siguiente.estado !== 'Completada') {
          this.terminalLogs.push(`➡️ Avanzando a la siguiente misión: "${siguiente.titulo}"`);
          this.scrollToBottom();
          setTimeout(() => this.comenzarMision(plan, siguiente), 900);
          return;
        }
      }

      // Dimensión actual completada → buscar la siguiente según orden canónico
      const idxDimension = this.DIMENSION_ORDER.indexOf(plan.id);
      const nextDimId    = idxDimension >= 0 ? this.DIMENSION_ORDER[idxDimension + 1] : null;
      const nextPlan     = nextDimId ? this.planes.find(p => p.id === nextDimId) : null;
      const pendiente    = nextPlan?.misiones.find((m: any) => m.estado !== 'Completada');

      if (pendiente && nextPlan) {
        this._mostrarCelebracionDimension(plan.titulo, nextPlan.titulo);
        this.terminalLogs.push(`🔓 Dimensión "${plan.titulo}" completada → desbloqueando "${nextPlan.titulo}"`);
        this.scrollToBottom();
        setTimeout(() => this.comenzarMision(nextPlan, pendiente), 1800);
        return;
      }

      // ¿Algún otro plan con misiones pendientes (por si acaso no hay orden canónico)?
      for (const otroPlan of this.planes) {
        if (otroPlan.id === plan.id) continue;
        const pend = otroPlan.misiones.find((m: any) => m.estado !== 'Completada');
        if (pend) {
          this._mostrarCelebracionDimension(plan.titulo, otroPlan.titulo);
          setTimeout(() => this.comenzarMision(otroPlan, pend), 1800);
          return;
        }
      }

      // Todas las misiones completadas → avanzar al siguiente pilar
      this._mostrarCelebracionDimension(plan.titulo, null);
      this.terminalLogs.push(`🏆 ¡Todas las misiones del pilar completadas! Avanzando al siguiente pilar...`);
      this.scrollToBottom();
      setTimeout(() => this.avanzarPilar(), 2000);
      return;
    }
  }

  private _mostrarCelebracionDimension(completada: string, desbloqueada: string | null): void {
    const emojis: Record<string, string> = {
      'Regulación & Clima Emocional':        '💛',
      'Comunicación Asertiva':               '💬',
      'Hábitos & Convivencia Colectiva':     '🌿',
      'Tiempos de Conexión Activa':          '⏰',
    };
    this.dimensionCompletadaBanner = {
      dimensionCompletada:  completada,
      dimensionDesbloqueada: desbloqueada,
      emoji: emojis[completada] ?? '🏆',
    };
    if (this._bannerTimer) clearTimeout(this._bannerTimer);
    this._bannerTimer = setTimeout(() => { this.dimensionCompletadaBanner = null; }, 9000);
  }

  cerrarBannerDimension(): void {
    this.dimensionCompletadaBanner = null;
    if (this._bannerTimer) { clearTimeout(this._bannerTimer); this._bannerTimer = null; }
  }

  /** Llama al backend para avanzar el hito/pilar y genera la Película Familiar automáticamente. */
  private avanzarPilar() {
    this.terminalLogs.push(`🎬 Generando Película Familiar del pilar completado...`);
    this.scrollToBottom();

    // 1. Avanzar hito (puede fallar si criterios no cumplen — no bloquea el flujo)
    this.http.post<any>(`${this.api.base}/milestones/family/${this.familyId}/advance`, {})
      .subscribe({
        next: (res) => {
          const nuevo = res?.data?.newMilestone ?? res?.newMilestone;
          if (nuevo) this.terminalLogs.push(`📈 Hito avanzado: ${nuevo} — ${this.milestones.find(m => m.code === nuevo)?.label || ''}`);
        },
        error: (err) => {
          const msg = err?.error?.message || '';
          // Los criterios (tiempo, ICF, tareas) pueden no cumplirse aún — es normal
          if (msg.toLowerCase().includes('criteria') || msg.toLowerCase().includes('criterio') || msg.toLowerCase().includes('not met')) {
            this.terminalLogs.push(`ℹ️ El hito avanzará automáticamente al cumplir los 3 criterios (tiempo · ICF · tareas).`);
          } else {
            this.terminalLogs.push(`ℹ️ Avance de hito en evaluación. El sistema lo procesará esta noche.`);
          }
          this.scrollToBottom();
        }
      });

    // 2. Generar Película Familiar (el gran hito del pilar)
    this.http.post<any>(`${this.api.base}/families/${this.familyId}/movies/generate/quarter`, {})
      .subscribe({
        next: () => {
          this.terminalLogs.push(`🌟 ¡PILAR COMPLETADO! Tu Película Familiar está lista.`);
          this.scrollToBottom();
          this.load();
          this.loadDashboard();
          // Navegar a la Película Familiar tras un momento de celebración
          setTimeout(() => this.router.navigate(['/family-movie'], { queryParams: { autoGenerate: '1' } }), 2000);
        },
        error: () => {
          // Si la generación falla (sin API key, etc.), igual navegar a la página de película
          this.terminalLogs.push(`🌟 ¡PILAR COMPLETADO! Revisa tu Película Familiar.`);
          this.scrollToBottom();
          this.load();
          this.loadDashboard();
          setTimeout(() => this.router.navigate(['/family-movie'], { queryParams: { autoGenerate: '1' } }), 1500);
        }
      });
  }

  openMisionEvidence(mision: Mision) {
    if (!mision.backendTaskId) return;
    this.openEvidenceModal({ id: mision.backendTaskId, title: mision.titulo }, 'BITACORA');
  }

  selectProposedMission(mission: any) {
    if (!this.plans || this.plans.length === 0) return;
    
    const payload = {
      title: mission.title,
      description: mission.description,
      dimension: mission.dimension.toUpperCase(),
      fase: this.activePillar,
      riesgoAsociado: 'BAJO',
      objetivo: mission.objetivo,
      accionConcreta: mission.accion,
      indicadorCumplimiento: mission.indicador,
      evidenciaRequerida: mission.evidencia,
      impactoIcf: mission.impacto,
      completed: false,
      plan: { id: this.plans[0].id }
    };

    this.http.post<any>(`${this.api.base}/plans/tasks`, payload)
      .subscribe({
        next: () => {
          this.terminalLogs.push(`✅ DECISIÓN ACTIVA: Han elegido la misión sugerida por la IA: "${mission.title}".`);
          this.terminalLogs.push(`🧠 Sentinel AI ha acoplado este compromiso a su plan clínico para análisis predictivo.`);
          this.proposedMissions = []; // Remover tras selección
          this.load(true);
          this.loadDashboard();
          this.scrollToBottom();
        },
        error: (err) => {
          console.error('Error activating proposed mission:', err);
          this.terminalLogs.push(`❌ Error al activar la misión: ${err.message || 'Error del servidor'}`);
          this.scrollToBottom();
        }
      });
  }

  setFeelingEmoji(emoji: string) {
    this.evidenceForm.feelingEmoji = emoji;
  }

  getDimensionColor(dim: string): string {
    const map: { [key: string]: string } = {
      'emociones': '#fb7185',
      'comunicacion': '#38bdf8',
      'habitos': '#fbbf24',
      'tiempos': '#a78bfa'
    };
    return map[dim.toLowerCase()] || '#94a3b8';
  }

  getDimensionBg(dim: string): string {
    const map: { [key: string]: string } = {
      'emociones': 'rgba(251, 113, 133, 0.1)',
      'comunicacion': 'rgba(56, 189, 248, 0.1)',
      'habitos': 'rgba(251, 191, 36, 0.1)',
      'tiempos': 'rgba(167, 139, 250, 0.1)'
    };
    return map[dim.toLowerCase()] || 'rgba(255,255,255,0.05)';
  }

  toggle(taskId: number, completed: boolean) {
    this.http.put<any>(`${this.api.base}/plans/tasks/${taskId}/complete`, { completed })
      .subscribe({ next: () => {
        this.load();
        this.loadDashboard(); // Recargar el estado analítico al marcar como completada
      }});
  }

  get activePillar(): string {
    const code = this.familyDashboard?.currentMilestone || 'W1';
    const c = code.toUpperCase().trim();
    if (c === 'W1' || c === 'M1' || c === 'M2' || c === 'M3' || c === 'MES_00_DIAGNOSTICO') {
      return 'RECONOCIMIENTO';
    }
    if (c === 'M4' || c === 'M5' || c === 'M6' || c === 'M9' || c === 'M12') {
      return 'AMOR';
    }
    if (c === 'M15' || c === 'M18' || c === 'M21' || c === 'M24' || c === 'M36') {
      return 'ENTREGA';
    }
    return 'RECONOCIMIENTO';
  }

  getActivePillarTasks(tasks: any[]): any[] {
    if (!tasks) return [];
    const pillar = this.activePillar;
    return tasks.filter(t => t.fase === pillar);
  }

  activePillarTasksCount(p: Plan): number {
    return p.tasks ? p.tasks.filter(t => t.fase === this.activePillar).length : 0;
  }

  completedActivePillarCount(p: Plan): number {
    return p.tasks ? p.tasks.filter(t => t.fase === this.activePillar && t.completed).length : 0;
  }

  planPct(p: Plan): number {
    const total = this.activePillarTasksCount(p);
    return total ? Math.round(this.completedActivePillarCount(p) / total * 100) : 0;
  }
  
  // Cálculo de circunferencia para el progreso visual circular
  getDashOffset(p: Plan) {
    const pct = this.planPct(p);
    const circumference = 2 * Math.PI * 25; // r=25
    return circumference - (pct / 100) * circumference;
  }

  getCurrentMilestoneOrderIndex(): number {
    const code = this.familyDashboard?.currentMilestone || 'W1';
    const current = this.milestones.find(m => m.code === code);
    return current ? current.orderIndex : 1;
  }

  getQueSeHace(code: string): string[] {
    return this.fasesDetalles[code]?.queSeHace || ['Desplegar el plan de evolución familiar.'];
  }

  getResultadoEsperado(code: string): string {
    return this.fasesDetalles[code]?.resultado || 'Evolución y madurez del núcleo familiar.';
  }

  /**
   * Adapta el contenido de las misiones IA-2 (id termina en '-ia-2') al hito diagnóstico actual.
   * Se llama tras clonar el mock y también cuando llega familyDashboard (ambos son async).
   */
  adaptIaMissionsToDiagnostic(): void {
    if (!this.planes || this.planes.length === 0) return;
    const milestone = this.familyDashboard?.currentMilestone || 'W1';
    const fase = this.fasesDetalles[milestone] || this.fasesDetalles['W1'];
    const qs = fase.queSeHace;
    const resultado = fase.resultado;

    const contenidos: Record<string, { titulo: string; descripcionGeneral: string; queBusca: string; pasoAPaso: string[] }> = {
      EMOCIONES: {
        titulo: '[IA] Diagnóstico Emocional Adaptativo',
        descripcionGeneral: `Análisis Sentinel de regulación emocional en la fase ${milestone}: ${qs.slice(0, 3).join(' · ')}.`,
        queBusca: 'Mapear los patrones emocionales dominantes y diseñar intervenciones específicas según el diagnóstico actual.',
        pasoAPaso: [
          `Evaluar el indicador de reactividad familiar en el contexto del hito ${milestone}: ${qs[0] || 'observar convivencia'}.`,
          'Identificar las 2 emociones más frecuentes y disruptivas de la semana.',
          `Registrar los avances hacia el resultado esperado del hito: "${resultado}".`
        ]
      },
      COMUNICACION: {
        titulo: '[IA] Mapa de Fricciones Comunicativas',
        descripcionGeneral: `Diagnóstico Sentinel de patrones comunicativos críticos en la fase ${milestone}: ${qs.slice(0, 3).join(' · ')}.`,
        queBusca: 'Localizar los bloqueos lingüísticos repetitivos y generar un plan de intervención personalizado.',
        pasoAPaso: [
          `Identificar las 3 fricciones comunicativas más frecuentes (fase ${milestone}): ${qs[1] || 'fortalecer comunicación'}.`,
          'Aplicar la técnica diagnóstica de reformulación asertiva en los conflictos detectados.',
          `Registrar el progreso esperado hacia: "${resultado}".`
        ]
      },
      HABITOS: {
        titulo: '[IA] Diagnóstico de Adherencia de Hábitos',
        descripcionGeneral: `Evaluación Sentinel de sostenibilidad de rutinas en la fase ${milestone}: ${qs.slice(0, 3).join(' · ')}.`,
        queBusca: 'Determinar qué hábitos están consolidados y cuáles necesitan refuerzo inmediato según el avance clínico.',
        pasoAPaso: [
          `Revisar el cumplimiento de rutinas de la fase ${milestone}: ${qs[0] || 'mantener constancia'}.`,
          'Identificar los 2 hábitos con menor adherencia y su causa raíz.',
          `Proponer ajuste de refuerzo de 7 días para alcanzar: "${resultado}".`
        ]
      },
      TIEMPOS: {
        titulo: '[IA] Auditoría de Distribución de Tiempos',
        descripcionGeneral: `Análisis Sentinel de uso del tiempo relacional en la fase ${milestone}: ${qs.slice(0, 3).join(' · ')}.`,
        queBusca: 'Detectar fugas de tiempo vincular y redistribuir la agenda familiar hacia espacios de alto impacto afectivo.',
        pasoAPaso: [
          `Mapear la distribución de tiempo familiar en la fase ${milestone}.`,
          'Identificar los 3 bloques de tiempo con mayor fuga relacional.',
          `Rediseñar la agenda hacia el objetivo del hito: "${resultado}".`
        ]
      }
    };

    this.planes.forEach(plan => {
      const contenido = contenidos[plan.pilar];
      if (!contenido) return;
      const ia2 = plan.misiones.find((m: any) => m.id.endsWith('-ia-2'));
      if (!ia2) return;
      ia2.titulo = contenido.titulo;
      ia2.descripcionGeneral = contenido.descripcionGeneral;
      ia2.queBusca = contenido.queBusca;
      ia2.pasoAPaso = contenido.pasoAPaso;
    });
  }

  // Getter para verificar si se completó la misión lúdica del Bloque Dorado
  get isBloqueDoradoUnlocked(): boolean {
    if (!this.planes || this.planes.length === 0) return false;
    
    // Buscar el plan de tiempos
    const tiemposPlan = this.planes.find(p => p.pilar === 'TIEMPOS');
    if (!tiemposPlan) return false;
    
    // Buscar la misión "El Bloque Familiar Dorado"
    const bloqueMision = tiemposPlan.misiones.find(m => 
      m.id === 'mis-tiempos-1' || 
      m.titulo.toLowerCase().includes('dorado')
    );
    
    // Se desbloquea si la misión está Completada
    return bloqueMision?.estado === 'Completada';
  }

  // Avanzar hito clínico de manera asertiva gatillado desde el botón áureo
  ejecutarAvanceBloqueDorado(): void {
    if (!this.isBloqueDoradoUnlocked) return;
    
    this.terminalLogs.push('🌟 [BLOQUE DORADO]: ¡Felicidades! Han completado la agenda lúdica blindada semanal.');
    this.terminalLogs.push('🌟 [BLOQUE DORADO]: Iniciando transición de fase evolutiva familiar...');
    this.scrollToBottom();
    
    this.http.post<any>(`${this.api.base}/milestones/family/${this.familyId}/advance`, {})
      .subscribe({
        next: (res) => {
          const nextMilestoneCode = res?.data ?? 'siguiente hito';
          this.terminalLogs.push(`✅ ÉXITO: ¡Felicidades! La familia ha avanzado formalmente al hito [${nextMilestoneCode}].`);
          this.terminalLogs.push('🔄 [SISTEMA]: Sincronizando nuevo estado con el motor de IA...');
          this.scrollToBottom();
          
          // Recargar todo el estado dinámico
          this.loadDashboard();
          this.load(true);
        },
        error: (err) => {
          this.terminalLogs.push('❌ ERROR: Ocurrió un problema inesperado al registrar el avance en el servidor.');
          this.scrollToBottom();
        }
      });
  }

  openCreativeModal(plan: PlanTransformacion, mision: Mision) {
    this.activeCreativePlan = plan;
    this.activeCreativeMision = mision;
    this.isCreativeModalOpen = true;
    
    // Cargar valores previos o por defecto
    this.creativeForm = {
      titulo: mision.titulo.includes('(Iniciativa)') ? mision.titulo.replace(' (Iniciativa)', '') : mision.titulo,
      queBusca: mision.queBusca || '',
      accion1: mision.microacciones[0]?.descripcion || '',
      accion2: mision.microacciones[1]?.descripcion || '',
      accion3: mision.microacciones[2]?.descripcion || ''
    };
  }

  closeCreativeModal() {
    this.isCreativeModalOpen = false;
    this.activeCreativePlan = null;
    this.activeCreativeMision = null;
  }

  saveCreativeMision() {
    if (!this.creativeForm.titulo.trim()) return;
    
    const plan = this.activeCreativePlan;
    const mision = this.activeCreativeMision;
    
    mision.titulo = `${this.creativeForm.titulo} (Iniciativa)`;
    mision.queBusca = this.creativeForm.queBusca || 'Iniciativa y creatividad familiar activa.';
    mision.descripcionGeneral = `Dinámica personalizada: ${mision.titulo}.`;
    
    mision.microacciones = [
      { id: mision.id + '-ma1', icono: 'palette', descripcion: this.creativeForm.accion1 || 'Paso creativo 1' },
      { id: mision.id + '-ma2', icono: 'timer', descripcion: this.creativeForm.accion2 || 'Paso creativo 2' },
      { id: mision.id + '-ma3', icono: 'done_all', descripcion: this.creativeForm.accion3 || 'Paso creativo 3' }
    ];
    
    mision.pasoAPaso = [
      this.creativeForm.accion1 || 'Establecer la dinámica familiar.',
      this.creativeForm.accion2 || 'Ejecutar el compromiso de mutuo acuerdo.',
      this.creativeForm.accion3 || 'Subir evidencia fotográfica o nota al portal.'
    ];

    // Ahora la marcamos lista para ser activada (en progreso)
    mision.estado = 'En_Progreso';
    
    // Inyectar en el CLI para que la familia vea la trazabilidad
    this.terminalLogs.push(`🎨 [CREATIVIDAD]: La familia ha definido una misión personalizada: "${mision.titulo}".`);
    this.terminalLogs.push(`⚙️ [CREATIVIDAD]: Misión acoplada. Se inicia el paso a paso en el plan de ${plan.pilar}.`);
    this.scrollToBottom();
    
    this.closeCreativeModal();
  }


  getMilestoneMonthLabel(code: string): string {
    if (code.startsWith('W')) {
      return 'S' + code.replace('W', ''); // Semana 1
    }
    return 'M' + code.replace('M', ''); // Mes 1
  }

  getMilestoneEmoji(code: string): string {
    const map: { [key: string]: string } = {
      'W1': '📍',
      'M1': '🌱',
      'M3': '🌿',
      'M6': '🌳',
      'M9': '⚙️',
      'M12': '🏆',
      'M18': '🛡️',
      'M24': '⭐',
      'M30': '🌌',
      'M36': '👑'
    };
    return map[code] || '⬜';
  }

  getMilestoneStatusClass(m: any): string {
    const currentIndex = this.getCurrentMilestoneOrderIndex();
    if (m.orderIndex < currentIndex) return 'cli-success';
    if (m.orderIndex === currentIndex) return 'cli-warning';
    return 'text-muted';
  }

  getMilestoneStatusText(m: any): string {
    const currentIndex = this.getCurrentMilestoneOrderIndex();
    if (m.orderIndex < currentIndex) return 'Completado';
    if (m.orderIndex === currentIndex) {
      return 'ACTUAL / EN CURSO';
    }
    return 'Pendiente';
  }

  scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.cli-drawer-body');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 50);
  }

  onCommand(cmd: string) {
    const trimmed = cmd.trim();
    if (!trimmed) return;

    this.terminalLogs.push(`$ ${cmd}`);
    
    // Registrar telemetría inteligente del comando ejecutado
    this.telemetry.logEvent('CLI_COMMAND_EXECUTED', {
      command: trimmed,
      familyId: this.familyId,
      familyCode: this.familyCode
    });

    let lower = trimmed.toLowerCase();

    // [SDD-FIX: Self-Healing Quotes]
    // Si el comando viene envuelto en comillas simples o dobles, las removemos automáticamente
    if ((lower.startsWith("'") && lower.endsWith("'")) || (lower.startsWith('"') && lower.endsWith('"'))) {
      lower = lower.substring(1, lower.length - 1).trim();
    }

    // Command Parser
    if (lower === 'clear' || lower === 'limpiar') {
      this.terminalLogs = [];
      return;
    }

    if (lower === 'help' || lower === 'ayuda' || lower === 'dir' || lower === 'ls') {
      this.terminalLogs.push(
        '─────────────────────────────────────────────────',
        '⚙️ COMANDOS DE CONSOLA INTEGRITY:',
        '  evaluacion inicio - Inicia el diagnóstico familiar',
        '  evidencias        - Ver compromisos de cambio y bitácora clínica',
        '  dia critico       - Abrir protocolo de contención Sentinel',
        '  ver ruta          - Mostrar mapa longitudinal (36 meses) interactivo',
        '  ver perfil        - Mostrar miembros del nodo familiar actuales',
        '  avanzar hito      - Evaluar y avanzar de fase evolutiva familiar',
        '  reporte           - Exportar reporte evolutivo clínico (PDF)',
        '  dashboard         - Retornar al panóptico general',
        '  portal            - Abrir el Portal Familiar Móvil interactivo',
        '  inyectar [mision] - Inyectar micro-misión de prueba al plan activo',
        '  clear             - Limpiar historial de la consola',
        '─────────────────────────────────────────────────'
      );
      this.scrollToBottom();
      return;
    }

    if (lower === 'ver ruta' || lower === 'ruta' || lower === 'plan longitudinal' || lower === 'ver' || lower === 'roadmap') {
      this.terminalLogs.push(
        '🗺️  [RUTA DE TRANSFORMACIÓN LONGITUDINAL INTERACTIVA]:',
        '  ─────────────────────────────────────────────────'
      );
      this.milestones.forEach(m => {
        const prefix = m.orderIndex < this.getCurrentMilestoneOrderIndex() ? '  [✅] ' :
                       (m.orderIndex === this.getCurrentMilestoneOrderIndex() ? '  [⏳] ' : '  [  ] ');
        const statusText = m.orderIndex < this.getCurrentMilestoneOrderIndex() ? 'Completado' :
                           (m.orderIndex === this.getCurrentMilestoneOrderIndex() ? 'ACTUAL' : 'Pendiente');
        this.terminalLogs.push(`${prefix} ${this.getMilestoneMonthLabel(m.code)} - ${m.label} (${statusText})`);
      });
      this.terminalLogs.push('  ─────────────────────────────────────────────────');
      this.scrollToBottom();
      return;
    }

    if (lower === 'ver perfil' || lower === 'miembros' || lower === 'perfil') {
      this.terminalLogs.push(
        '👤 [ESTRUCTURA DE MIEMBROS DEL NODO FAMILIAR]:',
        '  ─────────────────────────────────────────────────'
      );
      if (this.familyMembers.length === 0) {
        this.terminalLogs.push('  No se encontraron miembros registrados en este nodo.');
      } else {
        this.familyMembers.forEach(m => {
          this.terminalLogs.push(`  • Nombre: ${m.fullName}`);
          this.terminalLogs.push(`    Rol:    ${m.roleType || 'MIEMBRO'}`);
          this.terminalLogs.push(`    Edad:   ${m.age || 'N/A'} años`);
          this.terminalLogs.push(`    Estado: ACTIVO`);
          this.terminalLogs.push('  ─────────────────────────────────────────────────');
        });
      }
      this.scrollToBottom();
      return;
    }

    if (lower === 'avanzar hito' || lower === 'avanzar' || lower === 'siguiente hito' || lower === 'next') {
      this.terminalLogs.push('🚀 [SISTEMA]: Evaluando requisitos para transición de hito...');
      this.scrollToBottom();

      this.http.get<any>(`${this.api.base}/milestones/family/${this.familyId}/advancement-status`)
        .subscribe({
          next: (res) => {
            const canAdvance: boolean = res?.data?.canAdvance ?? false;
            if (canAdvance) {
              this.terminalLogs.push('🚀 [SISTEMA]: Validación exitosa. Todos los compromisos del hito actual han sido cumplidos.');
              this.terminalLogs.push('🚀 [SISTEMA]: Iniciando transición de fase evolutiva...');
              this.scrollToBottom();

              this.http.post<any>(`${this.api.base}/milestones/family/${this.familyId}/advance`, {})
                .subscribe({
                  next: (res) => {
                    const nextMilestoneCode = res?.data ?? 'siguiente hito';
                    this.terminalLogs.push(`✅ ÉXITO: ¡Felicidades! La familia ha avanzado formalmente al hito [${nextMilestoneCode}].`);
                    this.terminalLogs.push('🔄 [SISTEMA]: Sincronizando nuevo estado con el motor de IA...');
                    this.scrollToBottom();
                    
                    // Recargar todo el estado dinámico
                    this.loadDashboard();
                    this.load();
                  },
                  error: (err) => {
                    this.terminalLogs.push('❌ ERROR: Ocurrió un problema inesperado al registrar el avance en el servidor.');
                    this.scrollToBottom();
                  }
                });
            } else {
              this.terminalLogs.push('⚠️ RECHAZO: No es posible realizar la transición en este momento.');
              this.terminalLogs.push('⚠️ MOTIVO: El nodo familiar posee misiones y tareas activas pendientes.');
              
              // Mostrar las misiones/tareas bloqueantes reales del frontend
              const pendingTasks: string[] = [];
              this.plans.forEach(p => {
                p.tasks.forEach(t => {
                  if (!t.completed) {
                    pendingTasks.push(`   - [ ] ${t.title} [${t.dimension}]`);
                  }
                });
              });

              if (pendingTasks.length > 0) {
                this.terminalLogs.push('📋 TAREAS BLOQUEANTES PENDIENTES:');
                pendingTasks.slice(0, 5).forEach(taskLine => this.terminalLogs.push(taskLine));
                if (pendingTasks.length > 5) {
                  this.terminalLogs.push(`   ... y ${pendingTasks.length - 5} tareas más.`);
                }
              } else {
                this.terminalLogs.push('📋 Verifique y complete todas las actividades de su plan de acción primero.');
              }
              this.scrollToBottom();
            }
          },
          error: (err) => {
            this.terminalLogs.push('❌ ERROR: No se pudo verificar el estado de avance familiar.');
            this.scrollToBottom();
          }
        });
      return;
    }

    if (lower === 'reporte' || lower === 'pdf' || lower === 'exportar' || lower === 'descargar' || lower === 'descargar pdf') {
      this.terminalLogs.push('📄 [REPORTE]: Generando reporte evolutivo individual...');
      this.terminalLogs.push('📄 [REPORTE]: Conectando con PdfExportService...');
      this.terminalLogs.push('📄 [REPORTE]: Descargando documento clínico...');
      this.scrollToBottom();
      this.downloadFamilyReport();
      return;
    }

    if (lower === 'evaluacion inicio' || lower === 'evaluación inicio' || lower === 'evaluacion' || lower === 'evaluaciones') {
      this.terminalLogs.push('⚡ [SISTEMA]: Inicializando Evaluación del Hito...');
      this.terminalLogs.push('⚡ [SISTEMA]: Redireccionando a /evaluations/start');
      this.scrollToBottom();
      setTimeout(() => this.router.navigate(['/evaluations/start']), 1000);
      return;
    }

    if (lower === 'checklist' || lower === 'tareas' || lower === 'habitos' || lower === 'hábitos' || lower === 'evidencias' || lower === 'evidencia') {
      this.terminalLogs.push('🌱 [SISTEMA]: Sincronizando Módulo de Evidencias Clínicas...');
      this.terminalLogs.push('🌱 [SISTEMA]: Redireccionando a /checklist (Evidencias)...');
      this.scrollToBottom();
      setTimeout(() => this.router.navigate(['/checklist']), 1000);
      return;
    }

    if (lower === 'portal' || lower === 'portal movil' || lower === 'portal familiar' || lower === 'movil' || lower === 'móvil') {
      this.terminalLogs.push('📱 [PORTAL]: Redireccionando al Portal Familiar Móvil...');
      this.terminalLogs.push('📱 [PORTAL]: Sincronizando estado móvil...');
      this.scrollToBottom();
      setTimeout(() => this.router.navigate(['/portal']), 1000);
      return;
    }

    if (lower.startsWith('inyectar') || lower.startsWith('inject')) {
      this.terminalLogs.push('⚡ [SENTINEL INJECTOR]: Buscando micro-misiones interactivas...');
      
      const sub = trimmed.substring(trimmed.indexOf(' ') + 1).trim().toLowerCase();
      let targetMission = null;
      
      const demoMissions = [
        {
          title: 'Cena sin celulares',
          description: 'Establecer una cena familiar de 15 minutos donde todos guarden sus dispositivos móviles para dialogar cara a cara.',
          dimension: 'comunicacion',
          objetivo: 'Desconectar la tecnología para reconectar emocionalmente.',
          accion: 'Implementar una caja recolectora de celulares decorada en la mesa del comedor.',
          indicador: 'Cena sin ninguna interrupción digital.',
          evidencia: 'Subir una nota corta detallando las risas o temas de conversación de la cena.',
          impacto: 15
        },
        {
          title: 'Reconocimiento sincero',
          description: 'Espacio diario nocturno para que cada integrante reconozca el valor y agradezca una acción específica realizada por otro.',
          dimension: 'emociones',
          objetivo: 'Fomentar un clima de validación y afecto sincero en el hogar.',
          accion: 'Dedicar 5 minutos al finalizar el día para decir una palabra de aliento.',
          indicador: 'Agradecimiento verbal compartido.',
          evidencia: 'Compartir cómo reaccionaron los hijos ante el reconocimiento.',
          impacto: 20
        },
        {
          title: 'Cartel de responsabilidades',
          description: 'Discutir, consensuar y diagramar la asignación de las tareas domésticas y cuidado colaborativo dentro del hogar.',
          dimension: 'habitos',
          objetivo: 'Disminuir el estrés parental mediante corresponsabilidad equitativa.',
          accion: 'Elaborar un cartel visual en un área común con los roles firmados por todos.',
          indicador: 'Asignación visual de responsabilidades.',
          evidencia: 'Describir el acuerdo o subir una foto del cartel.',
          impacto: 10
        }
      ];

      if (sub && sub !== 'inyectar' && sub !== 'inject' && sub !== 'mision') {
        targetMission = demoMissions.find(m => m.title.toLowerCase().includes(sub));
      } else {
        targetMission = demoMissions[0];
      }

      if (targetMission) {
        this.terminalLogs.push(`⚡ [SENTINEL INJECTOR]: Preparando inyección de "${targetMission.title}"...`);
        this.scrollToBottom();
        this.selectProposedMission(targetMission);
      } else {
        this.terminalLogs.push('❌ [SENTINEL INJECTOR]: Misión no reconocida. Use:');
        this.terminalLogs.push('   inyectar cena | inyectar reconocimiento | inyectar cartel');
        this.scrollToBottom();
      }
      return;
    }

    if (lower === 'dia critico' || lower === 'día crítico' || lower === 'crisis' || lower === 'sentinel') {
      this.terminalLogs.push('🚨 [SENTINEL]: Activando Consola de Contención de Emergencia...');
      this.terminalLogs.push('🚨 [SENTINEL]: Redireccionando a /crisis');
      this.scrollToBottom();
      setTimeout(() => this.router.navigate(['/crisis']), 1000);
      return;
    }

    if (lower === 'dashboard' || lower === 'panoptico' || lower === 'panóptico') {
      this.terminalLogs.push('📊 [SISTEMA]: Cargando Panel Analítico Central...');
      this.terminalLogs.push('📊 [SISTEMA]: Redireccionando a /dashboard');
      this.scrollToBottom();
      setTimeout(() => this.router.navigate(['/dashboard']), 1000);
      return;
    }

    this.terminalLogs.push(`❌ ERROR: Comando "${trimmed}" no reconocido.`, `   Escribe "ayuda" o "help" para ver el manual de comandos.`);
    this.scrollToBottom();
  }

  downloadFamilyReport(): void {
    if (!this.familyId) {
      this.terminalLogs.push('❌ ERROR: ID de familia no encontrado.');
      this.scrollToBottom();
      return;
    }
    // FIX: ExportController is at /api/v1/reports (not /api/reports)
    this.http.get(`/api/v1/reports/export/pdf/family/${this.familyId}`, { responseType: 'blob' })
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `Reporte_Evolutivo_Familia_${this.familyId}.pdf`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
          this.terminalLogs.push('✅ ÉXITO: Reporte evolutivo clínico descargado correctamente.');
          this.scrollToBottom();
        },
        error: (err: any) => {
          console.error(err);
          this.terminalLogs.push('❌ ERROR: Falla al descargar reporte clínico. Verifique que la familia tenga evaluaciones finalizadas.');
          this.scrollToBottom();
        }
      });
  }

  onAudioLoaded(event: Event): void {
    const audio = event.target as HTMLAudioElement;
    if (audio) {
      if (audio.duration === Infinity || isNaN(audio.duration) || audio.duration === 0) {
        audio.currentTime = 1e101;
        const onTimeUpdate = () => {
          audio.currentTime = 0;
          audio.removeEventListener('timeupdate', onTimeUpdate);
        };
        audio.addEventListener('timeupdate', onTimeUpdate);
      }
    }
  }
}
