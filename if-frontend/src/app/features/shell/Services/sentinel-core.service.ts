/**
 * SERVICIO NÚCLEO - PROTOCOLO SENTINEL
 * Responsabilidad: Gestión de alertas críticas y monitoreo de integridad del nodo.
 */
import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class SentinelCoreService {
    // [SDD] Uso de Signals para un rendimiento óptimo en la detección de cambios
    private criticalAlertActive = signal<boolean>(false);
    private lastAlertTimestamp = signal<Date | null>(null);

    constructor() {
        // Simulación de monitoreo de integridad (Inferencia: Conexión con backend de auditoría)
        this.checkSystemIntegrity();
    }

    /**
     * Determina si existe una amenaza activa que requiera el banner global.
     */
    hasCriticalAlert(): boolean {
        return this.criticalAlertActive();
    }

    /**
     * Protocolo de mitigación: Marca las alertas como gestionadas.
     */
    markAllAsViewed(): void {
        console.warn('⚠️ [SENTINEL] Iniciando protocolo de mitigación de alertas...');
        this.criticalAlertActive.set(false);
    }

    /**
     * Protocolo de emergencia: Activa alertas críticas.
     */
    triggerEmergency(): void {
        this.criticalAlertActive.set(true);
        this.lastAlertTimestamp.set(new Date());
    }

    private checkSystemIntegrity(): void {
        // Lógica futura: Inyectar HttpClient para polling de salud del nodo
        // Por ahora, mantenemos el estado limpio para permitir el build.
    }
}