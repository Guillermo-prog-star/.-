import { HttpContextToken } from '@angular/common/http';

/**
 * Marca una request como "silenciosa" ante 401: authInterceptor no
 * expulsará al usuario al login si esta request en particular falla.
 *
 * Existe para el polling de fondo de SentinelCoreService (interval de 15s,
 * providedIn:'root' -- vive toda la sesión de la SPA, no se destruye al
 * navegar ni al loguearse de nuevo). Una request en vuelo con un token
 * viejo puede resolver con 401 justo después de un login fresco y válido;
 * sin este flag, el interceptor global expulsaba al usuario de la página
 * a la que acababa de entrar, aunque su sesión actual fuera perfectamente
 * válida (bug: "entra y me saca" al ir a /professional).
 */
export const SILENT_ON_401 = new HttpContextToken<boolean>(() => false);
