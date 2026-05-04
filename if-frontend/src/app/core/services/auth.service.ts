import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FamilyStateService } from './family-state.service';

/** * SDD: Contrato de Identidad Global
 */
export interface AuthUser {
  token: string;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'USER' | 'SENTINEL';
  familyId?: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private http: HttpClient,
    private router: Router,
    private familyState: FamilyStateService
  ) {}


  // Sincronización de URL según especificación del entorno
  private readonly authUrl = `${environment.apiUrl}${environment.apiBaseUrl}/auth`;

  // 1. Estado reactivo tipado mediante Signals
  private _user = signal<AuthUser | null>(this.loadUserFromStorage());

  // 2. Exposición de estado (Readonly para protección de datos)
  readonly user = this._user.asReadonly();

  /**
   * SÍMBOLO CRÍTICO: Utilizado por Guards e Interceptors
   */
  get currentUserValue(): AuthUser | null {
    return this._user();
  }

  /**
   * SÍMBOLO CRÍTICO: Utilizado por Sidebar y Navbar
   */
  get fullName(): string {
    return this._user()?.fullName || 'Invitado';
  }

  /**
   * SDD: Protocolo de Autenticación
   */
  login(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/login`, credentials).pipe(
      tap(res => {
        if (res?.token) {
          const roleStr = res.user?.role || res.role || '';
          const roleMapping = (roleStr.includes('ADMIN') || roleStr.includes('SENTINEL')) ? 'ADMIN' : 'USER';

          const userData: AuthUser = {
            token: res.token,
            fullName: res.user?.fullName || res.fullName || 'Usuario',
            email: res.user?.email || res.email,
            role: roleMapping,
            familyId: res.user?.familyId || res.familyId
          };
          this.saveUserToStorage(userData);
          this._user.set(userData);
          if (userData.familyId) {
            this.familyState.setFamily({ id: userData.familyId, name: userData.fullName });
          }
        }
      })
    );
  }

  /**
   * SDD: Protocolo de Registro (Fix para build de producción)
   * Hecho: Ahora guarda el token y estado post-registro para evitar 403 inmediatamente.
   */
  register(payload: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/register`, payload).pipe(
      tap(res => {
        if (res?.token) {
          const roleStr = res.user?.role || res.role || '';
          const roleMapping = (roleStr.includes('ADMIN') || roleStr.includes('SENTINEL')) ? 'ADMIN' : 'USER';
          const userData: AuthUser = {
            token: res.token,
            fullName: res.user?.fullName || res.fullName || 'Usuario',
            email: res.user?.email || res.email,
            role: roleMapping,
            familyId: res.user?.familyId || res.familyId
          };
          this.saveUserToStorage(userData);
          this._user.set(userData);
          if (userData.familyId) {
            this.familyState.setFamily({ id: userData.familyId, name: userData.fullName });
          }
        }
      })
    );
  }

  /**
   * SDD: Registro de Nueva Familia (Admin Flow)
   */
  registerFamily(payload: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/register-family`, payload).pipe(
      tap(res => {
        if (res?.token) {
          const userData: AuthUser = {
            token: res.token,
            fullName: res.user?.fullName || res.fullName || 'Administrador',
            email: res.user?.email || res.email,
            role: 'ADMIN',
            familyId: res.user?.familyId || res.familyId
          };
          this.saveUserToStorage(userData);
          this._user.set(userData);
          if (userData.familyId) {
            this.familyState.setFamily({ id: userData.familyId, name: userData.fullName });
          }
        }
      })
    );
  }



  /**
   * SÍMBOLO CRÍTICO: Utilizado por Guards antiguos y el Interceptor
   */
  getToken(): string | null {
    return this._user()?.token || null;
  }

  /**
   * SDD: Verificación de Sesión Activa
   */
  isAuthenticated(): boolean {
    return !!this._user()?.token;
  }

  /**
   * SDD: Protocolo de Cierre de Seguridad (Sentinel Reset)
   */
  logout(): void {
    console.warn('SENTINEL: Limpiando estado de sesión y redirigiendo al acceso.');
    localStorage.removeItem('auth_user');
    this._user.set(null);
    this.router.navigate(['/auth/login']);
  }

  private saveUserToStorage(user: AuthUser): void {
    localStorage.setItem('auth_user', JSON.stringify(user));
  }

  private loadUserFromStorage(): AuthUser | null {
    try {
      const saved = localStorage.getItem('auth_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null; // Mitigación ante corrupción de datos local
    }
  }
}