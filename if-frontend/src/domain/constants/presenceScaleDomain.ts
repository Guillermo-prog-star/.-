// src/domain/constants/presenceScaleDomain.ts

/**
 * Interfaz que define las tres dimensiones del eje X.
 * Facilita la inyección de estilos (colorCode) y renderizado semántico en la UI.
 */
export interface PresenceScaleDefinition {
  description: string;
  state: string;
  colorCode: string;
}

/**
 * Diccionario de datos estricto que consolida la matriz 4x5.
 */
export const PRESENCE_SCALE: Readonly<Record<number, PresenceScaleDefinition>> = {
  1: {
    description: 'casi nunca estoy presente',
    state: 'INCONSCIENTE',
    colorCode: 'GRIS'
  },
  2: {
    description: 'Me distraigo constantemente',
    state: 'REACTIVO',
    colorCode: 'ROJO SUAVE'
  },
  3: {
    description: 'intento equilibrarlo',
    state: 'CONSCIENTE',
    colorCode: 'AMARILLO'
  },
  4: {
    description: 'Estoy priorizando mas',
    state: 'INTENCIONAL',
    colorCode: 'AZUL'
  },
  5: {
    description: 'Comparto tiempo con verdadera presencia',
    state: 'PLENO',
    colorCode: 'VERDE'
  },
} as const;

export type PresenceLevel = keyof typeof PRESENCE_SCALE;
