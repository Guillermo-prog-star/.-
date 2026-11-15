/**
 * Estado del intento en el check-in diario del sprint (backend: AttemptStatus, V114).
 * Separa "no hubo oportunidad" de "no lo intenté" y de "lo intenté sin buen
 * resultado": un día sin oportunidad no es un fracaso. null = sin responder
 * (incluye "prefiero no indicarlo").
 */
export type AttemptStatus =
  | 'NO_OPPORTUNITY'
  | 'NOT_ATTEMPTED'
  | 'ATTEMPTED_UNFAVORABLE'
  | 'ATTEMPTED_FAVORABLE';

export const ATTEMPT_STATUS_OPTIONS: ReadonlyArray<{ value: AttemptStatus; text: string }> = [
  { value: 'NO_OPPORTUNITY',        text: 'No hubo oportunidad' },
  { value: 'NOT_ATTEMPTED',         text: 'Hubo oportunidad, pero no lo intenté' },
  { value: 'ATTEMPTED_UNFAVORABLE', text: 'Lo intenté y no salió como esperaba' },
  { value: 'ATTEMPTED_FAVORABLE',   text: 'Lo intenté y ayudó' },
];

export function attemptStatusText(value: AttemptStatus | null | undefined): string | null {
  return ATTEMPT_STATUS_OPTIONS.find(o => o.value === value)?.text ?? null;
}
