import { countMissions, missionProgressPercent } from './missionStatusDomain';

describe('missionStatusDomain', () => {
  const missions = [
    { status: 'COMPLETED' }, { status: 'PENDING' },
    { status: 'CHANGED_BY_AGREEMENT' }, { status: 'POSTPONED' },
  ];

  it('cuenta cada estado por separado', () => {
    expect(countMissions(missions)).toEqual(
      { completed: 1, pending: 1, changed: 1, postponed: 1, committed: 2 });
  });

  it('el porcentaje excluye cambiadas por acuerdo y pospuestas', () => {
    expect(missionProgressPercent(missions)).toBe(50);
  });

  it('sin misiones comprometidas devuelve 0', () => {
    expect(missionProgressPercent([{ status: 'POSTPONED' }])).toBe(0);
    expect(missionProgressPercent(null)).toBe(0);
  });
});
