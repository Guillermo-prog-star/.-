// src/domain/constants/rutaConcienciaDomain.ts
//
// Ruta de Conciencia Familiar — escala metodológica oficial de Integrity Family.
// Traduce el modelo científico de 5 niveles de conciencia (Inconsciente -> Pleno)
// a un único lenguaje experiencial, sin tecnicismos, aplicable a cualquier
// dimensión o escenario evaluado (la pregunta ya aporta el contexto específico).
// Ver CLAUDE.md -> "Ruta de Conciencia Familiar" para la justificación metodológica.

export type ConsciousnessState = 'INCONSCIENTE' | 'REACTIVO' | 'CONSCIENTE' | 'INTENCIONAL' | 'PLENO';

export interface RutaConcienciaLevel {
  score: 1 | 2 | 3 | 4 | 5;
  state: ConsciousnessState;
  label: string;
  icon: string;
  text: string;
  colorClass: string;
  hexColor: string;
}

export const RUTA_CONCIENCIA_SCALE: Readonly<Record<1 | 2 | 3 | 4 | 5, RutaConcienciaLevel>> = {
  1: {
    score: 1,
    state: 'INCONSCIENTE',
    label: 'Inconsciente',
    icon: '🌑',
    text: 'Aún no logro reconocer esta realidad en nuestra familia.',
    colorClass: 'Gris',
    hexColor: '#64748b',
  },
  2: {
    score: 2,
    state: 'REACTIVO',
    label: 'Reactivo',
    icon: '🌒',
    text: 'Empiezo a darme cuenta, pero normalmente cuando la situación ya pasó.',
    colorClass: 'Rojo Suave',
    hexColor: '#f87171',
  },
  3: {
    score: 3,
    state: 'CONSCIENTE',
    label: 'Consciente',
    icon: '🌓',
    text: 'Reconozco esta realidad cuando ocurre.',
    colorClass: 'Amarillo',
    hexColor: '#f59e0b',
  },
  4: {
    score: 4,
    state: 'INTENCIONAL',
    label: 'Intencional',
    icon: '🌔',
    text: 'Cuando la reconozco, procuro actuar para fortalecerla o transformarla.',
    colorClass: 'Azul',
    hexColor: '#3b82f6',
  },
  5: {
    score: 5,
    state: 'PLENO',
    label: 'Pleno',
    icon: '🌕',
    text: 'Esta forma de vivir ya hace parte natural de nuestra familia.',
    colorClass: 'Verde',
    hexColor: '#10b981',
  },
} as const;

export type ConsciousnessLevel = keyof typeof RUTA_CONCIENCIA_SCALE;
