import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  // ELIMINAMOS el 'localhost' manual para usar la variable del entorno
  readonly base = environment.apiBaseUrl;
}