import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { FamilyCreatePageComponent } from './family-create-page.component';
import { ApiService } from '../../core/services/api.service';
import { FamilyStateService } from '../../core/services/family-state.service';

// ─── Helpers ───────────────────────────────────────────────────────────────

const API_BASE = '/api';

function buildComponent() {
  const familyStateSpy = jasmine.createSpyObj<FamilyStateService>(
    'FamilyStateService', ['setFamily', 'clearFamily']
  );
  const apiServiceStub = { base: API_BASE } as ApiService;

  TestBed.configureTestingModule({
    imports: [FamilyCreatePageComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ApiService, useValue: apiServiceStub },
      { provide: FamilyStateService, useValue: familyStateSpy }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture = TestBed.createComponent(FamilyCreatePageComponent);
  const component = fixture.componentInstance;
  const httpMock = TestBed.inject(HttpTestingController);
  const router = TestBed.inject(Router);
  spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

  return { fixture, component, httpMock, router, familyStateSpy };
}

// ───────────────────────────────────────────────────────────────────────────

describe('FamilyCreatePageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  Estado inicial
  // ═══════════════════════════════════════════════════════════════════════

  describe('Estado inicial', () => {
    it('debe iniciar con campos vacíos, loading=false y error=""', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      // Responder al GET de ngOnInit
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      expect(component.name).toBe('');
      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  Validaciones en línea
  // ═══════════════════════════════════════════════════════════════════════

  describe('isWaValid() e isPinValid()', () => {
    it('isWaValid() debe ser false con menos de 10 caracteres', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.whatsapp = '312345678'; // 9 chars
      expect(component.isWaValid()).toBeFalse();
      httpMock.verify();
    }));

    it('isWaValid() debe ser true con 10 o más caracteres', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.whatsapp = '3123456789'; // 10 chars
      expect(component.isWaValid()).toBeTrue();
      httpMock.verify();
    }));

    it('isPinValid() debe ser true solo con exactamente 4 caracteres', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.pin = '1234';
      expect(component.isPinValid()).toBeTrue();
      component.pin = '123';
      expect(component.isPinValid()).toBeFalse();
      component.pin = '12345';
      expect(component.isPinValid()).toBeFalse();
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit() — recuperación automática si ya tiene familia
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit() — recuperación automática', () => {
    it('si el usuario ya tiene familia, debe establecer el estado y navegar a /members', fakeAsync(() => {
      const { fixture, component, httpMock, router, familyStateSpy } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/families/mine`).flush({
        data: { id: 10, name: 'Familia López', familyCode: 'IF-2026-XYZ' }
      });
      tick();

      expect(familyStateSpy.setFamily).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 10, name: 'Familia López' })
      );
      expect(router.navigate).toHaveBeenCalledWith(['/members']);
      httpMock.verify();
    }));

    it('si no tiene familia (error HTTP), debe permanecer en el formulario sin error', fakeAsync(() => {
      const { fixture, component, httpMock, router } = buildComponent();
      fixture.detectChanges();

      httpMock.expectOne(`${API_BASE}/families/mine`).flush(
        'Not found', { status: 404, statusText: 'Not Found' }
      );
      tick();

      expect(router.navigate).not.toHaveBeenCalled();
      expect(component.error).toBe('');
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — validación de campos
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — validación', () => {
    function setupReady(component: FamilyCreatePageComponent) {
      component.name = 'Familia Gómez';
      component.whatsapp = '3123456789';
      component.pin = '1234';
    }

    it('debe mostrar error si falta el nombre', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      // name vacío
      component.whatsapp = '3123456789';
      component.pin = '1234';
      component.submit();

      expect(component.error).toContain('incompletos');
      httpMock.verify();
    }));

    it('debe mostrar error si whatsapp es inválido (< 10 chars)', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia Gómez';
      component.whatsapp = '312345'; // < 10
      component.pin = '1234';
      component.submit();

      expect(component.error).toContain('incompletos');
      httpMock.verify();
    }));

    it('debe mostrar error si el PIN no tiene exactamente 4 dígitos', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia Gómez';
      component.whatsapp = '3123456789';
      component.pin = '12'; // ≠ 4
      component.submit();

      expect(component.error).toContain('incompletos');
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — flujo exitoso
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — creación exitosa', () => {
    it('debe llamar setFamily y navegar a /members al recibir 200', fakeAsync(() => {
      const { fixture, component, httpMock, router, familyStateSpy } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia Gómez';
      component.whatsapp = '3123456789';
      component.pin = '1234';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush({
        data: { id: 20, name: 'Familia Gómez', familyCode: 'IF-2026-ABC' }
      });
      tick();

      expect(familyStateSpy.setFamily).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 20, name: 'Familia Gómez' })
      );
      expect(router.navigateByUrl).toHaveBeenCalledWith('/members');
      httpMock.verify();
    }));

    it('debe funcionar también si el servidor devuelve la familia directamente (sin .data wrapper)', fakeAsync(() => {
      const { fixture, component, httpMock, familyStateSpy } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia Test';
      component.whatsapp = '3123456789';
      component.pin = '9999';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush({
        id: 55, name: 'Familia Test', familyCode: 'IF-2026-DEF'
      });
      tick();

      expect(familyStateSpy.setFamily).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 55 })
      );
      httpMock.verify();
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  submit() — manejo de errores
  // ═══════════════════════════════════════════════════════════════════════

  describe('submit() — errores', () => {
    it('409 con familyId → recupera identidad y navega a /members', fakeAsync(() => {
      const { fixture, component, httpMock, router, familyStateSpy } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia X';
      component.whatsapp = '3123456789';
      component.pin = '0000';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush(
        { message: 'ya posee una familia', familyId: 99, familyName: 'Familia Existente' },
        { status: 409, statusText: 'Conflict' }
      );
      tick();

      expect(familyStateSpy.setFamily).toHaveBeenCalledWith(
        jasmine.objectContaining({ id: 99, name: 'Familia Existente' })
      );
      expect(router.navigate).toHaveBeenCalledWith(['/members']);
      httpMock.verify();
    }));

    it('409 sin familyId → navega a /families', fakeAsync(() => {
      const { fixture, component, httpMock, router } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia X';
      component.whatsapp = '3123456789';
      component.pin = '0000';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush(
        { message: 'ya posee una familia' },
        { status: 409, statusText: 'Conflict' }
      );
      tick();

      expect(router.navigate).toHaveBeenCalledWith(['/families']);
      httpMock.verify();
    }));

    it('otro error del servidor → muestra mensaje de error y loading=false', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia X';
      component.whatsapp = '3123456789';
      component.pin = '0000';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush(
        { message: 'Error interno del servidor' },
        { status: 500, statusText: 'Internal Server Error' }
      );
      tick();

      expect(component.error).toBe('Error interno del servidor');
      expect(component.loading).toBeFalse();
      httpMock.verify();
    }));

    it('error 502 del servidor → muestra mensaje de mantenimiento e infraestructura', fakeAsync(() => {
      const { fixture, component, httpMock } = buildComponent();
      fixture.detectChanges();
      httpMock.expectOne(`${API_BASE}/families/mine`).flush({ data: null });
      tick();

      component.name = 'Familia X';
      component.whatsapp = '3123456789';
      component.pin = '0000';
      component.submit();

      httpMock.expectOne(`${API_BASE}/families`).flush(
        'Server down',
        { status: 502, statusText: 'Bad Gateway' }
      );
      tick();

      expect(component.error).toContain('mantenimiento');
      expect(component.error).toContain('502');
      expect(component.loading).toBeFalse();
      httpMock.verify();
    }));
  });
});
