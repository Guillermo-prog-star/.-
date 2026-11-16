import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, of, tap } from 'rxjs';
import { FamilyHomeApiService } from './family-home-api.service';
import { AcceptFirstSprintRequest, FamilyHomeView, ViewType } from './family-home-view.model';

export interface FamilyHomeErrorState {
  code: string;
  title: string;
  detail: string;
}

/**
 * Store de estado del Hogar Digital (signals). Envuelve FamilyHomeApiService
 * para que los componentes nunca manejen el JSON crudo ni las peticiones HTTP
 * directamente — solo leen signals y llaman comandos.
 */
@Injectable({ providedIn: 'root' })
export class FamilyHomeStoreService {
  private readonly api = inject(FamilyHomeApiService);

  private readonly _view = signal<FamilyHomeView | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<FamilyHomeErrorState | null>(null);
  private readonly _actionPending = signal(false);

  readonly view = this._view.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly actionPending = this._actionPending.asReadonly();

  readonly viewType = computed<ViewType | null>(() => this._view()?.viewType ?? null);

  /** Carga (o recarga) la proyección del Hogar Digital para la familia dada. */
  load(familyId: string): void {
    this._loading.set(true);
    this._error.set(null);

    this.api
      .getHome(familyId)
      .pipe(
        tap((view) => {
          this._view.set(view);
          this._loading.set(false);
        }),
        catchError((err: HttpErrorResponse) => {
          this._error.set(this.toErrorState(err));
          this._loading.set(false);
          return of(null);
        })
      )
      .subscribe();
  }

  /**
   * Ejecuta el comando `accept-first-sprint` (Family Action Engine, Hito 5).
   * Al completar con éxito, recarga la proyección (invalidación del lado del
   * cliente — el backend no expone caché que invalidar hoy).
   */
  acceptFirstSprint(familyId: string, request: AcceptFirstSprintRequest = {}): void {
    if (this._actionPending()) {
      return;
    }
    this._actionPending.set(true);
    this._error.set(null);
    const idempotencyKey = crypto.randomUUID();

    this.api
      .acceptFirstSprint(familyId, request, idempotencyKey)
      .pipe(
        tap(() => {
          this._actionPending.set(false);
          this.load(familyId);
        }),
        catchError((err: HttpErrorResponse) => {
          this._error.set(this.toErrorState(err));
          this._actionPending.set(false);
          return of(null);
        })
      )
      .subscribe();
  }

  private toErrorState(err: HttpErrorResponse): FamilyHomeErrorState {
    const body = err.error;
    return {
      code: body?.code ?? 'UNKNOWN_ERROR',
      title: body?.title ?? 'Error inesperado',
      detail: body?.detail ?? 'Ocurrió un error al comunicarse con el servidor.',
    };
  }
}
