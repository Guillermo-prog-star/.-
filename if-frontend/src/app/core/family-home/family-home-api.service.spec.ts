import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FamilyHomeApiService } from './family-home-api.service';
import { ApiService } from '../services/api.service';
import { FAMILY_HOME_CONTRACT_VERSION, FamilyActionResult, FamilyHomeView } from './family-home-view.model';

const API_BASE = '/api';
const FAMILY_ID = '11111111-2222-3333-4444-555555555555';

describe('FamilyHomeApiService', () => {
  let service: FamilyHomeApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ApiService, useValue: { base: API_BASE } as ApiService },
      ],
    });
    service = TestBed.inject(FamilyHomeApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getHome() hace GET a /v1/families/{id}/home con el header de versión de contrato', () => {
    let result: FamilyHomeView | undefined;
    service.getHome(FAMILY_ID).subscribe((view) => (result = view));

    const req = httpMock.expectOne(`${API_BASE}/v1/families/${FAMILY_ID}/home`);
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('X-Family-Home-Contract-Version')).toBe(FAMILY_HOME_CONTRACT_VERSION);
    expect(req.request.headers.has('X-Correlation-Id')).toBeFalse();

    const stub = { viewType: 'ONBOARDING' } as unknown as FamilyHomeView;
    req.flush(stub);

    expect(result).toEqual(stub);
  });

  it('getHome() agrega X-Correlation-Id cuando se provee', () => {
    service.getHome(FAMILY_ID, 'corr-123').subscribe();

    const req = httpMock.expectOne(`${API_BASE}/v1/families/${FAMILY_ID}/home`);
    expect(req.request.headers.get('X-Correlation-Id')).toBe('corr-123');
    req.flush({});
  });

  it('acceptFirstSprint() hace POST con el header Idempotency-Key', () => {
    let result: FamilyActionResult | undefined;
    service
      .acceptFirstSprint(FAMILY_ID, { objective: 'Mejorar la cena' }, 'idem-1')
      .subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${API_BASE}/v1/families/${FAMILY_ID}/actions/accept-first-sprint`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Idempotency-Key')).toBe('idem-1');
    expect(req.request.body).toEqual({ objective: 'Mejorar la cena' });

    const stub: FamilyActionResult = {
      action: 'accept-first-sprint',
      status: 'COMPLETED',
      executedAt: '2026-01-01T00:00:00Z',
      replayed: false,
    };
    req.flush(stub);

    expect(result).toEqual(stub);
  });
});
