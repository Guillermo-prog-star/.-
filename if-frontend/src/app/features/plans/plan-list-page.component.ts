import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TelemetryService } from '../../core/services/telemetry.service';
import { PlanTransformacion, Mision } from '../../core/models/plan-transformacion.model';
import { PLANES_MOCK } from '../../data/planes-transformacion.mock';
import { NarrativeCompanionComponent } from '../../shared/components/narrative-companion.component';

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
  private router = inject(Router);
  private telemetry = inject(TelemetryService);

  plans: Plan[] = []; 
  planes: PlanTransformacion[] = []; 
  evidences: any[] = [];
  loading = false;
  isWaitingForPlan = false;
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
    if (this.familyId) {
      this.load(false); // Carga inicial
      this.loadMilestones();
      this.loadDashboard();
      this.loadMembers();
      
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
        },
        error: (err) => console.error('Error fetching dashboard summary:', err)
      });
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
        next: (res) => {
          this.submittingEvidence = false;
          this.closeEvidenceModal();
          this.load(true); // Recargar evidencias de manera silenciosa
          this.loadDashboard();
          
          this.terminalLogs.push(`📥 Evidencia "${payload.title}" enviada exitosamente para análisis cognitivo.`);
          this.terminalLogs.push(`🤖 Sentinel AI ha recibido la evidencia. Iniciando análisis en segundo plano...`);
          this.scrollToBottom();

          // Esperar y refrescar de nuevo de forma asíncrona para cargar la validación de la IA
          setTimeout(() => {
            this.loadFamilyEvidences();
            this.terminalLogs.push(`🔄 Sentinel AI ha finalizado el análisis de la evidencia. Revisa la tarjeta de la misión.`);
            this.scrollToBottom();
          }, 3500);
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

          // Sincronizar dinámicamente con el PLANES_MOCK
          this.planes = JSON.parse(JSON.stringify(PLANES_MOCK));
          
          if (this.plans.length > 0) {
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
              const matchedTasks = planTasks.filter((t: any) => t.dimension.toUpperCase() === plan.pilar.toUpperCase());
              
              // Mantener un conjunto de IDs de tareas del backend mapeadas
              const mappedTaskIds = new Set<number>();
              
              // 1. Sincronizar misiones predefinidas en el mock con tareas que coincidan por título o contexto de hito
              plan.misiones.forEach(mision => {
                const titleLower = mision.titulo.toLowerCase();
                const matchedTask = matchedTasks.find(t => {
                  if (mappedTaskIds.has(t.id)) return false;
                  
                  const tTitle = t.title.toLowerCase();
                  return tTitle.includes(titleLower) || 
                         titleLower.includes(tTitle) ||
                         (titleLower.includes('semáforo') && tTitle.includes('gratitud')) ||
                         (titleLower.includes('cena') && tTitle.includes('diálogo')) ||
                         (titleLower.includes('descanso') && tTitle.includes('responsabilidades'));
                });
                
                if (matchedTask) {
                  mision.backendTaskId = matchedTask.id;
                  mision.estado = matchedTask.completed ? 'Completada' : 'En_Progreso';
                  mision.titulo = matchedTask.title;
                  mision.descripcionGeneral = matchedTask.description;
                  mappedTaskIds.add(matchedTask.id);
                } else {
                  mision.backendTaskId = undefined;
                  mision.estado = 'Pendiente';
                }
              });
              
              // 2. Inyectar dinámicamente cualquier tarea del backend del pilar no mapeada aún (ej: IA o custom)
              matchedTasks.forEach(task => {
                if (!mappedTaskIds.has(task.id)) {
                  const isAi = task.title.includes('[IA]') || task.title.includes('Sentinel');
                  plan.misiones.push({
                    id: 'backend-task-' + task.id,
                    titulo: task.title,
                    estado: task.completed ? 'Completada' : 'En_Progreso',
                    backendTaskId: task.id,
                    descripcionGeneral: task.description || 'Misión clínica adaptada por la IA.',
                    isAi: isAi,
                    microacciones: [
                      { id: 'ma-ai-1-' + task.id, icono: isAi ? 'psychology' : 'settings', descripcion: task.accionConcreta || 'Realizar la dinámica principal recomendada.' },
                      { id: 'ma-ai-2-' + task.id, icono: 'assignment', descripcion: task.indicadorCumplimiento || 'Registrar la asimilación o avance del compromiso.' },
                      { id: 'ma-ai-3-' + task.id, icono: 'done_all', descripcion: task.evidenciaRequerida || 'Reportar la evidencia en el portal familiar.' }
                    ]
                  });
                  mappedTaskIds.add(task.id);
                }
              });
              
              // 3. Calcular progreso dinámico del pilar real
              const completedTasks = matchedTasks.filter((t: any) => t.completed).length;
              plan.misionesLogradas = completedTasks;
              plan.misionesTotales = plan.misiones.length;
              plan.progresoPilar = plan.misionesTotales > 0 ? Math.round((completedTasks / plan.misionesTotales) * 100) : 0;
            });

            // Proponer 2 misiones clínicas proporcionadas por la IA adaptadas al diagnóstico
            this.proposedMissions = [];
            
            const hasReactividadMission = planTasks.some((t: any) => t.title.includes('[IA] Escucha Activa Contra Reactividad'));
            const hasDigitalMission = planTasks.some((t: any) => t.title.includes('[IA] Receso Digital y Conexión Activa'));

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
    if (mision.backendTaskId) {
      const completada = mision.estado === 'Completada';
      this.toggle(mision.backendTaskId, !completada);
    } else {
      if (!this.plans || this.plans.length === 0) {
        this.terminalLogs.push(`❌ ERROR: No hay plan clínico activo para asociar esta misión.`);
        this.scrollToBottom();
        return;
      }

      this.terminalLogs.push(`⚡ [CONECTOR]: Instanciando misión "${mision.titulo}" en el pilar ${plan.pilar}...`);
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

      this.http.post<any>(`${this.api.base}/plans/tasks`, payload)
        .subscribe({
          next: () => {
            this.terminalLogs.push(`✅ ÉXITO: Misión "${mision.titulo}" activada e inyectada en la base de datos.`);
            this.scrollToBottom();
            this.load(true);
            this.loadDashboard();
          },
          error: (err) => {
            console.error('Error al instanciar misión:', err);
            this.terminalLogs.push(`❌ ERROR: No se pudo instanciar la misión en el backend: ${err.message || 'Error del servidor'}`);
            this.scrollToBottom();
          }
        });
    }
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
}
