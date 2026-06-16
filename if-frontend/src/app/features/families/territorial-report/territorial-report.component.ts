import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import * as echarts from 'echarts';

@Component({
  selector: 'app-territorial-report',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink],
  templateUrl: './territorial-report.component.html',
  styleUrl: './territorial-report.component.css'
})
export class TerritorialReportComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  familyId: string | null = null;
  reportData: any = null;
  loading = true;
  private chart: echarts.ECharts | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.familyId = this.route.snapshot.paramMap.get('id');
    if (this.familyId) this.loadReport();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.chart?.dispose();
    this.chart = null;
  }

  get lastMilestone(): any {
    const list = this.reportData?.milestones ?? [];
    return list[list.length - 1] ?? { icfPercent: 0, riskLevel: '—' };
  }

  get avgChecklist(): number {
    const list: any[] = this.reportData?.milestones ?? [];
    if (!list.length) return 0;
    return list.reduce((s: number, m: any) => s + (m.checklist?.completionPercent ?? 0), 0) / list.length;
  }

  loadReport(): void {
    this.http.get(`/api/families/${this.familyId}/report/territorial`).subscribe({
      next: (res: any) => {
        this.reportData = res.data;
        this.loading = false;
        setTimeout(() => this.initChart(), 0);
      },
      error: () => { this.loading = false; }
    });
  }

  initChart(): void {
    if (!this.chartContainer || !this.reportData) return;

    this.chart = echarts.init(this.chartContainer.nativeElement, 'dark');

    const milestones: any[] = this.reportData.milestones ?? [];
    const xData = milestones.map((m: any) => m.hitoKey);
    const yData = milestones.map((m: any) => m.icfPercent);

    this.chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 48, right: 24, top: 24, bottom: 40 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15,15,20,0.92)',
        borderColor: 'rgba(99,102,241,0.3)',
        textStyle: { color: '#e0e0e0', fontSize: 12 },
        formatter: (p: any) => `${p[0].name}<br/>ICF: <b>${p[0].value}%</b>`
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
        axisLabel: { color: 'rgba(255,255,255,0.35)', fontSize: 11 },
        splitLine: { show: false }
      },
      yAxis: {
        type: 'value', min: 0, max: 100,
        axisLine: { show: false },
        axisLabel: { color: 'rgba(255,255,255,0.3)', fontSize: 11, formatter: '{value}%' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
      },
      series: [{
        data: yData,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3, color: '#6366f1' },
        itemStyle: { color: '#818cf8', borderWidth: 2, borderColor: '#1e1e2e' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99,102,241,0.25)' },
            { offset: 1, color: 'rgba(99,102,241,0.0)' }
          ])
        }
      }]
    });
  }
}
