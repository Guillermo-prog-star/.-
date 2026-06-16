import { TestBed } from '@angular/core/testing';

import { DocumentaryProductionService } from './documentary-production.service';

describe('DocumentaryProductionService', () => {
  let service: DocumentaryProductionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DocumentaryProductionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
