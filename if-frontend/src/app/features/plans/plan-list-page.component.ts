import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TelemetryService } from '../../core/services/telemetry.service';

@Component({
  selector: 'app-plan-list-page', 
  standalone: true, 
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './plan-list-page.component.html',
  styleUrls: ['./plan-list-page.component.css']
})
export class PlanListPageComponent implements OnInit {
  private http = inject(HttpClient); 
  private api = inject(ApiService);
  private familyState = inject(FamilyStateService);
  private router = inject(Router);
  private telemetry = inject(TelemetryService);

  plans: Plan[] = []; 
  evidences: any[] = [];
  loading = false;
  isWaitingForPlan = false;
  terminalLogs: string[] = [];
  selectedTaskId: number | null = null;
  isCliCollapsed = false;

  proposedMissions: any[] = [];

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
  
  get familyId() { return this.familyState.currentFamilyId(); }
  get familyCode() { return localStorage.getItem('selectedFamilyCode') || 'IF-CO-QUI-2026-0001'; }

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
      
      const interval = setInterval(() => {
        if (this.plans.length > 0 || attempts > 20) {
          this.isWaitingForPlan = false;
          clearInterval(interval);
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
          this.familyDashboard = res;
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
          this.plans = data; 
          this.loading = false;
          this.loadFamilyEvidences(); // Carga secundaria

          // Seccionar y proponer micro-misiones dinámicamente si no se han elegido aún
          if (this.plans.length > 0) {
            const planTasks = this.plans[0].tasks || [];
            // Si no se tiene una tarea activa con estos nombres, proponemos las micro-misiones
            const hasScreenFree = planTasks.some((t: any) => t.title.includes('Cena sin celulares'));
            const hasGratitude = planTasks.some((t: any) => t.title.includes('Reconocimiento sincero'));
            
            if (!hasScreenFree && !hasGratitude) {
              this.proposedMissions = [
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
            } else {
              this.proposedMissions = [];
            }
          }
        }, 
        error: () => this.loading = false 
      });
  }

  selectProposedMission(mission: any) {
    if (!this.plans || this.plans.length === 0) return;
    
    const payload = {
      title: mission.title,
      description: mission.description,
      dimension: mission.dimension.toUpperCase(),
      fase: 'EJECUCION',
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
          this.terminalLogs.push(`✅ DECISIÓN ACTIVA: Han elegido la micro-misión "${mission.title}". ¡A por esa micro-victoria!`);
          this.terminalLogs.push(`🌱 Sentinel AI ha registrado su racha de autonomía y la ha acoplado a su plan clínico.`);
          this.proposedMissions = []; // Remover tras selección para conservar la interfaz impecable
          this.load(true);
          this.loadDashboard();
          this.scrollToBottom();
        },
        error: (err) => {
          console.error('Error activating micro-mission:', err);
          this.terminalLogs.push(`❌ Error al activar la micro-misión: ${err.message || 'Error del servidor'}`);
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

  completedCount(p: Plan) { return p.tasks.filter(t => t.completed).length; }
  planPct(p: Plan) { return p.tasks.length ? Math.round(this.completedCount(p) / p.tasks.length * 100) : 0; }
  
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

      this.http.get<boolean>(`${this.api.base}/milestones/family/${this.familyId}/check-advance`)
        .subscribe({
          next: (canAdvance) => {
            if (canAdvance) {
              this.terminalLogs.push('🚀 [SISTEMA]: Validación exitosa. Todos los compromisos del hito actual han sido cumplidos.');
              this.terminalLogs.push('🚀 [SISTEMA]: Iniciando transición de fase evolutiva...');
              this.scrollToBottom();

              this.http.post(`${this.api.base}/milestones/family/${this.familyId}/advance`, {}, { responseType: 'text' })
                .subscribe({
                  next: (nextMilestoneCode) => {
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
    this.http.get(`${this.api.base}/reports/export/pdf/family/${this.familyId}`, { responseType: 'blob' })
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
