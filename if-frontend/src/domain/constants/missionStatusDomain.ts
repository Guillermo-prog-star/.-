/**
 * Estado de una misión de sprint (espejo de SprintMissionStatus en backend).
 *
 * "Cambiada por acuerdo" y "pospuesta" NO son incumplimiento: la familia
 * decidió ajustar el plan. Se muestran aparte y no entran al porcentaje.
 */
export type MissionStatus = 'PENDING' | 'COMPLETED' | 'CHANGED_BY_AGREEMENT' | 'POSTPONED';

export const MISSION_STATUS_TEXT: Record<MissionStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completada',
  CHANGED_BY_AGREEMENT: 'Cambiada por acuerdo',
  POSTPONED: 'Pospuesta',
};

export interface MissionCounts {
  completed: number;
  pending: number;
  changed: number;
  postponed: number;
  /** Completadas + pendientes: lo que sigue comprometido. */
  committed: number;
}

export function countMissions(missions: { status: string }[] | null | undefined): MissionCounts {
  const c: MissionCounts = { completed: 0, pending: 0, changed: 0, postponed: 0, committed: 0 };
  for (const m of missions ?? []) {
    if (m.status === 'COMPLETED') c.completed++;
    else if (m.status === 'CHANGED_BY_AGREEMENT') c.changed++;
    else if (m.status === 'POSTPONED') c.postponed++;
    else c.pending++;
  }
  c.committed = c.completed + c.pending;
  return c;
}

/** Porcentaje de cumplimiento sobre lo comprometido (excluye cambiadas y pospuestas). */
export function missionProgressPercent(missions: { status: string }[] | null | undefined): number {
  const c = countMissions(missions);
  return c.committed === 0 ? 0 : Math.round((c.completed / c.committed) * 100);
}
