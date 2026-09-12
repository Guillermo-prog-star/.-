import {
  ActiveHomeView,
  AssessmentHomeView,
  Command,
  OnboardingHomeView,
  ReturnStageHomeView,
} from '../../core/family-home';

/** Fixtures compartidos entre los specs de las vistas del Hogar Digital (Hito 7). */

const BASE = {
  contractVersion: '0.9.0-Candidate',
  viewer: { memberId: 'm-1', role: 'ADULT_MEMBER' as const, permissions: [] },
  family: { id: 'f-1', displayName: 'Familia Lopez' },
  safetyPresentation: { mode: 'NONE' as const },
  responseMetadata: {
    generatedAt: '2026-01-01T00:00:00Z',
    expiresAt: '2026-01-01T01:00:00Z',
    projectionVersion: '1',
    dataStatus: 'FRESH' as const,
  },
  moduleAvailability: {
    todayBlock: 'NOT_APPLICABLE' as const,
    dimensionsBlock: 'NOT_APPLICABLE' as const,
    memoryBlock: 'NOT_APPLICABLE' as const,
  },
};

export function navigateCommand(overrides: Partial<Command> = {}): Command {
  return {
    id: 'cmd-1',
    label: 'Continuar',
    type: 'NAVIGATE',
    target: '/onboarding/members',
    enabled: true,
    requiresConfirmation: false,
    ...overrides,
  };
}

export function submitActionCommand(overrides: Partial<Command> = {}): Command {
  return {
    id: 'cmd-submit',
    label: 'Aceptar Primer Sprint',
    type: 'SUBMIT_ACTION',
    target: 'accept-first-sprint',
    enabled: true,
    requiresConfirmation: false,
    ...overrides,
  };
}

export function buildOnboardingView(overrides: Partial<OnboardingHomeView> = {}): OnboardingHomeView {
  return {
    ...BASE,
    viewType: 'ONBOARDING',
    journey: { stage: 'NEW_FAMILY', progress: { completed: 0, total: 4, percentage: 0 }, nextCommand: navigateCommand() },
    ...overrides,
  };
}

export function buildAssessmentView(overrides: Partial<AssessmentHomeView> = {}): AssessmentHomeView {
  return {
    ...BASE,
    viewType: 'ASSESSMENT',
    journey: {
      stage: 'ASSESSMENT_IN_PROGRESS',
      progress: { completed: 12, total: 40, percentage: 30 },
      nextCommand: navigateCommand({ id: 'cmd-assess', label: 'Continuar evaluación', target: '/journey/assessment' }),
    },
    ...overrides,
  };
}

export function buildReturnStageView(overrides: Partial<ReturnStageHomeView> = {}): ReturnStageHomeView {
  return {
    ...BASE,
    viewType: 'RETURN_STAGE',
    journey: {
      stage: 'RETURN_AVAILABLE',
      progress: { completed: 1, total: 1, percentage: 100 },
      nextCommand: submitActionCommand(),
    },
    ...overrides,
  };
}

export function buildActiveView(overrides: Partial<ActiveHomeView> = {}): ActiveHomeView {
  return {
    ...BASE,
    viewType: 'ACTIVE_HOME',
    journey: { stage: 'ACTIVE_HOME', progress: { completed: 1, total: 1, percentage: 100 } },
    today: {
      narrativeBlock: {
        text: 'Mantente al día con tus actividades compartidas.',
        provenance: {
          generatorType: 'RULE_ENGINE',
          generatorId: 'g-1',
          reviewStatus: 'AUTO_APPROVED',
          evidencePolicy: 'FAMILY_APPROVED',
          generatedAt: '2026-01-01T00:00:00Z',
        },
      },
      primaryCommand: navigateCommand({ id: 'cmd-primary-active', label: 'Misión de Hoy', target: '/sprint/mission' }),
      displayPace: 'BALANCED',
    },
    activeSprint: {
      status: 'ACTIVE',
      sprintId: 's-1',
      title: 'Sprint 1: Reconectar',
      progress: { completed: 2, total: 7, percentage: 28 },
      todayMission: navigateCommand({ id: 'cmd-mission', label: 'Ver misión de hoy', target: '/sprint/mission/today' }),
    },
    dimensions: {
      emotions: { status: 'STABLE', labelKey: 'family.dimension.emotions.stable', updatedAt: '2026-01-01T00:00:00Z', detailsAvailable: false },
      communication: { status: 'IMPROVING', labelKey: 'family.dimension.communication.improving', updatedAt: '2026-01-01T00:00:00Z', detailsAvailable: false },
    },
    ...overrides,
  };
}
