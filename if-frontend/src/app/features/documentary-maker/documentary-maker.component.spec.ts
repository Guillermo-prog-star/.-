import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DocumentaryMakerComponent } from './documentary-maker.component';

describe('DocumentaryMakerComponent', () => {
  let component: DocumentaryMakerComponent;
  let fixture: ComponentFixture<DocumentaryMakerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentaryMakerComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DocumentaryMakerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
