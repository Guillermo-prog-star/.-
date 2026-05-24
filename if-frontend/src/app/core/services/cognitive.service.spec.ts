import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { CognitiveService } from './cognitive.service';
import { CognitiveSnapshot, NarrativeResponse, GraphResponse, MemoryResponse, ReflectionResponse, IcfHistoryPoint } from '../models/cognitive.model';
import { ApiResponse } from '../models/api-response.model';

// ─── Helpers ───────────────────────────────────────────────────────────────

function wrap<T>(data: T): ApiResponse<T> {
  return { success: true, data, message: 'OK' };
}

// ─── Fixtures ──────────────────────────────────────────────────────────────

const SNAPSHOT_FIXTURE: CognitiveSnapshot = {
  familyId: 1,
  identityProfile: {
    evolutionStage: 'INITIAL',
    completedCycles: 1,
    communicationStyle: 'DIRECT',
    conflictStyle: 'AVOIDANT',
    emotionalExpression: 'MEDIUM',
    adaptabilityIndex: 0.6,
    identityNarrative: 'Una familia en construcción.'
  },
  currentChapter: null,
  graphSummary: null,
  generatedAt: '2026-05-01T00:00:00'
} as any;

const NARRATIVE_FIXTURE: NarrativeResponse = {
  familyId: 1,
  chapters: [],
  currentPhase: 'AWAKENING',
  totalChapters: 0,
  turningPoints: 0,
  storyArcSummary: 'Sin historia aún.'
} as any;

const REFLECTION_FIXTURE: ReflectionResponse = {
  familyId: 1,
  requiresUrgentAttention: false,
  effectivenessLevel: 'MODERATE',
  icfTrend: 0,
  avgAdherence: 0.75,
  abandonmentLevel: 'LOW',
  effectivenessSummary: 'Moderada.',
  abandonmentSignals: [],
  lessonLearned: null
} as any;

const FAMILY_ID = 7;
const BASE = '/api/cognitive';

// ───────────────────────────────────────────────────────────────────────────

