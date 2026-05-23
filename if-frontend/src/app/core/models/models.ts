// Auth
export interface LoginRequest  { email: string; password: string; }
export interface LoginResponse { userId: number; fullName: string; email: string; accessToken: string; }
export interface RegisterRequest { fullName: string; email: string; password: string; }

// Family
export interface Family {
  id: number; name: string; description: string; familyCode: string;
  currentMilestone: string; municipio: string; whatsapp: string;
  createdByUserId: number; createdByName: string;
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

// Chat
export interface ChatRequest  { familyId: number; message: string; }
export interface ChatResponse { reply: string; familyCode: string; currentMilestone: string; }

export interface DimensionResult {
  dimension: string;
  score: number; // Promedio de 1 a 5
  status: 'Bajo' | 'Medio' | 'Alto';
}