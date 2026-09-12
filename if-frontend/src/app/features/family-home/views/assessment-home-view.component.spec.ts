import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { AssessmentHomeViewComponent } from './assessment-home-view.component';
import { buildAssessmentView, navigateCommand } from '../family-home-test-fixtures';

describe('AssessmentHomeViewComponent', () => {
  function build() {
    const fixture = TestBed.createComponent(AssessmentHomeViewComponent);
    return { fixture, component: fixture.componentInstance };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AssessmentHomeViewComponent] });
  });

  it('muestra el porcentaje de avance de la evaluación', () => {
    const { fixture, component } = build();
    component.view = buildAssessmentView();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.fh-progress-pct')).nativeElement.textContent).toContain('30%');
  });

  it('emite el comando "Continuar evaluación" al hacer click', () => {
    const { fixture, component } = build();
    const cmd = navigateCommand({ id: 'cmd-assess', label: 'Continuar evaluación', target: '/journey/assessment' });
    component.view = buildAssessmentView({
      journey: { stage: 'ASSESSMENT_IN_PROGRESS', progress: { completed: 12, total: 40, percentage: 30 }, nextCommand: cmd },
    });
    fixture.detectChanges();

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    fixture.debugElement.query(By.css('.fh-cta')).nativeElement.click();

    expect(emitted).toEqual(cmd);
  });
});
