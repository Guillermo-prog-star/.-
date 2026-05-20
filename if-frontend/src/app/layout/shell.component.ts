import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../shared/components/sidebar.component';
import { NavbarComponent } from '../shared/components/navbar.component';
import { FeedbackDialogComponent } from '../shared/components/feedback-dialog/feedback-dialog.component';
import { SentinelCoreService } from '../core/services/sentinel-core.service';

/**
 * SDD: Shell Sentinel Core
 * Postura Técnica: Orquestación de layout con monitoreo reactivo de crisis.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    CommonModule,
    SidebarComponent,
    NavbarComponent,
    FeedbackDialogComponent
  ],
  template: `
    <div class="flex h-screen bg-[#0a0a0c] overflow-hidden">
      <app-sidebar class="hidden md:block w-[280px] fixed h-full border-r border-white/5" />
      
      <div class="flex-1 md:ml-[280px] flex flex-col min-w-0 h-full">
        
        <div *ngIf="sentinel.hasCriticalAlert()" 
             class="bg-red-500/10 backdrop-blur-md border-b border-red-500/20 text-white px-6 py-3 flex justify-between items-center z-[60] shadow-[0_4px_30px_rgba(239,68,68,0.15)] relative overflow-hidden">
           <div class="absolute bottom-0 left-0 h-[2px] w-full bg-gradient-to-r from-transparent via-red-500 to-transparent animate-pulse"></div>
           <div class="flex items-center gap-3">
              <span class="text-xl animate-bounce">🚨</span>
              <span class="text-xs font-extrabold uppercase tracking-widest text-red-200">
                Alerta Sentinel: Se requiere intervención en red Alfa
              </span>
           </div>
           <a [routerLink]="['/admin/stats']" 
              class="text-[10px] font-black bg-gradient-to-r from-red-600 to-rose-600 text-white px-5 py-2 rounded-xl hover:shadow-[0_0_15px_rgba(239,68,68,0.5)] transform hover:scale-105 transition-all uppercase tracking-wider border border-red-500/30">
              Analizar Crisis →
           </a>
        </div>

        <app-navbar />
        
        <main class="p-6 md:p-10 flex-1 overflow-y-auto">
          <router-outlet />
        </main>
      </div>

      <app-feedback-dialog />
    </div>
  `
})
export class ShellComponent {
  // Inyección de servicio core para reactividad de señales
  constructor(public sentinel: SentinelCoreService) {}
}