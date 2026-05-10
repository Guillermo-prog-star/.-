import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from './api.service';

/**
 * TelemetryService: Servicio cliente de telemetría y auditoría inteligente.
 * Sincroniza las interacciones de UI, CLI y evaluaciones de forma asíncrona con el backend.
 */
@Injectable({ providedIn: 'root' })
export class TelemetryService {
  private http = inject(HttpClient);
  private api = inject(ApiService);

  /**
   * Envía un evento de telemetría al backend.
   * @param eventType El tipo de evento (e.g., 'CLI_COMMAND_EXECUTED', 'PLAN_TASK_TOGGLED')
   * @param metadata Objeto de metadatos personalizados que se convertirá a JSON.
   */
  logEvent(eventType: string, metadata: any = {}): void {
    const payload = {
      eventType,
      metadataJson: JSON.stringify(metadata)
    };
    
    this.http.post<any>(`${this.api.base}/telemetry`, payload).subscribe({
      next: () => console.log(`[Telemetry] Evento '${eventType}' registrado con éxito.`),
      error: (err) => console.error(`[Telemetry] Error registrando evento '${eventType}':`, err)
    });
  }
}
