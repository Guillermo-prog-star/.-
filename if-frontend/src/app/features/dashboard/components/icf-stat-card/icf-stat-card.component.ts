import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-icf-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="icf-card glass-premium p-6 rounded-3xl" [ngClass]="theme">
      <div class="flex justify-between items-start mb-4">
        <span class="text-sm font-semibold opacity-80">{{ label }}</span>
        <span class="trend-badge" [ngClass]="trendClass">
          {{ trend === 'up' ? '↑' : '↓' }} {{ trendValue }}%
        </span>
      </div>
      <div class="flex items-baseline gap-2">
        <h2 class="text-5xl font-bold tracking-tight">{{ value }}</h2>
        <span class="text-xl opacity-60">/ 100</span>
      </div>
      <p class="mt-4 text-xs font-medium uppercase tracking-widest opacity-50">{{ status }}</p>
      
      <!-- Mini Progress Line -->
      <div class="mt-4 h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
        <div class="h-full bg-current transition-all duration-1000" [style.width.%]="value"></div>
      </div>
    </div>
  `,
  styles: [`
    .icf-card {
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    .icf-card:hover {
      transform: translateY(-5px) scale(1.02);
      box-shadow: 0 20px 40px rgba(0,0,0,0.3);
    }
    .trend-badge {
      font-size: 0.75rem;
      padding: 2px 8px;
      border-radius: 99px;
      font-weight: 700;
    }
    .trend-up { background: rgba(16, 185, 129, 0.2); color: #10b981; }
    .trend-down { background: rgba(239, 68, 68, 0.2); color: #ef4444; }
    
    .glass-premium {
      background: rgba(255, 255, 255, 0.05);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    
    .theme-harmony { color: #2dd4bf; border-left: 4px solid #2dd4bf; }
    .theme-growth { color: #6366f1; border-left: 4px solid #6366f1; }
    .theme-peace { color: #a855f7; border-left: 4px solid #a855f7; }
  `]
})
export class IcfStatCardComponent {
  @Input() label: string = 'ICF ACTUAL';
  @Input() value: number = 0;
  @Input() trend: 'up' | 'down' = 'up';
  @Input() trendValue: number = 0;
  @Input() status: string = 'ARMONIA CRECIENTE';
  @Input() theme: 'theme-harmony' | 'theme-growth' | 'theme-peace' = 'theme-harmony';

  get trendClass() {
    return this.trend === 'up' ? 'trend-up' : 'trend-down';
  }
}
