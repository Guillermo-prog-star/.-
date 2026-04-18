import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FamilyStateService {
  // Signal para guardar el estado del ID de familia seleccionado mundialmente en la App
  private readonly familyIdSignal = signal<number>(this.getInitialState());

  // Exponemos el signal de solo lectura
  public readonly currentFamilyId = this.familyIdSignal.asReadonly();

  constructor() {}

  /**
   * Obtiene la familia actual seleccionada.
   */
  getFamilyId(): number {
    return this.familyIdSignal();
  }

  /**
   * Actualiza el contexto familiar compartido en toda la aplicación
   */
  setFamilyId(id: number, familyName: string): void {
    this.familyIdSignal.set(id);
    localStorage.setItem('selectedFamilyId', id.toString());
    localStorage.setItem('selectedFamilyName', familyName);
  }

  /**
   * Reinicia la familia seleccionada (útil para cerrar sesión)
   */
  clearFamily(): void {
    this.familyIdSignal.set(0);
    localStorage.removeItem('selectedFamilyId');
    localStorage.removeItem('selectedFamilyName');
  }

  private getInitialState(): number {
    return Number(localStorage.getItem('selectedFamilyId') ?? 0);
  }
}
