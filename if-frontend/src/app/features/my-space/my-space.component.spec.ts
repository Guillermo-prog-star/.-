import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { MySpaceComponent } from './my-space.component';
import { MySpaceService } from './services/my-space.service';

// ─── Helpers ────────────────────────────────────────────────────────────────

const ENTRIES_STUB = [
  { id: 1, title: 'Entrada 1', content: 'Contenido 1', emotionalState: 'CALM' },
  { id: 2, title: 'Entrada 2', content: 'Contenido 2', emotionalState: 'HAPPY' }
];

function buildComponent() {
  const mySpaceSpy = jasmine.createSpyObj<MySpaceService>(
    'MySpaceService', {
      getEntries:  of({ data: ENTRIES_STUB }),
      createEntry: of({ id: 99, title: 'Nueva' })
    }
  );

  TestBed.configureTestingModule({
    imports: [MySpaceComponent],
    providers: [{ provide: MySpaceService, useValue: mySpaceSpy }],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture   = TestBed.createComponent(MySpaceComponent);
  const component = fixture.componentInstance;

  return { fixture, component, mySpaceSpy };
}

// ─────────────────────────────────────────────────────────────────────────────

describe('MySpaceComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  // ═══════════════════════════════════════════════════════════════════════
  //  ngOnInit() / loadEntries()
  // ═══════════════════════════════════════════════════════════════════════

  describe('ngOnInit() / loadEntries()', () => {
    it('llama getEntries y puebla entries con response.data', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      expect(component.entries.length).toBe(2);
      expect(component.entries[0].title).toBe('Entrada 1');
      expect(component.loading).toBeFalse();
    }));

    it('respuesta sin .data → usa el array directamente', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      mySpaceSpy.getEntries.and.returnValue(of(ENTRIES_STUB));
      fixture.detectChanges();
      tick();

      expect(component.entries.length).toBe(2);
    }));

    it('error en getEntries → loading=false y entries permanece vacío', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      mySpaceSpy.getEntries.and.returnValue(throwError(() => new Error('500')));
      fixture.detectChanges();
      tick();

      expect(component.loading).toBeFalse();
      expect(component.entries).toEqual([]);
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  saveEntry()
  // ═══════════════════════════════════════════════════════════════════════

  describe('saveEntry()', () => {
    it('título vacío → alert y no llama createEntry', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      spyOn(window, 'alert');

      component.newEntry.title = '';
      component.newEntry.content = 'Algo';
      component.saveEntry();

      expect(window.alert).toHaveBeenCalled();
      expect(mySpaceSpy.createEntry).not.toHaveBeenCalled();
    }));

    it('contenido vacío → alert y no llama createEntry', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      spyOn(window, 'alert');

      component.newEntry.title = 'Título';
      component.newEntry.content = '';
      component.saveEntry();

      expect(window.alert).toHaveBeenCalled();
      expect(mySpaceSpy.createEntry).not.toHaveBeenCalled();
    }));

    it('campos válidos → llama createEntry con los datos del formulario', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.newEntry = {
        title: 'Mi reflexión',
        content: 'Texto interesante',
        emotionalState: 'CALM',
        category: 'REFLEXION'
      };
      component.saveEntry();
      tick();

      expect(mySpaceSpy.createEntry).toHaveBeenCalledWith(
        jasmine.objectContaining({ title: 'Mi reflexión', content: 'Texto interesante' })
      );
    }));

    it('éxito → resetea el formulario a valores por defecto', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.newEntry = {
        title: 'T', content: 'C', emotionalState: 'HAPPY', category: 'LOGRO'
      };
      component.saveEntry();
      tick();

      expect(component.newEntry.title).toBe('');
      expect(component.newEntry.content).toBe('');
      expect(component.newEntry.emotionalState).toBe('NEUTRAL');
      expect(component.newEntry.category).toBe('REFLEXION');
    }));

    it('éxito → recarga las entradas (llama getEntries de nuevo)', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();

      component.newEntry = { title: 'T', content: 'C', emotionalState: 'CALM', category: 'REFLEXION' };
      component.saveEntry();
      tick();

      // detectChanges → 1, saveEntry → 1 más = al menos 2 llamadas
      expect(mySpaceSpy.getEntries.calls.count()).toBeGreaterThan(1);
    }));

    it('error en createEntry → loading=false y no resetea el formulario', fakeAsync(() => {
      const { fixture, component, mySpaceSpy } = buildComponent();
      fixture.detectChanges();
      tick();
      spyOn(window, 'alert');

      mySpaceSpy.createEntry.and.returnValue(throwError(() => new Error('503')));
      component.newEntry = { title: 'T', content: 'C', emotionalState: 'CALM', category: 'REFLEXION' };
      component.saveEntry();
      tick();

      expect(component.loading).toBeFalse();
      expect(component.newEntry.title).toBe('T'); // no reseteado
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  resetForm()
  // ═══════════════════════════════════════════════════════════════════════

  describe('resetForm()', () => {
    it('restaura todos los campos a sus valores iniciales', fakeAsync(() => {
      const { fixture, component } = buildComponent();
      fixture.detectChanges();
      tick();

      component.newEntry = { title: 'X', content: 'Y', emotionalState: 'ANGRY', category: 'DUDA' };
      component.resetForm();

      expect(component.newEntry).toEqual({
        title: '', content: '', emotionalState: 'NEUTRAL', category: 'REFLEXION'
      });
    }));
  });

  // ═══════════════════════════════════════════════════════════════════════
  //  getEmotionIcon()
  // ═══════════════════════════════════════════════════════════════════════

  describe('getEmotionIcon()', () => {
    let component: MySpaceComponent;

    beforeEach(fakeAsync(() => {
      const ctx = buildComponent();
      ctx.fixture.detectChanges();
      tick();
      component = ctx.component;
    }));

    it('CALM → 😊', () => expect(component.getEmotionIcon('CALM')).toBe('😊'));
    it('HAPPY → 😃', () => expect(component.getEmotionIcon('HAPPY')).toBe('😃'));
    it('SAD → 😢',   () => expect(component.getEmotionIcon('SAD')).toBe('😢'));
    it('ANGRY → 😠', () => expect(component.getEmotionIcon('ANGRY')).toBe('😠'));
    it('ANXIOUS → 😰', () => expect(component.getEmotionIcon('ANXIOUS')).toBe('😰'));
    it('NEUTRAL → 😐', () => expect(component.getEmotionIcon('NEUTRAL')).toBe('😐'));
    it('estado desconocido → 📝 (fallback)', () =>
      expect(component.getEmotionIcon('UNKNOWN')).toBe('📝'));
  });
});
