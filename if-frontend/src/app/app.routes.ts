import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

/**
 * SDD: Mapa de Rutas del Nodo Central.
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
      {
        path: 'forgot-password',
        title: 'Recuperar contraseña',
        loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent)
      },
      {
        path: 'reset-password',
        title: 'Nueva contraseña',
        loadComponent: () => import('./features/auth/reset-password.component').then(m => m.ResetPasswordComponent)
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
      {
        path: 'portal/invisible-stories',
        title: 'Historias Invisibles',
        loadComponent: () => import('./features/portal-familiar/invisible-stories/invisible-stories.component').then(m => m.InvisibleStoriesComponent)
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
          { path: 'start',   title: 'Nueva Evaluación',    loadComponent: () => import('./features/evaluation/evaluation-start-page.component').then(m => m.EvaluationStartPageComponent) },
          { path: 'history', title: 'Historial de Diagnósticos', loadComponent: () => import('./features/evaluation/evaluation-history-page.component').then(m => m.EvaluationHistoryPageComponent) },
          { path: 'evolution', title: 'Evolución Clínica', loadComponent: () => import('./features/evaluation/evaluation-evolution-page.component').then(m => m.EvaluationEvolutionPageComponent) },
          { path: 'inferences', title: 'Trazabilidad Epistémica', loadComponent: () => import('./features/scanner/inference-history-page.component').then(m => m.InferenceHistoryPageComponent) },
          { path: 'analytics',  title: 'Panel Clínico IF-VIS',  loadComponent: () => import('./features/scanner/scanner-analytics-page.component').then(m => m.ScannerAnalyticsPageComponent) },
          { path: ':id/form',   loadComponent: () => import('./features/evaluation/evaluation.component').then(m => m.EvaluationComponent) },
          { path: ':id/result', loadComponent: () => import('./features/evaluation/evaluation-result-page.component').then(m => m.EvaluationResultPageComponent) }
        ]
      },

      // Dominio: Administración
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: 'stats', loadComponent: () => import('./features/admin/stats/stats.component').then(m => m.StatsComponent) },
          { path: 'voice-monitor', loadComponent: () => import('./features/admin/voice-monitor/voice-monitor.component').then(m => m.VoiceMonitorComponent) },
          { path: 'sandbox', loadComponent: () => import('./features/admin/sandbox/sandbox.component').then(m => m.SandboxComponent) },
          { path: 'eedsl', title: 'Reglas EEDSL', loadComponent: () => import('./features/admin/emotional-rules/emotional-rules-page.component').then(m => m.EmotionalRulesPageComponent) }
        ]
      },

      // Rutas Transversales
      { path: 'members', loadComponent: () => import('./features/members/member-list-page.component').then(m => m.MemberListPageComponent) },
      { path: 'plans', loadComponent: () => import('./features/plans/plan-list-page.component').then(m => m.PlanListPageComponent) },
      { path: 'plans/mission/:taskId', loadComponent: () => import('./features/plans/mission-detail-page.component').then(m => m.MissionDetailPageComponent) },
      { path: 'checklist', loadComponent: () => import('./features/checklist/checklist-page.component').then(m => m.ChecklistPageComponent) },
      { path: 'chat', loadComponent: () => import('./features/chat/chat-page.component').then(m => m.ChatPageComponent) },
      { path: 'crisis', loadComponent: () => import('./features/crisis/crisis-page.component').then(m => m.CrisisPageComponent) },
      { path: 'logbook', title: 'Bitácora Familiar', loadComponent: () => import('./features/family-logbook/family-logbook.component').then(m => m.FamilyLogbookComponent) },
      { path: 'gratitude', title: 'Muro de Gratitud', loadComponent: () => import('./features/family-gratitude/family-gratitude.component').then(m => m.FamilyGratitudeComponent) },
      { path: 'my-space', title: 'Mi Espacio', loadComponent: () => import('./features/my-space/my-space.component').then(m => m.MySpaceComponent) },
      { path: 'cognitive', title: 'Sistema Cognitivo', loadComponent: () => import('./features/cognitive/cognitive-page.component').then(m => m.CognitivePageComponent) },
      { path: 'profile', title: 'Mi Perfil', loadComponent: () => import('./features/profile/profile-page.component').then(m => m.ProfilePageComponent) },

      // Guardián Familiar
      {
        path: 'guardian/:familyId/election',
        title: 'Elegir Guardián Familiar',
        loadComponent: () => import('./features/guardian/guardian-election.component').then(m => m.GuardianElectionComponent)
      },

      // ── Flujo Maestro: Transformación ────────────────────────────────
      {
        path: 'transformation',
        children: [
          {
            path: 'route',
            title: 'Ruta de Transformación — 36 Meses',
            loadComponent: () => import('./features/transformation/transformation-route.component')
              .then(m => m.TransformationRouteComponent)
          },
          {
            path: 'weekly-plan',
            title: 'Planeación Semanal Familiar',
            loadComponent: () => import('./features/transformation/weekly-plan.component')
              .then(m => m.WeeklyPlanComponent)
          },
          {
            path: 'error-protocol',
            title: 'Gestión del Error Familiar',
            loadComponent: () => import('./features/transformation/error-protocol.component')
              .then(m => m.ErrorProtocolComponent)
          },
        ]
      },

      // ── Legado Familiar ──────────────────────────────────────────────
      {
        path: 'legado',
        title: 'Legado Familiar',
        loadComponent: () => import('./features/legado/legado.component')
          .then(m => m.LegadoComponent)
      },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // Manejo de Error 404 / Fallback
  { path: '**', redirectTo: 'auth/login' }
];