/**
 * Tipos TypeScript del contrato IFRM-D FamilyHomeView v0.9.0-Candidate.
 * Generados a mano a partir de los DTOs Java reales en
 * backend/src/main/java/com/integrityfamily/dto/home/ (fuente de verdad — el
 * JSON Schema en backend/src/main/resources/schemas/family-home-view.schema.json
 * describe el mismo contrato, pero los DTOs son lo que Jackson serializa
 * realmente sobre el wire).
 *
 * No editar a mano campo por campo sin revisar el DTO Java correspondiente:
 * cualquier divergencia entre este archivo y el backend rompe silenciosamente
 * el contrato (ver hallazgo IFRM-D Hito 1 sobre PausedHomeView).
 */

export const FAMILY_HOME_CONTRACT_VERSION = '0.9.0-Candidate';

// ── Enums ────────────────────────────────────────────────────────────────

export type ViewerRole = 'ADULT_MEMBER' | 'YOUNG_MEMBER' | 'SUPPORT_PERSON';

export type ViewType = 'ONBOARDING' | 'ASSESSMENT' | 'RETURN_STAGE' | 'ACTIVE_HOME' | 'PAUSED_HOME';

export type JourneyStage =
  | 'NEW_FAMILY'
  | 'PROFILE_IN_PROGRESS'
  | 'ASSESSMENT_IN_PROGRESS'
  | 'ASSESSMENT_COMPLETED'
  | 'RETURN_AVAILABLE'
  | 'FIRST_SPRINT_PENDING'
  | 'ACTIVE_HOME'
  | 'PAUSED_HOME'
  | 'RESUMING_HOME';

export type CommandType = 'NAVIGATE' | 'SUBMIT_ACTION' | 'OPEN_MODAL' | 'CONTACT_SUPPORT';

export type SafetyMode = 'NONE' | 'SUPPORT_AVAILABLE' | 'CONTACT_PROFESSIONAL' | 'URGENT_GUIDANCE';

export type DataStatus = 'FRESH' | 'STALE' | 'PARTIAL' | 'DEGRADED';

export type ModuleStatus =
  | 'AVAILABLE'
  | 'INSUFFICIENT_DATA'
  | 'PROCESSING'
  | 'EXPIRED'
  | 'STALE'
  | 'DISABLED'
  | 'NOT_APPLICABLE'
  | 'ERROR';

export type DimensionStatus = 'IMPROVING' | 'STABLE' | 'DECLINING' | 'INSUFFICIENT_DATA';

export type GeneratorType = 'RULE_ENGINE' | 'AI_GENERATOR' | 'HUMAN_INPUT';

export type ReviewStatus = 'AUTO_APPROVED' | 'MANUALLY_REVIEWED';

export type EvidencePolicy = 'FAMILY_APPROVED';

export type SprintDisplayStatus = 'ACTIVE' | 'NO_MISSION_TODAY' | 'PROCESSING' | 'COMPLETED' | 'PAUSED';

export type DisplayPace = 'LIGHT' | 'BALANCED' | 'ACTIVE' | 'PAUSED' | 'RETURNING';

export type MediaProcessingStatus = 'PENDING' | 'PROCESSED' | 'FAILED';

export type ConsentStatus = 'GRANTED' | 'REVOKED' | 'PENDING' | 'NOT_REQUIRED';

export type MediaVisibility = 'FAMILY_SHARED' | 'OWNER_ONLY' | 'PROFESSIONAL_SHARED';

// ── Bloques reutilizables ────────────────────────────────────────────────

export interface Command {
  id: string;
  label: string;
  type: CommandType;
  target: string;
  enabled: boolean;
  requiresConfirmation: boolean;
  permission?: string | null;
  analyticsEvent?: string | null;
  idempotencyRequired?: boolean | null;
}

export interface MediaAsset {
  assetId: string;
  /** Ausente cuando consentStatus es REVOKED o PENDING — nunca asumir presencia. */
  url?: string | null;
  altText?: string | null;
  ownerId?: string | null;
  processedStatus: MediaProcessingStatus;
  consentStatus: ConsentStatus;
  visibility: MediaVisibility;
  expiresAt?: string | null;
}

