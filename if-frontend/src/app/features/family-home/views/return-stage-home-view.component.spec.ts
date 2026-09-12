import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ReturnStageHomeViewComponent } from './return-stage-home-view.component';
import { buildReturnStageView, submitActionCommand } from '../family-home-test-fixtures';

describe('ReturnStageHomeViewComponent', () => {
  function build() {
    const fixture = TestBed.createComponent(ReturnStageHomeViewComponent);
    return { fixture, component: fixture.componentInstance };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ReturnStageHomeViewComponent] });
  });

  it('muestra el CTA "Aceptar Primer Sprint" y lo emite al hacer click', () => {
    const { fixture, component } = build();
    const cmd = submitActionCommand();
    component.view = buildReturnStageView({
      journey: { stage: 'RETURN_AVAILABLE', progress: { completed: 1, total: 1, percentage: 100 }, nextCommand: cmd },
    });
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('.fh-cta')).nativeElement;
    expect(button.textContent).toContain('Aceptar Primer Sprint');

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    button.click();

    expect(emitted).toEqual(cmd);
  });

  it('deshabilita el CTA y muestra "Creando sprint…" mientras actionPending es true', () => {
    const { fixture, component } = build();
    component.view = buildReturnStageView();
    component.actionPending = true;
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('.fh-cta')).nativeElement;
    expect(button.disabled).toBeTrue();
    expect(button.textContent).toContain('Creando sprint…');
  });
});
