import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { TelemetryService } from '../../core/services/telemetry.service';

@Component({
  selector: 'app-plan-list-page', 
  standalone: true, 
  imports: [CommonModule, RouterLink],
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
  loading = false;
  isWaitingForPlan = false;
  terminalLogs: string[] = [];
  selectedTaskId: number | null = null;
  isCliCollapsed = false;
  
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

  load(silent: boolean = false) {
    if (!silent) this.loading = true;
    this.http.get<any>(`${this.api.base}/plans/family/${this.familyId}`)
      .subscribe({ 
        next: ({ data }) => { 
          this.plans = data; 
          this.loading = false;
        }, 
        error: () => this.loading = false 
      });
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
