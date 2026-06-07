import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FamilyTimelineService, TimelineEvent, EventType } from '../../core/services/family-timeline.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { catchError, of } from 'rxjs';

const EVENT_CONFIG: Record<EventType, { icon: string; color: string; label: string }> = {
  EVALUATION:    { icon: '🎯', color: '#6366f1', label: 'Diagnóstico' },
  GRATITUDE:     { icon: '💖', color: '#ec4899', label: 'Gratitud' },
  LOGBOOK:       { icon: '📔', color: '#f59e0b', label: 'Bitácora' },
  EVIDENCE:      { icon: '📸', color: '#10b981', label: 'Evidencia' },
  CRISIS:        { icon: '🆘', color: '#ef4444', label: 'Crisis' },
  MISSION:       { icon: '🏆', color: '#8b5cf6', label: 'Misión' },
  DNA:           { icon: '🧬', color: '#06b6d4', label: 'ADN Familiar' },
  MEMBER_JOINED: { icon: '👤', color: '#64748b', label: 'Nuevo miembro' },
};

@Component({
  selector: 'app-family-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="tl-page">

      <!-- Header -->
      <div class="tl-header">
        <div class="tl-icon">📜</div>
        <div>
          <h1 class="tl-title">Historia Familiar</h1>
          <p class="tl-sub">Cada momento que han vivido juntos, en una sola línea de tiempo</p>
        </div>
      </div>

      <!-- Filtros por tipo -->
      @if (events().length) {
        <div class="tl-filters">
          <button class="tl-filter" [class.active]="activeFilter() === null" (click)="activeFilter.set(null)">
            Todos <span class="count">{{ events().length }}</span>
          </button>
          @for (entry of filterEntries(); track entry.type) {
            <button class="tl-filter" [class.active]="activeFilter() === entry.type" (click)="activeFilter.set(entry.type)">
              {{ entry.icon }} {{ entry.label }} <span class="count">{{ entry.count }}</span>
            </button>
          }
        </div>
      }

      <!-- Cargando -->
      @if (loading()) {
        <div class="tl-loading">
          <div class="tl-spinner"></div>
          <p>Construyendo la historia de tu familia...</p>
        </div>
      }

      <!-- Sin familia -->
      @if (!familyId() && !loading()) {
        <div class="tl-empty">
          <div class="ei">👨‍👩‍👧‍👦</div>
          <p>Selecciona una familia para ver su historia.</p>
        </div>
      }

      <!-- Sin eventos -->
      @if (familyId() && !loading() && !events().length) {
        <div class="tl-empty">
          <div class="ei">🌱</div>
          <h2>La historia está por comenzar</h2>
          <p>Cada diagnóstico, gratitud, misión y momento registrado irá apareciendo aquí.</p>
        </div>
      }

      <!-- Timeline -->
      @if (filtered().length) {
        <div class="tl-track">
          <!-- Agrupado por mes -->
          @for (group of grouped(); track group.label) {
            <div class="tl-month-group">
              <div class="month-label">{{ group.label }}</div>
              @for (ev of group.events; track ev.id) {
                <div class="tl-event" [style.--accent]="color(ev.type)">
                  <div class="tl-dot">
                    <span class="dot-icon">{{ icon(ev.type) }}</span>
                  </div>
                  <div class="tl-card">
                    <div class="tc-top">
                      <span class="tc-badge" [style.background]="colorFaint(ev.type)" [style.color]="color(ev.type)">
                        {{ label(ev.type) }}
                      </span>
                      <span class="tc-date">{{ formatDate(ev.occurredAt) }}</span>
                    </div>
                    <div class="tc-title">{{ ev.title }}</div>
                    @if (ev.description) {
                      <div class="tc-desc">{{ truncate(ev.description, 140) }}</div>
                    }
                    <div class="tc-meta-row">
                      @if (ev.actor && ev.actor !== 'Familia') {
                        <span class="tc-actor">{{ ev.actor }}</span>
                      }
                      @if (ev.emotion) {
                        <span class="tc-emotion">{{ ev.emotion }}</span>
                      }
                    </div>
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }

      <!-- Error -->
      @if (error()) {
        <div class="tl-error">⚠️ {{ error() }}</div>
      }

    </div>
  `,
  styles: [`
    .tl-page {
      max-width: 760px;
      margin: 0 auto;
      padding: 24px 20px 60px;
      color: var(--if-text-primary, #e0e0e0);
      font-family: inherit;
    }

    /* Header */
    .tl-header { display: flex; align-items: center; gap: 16px; margin-bottom: 28px; }
    .tl-icon { font-size: 44px; }
    .tl-title { font-size: 26px; font-weight: 800; margin: 0 0 4px; }
    .tl-sub { font-size: 13px; color: var(--if-text-secondary, #888); margin: 0; }

    /* Filtros */
    .tl-filters {
      display: flex; flex-wrap: wrap; gap: 8px;
      margin-bottom: 28px;
    }
    .tl-filter {
      padding: 5px 12px; border-radius: 99px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--if-text-secondary, #aaa);
      font-size: 12px; font-weight: 600;
      cursor: pointer; transition: all 0.18s;
    }
    .tl-filter:hover { background: rgba(255,255,255,0.09); }
    .tl-filter.active {
      background: rgba(99,102,241,0.18);
      border-color: rgba(99,102,241,0.4);
      color: #a5b4fc;
    }
    .count {
      display: inline-block;
      background: rgba(255,255,255,0.08);
      border-radius: 99px;
      padding: 0 6px;
      font-size: 10px;
      margin-left: 4px;
    }

    /* Carga / vacío */
    .tl-loading, .tl-empty {
      text-align: center; padding: 60px 20px;
      color: var(--if-text-secondary, #888);
    }
    .tl-loading { display: flex; flex-direction: column; align-items: center; gap: 16px; }
    .tl-spinner {
      width: 36px; height: 36px;
      border: 3px solid rgba(255,255,255,0.08);
      border-top-color: #6366f1;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .ei { font-size: 48px; margin-bottom: 12px; }
    .tl-empty h2 { font-size: 18px; margin: 0 0 8px; font-weight: 700; color: var(--if-text-primary, #ccc); }

    /* Grupos de mes */
    .tl-track { position: relative; }
    .tl-month-group { margin-bottom: 32px; }
    .month-label {
      font-size: 11px; font-weight: 800;
      letter-spacing: 0.1em; text-transform: uppercase;
      color: var(--if-text-secondary, #666);
      margin-bottom: 16px;
      padding-left: 44px;
    }

    /* Evento individual */
    .tl-event {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
      position: relative;
    }
    .tl-event::before {
      content: '';
      position: absolute;
      left: 18px; top: 40px;
      bottom: -16px;
      width: 2px;
      background: rgba(255,255,255,0.06);
    }
    .tl-event:last-child::before { display: none; }

    /* Punto del timeline */
    .tl-dot {
      width: 36px; height: 36px; flex-shrink: 0;
      border-radius: 50%;
      background: rgba(255,255,255,0.05);
      border: 2px solid var(--accent, #6366f1);
      display: flex; align-items: center; justify-content: center;
      margin-top: 2px;
      box-shadow: 0 0 10px color-mix(in srgb, var(--accent, #6366f1) 30%, transparent);
    }
    .dot-icon { font-size: 15px; }

    /* Tarjeta */
    .tl-card {
      flex: 1;
      background: var(--if-surface, rgba(255,255,255,0.04));
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 3px solid var(--accent, #6366f1);
      border-radius: 12px;
      padding: 14px 16px;
      transition: border-color 0.2s;
    }
    .tl-card:hover { border-color: var(--accent, #6366f1); background: rgba(255,255,255,0.06); }

    .tc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; gap: 8px; }
    .tc-badge {
      font-size: 10px; font-weight: 700;
      padding: 2px 8px; border-radius: 99px;
      letter-spacing: 0.06em; text-transform: uppercase;
    }
    .tc-date { font-size: 11px; color: var(--if-text-secondary, #666); white-space: nowrap; }

    .tc-title { font-size: 14px; font-weight: 700; margin-bottom: 6px; line-height: 1.4; }
    .tc-desc { font-size: 13px; color: var(--if-text-secondary, #999); line-height: 1.5; margin-bottom: 8px; }

    .tc-meta-row { display: flex; gap: 8px; flex-wrap: wrap; }
    .tc-actor {
      font-size: 11px; font-weight: 600;
      color: var(--if-text-secondary, #888);
      background: rgba(255,255,255,0.05);
      padding: 2px 8px; border-radius: 99px;
    }
    .tc-emotion {
      font-size: 11px; font-weight: 600;
      color: #f9a8d4;
      background: rgba(236,72,153,0.1);
      padding: 2px 8px; border-radius: 99px;
    }

    /* Error */
    .tl-error {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: 10px;
      padding: 14px 18px;
      font-size: 13px; color: #fca5a5;
    }
  `]
})
export class FamilyTimelineComponent implements OnInit {
  private readonly tlService  = inject(FamilyTimelineService);
  private readonly familyState = inject(FamilyStateService);

  readonly familyId   = this.familyState.currentFamilyId;
  readonly events     = signal<TimelineEvent[]>([]);
  readonly loading    = signal(false);
  readonly error      = signal<string | null>(null);
  readonly activeFilter = signal<EventType | null>(null);

  readonly filtered = computed(() => {
    const f = this.activeFilter();
    return f ? this.events().filter(e => e.type === f) : this.events();
  });

  readonly filterEntries = computed(() => {
    const counts = new Map<EventType, number>();
    for (const ev of this.events()) {
      counts.set(ev.type, (counts.get(ev.type) ?? 0) + 1);
    }
    return [...counts.entries()]
      .map(([type, count]) => ({ type, count, ...EVENT_CONFIG[type] }))
      .sort((a, b) => b.count - a.count);
  });

  readonly grouped = computed(() => {
    const map = new Map<string, TimelineEvent[]>();
    for (const ev of this.filtered()) {
      const label = this.monthLabel(ev.occurredAt);
      if (!map.has(label)) map.set(label, []);
      map.get(label)!.push(ev);
    }
    return [...map.entries()].map(([label, events]) => ({ label, events }));
  });

  ngOnInit(): void {
    const id = this.familyId();
    if (!id) return;
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.tlService.get(id).pipe(
      catchError(() => {
        this.error.set('No se pudo cargar la historia familiar.');
        return of([]);
      })
    ).subscribe(data => {
      this.events.set(data);
      this.loading.set(false);
    });
  }

  icon(type: EventType): string  { return EVENT_CONFIG[type]?.icon  ?? '●'; }
  color(type: EventType): string { return EVENT_CONFIG[type]?.color ?? '#6366f1'; }
  label(type: EventType): string { return EVENT_CONFIG[type]?.label ?? type; }

  colorFaint(type: EventType): string {
    const c = EVENT_CONFIG[type]?.color ?? '#6366f1';
    return c + '22';
  }

  truncate(text: string, max: number): string {
    return text.length > max ? text.slice(0, max) + '…' : text;
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('es-CO', {
        day: '2-digit', month: 'short', year: 'numeric'
      });
    } catch { return iso; }
  }

  monthLabel(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
    } catch { return '—'; }
  }
}
