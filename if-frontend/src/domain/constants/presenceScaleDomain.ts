// src/domain/constants/presenceScaleDomain.ts

export type ConsciousnessState = 'INCONSCIENTE' | 'REACTIVO' | 'CONSCIENTE' | 'INTENCIONAL' | 'PLENO';
export type ConsciousnessColorCode = 'GRIS' | 'ROJO_SUAVE' | 'AMARILLO' | 'AZUL' | 'VERDE';

export interface PresenceScaleDefinition {
  score: 1 | 2 | 3 | 4 | 5;
  description: string;
  state: ConsciousnessState;
  colorCode: ConsciousnessColorCode;
}

export const PRESENCE_SCALE: Readonly<Record<1 | 2 | 3 | 4 | 5, PresenceScaleDefinition>> = {
  1: {
    score: 1,
    description: 'Casi nunca estoy presente',
    state: 'INCONSCIENTE',
    colorCode: 'GRIS',
  },
  2: {
    score: 2,
    description: 'Me distraigo constantemente',
    state: 'REACTIVO',
    colorCode: 'ROJO_SUAVE',
  },
  3: {
    score: 3,
    description: 'Intento equilibrarlo',
    state: 'CONSCIENTE',
    colorCode: 'AMARILLO',
  },
  4: {
    score: 4,
    description: 'Estoy priorizando más a mi familia',
    state: 'INTENCIONAL',
    colorCode: 'AZUL',
  },
  5: {
    score: 5,
    description: 'Comparto tiempo con verdadera presencia',
    state: 'PLENO',
    colorCode: 'VERDE',
  },
} as const;

export type PresenceLevel = keyof typeof PRESENCE_SCALE;
