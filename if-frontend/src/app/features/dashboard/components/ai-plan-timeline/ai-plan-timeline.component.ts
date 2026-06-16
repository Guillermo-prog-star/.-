import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardDataService } from '../../services/dashboard-data.service';

@Component({
  selector: 'app-ai-plan-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-plan-timeline.component.html',
  styleUrls: ['./ai-plan-timeline.component.css']
})
export class AiPlanTimelineComponent {
  constructor(private dashboardService: DashboardDataService) {}
  
  private _actions: any[] = [];
  @Input() set actions(val: any[]) { this._actions = val ?? []; }
  get actions(): any[] { return this._actions; }

  getIconForDimension(dimension: string): string {
    const dim = dimension?.toLowerCase() || '';
    if (dim.includes('emocio')) return '🧠';
    if (dim.includes('comunica')) return '💬';
    if (dim.includes('habito') || dim.includes('hábito')) return '📅';
    if (dim.includes('tiempo')) return '⏳';
    if (dim.includes('reconoci')) return '🔍';
    if (dim.includes('amor')) return '❤️';
    if (dim.includes('entrega')) return '🤝';
    return '✨';
  }

  getProgressPercentage(): number {
    if (!this.actions || this.actions.length === 0) return 0;
    const completedCount = this.actions.filter(a => a.completed).length;
    return (completedCount / this.actions.length) * 100;
  }

  toggleComplete(action: any) {
    if (action.completed) return;
    
    // Actualización de UI Optimista (Optimistic Update) para respuesta instantánea
    action.completed = true;
    
    this.dashboardService.completeTask(action.id).subscribe({
      next: () => {
        console.log(`[TIMELINE] Tarea ${action.id} marcada como completada con éxito.`);
      },
      error: (err) => {
        console.error('[TIMELINE] Error al completar tarea, revirtiendo estado:', err);
        action.completed = false; // Reversión segura en caso de error
      }
    });
  }
}
