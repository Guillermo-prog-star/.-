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
  
  @Input() actions: any[] = [];

  getIconForDimension(dimension: string): string {
    const dim = dimension?.toLowerCase() || '';
    if (dim.includes('reconoci')) return '🔍';
    if (dim.includes('amor')) return '❤️';
    if (dim.includes('entrega')) return '🤝';
    return '✨';
  }

  toggleComplete(action: any) {
    if (action.completed) return;
    this.dashboardService.completeTask(action.id).subscribe();
  }
}
