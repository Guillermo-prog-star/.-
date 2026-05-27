import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';

import { ProfilePageComponent } from './profile-page.component';
import { AuthService } from '../../core/services/auth.service';

describe('ProfilePageComponent', () => {
  let component: ProfilePageComponent;
  let fixture: ComponentFixture<ProfilePageComponent>;

  const userSignal = signal({
    id: 1,
    email: 'william@test.com',
    fullName: 'William Lopez',
    role: 'ADMIN',
    familyId: 1,
    familyName: 'Familia Lopez'
  });

  const authServiceMock = {
    user: userSignal,
    getToken: jasmine.createSpy('getToken').and.returnValue('fake-token'),
    getAuthenticatedProfile: jasmine.createSpy('getAuthenticatedProfile').and.returnValue(
      of({
        id: 1,
        email: 'william@test.com',
        fullName: 'William Lopez',
        role: 'ADMIN',
        familyId: 1,
        familyName: 'Familia Lopez'
      })
    ),
    logout: jasmine.createSpy('logout')
  };

  beforeEach(async () => {
    authServiceMock.getAuthenticatedProfile.and.returnValue(
      of({
        id: 1,
        email: 'william@test.com',
        fullName: 'William Lopez',
        role: 'ADMIN',
        familyId: 1,
        familyName: 'Familia Lopez'
      })
    );

    await TestBed.configureTestingModule({
      imports: [ProfilePageComponent],
      providers: [
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfilePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    authServiceMock.getToken.calls.reset();
    authServiceMock.getAuthenticatedProfile.calls.reset();
    authServiceMock.logout.calls.reset();
  });

  it('debe crear el componente de perfil', () => {
    expect(component).toBeTruthy();
  });

  it('debe cargar el perfil autenticado desde AuthService', () => {
    expect(authServiceMock.getAuthenticatedProfile).toHaveBeenCalled();

    expect(component.displayName()).toBe('William Lopez');
    expect(component.displayEmail()).toBe('william@test.com');
    expect(component.roleLabel()).toBe('Administrador');
  });

  it('debe marcar token activo cuando existe token', () => {
    expect(component.isTokenActive()).toBeTrue();
  });

  it('debe calcular iniciales correctamente', () => {
    expect(component.initials()).toBe('WL');
  });

  it('debe cerrar sesión usando AuthService tras confirmación inline', () => {
    component.logout();
    expect(component.showLogoutConfirm()).toBeTrue();

    component.confirmLogout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });

  it('debe cancelar cierre de sesión al llamar cancelLogout()', () => {
    component.logout();
    component.cancelLogout();
    expect(component.showLogoutConfirm()).toBeFalse();
    expect(authServiceMock.logout).not.toHaveBeenCalled();
  });

  it('debe usar datos locales cuando falla la verificación del backend', () => {
    authServiceMock.getAuthenticatedProfile.and.returnValue(
      throwError(() => new Error('Backend error'))
    );

    fixture = TestBed.createComponent(ProfilePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.hasBackendError()).toBeTrue();
    expect(component.displayName()).toBe('William Lopez');
    expect(component.displayEmail()).toBe('william@test.com');
  });

  it('debe clasificar rol familiar no administrador como Miembro Familiar', () => {
    authServiceMock.getAuthenticatedProfile.and.returnValue(
      of({
        id: 2,
        email: 'hijo@test.com',
        fullName: 'Hijo Lopez',
        role: 'MEMBER',
        familyId: 1,
        familyName: 'Familia Lopez'
      })
    );

    fixture = TestBed.createComponent(ProfilePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.roleLabel()).toBe('Miembro Familiar');
    expect(component.roleColorClass()).toBe('badge-user');
  });
});
