import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  styles: [`
    .sidebar { 
      width: 280px;
      background: var(--primary); 
      height: 100vh; 
      padding: 32px 0; 
      display: flex; 
      flex-direction: column; 
      position: fixed; /* Ahora es estático respecto a la pantalla */
      top: 0; 
      left: 0;
      font-family: 'Inter', sans-serif; 
      border-right: 1px solid rgba(255,255,255,0.05); 
      z-index: 1000;
      overflow-y: auto; /* Permite scroll si hay pantallas pequeñas */
    }
    /* Ocultar barra de scroll para estética premium */
    .sidebar::-webkit-scrollbar { width: 0; }
    
    .brand { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 0 20px 24px; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 24px; }
    .logo-circle { width: 64px; height: 64px; background: linear-gradient(135deg, var(--accent), var(--primary-light)); border-radius: 16px; display: flex; align-items: center; justify-content: center; color: white; font-weight: 900; font-size: 28px; box-shadow: 0 10px 20px rgba(0,0,0,0.4); }
    .brand-title { color: #fff; font-size: 15px; font-weight: 800; letter-spacing: 2px; margin-top: 14px;}
    .version-tag { font-size: 9px; color: var(--accent); font-weight: bold; background: var(--accent-glow); padding: 4px 12px; border-radius: 20px; margin-top: 6px; border: 1px solid var(--accent-glow); }
    
    nav { flex: 1; padding: 0 16px; }
    .nav-item { display: flex; align-items: center; gap: 14px; padding: 14px 20px; border-radius: 12px; color: rgba(255,255,255,.5); font-size: 15px; font-weight: 500; margin-bottom: 6px; transition: all .3s cubic-bezier(0.4, 0, 0.2, 1); text-decoration: none; }
    .nav-item:hover { color: #fff; background: rgba(255,255,255,0.05); transform: translateX(6px); }
    .nav-item.active { background: var(--accent-glow); color: var(--accent); border: 1px solid hsla(239, 84%, 67%, 0.2); }
    .icon { font-size: 20px; filter: grayscale(1) brightness(2); }
    .active .icon { filter: none; }
    
    .divider { height: 1px; background: rgba(255,255,255,0.05); margin: 24px 32px; }
    .family-box { margin: 24px; padding: 20px; background: rgba(255,255,255,0.03); border-radius: 16px; border: 1px solid rgba(255,255,255,0.05); backdrop-filter: blur(10px); }
    .f-name { color: #fff; font-size: 14px; font-weight: 700; margin-bottom: 4px; }
    .f-milestone { color: var(--accent); font-size: 11px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: bold; }
  `],
  template: `
    <div class="sidebar">
      <div class="brand">
        <div class="logo-circle">IF</div>
        <div class="brand-title">INTEGRITY FAMILY</div>
        <div class="version-tag">SISTEMA INTEGRAL v4.1</div>
      </div>
      
      <nav>
        <a routerLink="/dashboard" class="nav-item" routerLinkActive="active"><span class="icon">📊</span> Dashboard</a>
        <div class="divider"></div>
        <a routerLink="/families"  class="nav-item" routerLinkActive="active"><span class="icon">👨‍👩‍👧‍👦</span> 1. Familias</a>
        <a routerLink="/members"   class="nav-item" routerLinkActive="active"><span class="icon">👥</span> 2. Miembros (Equipo)</a>
        <a routerLink="/evaluations/start" class="nav-item" routerLinkActive="active"><span class="icon">◈</span> 3. Diagnóstico</a>
        <a routerLink="/plans"      class="nav-item" routerLinkActive="active"><span class="icon">📜</span> 4. Planes de Acción</a>
        <a routerLink="/checklist"  class="nav-item" routerLinkActive="active"><span class="icon">☑</span> 5. Mi Ruta (Hábitos)</a>
        <a routerLink="/chat"       class="nav-item" routerLinkActive="active"><span class="icon">✨</span> 6. Consultor IA</a>
      </nav>

      <div class="family-box">
        <div class="f-name">{{ familyName }}</div>
        <div class="f-milestone">● {{ milestone }}</div>
      </div>
    </div>`
})
export class SidebarComponent {
  get familyName() { return localStorage.getItem('selectedFamilyName') ?? 'Sin Familia'; }
  get milestone()  { return localStorage.getItem('currentMilestone')   ?? 'Inicio'; }
}