// ── Tipos semánticos de generación ───────────────────────────────────────
export type GenerationType =
  'founding'    |  // -2 y menor: Ancestros Fundadores
  'builder'     |  // -1: Generación Constructora
  'responsible' |  //  0: Generación Responsable (ancla)
  'current'     |  // +1: Generación Actual
  'future'      |  // +2: Generación Futura
  'projected';     // +3: Proyección

export type MemberStatus = 'alive' | 'deceased' | 'unknown' | 'future';

// ── Etiquetas y colores por nivel ─────────────────────────────────────────
export const GEN_META: Record<number, { label: string; type: GenerationType; color: string; desc: string }> = {
  [-3]: { label: 'Tatarabuelos',          type: 'founding',     color: '#4b2600', desc: 'Raíces más profundas' },
  [-2]: { label: 'Ancestros Fundadores',  type: 'founding',     color: '#78350f', desc: 'Historia, migraciones, oficios' },
  [-1]: { label: 'Generación Constructora', type: 'builder',    color: '#b45309', desc: 'Valores, costumbres, aprendizajes' },
  [0]:  { label: 'Generación Responsable', type: 'responsible', color: '#d97706', desc: 'Diagnósticos, gobierno, constitución' },
  [1]:  { label: 'Generación Actual',      type: 'current',     color: '#fbbf24', desc: 'Participación, misiones, evolución' },
  [2]:  { label: 'Generación Futura',      type: 'future',      color: '#fde68a', desc: 'Legado recibido, aprendizajes' },
  [3]:  { label: 'Proyección',             type: 'projected',   color: '#fef9c3', desc: 'Bisnietos, visión futura' },
};

export function getGenMeta(gen: number) {
  return GEN_META[gen] ?? { label: `Generación ${gen}`, type: 'responsible' as GenerationType, color: '#9ca3af', desc: '' };
}

// ── DTOs ──────────────────────────────────────────────────────────────────
export interface LineageEvent {
  id: number;
  eventYear: string | null;
  title: string;
  description: string | null;
  eventType: string;
  isApproximate: boolean;
  sortOrder: number;
}

export interface LineageMember {
  id: number;
  firstName: string | null;
  lastName: string | null;
  fullName: string;
  avatarInitials: string | null;
  avatarColor: string | null;
  generation: number;
  generationType: GenerationType;
  isAnchor: boolean;
  status: MemberStatus;
  birthYear: number | null;
  birthYearApproximate: boolean | null;
  birthDate: string | null;
  deathYear: number | null;
  deathDate: string | null;
  origin: string | null;
  roleLabel: string | null;
  confidenceLevel: number;
  dataSource: string | null;
  calculatedAge: string | null;
  // Campos de evolución
  story: string | null;
  valores: string | null;
  aprendizajes: string | null;
  erroresSuperados: string | null;
  tradiciones: string | null;
  misionesCumplidas: string | null;
  legadoPersonal: string | null;
  photoUrl: string | null;
  positionX: number | null;
  positionY: number | null;
  familyMemberId: number | null;
  events: LineageEvent[];
}

export interface LineageRelationship {
  id: number;
  fromMemberId: number;
  toMemberId: number;
  relationshipType: string;
  isCouple: boolean;
}

export interface GenerationInfo {
  id: number;
  generationLevel: number;
  generationType: string;
  title: string | null;
  summary: string | null;
  context: string | null;
  keyChallenge: string | null;
  keyAchievement: string | null;
  periodStart: string | null;
  periodEnd: string | null;
}

export interface Lineage {
  id: number;
  familyId: number;
  lineageCode: string;
  title: string;
  description: string | null;
  anchorGeneration: number;
  maxPastGen: number;
  maxFutureGen: number;
  visionStatement: string | null;
  foundingYear: string | null;
  members: LineageMember[];
  relationships: LineageRelationship[];
  generationInfos: GenerationInfo[];
}

// ── Requests ──────────────────────────────────────────────────────────────
export interface LineageMemberRequest {
  firstName?: string;
  lastName?: string;
  avatarInitials?: string;
  avatarColor?: string;
  generation: number;
  generationType?: string;
  isAnchor?: boolean;
  status: string;
  birthYear?: number;
  birthYearApproximate?: boolean;
  birthDate?: string;
  deathYear?: number;
  deathDate?: string;
  origin?: string;
  roleLabel?: string;
  confidenceLevel?: number;
  dataSource?: string;
  story?: string;
  valores?: string;
  aprendizajes?: string;
  erroresSuperados?: string;
  tradiciones?: string;
  misionesCumplidas?: string;
  legadoPersonal?: string;
  photoUrl?: string;
  positionX?: number;
  positionY?: number;
  familyMemberId?: number;
  events?: LineageEventRequest[];
}

export interface LineageEventRequest {
  eventYear?: string;
  title: string;
  description?: string;
  eventType?: string;
  isApproximate?: boolean;
  sortOrder?: number;
}

export interface GenerationInfoRequest {
  generationLevel: number;
  generationType?: string;
  title?: string;
  summary?: string;
  context?: string;
  keyChallenge?: string;
  keyAchievement?: string;
  periodStart?: string;
  periodEnd?: string;
}
