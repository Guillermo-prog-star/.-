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
  // Signal para el identificador público (UUID) de la familia — contrato IFRM-D Family Home
  private readonly homeIdSignal = signal<string>(localStorage.getItem('selectedFamilyHomeId') ?? '');
  // Signal para el ID del miembro autenticado en la familia activa
  private readonly memberIdSignal   = signal<number | null>(this.getInitialMemberId());
  // Signal para el hito actual de la familia (W1, M1, M3, …)
  private readonly milestoneSignal  = signal<string>(localStorage.getItem('currentMilestone') ?? '');
  // Signal para el modo observador del profesional
  private readonly isObservingSignal = signal<boolean>(localStorage.getItem('isObserving') === 'true');
  // Signal para la asignación/vínculo del profesional observador
  private readonly assignmentIdSignal = signal<number | null>(this.getInitialAssignmentId());

  // Exponemos los signals de solo lectura para componentes reactivos
  public readonly currentFamilyId   = this.familyIdSignal.asReadonly();
  public readonly currentFamilyName = this.familyNameSignal.asReadonly();
  public readonly currentFamilyCode = this.familyCodeSignal.asReadonly();
  public readonly currentHomeId = this.homeIdSignal.asReadonly();
  public readonly currentMemberId   = this.memberIdSignal.asReadonly();
  public readonly currentMilestone  = this.milestoneSignal.asReadonly();
  public readonly isObserving        = this.isObservingSignal.asReadonly();
  public readonly currentAssignmentId = this.assignmentIdSignal.asReadonly();

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
   * Identificador público (UUID) de la familia activa, usado por el contrato
   * IFRM-D Family Home. Vacío si la familia activa aún no trae `homeId`
   * (ver FamilyResponse.homeId en el backend).
   */
  getSelectedHomeId(): string {
    return this.homeIdSignal();
  }

  /**
   * SDD Spec: Única Fuente de Verdad para Identidad Familiar.
   * Centraliza la persistencia reactiva y local en una operación atómica.
   */
  setFamily(family: any): void {
    if (!family || !family.id) return;

    this.familyIdSignal.set(family.id);
    const familyName = family.name ?? 'Familia';
    const familyCode = family.familyCode || '';
    const homeId = family.homeId ?? '';
    this.familyNameSignal.set(familyName);
    this.familyCodeSignal.set(familyCode);
    this.homeIdSignal.set(homeId);

    localStorage.setItem('selectedFamilyId', family.id.toString());
    localStorage.setItem('selectedFamilyName', familyName);
    localStorage.setItem('selectedFamilyCode', familyCode);
    if (homeId) {
      localStorage.setItem('selectedFamilyHomeId', homeId);
    } else {
      localStorage.removeItem('selectedFamilyHomeId');
    }
    // currentMilestone viaja en FamilyResponse — se sincroniza aquí para que
    // esté disponible en cualquier flujo que llame setFamily() (login, registro,
    // Hogar Digital), no solo donde alguien llame setMilestone() explícitamente.
    if (family.currentMilestone) {
      this.setMilestone(family.currentMilestone);
    }
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

  setObserving(observing: boolean): void {
    this.isObservingSignal.set(observing);
    localStorage.setItem('isObserving', observing.toString());
  }

  setAssignmentId(id: number | null): void {
    this.assignmentIdSignal.set(id);
    if (id !== null) {
      localStorage.setItem('selectedAssignmentId', id.toString());
    } else {
      localStorage.removeItem('selectedAssignmentId');
    }
  }

  private getInitialAssignmentId(): number | null {
    const saved = localStorage.getItem('selectedAssignmentId');
    return saved ? Number(saved) : null;
  }

  /**
   * Reinicia la familia seleccionada (útil para cerrar sesión).
   * Limpia también el miembro y el hito asociados a la familia.
   */
  clearFamily(): void {
    this.familyIdSignal.set(0);
    this.familyNameSignal.set('');
    this.familyCodeSignal.set('');
    this.homeIdSignal.set('');
    this.memberIdSignal.set(null);
    this.milestoneSignal.set('');
    this.isObservingSignal.set(false);
    this.assignmentIdSignal.set(null);
    localStorage.removeItem('selectedFamilyId');
    localStorage.removeItem('selectedFamilyName');
    localStorage.removeItem('selectedFamilyCode');
    localStorage.removeItem('selectedFamilyHomeId');
    localStorage.removeItem('currentMemberId');
    localStorage.removeItem('currentMilestone');
    localStorage.removeItem('isObserving');
    localStorage.removeItem('selectedAssignmentId');
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