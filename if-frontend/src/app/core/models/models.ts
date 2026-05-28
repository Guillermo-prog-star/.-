// Auth
export interface LoginRequest  { email: string; password: string; }
export interface LoginResponse { userId: number; fullName: string; email: string; accessToken: string; }
export interface RegisterRequest { fullName: string; email: string; password: string; }

// Family
export interface Family {
  id: number; name: string; description: string; familyCode: string;
  currentMilestone: string; municipio: string; whatsapp: string;
  pin?: string; sentinelActive?: boolean;
  members?: any[];
  // Guardián Familiar
  guardianMemberId?: number | null;
  guardianFullName?: string | null;
  guardianSince?: string | null;
  participationScore?: number;
}

// ── Guardián Familiar ──────────────────────────────────────────────────────

export type MissionStatus   = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type MissionCategory = 'CONEXION' | 'COMUNICACION' | 'GRATITUD' | 'HABITOS' | 'MEMORIA' | 'REFLEXION' | 'BIENESTAR';

export interface MissionTemplate {
  title: string; description: string;
  category: MissionCategory; durationMinutes: number;
  difficulty: string; emoji: string;
}

export interface MissionDto {
  id: number; title: string; description: string;
  category: MissionCategory; durationMinutes: number;
  status: MissionStatus;
  createdByMemberId?: number; createdByFullName?: string;
  activatedAt?: string; completedAt?: string; createdAt?: string;
}

export interface VoteCount { memberId: number; fullName: string; votes: number; }

export interface GuardianStatusResponse {
  familyId: number;
  hasGuardian: boolean;
  guardianMemberId?: number;
  guardianFullName?: string;
  guardianSince?: string;
  totalVotes: number;
  voteCounts: VoteCount[];
  currentUserHasVoted: boolean;
  activeMission?: MissionDto;
  participationScore: number;
  completedMissions: number;
}

export interface VoteRequest { voterMemberId: number; nominatedMemberId: number; }

export interface MemberPulse {
  memberId: number;
  name: string;
  initials: string;
  activeThisWeek: boolean;
  daysSinceLastActivity: number;
}

export interface DayActivity {
  dayLabel: string;
  eventCount: number;
}

export interface ParticipationPulseResponse {
  totalMembers: number;
  activeThisWeek: number;
  participationRate: number;
  members: MemberPulse[];
  weeklyActivity: DayActivity[];
}

export interface GuardianMemberSummary {
  memberId: number;
  name: string;
  activeThisWeek: boolean;
  daysSinceLastActivity: number;
}

export interface GuardianBriefingResponse {
  guardianName: string;
  fatigueSignal: 'NONE' | 'MILD' | 'HIGH';
  activeParticipants: number;
  inactiveParticipants: number;
  members: GuardianMemberSummary[];
  currentMilestone: string | null;
  planCompletionRate: number;
  aiMessage: string;
}

export interface ActivateMissionRequest {
  title: string; description: string;
  category: MissionCategory; durationMinutes: number;
  guardianMemberId: number;
}
export interface FamilyCreateRequest { name: string; description: string; municipio?: string; whatsapp?: string; pin?: string; }

// Member
export interface Member {
  id: number; familyId: number; familyName: string; fullName: string;
  roleType: string; age: number; autonomyLevel: number;
  responsibilityLevel: number; active: boolean;
}
export interface MemberRequest {
  familyId: number; fullName: string; roleType: string; age: number;
  autonomyLevel: number; responsibilityLevel: number;
}

// Milestone
export interface Milestone { milestoneKey: string; label: string; months: number; phase: string; bloque: string; sortOrder: number; }

// Evaluation
/** Niveles de riesgo que devuelve RISK_ALGO_V1 */
export type RiskLevel = 'BAJO' | 'MODERADO' | 'ALTO' | 'CRITICO';

export interface EvaluationStartRequest { familyId: number; memberId?: number | null; }
export interface EvaluationResponse {
  id: number; familyId: number; memberId: number | null;
  status: string; startedAt: string; finalizedAt: string | null;
  icf?: number; riskLevel?: string; criticalDimension?: string;
}

/** Score por dimensión devuelto por RISK_ALGO_V1 */
export interface DimensionScoreDto {
  dimension: string;
  score: number;
  normalizedScore: number;
}

/** Resultado enriquecido de RISK_ALGO_V1 (POST /api/assessments/{id}/finalize) */
export interface EvaluationResultResponse {
  evaluationId: number;
  familyId: number;
  riskLevel: string;                // BAJO | MODERADO | ALTO | CRITICO
  dimensionScores: DimensionScoreDto[];
  healthyIndex: number;             // ICF 0-100
  riskSnapshotId?: number;
  spiritualSynthesis?: string;      // Interpretación cualitativa por rol
  hasCrisis: boolean;
  simulationSuspected?: boolean;
  relapseDetected?: boolean;
  suggestedMissionGenerator?: string;
  consciousnessLabel?: string;      // Plena | Madura | Consciente | Reactiva | Inconsciente
  consciousnessLevel?: number;      // 1-5
  relapseFlags?: string[];
  mirrorFlags?: string[];
}
export interface QuestionResponse { id: number; questionText: string; dimension: string; bloque: string; }

/** Resumen de una evaluación del historial familiar (GET /assessments/family/{id}/history) */
export interface EvaluationHistory {
  id: number;
  familyId: number;
  memberId: number | null;
  memberName?: string | null;
  status: string;              // STARTED | FINALIZED | CANCELLED
  startedAt: string;
  finalizedAt: string | null;
  icf?: number | null;
  riskLevel?: string | null;
  criticalDimension?: string | null;
}

