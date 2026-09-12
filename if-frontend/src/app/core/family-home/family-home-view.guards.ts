import {
  ActiveHomeView,
  AssessmentHomeView,
  FamilyHomeView,
  OnboardingHomeView,
  PausedHomeView,
  ReturnStageHomeView,
} from './family-home-view.model';

/**
 * Type guards para discriminar las 5 variantes de FamilyHomeView por viewType.
 * Angular nunca debe leer el JSON crudo del Home ni castear a mano — siempre
 * a través de estos guards o del switch exhaustivo de assertNever().
 */

export function isOnboardingHomeView(view: FamilyHomeView): view is OnboardingHomeView {
  return view.viewType === 'ONBOARDING';
}

export function isAssessmentHomeView(view: FamilyHomeView): view is AssessmentHomeView {
  return view.viewType === 'ASSESSMENT';
}

export function isReturnStageHomeView(view: FamilyHomeView): view is ReturnStageHomeView {
  return view.viewType === 'RETURN_STAGE';
}

export function isActiveHomeView(view: FamilyHomeView): view is ActiveHomeView {
  return view.viewType === 'ACTIVE_HOME';
}

export function isPausedHomeView(view: FamilyHomeView): view is PausedHomeView {
  return view.viewType === 'PAUSED_HOME';
}

/**
 * Ayuda a que el compilador exija manejar las 5 variantes en un switch:
 * si se agrega un viewType nuevo, el `never` deja de tipar y TypeScript
 * marca error en tiempo de compilación en vez de fallar en runtime.
 */
export function assertNeverFamilyHomeView(view: never): never {
  throw new Error(`FamilyHomeView.viewType no manejado: ${JSON.stringify(view)}`);
}
