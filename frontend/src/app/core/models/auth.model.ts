/**
 * Modelos de Autenticación - Nodo Armenia
 * Sincronizados con las clases Java del Backend para asegurar el flujo de datos.
 */

export interface LoginRequest {
  email: string;      // Debe coincidir con el campo en tu LoginRequest.java
  password: string;   // O 'pin' si estás usando el sistema de acceso simplificado
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role?: string;      // Opcional, por defecto suele ser USER o FAMILY_ADMIN
}

/**
 * AuthResponse: Esta es la "Llave Maestra". 
 * Se ha optimizado para que incluya fullName y así evitar errores en el Navbar.
 */
export interface AuthResponse {
  token: string;
  type: string;       // Usualmente "Bearer"
  email: string;
  fullName: string;   // Crítico: Esto permite que 'this.auth.fullName' sea real
  roles: string[];    // Lista de permisos (ADMIN, USER, etc.)
  user?: User;        // Enlace al objeto de usuario completo si es necesario
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: string;
}