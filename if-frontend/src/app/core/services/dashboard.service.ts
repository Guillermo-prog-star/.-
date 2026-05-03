import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardService {
    private http = inject(HttpClient);
    private readonly apiUrl = `${environment.apiUrl}${environment.apiBaseUrl}/dashboard`;

    private _metrics = signal<any>(null);
    readonly metrics = this._metrics.asReadonly();

    /**
     * SDD: Sincronización de métricas solicitada por ScenariosGrid
     */
    updateMetrics(payload: any): void {
        console.log('SDD: Sincronizando Nodo Armenia...', payload);
        this._metrics.set({ ...this._metrics(), ...payload });

        this.http.patch(`${this.apiUrl}/metrics`, payload).subscribe({
            next: (res: any) => console.log('Métricas persistidas.'),
            error: (err: any) => console.error('Error de persistencia:', err)
        });
    }
}