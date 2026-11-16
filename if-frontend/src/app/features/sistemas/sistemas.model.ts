export interface ScreenLink {
  label: string;
  route: string;
}

export interface SystemView {
  number: number;
  name: string;
  functionLabel: string;
  functionText: string;
  ruleLabel: string;
  ruleText: string;
  outcomeLabel: string | null;
  outcomeText: string | null;
  whereInIf: string;
  eventTriggered: boolean;
  reviewPath: string | null;
  duration: string | null;
  links: ScreenLink[];
}

export interface LevelView {
  level: number;
  title: string;
  summary: string;
  systems: SystemView[];
}

export interface MonthStep {
  week: number;
  focus: string;
}

export interface CatalogResponse {
  title: string;
  subtitle: string;
  problem: string;
  failures: string;
  counterargument: string;
  levels: LevelView[];
  firstMonth: MonthStep[];
  checkQuestions: string[];
  statusNote: string;
}

export interface ProjectView {
  name: string;
  decision: string;
  decisionLabel: string;
}

export interface ObjectiveView {
  objective: string;
  achievementCriterion: string;
  reviewOn: string;
}

export interface AdjustmentView {
  description: string;
  kind: string;
  kindLabel: string;
}

export interface ReviewResponse {
  id: number;
  type: string;
  typeLabel: string;
  reviewDate: string;
  periodLabel: string | null;
  authorName: string | null;
  sprintId: number | null;
  sprintDailyId: number | null;
  retrospectiveId: number | null;
  rhythmNote: string;
  captureEmptied: boolean | null;
  agendaChecked: boolean | null;
  actions: string[];
  calendarReviewed: boolean | null;
  tasksPruned: boolean | null;
  commitmentsTransferred: boolean | null;
  priorities: string[];
  projects: ProjectView[];
  stageChangeUnnoticed: boolean | null;
  objectives: ObjectiveView[];
  changeDescription: string | null;
  whatNoLongerFits: string | null;
  adjustments: AdjustmentView[];
  createdAt: string;
}

export interface WeekBucketView {
  start: string;
  end: string;
  weeklyReviewDone: boolean;
  captureEmptied: boolean;
}

export interface SavedCheckView {
  id: number;
  windowStart: string;
  windowEnd: string;
  weeksWithWeeklyReview: number;
  captureEmptiedEveryWeek: boolean;
  unregisteredCommitment: boolean;
  authorName: string | null;
  createdAt: string;
}

export interface FourWeekCheckResponse {
  windowStart: string;
  windowEnd: string;
  weeksWithWeeklyReview: number;
  weeksInWindow: number;
  weeklyTarget: number;
  weeklyReviewsMet: boolean;
  captureEmptiedEveryWeek: boolean;
  weeks: WeekBucketView[];
  unregisteredCommitment: boolean | null;
  savedChecks: SavedCheckView[];
}

export interface ReviewKind {
  slug: string;
  code: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ANNUAL' | 'CHANGE';
  title: string;
  duration: string;
  hint: string;
}

export const REVIEW_KINDS: ReviewKind[] = [
  {
    slug: 'diaria',
    code: 'DAILY',
    title: 'Revisión diaria',
    duration: '5–10 min',
    hint: 'Al empezar el día. Deja la captura vacía y elige de 1 a 3 acciones.'
  },
  {
    slug: 'semanal',
    code: 'WEEKLY',
    title: 'Revisión semanal',
    duration: '30–60 min',
    hint: 'Un día fijo. Si un día hay que recortar, se recorta la diaria, nunca la semanal.'
  },
  {
    slug: 'mensual',
    code: 'MONTHLY',
    title: 'Revisión mensual',
    duration: '30 min',
    hint: 'El primer día hábil. Una decisión por proyecto.'
  },
  {
    slug: 'anual',
    code: 'ANNUAL',
    title: 'Revisión anual',
    duration: '2–3 h',
    hint: 'Una fecha fija. Pocos objetivos, cada uno con criterio de logro y fecha de revisión.'
  },
  {
    slug: 'cambio',
    code: 'CHANGE',
    title: 'Ajuste por cambio',
    duration: 'Cuando algo cambia',
    hint: 'Una mudanza, un trabajo nuevo, una enfermedad o un hijo que empieza el colegio. No espera al calendario.'
  }
];

export const PROJECT_DECISIONS = [
  { code: 'CONTINUAR', label: 'Continuar' },
  { code: 'CORREGIR', label: 'Corregir' },
  { code: 'PAUSAR', label: 'Pausar' },
  { code: 'CERRAR', label: 'Cerrar' }
];

export const ADJUSTMENT_KINDS = [
  { code: 'DEJAR', label: 'Dejar' },
  { code: 'REDUCIR', label: 'Reducir' },
  { code: 'POSPONER', label: 'Posponer' }
];

const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

export function formatIsoDate(iso: string | null | undefined): string {
  if (!iso) return '';
  const [year, month, day] = iso.split('-');
  const monthIndex = Number(month) - 1;
  if (!year || monthIndex < 0 || monthIndex > 11 || !day) return iso;
  return `${Number(day)} ${MONTHS[monthIndex]} ${year}`;
}

export function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function apiErrorMessage(err: unknown, fallback: string): string {
  const message = (err as { error?: { message?: string } })?.error?.message;
  return message || fallback;
}
