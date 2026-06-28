export type RiskMacrodomain =
  | 'RELACIONES_PAREJA'
  | 'CRIANZA_ADOLESCENCIA'
  | 'SALUD_MENTAL'
  | 'ADICCIONES'
  | 'EDUCACION_DESARROLLO'
  | 'ECONOMIA_FAMILIAR'
  | 'GOBERNANZA'
  | 'ADULTO_MAYOR'
  | 'LEGADO';

export type TrajectoryStatus = 'DETECTED' | 'IN_PROGRESS' | 'RESOLVED' | 'RELAPSED' | 'CLOSED';
export type SeverityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TrajectoryBankItem {
  id: number;
  code: string;
  name: string;
  macrodomain: RiskMacrodomain;
  description: string;
  earlySignals: string;
  potentialEvolution: string;
  severityDefault: SeverityLevel;
}

export interface TrajectoryBankResponse {
  byMacrodomain: Record<string, TrajectoryBankItem[]>;
  totalTrajectories: number;
}

export interface FamilyTrajectoryDto {
  id: number;
  trajectory: TrajectoryBankItem;
  status: TrajectoryStatus;
  detectedAt: string;
  resolvedAt?: string;
  notes?: string;
  assignedBy?: string;
}

export interface TrajectoryTimelineDto {
  id: number;
  eventDate: string;
  ageAtEvent?: number;
  eventDescription: string;
  riskLevel: SeverityLevel;
  actionTaken?: string;
  result?: string;
  recordedAt: string;
}

export interface TrajectoryImpactDto {
  id: number;
  indicatorName: string;
  indicatorKey: string;
  baselineValue?: number;
  currentValue?: number;
  unit?: string;
  higherIsBetter: boolean;
  improvementPct?: number;
}

export interface AssignTrajectoryRequest {
  code: string;
  notes?: string;
}

export interface TimelineEventRequest {
  eventDate: string;
  ageAtEvent?: number;
  eventDescription: string;
  riskLevel: SeverityLevel;
  actionTaken?: string;
  result?: string;
}

export interface IndicatorRequest {
  indicatorName: string;
  indicatorKey: string;
  baselineValue?: number;
  currentValue?: number;
  unit?: string;
  higherIsBetter?: boolean;
  notes?: string;
}

export const MACRODOMAIN_LABELS: Record<string, string> = {
  RELACIONES_PAREJA: 'Relaciones de Pareja',
  CRIANZA_ADOLESCENCIA: 'Crianza y Adolescencia',
  SALUD_MENTAL: 'Salud Mental',
  ADICCIONES: 'Adicciones',
  EDUCACION_DESARROLLO: 'Educación y Desarrollo',
  ECONOMIA_FAMILIAR: 'Economía Familiar',
  GOBERNANZA: 'Gobernanza Familiar',
  ADULTO_MAYOR: 'Adulto Mayor',
  LEGADO: 'Legado Familiar',
};

export const SEVERITY_CONFIG: Record<SeverityLevel, { label: string; color: string; bg: string }> = {
  LOW: { label: 'Bajo', color: 'text-green-400', bg: 'bg-green-900/30' },
  MEDIUM: { label: 'Medio', color: 'text-yellow-400', bg: 'bg-yellow-900/30' },
  HIGH: { label: 'Alto', color: 'text-orange-400', bg: 'bg-orange-900/30' },
  CRITICAL: { label: 'Crítico', color: 'text-red-400', bg: 'bg-red-900/30' },
};

export const STATUS_CONFIG: Record<TrajectoryStatus, { label: string; color: string }> = {
  DETECTED: { label: 'Detectado', color: 'text-blue-400' },
  IN_PROGRESS: { label: 'En progreso', color: 'text-yellow-400' },
  RESOLVED: { label: 'Resuelto', color: 'text-green-400' },
  RELAPSED: { label: 'Recaída', color: 'text-orange-400' },
  CLOSED: { label: 'Cerrado', color: 'text-gray-400' },
};
