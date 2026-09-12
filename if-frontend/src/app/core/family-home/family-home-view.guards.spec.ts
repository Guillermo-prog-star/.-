import {
  assertNeverFamilyHomeView,
  isActiveHomeView,
  isAssessmentHomeView,
  isOnboardingHomeView,
  isPausedHomeView,
  isReturnStageHomeView,
} from './family-home-view.guards';
import { FamilyHomeView } from './family-home-view.model';

// ─── Fixture mínimo por variante ───────────────────────────────────────────

const base = {
  contractVersion: '0.9.0-Candidate',
  viewer: { memberId: 'm-1', role: 'ADULT_MEMBER' as const, permissions: [] },
  family: { id: 'f-1', displayName: 'Familia Test' },
  journey: { stage: 'NEW_FAMILY' as const, progress: { completed: 0, total: 1, percentage: 0 } },
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

const onboarding: FamilyHomeView = { ...base, viewType: 'ONBOARDING' };
const assessment: FamilyHomeView = { ...base, viewType: 'ASSESSMENT' };
const returnStage: FamilyHomeView = { ...base, viewType: 'RETURN_STAGE' };
const paused: FamilyHomeView = { ...base, viewType: 'PAUSED_HOME', displayPace: 'PAUSED' };
const active: FamilyHomeView = {
  ...base,
  viewType: 'ACTIVE_HOME',
  today: {
    narrativeBlock: {
      text: 'Hola',
      provenance: {
        generatorType: 'RULE_ENGINE',
        generatorId: 'g',
        reviewStatus: 'AUTO_APPROVED',
        evidencePolicy: 'FAMILY_APPROVED',
        generatedAt: '2026-01-01T00:00:00Z',
      },
    },
    primaryCommand: {
      id: 'c1',
      label: 'Ir',
      type: 'NAVIGATE',
      target: '/x',
      enabled: true,
      requiresConfirmation: false,
    },
    displayPace: 'BALANCED',
  },
  dimensions: {},
};

describe('family-home-view.guards', () => {
  it('isOnboardingHomeView solo es true para ONBOARDING', () => {
    expect(isOnboardingHomeView(onboarding)).toBeTrue();
    expect(isOnboardingHomeView(assessment)).toBeFalse();
  });

  it('isAssessmentHomeView solo es true para ASSESSMENT', () => {
    expect(isAssessmentHomeView(assessment)).toBeTrue();
    expect(isAssessmentHomeView(onboarding)).toBeFalse();
  });

  it('isReturnStageHomeView solo es true para RETURN_STAGE', () => {
    expect(isReturnStageHomeView(returnStage)).toBeTrue();
    expect(isReturnStageHomeView(paused)).toBeFalse();
  });

  it('isActiveHomeView solo es true para ACTIVE_HOME', () => {
    expect(isActiveHomeView(active)).toBeTrue();
    expect(isActiveHomeView(paused)).toBeFalse();
  });

  it('isPausedHomeView solo es true para PAUSED_HOME', () => {
    expect(isPausedHomeView(paused)).toBeTrue();
    expect(isPausedHomeView(active)).toBeFalse();
  });

  it('el compilador narrows el tipo tras el guard (chequeo estático, no runtime)', () => {
    if (isActiveHomeView(active)) {
      // Si esto compila, dimensions existe solo en la rama ACTIVE_HOME.
      expect(active.dimensions).toEqual({});
    }
  });

  it('assertNeverFamilyHomeView lanza en runtime si se le fuerza un valor', () => {
    expect(() => assertNeverFamilyHomeView(onboarding as never)).toThrow();
  });
});
