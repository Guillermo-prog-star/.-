import { Injectable } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class StorageService {
  get(k: string): string | null { return localStorage.getItem(k); }
  set(k: string, v: string): void { localStorage.setItem(k, v); }
  remove(k: string): void { localStorage.removeItem(k); }
  clear(): void { localStorage.clear(); }
  getNumber(k: string, def = 1): number { return Number(this.get(k) ?? def); }
}
