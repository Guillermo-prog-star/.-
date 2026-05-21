export interface Question {
  id: number;
  dimension: string;
  area: string;      // Nuevo: coincide con Backend Entity
  vertice?: string;  // Deprecated: antes se llamaba así
  text: string;      // Nuevo: coincide con Backend Entity
  questionText?: string; // Deprecated: antes se llamaba así
  active: boolean;

  // --- Nueva Taxonomía del Modelo Híbrido Adaptativo ---
  questionKey?: string;
  pillar?: string;
  phase?: string;
  type?: string; // CORE, ADAPTIVE, FASE_PILLAR, MIRROR, EXPLORATORY
  severityWeight?: number;
  detectsRelapse?: boolean;
  requiresEvidence?: boolean;
  reverseQuestion?: boolean;
  category?: string;
  adaptiveTriggers?: string;
  evidenceType?: string;

  // --- Taxonomía Longitudinal v2 ---
  pillarName?: string;
  milestoneCode?: string;
  memberType?: string;
  riskType?: string;
  missionGenerator?: string;
}