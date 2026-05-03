import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { NgxEchartsDirective, provideEcharts } from 'ngx-echarts';
import { EChartsOption } from 'echarts';

@Component({
  selector: 'app-voice-monitor',
  standalone: true,
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEcharts()],
  templateUrl: './voice-monitor.component.html',
  styleUrls: ['./voice-monitor.component.css']
})
export class VoiceMonitorComponent implements OnInit {
  private http = inject(HttpClient);

  stats = { totalMessages: 0, successRate: 0, activeFamilies: 0, totalDuration: 0 };
  recentInteractions: any[] = [];
  
  regionalChartOption: EChartsOption = {};
  performanceChartOption: EChartsOption = {};

  private refreshInterval: any;

  ngOnInit() {
    this.loadAllData();
    // Iniciar auto-refresco cada 30 segundos para monitoreo en tiempo real
    this.refreshInterval = setInterval(() => this.loadAllData(), 30000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadAllData() {
    console.log('🔄 [SONIC-MONITOR] Actualizando flujo de datos...');
    // 1. Stats Summary
    this.http.get<any>('/api/admin/voice/stats').subscribe(res => {
      this.stats = res.data;
      this.updatePerformanceChart();
    });

    // 2. Recent Activity
    this.http.get<any>('/api/admin/voice/recent').subscribe(res => {
      this.recentInteractions = res.data;
    });

    // 3. Regional Stats
    this.http.get<any>('/api/admin/voice/regional').subscribe(res => {
      this.updateRegionalChart(res.data);
    });
  }

  private updateRegionalChart(data: any[]) {
    this.regionalChartOption = {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { 
        type: 'value', 
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
        axisLabel: { color: '#94a3b8' }
      },
      yAxis: { 
        type: 'category', 
        data: data.map(d => d.name),
        axisLabel: { color: '#fff', fontWeight: 'bold' }
      },
      series: [{
        name: 'Interacciones',
        type: 'bar',
        data: data.map(d => d.count),
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#8b5cf6' }]
          }
        }
      }]
    };
  }

  private updatePerformanceChart() {
    this.performanceChartOption = {
      series: [{
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        center: ['50%', '75%'],
        radius: '100%',
        min: 0, max: 100,
        progress: { show: true, width: 12, itemStyle: { color: '#10b981' } },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(255,255,255,0.05)']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: '#fff',
          fontSize: 24,
          fontWeight: 'bold',
          offsetCenter: [0, '-10%']
        },
        data: [{ value: this.stats.successRate }]
      }]
    };
  }
}
