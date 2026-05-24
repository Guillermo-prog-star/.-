import { TestBed } from '@angular/core/testing';
import { FamilyStateService } from './family-state.service';

describe('FamilyStateService', () => {
  let service: FamilyStateService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [FamilyStateService]
    });

    service = TestBed.inject(FamilyStateService);
  });

  afterEach(() => localStorage.clear());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('estado inicial', () => {
    it('currentFamilyId debe ser 0 cuando localStorage está vacío', () => {
      expect(service.currentFamilyId()).toBe(0);
    });

    it('currentFamilyName debe ser "" cuando localStorage está vacío', () => {
      expect(service.currentFamilyName()).toBe('');
    });

    it('getSelectedFamilyId() debe devolver 0 en estado inicial', () => {
      expect(service.getSelectedFamilyId()).toBe(0);
    });

    it('debe leer familyId del localStorage si existe al inicializarse', () => {
      localStorage.setItem('selectedFamilyId', '99');
      localStorage.setItem('selectedFamilyName', 'Familia Persistida');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [FamilyStateService] });
      const freshService = TestBed.inject(FamilyStateService);

      expect(freshService.currentFamilyId()).toBe(99);
      expect(freshService.currentFamilyName()).toBe('Familia Persistida');
    });

    it('debe recuperar familyId de auth_user como fallback si selectedFamilyId no existe', () => {
      localStorage.setItem('auth_user', JSON.stringify({ familyId: 55, familyName: 'Familia Auth' }));

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: [FamilyStateService] });
      const freshService = TestBed.inject(FamilyStateService);

      expect(freshService.currentFamilyId()).toBe(55);
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  setFamily()
  // ═══════════════════════════════════════════════════════════════════════

  describe('setFamily()', () => {
    it('debe actualizar el signal currentFamilyId', () => {
      service.setFamily({ id: 42, name: 'Familia Lopez' });
      expect(service.currentFamilyId()).toBe(42);
    });

    it('debe actualizar el signal currentFamilyName', () => {
      service.setFamily({ id: 42, name: 'Familia Lopez' });
      expect(service.currentFamilyName()).toBe('Familia Lopez');
    });

    it('debe persistir selectedFamilyId en localStorage', () => {
      service.setFamily({ id: 42, name: 'Familia Lopez' });
      expect(localStorage.getItem('selectedFamilyId')).toBe('42');
    });

    it('debe persistir selectedFamilyName en localStorage', () => {
      service.setFamily({ id: 42, name: 'Familia Lopez' });
      expect(localStorage.getItem('selectedFamilyName')).toBe('Familia Lopez');
    });

    it('debe usar "Familia" como nombre por defecto si no se provee', () => {
      service.setFamily({ id: 42 });
      expect(service.currentFamilyName()).toBe('Familia');
    });

    it('debe ser un no-op si se llama con null', () => {
      service.setFamily(null);
      expect(service.currentFamilyId()).toBe(0);
    });

    it('debe ser un no-op si se llama con objeto sin id', () => {
      service.setFamily({ name: 'Sin ID' });
      expect(service.currentFamilyId()).toBe(0);
    });

    it('debe sobrescribir una familia previa', () => {
      service.setFamily({ id: 1, name: 'Primera' });
      service.setFamily({ id: 2, name: 'Segunda' });

      expect(service.currentFamilyId()).toBe(2);
      expect(service.currentFamilyName()).toBe('Segunda');
      expect(localStorage.getItem('selectedFamilyId')).toBe('2');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  clearFamily()
  // ═══════════════════════════════════════════════════════════════════════

  describe('clearFamily()', () => {
    beforeEach(() => {
      service.setFamily({ id: 42, name: 'Familia Lopez' });
    });

    it('debe resetear currentFamilyId a 0', () => {
      service.clearFamily();
      expect(service.currentFamilyId()).toBe(0);
    });

    it('debe resetear currentFamilyName a ""', () => {
      service.clearFamily();
      expect(service.currentFamilyName()).toBe('');
    });

    it('debe eliminar selectedFamilyId de localStorage', () => {
      service.clearFamily();
      expect(localStorage.getItem('selectedFamilyId')).toBeNull();
    });

    it('debe eliminar selectedFamilyName de localStorage', () => {
      service.clearFamily();
      expect(localStorage.getItem('selectedFamilyName')).toBeNull();
    });

    it('debe eliminar selectedFamilyCode de localStorage', () => {
      localStorage.setItem('selectedFamilyCode', 'LOPEZ-001');
      service.clearFamily();
      expect(localStorage.getItem('selectedFamilyCode')).toBeNull();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  setFamilyId() — alias de setFamily
  // ═══════════════════════════════════════════════════════════════════════

  describe('setFamilyId()', () => {
    it('debe actualizar id y nombre correctamente', () => {
      service.setFamilyId(77, 'Familia Rápida');
      expect(service.currentFamilyId()).toBe(77);
      expect(service.currentFamilyName()).toBe('Familia Rápida');
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Signals son readonly (no modificables desde fuera)
  // ═══════════════════════════════════════════════════════════════════════

  describe('signals readonly', () => {
    it('currentFamilyId debe ser accesible como función signal', () => {
      service.setFamily({ id: 10, name: 'Test' });
      expect(typeof service.currentFamilyId).toBe('function');
      expect(service.currentFamilyId()).toBe(10);
    });

    it('currentFamilyName debe ser accesible como función signal', () => {
      service.setFamily({ id: 10, name: 'Test' });
      expect(typeof service.currentFamilyName).toBe('function');
      expect(service.currentFamilyName()).toBe('Test');
    });
  });
});
