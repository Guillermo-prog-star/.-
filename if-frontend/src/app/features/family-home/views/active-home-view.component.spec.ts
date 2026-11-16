import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActiveHomeViewComponent } from './active-home-view.component';
import { buildActiveView, navigateCommand } from '../family-home-test-fixtures';

describe('ActiveHomeViewComponent', () => {
  function build() {
    const fixture = TestBed.createComponent(ActiveHomeViewComponent);
    return { fixture, component: fixture.componentInstance };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ActiveHomeViewComponent] });
  });

  it('muestra la narrativa de hoy y emite el comando primario', () => {
    const { fixture, component } = build();
    component.view = buildActiveView();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Mantente al día con tus actividades compartidas.');

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    fixture.debugElement.queryAll(By.css('.fh-cta'))[0].nativeElement.click();

    expect(emitted).toEqual(component.view.today!.primaryCommand);
  });

  it('muestra el progreso del sprint activo y emite el comando de la misión de hoy', () => {
    const { fixture, component } = build();
    component.view = buildActiveView();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sprint 1: Reconectar');
    expect(fixture.nativeElement.textContent).toContain('2/7');

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    const buttons = fixture.debugElement.queryAll(By.css('.fh-cta'));
    buttons[1].nativeElement.click();

    expect(emitted).toEqual(component.view.activeSprint!.todayMission);
  });

  it('no muestra el bloque de sprint cuando activeSprint es null', () => {
    const { fixture, component } = build();
    component.view = buildActiveView({ activeSprint: null });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Sprint Familiar');
  });

  it('muestra el bloque de retomar actividades cuando resumeBlock está presente', () => {
    const { fixture, component } = build();
    const resumeCmd = navigateCommand({ id: 'cmd-confirm-resume', label: 'Retomar Actividades', target: 'confirm-resume' });
    component.view = buildActiveView({
      resumeBlock: { instructionsKey: 'family.resume.welcome', confirmCommand: resumeCmd },
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Retomando el Ritmo Familiar');

    let emitted: unknown = null;
    component.command.subscribe((c) => (emitted = c));
    const buttons = fixture.debugElement.queryAll(By.css('.fh-cta'));
    buttons[buttons.length - 1].nativeElement.click();

    expect(emitted).toEqual(resumeCmd);
  });

  it('renderiza un chip por cada dimensión con su etiqueta traducida', () => {
    const { fixture, component } = build();
    component.view = buildActiveView();
    fixture.detectChanges();

    const chips = fixture.debugElement.queryAll(By.css('.fh-dimension-chip'));
    expect(chips.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Emociones');
    expect(fixture.nativeElement.textContent).toContain('Comunicación');
  });

  it('dimensionLabel() devuelve la clave cruda si no hay traducción registrada', () => {
    const { component } = build();
    component.view = buildActiveView();
    expect(component.dimensionLabel('unknownDimension')).toBe('unknownDimension');
  });
});
