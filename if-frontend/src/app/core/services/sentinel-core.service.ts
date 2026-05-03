import { Injectable, signal, computed, inject, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval, switchMap, catchError, of, lastValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Injectable({ providedIn: 'root' })
export class SentinelCoreService {
  // 1. Estado Privado (Source of Truth) mediante Signals
  private _stats = signal<any>(null);
  private _alerts = signal<any[]>([]);
  private _sentiment = signal<any>(null);
  private _loading = signal<boolean>(false);

  // 2. Estado Público (Read-Only) para garantizar inmutabilidad desde componentes
  readonly stats = computed(() => this._stats());
  readonly alerts = computed(() => this._alerts());
  readonly sentiment = computed(() => this._sentiment());
  readonly loading = computed(() => this._loading());

  // 3. Lógica derivada: Detección de alertas críticas no leídas
  readonly hasCriticalAlert = computed(() =>
    this._alerts().some(a => a.severity === 'CRITICAL' && !a.viewed)
  );

  constructor(private http: HttpClient) {
    // Iniciar Vigilancia Automática al instanciar el nodo
    this.startWatchdog();
    this.refreshAll(); // Carga inicial de datos
    
    // SDD-NOTIFICATION: Audio/Visual trigger for critical events.
    effect(() => {
      if (this.hasCriticalAlert()) {
        this.playAlertSound();
      }
    });
  }


  private playAlertSound() {
    const audio = new Audio();
    // Sonido de alerta técnica/digital premium
    audio.src = 'https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3';
    audio.volume = 0.4;
    audio.play().catch(err => console.log('Autoplay prevented or audio error', err));
  }

  /**
   * SDD: Protocolo Watchdog
   * Mantiene el Security Feed actualizado cada 15s en segundo plano.
   */
  private startWatchdog() {
    interval(15000)
      .pipe(
        takeUntilDestroyed(),
        switchMap(() => this.http.get<any>('/api/admin/analytics/alerts').pipe(
          catchError(() => of({ data: [] }))
        ))
      )
      .subscribe(res => {
        if (res?.data) {
          this._alerts.set(res.data);
        }
      });
  }

  /**
   * Carga masiva y sincronización manual del estado global.
   * Optimizado mediante ejecución paralela de promesas.
   */
  async refreshAll() {
    this._loading.set(true);
    try {
      // Uso de lastValueFrom para cumplimiento de estandares RxJS modernos en promesas
      const statsReq = lastValueFrom(this.http.get<any>('/api/admin/analytics/alpha-stats'));
      const alertsReq = lastValueFrom(this.http.get<any>('/api/admin/analytics/alerts'));
      const sentimentReq = lastValueFrom(this.http.get<any>('/api/admin/analytics/sentiment'));

      const [s, a, sen] = await Promise.all([statsReq, alertsReq, sentimentReq]);

      this._stats.set(s?.data || null);
      this._alerts.set(a?.data || []);
      this._sentiment.set(sen?.data || null);
    } catch (error) {
      console.error('CRÍTICO - Sentinel Core Refresh Failure:', error);
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Protocolo de Limpieza: Marca alertas críticas como gestionadas localmente.
   */
  async markAllAsViewed() {
    const updatedAlerts = this._alerts().map(a => ({ ...a, viewed: true }));
    this._alerts.set(updatedAlerts);
  }

  /**
   * SDD: Exportación de Reportes
   * Descarga el binario PDF generado por el motor de reportes de Integrity Family.
   */
  downloadExecutivePdf() {
    this.http.get('/api/v1/reports/export/pdf', { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `SENTINEL_Reporte_${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (err) => console.error('Error al descargar el informe PDF:', err)
      });
  }

  /**
   * TEST ONLY: Simulates a critical crisis event to verify audio/visual triggers.
   */
  simulateCrisis() {
    const mockAlert = {
      id: Date.now(),
      title: '🚨 CRISIS SIMULADA: Nodo Armenia',
      message: 'Intrusión emocional detectada. Se requiere intervención proactiva inmediata.',
      severity: 'CRITICAL',
      category: 'SENTINEL',
      viewed: false,
      createdAt: new Date()
    };
    
    // Inyectar la alerta al inicio del array para que sea detectada por hasCriticalAlert
    this._alerts.update(current => [mockAlert, ...current]);
    console.log('🧪 Crisis simulada inyectada en el Signal');
  }
}