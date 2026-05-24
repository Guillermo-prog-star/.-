import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import {
  provideHttpClient,
  withInterceptorsFromDi
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { FamilyListPageComponent } from './family-list-page.component';
import { FamilyStateService } from '../../core/services/family-state.service';
import { Family } from '../../core/models/models';
import { ApiService } from '../../core/services/api.service';

// ─── Stubs ───────────────────────────────────────────────────────────────────

const FAMILIES_STUB: Family[] = [
  { id: 1, name: 'Familia López', familyCode: 'IF-001', currentMilestone: 'W1',
    members: [], icf: 75, createdAt: '2026-01-01T00:00:00' } as any,
  { id: 2, name: 'Familia García', familyCode: 'IF-002', currentMilestone: 'M3',
    members: [], icf: 80, createdAt: '2026-01-01T00:00:00' } as any
];

// ─────────────────────────────────────────────────────────────────────────────

describe('FamilyListPageComponent', () => {
  let httpMock: HttpTestingController;
  let router: Router;
  let fsSpy: jasmine.SpyObj<FamilyStateService>;

  beforeEach(() => {
    fsSpy = jasmine.createSpyObj<FamilyStateService>(
      'FamilyStateService',
      ['setFamilyId'],
      { currentFamilyId: signal(1) }
    );

    TestBed.configureTestingModule({
      imports: [FamilyListPageComponent],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: FamilyStateService, useValue: fsSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });

    httpMock = TestBed.inject(HttpTestingController);
    router  = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  ngOnInit()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('ngOnInit()', () => {
    it('GET /api/families éxito → families poblado y loading=false', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      fixture.detectChanges();

      httpMock.expectOne('/api/families').flush({ data: FAMILIES_STUB });
      tick();

      expect(fixture.componentInstance.families.length).toBe(2);
      expect(fixture.componentInstance.loading).toBeFalse();
    }));

    it('GET /api/families error → loading=false', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      fixture.detectChanges();

      httpMock.expectOne('/api/families').flush('Server Error',
        { status: 500, statusText: 'Internal Server Error' }
      );
      tick();

      expect(fixture.componentInstance.loading).toBeFalse();
      expect(fixture.componentInstance.families).toEqual([]);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  select()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('select()', () => {
    it('llama setFamilyId con id y nombre de la familia', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      spyOn(router, 'navigate');
      fixture.detectChanges();
      httpMock.expectOne('/api/families').flush({ data: [] });
      tick();

      fixture.componentInstance.select(FAMILIES_STUB[0]);

      expect(fsSpy.setFamilyId).toHaveBeenCalledWith(1, 'Familia López');
    }));

    it('navega a /members tras seleccionar familia', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      const navigateSpy = spyOn(router, 'navigate');
      fixture.detectChanges();
      httpMock.expectOne('/api/families').flush({ data: [] });
      tick();

      fixture.componentInstance.select(FAMILIES_STUB[0]);

      expect(navigateSpy).toHaveBeenCalledWith(['/members']);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //  isSelected()
  // ═══════════════════════════════════════════════════════════════════════════

  describe('isSelected()', () => {
    it('id === currentFamilyId → true', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      fixture.detectChanges();
      httpMock.expectOne('/api/families').flush({ data: [] });
      tick();

      expect(fixture.componentInstance.isSelected(FAMILIES_STUB[0])).toBeTrue();
    }));

    it('id !== currentFamilyId → false', fakeAsync(() => {
      const fixture = TestBed.createComponent(FamilyListPageComponent);
      fixture.detectChanges();
      httpMock.expectOne('/api/families').flush({ data: [] });
      tick();

      expect(fixture.componentInstance.isSelected(FAMILIES_STUB[1])).toBeFalse();
    }));
  });
});
