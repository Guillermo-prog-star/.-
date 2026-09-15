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
  /** Miembro responsable de esta misión (PlanTask.responsible.fullName) — ausente = misión familiar compartida. */
  responsibleName?: string;
  /** Categoría de rol usada para generar la misión (familia/padre/madre/hijo/hija) — PlanTask.memberType. */
  memberType?: string;
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
