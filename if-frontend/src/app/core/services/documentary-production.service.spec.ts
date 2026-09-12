import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DocumentaryProductionService } from './documentary-production.service';

describe('DocumentaryProductionService', () => {
  let service: DocumentaryProductionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DocumentaryProductionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
