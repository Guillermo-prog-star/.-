export interface Question {
  id: number;
  dimension: string;
  area: string;      // Nuevo: coincide con Backend Entity
  vertice?: string;  // Deprecated: antes se llamaba así
  text: string;      // Nuevo: coincide con Backend Entity
  questionText?: string; // Deprecated: antes se llamaba así
  active: boolean;
}