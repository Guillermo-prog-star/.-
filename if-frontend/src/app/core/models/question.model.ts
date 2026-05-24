// ── DTOs de evaluación incremental ──────────────────────────────────────────

/** Respuesta individual enviada al endpoint POST /{evalId}/answers */
export interface SaveAnswerRequest {
  questionId: number;
  value?: number;         // escala 1-5
  booleanAnswer?: boolean;
}

/** Respuesta ya persistida (devuelta por GET /{evalId}/answers) */
export interface SavedAnswerDto {
  questionId?: number;
  questionKey?: string;
  score: number;
  booleanAnswer?: boolean;
  dimension?: string;
  answeredAt?: string;
}

/** Progreso del cuestionario */
export interface AnswerProgressResponse {
  evaluationId: number;
  answered: number;
  totalExpected: number;
  canFinalize: boolean;
  answers: SavedAnswerDto[];
}

/** Body del endpoint POST /evaluations/{id}/finalize */
export interface FinalizeRequest {
  /** Si se omite (mobile-first), el backend carga las respuestas guardadas desde BD. */
  answers?: { questionId: number; value: number; booleanAnswer?: boolean }[];
  icf?: number;
  hasCrisis?: boolean;
  dimensionScores?: Record<string, number>;
}

// ── Modelo de pregunta ────────────────────────────────────────────────────────

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