describe('CognitiveService', () => {
  let service: CognitiveService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CognitiveService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CognitiveService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ═══════════════════════════════════════════════════════════════════════
  //  getSnapshot()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getSnapshot()', () => {
    it('debe hacer GET a /api/cognitive/{id}/snapshot', () => {
      service.getSnapshot(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/snapshot`);
      expect(req.request.method).toBe('GET');
      req.flush(wrap(SNAPSHOT_FIXTURE));
    });

    it('debe desempaquetar r.data del ApiResponse', (done) => {
      service.getSnapshot(FAMILY_ID).subscribe(result => {
        expect(result).toEqual(SNAPSHOT_FIXTURE);
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/snapshot`).flush(wrap(SNAPSHOT_FIXTURE));
    });

    it('debe devolver null cuando el servidor devuelve error HTTP', (done) => {
      service.getSnapshot(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/snapshot`).flush('Server Error', {
        status: 500, statusText: 'Internal Server Error'
      });
    });

    it('debe devolver null cuando el servidor devuelve 401', (done) => {
      service.getSnapshot(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/snapshot`).flush('Unauthorized', {
        status: 401, statusText: 'Unauthorized'
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getNarrative()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getNarrative()', () => {
    it('debe hacer GET a /api/cognitive/{id}/narrative', () => {
      service.getNarrative(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/narrative`);
      expect(req.request.method).toBe('GET');
      req.flush(wrap(NARRATIVE_FIXTURE));
    });

    it('debe desempaquetar r.data', (done) => {
      service.getNarrative(FAMILY_ID).subscribe(result => {
        expect(result).toEqual(NARRATIVE_FIXTURE);
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/narrative`).flush(wrap(NARRATIVE_FIXTURE));
    });

    it('debe devolver null en error', (done) => {
      service.getNarrative(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/narrative`).flush('Error', {
        status: 503, statusText: 'Service Unavailable'
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getMemory()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getMemory()', () => {
    const MEMORY_FIXTURE: MemoryResponse = {
      familyId: FAMILY_ID,
      episodic: [],
      semantic: [],
      procedural: []
    } as any;

    it('debe hacer GET a /api/cognitive/{id}/memory', () => {
      service.getMemory(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/memory`);
      expect(req.request.method).toBe('GET');
      req.flush(wrap(MEMORY_FIXTURE));
    });

    it('debe desempaquetar r.data con las tres listas', (done) => {
      service.getMemory(FAMILY_ID).subscribe(result => {
        expect(result?.episodic).toEqual([]);
        expect(result?.semantic).toEqual([]);
        expect(result?.procedural).toEqual([]);
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/memory`).flush(wrap(MEMORY_FIXTURE));
    });

    it('debe devolver null en error', (done) => {
      service.getMemory(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/memory`).flush('Error', {
        status: 500, statusText: 'Error'
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getIcfHistory() — URL base distinta: /api/analytics/
  // ═══════════════════════════════════════════════════════════════════════

  describe('getIcfHistory()', () => {
    const ICF_POINTS: IcfHistoryPoint[] = [
      { evaluationId: 101, icf: 55, riskLevel: 'LOW', hasCrisis: false, finalizedAt: '2025-01-01T00:00:00' },
      { evaluationId: 102, icf: 72, riskLevel: 'LOW', hasCrisis: false, finalizedAt: '2025-02-01T00:00:00' }
    ];

    it('debe hacer GET a /api/analytics/family/{id}/icf-history', () => {
      service.getIcfHistory(FAMILY_ID).subscribe();

      const req = http.expectOne(`/api/analytics/family/${FAMILY_ID}/icf-history`);
      expect(req.request.method).toBe('GET');
      req.flush(wrap(ICF_POINTS));
    });

    it('debe desempaquetar r.data (array)', (done) => {
      service.getIcfHistory(FAMILY_ID).subscribe(result => {
        expect(result.length).toBe(2);
        expect(result[0].evaluationId).toBe(101);
        done();
      });

      http.expectOne(`/api/analytics/family/${FAMILY_ID}/icf-history`).flush(wrap(ICF_POINTS));
    });

    it('debe devolver [] (array vacío) en error — no null', (done) => {
      service.getIcfHistory(FAMILY_ID).subscribe(result => {
        expect(result).toEqual([]);
        done();
      });

      http.expectOne(`/api/analytics/family/${FAMILY_ID}/icf-history`).flush('Error', {
        status: 500, statusText: 'Error'
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getLatestReflection()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getLatestReflection()', () => {
    it('debe hacer GET a /api/cognitive/{id}/reflection/latest', () => {
      service.getLatestReflection(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/reflection/latest`);
      expect(req.request.method).toBe('GET');
      req.flush(wrap(REFLECTION_FIXTURE));
    });

    it('debe devolver null en error', (done) => {
      service.getLatestReflection(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/reflection/latest`).flush('Error', {
        status: 403, statusText: 'Forbidden'
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  triggerReflection() — único método POST
  // ═══════════════════════════════════════════════════════════════════════

  describe('triggerReflection()', () => {
    it('debe hacer POST a /api/cognitive/{id}/reflect', () => {
      service.triggerReflection(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/reflect`);
      expect(req.request.method).toBe('POST');
      req.flush(wrap(REFLECTION_FIXTURE));
    });

    it('debe enviar body vacío {}', () => {
      service.triggerReflection(FAMILY_ID).subscribe();

      const req = http.expectOne(`${BASE}/${FAMILY_ID}/reflect`);
      expect(req.request.body).toEqual({});
      req.flush(wrap(REFLECTION_FIXTURE));
    });

    it('debe desempaquetar r.data del ApiResponse', (done) => {
      service.triggerReflection(FAMILY_ID).subscribe(result => {
        expect(result?.effectivenessLevel).toBe('MODERATE');
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/reflect`).flush(wrap(REFLECTION_FIXTURE));
    });

    it('debe devolver null en error — no lanzar excepción', (done) => {
      service.triggerReflection(FAMILY_ID).subscribe(result => {
        expect(result).toBeNull();
        done();
      });

      http.expectOne(`${BASE}/${FAMILY_ID}/reflect`).flush('Error', {
        status: 500, statusText: 'Internal Server Error'
      });
    });
  });
});
