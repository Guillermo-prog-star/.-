// ── Modelos del Sistema de Medición del Fortalecimiento Familiar (SMFF) ──────

export interface IndicatorResult {
  id:          string;   // IND-01 … IND-20
  name:        string;
  group:       SmffGroup;
  cls:         'RESULTADO' | 'PROCESO' | 'ESTADO';
  value:       number;   // 0–100 normalizado
  rawValue:    number;
  rawUnit:     string;
  isEstimated: boolean;
  dataPoints:  number;
}

export interface IndicatorsSnapshot {
  familyId:        number;
  calculatedAt:    string;
  smffScore:       number;
  indicators:      IndicatorResult[];
  totalReal:       number;
  dataCompletePct: number;
}

export type SmffGroup = 'cohesion' | 'confianza' | 'transf' | 'resil' | 'long';

// ── Metadatos estáticos de cada grupo ────────────────────────────────────────

export interface GroupMeta {
  key:    SmffGroup;
  label:  string;
  color:  string;
  weight: number;   // peso sugerido en el SMFF (informativo)
  ids:    string[];
}

export const SMFF_GROUPS: GroupMeta[] = [
  {
    key: 'cohesion', label: 'Cohesión y Vínculo', color: '#6b8cff', weight: 0.25,
    ids: ['IND-01','IND-02','IND-03','IND-04']
  },
  {
    key: 'confianza', label: 'Confianza', color: '#7ecfb3', weight: 0.15,
    ids: ['IND-05','IND-06','IND-07']
  },
  {
    key: 'transf', label: 'Transformación', color: '#f5a623', weight: 0.25,
    ids: ['IND-08','IND-09','IND-10','IND-11','IND-12']
  },
  {
    key: 'resil', label: 'Resiliencia', color: '#e06b8b', weight: 0.20,
    ids: ['IND-13','IND-14','IND-15','IND-16']
  },
  {
    key: 'long', label: 'Trayectoria', color: '#a08ff0', weight: 0.15,
    ids: ['IND-17','IND-18','IND-19','IND-20']
  },
];

export const GROUP_MAP = new Map<SmffGroup, GroupMeta>(
  SMFF_GROUPS.map(g => [g.key, g])
);

// ── Score SMFF → nivel cualitativo ───────────────────────────────────────────

export interface SmffLevel {
  label:  string;
  color:  string;
  from:   number;
  to:     number;
}

export const SMFF_LEVELS: SmffLevel[] = [
  { label: 'Crítico',       color: '#e05555', from: 0,  to: 29  },
  { label: 'Vulnerable',    color: '#e09055', from: 30, to: 49  },
  { label: 'En desarrollo', color: '#e0c655', from: 50, to: 64  },
  { label: 'Fortalecida',   color: '#7ecfb3', from: 65, to: 79  },
  { label: 'Floreciente',   color: '#6b8cff', from: 80, to: 100 },
];

export function smffLevel(score: number): SmffLevel {
  return SMFF_LEVELS.find(l => score >= l.from && score <= l.to)
      ?? SMFF_LEVELS[0];
}
