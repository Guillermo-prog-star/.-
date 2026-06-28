// Modelos del Índice de Capital Familiar (ICaF)

export interface IcafDomainScore {
  key: string;
  label: string;
  score: number;        // 0-100
  weight: number;       // 0.0-1.0
  isEstimated: boolean;
  source: 'ICF' | 'CUESTIONARIO' | 'EVENTOS' | 'ESTIMADO';
}

export interface IcafDashboardResponse {
  familyId: number;

  // Índice global
  icaf: number;
  madurezNivel: number;   // 1-5
  madurezLabel: string;   // "Supervivencia" | "Reactividad" | "Organización" | "Propósito" | "Legado"
  trend: 'IMPROVING' | 'STABLE' | 'DECLINING';

  // Trayectoria longitudinal
  icaf6mAgo:  number | null;
  icaf12mAgo: number | null;
  icaf36mAgo: number | null;

  // 11 dominios
  domains: IcafDomainScore[];

  // Eventos críticos
  activeEvents:       number;
  resolvedEvents:     number;
  resolutionRate:     number;   // 0-100 %
  avgDaysToResolution: number;
  totalRelapses:      number;

  // Metadatos
  lastCalculatedAt: string | null;
  hasRealData: boolean;
}

export interface IcafDomainScoreResult {
  domain: string;
  score: number;
  savedCount: number;
  totalAnswered: number;
}

export interface IcafQuestion {
  id: number;
  questionKey: string;
  text: string;
  icafDomain: string;
  direction: 'POSITIVE' | 'NEGATIVE';
  sortOrder: number;
}

export interface ObservatorySnapshot {
  snapshotMonth: string;       // "2026-06-01"
  familiesCount: number;

  // Distribución ICaF
  icafAvg:    number;
  icafP25:    number;
  icafMedian: number;
  icafP75:    number;

  // Distribución madurez (% por nivel)
  nivel1Pct: number;
  nivel2Pct: number;
  nivel3Pct: number;
  nivel4Pct: number;
  nivel5Pct: number;

  // Eventos críticos poblacionales
  eventsDetected:    number;
  eventsResolved:    number;
  avgDaysResolution: number;
  resolutionRatePct: number;

  // Tendencias longitudinales
  familiesImproving: number;
  familiesStable:    number;
  familiesDeclining: number;

  // Promedios de dominio
  avgDomCohesion:      number;
  avgDomConfianza:     number;
  avgDomResiliencia:   number;
  avgDomComunicacion:  number;
  avgDomAutonomia:     number;
  avgDomBienestar:     number;
  avgDomProposito:     number;
  avgDomIntegracion:   number;
  avgDomEmprendimiento: number;
  avgDomLegado:        number;
}

// Mapa de colores por nivel de madurez
export const MADUREZ_CONFIG: Record<number, { color: string; bg: string; label: string }> = {
  1: { color: '#ef4444', bg: 'rgba(239,68,68,0.12)',   label: 'Supervivencia' },
  2: { color: '#f97316', bg: 'rgba(249,115,22,0.12)',  label: 'Reactividad'   },
  3: { color: '#eab308', bg: 'rgba(234,179,8,0.12)',   label: 'Organización'  },
  4: { color: '#3b82f6', bg: 'rgba(59,130,246,0.12)',  label: 'Propósito'     },
  5: { color: '#10b981', bg: 'rgba(16,185,129,0.12)',  label: 'Legado'        },
};

// Mapa de colores por dominio
export const DOMAIN_COLORS: Record<string, string> = {
  cohesion:       '#6366f1',
  confianza:      '#0ea5e9',
  resiliencia:    '#10b981',
  comunicacion:   '#a855f7',
  autonomia:      '#f59e0b',
  bienestar:      '#ec4899',
  proposito:      '#14b8a6',
  integracion:    '#8b5cf6',
  emprendimiento: '#f97316',
  legado:         '#d4af37',
  madurez:        '#64748b',
};
