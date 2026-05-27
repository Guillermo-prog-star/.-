import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FamilyStateService {
  // Signal para guardar el estado del ID de familia seleccionado
  private readonly familyIdSignal   = signal<number>(this.getInitialState());
  // Signal para guardar el estado del nombre de la familia seleccionada
  private readonly familyNameSignal = signal<string>(this.getInitialFamilyName());
  // Signal para el código de familia (ej. IF-CO-QUI-2026-0001)
  private readonly familyCodeSignal = signal<string>(localStorage.getItem('selectedFamilyCode') ?? '');
  // Signal para el ID del miembro autenticado en la familia activa
  private readonly memberIdSignal   = signal<number | null>(this.getInitialMemberId());
  // Signal para el hito actual de la familia (W1, M1, M3, …)
  private readonly milestoneSignal  = signal<string>(localStorage.getItem('currentMilestone') ?? '');

  // Exponemos los signals de solo lectura para componentes reactivos
  public readonly currentFamilyId   = this.familyIdSignal.asReadonly();
  public readonly currentFamilyName = this.familyNameSignal.asReadonly();
  public readonly currentFamilyCode = this.familyCodeSignal.asReadonly();
  /** ID del miembro autenticado dentro de la familia seleccionada. Null si no se ha resuelto aún. */
  public readonly currentMemberId   = this.memberIdSignal.asReadonly();
  /** Código del hito familiar activo (W1, M1, M3 …). Cadena vacía si no se ha seleccionado familia. */
  public readonly currentMilestone  = this.milestoneSignal.asReadonly();

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
    const familyCode = family.familyCode || '';
    this.familyNameSignal.set(familyName);
    this.familyCodeSignal.set(familyCode);

    localStorage.setItem('selectedFamilyId', family.id.toString());
    localStorage.setItem('selectedFamilyName', familyName);
    localStorage.setItem('selectedFamilyCode', familyCode);
  }

  /**
   * Actualiza el contexto familiar compartido (Compatibilidad)
   */
  setFamilyId(id: number, familyName: string): void {
    this.setFamily({ id, name: familyName });
  }

  /**
   * Persiste el ID del miembro autenticado en la familia activa.
   * Llamado desde DashboardPage y PortalFamiliar tras resolver el miembro por email.
   */
  setMemberId(id: number): void {
    this.memberIdSignal.set(id);
    localStorage.setItem('currentMemberId', id.toString());
  }

  /**
   * Actualiza el hito activo de la familia.
   * Llamado desde FamilyListPage al seleccionar familia.
   */
  setMilestone(milestone: string): void {
    this.milestoneSignal.set(milestone);
    localStorage.setItem('currentMilestone', milestone);
  }

  /**
   * Reinicia la familia seleccionada (útil para cerrar sesión).
   * Limpia también el miembro y el hito asociados a la familia.
   */
  clearFamily(): void {
    this.familyIdSignal.set(0);
    this.familyNameSignal.set('');
    this.familyCodeSignal.set('');
    this.memberIdSignal.set(null);
    this.milestoneSignal.set('');
    localStorage.removeItem('selectedFamilyId');
    localStorage.removeItem('selectedFamilyName');
    localStorage.removeItem('selectedFamilyCode');
    localStorage.removeItem('currentMemberId');
    localStorage.removeItem('currentMilestone');
  }

  private getInitialMemberId(): number | null {
    const saved = localStorage.getItem('currentMemberId');
    return saved ? Number(saved) : null;
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