// ─── SDD Analytics v2 ────────────────────────────────────────────────────────
export interface IcfHistoryPoint {
  evaluationId: number;
  icf: number;
  riskLevel: string;
  hasCrisis: boolean;
  finalizedAt: string | null;
}

export interface DimensionHistoryPoint {
  evaluationId: number;
  finalizedAt: string | null;
  dimensions: {
    emociones?:    number;
    comunicacion?: number;
    habitos?:      number;
    tiempos?:      number;
    [key: string]: number | undefined;
  };
}

// ─── SDD Fases 1–5: Modelos del Sistema Cognitivo Familiar ───────────────────

export type NarrativePhase =
  'AWAKENING' | 'DISCOVERY' | 'TRANSITION' | 'CONSOLIDATION' |
  'CRISIS' | 'RECOVERY' | 'AUTONOMY';

export type DynamicType = 'SUPPORTIVE' | 'BALANCED' | 'DISTANT' | 'CONFLICTIVE';
export type EffectivenessLevel = 'HIGH' | 'MODERATE' | 'LOW' | 'REGRESSING' | 'INSUFFICIENT_DATA';
export type AbandonmentLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
export type SystemRole = 'ANCHOR' | 'PEACEMAKER' | 'ESCALATOR' | 'DISCONNECTED' | 'NEUTRAL';

// ─── Identity ─────────────────────────────────────────────────────────────────
export interface IdentityProfile {
  evolutionStage: string;
  communicationStyle: string;
  conflictStyle: string;
  emotionalExpression: string;
  adaptabilityIndex: number;
  completedCycles: number;
  identityNarrative: string | null;
}

// ─── Narrative ────────────────────────────────────────────────────────────────
export interface NarrativeChapter {
  chapterNumber: number;
  title: string;
  body: string;
  phase: NarrativePhase;
  icfAtOpen: number | null;
  icfAtClose: number | null;
  turningPoint: boolean;
  open: boolean;
  openedAt: string;
  closedAt: string | null;
}

export interface NarrativeResponse {
  familyId: number;
  chapters: NarrativeChapter[];
  currentPhase: string;
  totalChapters: number;
  turningPoints: number;
  storyArcSummary: string;
}

// ─── Graph ────────────────────────────────────────────────────────────────────
export interface DyadDto {
  memberAId: number;
  memberAName: string;
  memberBId: number;
  memberBName: string;
  relationshipType: string;
  dynamicType: DynamicType;
  cohesionScore: number;
  tensionScore: number;
  communicationScore: number;
  healthScore: number;
  evolutionTrend: string;
  roleA: string;
  roleB: string;
}

export interface MemberRoleDto {
  memberId: number;
  memberName: string;
  systemRole: SystemRole;
}

export interface GraphResponse {
  familyId: number;
  dyads: DyadDto[];
  systemRoles: MemberRoleDto[];
  cohesionDensity: number;
  tensionDensity: number;
  conflictiveEdges: number;
  healthy: boolean;
  summary: string;
}

// ─── Reflection ───────────────────────────────────────────────────────────────
export interface ReflectionResponse {
  familyId: number;
  effectivenessLevel: EffectivenessLevel;
  evaluationCount: number;
  icfTrend: number;
  avgAdherence: number;
  reflectionRate: number;
  effectivenessSummary: string;
  abandonmentLevel: AbandonmentLevel;
  abandonmentSignals: string[];
  abandonmentScore: number;
  lessonLearned: string | null;
  updatedNarrative: string | null;
  requiresUrgentAttention: boolean;
  generatedAt: string;
}

// ─── Cognitive Snapshot (todo en uno) ─────────────────────────────────────────
export interface GraphSummary {
  totalDyads: number;
  cohesionDensity: number;
  tensionDensity: number;
  conflictiveEdges: number;
  healthy: boolean;
  systemRoles: MemberRoleDto[];
}

export interface MemoryDto {
  id: number;
  memoryType: string;
  semanticKey: string;
  content: string;
  importanceScore: number;
  sourceType: string;
  createdAt: string;
}

export interface CurrentChapter {
  chapterNumber: number;
  title: string;
  body: string;
  phase: NarrativePhase;
  icfAtOpen: number | null;
  turningPoint: boolean;
}

export interface MemoryResponse {
  familyId: number;
  episodic: MemoryDto[];
  semantic: MemoryDto[];
  procedural: MemoryDto[];
}

export interface CognitiveSnapshot {
  familyId: number;
  identityProfile: IdentityProfile;
  currentChapter: CurrentChapter | null;
  totalChapters: number;
  turningPoints: number;
  graphSummary: GraphSummary;
  recentMemories: MemoryDto[];
  appliedSkills: any[];
  storyArcSummary: string;
  generatedAt: string;
}