export interface ViewerContext {
  memberId: string;
  role: ViewerRole;
  permissions: string[];
}

export interface FamilyContext {
  id: string;
  displayName: string;
  featuredImage?: MediaAsset | null;
}

export interface ProgressBlock {
  completed: number;
  total: number;
  percentage: number;
}

export interface JourneyContext {
  stage: JourneyStage;
  progress: ProgressBlock;
  nextCommand?: Command | null;
}

export interface SafetyPresentation {
  mode: SafetyMode;
  title?: string | null;
  message?: string | null;
  allowedCommands?: Command[] | null;
}

export interface ResponseMetadata {
  generatedAt: string;
  expiresAt: string;
  projectionVersion: string;
  dataStatus: DataStatus;
  correlationId?: string | null;
}

export interface ModuleAvailability {
  todayBlock: ModuleStatus;
  dimensionsBlock: ModuleStatus;
  memoryBlock: ModuleStatus;
}

export interface NarrativeProvenance {
  generatorType: GeneratorType;
  generatorId: string;
  promptVersion?: string | null;
  sourceIds?: string[] | null;
  reviewStatus: ReviewStatus;
  evidencePolicy: EvidencePolicy;
  generatedAt: string;
  validUntil?: string | null;
  locale?: string | null;
  editedByHuman?: boolean | null;
}

export interface NarrativeBlock {
  text: string;
  provenance: NarrativeProvenance;
}

export interface DimensionBlock {
  status: DimensionStatus;
  labelKey: string;
  updatedAt: string;
  detailsAvailable: boolean;
}

export interface ActiveSprintBlock {
  status: SprintDisplayStatus;
  sprintId: string;
  title: string;
  progress: ProgressBlock;
  /** Presente únicamente cuando status === 'ACTIVE'. */
  todayMission?: Command | null;
}

export interface TodayBlock {
  narrativeBlock: NarrativeBlock;
  primaryCommand: Command;
  displayPace: DisplayPace;
}

export interface ResumeBlock {
  instructionsKey: string;
  confirmCommand: Command;
}

// ── Variantes polimórficas (discriminadas por viewType) ──────────────────

interface FamilyHomeViewBase {
  contractVersion: string;
  viewer: ViewerContext;
  family: FamilyContext;
  journey: JourneyContext;
  safetyPresentation: SafetyPresentation;
  responseMetadata: ResponseMetadata;
  moduleAvailability: ModuleAvailability;
}

export interface OnboardingHomeView extends FamilyHomeViewBase {
  viewType: 'ONBOARDING';
}

export interface AssessmentHomeView extends FamilyHomeViewBase {
  viewType: 'ASSESSMENT';
}

export interface ReturnStageHomeView extends FamilyHomeViewBase {
  viewType: 'RETURN_STAGE';
}

export interface ActiveHomeView extends FamilyHomeViewBase {
  viewType: 'ACTIVE_HOME';
  today: TodayBlock;
  /** Requerido cuando journey.stage === 'ACTIVE_HOME'. */
  activeSprint?: ActiveSprintBlock | null;
  dimensions: Record<string, DimensionBlock>;
  /** Requerido cuando journey.stage === 'RESUMING_HOME'. */
  resumeBlock?: ResumeBlock | null;
}

export interface PausedHomeView extends FamilyHomeViewBase {
  viewType: 'PAUSED_HOME';
  /** Campo de nivel superior en el DTO real — no anidado bajo "today". */
  displayPace: 'PAUSED';
}

export type FamilyHomeView =
  | OnboardingHomeView
  | AssessmentHomeView
  | ReturnStageHomeView
  | ActiveHomeView
  | PausedHomeView;

// ── Family Action Engine (Hito 5) ────────────────────────────────────────

export interface AcceptFirstSprintRequest {
  objective?: string;
  riskDimension?: string;
  durationDays?: number;
  missions?: string[];
}

export interface FamilyActionResult {
  action: string;
  status: string;
  sprintId?: string | null;
  executedAt: string;
  replayed: boolean;
}
