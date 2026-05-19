import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

/**
 * SDD: Mapa de Rutas del Nodo Central.
 * Postura Técnica: Optimización de carga por chunks y segmentación por dominios de seguridad.
 * REGISTRO: Se activa el 'Admin Sentinel Guard' siguiendo el protocolo de auditoría de William.
 * HMR-TRIGGER: Forzando reconstrucción del grafo de chunks interactivos del Portal Familiar.
 */
export const routes: Routes = [
  // ZONA PÚBLICA
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        title: 'Integrity - Login',
        loadComponent: () => import('./features/auth/login-page.component').then(m => m.LoginPageComponent)
      },
      {
        path: 'register',
        title: 'Integrity - Registro',
        loadComponent: () => import('./features/auth/register-page.component').then(m => m.RegisterPageComponent)
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ZONA PRIVADA (SENTINEL PROTOCOL)
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        title: 'Panel Principal',
        loadComponent: () => import('./features/dashboard/dashboard-page.component').then(m => m.DashboardPageComponent)
      },
      {
        path: 'portal',
        title: 'Portal Familiar Móvil',
        loadComponent: () => import('./features/portal-familiar/portal-familiar.component').then(m => m.PortalFamiliarComponent)
      },

      // Dominio: Gestión Familiar
      {
        path: 'families',
        children: [
          { path: '', loadComponent: () => import('./features/families/family-list-page.component').then(m => m.FamilyListPageComponent) },
          { path: 'create', loadComponent: () => import('./features/families/family-create-page.component').then(m => m.FamilyCreatePageComponent) },
          { path: ':id/report', title: 'Reporte Territorial', loadComponent: () => import('./features/families/territorial-report/territorial-report.component').then(m => m.TerritorialReportComponent) }
        ]
      },

      // Dominio: Evaluación e Inteligencia
      {
        path: 'evaluations',
        children: [
          { path: 'start', loadComponent: () => import('./features/evaluation/evaluation-start-page.component').then(m => m.EvaluationStartPageComponent) },
          { path: ':id/form', loadComponent: () => import('./features/evaluation/evaluation.component').then(m => m.EvaluationComponent) },
          { path: ':id/result', loadComponent: () => import('./features/evaluation/evaluation-result-page.component').then(m => m.EvaluationResultPageComponent) }
        ]
      },

      // Dominio: Administración (Protocolo Sentinel Activo)
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: 'stats', loadComponent: () => import('./features/admin/stats/stats.component').then(m => m.StatsComponent) },
          { path: 'voice-monitor', loadComponent: () => import('./features/admin/voice-monitor/voice-monitor.component').then(m => m.VoiceMonitorComponent) },
          { path: 'sandbox', loadComponent: () => import('./features/admin/sandbox/sandbox.component').then(m => m.SandboxComponent) }
        ]
      },

      // Rutas Transversales
      { path: 'members', loadComponent: () => import('./features/members/member-list-page.component').then(m => m.MemberListPageComponent) },
      { path: 'plans', loadComponent: () => import('./features/plans/plan-list-page.component').then(m => m.PlanListPageComponent) },
      { path: 'checklist', loadComponent: () => import('./features/checklist/checklist-page.component').then(m => m.ChecklistPageComponent) },
      { path: 'chat', loadComponent: () => import('./features/chat/chat-page.component').then(m => m.ChatPageComponent) },
      { path: 'crisis', loadComponent: () => import('./features/crisis/crisis-page.component').then(m => m.CrisisPageComponent) },
      { path: 'logbook', title: 'Bitácora Familiar', loadComponent: () => import('./features/family-logbook/family-logbook.component').then(m => m.FamilyLogbookComponent) },
      { path: 'gratitude', title: 'Muro de Gratitud', loadComponent: () => import('./features/family-gratitude/family-gratitude.component').then(m => m.FamilyGratitudeComponent) },
      { path: 'my-space', title: 'Mi Espacio', loadComponent: () => import('./features/my-space/my-space.component').then(m => m.MySpaceComponent) },
      { path: 'cognitive', title: 'Sistema Cognitivo', loadComponent: () => import('./features/cognitive/cognitive-page.component').then(m => m.CognitivePageComponent) },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // Manejo de Error 404 / Fallback
  { path: '**', redirectTo: 'auth/login' }
];