/** Punto del timeline evolutivo (GET /assessments/family/{id}/timeline) */
export interface TimelineEntryDto {
  evaluationId: number;
  finalizedAt: string | null;
  healthyIndex: number;
  riskLevel: string;
  criticalDimension?: string | null;
  algorithmVersion?: string;
  /** IF-TOS: EMERGING | STABLE | ESCALATING | CRITICAL | RECOVERING | RESOLVED */
  operationalState?: string | null;
  /** IF-SUM: incertidumbre estructural total 0.0–1.0 */
  uncertaintyTotal?: number | null;
}

// Plan
export interface PlanTaskStep {
  id: number;
  type: string;
  detail: string;
}

export interface PlanTask { 
  id: number; 
  title: string; 
  description: string; 
  assignedMemberId: number|null; 
  assignedMemberName: string|null; 
  completed: boolean; 
  dimension?: string; 
  periodicityMonths?: number; 
  dueDate?: string; 
  fase?: string;
  riesgoAsociado?: string;
  objetivo?: string;
  accionConcreta?: string;
  indicadorCumplimiento?: string;
  evidenciaRequerida?: string;
  impactoIcf?: number;
  steps?: PlanTaskStep[];
  // --- Taxonomía Longitudinal v2 ---
  pillarName?: string;
  milestoneCode?: string;
  memberType?: string;
  riskType?: string;
  missionGenerator?: string;
}
export interface Plan { id: number; familyId: number; evaluationId: number; title: string; description: string; aiReport: string|null; aiGeneratedAt: string|null; status: string; vision3y?: string; tasks: PlanTask[]; }

// Checklist
export interface ChecklistItem { id: number; familyId: number; planId: number|null; planTaskId: number|null; title: string; completed: boolean; }

// Dashboard
export interface DashboardSummary {
  familyId: number; familyName: string; familyCode: string; currentMilestone: string;
  totalMembers: number; totalEvaluations: number; totalPlans: number;
  totalChecklistItems: number; completedChecklistItems: number;
  totalPlanTasks: number; completedPlanTasks: number;
  latestRiskLevel: RiskLevel | null; latestGlobalScore: number;
  latestConsciousnessLevel: number; latestConsciousnessLabel: string;
  hasCrisis: boolean;
  baselineScore: number; awarenessGrowth: number;
  nextEvaluationAt: string | null;
  isQuarterlyMilestone: boolean;
  pillarProgress: number;
  aiRecommendation: string;
  riskHistory: RiskHistory[];
}

export interface RiskHistory { 
  id: number; 
  evaluationId: number; 
  riskLevel: RiskLevel; 
  scoreEmotions: number; 
  scoreCommunication: number; 
  scoreHabits: number; 
  scoreTimes: number; 
  globalScore: number; 
  consciousnessLevel?: number;
  hasCrisis?: boolean;
  createdAt: string; 
}

// IF-Scanner
/** Registro de inferencia formal (GET /api/scanner/family/{id}/inferences) */
export interface InferenceRecordDto {
  id: number;
  evaluationId: number;
  inferenceKey: string;
  epistemicState: string;          // INFERRED | STABILIZED | REVISED | DEPRECATED
  operationalState: string | null; // IF-TOS
  icfValue: number;
  riskLevel: string;
  criticalDimension: string | null;
  uncertaintyTotal: number | null; // IF-SUM 0.0–1.0
  simulationSuspected: boolean | null;
  evidenceHash: string | null;     // IF-CIS: SHA-256 determinístico
  createdAt: string;
}

/** Regla emocional EEDSL (GET /api/admin/eedsl) */
export interface EmotionalRuleDto {
  id: number;
  ruleKey: string;
  version: number;
  active: boolean;
  milestoneScope: string;
  memberRole: string;
  requiredSignals: string[];
  temporalWindowDays: number;
  projectionLabel: string | null;
  confidenceBase: number;
  riskOutput: string | null;
  createdBy: string;
  createdAt: string;
}

export interface EmotionalRuleRequest {
  ruleKey: string;
  milestoneScope?: string;
  memberRole?: string;
  requiredSignals?: string[];
  temporalWindowDays?: number;
  projectionLabel?: string;
  confidenceBase?: number;
  riskOutput?: string;
}

/** Estado operacional actual (GET /api/scanner/family/{id}/state) */
export interface OperationalStateDto {
  familyId: number;
  operationalState: string;
  label: string;
  description: string;
}

/** Alerta clínica generada por IF-ALT */
export interface FamilyAlertDto {
  id: number;
  familyId: number;
  alertType: string;  // CONSECUTIVE_HIGH_RISK | CRITICAL_STATE_SUSTAINED | SIMULATION_REPEAT | RELAPSE_CONFIRMED | MULTI_RULE_ACTIVATION
  severity: string;   // LOW | MEDIUM | HIGH | CRITICAL
  title: string;
  detail: string | null;
  inferenceKey: string | null;
  evaluationId: number | null;
  resolved: boolean;
  resolvedAt: string | null;
  createdAt: string;
}

// Chat
export interface ChatRequest  { familyId: number; message: string; }
export interface ChatResponse { reply: string; familyCode: string; currentMilestone: string; }

export interface DimensionResult {
  dimension: string;
  score: number; // Promedio de 1 a 5
  status: 'Bajo' | 'Medio' | 'Alto';
}