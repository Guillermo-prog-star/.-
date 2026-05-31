export type RiskLevel = 'BAJO' | 'MEDIO' | 'ALTO' | 'CRITICO';

export interface DimensionScore {
  emociones: number;
  comunicacion: number;
  habitos: number;
  tiempos: number;
}

export interface SuggestedAction {
  id: number;
  description: string;
  dimension: string;
  completed: boolean;
}

export interface FamilyProgressResponse {
  familyId: number;
  currentEvaluationId: number;
  previousEvaluationId: number | null;
  milestoneCode: string;
  previousIcf: number;
  currentIcf: number;
  deltaIcf: number;
  classification: string;
  interpretation: string;
  dimensionEvolution: { [key: string]: number };
  recommendedAction: string;
}

export interface DashboardDTO {
  id: number;
  familyCode: string;
  currentMilestone: string;
  latestGlobalScore: number;
  latestConsciousnessLabel: string;
  isSentinelActive: boolean;
  dimensionScores: DimensionScore;
  totalPlanTasks: number;
  completedPlanTasks: number;
  pillarProgress: number;
  aiRecommendation: string;
  suggestedActions: SuggestedAction[];
  activeCrisesCount: number;
  riskTrend: 'UP' | 'DOWN' | 'STABLE';
  readyToAdvance: boolean;
  awarenessGrowth: number;
  totalEvaluations: number;
  openLogbookEntriesCount?: number;
  latestFamilyAgreement?: string;
  progress?: FamilyProgressResponse;
  planAiReport?: string;

  // ── Estado Longitudinal (GAP 2 resuelto) ─────────────────────────────────
  evolutionPhase?: 'inconsciente' | 'reactivo' | 'consciente' | 'pleno';
  narrativeStage?: 'RECONOCIMIENTO' | 'AMOR' | 'ENTREGA';
  icfDelta30d?: number;
  crisisCount30d?: number;
  consecutiveDeteriorations?: number;
  communicationCollapseActive?: boolean;
  inActiveCrisis?: boolean;
  criticalDimension?: 'emociones' | 'comunicacion' | 'habitos' | 'tiempos';

  // ── Motor Inferencial Causal (GAP 3 resuelto) ─────────────────────────────
  activeCausalRules?: string[];
  causalExplanations?: string[];
  requiresImmediateIntervention?: boolean;
}

// ── Estado Longitudinal completo (endpoint /longitudinal-state) ───────────────
export interface LongitudinalStateDTO {
  familyId: number;
  icfCurrent: number;
  icfDelta30d: number;
  riskTrend: 'IMPROVING' | 'STABLE' | 'DETERIORATING' | 'CRITICAL';
  currentRiskLevel: 'BAJO' | 'MODERADO' | 'ALTO' | 'CRITICO';
  dimEmociones: number;
  dimComunicacion: number;
  dimHabitos: number;
  dimTiempos: number;
  criticalDimension: string;
  crisisCount30d: number;
  crisisCountTotal: number;
  consecutiveDeteriorations: number;
  consecutiveImprovements: number;
  communicationCollapseActive: boolean;
  evolutionPhase: string;
  narrativeStage: string;
  consciousnessLevel: number;
  consciousnessLabel: string;
  planAdherencePercent: number;
  inactivityDays: number;
  inActiveCrisis: boolean;
  hasEmotionalDeterioration: boolean;
  isImprovingTrend: boolean;
}

// ── Motor Inferencial Causal (endpoint /causal-inference) ────────────────────
export interface CausalInferenceDTO {
  familyId: number;
  icf: number;
  inferredRiskLevel: string;
  trend: string;
  evolutionPhase: string;
  narrativeStage: string;
  consciousnessLevel: number;
  consciousnessLabel: string;
  criticalDimension: string;
  communicationCollapseActive: boolean;
  evolutionMilestoneReached: boolean;
  activeRules: string[];
  explanations: string[];
  /** true si requiere intervención inmediata (CRITICO o colapso comunicacional) */
  requiresImmediateIntervention?: boolean;
}