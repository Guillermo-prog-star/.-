import { Injectable, inject } from '@angular/core';
import { FamilyStateService } from './family-state.service';

/**
 * Mapa módulo↔hito de ADR-002 (docs/adr/ADR-002-hogar-digital-como-raiz.md),
 * aplicado como profundidad — no como ocultamiento: un enlace "bloqueado" sigue
 * siendo visible y clicable, solo se marca como "creciendo" hasta que la familia
 * alcance el hito requerido. Rutas que no aparecen aquí están disponibles siempre
 * (Familia/Miembros/Guardián/ADN desde W1, y los espacios "Aprender"/"Conversar"/
 * "Ayuda" del ADR, que incluyen la regla de seguridad — nunca se gatean).
 */
const MILESTONE_ORDER = ['W1', 'M1', 'M3', 'M6', 'M9', 'M12', 'M18', 'M24', 'M30', 'M36'];

const ROUTE_MIN_MILESTONE: Record<string, string> = {
  // Nuestro Camino
  '/evaluations/start': 'M1',
  '/plans': 'M1',
  '/transformation/route': 'M6',
  '/capital': 'M3',
  '/smff': 'M3',
  '/transformation/adaptive': 'M6',
  // Crecer
  '/sprint': 'M1',
  '/logbook': 'M1',
  '/transformation/weekly-plan': 'M1',
  '/checklist': 'M1',
  '/evidence/capture': 'M1',
  '/transformation/error-protocol': 'M1',
  '/family-pulse': 'M12',
  '/gratitude': 'M12',
  '/my-space': 'M12',
  // Recordar
  '/family-timeline': 'M18',
  '/family-tree': 'M18',
  '/documentary-maker': 'M18',
  '/family-movie': 'M18',
  '/legado': 'M24',
  '/lineage': 'M24',
  '/digital-twin': 'M24',
};

@Injectable({ providedIn: 'root' })
export class MilestoneDepthService {
  private readonly familyState = inject(FamilyStateService);

  /** true si la ruta ya alcanzó la profundidad de contenido real para el hito actual de la familia. */
  hasDepth(route: string): boolean {
    const required = ROUTE_MIN_MILESTONE[route];
    if (!required) return true; // no gateada — siempre con profundidad completa

    const current = this.familyState.currentMilestone();
    if (!current) return false; // sin hito conocido aún: tratar como no alcanzado, no como error

    const currentIdx = MILESTONE_ORDER.indexOf(this.normalize(current));
    const requiredIdx = MILESTONE_ORDER.indexOf(required);
    if (currentIdx === -1 || requiredIdx === -1) return true; // hito desconocido: no bloquear por un dato raro

    return currentIdx >= requiredIdx;
  }

  /** Hito en el que este espacio empieza a tener contenido real (para mostrar en el tooltip). */
  unlocksAt(route: string): string | null {
    return ROUTE_MIN_MILESTONE[route] ?? null;
  }

  private normalize(raw: string): string {
    if (!raw || raw === 'MES_00_DIAGNOSTICO_BASE' || raw === 'MES_00_DIAGNOSTICO' || raw === 'M00') return 'W1';
    return raw;
  }
}
