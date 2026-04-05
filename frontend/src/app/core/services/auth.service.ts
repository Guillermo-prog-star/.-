import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Construcción segura y unificada de la URL del API
  private readonly authUrl = `${environment.apiUrl}${environment.apiBaseUrl}/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  /**
   * Realiza el login y persiste la sesión en el navegador.
   * Optimizado para manejar la sincronización del usuario William Lopez.
   */
  login(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/login`, credentials).pipe(
      tap(res => {
        if (res?.token) {
          localStorage.setItem('token', res.token);
          // Fallback al nombre por defecto si el backend no lo envía
          localStorage.setItem('fullName', res.fullName || 'William Lopez');
        }
      })
    );
  }

  register(payload: any): Observable<any> {
    return this.http.post(`${this.authUrl}/register`, payload);
  }

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  // Getters para gestión de estado de sesión
  getToken(): string | null { 
    return localStorage.getItem('token'); 
  }

  get fullName(): string | null { 
    return localStorage.getItem('fullName'); 
  }

  isLoggedIn(): boolean { 
    return !!this.getToken(); 
  }
}