import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-sandbox',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-8 max-w-5xl mx-auto">
      <div class="glass-premium p-10 rounded-[2rem] border border-white/10 shadow-2xl">
        <h1 class="text-4xl font-black text-white mb-6">IF <span class="text-indigo-500">SANDBOX</span></h1>
        <p class="text-white/60 mb-10 leading-relaxed">
          Orquestador de Resiliencia para <span class="text-white font-bold">William</span>. 
          Valida la respuesta proactiva ante incidentes puntuales o tendencias sistémicas.
        </p>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
          <!-- Beta Onboarding Card -->
          <div class="bg-white/5 p-6 rounded-3xl border border-white/5 hover:border-indigo-500/50 transition-all flex flex-col h-full">
            <h3 class="text-sm font-bold text-white mb-2 uppercase tracking-tighter">1. Beta Onboarding</h3>
            <p class="text-[10px] text-white/40 mb-6 flex-grow">Sembrado de entornos completos de prueba.</p>
            <input [(ngModel)]="email" placeholder="test@email.com" class="bg-black/20 border border-white/10 rounded-xl px-4 py-2 text-white text-xs mb-3 outline-none">
            <button (click)="launchBeta()" [disabled]="loading" class="bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 rounded-xl transition-all">
               Lanzar Familia
            </button>
          </div>

          <!-- Crisis Simulation Card -->
          <div class="bg-white/5 p-6 rounded-3xl border border-white/5 hover:border-red-500/50 transition-all flex flex-col h-full">
            <h3 class="text-sm font-bold text-white mb-2 uppercase tracking-tighter">2. Crisis Sentinel</h3>
            <p class="text-[10px] text-white/40 mb-6 flex-grow">Inyección de alerta crítica individual (Banner Global).</p>
            <button (click)="triggerGlobalCrisis()" [disabled]="loadingCrisis" class="bg-red-600 hover:bg-red-500 text-white font-bold py-3 rounded-xl transition-all">
               Fuerza Emergencia
            </button>
          </div>

          <!-- Trend Failure Card -->
          <div class="bg-white/5 p-6 rounded-3xl border border-orange-500/20 hover:border-orange-500/50 transition-all flex flex-col h-full">
            <h3 class="text-sm font-bold text-white mb-2 uppercase tracking-tighter">3. Fallo Sistémico</h3>
            <p class="text-[10px] text-white/40 mb-6 flex-grow">Simulación de estancamiento del 70% de los nodos Alfa.</p>
            <button (click)="triggerMassFailure()" [disabled]="loadingTrend" class="bg-orange-600 hover:bg-orange-500 text-white font-bold py-3 rounded-xl transition-all">
               Simular Estancamiento
            </button>
          </div>
        </div>

        @if (logContent) {
        <div class="mt-8 bg-black/40 rounded-2xl border border-white/5 p-6 font-mono text-[10px] overflow-auto max-h-[250px]">
          <pre class="text-indigo-300">{{ logContent | json }}</pre>
        </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .glass-premium { background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(40px); }
  `]
})
export class SandboxComponent {
  private http = inject(HttpClient);
  
  email: string = 'tester@integrity.ia';
  loading: boolean = false;
  loadingCrisis: boolean = false;
  loadingTrend: boolean = false;
  logContent: any = null;

  launchBeta() {
    this.loading = true;
    this.http.post<any>(`/api/simulation/launch-beta?email=${this.email}`, {}).subscribe({
      next: (res) => { this.logContent = res; this.loading = false; },
      error: (err) => { this.logContent = err; this.loading = false; }
    });
  }

  triggerGlobalCrisis() {
    this.loadingCrisis = true;
    this.http.post<any>(`/api/simulation/trigger-crisis-test`, {}).subscribe({
        next: (res) => { this.logContent = res.data; this.loadingCrisis = false; },
        error: (err) => { this.logContent = err; this.loadingCrisis = false; }
      });
  }

  triggerMassFailure() {
    this.loadingTrend = true;
    this.http.post<any>(`/api/simulation/trigger-mass-failure`, {}).subscribe({
        next: (res) => {
            this.logContent = { msg: '¡Ruido Sistémico Inyectado! Verifica los stats agregados.', data: res.data };
            this.loadingTrend = false;
        },
        error: (err) => { this.logContent = err; this.loadingTrend = false; }
    });
  }
}
