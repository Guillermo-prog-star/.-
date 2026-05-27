export interface Microaccion {
  id: string;
  descripcion: string;
  icono: string; // Ej: 'settings', 'assignment', 'analytics'
}

export interface Mision {
  id: string;
  titulo: string;
  estado: 'Pendiente' | 'En_Progreso' | 'Completada';
  descripcionGeneral: string;
  microacciones: Microaccion[];
  backendTaskId?: number;
  isAi?: boolean;
  queBusca?: string;
  pasoAPaso?: string[];
  esIniciativaFamiliar?: boolean;
  iniciada?: boolean;
}


export interface PlanTransformacion {
  id: string;
  pilar: 'EMOCIONES' | 'COMUNICACION' | 'HABITOS' | 'TIEMPOS';
  titulo: string;
  visionFamiliar: string;
  progresoPilar: number;
  misionesLogradas: number;
  misionesTotales: number;
  misiones: Mision[];
}
