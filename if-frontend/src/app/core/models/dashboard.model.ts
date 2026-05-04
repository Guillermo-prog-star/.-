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
}