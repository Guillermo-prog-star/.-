import {
  Component, OnInit, OnDestroy, inject, signal, computed, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LineageService } from './lineage.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import {
  Lineage, LineageMember, LineageMemberRequest, LineageRelationship,
  GenerationInfo, GenerationInfoRequest,
  GEN_META, getGenMeta, MemberStatus
} from './lineage.model';
import { catchError, of } from 'rxjs';

type TreeTab = 'tree' | 'history' | 'narrative' | 'legacy';
type MemberTab = 'bio' | 'evolution' | 'events' | 'connections';

const STATUS_ICON: Record<string, string> = {
  alive: '🌱', deceased: '🕊️', unknown: '❓', future: '⭐'
};
const STATUS_LABEL: Record<string, string> = {
  alive: 'Vivo', deceased: 'Fallecido', unknown: 'Desconocido', future: 'Futura generación'
};

/** Calcula rango de generaciones visibles dado el ancla */
function genRange(anchor: number, maxPast: number, maxFuture: number): number[] {
  const result: number[] = [];
  for (let g = maxPast; g <= maxFuture; g++) result.push(g);
  return result;
}

@Component({
  selector: 'app-lineage-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="lin-page">

  <!-- ── HEADER ──────────────────────────────────────── -->
  <div class="lin-header">
    <div class="lh-icon">🌳</div>
    <div class="lh-text">
      <div class="lh-title">{{ lineage()?.title || 'Árbol de Evolución y Legado' }}</div>
      <div class="lh-sub">
        {{ lineage()?.lineageCode || '' }}
        @if (lineage()) {
          · {{ memberCount() }} miembros
          · {{ visibleGenCount() }} generaciones
          · Ancla: <strong>{{ anchorLabel() }}</strong>
        }
      </div>
    </div>
    <div class="header-actions">
      @if (lineage()) {
        <button class="btn-outline" (click)="openEditLineage()">⚙️ Linaje</button>
        <button class="btn-outline" (click)="openGenInfo()">📋 Contexto</button>
        <button class="btn-gold" (click)="openAddMember()">+ Miembro</button>
      }
    </div>
  </div>

  <!-- ── TOAST ──────────────────────────────────────────── -->
  @if (toast()) {
    <div class="toast" [class.toast-error]="toastError()">{{ toast() }}</div>
  }

  <!-- ── TABS ────────────────────────────────────────── -->
  <div class="tabs">
    <button class="tab" [class.active]="activeTab() === 'tree'"      (click)="activeTab.set('tree')">🌳 Árbol</button>
    <button class="tab" [class.active]="activeTab() === 'history'"   (click)="activeTab.set('history')">📅 Historia</button>
    <button class="tab" [class.active]="activeTab() === 'narrative'" (click)="activeTab.set('narrative')">✍️ Narrativa</button>
    <button class="tab" [class.active]="activeTab() === 'legacy'"    (click)="activeTab.set('legacy')">🏛️ Legado</button>
  </div>

  <!-- ── LOADING / EMPTY ──────────────────────────────── -->
  @if (loading()) {
    <div class="state-center"><div class="spinner"></div><p>Cargando linaje...</p></div>
  } @else if (error()) {
    <div class="state-center error-box">
      <p>{{ error() }}</p>
      @if (noLineage()) {
        <div class="init-form">
          <p class="init-hint">¿Desde qué generación arrancas?</p>
          <div class="gen-anchor-select">
            @for (g of initGenOptions; track g.value) {
              <button class="ga-btn" [class.selected]="initAnchor === g.value"
                (click)="initAnchor = g.value">
                <span class="ga-icon">{{ g.icon }}</span>
                <span class="ga-label">{{ g.label }}</span>
                <span class="ga-desc">{{ g.desc }}</span>
              </button>
            }
          </div>
          <button class="btn-gold mt-16" (click)="createLineage()" [disabled]="saving()">
            {{ saving() ? 'Creando...' : 'Iniciar Árbol de Linaje' }}
          </button>
        </div>
      }
    </div>

  } @else {

    <!-- ── TAB: ÁRBOL ──────────────────────────────────── -->
    @if (activeTab() === 'tree') {
      <div class="tree-layout">

        <!-- Zoom toolbar -->
        <div class="zoom-bar">
          <button class="zb-btn" (click)="zoomOut()" title="Alejar (−)">−</button>
          <span class="zb-pct">{{ zoomPct() }}%</span>
          <button class="zb-btn" (click)="zoomIn()" title="Acercar (+)">+</button>
          <button class="zb-btn zb-reset" (click)="resetView()" title="Restablecer vista">⊞</button>
          <span class="zb-hint">Alt+arrastrar · rueda = zoom</span>
        </div>

        <!-- Canvas SVG -->
        <div class="tree-canvas-wrap" (click)="clearSelection()">
          <svg class="tree-svg" [attr.viewBox]="svgViewBox()" preserveAspectRatio="xMidYMid meet"
            style="touch-action:none"
            (wheel)="onWheel($event)"
            (mousedown)="onSvgMouseDown($event)"
            (mousemove)="onDragMove($event)" (mouseup)="endDrag($event)" (mouseleave)="endDrag($event)">
            <defs>
              <filter id="lin-glow" x="-60%" y="-60%" width="220%" height="220%">
                <feGaussianBlur stdDeviation="6"  result="b1"/>
                <feGaussianBlur stdDeviation="14" result="b2"/>
                <feMerge><feMergeNode in="b2"/><feMergeNode in="b1"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
              <linearGradient id="lin-trunk-g" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%"   stop-color="#fcd34d" stop-opacity=".5"/>
                <stop offset="35%"  stop-color="#f59e0b" stop-opacity="1"/>
                <stop offset="70%"  stop-color="#d97706" stop-opacity="1"/>
                <stop offset="100%" stop-color="#92400e" stop-opacity=".9"/>
              </linearGradient>
              <radialGradient id="lin-root-g" cx="50%" cy="50%" r="50%">
                <stop offset="0%"   stop-color="#fbbf24" stop-opacity=".3"/>
                <stop offset="100%" stop-color="#fbbf24" stop-opacity="0"/>
              </radialGradient>
            </defs>

            <!-- Generation bands (horizontal) -->
            @for (band of genBands(); track band.gen) {
              <rect [attr.x]="0" [attr.y]="band.y - band.h/2" [attr.width]="svgW()"
                [attr.height]="band.h" [attr.fill]="band.color" opacity=".04"/>
              <!-- generation label left -->
              <text x="12" [attr.y]="band.y + 5" font-size="10" [attr.fill]="band.color"
                opacity=".6" font-weight="600">{{ band.label }}</text>
              <!-- anchor glow ring -->
              @if (band.isAnchor) {
                <ellipse [attr.cx]="svgW()/2" [attr.cy]="band.y" [attr.rx]="svgW()*0.4" ry="38"
                  fill="url(#lin-root-g)" opacity=".5"/>
              }
            }

            <!-- Relationship branches -->
            @for (path of branchPaths(); track path.id) {
              <path [attr.d]="path.d" stroke="#fbbf24" stroke-width="28" fill="none" opacity=".05"/>
              <path [attr.d]="path.d" stroke="url(#lin-trunk-g)"
                [attr.stroke-width]="path.isCouple ? 1.5 : 3.5" fill="none"
                filter="url(#lin-glow)" opacity=".9" stroke-linecap="round"
                [attr.stroke-dasharray]="path.isCouple ? '6 4' : 'none'"/>
            }

            <!-- Member nodes -->
            @for (m of positionedMembers(); track m.id) {
              <g class="member-node" [class.selected]="selectedId() === m.id"
                 [class.dragging]="dragId() === m.id"
                 (mousedown)="startDrag(m, $event)"
                 (click)="selectMember(m, $event)"
                 style="cursor: grab">
                <!-- anchor glow -->
                @if (m.isAnchor) {
                  <circle [attr.cx]="m.px" [attr.cy]="m.py"
                    [attr.r]="nodeRadius(m.generation) + 12"
                    fill="none" stroke="#fbbf24" stroke-width="1.5" opacity=".4"
                    filter="url(#lin-glow)"/>
                }
                <circle [attr.cx]="m.px" [attr.cy]="m.py"
                  [attr.r]="nodeRadius(m.generation)"
                  [attr.fill]="nodeFill(m)"
                  [attr.stroke]="nodeStroke(m.generation)"
                  [attr.stroke-width]="selectedId() === m.id ? 3 : (m.isAnchor ? 2.5 : 1.8)"
                  filter="url(#lin-glow)" opacity=".95"/>
                <text [attr.x]="m.px" [attr.y]="m.py + 1"
                  text-anchor="middle" dominant-baseline="middle"
                  [attr.font-size]="nodeRadius(m.generation) * 0.62"
                  fill="#fffbeb" font-weight="600" style="pointer-events:none">
                  {{ m.avatarInitials || initials(m) }}
                </text>
                <!-- deceased overlay -->
                @if (m.status === 'deceased') {
                  <text [attr.x]="m.px + nodeRadius(m.generation)*0.55"
                    [attr.y]="m.py - nodeRadius(m.generation)*0.55"
                    font-size="11" style="pointer-events:none">🕊️</text>
                }
                <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + 14"
                  text-anchor="middle" font-size="11" fill="#fde68a" style="pointer-events:none">
                  {{ shortName(m) }}
                </text>
                @if (m.roleLabel) {
                  <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + 26"
                    text-anchor="middle" font-size="9" [attr.fill]="nodeStroke(m.generation)"
                    opacity=".8" style="pointer-events:none">
                    {{ m.roleLabel }}
                  </text>
                }
              </g>
            }
          </svg>
        </div>

        <!-- Side panel -->
        <div class="side-panel" [class.open]="!!selectedMember()">
          @if (selectedMember(); as m) {
            <div class="sp-header" [style.border-left]="'4px solid ' + nodeStroke(m.generation)">
              <div class="sp-avatar" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                {{ m.avatarInitials || initials(m) }}
              </div>
              <div>
                <div class="sp-name">{{ m.fullName }}</div>
                <div class="sp-gen-badge" [style.color]="nodeStroke(m.generation)">
                  {{ getGenMeta(m.generation).label }}
                  @if (m.isAnchor) { <span class="anchor-tag">⚓ Ancla</span> }
                </div>
                <div class="sp-status">{{ statusIcon(m.status) }} {{ statusLabel(m.status) }}</div>
              </div>
            </div>

            <!-- Member sub-tabs -->
            <div class="sp-tabs">
              <button class="sp-tab" [class.active]="memberTab() === 'bio'"
                (click)="memberTab.set('bio')">Datos</button>
              <button class="sp-tab" [class.active]="memberTab() === 'evolution'"
                (click)="memberTab.set('evolution')">Evolución</button>
              <button class="sp-tab" [class.active]="memberTab() === 'events'"
                (click)="memberTab.set('events')">Eventos</button>
              <button class="sp-tab" [class.active]="memberTab() === 'connections'"
                (click)="memberTab.set('connections')">
                Conexiones
                @if (memberConnections(m).length) {
                  <span class="sp-tab-badge">{{ memberConnections(m).length }}</span>
                }
              </button>
            </div>

            <div class="sp-body">

              <!-- BIO -->
              @if (memberTab() === 'bio') {
                @if (m.origin) {
                  <div class="sp-row"><span class="sp-lbl">Origen</span><span>{{ m.origin }}</span></div>
                }
                @if (m.calculatedAge) {
                  <div class="sp-row"><span class="sp-lbl">Edad</span><span>{{ m.calculatedAge }}</span></div>
                }
                @if (m.birthYear) {
                  <div class="sp-row">
                    <span class="sp-lbl">Nacimiento</span>
                    <span>{{ m.birthYearApproximate ? '~' : '' }}{{ m.birthYear }}</span>
                  </div>
                }
                @if (m.deathYear) {
                  <div class="sp-row"><span class="sp-lbl">Fallecimiento</span><span>{{ m.deathYear }}</span></div>
                }
                <div class="sp-row conf-row">
                  <span class="sp-lbl">Certeza</span>
                  <div class="conf-bar-wrap">
                    <div class="conf-bar" [style.width.%]="m.confidenceLevel"
                      [style.background]="'linear-gradient(90deg,' + nodeStroke(m.generation) + '80,' + nodeStroke(m.generation) + ')'">
                    </div>
                  </div>
                  <span class="conf-pct">{{ m.confidenceLevel }}%</span>
                </div>
                @if (m.story) {
                  <div class="sp-field-title">Historia personal</div>
                  <p class="sp-text">{{ m.story }}</p>
                }
              }

              <!-- EVOLUCIÓN -->
              @if (memberTab() === 'evolution') {
                @if (m.valores) {
                  <div class="ev-block founding">
                    <div class="ev-icon">💎</div>
                    <div><div class="ev-title">Valores</div><p>{{ m.valores }}</p></div>
                  </div>
                }
                @if (m.aprendizajes) {
                  <div class="ev-block builder">
                    <div class="ev-icon">📚</div>
                    <div><div class="ev-title">Aprendizajes</div><p>{{ m.aprendizajes }}</p></div>
                  </div>
                }
                @if (m.erroresSuperados) {
                  <div class="ev-block responsible">
                    <div class="ev-icon">🔥</div>
                    <div><div class="ev-title">Errores superados</div><p>{{ m.erroresSuperados }}</p></div>
                  </div>
                }
                @if (m.tradiciones) {
                  <div class="ev-block current">
                    <div class="ev-icon">🎋</div>
                    <div><div class="ev-title">Tradiciones</div><p>{{ m.tradiciones }}</p></div>
                  </div>
                }
                @if (m.misionesCumplidas) {
                  <div class="ev-block future">
                    <div class="ev-icon">🎯</div>
                    <div><div class="ev-title">Misiones cumplidas</div><p>{{ m.misionesCumplidas }}</p></div>
                  </div>
                }
                @if (m.legadoPersonal) {
                  <div class="ev-block projected">
                    <div class="ev-icon">🏛️</div>
                    <div><div class="ev-title">Legado personal</div><p>{{ m.legadoPersonal }}</p></div>
                  </div>
                }
                @if (!m.valores && !m.aprendizajes && !m.erroresSuperados && !m.tradiciones && !m.misionesCumplidas && !m.legadoPersonal) {
                  <p class="empty-msg">Sin datos de evolución aún. Edita este miembro para agregar.</p>
                }
              }

              <!-- CONEXIONES -->
              @if (memberTab() === 'connections') {
                @if (memberConnections(m).length === 0) {
                  <p class="empty-msg">Sin conexiones. Usa "🔗 Conectar" para agregar.</p>
                } @else {
                  @for (conn of memberConnections(m); track conn.rel.id) {
                    <div class="sp-conn-row">
                      <div class="sp-conn-avatar"
                        [style.background]="conn.other ? nodeStroke(conn.other.generation) : '#374151'">
                        {{ conn.other ? (conn.other.avatarInitials || initials(conn.other)) : '?' }}
                      </div>
                      <div class="sp-conn-info">
                        <div class="sp-conn-name">{{ conn.other?.fullName || 'Miembro desconocido' }}</div>
                        <div class="sp-conn-type">
                          {{ conn.rel.isCouple ? '💑 Pareja' :
                             conn.rel.fromMemberId === m.id ? '→ Padre/Madre de' : '← Hijo/a de' }}
                          · <span class="sp-conn-rel-type">{{ conn.rel.relationshipType }}</span>
                        </div>
                      </div>
                      <button class="btn-danger-sm"
                        (click)="deleteRelationship(conn.rel.id)"
                        title="Eliminar conexión">✕</button>
                    </div>
                  }
                }
              }

              <!-- EVENTOS -->
              @if (memberTab() === 'events') {
                <!-- Lista de eventos existentes -->
                @if (m.events.length) {
                  @for (ev of m.events; track ev.id) {
                    <div class="sp-event">
                      <span class="ev-year">{{ ev.isApproximate ? '~' : '' }}{{ ev.eventYear || '?' }}</span>
                      <div class="sp-ev-body">
                        <div class="ev-title-sm">{{ ev.title }}</div>
                        @if (ev.description) { <p class="ev-desc">{{ ev.description }}</p> }
                      </div>
                    </div>
                  }
                } @else {
                  <p class="empty-msg-sm">Sin eventos registrados.</p>
                }

                <!-- Formulario inline: agregar evento rápido -->
                @if (addingEvent()) {
                  <div class="quick-event-form">
                    <div class="qef-row">
                      <input class="qef-input qef-year" type="number"
                        [(ngModel)]="quickEvent.eventYear"
                        placeholder="Año" maxlength="4"/>
                      <input class="qef-input qef-title"
                        [(ngModel)]="quickEvent.title"
                        placeholder="Título del hito *" />
                    </div>
                    <textarea class="qef-textarea"
                      [(ngModel)]="quickEvent.description"
                      rows="2" placeholder="Descripción opcional..."></textarea>
                    <div class="qef-actions">
                      <button class="btn-ghost-sm" (click)="addingEvent.set(false)">Cancelar</button>
                      <button class="btn-gold-sm" (click)="saveQuickEvent(m)"
                        [disabled]="!quickEvent.title || saving()">
                        {{ saving() ? '...' : '+ Guardar evento' }}
                      </button>
                    </div>
                  </div>
                } @else {
                  <button class="btn-add-event" (click)="addingEvent.set(true)">
                    + Agregar hito de vida
                  </button>
                }
              }
            </div>

            <div class="sp-actions">
              <button class="btn-ghost-sm" (click)="openEditMember(m)">✏️ Editar</button>
              <button class="btn-ghost-sm" (click)="openConnectModal(m)">🔗 Conectar</button>
              @if (m.positionX || m.positionY) {
                <button class="btn-ghost-sm" (click)="resetPosition(m)" title="Volver al auto-layout">📐</button>
              }
              <button class="btn-danger-sm" (click)="confirmDelete(m)">🗑️</button>
            </div>
          }
        </div>

      </div><!-- /tree-layout -->

      <!-- generation legend -->
      <div class="gen-legend">
        @for (band of genBands(); track band.gen) {
          <div class="gl-item" (click)="filterByGen(band.gen)"
            [class.active]="genFilter() === band.gen">
            <div class="gl-dot" [style.background]="band.color"></div>
            <span class="gl-gen">{{ band.label }}</span>
            <span class="gl-count">{{ band.count }}</span>
            @if (band.isAnchor) { <span class="gl-anchor">⚓</span> }
          </div>
        }
        @if (genFilter() !== null) {
          <button class="gl-clear" (click)="genFilter.set(null)">✕ Todos</button>
        }
      </div>

    }<!-- /tree tab -->

    <!-- ── TAB: HISTORIA ───────────────────────────────── -->
    @if (activeTab() === 'history') {
      <div class="content-tab">
        <div class="ct-header">
          <h3>Línea de Tiempo del Linaje</h3>
          <span class="ct-sub">{{ allEvents().length }} eventos · {{ lineage()?.foundingYear ? 'Desde ' + lineage()!.foundingYear : '' }}</span>
        </div>
        <!-- Generation context blocks -->
        @for (info of lineage()?.generationInfos || []; track info.id) {
          @if (info.summary || info.keyChallenge || info.keyAchievement) {
            <div class="gen-context-block" [style.border-left-color]="getGenMeta(info.generationLevel).color">
              <div class="gcb-header">
                <span class="gcb-label" [style.color]="getGenMeta(info.generationLevel).color">
                  {{ info.title || getGenMeta(info.generationLevel).label }}
                </span>
                @if (info.periodStart || info.periodEnd) {
                  <span class="gcb-period">{{ info.periodStart }}–{{ info.periodEnd }}</span>
                }
              </div>
              @if (info.summary)        { <p class="gcb-text">{{ info.summary }}</p> }
              @if (info.keyChallenge)   { <div class="gcb-kv"><span>⚡ Desafío:</span> {{ info.keyChallenge }}</div> }
              @if (info.keyAchievement) { <div class="gcb-kv"><span>🏆 Logro:</span> {{ info.keyAchievement }}</div> }
            </div>
          }
        }
        @if (allEvents().length === 0) {
          <p class="empty-msg">No hay eventos registrados aún.</p>
        } @else {
          <div class="timeline">
            @for (ev of allEvents(); track ev.event.id) {
              <div class="tl-item">
                <div class="tl-year">{{ ev.event.isApproximate ? '~' : '' }}{{ ev.event.eventYear || '?' }}</div>
                <div class="tl-dot" [style.background]="ev.memberColor" [style.box-shadow]="'0 0 8px ' + ev.memberColor + '80'"></div>
                <div class="tl-body">
                  <div class="tl-member" [style.color]="ev.memberColor">{{ ev.memberName }}</div>
                  <div class="tl-title">{{ ev.event.title }}</div>
                  @if (ev.event.description) { <p class="tl-desc">{{ ev.event.description }}</p> }
                </div>
              </div>
            }
          </div>
        }
      </div>
    }

    <!-- ── TAB: NARRATIVA ──────────────────────────────── -->
    @if (activeTab() === 'narrative') {
      <div class="content-tab">
        <h3>Narrativa del Linaje</h3>
        @if (lineage()?.visionStatement) {
          <div class="vision-block">
            <span class="vision-icon">🔭</span>
            <p>{{ lineage()!.visionStatement }}</p>
          </div>
        }
        <!-- by generation descending (past first) -->
        @for (genGroup of membersByGeneration(); track genGroup.gen) {
          @if (genHasNarrative(genGroup.members)) {
            <div class="gen-narrative-section">
              <div class="gns-title" [style.color]="getGenMeta(genGroup.gen).color">
                {{ getGenMeta(genGroup.gen).label }}
              </div>
              @for (m of genGroup.members; track m.id) {
                @if (m.story || m.valores || m.aprendizajes || m.legadoPersonal) {
                  <div class="narrative-card">
                    <div class="nc-header">
                      <div class="nc-avatar" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                        {{ m.avatarInitials || initials(m) }}
                      </div>
                      <div>
                        <div class="nc-name">{{ m.fullName }}</div>
                        <div class="nc-role">{{ m.roleLabel || '' }}</div>
                      </div>
                    </div>
                    @if (m.story)         { <p class="nc-story italic">{{ m.story }}</p> }
                    @if (m.valores)       { <p class="nc-field"><span>💎 Valores:</span> {{ m.valores }}</p> }
                    @if (m.aprendizajes)  { <p class="nc-field"><span>📚 Aprendizajes:</span> {{ m.aprendizajes }}</p> }
                    @if (m.legadoPersonal){ <p class="nc-field"><span>🏛️ Legado:</span> {{ m.legadoPersonal }}</p> }
                  </div>
                }
              }
            </div>
          }
        }
        @if (!hasMembersWithNarrative()) {
          <p class="empty-msg">Agrega historias al editar cada miembro del árbol.</p>
        }
      </div>
    }

    <!-- ── TAB: LEGADO ─────────────────────────────────── -->
    @if (activeTab() === 'legacy') {
      <div class="content-tab">
        <div class="legacy-banner">
          <div class="lb-title">Árbol de Evolución y Legado Familiar</div>
          <div class="lb-sub">Historias que inspiran · Lecciones que trascienden · Decisiones que transforman</div>
        </div>

        <div class="legacy-stats">
          @for (s of legacyStats(); track s.label) {
            <div class="ls-card">
              <div class="ls-num">{{ s.value }}</div>
              <div class="ls-lbl">{{ s.label }}</div>
            </div>
          }
        </div>

        <!-- Generation columns -->
        <div class="legacy-columns">
          @for (genGroup of membersByGeneration(); track genGroup.gen) {
            <div class="lc-col">
              <div class="lc-header" [style.border-color]="getGenMeta(genGroup.gen).color"
                [style.color]="getGenMeta(genGroup.gen).color">
                <div class="lc-label">{{ getGenMeta(genGroup.gen).label }}</div>
                <div class="lc-desc">{{ getGenMeta(genGroup.gen).desc }}</div>
              </div>
              @for (m of genGroup.members; track m.id) {
                <div class="lc-member" [style.border-color]="getGenMeta(genGroup.gen).color + '40'">
                  <div class="lc-avatar" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                    {{ m.avatarInitials || initials(m) }}
                  </div>
                  <div>
                    <div class="lc-name">{{ m.fullName }}</div>
                    <div class="lc-status">{{ statusIcon(m.status) }} {{ statusLabel(m.status) }}</div>
                    @if (m.legadoPersonal) {
                      <p class="lc-legacy">{{ m.legadoPersonal }}</p>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      </div>
    }

  }<!-- /else has data -->

  <!-- ── MODAL: ADD/EDIT MEMBER ────────────────────────── -->
  @if (showModal()) {
    <div class="modal-overlay" (click)="closeModal()">
      <div class="modal modal-wide" (click)="$event.stopPropagation()">
        <div class="modal-title">
          {{ editingMember() ? 'Editar: ' + editingMember()!.fullName : 'Agregar Miembro al Linaje' }}
        </div>

        <!-- Modal form tabs -->
        <div class="modal-tabs">
          <button [class.active]="formTab === 'bio'"       (click)="formTab = 'bio'">Datos biográficos</button>
          <button [class.active]="formTab === 'evolution'" (click)="formTab = 'evolution'">Evolución y Legado</button>
        </div>

        @if (formTab === 'bio') {
          <div class="form-grid">
            <div class="fg-row">
              <label>Nombre</label>
              <input [(ngModel)]="form.firstName" placeholder="Nombre"/>
            </div>
            <div class="fg-row">
              <label>Apellido</label>
              <input [(ngModel)]="form.lastName" placeholder="Apellido"/>
            </div>
            <div class="fg-row">
              <label>Generación (relativa al ancla)</label>
              <select [(ngModel)]="form.generation">
                <option [value]="-3">-3 · Tatarabuelos</option>
                <option [value]="-2">-2 · Ancestros Fundadores</option>
                <option [value]="-1">-1 · Generación Constructora</option>
                <option [value]="0"> 0 · Generación Responsable</option>
                <option [value]="1">+1 · Generación Actual</option>
                <option [value]="2">+2 · Generación Futura</option>
                <option [value]="3">+3 · Proyección</option>
              </select>
            </div>
            <div class="fg-row">
              <label>Estado</label>
              <select [(ngModel)]="form.status">
                <option value="alive">🌱 Vivo</option>
                <option value="deceased">🕊️ Fallecido</option>
                <option value="unknown">❓ Desconocido</option>
                <option value="future">⭐ Futura generación</option>
              </select>
            </div>
            <div class="fg-row">
              <label>Año nacimiento</label>
              <input type="number" [(ngModel)]="form.birthYear" placeholder="Ej: 1945"/>
            </div>
            <div class="fg-row">
              <label>Año fallecimiento</label>
              <input type="number" [(ngModel)]="form.deathYear" placeholder="Si aplica"/>
            </div>
            <div class="fg-row">
              <label>Origen / Lugar</label>
              <input [(ngModel)]="form.origin" placeholder="Ciudad, País"/>
            </div>
            <div class="fg-row">
              <label>Rol en la familia</label>
              <input [(ngModel)]="form.roleLabel" placeholder="Patriarca, Matriarca, Hijo..."/>
            </div>
            <div class="fg-row fg-full">
              <label>Historia personal</label>
              <textarea [(ngModel)]="form.story" rows="3" placeholder="Narración libre de su vida..."></textarea>
            </div>
            <div class="fg-row">
              <label>Certeza de datos: {{ form.confidenceLevel }}%</label>
              <input type="range" min="0" max="100" [(ngModel)]="form.confidenceLevel"/>
            </div>
            <div class="fg-row">
              <label>Fuente de datos</label>
              <input [(ngModel)]="form.dataSource" placeholder="Acta civil, testimonio, fotografía..."/>
            </div>
            <div class="fg-row">
              <label class="check-label">
                <input type="checkbox" [(ngModel)]="form.isAnchor"/>
                Es el nodo ancla (punto de partida del árbol)
              </label>
            </div>
          </div>
        }

        @if (formTab === 'evolution') {
          <div class="form-grid">
            <div class="fg-row fg-full ev-field founding">
              <label>💎 Valores que aportó o heredó</label>
              <textarea [(ngModel)]="form.valores" rows="2"
                placeholder="Ej: Honestidad, trabajo, fe, resiliencia familiar..."></textarea>
            </div>
            <div class="fg-row fg-full ev-field builder">
              <label>📚 Aprendizajes clave de vida</label>
              <textarea [(ngModel)]="form.aprendizajes" rows="2"
                placeholder="Ej: El valor del ahorro, la importancia de la educación..."></textarea>
            </div>
            <div class="fg-row fg-full ev-field responsible">
              <label>🔥 Errores o traumas superados</label>
              <textarea [(ngModel)]="form.erroresSuperados" rows="2"
                placeholder="Ej: Emigración forzada, crisis económica superada..."></textarea>
            </div>
            <div class="fg-row fg-full ev-field current">
              <label>🎋 Tradiciones que inició o preservó</label>
              <textarea [(ngModel)]="form.tradiciones" rows="2"
                placeholder="Ej: La reunión de diciembre, la receta familiar, el rezo de noches..."></textarea>
            </div>
            <div class="fg-row fg-full ev-field future">
              <label>🎯 Misiones cumplidas</label>
              <textarea [(ngModel)]="form.misionesCumplidas" rows="2"
                placeholder="Ej: Sacó a la familia adelante, fundó el negocio familiar..."></textarea>
            </div>
            <div class="fg-row fg-full ev-field projected">
              <label>🏛️ Legado personal</label>
              <textarea [(ngModel)]="form.legadoPersonal" rows="2"
                placeholder="¿Qué dejó o dejará esta persona para las generaciones futuras?"></textarea>
            </div>
          </div>
        }

        <div class="modal-actions">
          <button class="btn-ghost" (click)="closeModal()">Cancelar</button>
          <button class="btn-gold" (click)="saveMember()" [disabled]="saving()">
            {{ saving() ? 'Guardando...' : (editingMember() ? 'Actualizar' : 'Agregar') }}
          </button>
        </div>
      </div>
    </div>
  }

  <!-- ── MODAL: EDITAR LINAJE ─────────────────────────── -->
  @if (showEditLineageModal()) {
    <div class="modal-overlay" (click)="showEditLineageModal.set(false)">
      <div class="modal modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-title">⚙️ Configuración del Linaje</div>
        <div class="form-grid" style="grid-template-columns:1fr;">
          <div class="fg-row">
            <label>Título del árbol</label>
            <input [(ngModel)]="lineageForm.title" placeholder="Árbol de Evolución · Familia..."/>
          </div>
          <div class="fg-row">
            <label>Visión del legado familiar</label>
            <textarea [(ngModel)]="lineageForm.visionStatement" rows="3"
              placeholder="Que las decisiones de hoy sean el legado de mañana..."></textarea>
          </div>
          <div class="fg-row">
            <label>Año de fundación aproximado</label>
            <input [(ngModel)]="lineageForm.foundingYear" placeholder="~1880"/>
          </div>
          <div class="fg-row">
            <label>Generaciones pasadas visibles (ej: -2 = bisabuelos, -3 = tatarabuelos)</label>
            <input type="number" [(ngModel)]="lineageForm.maxPastGen" min="-3" max="0"/>
          </div>
          <div class="fg-row">
            <label>Generaciones futuras visibles (ej: +2 = nietos, +3 = bisnietos)</label>
            <input type="number" [(ngModel)]="lineageForm.maxFutureGen" min="0" max="3"/>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-ghost" (click)="showEditLineageModal.set(false)">Cancelar</button>
          <button class="btn-gold" (click)="saveLineage()" [disabled]="saving()">
            {{ saving() ? 'Guardando...' : 'Guardar cambios' }}
          </button>
        </div>
      </div>
    </div>
  }

  <!-- ── MODAL: GEN INFO ──────────────────────────────── -->
  @if (showGenInfoModal()) {
    <div class="modal-overlay" (click)="showGenInfoModal.set(false)">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-title">📋 Contexto por Generación</div>
        <div class="fg-row">
          <label>Generación</label>
          <select [(ngModel)]="genInfoForm.generationLevel">
            <option [value]="-3">-3 · Tatarabuelos</option>
            <option [value]="-2">-2 · Ancestros Fundadores</option>
            <option [value]="-1">-1 · Generación Constructora</option>
            <option [value]="0"> 0 · Generación Responsable</option>
            <option [value]="1">+1 · Generación Actual</option>
            <option [value]="2">+2 · Generación Futura</option>
            <option [value]="3">+3 · Proyección</option>
          </select>
        </div>
        <div class="form-grid mt-12">
          <div class="fg-row">
            <label>Título de la generación</label>
            <input [(ngModel)]="genInfoForm.title" placeholder="Ej: Los fundadores migrantes"/>
          </div>
          <div class="fg-row">
            <label>Período</label>
            <div class="period-inputs">
              <input [(ngModel)]="genInfoForm.periodStart" placeholder="Desde (año)"/>
              <span>—</span>
              <input [(ngModel)]="genInfoForm.periodEnd" placeholder="Hasta (año)"/>
            </div>
          </div>
          <div class="fg-row fg-full">
            <label>Resumen del contexto</label>
            <textarea [(ngModel)]="genInfoForm.summary" rows="2"
              placeholder="¿En qué mundo vivió esta generación?"></textarea>
          </div>
          <div class="fg-row fg-full">
            <label>⚡ Principal desafío</label>
            <textarea [(ngModel)]="genInfoForm.keyChallenge" rows="2"
              placeholder="¿Cuál fue su mayor reto?"></textarea>
          </div>
          <div class="fg-row fg-full">
            <label>🏆 Principal logro</label>
            <textarea [(ngModel)]="genInfoForm.keyAchievement" rows="2"
              placeholder="¿Qué construyeron o lograron?"></textarea>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-ghost" (click)="showGenInfoModal.set(false)">Cancelar</button>
          <button class="btn-gold" (click)="saveGenInfo()" [disabled]="saving()">
            {{ saving() ? 'Guardando...' : 'Guardar contexto' }}
          </button>
        </div>
      </div>
    </div>
  }

  <!-- ── MODAL: CONECTAR MIEMBRO ─────────────────────── -->
  @if (showConnectModal()) {
    <div class="modal-overlay" (click)="showConnectModal.set(false)">
      <div class="modal modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-title">🔗 Conectar con otro miembro</div>
        <p class="connect-source">
          Origen: <strong [style.color]="nodeStroke(connectSource()?.generation ?? 0)">
            {{ connectSource()?.fullName }}
          </strong>
        </p>
        <div class="form-grid" style="grid-template-columns:1fr;">
          <div class="fg-row">
            <label>Destino (miembro a conectar)</label>
            <select [(ngModel)]="connectTargetId">
              <option [value]="null" disabled>— Selecciona un miembro —</option>
              @for (m of connectableMembers(); track m.id) {
                <option [value]="m.id">
                  Gen {{ m.generation >= 0 ? '+' + m.generation : m.generation }}
                  · {{ m.fullName }} ({{ m.roleLabel || m.generationType }})
                </option>
              }
            </select>
          </div>
          <div class="fg-row">
            <label>Tipo de relación</label>
            <select [(ngModel)]="connectRelType">
              <option value="biological">🧬 Biológica (padre/madre → hijo/a)</option>
              <option value="couple">💑 Pareja / Cónyuge</option>
              <option value="adoptive">🤝 Adoptiva</option>
              <option value="step">↗️ Madrastra / Padrastro</option>
            </select>
          </div>
          <div class="fg-row">
            <label class="check-label">
              <input type="checkbox" [(ngModel)]="connectIsCouple"/>
              Es una relación de pareja (línea punteada en el árbol)
            </label>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-ghost" (click)="showConnectModal.set(false)">Cancelar</button>
          <button class="btn-gold" (click)="saveConnection()" [disabled]="saving() || !connectTargetId">
            {{ saving() ? 'Conectando...' : 'Crear conexión' }}
          </button>
        </div>
      </div>
    </div>
  }

  <!-- ── CONFIRM DELETE ──────────────────────────────── -->
  @if (deleteTarget()) {
    <div class="modal-overlay" (click)="deleteTarget.set(null)">
      <div class="modal modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-title">¿Eliminar miembro?</div>
        <p>¿Seguro que deseas eliminar a <strong>{{ deleteTarget()?.fullName }}</strong>?
          Se eliminarán también sus relaciones.</p>
        <div class="modal-actions">
          <button class="btn-ghost" (click)="deleteTarget.set(null)">Cancelar</button>
          <button class="btn-danger" (click)="deleteMember()" [disabled]="saving()">Eliminar</button>
        </div>
      </div>
    </div>
  }

</div><!-- /lin-page -->
  `,
  styles: [`
    :host { display: block; }

    /* ── PAGE ──────────────────────────────────────── */
    .lin-page {
      min-height: 100vh; background: #0d1117;
      color: #fde68a; font-family: 'Inter', sans-serif; padding-bottom: 60px;
    }

    /* ── HEADER ─────────────────────────────────────── */
    .lin-header {
      display: flex; align-items: center; gap: 14px;
      padding: 20px 28px 12px; border-bottom: 1px solid #1f2937;
    }
    .lh-icon { font-size: 32px; }
    .lh-text { flex: 1; }
    .lh-title { font-size: 22px; font-weight: 700; color: #fbbf24; }
    .lh-sub   { font-size: 12px; color: #9ca3af; margin-top: 3px; }
    .lh-sub strong { color: #d97706; }
    .header-actions { display: flex; gap: 8px; }

    /* ── TABS ───────────────────────────────────────── */
    .tabs { display: flex; gap: 4px; padding: 12px 28px 0; border-bottom: 1px solid #1f2937; }
    .tab {
      padding: 8px 16px; border: none; cursor: pointer;
      border-radius: 8px 8px 0 0; font-size: 13px; font-weight: 500;
      background: transparent; color: #9ca3af; transition: all .2s;
    }
    .tab:hover  { color: #fbbf24; background: #1f2937; }
    .tab.active { background: #1f2937; color: #fbbf24; border-bottom: 2px solid #fbbf24; }

    /* ── STATES ─────────────────────────────────────── */
    .state-center {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 16px; padding: 60px 20px; color: #9ca3af;
    }
    .error-box { color: #f87171; }
    .spinner {
      width: 40px; height: 40px; border: 3px solid #1f2937;
      border-top-color: #fbbf24; border-radius: 50%; animation: spin .8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Init form */
    .init-form { display: flex; flex-direction: column; align-items: center; gap: 16px; max-width: 600px; }
    .init-hint { color: #d97706; font-size: 15px; font-weight: 600; }
    .gen-anchor-select { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
    .ga-btn {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 14px 18px; border: 1px solid #374151; border-radius: 12px;
      cursor: pointer; background: #111827; color: #9ca3af; transition: all .2s;
      min-width: 110px;
    }
    .ga-btn:hover, .ga-btn.selected {
      border-color: #fbbf24; color: #fbbf24; background: #1f2937;
    }
    .ga-icon { font-size: 24px; }
    .ga-label { font-size: 12px; font-weight: 600; }
    .ga-desc  { font-size: 10px; opacity: .7; text-align: center; }
    .mt-16 { margin-top: 16px; }

    /* ── BUTTONS ─────────────────────────────────────── */
    .btn-gold {
      padding: 8px 18px; border: none; cursor: pointer;
      background: linear-gradient(135deg,#f59e0b,#d97706);
      color: #1a0a00; font-weight: 700; border-radius: 8px; font-size: 13px;
      transition: opacity .2s;
    }
    .btn-gold:hover    { opacity: .85; }
    .btn-gold:disabled { opacity: .5; cursor: not-allowed; }
    .btn-outline {
      padding: 7px 14px; border: 1px solid #374151; cursor: pointer;
      background: transparent; color: #9ca3af; border-radius: 8px; font-size: 12px;
      transition: all .2s;
    }
    .btn-outline:hover { border-color: #f59e0b; color: #f59e0b; }

    /* ── Toast ── */
    .toast {
      position: fixed; bottom: 28px; right: 28px; z-index: 9999;
      background: #1a3a1a; color: #86efac; border: 1px solid #16a34a;
      border-radius: 10px; padding: 12px 20px; font-size: 13px;
      box-shadow: 0 4px 20px #0007; animation: toastIn .25s ease;
    }
    .toast.toast-error { background: #3b1414; color: #fca5a5; border-color: #dc2626; }
    @keyframes toastIn { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }
    .btn-ghost {
      padding: 7px 16px; border: 1px solid #374151; cursor: pointer;
      background: transparent; color: #9ca3af; border-radius: 8px; font-size: 13px;
      transition: all .2s;
    }
    .btn-ghost:hover   { border-color: #fbbf24; color: #fbbf24; }
    .btn-ghost-sm      { padding: 5px 10px; font-size: 12px; border: 1px solid #374151;
      background: transparent; color: #9ca3af; border-radius: 6px; cursor: pointer; }
    .btn-ghost-sm:hover{ color: #fbbf24; border-color: #fbbf24; }
    .btn-danger {
      padding: 7px 16px; border: none; cursor: pointer;
      background: #7f1d1d; color: #fca5a5; border-radius: 8px; font-size: 13px;
    }
    .btn-danger:disabled { opacity: .5; }
    .btn-danger-sm { padding: 5px 10px; font-size: 16px; border: none;
      background: transparent; color: #ef4444; cursor: pointer; border-radius: 6px; }

    /* ── TREE LAYOUT ─────────────────────────────────── */
    .tree-layout {
      display: flex; height: calc(100vh - 165px); min-height: 500px; overflow: hidden;
    }
    .tree-canvas-wrap {
      flex: 1; overflow: auto; padding: 16px;
      background: radial-gradient(ellipse at 50% 50%, #140c00 0%, #0d1117 75%);
      cursor: default;
    }
    .tree-svg { display: block; width: 100%; height: auto; user-select: none; }
    .member-node { cursor: grab; }
    .member-node.dragging { cursor: grabbing; filter: drop-shadow(0 0 14px #fbbf24aa); }
    .member-node { cursor: pointer; }
    .member-node circle { transition: stroke .2s, stroke-width .2s, filter .2s; }
    .member-node:hover  circle:last-of-type { stroke-width: 3px !important; }
    .member-node.selected circle:last-of-type { stroke-width: 4px !important; }

    /* ── SIDE PANEL ──────────────────────────────────── */
    .side-panel {
      width: 0; overflow: hidden; transition: width .3s ease;
      background: #0f172a; border-left: 1px solid #1f2937;
      display: flex; flex-direction: column;
    }
    .side-panel.open { width: 300px; }
    .sp-header {
      display: flex; gap: 12px; align-items: flex-start;
      padding: 16px; border-bottom: 1px solid #1f2937; border-left: 4px solid #fbbf24;
    }
    .sp-avatar {
      width: 50px; height: 50px; border-radius: 50%; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      font-size: 17px; font-weight: 700; color: #fffbeb;
    }
    .sp-name     { font-size: 15px; font-weight: 700; color: #fbbf24; }
    .sp-gen-badge{ font-size: 11px; margin-top: 2px; font-weight: 600; }
    .anchor-tag  { margin-left: 6px; font-size: 10px; color: #d97706; }
    .sp-status   { font-size: 11px; color: #9ca3af; margin-top: 4px; }
    .sp-tabs     { display: flex; border-bottom: 1px solid #1f2937; }
    .sp-tab      {
      flex: 1; padding: 8px 4px; font-size: 11px; font-weight: 600;
      border: none; background: transparent; color: #6b7280; cursor: pointer;
      transition: all .2s;
    }
    .sp-tab:hover  { color: #fbbf24; }
    .sp-tab.active { color: #fbbf24; border-bottom: 2px solid #fbbf24; }
    .sp-tab-badge {
      display: inline-block; background: #fbbf24; color: #1a1005;
      border-radius: 10px; font-size: 10px; font-weight: 700;
      padding: 1px 6px; margin-left: 4px; vertical-align: middle;
    }
    /* Fila de conexión */
    .sp-conn-row {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 0; border-bottom: 1px solid #1f2937;
    }
    .sp-conn-row:last-child { border-bottom: none; }
    .sp-conn-avatar {
      width: 32px; height: 32px; border-radius: 50%; display: flex;
      align-items: center; justify-content: center; font-size: 12px;
      font-weight: 700; color: #fffbeb; flex-shrink: 0;
    }
    .sp-conn-info { flex: 1; min-width: 0; }
    .sp-conn-name { font-size: 12px; font-weight: 600; color: #f3f4f6;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sp-conn-type { font-size: 11px; color: #6b7280; margin-top: 2px; }
    .sp-conn-rel-type { color: #9ca3af; text-transform: capitalize; }
    .sp-body       { flex: 1; overflow-y: auto; padding: 12px 14px; }
    .sp-row        { display: flex; gap: 8px; align-items: center; margin-bottom: 8px;
      font-size: 12px; flex-wrap: wrap; }
    .sp-lbl        { color: #6b7280; min-width: 78px; }
    .conf-row      { align-items: center; }
    .conf-bar-wrap { flex: 1; height: 5px; background: #1f2937; border-radius: 3px; overflow: hidden; }
    .conf-bar      { height: 100%; border-radius: 3px; }
    .conf-pct      { font-size: 11px; color: #d97706; }
    .sp-field-title{ font-size: 10px; font-weight: 600; color: #6b7280;
      text-transform: uppercase; margin: 10px 0 4px; }
    .sp-text       { font-size: 12px; color: #d1d5db; line-height: 1.6; font-style: italic; }

    /* Evolution blocks in side panel */
    .ev-block {
      display: flex; gap: 10px; margin-bottom: 10px;
      padding: 8px 10px; border-radius: 8px; font-size: 12px; color: #d1d5db;
    }
    .ev-block.founding    { background: #1c1009; border-left: 3px solid #78350f; }
    .ev-block.builder     { background: #1c1204; border-left: 3px solid #b45309; }
    .ev-block.responsible { background: #1c1504; border-left: 3px solid #d97706; }
    .ev-block.current     { background: #1c1a04; border-left: 3px solid #fbbf24; }
    .ev-block.future      { background: #1c1c06; border-left: 3px solid #fde68a; }
    .ev-block.projected   { background: #151515; border-left: 3px solid #9ca3af; }
    .ev-icon   { font-size: 18px; flex-shrink: 0; }
    .ev-title  { font-size: 11px; font-weight: 600; color: #fbbf24; margin-bottom: 3px; }
    .ev-block p { margin: 0; line-height: 1.5; }

    /* Events in side panel */
    .sp-event  { display: flex; gap: 10px; margin-bottom: 10px; font-size: 12px;
      padding-bottom: 8px; border-bottom: 1px solid #1f2937; }
    .ev-year   { color: #d97706; min-width: 36px; font-weight: 600; }
    .ev-title-sm { color: #fde68a; font-weight: 600; }
    .ev-desc   { color: #9ca3af; font-size: 11px; margin: 3px 0 0; }
    .sp-ev-body { flex: 1; min-width: 0; }
    .empty-msg-sm { color: #6b7280; font-size: 11px; text-align: center; padding: 6px 0; }

    /* Formulario evento rápido */
    .quick-event-form { background: #111827; border: 1px solid #374151;
      border-radius: 8px; padding: 10px; margin-top: 8px; }
    .qef-row { display: flex; gap: 6px; margin-bottom: 6px; }
    .qef-input { background: #1f2937; border: 1px solid #374151; border-radius: 6px;
      color: #f3f4f6; font-size: 12px; padding: 6px 8px; outline: none; }
    .qef-input:focus { border-color: #f59e0b; }
    .qef-year  { width: 72px; flex-shrink: 0; }
    .qef-title { flex: 1; }
    .qef-textarea { width: 100%; background: #1f2937; border: 1px solid #374151;
      border-radius: 6px; color: #f3f4f6; font-size: 12px; padding: 6px 8px;
      resize: none; outline: none; box-sizing: border-box; margin-bottom: 8px; }
    .qef-textarea:focus { border-color: #f59e0b; }
    .qef-actions { display: flex; gap: 6px; justify-content: flex-end; }
    .btn-gold-sm { padding: 5px 12px; background: #d97706; color: #fffbeb;
      border: none; border-radius: 6px; font-size: 12px; cursor: pointer;
      transition: background .2s; }
    .btn-gold-sm:hover    { background: #b45309; }
    .btn-gold-sm:disabled { opacity: .5; cursor: not-allowed; }
    .btn-add-event { width: 100%; margin-top: 8px; padding: 7px;
      background: transparent; border: 1px dashed #374151; border-radius: 8px;
      color: #6b7280; font-size: 12px; cursor: pointer; transition: all .2s; }
    .btn-add-event:hover { border-color: #f59e0b; color: #f59e0b; }

    .sp-actions{ display: flex; gap: 8px; padding: 10px 14px; border-top: 1px solid #1f2937;
      justify-content: space-between; align-items: center; }

    /* ── ZOOM TOOLBAR ────────────────────────────────── */
    .zoom-bar {
      display: flex; align-items: center; gap: 6px;
      padding: 6px 16px; border-bottom: 1px solid #1f2937;
      background: #0d1117;
    }
    .zb-btn {
      width: 28px; height: 28px; border-radius: 6px;
      background: #1f2937; border: 1px solid #374151;
      color: #d1d5db; font-size: 16px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: background .15s;
    }
    .zb-btn:hover { background: #374151; color: #fbbf24; }
    .zb-reset { font-size: 13px; width: auto; padding: 0 8px; }
    .zb-pct {
      min-width: 40px; text-align: center;
      font-size: 12px; font-weight: 600; color: #9ca3af;
    }
    .zb-hint {
      margin-left: 8px; font-size: 10px; color: #4b5563;
    }

    /* ── GENERATION LEGEND ───────────────────────────── */
    .gen-legend {
      display: flex; flex-wrap: wrap; gap: 8px; align-items: center;
      padding: 10px 24px; border-top: 1px solid #1f2937;
    }
    .gl-item {
      display: flex; align-items: center; gap: 6px;
      font-size: 11px; color: #9ca3af; cursor: pointer;
      padding: 4px 8px; border-radius: 12px; transition: background .2s;
    }
    .gl-item:hover, .gl-item.active { background: #1f2937; color: #fde68a; }
    .gl-dot    { width: 10px; height: 10px; border-radius: 50%; }
    .gl-gen    { }
    .gl-count  { color: #6b7280; font-size: 10px; }
    .gl-anchor { font-size: 10px; }
    .gl-clear  {
      padding: 3px 10px; border: 1px solid #374151; border-radius: 10px;
      background: transparent; color: #6b7280; cursor: pointer; font-size: 11px;
    }

    /* ── CONTENT TABS ────────────────────────────────── */
    .content-tab { padding: 24px 28px; max-width: 900px; }
    .ct-header   { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
    .ct-header h3{ color: #fbbf24; font-size: 18px; margin: 0; }
    .ct-sub      { color: #6b7280; font-size: 12px; }
    .empty-msg   { color: #4b5563; font-style: italic; font-size: 13px; }

    /* Generation context block */
    .gen-context-block {
      border-left: 3px solid; padding: 12px 16px; margin-bottom: 16px;
      background: #111827; border-radius: 0 8px 8px 0;
    }
    .gcb-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px; }
    .gcb-label  { font-weight: 700; font-size: 13px; }
    .gcb-period { font-size: 11px; color: #6b7280; }
    .gcb-text   { font-size: 12px; color: #9ca3af; margin: 4px 0; }
    .gcb-kv     { font-size: 12px; color: #d1d5db; margin-top: 4px; }
    .gcb-kv span{ color: #d97706; margin-right: 4px; }

    /* Timeline */
    .timeline  { padding-left: 80px; }
    .tl-item   { display: flex; gap: 16px; align-items: flex-start;
      position: relative; margin-bottom: 20px; }
    .tl-year   { position: absolute; left: -80px; width: 68px;
      text-align: right; font-size: 12px; font-weight: 600; color: #d97706; padding-top: 3px; }
    .tl-dot    { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; }
    .tl-body   { flex: 1; }
    .tl-member { font-size: 10px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px; }
    .tl-title  { font-size: 13px; font-weight: 600; color: #fde68a; }
    .tl-desc   { font-size: 12px; color: #9ca3af; margin-top: 4px; }

    /* Narrative */
    .vision-block {
      display: flex; gap: 12px; align-items: flex-start;
      background: #111827; border-left: 3px solid #f59e0b;
      padding: 14px 16px; border-radius: 0 8px 8px 0; margin-bottom: 24px;
    }
    .vision-icon { font-size: 24px; }
    .vision-block p { color: #fde68a; font-style: italic; font-size: 14px; line-height: 1.7; margin: 0; }
    .gen-narrative-section { margin-bottom: 28px; }
    .gns-title { font-size: 13px; font-weight: 700; text-transform: uppercase;
      margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid #1f2937; }
    .narrative-card {
      background: #111827; border: 1px solid #1f2937;
      border-radius: 12px; padding: 14px; margin-bottom: 12px;
    }
    .nc-header { display: flex; gap: 10px; align-items: center; margin-bottom: 8px; }
    .nc-avatar {
      width: 40px; height: 40px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 15px; font-weight: 700; color: #fffbeb;
    }
    .nc-name   { font-size: 14px; font-weight: 700; color: #fbbf24; }
    .nc-role   { font-size: 11px; color: #9ca3af; }
    .nc-story  { font-size: 12px; color: #d1d5db; line-height: 1.7; }
    .italic    { font-style: italic; }
    .nc-field  { font-size: 12px; color: #9ca3af; margin-top: 6px; }
    .nc-field span { color: #d97706; font-weight: 600; margin-right: 4px; }

    /* Legacy */
    .legacy-banner {
      background: linear-gradient(135deg, #1c1200, #0d1117);
      border: 1px solid #374151; border-radius: 12px;
      padding: 20px 24px; margin-bottom: 24px; text-align: center;
    }
    .lb-title { font-size: 20px; font-weight: 700; color: #fbbf24; }
    .lb-sub   { font-size: 12px; color: #d97706; margin-top: 6px; font-style: italic; }
    .legacy-stats { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 28px; }
    .ls-card  {
      background: #111827; border: 1px solid #1f2937; border-radius: 10px;
      padding: 14px 20px; text-align: center; min-width: 90px;
    }
    .ls-num   { font-size: 26px; font-weight: 700; color: #fbbf24; }
    .ls-lbl   { font-size: 11px; color: #9ca3af; margin-top: 2px; }
    .legacy-columns { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 16px; }
    .lc-col   { min-width: 200px; flex-shrink: 0; }
    .lc-header {
      border: 1px solid; border-radius: 8px; padding: 10px 12px;
      margin-bottom: 10px; background: #0f172a;
    }
    .lc-label { font-size: 12px; font-weight: 700; }
    .lc-desc  { font-size: 10px; opacity: .7; margin-top: 2px; }
    .lc-member{
      display: flex; gap: 8px; align-items: flex-start;
      padding: 8px 10px; background: #111827;
      border: 1px solid; border-radius: 8px; margin-bottom: 6px;
    }
    .lc-avatar{
      width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: 700; color: #fffbeb;
    }
    .lc-name   { font-size: 12px; font-weight: 600; color: #fde68a; }
    .lc-status { font-size: 10px; color: #9ca3af; }
    .lc-legacy { font-size: 11px; color: #9ca3af; font-style: italic; margin-top: 4px; }

    /* ── MODAL ───────────────────────────────────────── */
    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,.8);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .modal {
      background: #111827; border: 1px solid #374151; border-radius: 16px;
      padding: 24px; width: 540px; max-width: 95vw;
      max-height: 90vh; overflow-y: auto;
    }
    .modal-wide { width: 640px; }
    .modal-sm   { width: 420px; }
    .modal-title{ font-size: 16px; font-weight: 700; color: #fbbf24; margin-bottom: 16px; }
    .modal-tabs {
      display: flex; gap: 4px; margin-bottom: 16px;
      border-bottom: 1px solid #374151; padding-bottom: 4px;
    }
    .modal-tabs button {
      padding: 6px 14px; border: none; cursor: pointer;
      background: transparent; color: #6b7280; border-radius: 6px 6px 0 0;
      font-size: 12px; font-weight: 600; transition: all .2s;
    }
    .modal-tabs button:hover  { color: #fbbf24; }
    .modal-tabs button.active { color: #fbbf24; border-bottom: 2px solid #fbbf24; }
    .form-grid  { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .mt-12      { margin-top: 12px; }
    .fg-row     { display: flex; flex-direction: column; gap: 4px; }
    .fg-full    { grid-column: 1/-1; }
    .fg-row label { font-size: 11px; color: #9ca3af; }
    .fg-row input, .fg-row select, .fg-row textarea {
      background: #1f2937; border: 1px solid #374151; border-radius: 8px;
      color: #e5e7eb; padding: 8px 10px; font-size: 13px; outline: none;
    }
    .fg-row input:focus, .fg-row select:focus, .fg-row textarea:focus {
      border-color: #f59e0b;
    }
    .fg-row input[type=range]   { padding: 0; border: none; background: transparent; }
    .fg-row input[type=checkbox]{ width: auto; margin-right: 6px; }
    .check-label { flex-direction: row !important; align-items: center;
      color: #9ca3af !important; font-size: 12px !important; cursor: pointer; }
    .period-inputs { display: flex; gap: 8px; align-items: center; }
    .period-inputs input { flex: 1; }
    .period-inputs span  { color: #6b7280; }
    /* Evolution fields in modal */
    .ev-field label { font-weight: 600 !important; }
    .ev-field.founding    label { color: #b45309 !important; }
    .ev-field.builder     label { color: #d97706 !important; }
    .ev-field.responsible label { color: #f59e0b !important; }
    .ev-field.current     label { color: #fbbf24 !important; }
    .ev-field.future      label { color: #fde68a !important; }
    .ev-field.projected   label { color: #9ca3af !important; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
  `]
})
export class LineagePageComponent implements OnInit, OnDestroy {
  private svc         = inject(LineageService);
  private familyState = inject(FamilyStateService);

  lineage        = signal<Lineage | null>(null);
  loading        = signal(true);
  error          = signal<string | null>(null);
  noLineage      = signal(false);
  activeTab      = signal<TreeTab>('tree');
  memberTab      = signal<MemberTab>('bio');
  selectedId     = signal<number | null>(null);
  selectedMember = signal<LineageMember | null>(null);
  genFilter      = signal<number | null>(null);
  showModal      = signal(false);
  showGenInfoModal     = signal(false);
  showEditLineageModal = signal(false);

  // ── Toast ─────────────────────────────────────────────────────────────────
  toast      = signal<string | null>(null);
  toastError = signal(false);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  showToast(msg: string, isError = false) {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set(msg);
    this.toastError.set(isError);
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }

  // ── Editar linaje ─────────────────────────────────────────────────────────
  lineageForm: Partial<{ title: string; visionStatement: string; foundingYear: string;
    maxPastGen: number; maxFutureGen: number }> = {};

  openEditLineage() {
    const l = this.lineage();
    if (!l) return;
    this.lineageForm = {
      title: l.title, visionStatement: l.visionStatement ?? undefined,
      foundingYear: l.foundingYear ?? undefined,
      maxPastGen: l.maxPastGen ?? -2, maxFutureGen: l.maxFutureGen ?? 2
    };
    this.showEditLineageModal.set(true);
  }

  saveLineage() {
    this.saving.set(true);
    this.svc.updateLineage(this.familyId, this.lineageForm)
      .pipe(catchError(() => { this.saving.set(false); this.showToast('Error al guardar el linaje', true); return of(null); }))
      .subscribe(data => {
        if (data) {
          this.lineage.set(data);
          this.showEditLineageModal.set(false);
          this.showToast('✅ Linaje actualizado correctamente');
        }
        this.saving.set(false);
      });
  }
  editingMember  = signal<LineageMember | null>(null);
  deleteTarget   = signal<LineageMember | null>(null);
  saving         = signal(false);
  formTab: 'bio' | 'evolution' = 'bio';
  initAnchor = 0;

  initGenOptions = [
    { value: -2, label: 'Bisabuelo/a', icon: '🏺', desc: 'Eres el mayor del árbol' },
    { value: -1, label: 'Abuelo/a',    icon: '🌿', desc: 'Tienes padres y abuelos' },
    { value: 0,  label: 'Padre/Madre', icon: '🌳', desc: 'Eres la generación responsable' },
    { value: 1,  label: 'Hijo/a',      icon: '🌱', desc: 'Eres la generación actual' },
  ];

  form: Partial<LineageMemberRequest> & { confidenceLevel: number } = {
    generation: 0, status: 'alive', confidenceLevel: 50
  };

  genInfoForm: GenerationInfoRequest = { generationLevel: 0 };

  // ── Drag & Drop de nodos ─────────────────────────────────────────────────
  dragId       = signal<number | null>(null);
  dragOffsetX  = 0;
  dragOffsetY  = 0;
  /** Overrides de posición para los nodos en arrastre (sin guardar aún) */
  dragOverride = signal<Map<number, { px: number; py: number }>>(new Map());

  startDrag(m: LineageMember, e: MouseEvent) {
    e.stopPropagation();
    e.preventDefault();
    this.dragId.set(m.id);
    // Encontrar el nodo posicionado actual
    const pm = this.positionedMembers().find(p => p.id === m.id);
    if (!pm) return;
    const svg = (e.currentTarget as SVGElement).ownerSVGElement;
    if (!svg) return;
    const pt   = svg.createSVGPoint();
    pt.x = e.clientX; pt.y = e.clientY;
    const svgPt = pt.matrixTransform(svg.getScreenCTM()!.inverse());
    this.dragOffsetX = svgPt.x - pm.px;
    this.dragOffsetY = svgPt.y - pm.py;
  }

  onDragMove(e: MouseEvent) {
    // Paneo con Alt+drag o middle-click
    if (this.isPanning) {
      const dx = (e.clientX - this.panClientX) * this.panSvgScale;
      const dy = (e.clientY - this.panClientY) * this.panSvgScale;
      this.vbOffX.set(this.panVbStartX - dx);
      this.vbOffY.set(this.panVbStartY - dy);
      return;
    }
    const id = this.dragId();
    if (id == null) return;
    e.preventDefault();
    const svg = e.currentTarget as SVGSVGElement;
    const pt  = svg.createSVGPoint();
    pt.x = e.clientX; pt.y = e.clientY;
    const svgPt = pt.matrixTransform(svg.getScreenCTM()!.inverse());
    const newX  = svgPt.x - this.dragOffsetX;
    const newY  = svgPt.y - this.dragOffsetY;
    const map   = new Map(this.dragOverride());
    map.set(id, { px: newX, py: newY });
    this.dragOverride.set(map);
  }

  endDrag(e: MouseEvent) {
    if (this.isPanning) { this.isPanning = false; return; }
    const id = this.dragId();
    if (id == null) return;
    this.dragId.set(null);
    const pos = this.dragOverride().get(id);
    // Limpiar override — la posición quedará en el miembro después de guardar
    this.dragOverride.set(new Map());
    if (!pos) return;
    // Persistir posición en backend — enviamos todos los campos del miembro actual
    const member = this.lineage()?.members.find(m => m.id === id);
    if (!member) return;
    // Actualización optimista: modificar el signal local sin esperar al backend
    const currentLineage = this.lineage();
    if (currentLineage) {
      this.lineage.set({
        ...currentLineage,
        members: currentLineage.members.map(m =>
          m.id === id ? { ...m, positionX: Math.round(pos.px), positionY: Math.round(pos.py) } : m
        )
      });
    }
    const req: LineageMemberRequest = {
      firstName: member.firstName ?? undefined,
      lastName:  member.lastName  ?? undefined,
      avatarInitials: member.avatarInitials ?? undefined,
      avatarColor:    member.avatarColor    ?? undefined,
      generation:  member.generation,
      generationType: member.generationType,
      isAnchor:    member.isAnchor,
      status:      member.status,
      birthYear:   member.birthYear    ?? undefined,
      birthYearApproximate: member.birthYearApproximate ?? undefined,
      deathYear:   member.deathYear    ?? undefined,
      origin:      member.origin       ?? undefined,
      roleLabel:   member.roleLabel    ?? undefined,
      confidenceLevel: member.confidenceLevel,
      dataSource:  member.dataSource   ?? undefined,
      story:       member.story        ?? undefined,
      valores:     member.valores      ?? undefined,
      aprendizajes: member.aprendizajes ?? undefined,
      erroresSuperados: member.erroresSuperados ?? undefined,
      tradiciones:  member.tradiciones  ?? undefined,
      misionesCumplidas: member.misionesCumplidas ?? undefined,
      legadoPersonal: member.legadoPersonal ?? undefined,
      familyMemberId: member.familyMemberId ?? undefined,
      positionX: Math.round(pos.px), positionY: Math.round(pos.py)
    };
    // Guardar en backend en background (sin bloquear UI)
    this.svc.updateMember(this.familyId, id, req)
      .pipe(catchError(() => of(null)))
      .subscribe(); // Actualización optimista ya aplicada arriba
  }

  cancelDrag() {
    if (this.dragId() == null) return;
    this.dragId.set(null);
    this.dragOverride.set(new Map());
  }

  // ── Conectar miembros ─────────────────────────────────────────────────────
  showConnectModal = signal(false);
  connectSource    = signal<LineageMember | null>(null);
  connectTargetId: number | null = null;
  connectRelType   = 'biological';
  connectIsCouple  = false;

  connectableMembers = computed(() =>
    (this.lineage()?.members ?? []).filter(m => m.id !== this.connectSource()?.id)
  );

  openConnectModal(m: LineageMember) {
    this.connectSource.set(m);
    this.connectTargetId = null;
    this.connectRelType  = 'biological';
    this.connectIsCouple = false;
    this.showConnectModal.set(true);
  }

  saveConnection() {
    const src = this.connectSource();
    if (!src || !this.connectTargetId) return;
    this.saving.set(true);
    this.svc.addRelationship(
      this.familyId, src.id!, this.connectTargetId,
      this.connectRelType, this.connectIsCouple
    ).pipe(catchError(() => {
      this.saving.set(false);
      this.showToast('Error al crear la conexión', true);
      return of(null);
    })).subscribe(rel => {
       if (rel) {
         this.loadLineage();
         this.showConnectModal.set(false);
         this.showToast('✅ Conexión creada en el árbol');
       }
       this.saving.set(false);
     });
  }

  // ── Conexiones del miembro seleccionado ──────────────────────────────────
  memberConnections(m: LineageMember): { rel: LineageRelationship; other: LineageMember | undefined }[] {
    const rels     = this.lineage()?.relationships ?? [];
    const members  = this.lineage()?.members ?? [];
    return rels
      .filter(r => r.fromMemberId === m.id || r.toMemberId === m.id)
      .map(r => {
        const otherId = r.fromMemberId === m.id ? r.toMemberId : r.fromMemberId;
        return { rel: r, other: members.find(mb => mb.id === otherId) };
      });
  }

  deleteRelationship(relId: number) {
    this.svc.deleteRelationship(this.familyId, relId)
      .pipe(catchError(() => { this.showToast('Error al eliminar la conexión', true); return of(null); }))
      .subscribe(() => { this.loadLineage(); this.showToast('✅ Conexión eliminada'); });
  }

  // ── Agregar evento rápido ─────────────────────────────────────────────────
  addingEvent = signal(false);
  quickEvent: { eventYear?: string; title: string; description?: string } = { title: '' };

  saveQuickEvent(m: LineageMember) {
    if (!this.quickEvent.title) return;
    this.saving.set(true);
    // Construimos el request con todos los campos existentes + el nuevo evento
    const existing = (m.events ?? []).map(e => ({
      eventYear: e.eventYear ?? undefined,
      title: e.title,
      description: e.description ?? undefined,
      eventType: e.eventType,
      isApproximate: e.isApproximate,
      sortOrder: e.sortOrder
    }));
    const req: LineageMemberRequest = {
      firstName: m.firstName ?? undefined, lastName: m.lastName ?? undefined,
      avatarInitials: m.avatarInitials ?? undefined, avatarColor: m.avatarColor ?? undefined,
      generation: m.generation, generationType: m.generationType,
      isAnchor: m.isAnchor, status: m.status,
      birthYear: m.birthYear ?? undefined, origin: m.origin ?? undefined,
      roleLabel: m.roleLabel ?? undefined, confidenceLevel: m.confidenceLevel,
      dataSource: m.dataSource ?? undefined, story: m.story ?? undefined,
      valores: m.valores ?? undefined, aprendizajes: m.aprendizajes ?? undefined,
      erroresSuperados: m.erroresSuperados ?? undefined, tradiciones: m.tradiciones ?? undefined,
      misionesCumplidas: m.misionesCumplidas ?? undefined, legadoPersonal: m.legadoPersonal ?? undefined,
      familyMemberId: m.familyMemberId ?? undefined,
      positionX: m.positionX ?? undefined, positionY: m.positionY ?? undefined,
      events: [
        ...existing,
        {
          eventYear: this.quickEvent.eventYear,
          title: this.quickEvent.title,
          description: this.quickEvent.description,
          eventType: 'milestone',
          isApproximate: false,
          sortOrder: existing.length
        }
      ]
    };
    this.svc.updateMember(this.familyId, m.id, req)
      .pipe(catchError(() => { this.saving.set(false); this.showToast('Error al guardar el evento', true); return of(null); }))
      .subscribe(saved => {
        if (saved) {
          this.loadLineage();
          this.addingEvent.set(false);
          this.quickEvent = { title: '' };
          this.showToast('✅ Evento registrado en el linaje');
        }
        this.saving.set(false);
      });
  }

  resetPosition(m: LineageMember) {
    const req: LineageMemberRequest = {
      firstName: m.firstName ?? undefined, lastName: m.lastName ?? undefined,
      avatarInitials: m.avatarInitials ?? undefined, avatarColor: m.avatarColor ?? undefined,
      generation: m.generation, generationType: m.generationType,
      isAnchor: m.isAnchor, status: m.status,
      birthYear: m.birthYear ?? undefined, origin: m.origin ?? undefined,
      roleLabel: m.roleLabel ?? undefined, confidenceLevel: m.confidenceLevel,
      dataSource: m.dataSource ?? undefined, story: m.story ?? undefined,
      valores: m.valores ?? undefined, aprendizajes: m.aprendizajes ?? undefined,
      erroresSuperados: m.erroresSuperados ?? undefined, tradiciones: m.tradiciones ?? undefined,
      misionesCumplidas: m.misionesCumplidas ?? undefined, legadoPersonal: m.legadoPersonal ?? undefined,
      familyMemberId: m.familyMemberId ?? undefined,
      positionX: 0, positionY: 0
    };
    // Actualización optimista
    const cur = this.lineage();
    if (cur) {
      this.lineage.set({
        ...cur,
        members: cur.members.map(mb =>
          mb.id === m.id ? { ...mb, positionX: null, positionY: null } : mb
        )
      });
    }
    this.showToast('📐 Posición restablecida al auto-layout');
    this.svc.updateMember(this.familyId, m.id, req)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  // ── SVG sizing + Zoom/Pan ─────────────────────────────────────────────────
  svgW = signal(1000);
  svgH = computed(() => {
    const rows = this.visibleGens().length;
    return Math.max(500, rows * 140 + 100);
  });
  svgZoom  = signal(1.0);
  vbOffX   = signal(0);
  vbOffY   = signal(0);

  svgViewBox = computed(() => {
    const z   = this.svgZoom();
    const vbW = this.svgW() / z;
    const vbH = this.svgH() / z;
    return `${this.vbOffX()} ${this.vbOffY()} ${vbW} ${vbH}`;
  });

  // pan state (class fields, not signals — no re-render needed)
  private isPanning  = false;
  private panClientX = 0;
  private panClientY = 0;
  private panVbStartX = 0;
  private panVbStartY = 0;
  private panSvgScale = 1;

  onWheel(e: WheelEvent) {
    e.preventDefault();
    const svg = e.currentTarget as SVGSVGElement;
    const pt  = svg.createSVGPoint();
    pt.x = e.clientX; pt.y = e.clientY;
    const sp   = pt.matrixTransform(svg.getScreenCTM()!.inverse());
    const oldZ = this.svgZoom();
    const newZ = Math.max(0.25, Math.min(4.0, oldZ * (e.deltaY < 0 ? 1.15 : 1 / 1.15)));
    const vbW  = this.svgW() / oldZ;
    const vbH  = this.svgH() / oldZ;
    const fx   = (sp.x - this.vbOffX()) / vbW;
    const fy   = (sp.y - this.vbOffY()) / vbH;
    this.vbOffX.set(sp.x - fx * (this.svgW() / newZ));
    this.vbOffY.set(sp.y - fy * (this.svgH() / newZ));
    this.svgZoom.set(newZ);
  }

  onSvgMouseDown(e: MouseEvent) {
    // Middle-click (button=1) o Alt+click inicia paneo
    if (e.button !== 1 && !e.altKey) return;
    e.preventDefault();
    this.isPanning = true;
    this.panClientX  = e.clientX;
    this.panClientY  = e.clientY;
    this.panVbStartX = this.vbOffX();
    this.panVbStartY = this.vbOffY();
    const svg  = e.currentTarget as SVGSVGElement;
    const rect = svg.getBoundingClientRect();
    this.panSvgScale = (this.svgW() / this.svgZoom()) / rect.width;
  }

  zoomIn()  { this.applyZoomCentered(this.svgZoom() * 1.25); }
  zoomOut() { this.applyZoomCentered(this.svgZoom() / 1.25); }
  resetView() { this.svgZoom.set(1.0); this.vbOffX.set(0); this.vbOffY.set(0); }
  zoomPct() { return Math.round(this.svgZoom() * 100); }

  private applyZoomCentered(newZ: number) {
    const clampedZ = Math.max(0.25, Math.min(4.0, newZ));
    const oldZ  = this.svgZoom();
    const vbW   = this.svgW() / oldZ;
    const vbH   = this.svgH() / oldZ;
    const cx    = this.vbOffX() + vbW / 2;
    const cy    = this.vbOffY() + vbH / 2;
    const newVbW = this.svgW() / clampedZ;
    const newVbH = this.svgH() / clampedZ;
    this.vbOffX.set(cx - newVbW / 2);
    this.vbOffY.set(cy - newVbH / 2);
    this.svgZoom.set(clampedZ);
  }

  // ── Derived ───────────────────────────────────────────────────────────────
  memberCount    = computed(() => this.lineage()?.members?.length ?? 0);
  anchorLabel    = computed(() => getGenMeta(this.lineage()?.anchorGeneration ?? 0).label);
  visibleGenCount= computed(() => this.visibleGens().length);

  visibleGens = computed<number[]>(() => {
    const l = this.lineage();
    if (!l) return [];
    return genRange(l.anchorGeneration, l.maxPastGen, l.maxFutureGen);
  });

  /** Y position for a generation level (past = top, future = bottom) */
  private genY(gen: number): number {
    const gens   = this.visibleGens();
    const minGen = gens[0] ?? gen;
    const idx    = gens.indexOf(gen);
    const rowH   = (this.svgH() - 80) / Math.max(1, gens.length - 1);
    return 60 + idx * rowH;
  }

  genBands = computed(() => {
    const members = this.filteredMembers();
    return this.visibleGens().map(g => {
      const meta  = getGenMeta(g);
      const count = this.lineage()?.members.filter(m => m.generation === g).length ?? 0;
      const rowH  = (this.svgH() - 80) / Math.max(1, this.visibleGens().length - 1);
      return {
        gen: g, label: meta.label, color: meta.color,
        y: this.genY(g), h: rowH,
        isAnchor: g === (this.lineage()?.anchorGeneration ?? 0),
        count
      };
    });
  });

  filteredMembers = computed<LineageMember[]>(() => {
    const members = this.lineage()?.members ?? [];
    const f = this.genFilter();
    return f !== null ? members.filter(m => m.generation === f) : members;
  });

  positionedMembers = computed<(LineageMember & { px: number; py: number })[]>(() => {
    const members  = this.filteredMembers();
    const w        = this.svgW();
    const overrides = this.dragOverride();
    const grouped  = new Map<number, LineageMember[]>();
    members.forEach(m => {
      if (!grouped.has(m.generation)) grouped.set(m.generation, []);
      grouped.get(m.generation)!.push(m);
    });

    return members.map(m => {
      // Drag override tiene prioridad máxima
      const drag = overrides.get(m.id);
      if (drag) return { ...m, px: drag.px, py: drag.py };
      // Posición manual guardada en BD
      if (m.positionX != null && m.positionY != null && (m.positionX !== 0 || m.positionY !== 0)) {
        return { ...m, px: m.positionX, py: m.positionY };
      }
      // Auto-layout por generación
      const genMembers = grouped.get(m.generation) ?? [m];
      const idx    = genMembers.indexOf(m);
      const count  = genMembers.length;
      const margin = w * 0.12;
      const usable = w - margin * 2;
      const px = count === 1
        ? w / 2
        : margin + (idx / (count - 1)) * usable;
      return { ...m, px, py: this.genY(m.generation) };
    });
  });

  branchPaths = computed<{ id: string; d: string; isCouple: boolean }[]>(() => {
    const rels    = this.lineage()?.relationships ?? [];
    const mMap    = new Map(this.positionedMembers().map(m => [m.id, m]));
    return rels.map(r => {
      const from = mMap.get(r.fromMemberId);
      const to   = mMap.get(r.toMemberId);
      if (!from || !to) return null;
      const dy     = Math.abs(to.py - from.py);
      const ctrl1y = from.py + dy * 0.5;
      const ctrl2y = to.py   - dy * 0.5;
      const d = `M ${from.px} ${from.py} C ${from.px} ${ctrl1y} ${to.px} ${ctrl2y} ${to.px} ${to.py}`;
      return { id: `r-${r.id}`, d, isCouple: r.isCouple };
    }).filter(Boolean) as { id: string; d: string; isCouple: boolean }[];
  });

  allEvents = computed<{ event: any; memberName: string; memberColor: string }[]>(() => {
    const members = this.lineage()?.members ?? [];
    return members
      .flatMap(m => (m.events ?? []).map(ev => ({
        event: ev,
        memberName: m.fullName,
        memberColor: getGenMeta(m.generation).color
      })))
      .filter(x => x.event.eventYear)
      .sort((a, b) => parseInt(a.event.eventYear ?? '0') - parseInt(b.event.eventYear ?? '0'));
  });

  membersByGeneration = computed(() => {
    const members = this.lineage()?.members ?? [];
    const gens    = [...new Set(members.map(m => m.generation))].sort((a, b) => a - b);
    return gens.map(g => ({
      gen: g,
      members: members.filter(m => m.generation === g)
    }));
  });

  hasMembersWithNarrative = computed(() =>
    (this.lineage()?.members ?? []).some(m =>
      m.story || m.valores || m.aprendizajes || m.legadoPersonal));

  /** Helper accesible desde el template — evita arrow functions en @if */
  genHasNarrative(members: LineageMember[]): boolean {
    return members.some(m => !!(m.story || m.valores || m.aprendizajes));
  }

  legacyStats = computed(() => {
    const members = this.lineage()?.members ?? [];
    const gens    = new Set(members.map(m => m.generation)).size;
    return [
      { label: 'Generaciones', value: gens },
      { label: 'Miembros',     value: members.length },
      { label: 'Vivos',        value: members.filter(m => m.status === 'alive').length },
      { label: 'Relatos',      value: members.filter(m => !!m.story).length },
      { label: 'Con legado',   value: members.filter(m => !!m.legadoPersonal).length },
    ];
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit() { this.loadLineage(); }

  ngOnDestroy() {
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.dragId() != null) { this.cancelDrag(); return; }
    if (this.showModal())             { this.closeModal(); return; }
    if (this.showEditLineageModal())  { this.showEditLineageModal.set(false); return; }
    if (this.showGenInfoModal())      { this.showGenInfoModal.set(false); return; }
    if (this.showConnectModal())      { this.showConnectModal.set(false); return; }
    if (this.deleteTarget())          { this.deleteTarget.set(null); return; }
    if (this.selectedMember())        { this.clearSelection(); }
  }

  private get familyId() { return this.familyState.getFamilyId(); }

  loadLineage() {
    this.loading.set(true);
    this.error.set(null);
    this.svc.getLineage(this.familyId).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.noLineage.set(true);
          this.error.set('Esta familia aún no tiene un linaje registrado.');
        } else {
          this.error.set('Error al cargar el linaje.');
        }
        return of(null);
      })
    ).subscribe(data => {
      if (data) {
        this.lineage.set(data);
        this.error.set(null);
        this.noLineage.set(false);
        // Refresh selected member reference from reloaded data
        const selId = this.selectedId();
        if (selId != null) {
          const fresh = data.members.find(m => m.id === selId);
          this.selectedMember.set(fresh ?? null);
          if (!fresh) this.selectedId.set(null);
        }
      }
      this.loading.set(false);
    });
  }

  createLineage() {
    this.saving.set(true);
    this.svc.createLineage(this.familyId, {
      title: 'Linaje Familiar',
      anchorGeneration: this.initAnchor,
      maxPastGen: this.initAnchor - 2,
      maxFutureGen: this.initAnchor + 2
    }).pipe(catchError(() => { this.saving.set(false); return of(null); }))
      .subscribe(data => {
        if (data) { this.lineage.set(data); this.error.set(null); this.noLineage.set(false); }
        this.saving.set(false);
      });
  }

  // ── Node helpers ──────────────────────────────────────────────────────────
  nodeRadius(gen: number) { return gen === 0 ? 32 : Math.abs(gen) === 1 ? 28 : 22; }
  nodeFill(m: LineageMember) {
    const c = getGenMeta(m.generation).color;
    return m.status === 'deceased' ? '#1f2937' : c + '44';
  }
  nodeStroke(gen: number) { return getGenMeta(gen).color; }
  initials(m: LineageMember) {
    return [m.firstName, m.lastName].filter(Boolean).map(p => p![0]).join('').toUpperCase().slice(0,2) || '?';
  }
  shortName(m: LineageMember) { return m.firstName || m.fullName.split(' ')[0] || '?'; }
  statusIcon(s: string) { return STATUS_ICON[s] ?? '❓'; }
  statusLabel(s: string) { return STATUS_LABEL[s] ?? s; }
  getGenMeta = getGenMeta;

  // ── Selection ─────────────────────────────────────────────────────────────
  selectMember(m: LineageMember, e: Event) {
    e.stopPropagation();
    this.selectedId.set(m.id);
    this.selectedMember.set(m);
    this.memberTab.set('bio');
    this.addingEvent.set(false);
    this.quickEvent = { title: '' };
  }
  clearSelection() { this.selectedId.set(null); this.selectedMember.set(null); }
  filterByGen(gen: number) {
    this.genFilter.update(cur => cur === gen ? null : gen);
  }

  // ── Modal member ──────────────────────────────────────────────────────────
  openAddMember() {
    this.editingMember.set(null);
    this.form = { generation: this.lineage()?.anchorGeneration ?? 0, status: 'alive', confidenceLevel: 50 };
    this.formTab = 'bio';
    this.showModal.set(true);
  }

  openEditMember(m: LineageMember) {
    this.editingMember.set(m);
    this.form = {
      firstName: m.firstName ?? undefined, lastName: m.lastName ?? undefined,
      avatarInitials: m.avatarInitials ?? undefined, avatarColor: m.avatarColor ?? undefined,
      generation: m.generation, generationType: m.generationType,
      isAnchor: m.isAnchor, status: m.status,
      birthYear: m.birthYear ?? undefined, birthYearApproximate: m.birthYearApproximate ?? undefined,
      deathYear: m.deathYear ?? undefined, origin: m.origin ?? undefined,
      roleLabel: m.roleLabel ?? undefined, confidenceLevel: m.confidenceLevel,
      dataSource: m.dataSource ?? undefined, story: m.story ?? undefined,
      valores: m.valores ?? undefined, aprendizajes: m.aprendizajes ?? undefined,
      erroresSuperados: m.erroresSuperados ?? undefined, tradiciones: m.tradiciones ?? undefined,
      misionesCumplidas: m.misionesCumplidas ?? undefined, legadoPersonal: m.legadoPersonal ?? undefined
    };
    this.formTab = 'bio';
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.editingMember.set(null); }

  saveMember() {
    if (this.saving()) return;
    this.saving.set(true);
    const req: LineageMemberRequest = {
      ...this.form,
      generation: Number(this.form.generation),
      status: this.form.status || 'unknown',
      confidenceLevel: this.form.confidenceLevel
    };
    const em  = this.editingMember();
    const obs = em
      ? this.svc.updateMember(this.familyId, em.id, req)
      : this.svc.addMember(this.familyId, req);

    obs.pipe(catchError(() => {
      this.saving.set(false);
      this.showToast('Error al guardar el miembro', true);
      return of(null);
    })).subscribe(saved => {
        if (saved) {
          this.loadLineage();
          this.closeModal();
          this.showToast(em ? '✅ Miembro actualizado' : '✅ Miembro agregado al árbol');
        }
        this.saving.set(false);
      });
  }

  confirmDelete(m: LineageMember) { this.deleteTarget.set(m); }

  deleteMember() {
    const t = this.deleteTarget();
    if (!t) return;
    this.saving.set(true);
    this.svc.deleteMember(this.familyId, t.id)
      .pipe(catchError(() => { this.saving.set(false); return of(null); }))
      .subscribe(() => {
        this.deleteTarget.set(null);
        this.clearSelection();
        this.loadLineage();
        this.saving.set(false);
      });
  }

  // ── Modal generation info ─────────────────────────────────────────────────
  openGenInfo() {
    this.genInfoForm = { generationLevel: this.lineage()?.anchorGeneration ?? 0 };
    this.showGenInfoModal.set(true);
  }

  saveGenInfo() {
    this.saving.set(true);
    this.svc.upsertGenerationInfo(this.familyId, this.genInfoForm)
      .pipe(catchError(() => {
        this.saving.set(false);
        this.showToast('Error al guardar el contexto', true);
        return of(null);
      })).subscribe(saved => {
        if (saved) {
          this.loadLineage();
          this.showGenInfoModal.set(false);
          this.showToast('✅ Contexto generacional guardado');
        }
        this.saving.set(false);
      });
  }
}
