import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { FamilyHomePageComponent } from './family-home-page.component';
import { FamilyStateService } from '../../core/services/family-state.service';
import { FamilyHomeApiService, FamilyHomeStoreService } from '../../core/family-home';
import {
  buildOnboardingView,
  buildReturnStageView,
  navigateCommand,
  submitActionCommand,
} from './family-home-test-fixtures';

describe('FamilyHomePageComponent', () => {
  let httpSpy: jasmine.SpyObj<HttpClient>;
  let apiSpy: jasmine.SpyObj<FamilyHomeApiService>;
  let navigateByUrlSpy: jasmine.Spy;

  beforeEach(() => {
    localStorage.clear();
    httpSpy = jasmine.createSpyObj<HttpClient>('HttpClient', ['get']);
    apiSpy = jasmine.createSpyObj<FamilyHomeApiService>('FamilyHomeApiService', ['getHome', 'acceptFirstSprint']);
  });

  afterEach(() => localStorage.clear());

  function build() {
    TestBed.configureTestingModule({
      imports: [FamilyHomePageComponent],
      providers: [
        provideRouter([]),
        { provide: HttpClient, useValue: httpSpy },
        { provide: FamilyHomeApiService, useValue: apiSpy },
      ],
    });

    const router = TestBed.inject(Router);
    navigateByUrlSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);

    const fixture = TestBed.createComponent(FamilyHomePageComponent);
    return {
      fixture,
      component: fixture.componentInstance,
      familyState: TestBed.inject(FamilyStateService),
      store: TestBed.inject(FamilyHomeStoreService),
    };
  }

  describe('resolución de homeId', () => {
    it('con homeId ya persistido, carga la proyección directamente sin llamar a /families/mine', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture } = build();
      fixture.detectChanges();

      expect(apiSpy.getHome).toHaveBeenCalledWith('home-uuid-1');
      expect(httpSpy.get).not.toHaveBeenCalled();
    });

    it('sin homeId persistido, lo resuelve contra /families/mine y luego carga la proyección', () => {
      httpSpy.get.and.returnValue(of({ data: { id: 42, name: 'Familia Lopez', homeId: 'home-uuid-2' } }));
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture, familyState } = build();
      fixture.detectChanges();

      expect(httpSpy.get).toHaveBeenCalled();
      expect(familyState.currentHomeId()).toBe('home-uuid-2');
      expect(apiSpy.getHome).toHaveBeenCalledWith('home-uuid-2');
    });

    it('si /families/mine no trae homeId, muestra el estado vacío en vez de fallar', () => {
      httpSpy.get.and.returnValue(of({ data: { id: 42, name: 'Familia Lopez' } }));

      const { fixture } = build();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('No pudimos identificar el Hogar Digital');
      expect(apiSpy.getHome).not.toHaveBeenCalled();
    });
  });

  describe('renderizado por viewType', () => {
    it('renderiza app-onboarding-home-view para ONBOARDING', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture } = build();
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('app-onboarding-home-view'))).toBeTruthy();
    });

    it('renderiza app-return-stage-home-view para RETURN_STAGE', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildReturnStageView()));

      const { fixture } = build();
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('app-return-stage-home-view'))).toBeTruthy();
    });
  });

  describe('onCommand()', () => {
    it('NAVIGATE navega con el router', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture, component } = build();
      fixture.detectChanges();
      component.onCommand(navigateCommand({ target: '/onboarding/members' }));

      expect(navigateByUrlSpy).toHaveBeenCalledWith('/onboarding/members');
    });

    it('SUBMIT_ACTION accept-first-sprint dispara store.acceptFirstSprint con el homeId activo', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildReturnStageView()));
      apiSpy.acceptFirstSprint.and.returnValue(of({
        action: 'accept-first-sprint', status: 'COMPLETED', executedAt: '2026-01-01T00:00:00Z', replayed: false,
      }));

      const { fixture, component } = build();
      fixture.detectChanges();
      component.onCommand(submitActionCommand());

      expect(apiSpy.acceptFirstSprint).toHaveBeenCalledWith('home-uuid-1', {}, jasmine.any(String));
    });

    it('un SUBMIT_ACTION sin backend real (ej. confirm-resume) muestra un aviso en vez de fallar silenciosamente', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture, component } = build();
      fixture.detectChanges();
      component.onCommand({
        id: 'cmd-confirm-resume', label: 'Retomar Actividades', type: 'SUBMIT_ACTION',
        target: 'confirm-resume', enabled: true, requiresConfirmation: false,
      });

      expect(component.unsupportedAction()).toContain('Retomar Actividades');
      expect(apiSpy.acceptFirstSprint).not.toHaveBeenCalled();
    });

    it('ignora comandos deshabilitados', () => {
      localStorage.setItem('selectedFamilyHomeId', 'home-uuid-1');
      apiSpy.getHome.and.returnValue(of(buildOnboardingView()));

      const { fixture, component } = build();
      fixture.detectChanges();
      component.onCommand(navigateCommand({ enabled: false }));

      expect(navigateByUrlSpy).not.toHaveBeenCalled();
    });
  });
});
