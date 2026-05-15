import { Component, ElementRef, OnInit, ViewChild, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardDataService } from '../../services/dashboard-data.service';
import { Subscription } from 'rxjs';
import * as echarts from 'echarts';

@Component({
  selector: 'app-evolution-radar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="radar-wrapper glass-dark p-6 rounded-3xl h-full border border-white/10">
      <h3 class="text-xl font-bold mb-6 text-white/90">Evolución Construida</h3>
      <div #radarContainer class="radar-chart h-[400px]"></div>
      <div class="flex justify-center gap-6 mt-4 text-xs">
        <div class="flex items-center gap-2"><span class="w-2 h-2 rounded-full bg-slate-500"></span><span class="opacity-60">Inicio</span></div>
        <div class="flex items-center gap-2"><span class="w-2 h-2 rounded-full bg-indigo-500"></span><span class="opacity-60">Actual</span></div>
      </div>
    </div>
  `,
  styles: [`
    .glass-dark {
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(20px);
    }
  `]
})
export class EvolutionRadarComponent implements OnInit, OnDestroy {
  @ViewChild('radarContainer', { static: true }) radarContainer!: ElementRef;
  constructor(private dashboardService: DashboardDataService) {}
  private chart: echarts.ECharts | null = null;
  private sub = new Subscription();

  ngOnInit() {
    this.initChart();
    this.sub.add(
      this.dashboardService.getRadarData$().subscribe(data => {
        this.updateChart(data);
      })
    );
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
    if (this.chart) {
      this.chart.dispose();
    }
  }

  private initChart() {
    this.chart = echarts.init(this.radarContainer.nativeElement);
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255,255,255,0.1)',
        textStyle: { color: '#fff' }
      },
      legend: {
        show: false
      },
      radar: {
        indicator: [
          { name: 'Emociones', max: 100 },
          { name: 'Comunicación', max: 100 },
          { name: 'Hábitos', max: 100 },
          { name: 'Tiempos', max: 100 }
        ],
        shape: 'polygon',
        splitNumber: 5,
        axisName: {
          color: 'rgba(255,255,255,0.7)',
          fontSize: 12,
          fontWeight: 600
        },
        splitLine: {
          lineStyle: { color: 'rgba(255,255,255,0.05)' }
        },
        splitArea: {
          areaStyle: {
            color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)']
          }
        },
        axisLine: {
          lineStyle: { color: 'rgba(255,255,255,0.1)' }
        }
      },
      series: []
    };

    this.chart.setOption(option);
  }

  private updateChart(seriesData: any[]) {
    if (!this.chart || seriesData.length === 0) return;

    const dataSet = seriesData[0];
    const indicators = dataSet.labels.map((label: string) => ({ name: label, max: 100 }));

    this.chart.setOption({
      radar: {
        indicator: indicators
      },
      series: [{
        type: 'radar',
        data: seriesData.map((s, idx) => ({
          name: s.name,
          value: s.value,
          itemStyle: { color: idx === 0 ? '#6366f1' : '#94a3b8' },
          lineStyle: { 
            width: idx === 0 ? 3.5 : 2, 
            shadowBlur: idx === 0 ? 12 : 0, 
            shadowColor: idx === 0 ? 'rgba(99, 102, 241, 0.6)' : 'transparent',
            color: idx === 0 ? '#6366f1' : '#94a3b8'
          },
          areaStyle: { 
            color: idx === 0 ? 'rgba(99, 102, 241, 0.18)' : 'rgba(148, 163, 184, 0.08)' 
          },
          symbolSize: idx === 0 ? 6 : 4
        }))
      }]
    });
  }
}
