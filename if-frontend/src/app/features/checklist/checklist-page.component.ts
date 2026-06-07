import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { ChecklistItem, Plan } from '../../core/models/models';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ScrollPolicyService } from '../../shared/directives/scroll-policy.service';

@Component({
  selector: 'app-checklist-page', 
  standalone: true, 
  imports: [CommonModule, FormsModule],
  templateUrl: './checklist-page.component.html',
  styleUrls: ['./checklist-page.component.css']
})
export class ChecklistPageComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient); 
  private api = inject(ApiService);
  private familyState  = inject(FamilyStateService);
  private scrollPolicy = inject(ScrollPolicyService);

  items: any[] = []; 
  resolvedEvidences: any[] = []; // Archivo permanente de evidencias (Victorias de todos los hitos)
  taskEvidences: any[] = [];    // Victorias de misiones validadas por Sentinel AI (Claude)
  loading = false; 
  activeContemplation: any | null = null;
  activeInlineEvidenceId: number | null = null;
  private mapInstance: any = null;
  private inlineMapInstances: { [key: number]: any } = {};
  
  get familyId() { return this.familyState.getSelectedFamilyId(); }

  get done() { return this.items.filter(i => i.completed).length; }
  get pct()  { return this.items.length ? Math.round(this.done / this.items.length * 100) : 0; }

  // Agrupación por dimensiones para estética premium
  get itemsByDimension() {
    const groups: { [key: string]: any[] } = { 'emociones': [], 'comunicacion': [], 'habitos': [], 'tiempos': [], 'general': [] };
    this.items.forEach(i => {
      const dim = i.dimension || 'general';
      if (groups[dim]) groups[dim].push(i);
      else groups['general'].push(i);
    });
    return Object.entries(groups).filter(([_, items]) => items.length > 0);
  }

  getEmotionDetails(emotionKey: string | null | undefined) {
    const emotionsMap: { [key: string]: { label: string; emoji: string; color: string; glow: string } } = {
      alegria:   { label: 'Alegría',     emoji: '😊', color: '#ffb300', glow: 'rgba(255, 179, 0, 0.2)' },
      gratitud:  { label: 'Gratitud',    emoji: '🙏', color: '#a78bfa', glow: 'rgba(167, 139, 250, 0.2)' },
      amor:      { label: 'Amor',        emoji: '❤️', color: '#fb7185', glow: 'rgba(251, 113, 133, 0.2)' },
      orgullo:   { label: 'Orgullo',     emoji: '🌟', color: '#fbbf24', glow: 'rgba(251, 191, 36, 0.2)' },
      calma:     { label: 'Calma',       emoji: '😌', color: '#2dd4bf', glow: 'rgba(45, 212, 191, 0.2)' },
      esperanza: { label: 'Esperanza',   emoji: '🌱', color: '#34d399', glow: 'rgba(52, 211, 153, 0.2)' },
      tristeza:  { label: 'Tristeza',    emoji: '😢', color: '#60a5fa', glow: 'rgba(96, 165, 250, 0.2)' },
      tension:   { label: 'Tensión',     emoji: '😤', color: '#f87171', glow: 'rgba(248, 113, 113, 0.2)' },
    };
    return emotionsMap[emotionKey || ''] || { label: 'Reflexión', emoji: '📸', color: '#58a6ff', glow: 'rgba(88, 166, 255, 0.15)' };
  }
  
  ngOnInit() {
    this.scrollPolicy.set('scroll-to-new');
    if (this.familyId) {
      this.load();
    }
  }
  
  load() {
    this.loading = true;
    
    // Cargar hábitos/checklist
    this.http.get<any>(`${this.api.base}/checklist/family/${this.familyId}`)
      .subscribe({ 
        next: ({ data }) => { 
          this.items = data || []; 
          this.loading = false; 
        }, 
        error: () => {
          this.loading = false;
        } 
      });
 
    // Cargar evidencias históricas acumuladas de la bitácora (estado RESOLVED)
    this.http.get<any[]>(`${this.api.base}/family-logbook/family/${this.familyId}/status/RESOLVED`)
      .subscribe({
        next: (data) => {
          this.resolvedEvidences = data || [];
        },
        error: () => {}
      });

    // Cargar evidencias de misiones validadas por Sentinel AI
    this.http.get<any>(`${this.api.base}/evidences/family/${this.familyId}`)
      .subscribe({
        next: (res) => {
          if (res && res.data) {
            this.taskEvidences = res.data.filter((e: any) => e.status === 'VALIDATED');
          }
        },
        error: () => {}
      });
  }
  
  toggle(id: number, current: boolean) {
    if (current) return; // Por ahora solo marcamos como completado para rigor pedagógico
    
    // Simular quién completa (en versión pro vendría del perfil)
    const who = 'Núcleo Familiar';
    
    this.http.put<any>(`${this.api.base}/checklist/${id}/complete`, { completedBy: who })
      .subscribe({ next: () => this.load() });
  }

  openContemplation(ev: any) {
    this.activeContemplation = ev;
    if (ev.latitude && ev.longitude) {
      this.initMap(ev.latitude, ev.longitude);
    }
  }

  closeContemplation() {
    if (this.mapInstance) {
      try {
        this.mapInstance.remove();
      } catch (e) {
        console.error('Error removing map instance:', e);
      }
      this.mapInstance = null;
    }
    this.activeContemplation = null;
  }

  private initMap(lat: number, lng: number) {
    // Cargar CSS de Leaflet si no existe
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    // Cargar JS de Leaflet si no existe, luego instanciar
    if (typeof (window as any).L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.setupLeafletMap(lat, lng);
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.setupLeafletMap(lat, lng), 100);
    }
  }

  private setupLeafletMap(lat: number, lng: number) {
    if (!this.activeContemplation) return;

    if (this.mapInstance) {
      try {
        this.mapInstance.remove();
      } catch {}
      this.mapInstance = null;
    }

    setTimeout(() => {
      const container = document.getElementById('contemplation-map');
      if (!container) return;

      try {
        const L = (window as any).L;
        this.mapInstance = L.map('contemplation-map', {
          zoomControl: false,
          attributionControl: false
        }).setView([lat, lng], 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          maxZoom: 20
        }).addTo(this.mapInstance);

        const customIcon = L.divIcon({
          className: 'leaflet-custom-marker',
          html: `<div style="background-color: ${this.getEmotionDetails(this.activeContemplation.emotion).color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        });

        L.marker([lat, lng], { icon: customIcon }).addTo(this.mapInstance);
      } catch (e) {
        console.error('Error setting up Leaflet map:', e);
      }
    }, 150);
  }

  toggleInlineContemplation(ev: any) {
    if (this.activeInlineEvidenceId === ev.id) {
      this.closeInlineContemplation(ev.id);
      this.activeInlineEvidenceId = null;
    } else {
      if (this.activeInlineEvidenceId !== null) {
        this.closeInlineContemplation(this.activeInlineEvidenceId);
      }
      this.activeInlineEvidenceId = ev.id;
      if (ev.latitude && ev.longitude) {
        this.initInlineMap(ev.id, ev.latitude, ev.longitude);
      }
    }
  }

  private closeInlineContemplation(id: number) {
    const map = this.inlineMapInstances[id];
    if (map) {
      try {
        map.remove();
      } catch (e) {
        console.error('Error removing inline map:', e);
      }
      delete this.inlineMapInstances[id];
    }
  }

  private initInlineMap(id: number, lat: number, lng: number) {
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    if (typeof (window as any).L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.setupInlineMap(id, lat, lng);
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.setupInlineMap(id, lat, lng), 150);
    }
  }

  private setupInlineMap(id: number, lat: number, lng: number) {
    if (this.activeInlineEvidenceId !== id) return;

    this.closeInlineContemplation(id);

    setTimeout(() => {
      const mapId = `contemplation-map-inline-${id}`;
      const container = document.getElementById(mapId);
      if (!container) return;

      try {
        const L = (window as any).L;
        const map = L.map(mapId, {
          zoomControl: false,
          attributionControl: false
        }).setView([lat, lng], 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          maxZoom: 20
        }).addTo(map);

        const ev = this.taskEvidences.find((e: any) => e.id === id) || this.resolvedEvidences.find((e: any) => e.id === id);
        const emotion = ev ? (ev.emotion || ev.emotionIdentified) : null;
        const color = this.getEmotionDetails(emotion).color;

        const customIcon = L.divIcon({
          className: 'leaflet-custom-marker',
          html: `<div style="background-color: ${color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 10px rgba(0,0,0,0.5);"></div>`,
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        });

        L.marker([lat, lng], { icon: customIcon }).addTo(map);
        this.inlineMapInstances[id] = map;

        setTimeout(() => {
          map.invalidateSize();
        }, 300);
      } catch (e) {
        console.error('Error setting up inline Leaflet map:', e);
      }
    }, 150);
  }

  ngOnDestroy() {
    this.closeContemplation();
    Object.keys(this.inlineMapInstances).forEach(id => {
      this.closeInlineContemplation(Number(id));
    });
  }

  onAudioLoaded(event: Event): void {
    const audio = event.target as HTMLAudioElement;
    if (audio) {
      if (audio.duration === Infinity || isNaN(audio.duration) || audio.duration === 0) {
        audio.currentTime = 1e101;
        const onTimeUpdate = () => {
          audio.currentTime = 0;
          audio.removeEventListener('timeupdate', onTimeUpdate);
        };
        audio.addEventListener('timeupdate', onTimeUpdate);
      }
    }
  }
}
