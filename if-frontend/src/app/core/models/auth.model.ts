/**
 * Modelos de Autenticación - Nodo Armenia
 * Sincronizados con las clases Java del Backend para asegurar el flujo de datos.
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
}