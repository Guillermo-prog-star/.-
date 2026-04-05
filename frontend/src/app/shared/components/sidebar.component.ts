import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { DashboardSummary } from '../../core/models/models';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  styles: [`
    .sidebar { background: #1A3A2A; height: 100vh; padding: 20px 0; display: flex; flex-direction: column; position: sticky; top: 0; }
    .brand { display: flex; align-items: center; gap: 12px; padding: 0 20px 20px; border-bottom: 1px solid rgba(255,255,255,.1); margin-bottom: 12px; }
    .brand-mark { width: 40px; height: 40px; background: rgba(255,255,255,.15); border-radius: 10px; display: grid; place-items: center; color: #fff; font-weight: 700; font-size: 14px; flex-shrink: 0; }
    .brand-name { color: #fff; font-size: 14px; font-weight: 600; line-height: 1.3; }
    .brand-sub { color: rgba(255,255,255,.5); font-size: 11px; }
    nav { flex: 1; padding: 0 12px; }
    a { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; color: rgba(255,255,255,.7); font-size: 14px; margin-bottom: 2px; transition: all .15s; position: relative; text-decoration: none; }
    a:hover, a.active { background: rgba(255,255,255,0.1); color: #fff; }
    .icon { font-size: 16px; width: 20px; text-align: center; }
    .badge-red { position: absolute; right: 10px; width: 8px; height: 8px; background: #EF4444; border-radius: 50%; box-shadow: 0 0 8px rgba(239, 68, 68, 0.6); animation: pulse 2s infinite; }
    .badge-num { position: absolute; right: 8px; background: #22C55E; color: white; font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 10px; }
    @keyframes pulse { 0% { transform: scale(0.9); opacity: 1; } 50% { transform: scale(1.2); opacity: 0.5; } 100% { transform: scale(0.9); opacity: 1; } }
    .family-info { margin: 12px; padding: 12px; background: rgba(255,255,255,.07); border-radius: 10px; border-top: 1px solid rgba(255,255,255,.1); }
    .fi-name { color: #fff; font-size: 12px; font-weight: 600; }
    .fi-code { color: rgba(255,255,255,.45); font-size: 10px; margin-top: 2px; }
    .fi-hito { margin-top: 8px; background: rgba(255,255,255,.1); border-radius: 5px; padding: 3px 8px; color: rgba(255,255,255,.65); font-size: 10px; }
  `],
  template: `
    <div class="sidebar">
      <div class="brand">
        <div class="brand-mark">IF</div>
        <div>
          <div class="brand-name">Integrity Family</div>
          <div class="brand-sub">v1.0 MVP</div>
        </div>
      </div>
      
      <nav>
        <a routerLink="/dashboard" routerLinkActive="active"><span class="icon">◎</span> Dashboard</a>
        <a routerLink="/families"  routerLinkActive="active"><span class="icon">⌂</span> Familias</a>
        <a routerLink="/members"   routerLinkActive="active"><span class="icon">◑</span> Miembros</a>
        
        <a routerLink="/evaluations/start" routerLinkActive="active">
          <span class="icon">◈</span> Evaluación
          @if (needsEvaluation) { <span class="badge-red"></span> }
        </a>
        
        <a routerLink="/plans" routerLinkActive="active"><span class="icon">▦</span> Planes</a>
        
        <a routerLink="/checklist" routerLinkActive="active">
          <span class="icon">☑</span> Checklist
          @if (pendingTasks > 0) { <span class="badge-num">{{ pendingTasks }}</span> }
        </a>
        
        <a routerLink="/chat" routerLinkActive="active"><span class="icon">◉</span> Consultor IA</a>
      </nav>

      <div class="family-info">
        <div class="fi-name">{{ familyName }}</div>
        <div class="fi-code">{{ familyCode }}</div>
        <div class="fi-hito">🔵 {{ milestone }}</div>
      </div>
    </div>`
})
export class SidebarComponent implements OnInit {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  pendingTasks = 0;
  needsEvaluation = false;

  get familyName() { return localStorage.getItem('selectedFamilyName') ?? 'Sin familia'; }
  get familyCode()  { return localStorage.getItem('selectedFamilyCode') ?? '---'; }
  get milestone()   { return localStorage.getItem('currentMilestone')   ?? 'Inicio'; }

  ngOnInit() {
    this.refreshAlerts();
  }

  refreshAlerts() {
    const familyId = localStorage.getItem('selectedFamilyId');
    if (!familyId || familyId === 'undefined') return;

    this.http.get<{data: DashboardSummary}>(`${this.api.base}/analytics/dashboard/family/${familyId}`)
      .subscribe({
        next: (response) => {
          if (response && response.data) {
            const summary = response.data;
            this.pendingTasks = Math.max(0, (summary.totalChecklistItems || 0) - (summary.completedChecklistItems || 0));
            this.needsEvaluation = (summary.latestGlobalScore ?? 0) === 0;
          }
        },
        error: (err) => console.error('Error al sincronizar alertas del Sidebar:', err)
      });
  }
}