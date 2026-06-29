import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

/**
 * SDD: Mapa de Rutas del Nodo Central.
 */
export const routes: Routes = [
  // PÁGINAS LEGALES — acceso público sin autenticación
  {
    path: 'legal',
    children: [
      {
        path: 'privacy',
        title: 'Política de Privacidad — Integrity Family',
        loadComponent: () => import('./features/legal/privacy-policy.component').then(m => m.PrivacyPolicyComponent)
      },
      {
        path: 'terms',
        title: 'Términos de Uso — Integrity Family',
        loadComponent: () => import('./features/legal/terms-of-use.component').then(m => m.TermsOfUseComponent)
      }
    ]
  },

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
      { path: 'family-dna', title: 'ADN Familiar', loadComponent: () => import('./features/family-dna/family-dna.component').then(m => m.FamilyDnaComponent) },
      { path: 'family-timeline', title: 'Historia Familiar', loadComponent: () => import('./features/family-timeline/family-timeline.component').then(m => m.FamilyTimelineComponent) },
      { path: 'members', loadComponent: () => import('./features/members/member-list-page.component').then(m => m.MemberListPageComponent) },
      { path: 'plans', loadComponent: () => import('./features/plans/plan-list-page.component').then(m => m.PlanListPageComponent) },
      { path: 'plans/mission/:taskId', loadComponent: () => import('./features/plans/mission-detail-page.component').then(m => m.MissionDetailPageComponent) },
      { path: 'checklist', loadComponent: () => import('./features/checklist/checklist-page.component').then(m => m.ChecklistPageComponent) },
      { path: 'evidence/capture', title: 'Cápsula Familiar', loadComponent: () => import('./features/evidence-capture/evidence-capture.component').then(m => m.EvidenceCaptureComponent) },
      { path: 'evidence/documentary', title: 'Mini Documental Familiar', loadComponent: () => import('./features/evidence-capture/mision-documentary.component').then(m => m.MisionDocumentaryComponent) },
      { path: 'documentary-maker', title: 'Motor de Ensamblaje Documental', loadComponent: () => import('./features/documentary-maker/documentary-maker.component').then(m => m.DocumentaryMakerComponent) },
      { path: 'rituals', title: 'Motor de Rituales', loadComponent: () => import('./features/ritual-engine/ritual-engine.component').then(m => m.RitualEngineComponent) },
      { path: 'family-tree', title: 'Árbol Generacional', loadComponent: () => import('./features/family-tree/family-tree.component').then(m => m.FamilyTreeComponent) },
      { path: 'family-pulse',   title: 'Pulso Familiar',   loadComponent: () => import('./features/family-pulse/family-pulse.component').then(m => m.FamilyPulseComponent) },
      { path: 'family-council', title: 'Consejo Familiar', loadComponent: () => import('./features/family-council/family-council.component').then(m => m.FamilyCouncilComponent) },
      { path: 'family-movie',   title: 'Película Familiar',    loadComponent: () => import('./features/family-movie/family-movie.component').then(m => m.FamilyMovieComponent) },
      { path: 'digital-twin',   title: 'Gemelo Digital',       loadComponent: () => import('./features/digital-twin/digital-twin.component').then(m => m.DigitalTwinComponent) },
      { path: 'chat', loadComponent: () => import('./features/chat/chat-page.component').then(m => m.ChatPageComponent) },
      { path: 'crisis',   loadComponent: () => import('./features/crisis/crisis-page.component').then(m => m.CrisisPageComponent) },
      { path: 'reports',  title: 'Reportes', loadComponent: () => import('./features/reports/reports-page.component').then(m => m.ReportsPageComponent) },
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
            title: 'Planeación Mensual Familiar',
            loadComponent: () => import('./features/transformation/weekly-plan.component')
              .then(m => m.WeeklyPlanComponent)
          },
          {
            path: 'error-protocol',
            title: 'Gestión del Error Familiar',
            loadComponent: () => import('./features/transformation/error-protocol.component')
              .then(m => m.ErrorProtocolComponent)
          },
          {
            path: 'adaptive',
            title: 'Integración Adaptativa',
            loadComponent: () => import('./features/transformation/adaptive-plan.component')
              .then(m => m.AdaptivePlanComponent)
          },
        ]
      },

      // ── Linaje Generacional ──────────────────────────────────────────
      {
        path: 'lineage',
        title: 'Linaje Generacional',
        loadComponent: () => import('./features/lineage/lineage-page.component')
          .then(m => m.LineagePageComponent)
      },

      // ── Legado Familiar ──────────────────────────────────────────────
      {
        path: 'legado',
        title: 'Legado Familiar',
        loadComponent: () => import('./features/legado/legado.component')
          .then(m => m.LegadoComponent)
      },

      // ── Salud Familiar — panel ejecutivo ─────────────────────────────
      {
        path: 'health',
        title: 'Salud Familiar',
        loadComponent: () => import('./features/health/health-page.component')
          .then(m => m.HealthPageComponent)
      },

      // ── Radar de Señales Sutiles ─────────────────────────────────────
      {
        path: 'radar',
        title: 'Radar de Señales Sutiles',
        loadComponent: () => import('./features/radar/radar-page.component')
          .then(m => m.RadarPageComponent)
      },

      // ── Viaje Familiar (capa rectora) ────────────────────────────────
      {
        path: 'journey',
        title: 'Viaje Familiar',
        loadComponent: () => import('./features/journey/family-journey-page.component')
          .then(m => m.FamilyJourneyPageComponent)
      },

      // ── Sprint Familiar ──────────────────────────────────────────────
      {
        path: 'sprint',
        title: 'Sprint Familiar',
        loadComponent: () => import('./features/sprint/sprint-board-page.component')
          .then(m => m.SprintBoardPageComponent)
      },

      // ── Banco de Trayectorias de Riesgo ──────────────────────────────
      {
        path: 'trajectory',
        title: 'Trayectorias de Riesgo',
        loadComponent: () => import('./features/trajectory/trajectory-page.component')
          .then(m => m.TrajectoryPageComponent)
      },

      // ── Centro de Documentación ──────────────────────────────────────
      {
        path: 'documentation',
        title: 'Centro de Documentación',
        loadComponent: () => import('./features/documentation/documentation-page.component')
          .then(m => m.DocumentationPageComponent)
      },

      // ── Capital Familiar (ICaF) ───────────────────────────────────────────
      {
        path: 'capital',
        title: 'Capital Familiar — ICaF',
        loadComponent: () => import('./features/capital/icaf-dashboard-page.component')
          .then(m => m.IcafDashboardPageComponent)
      },
      {
        path: 'capital/questionnaire',
        title: 'Cuestionario ICaF',
        loadComponent: () => import('./features/capital/icaf-questionnaire.component')
          .then(m => m.IcafQuestionnaireComponent)
      },

      {
        path: 'capital/observatory',
        title: 'Observatorio Familiar — ICaF',
        loadComponent: () => import('./features/capital/observatory-page.component')
          .then(m => m.ObservatoryPageComponent)
      },

      // ── SMFF — 20 Indicadores de Fortalecimiento ─────────────────────────
      {
        path: 'smff',
        title: 'Fortalecimiento Familiar — SMFF',
        loadComponent: () => import('./features/capital/smff-panel.component')
          .then(m => m.SmffPanelComponent)
      },

      // ── Ecosistema de Apoyo (5 niveles de red) ──────────────────────────
      {
        path: 'ecosystem',
        title: 'Ecosistema de Apoyo',
        loadComponent: () => import('./features/ecosystem/ecosystem-page.component')
          .then(m => m.EcosystemPageComponent)
      },

      // ── Panel del Profesional (terapeuta, orientador, trabajador social) ─
      {
        path: 'professional',
        title: 'Panel del Profesional',
        loadComponent: () => import('./features/professional/professional-dashboard.component')
          .then(m => m.ProfessionalDashboardComponent)
      },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // Manejo de Error 404 / Fallback
  { path: '**', redirectTo: 'auth/login' }
];