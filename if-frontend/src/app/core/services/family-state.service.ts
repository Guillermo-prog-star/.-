import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FamilyStateService {
  // Signal para guardar el estado del ID de familia seleccionado
  private readonly familyIdSignal = signal<number>(this.getInitialState());

  // Exponemos el signal de solo lectura para componentes reactivos
  public readonly currentFamilyId = this.familyIdSignal.asReadonly();

  constructor() { }

  /**
   * CORRECCIÓN TÉCNICA (Sincronización SDD): 
   * Se renombra/añade para satisfacer la llamada de los componentes.
   * Esto resuelve los errores TS2339 en Checklist y Crisis components.
   */
  getSelectedFamilyId(): number {
    return this.familyIdSignal();
  }

  /**
   * Alias opcional para mantener compatibilidad interna
   */
  getFamilyId(): number {
    return this.getSelectedFamilyId();
  }

  /**
   * SDD Spec: Única Fuente de Verdad para Identidad Familiar.
   * Centraliza la persistencia reactiva y local en una operación atómica.
   */
  setFamily(family: any): void {
    if (!family || !family.id) return;
    
    this.familyIdSignal.set(family.id);
    localStorage.setItem('selectedFamilyId', family.id.toString());
    localStorage.setItem('selectedFamilyName', family.name || 'Familia');
    localStorage.setItem('selectedFamilyCode', family.familyCode || '');
  }

  /**
   * Actualiza el contexto familiar compartido (Compatibilidad)
   */
  setFamilyId(id: number, familyName: string): void {
    this.setFamily({ id, name: familyName });
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
    const savedId = localStorage.getItem('selectedFamilyId');
    return savedId ? Number(savedId) : 0;
  }
}