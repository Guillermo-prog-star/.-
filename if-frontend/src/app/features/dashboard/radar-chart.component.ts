import {
  Component,
  Input,
  OnInit,
  AfterViewInit,
  ElementRef,
  ViewChild,
  OnChanges,
  SimpleChanges
} from '@angular/core'; // CORRECCIÓN: Importación desde @angular/core
import { CommonModule } from '@angular/common';

declare var Chart: any; // Integración con CDN externo

@Component({
  selector: 'app-radar-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="radar-container glass-dark p-24 rounded-24">
      <h3 class="text-center mb-16 gradient-text">Balance de Conciencia</h3>
      <div class="canvas-wrapper" style="position: relative; height: 300px; width: 100%;">
        <canvas #radarCanvas></canvas>
      </div>
      <div class="legend mt-20 flex-center gap-16">
        <div class="flex-center gap-8">
          <span class="dot" style="background:var(--accent);"></span> 
          <span class="small text-white">Actual</span>
        </div>
        <div class="flex-center gap-8">
          <span class="dot" style="background:rgba(255,255,255,0.2);"></span> 
          <span class="small text-white">Meta IF</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .radar-container { 
      border: 1px solid rgba(255,255,255,0.1); 
      box-shadow: 0 20px 40px rgba(0,0,0,0.4); 
    }
    .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
    .flex-center { display: flex; align-items: center; justify-content: center; }
    .gap-8 { gap: 8px; }
    .gap-16 { gap: 16px; }
    .mt-20 { margin-top: 20px; }
    .text-white { color: #fff; }
  `]
})
export class RadarChartComponent implements OnInit, AfterViewInit, OnChanges {
  @ViewChild('radarCanvas') canvas!: ElementRef<HTMLCanvasElement>;
  @Input() dimensions: { [key: string]: number } = {};

  chart: any;

  ngOnInit() {
    this.injectChartJs();
  }

  ngAfterViewInit() {
    this.renderChart();
  }

  ngOnChanges(changes: SimpleChanges) {
    // Si las dimensiones cambian tras la inicialización, redibujamos
    if (changes['dimensions'] && !changes['dimensions'].firstChange) {
      this.renderChart();
    }
  }

  /**
   * Inyecta dinámicamente Chart.js si no existe en el scope global.
   * Esto previene deuda técnica si el CDN en index.html falla.
   */
  private injectChartJs() {
    if (typeof Chart !== 'undefined') return;

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
    script.async = true;
    script.onload = () => this.renderChart();
    document.head.appendChild(script);
  }

  renderChart() {
    if (typeof Chart === 'undefined' || !this.canvas) return;

    if (this.chart) {
      this.chart.destroy();
    }

    const labels = ['Emociones', 'Comunicación', 'Hábitos', 'Tiempos'];
    const values = [
      this.dimensions['emociones'] || 0,
      this.dimensions['comunicacion'] || 0,
      this.dimensions['habitos'] || 0,
      this.dimensions['tiempos'] || 0
    ];

    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart = new Chart(ctx, {
      type: 'radar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Actual',
            data: values,
            backgroundColor: 'rgba(59, 130, 246, 0.2)', // Ajuste manual si var(--accent) no carga
            borderColor: '#3b82f6',
            pointBackgroundColor: '#3b82f6',
            pointBorderColor: '#fff',
            borderWidth: 3
          },
          {
            label: 'Meta IF',
            data: [5, 5, 5, 5],
            backgroundColor: 'rgba(255,255,255,0.05)',
            borderColor: 'rgba(255,255,255,0.2)',
            borderDash: [5, 5],
            pointRadius: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          r: {
            min: 0,
            max: 5,
            beginAtZero: true,
            grid: { color: 'rgba(255,255,255,0.1)' },
            angleLines: { color: 'rgba(255,255,255,0.1)' },
            ticks: { display: false },
            pointLabels: { color: '#fff', font: { size: 12 } }
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }
}