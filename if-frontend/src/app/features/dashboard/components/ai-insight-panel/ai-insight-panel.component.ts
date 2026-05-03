import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ai-insight-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="glass-premium p-8 rounded-3xl border-l-4 border-indigo-500 hover:shadow-[0_0_40px_rgba(99,102,241,0.1)] transition-all">
      <div class="flex items-center gap-4 mb-4">
        <div class="w-10 h-10 bg-indigo-500/20 rounded-xl flex items-center justify-center text-xl">
          🤖
        </div>
        <div>
          <h3 class="font-bold text-white/90">Mentor de Integridad</h3>
          <span class="text-[9px] text-white/30 uppercase tracking-[0.2em]">Análisis Cognitivo en Tiempo Real</span>
        </div>
      </div>
      
      <div class="space-y-4">
        <p class="text-white/80 italic leading-relaxed text-sm border-l-2 border-white/5 pl-4 ml-1">
          "{{ recommendation }}"
        </p>
        
        <div *ngIf="trend" class="flex items-center gap-2 px-4 py-2 bg-white/5 rounded-xl w-fit">
          <span class="text-[10px] text-white/40 uppercase font-bold tracking-tighter">Tendencia de Riesgo:</span>
          <span [class]="trendClass" class="text-[10px] font-black uppercase">
            {{ trend === 'UP' ? '↑ Incrementando' : trend === 'DOWN' ? '↓ Mitigándose' : '↔ Estable' }}
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .glass-premium {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.05);
    }
  `]
})
export class AiInsightPanelComponent {
  @Input() recommendation: string = 'Iniciando diagnóstico profundo...';
  @Input() trend: 'UP' | 'DOWN' | 'STABLE' | undefined;

  get trendClass() {
    return {
      'text-red-400': this.trend === 'UP',
      'text-emerald-400': this.trend === 'DOWN',
      'text-indigo-400': this.trend === 'STABLE'
    };
  }
}
