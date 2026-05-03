import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sentinel-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="active" 
         class="sentinel-container glass-emergency p-6 rounded-3xl border-2 border-red-500/50 flex flex-col md:flex-row items-center gap-6 animate-pulse-slow shadow-[0_0_50px_rgba(239,68,68,0.2)]">
      
      <div class="flex items-center gap-4">
        <div class="relative">
          <div class="w-12 h-12 bg-red-500 rounded-full flex items-center justify-center text-2xl shadow-[0_0_20px_rgba(239,68,68,0.6)]">
            🛡️
          </div>
          <div class="absolute -top-1 -right-1 w-4 h-4 bg-white rounded-full animate-ping"></div>
        </div>
        
        <div class="text-left">
          <h2 class="text-xl font-black text-white uppercase tracking-tighter">Sentinel Protocolo de Contención</h2>
          <p class="text-red-200/70 text-[10px] font-bold uppercase tracking-[0.2em]">Estado: Inestabilidad de Consciencia Detectada</p>
        </div>
      </div>

      <div class="flex-1 bg-black/20 p-4 rounded-2xl border border-white/5">
        <p class="text-xs text-white/80 leading-relaxed">
          Atención: El Nodo Armenia ha detectado una deriva crítica en el ICF. Se han suspendido los objetivos de largo plazo. El foco actual es la <strong>estabilización del núcleo</strong>.
        </p>
      </div>

      <div class="flex flex-col gap-2">
        <button class="px-6 py-2 bg-white text-red-600 text-[10px] font-black uppercase rounded-lg hover:bg-red-50 transition-colors">
          Protocolo Primeros Auxilios
        </button>
        <button class="px-6 py-2 bg-transparent border border-white/20 text-white text-[10px] font-black uppercase rounded-lg hover:bg-white/5 transition-colors">
          Contactar Mentor Humano
        </button>
      </div>
    </div>
  `,
  styles: [`
    .glass-emergency {
      background: linear-gradient(135deg, rgba(127, 29, 29, 0.9), rgba(69, 10, 10, 0.9));
      backdrop-filter: blur(20px);
    }
    .animate-pulse-slow {
      animation: pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.95; transform: scale(0.995); }
    }
  `]
})
export class SentinelAlertComponent {
  @Input() active: boolean = false;
}
