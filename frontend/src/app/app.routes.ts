import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Rutas Públicas (Auth)
  { path: 'login',    loadComponent: () => import('./features/auth/login-page.component').then(m => m.LoginPageComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register-page.component').then(m => m.RegisterPageComponent) },

  // Rutas Privadas (Bajo el Shell y Guard de Seguridad)
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard',          loadComponent: () => import('./features/dashboard/dashboard-page.component').then(m => m.DashboardPageComponent) },
      
      // 👈 NUEVA: Auditoría de Reactivos para William (Nodo Armenia)
      { path: 'admin/stats',        loadComponent: () => import('./features/admin/stats/stats.component').then(m => m.StatsComponent) },

      { path: 'families',           loadComponent: () => import('./features/families/family-list-page.component').then(m => m.FamilyListPageComponent) },
      { path: 'families/create',    loadComponent: () => import('./features/families/family-create-page.component').then(m => m.FamilyCreatePageComponent) },
      { path: 'members',            loadComponent: () => import('./features/members/member-list-page.component').then(m => m.MemberListPageComponent) },
      { path: 'evaluations/start',  loadComponent: () => import('./features/evaluation/evaluation-start-page.component').then(m => m.EvaluationStartPageComponent) },
      { 
        path: 'evaluations/:id/form',   
        loadComponent: () => import('./features/evaluation/evaluation.component').then(m => m.EvaluationComponent) 
      },
      { path: 'evaluations/:id/result', loadComponent: () => import('./features/evaluation/evaluation-result-page.component').then(m => m.EvaluationResultPageComponent) },
      { path: 'plans',               loadComponent: () => import('./features/plans/plan-list-page.component').then(m => m.PlanListPageComponent) },
      { path: 'checklist',           loadComponent: () => import('./features/checklist/checklist-page.component').then(m => m.ChecklistPageComponent) },
      { path: 'chat',                loadComponent: () => import('./features/chat/chat-page.component').then(m => m.ChatPageComponent) },
      
      // Redirección por defecto al Dashboard
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },

  // Comodín para rutas inexistentes
  { path: '**', redirectTo: 'login' }
];