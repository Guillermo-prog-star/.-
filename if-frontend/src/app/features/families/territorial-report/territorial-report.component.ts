import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import * as echarts from 'echarts';

@Component({
  selector: 'app-territorial-report',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './territorial-report.component.html',
  styleUrl: './territorial-report.component.css'
})
export class TerritorialReportComponent implements OnInit, AfterViewInit {
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
    if (this.familyId) {
      this.loadReport();
    }
  }

  ngAfterViewInit(): void {
    // El chart se inicializará después de que carguen los datos
  }

  loadReport(): void {
    this.http.get(`/api/families/${this.familyId}/report/territorial`)
      .subscribe({
        next: (res: any) => {
          this.reportData = res.data;
          this.loading = false;
          setTimeout(() => this.initChart(), 0); // Esperar a que el DOM se actualice
        },
        error: (err) => {
          console.error('Error loading report', err);
          this.loading = false;
        }
      });
  }

  initChart(): void {
    if (!this.chartContainer || !this.reportData) return;

    this.chart = echarts.init(this.chartContainer.nativeElement);
    
    const milestones = this.reportData.milestones || [];
    const xAxisData = milestones.map((m: any) => m.hitoKey);
    const seriesData = milestones.map((m: any) => m.icfPercent);

    const option = {
      title: {
        text: 'Evolución del ICF %',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: xAxisData
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100
      },
      series: [
        {
          data: seriesData,
          type: 'line',
          smooth: true,
          areaStyle: {
            opacity: 0.2
          },
          lineStyle: {
            width: 3
          },
          itemStyle: {
            color: '#4f46e5'
          }
        }
      ]
    };

    this.chart.setOption(option);
  }
}
