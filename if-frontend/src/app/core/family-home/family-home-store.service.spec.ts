import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { FamilyHomeStoreService } from './family-home-store.service';
import { FamilyHomeApiService } from './family-home-api.service';
import { FamilyActionResult, FamilyHomeView } from './family-home-view.model';

const FAMILY_ID = '11111111-2222-3333-4444-555555555555';

const ONBOARDING_VIEW = { viewType: 'ONBOARDING' } as unknown as FamilyHomeView;

describe('FamilyHomeStoreService', () => {
  let store: FamilyHomeStoreService;
  let apiSpy: jasmine.SpyObj<FamilyHomeApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj<FamilyHomeApiService>('FamilyHomeApiService', [
      'getHome',
      'acceptFirstSprint',
    ]);

    TestBed.configureTestingModule({
      providers: [{ provide: FamilyHomeApiService, useValue: apiSpy }],
    });
    store = TestBed.inject(FamilyHomeStoreService);
  });

  describe('load()', () => {
    it('éxito → view poblado, loading=false, error=null', () => {
      apiSpy.getHome.and.returnValue(of(ONBOARDING_VIEW));

      store.load(FAMILY_ID);

      expect(store.view()).toEqual(ONBOARDING_VIEW);
      expect(store.viewType()).toBe('ONBOARDING');
      expect(store.loading()).toBeFalse();
      expect(store.error()).toBeNull();
    });

    it('error → error mapeado desde ProblemDetail, loading=false', () => {
      const httpError = new HttpErrorResponse({
        error: { code: 'FAMILY_HOME_NOT_FOUND', title: 'Hogar no disponible', detail: 'No accesible' },
        status: 404,
      });
      apiSpy.getHome.and.returnValue(throwError(() => httpError));

      store.load(FAMILY_ID);

      expect(store.loading()).toBeFalse();
      expect(store.error()).toEqual({
        code: 'FAMILY_HOME_NOT_FOUND',
        title: 'Hogar no disponible',
        detail: 'No accesible',
      });
      expect(store.view()).toBeNull();
    });
  });

  describe('acceptFirstSprint()', () => {
    it('éxito → recarga la proyección tras el comando', () => {
      const result: FamilyActionResult = {
        action: 'accept-first-sprint',
        status: 'COMPLETED',
        executedAt: '2026-01-01T00:00:00Z',
        replayed: false,
      };
      apiSpy.acceptFirstSprint.and.returnValue(of(result));
      apiSpy.getHome.and.returnValue(of(ONBOARDING_VIEW));

      store.acceptFirstSprint(FAMILY_ID);

      expect(apiSpy.acceptFirstSprint).toHaveBeenCalledWith(FAMILY_ID, {}, jasmine.any(String));
      expect(apiSpy.getHome).toHaveBeenCalledWith(FAMILY_ID);
      expect(store.actionPending()).toBeFalse();
    });

    it('llamadas repetidas mientras hay una acción en curso se ignoran (evita doble envío)', () => {
      apiSpy.acceptFirstSprint.and.returnValue(of({
        action: 'accept-first-sprint', status: 'COMPLETED', executedAt: '2026-01-01T00:00:00Z', replayed: false,
      }));
      apiSpy.getHome.and.returnValue(of(ONBOARDING_VIEW));

      // Simula acción en curso marcando el signal privado vía la primera llamada real,
      // y verificando que una segunda invocación síncrona no duplique la petición.
      (store as any)['_actionPending'].set(true);
      store.acceptFirstSprint(FAMILY_ID);

      expect(apiSpy.acceptFirstSprint).not.toHaveBeenCalled();
    });

    it('error → error mapeado, actionPending=false, no recarga', () => {
      const httpError = new HttpErrorResponse({
        error: { code: 'FAMILY_HOME_STATE_CONFLICT', title: 'Conflicto', detail: 'Estado inválido' },
        status: 409,
      });
      apiSpy.acceptFirstSprint.and.returnValue(throwError(() => httpError));

      store.acceptFirstSprint(FAMILY_ID);

      expect(store.actionPending()).toBeFalse();
      expect(store.error()?.code).toBe('FAMILY_HOME_STATE_CONFLICT');
      expect(apiSpy.getHome).not.toHaveBeenCalled();
    });
  });
});
