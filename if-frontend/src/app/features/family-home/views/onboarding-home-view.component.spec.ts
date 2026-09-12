import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { OnboardingHomeViewComponent } from './onboarding-home-view.component';
import { buildOnboardingView, navigateCommand } from '../family-home-test-fixtures';

describe('OnboardingHomeViewComponent', () => {
  function build() {
    const fixture = TestBed.createComponent(OnboardingHomeViewComponent);
    return { fixture, component: fixture.componentInstance };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [OnboardingHomeViewComponent] });
  });

  it('muestra el nombre de la familia y el progreso del onboarding', () => {
    const { fixture, component } = build();
    component.view = buildOnboardingView();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Familia Lopez');
    expect(fixture.debugElement.query(By.css('.fh-progress-pct')).nativeElement.textContent).toContain('0/4');
  });

  it('emite el comando al hacer click en el CTA', () => {
    const { fixture, component } = build();
    const cmd = navigateCommand();
    component.view = buildOnboardingView({
      journey: { stage: 'NEW_FAMILY', progress: { completed: 0, total: 4, percentage: 0 }, nextCommand: cmd },
    });
    fixture.detectChanges();

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    fixture.debugElement.query(By.css('.fh-cta')).nativeElement.click();

    expect(emitted).toEqual(cmd);
  });

  it('deshabilita el CTA cuando el comando no está habilitado', () => {
    const { fixture, component } = build();
    component.view = buildOnboardingView({
      journey: {
        stage: 'NEW_FAMILY',
        progress: { completed: 0, total: 4, percentage: 0 },
        nextCommand: navigateCommand({ enabled: false }),
      },
    });
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.fh-cta')).nativeElement.disabled).toBeTrue();
  });
});
