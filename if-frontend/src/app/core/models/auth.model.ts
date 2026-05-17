/**
 * Modelos de Autenticación — Multi-Tenant
 * Sincronizados con las clases Java del Backend (AuthService, JwtTokenProvider).
 */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  voucher?: string;   // Código ALFA-XXXXXX para inmersión masiva
  role?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  email: string;
  fullName: string;
  roles: string[];
  familyId?: number;      // ID de la familia del usuario (claim 'fid' del JWT)
  familyName?: string;    // Nombre de la familia para el estado de sesión
  user?: {
    familyId?: number;
    familyName?: string;
    fullName?: string;
    email?: string;
    role?: string;
  };
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: string;
}

export interface RegisterFamilyRequest {
  familyName: string;
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;  // Requerido por el backend para validación
}