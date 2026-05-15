import { Component, inject, computed, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { DashboardDataService } from '../../services/dashboard-data.service';
import { DashboardDTO } from '../../../../core/models/dashboard.model';
import { FamilyStateService } from '../../../../core/services/family-state.service';

@Component({
  selector: 'app-scenarios-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scenarios-grid.component.html',
  styleUrls: ['./scenarios-grid.component.css']
})
export class ScenariosGridComponent {
  private dashboardData: Signal<DashboardDTO | null | undefined>;
  private readonly familyState = inject(FamilyStateService);

  constructor(public dashboardDataService: DashboardDataService) {
    this.dashboardData = toSignal(this.dashboardDataService.getDashboardState$());
  }

  // Selector reactivo de escenarios basados en IA sincronizado con DashboardDTO
  readonly scenarios = computed(() => {
    const data = this.dashboardData();

    if (!data) return this.getEmptyState();

    const list = [];

    // 1. Capa IA: Escenario Principal (Basado en recomendación IA)
    if (data.aiRecommendation) {
      // [SDD] Mapeo de lógica: Crisis detectada si activeCrisesCount > 0
      const isCritical = data.activeCrisesCount > 0;

      list.push({
        title: 'Proyección IA',
        status: isCritical ? 'Atención Crítica' : 'Evolución Estable',
        description: data.aiRecommendation,
        severity: isCritical ? 'error' : 'success'
      });
    }

    // 2. Capa de Acción: Escenarios Complementarios (Acciones sugeridas)
    if (data.suggestedActions && data.suggestedActions.length > 0) {
      data.suggestedActions.forEach((action, index: number) => {
        list.push({
          title: `Acción ${index + 1}: ${action.dimension || 'Estrategia'}`,
          status: action.completed ? 'Completado' : 'Sugerida',
          description: action.description,
          severity: action.completed ? 'success' : 'info'
        });
      });
    }

    return list.length > 0 ? list : this.getEmptyState();
  });

  private getEmptyState() {
    return [{
      title: 'Esperando Diagnóstico',
      status: 'Pendiente',
      description: 'Sincronizando con el motor de IA para generar proyecciones familiares...',
      severity: 'info'
    }];
  }

  triggerUpdate(): void {
    const familyId = this.familyState.getSelectedFamilyId();
    if (familyId) {
      this.dashboardDataService.fetchData(familyId).subscribe({
        next: () => console.log('🔄 [SDD] Métricas actualizadas con éxito para familia:', familyId),
        error: (err) => console.error('❌ [SDD] Falla al actualizar métricas:', err)
      });
    } else {
      console.warn('⚠️ [SDD] No se puede actualizar métricas: No hay ID de familia seleccionado.');
    }
  }
}