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
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export interface EvaluationStartRequest { familyId: number; memberId?: number | null; }
export interface EvaluationResponse { id: number; familyId: number; memberId: number | null; status: string; startedAt: string; finalizedAt: string | null; }
export interface AnswerItem { questionId: number; answerValue: number; }
export interface EvaluationFinalizeRequest { answers: AnswerItem[]; }
export interface EvaluationResultResponse {
  evaluationId: number; familyId: number; riskLevel: RiskLevel;
  scoreEmotions: number; scoreCommunication: number; scoreHabits: number; scoreTimes: number;
  globalScore: number; riskSnapshotId: number; aiReport: string | null;
  hasCrisis: boolean;
}
export interface QuestionResponse { id: number; questionText: string; dimension: string; bloque: string; }
export interface EvaluationHistory { id: number; familyId: number; memberId: number|null; memberName: string|null; status: string; startedAt: string; finalizedAt: string|null; }

// Risk
export interface RiskHistory { id: number; evaluationId: number; riskLevel: RiskLevel; scoreEmotions: number; scoreCommunication: number; scoreHabits: number; scoreTimes: number; globalScore: number; createdAt: string; }

// Plan
export interface PlanTask { id: number; title: string; description: string; assignedMemberId: number|null; assignedMemberName: string|null; completed: boolean; dimension?: string; periodicityMonths?: number; dueDate?: string; }
export interface Plan { id: number; familyId: number; evaluationId: number; title: string; description: string; aiReport: string|null; aiGeneratedAt: string|null; status: string; tasks: PlanTask[]; }

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