import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FamilyStateService {
  // Signal para guardar el estado del ID de familia seleccionado
  private readonly familyIdSignal = signal<number>(this.getInitialState());

  // Signal para guardar el estado del nombre de la familia seleccionada
  private readonly familyNameSignal = signal<string>(this.getInitialFamilyName());

  // Exponemos los signals de solo lectura para componentes reactivos
  public readonly currentFamilyId = this.familyIdSignal.asReadonly();
  public readonly currentFamilyName = this.familyNameSignal.asReadonly();

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
    const familyName = family.name || 'Familia';
    this.familyNameSignal.set(familyName);

    localStorage.setItem('selectedFamilyId', family.id.toString());
    localStorage.setItem('selectedFamilyName', familyName);
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
    this.familyNameSignal.set('');
    localStorage.removeItem('selectedFamilyId');
    localStorage.removeItem('selectedFamilyName');
    localStorage.removeItem('selectedFamilyCode');
  }

  private getInitialState(): number {
    const savedId = localStorage.getItem('selectedFamilyId');
    if (savedId) return Number(savedId);

    // [SDD Fallback] Intentar recuperar desde el objeto de usuario si la llave específica no existe
    const authUserJson = localStorage.getItem('auth_user');
    if (authUserJson) {
      try {
        const user = JSON.parse(authUserJson);
        return user.familyId || 0;
      } catch {
        return 0;
      }
    }
    
    return 0;
  }

  private getInitialFamilyName(): string {
    const savedName = localStorage.getItem('selectedFamilyName');
    if (savedName) return savedName;

    // [SDD Fallback] Intentar recuperar desde el objeto de usuario si la llave específica no existe
    const authUserJson = localStorage.getItem('auth_user');
    if (authUserJson) {
      try {
        const user = JSON.parse(authUserJson);
        return user.familyName || '';
      } catch {
        return '';
      }
    }
    
    return '';
  }
}