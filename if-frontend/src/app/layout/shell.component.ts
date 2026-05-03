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
    <div class="flex min-h-screen bg-[#0a0a0c]">
      <app-sidebar class="hidden md:block w-[280px] fixed h-full border-r border-white/5" />
      
      <div class="flex-1 md:ml-[280px] flex flex-col min-w-0">
        
        <div *ngIf="sentinel.hasCriticalAlert()" 
             class="bg-red-600 text-white px-6 py-2 flex justify-between items-center animate-pulse z-[60] shadow-2xl border-b border-red-500">
           <div class="flex items-center gap-3">
              <span class="text-xl">🚨</span>
              <span class="text-[10px] font-black uppercase tracking-widest text-red-100">
                Alerta Sentinel: Se requiere intervención en red Alfa
              </span>
           </div>
           <a [routerLink]="['/admin/stats']" 
              class="text-[9px] font-bold bg-white text-red-600 px-4 py-1.5 rounded-full hover:bg-black hover:text-white transition-all uppercase">
              Analizar Crisis
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