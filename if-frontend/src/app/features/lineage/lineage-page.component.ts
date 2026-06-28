import {
  Component, OnInit, OnDestroy, inject, signal, computed, HostListener, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { LineageService } from './lineage.service';
import { FamilyStateService } from '../../core/services/family-state.service';
import { ApiService } from '../../core/services/api.service';
import {
  Lineage, LineageMember, LineageMemberRequest, LineageRelationship,
  GenerationInfo, GenerationInfoRequest,
  GEN_META, getGenMeta, MemberStatus
} from './lineage.model';
import { catchError, of } from 'rxjs';

type TreeTab = 'tree' | 'history' | 'narrative' | 'legacy' | 'stats';
type MemberTab = 'bio' | 'evolution' | 'events' | 'connections' | 'capsule';

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
  providers: [],
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
      @if (lineage()) {
        <div class="lh-completeness">
          <div class="lhc-bar-wrap">
            <div class="lhc-bar" [style.width.%]="globalCompleteness()"
              [style.background]="globalCompleteness() >= 75 ? '#22c55e' : globalCompleteness() >= 40 ? '#f59e0b' : '#ef4444'">
            </div>
          </div>
          <span class="lhc-label">{{ globalCompleteness() }}% completitud del árbol</span>
        </div>
      }
    </div>
    <div class="header-actions">
      @if (lineage()) {
        <button class="btn-outline" (click)="openEditLineage()">⚙️ Linaje</button>
        <button class="btn-outline" (click)="openGenInfo()">📋 Contexto</button>
        <button class="btn-outline btn-doc" (click)="openDocument()">📄 Documento</button>
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
    <button class="tab" [class.active]="activeTab() === 'stats'"     (click)="activeTab.set('stats')">📊 Estadísticas</button>
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

      <!-- Zoom toolbar (fuera del flex-row del árbol) -->
      <div class="zoom-bar">
        <button class="zb-btn" (click)="zoomOut()" title="Alejar (−)">−</button>
        <span class="zb-pct">{{ zoomPct() }}%</span>
        <button class="zb-btn" (click)="zoomIn()" title="Acercar (+)">+</button>
        <button class="zb-btn zb-reset" (click)="resetView()" title="Restablecer vista">⊞</button>
        <button class="zb-btn zb-reorg" [class.active]="ignoreStoredPositions()"
          (click)="ignoreStoredPositions.set(!ignoreStoredPositions())"
          title="Reorganizar árbol automáticamente (ignora posiciones manuales)">🔀</button>
        <span class="zb-hint">Alt+arrastrar · rueda = zoom</span>
        <div class="zb-divider"></div>
        <button class="zb-btn zb-gen-mode" [class.active]="genMode()"
          (click)="toggleGenMode()" title="Vista expandible por generación">
          🌿 {{ genMode() ? 'Vista generacional' : 'Ver todo' }}
        </button>
        <div class="zb-divider"></div>
        <!-- Buscador de miembros -->
        <div class="zb-search-wrap">
          <input class="zb-search" type="text" placeholder="🔍 Buscar miembro…"
            [value]="memberSearch()"
            (input)="onMemberSearch($event)"
            (keydown.enter)="jumpToSearchResult()"
            (keydown.escape)="clearSearch()" />
          @if (searchResults().length > 0) {
            <div class="zb-search-dropdown">
              @for (r of searchResults(); track r.id) {
                <button class="zb-search-item" (click)="jumpToMember(r)">
                  <span class="zb-si-name">{{ r.fullName }}</span>
                  <span class="zb-si-gen">Gen {{ r.generation }}</span>
                </button>
              }
            </div>
          }
        </div>
      </div>

      <div class="tree-layout">

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

            <!-- Relationship branches (T-junction tree) -->
            @for (path of branchPaths(); track path.id) {
              @if (!path.isCouple) {
                @if (!path.isThin) {
                  <!-- trunk & vertical drops: glowing line -->
                  <path [attr.d]="path.d" stroke="#fbbf24" stroke-width="20" fill="none" opacity=".05"/>
                  <path [attr.d]="path.d" stroke="url(#lin-trunk-g)"
                    stroke-width="3" fill="none"
                    filter="url(#lin-glow)" opacity=".9" stroke-linecap="round"/>
                } @else {
                  <!-- horizontal sibling bar: thinner, no glow -->
                  <path [attr.d]="path.d" stroke="url(#lin-trunk-g)"
                    stroke-width="2" fill="none" opacity=".7" stroke-linecap="round"/>
                }
              }
            }

            <!-- Couple connectors: horizontal bar + ❤ badge -->
            @for (cp of coupleConnectors(); track cp.id) {
              <!-- background glow bar -->
              <line [attr.x1]="cp.x1" [attr.y1]="cp.y" [attr.x2]="cp.x2" [attr.y2]="cp.y"
                stroke="#fbbf24" stroke-width="14" opacity=".08" stroke-linecap="round"/>
              <!-- dashed line -->
              <line [attr.x1]="cp.x1" [attr.y1]="cp.y" [attr.x2]="cp.x2" [attr.y2]="cp.y"
                stroke="#f59e0b" stroke-width="1.5" opacity=".7"
                stroke-dasharray="5 4" stroke-linecap="round"/>
              <!-- ❤ at midpoint -->
              <circle [attr.cx]="cp.mx" [attr.cy]="cp.y" r="9"
                fill="#1f1410" stroke="#f59e0b" stroke-width="1.2" opacity=".9"/>
              <text [attr.x]="cp.mx" [attr.y]="cp.y + 1"
                text-anchor="middle" dominant-baseline="middle"
                font-size="10" style="pointer-events:none">❤️</text>
            }

            <!-- Couple background blocks (drawn before nodes so nodes appear on top) -->
            @for (cp of coupleConnectors(); track cp.id) {
              <rect
                [attr.x]="cp.x1 - 32" [attr.y]="cp.y - 32"
                [attr.width]="cp.x2 - cp.x1 + 64" height="64" rx="20"
                fill="#fbbf2408" stroke="#f59e0b" stroke-width="0.8"
                opacity=".5" style="pointer-events:none"/>
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
                <!-- Anillo de completitud: arco exterior (stroke-dasharray = pct/100 * circumference) -->
                @if (completeness(m) < 100) {
                  <!-- fondo gris del anillo -->
                  <circle [attr.cx]="m.px" [attr.cy]="m.py"
                    [attr.r]="nodeRadius(m.generation) + 5"
                    fill="none" stroke="#374151" stroke-width="3" opacity=".7"/>
                  <!-- arco de progreso -->
                  <circle [attr.cx]="m.px" [attr.cy]="m.py"
                    [attr.r]="nodeRadius(m.generation) + 5"
                    fill="none"
                    [attr.stroke]="completenessColor(m)"
                    stroke-width="3"
                    stroke-linecap="round"
                    [attr.stroke-dasharray]="(completeness(m) / 100) * 2 * 3.14159 * (nodeRadius(m.generation) + 5) + ' ' + (2 * 3.14159 * (nodeRadius(m.generation) + 5))"
                    [attr.transform]="'rotate(-90 ' + m.px + ' ' + m.py + ')'"
                    opacity=".9"/>
                } @else {
                  <!-- completitud 100% → anillo verde sólido -->
                  <circle [attr.cx]="m.px" [attr.cy]="m.py"
                    [attr.r]="nodeRadius(m.generation) + 5"
                    fill="none" stroke="#22c55e" stroke-width="3" opacity=".9"/>
                }
                <!-- defs locales para clipPath por miembro -->
                <defs>
                  <clipPath [attr.id]="'clip-' + m.id">
                    <circle [attr.cx]="m.px" [attr.cy]="m.py" [attr.r]="nodeRadius(m.generation)"/>
                  </clipPath>
                </defs>
                @if (highlightedId() === m.id) {
                  <circle [attr.cx]="m.px" [attr.cy]="m.py"
                    [attr.r]="nodeRadius(m.generation) + 10"
                    fill="none" stroke="#34d399" stroke-width="3" opacity=".8"
                    class="search-pulse-ring"/>
                }
                <circle [attr.cx]="m.px" [attr.cy]="m.py"
                  [attr.r]="nodeRadius(m.generation)"
                  [attr.fill]="nodeFill(m)"
                  [attr.stroke]="highlightedId() === m.id ? '#34d399' : nodeStroke(m.generation)"
                  [attr.stroke-width]="highlightedId() === m.id ? 4 : (selectedId() === m.id ? 3 : (m.isAnchor ? 2.5 : 1.8))"
                  filter="url(#lin-glow)" opacity=".95"/>
                @if (m.photoUrl) {
                  <!-- Foto de perfil circular -->
                  <image [attr.href]="m.photoUrl"
                    [attr.x]="m.px - nodeRadius(m.generation)"
                    [attr.y]="m.py - nodeRadius(m.generation)"
                    [attr.width]="nodeRadius(m.generation) * 2"
                    [attr.height]="nodeRadius(m.generation) * 2"
                    [attr.clip-path]="'url(#clip-' + m.id + ')'"
                    preserveAspectRatio="xMidYMid slice"
                    style="pointer-events:none"/>
                } @else {
                  <text [attr.x]="m.px" [attr.y]="m.py + 1"
                    text-anchor="middle" dominant-baseline="middle"
                    [attr.font-size]="nodeRadius(m.generation) * 0.62"
                    fill="#fffbeb" font-weight="600" style="pointer-events:none">
                    {{ m.avatarInitials || initials(m) }}
                  </text>
                }
                <!-- deceased overlay -->
                @if (m.status === 'deceased') {
                  <text [attr.x]="m.px + nodeRadius(m.generation)*0.55"
                    [attr.y]="m.py - nodeRadius(m.generation)*0.55"
                    font-size="11" style="pointer-events:none">🕊️</text>
                }
                <!-- Badge de vínculo con miembro registrado -->
                @if (m.familyMemberId) {
                  <text [attr.x]="m.px - nodeRadius(m.generation)*0.55"
                    [attr.y]="m.py - nodeRadius(m.generation)*0.55"
                    font-size="10" style="pointer-events:none" title="Vinculado a perfil registrado">🔗</text>
                }
                <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + 14"
                  text-anchor="middle" font-size="11" fill="#fde68a" style="pointer-events:none">
                  {{ shortName(m) }}
                </text>
                @if (m.birthYear) {
                  <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + 25"
                    text-anchor="middle" font-size="8.5" fill="#9ca3af" opacity=".8"
                    style="pointer-events:none">
                    {{ m.birthYearApproximate ? '~' : '' }}{{ m.birthYear }}{{ m.status === 'alive' ? '' : (m.deathYear ? '–' + m.deathYear : '') }}
                  </text>
                }
                @if (m.roleLabel) {
                  <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + (m.birthYear ? 35 : 26)"
                    text-anchor="middle" font-size="9" [attr.fill]="nodeStroke(m.generation)"
                    opacity=".8" style="pointer-events:none">
                    {{ m.roleLabel }}
                  </text>
                }
                <!-- ▼ badge: nodo con hijos ocultos en genMode -->
                @if (hasUnexpandedChildren().has(m.id)) {
                  <rect [attr.x]="m.px - 14" [attr.y]="m.py + nodeRadius(m.generation) + (m.roleLabel ? 30 : 20)"
                    width="28" height="14" rx="7"
                    fill="#14532d" stroke="#22c55e" stroke-width="1"
                    style="pointer-events:none" opacity=".9"/>
                  <text [attr.x]="m.px" [attr.y]="m.py + nodeRadius(m.generation) + (m.roleLabel ? 37 : 27)"
                    text-anchor="middle" dominant-baseline="middle"
                    font-size="8" fill="#4ade80" font-weight="700"
                    style="pointer-events:none">
                    ▼ {{ hiddenChildrenCount().get(m.id) ?? '' }}
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

              @if (selectedMemberSpouse(); as sp) {
                <!-- Couple header: both members side by side -->
                <div class="sp-couple-wrap">
                  <div class="sp-couple-unit">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" [alt]="m.fullName" class="sp-avatar sp-avatar-sm sp-avatar-photo"/>
                    } @else {
                      <div class="sp-avatar sp-avatar-sm" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                        {{ m.avatarInitials || initials(m) }}
                      </div>
                    }
                    <span class="sp-couple-name">{{ m.firstName || m.fullName }}</span>
                  </div>
                  <span class="sp-couple-heart">❤️</span>
                  <div class="sp-couple-unit" (click)="selectMember(sp, $event); $event.stopPropagation()" style="cursor:pointer" title="Ver perfil de {{ sp.fullName }}">
                    @if (sp.photoUrl) {
                      <img [src]="sp.photoUrl" [alt]="sp.fullName" class="sp-avatar sp-avatar-sm sp-avatar-photo"/>
                    } @else {
                      <div class="sp-avatar sp-avatar-sm" [style.background]="sp.avatarColor || nodeStroke(sp.generation)">
                        {{ sp.avatarInitials || initials(sp) }}
                      </div>
                    }
                    <span class="sp-couple-name">{{ sp.firstName || sp.fullName }}</span>
                  </div>
                </div>
                <div class="sp-gen-badge" [style.color]="nodeStroke(m.generation)">
                  {{ getGenMeta(m.generation).label }}
                  @if (m.isAnchor) { <span class="anchor-tag">⚓ Ancla</span> }
                  @if (memberChildrenCount(m) > 0) {
                    <span class="sp-children-count">· {{ memberChildrenCount(m) }} hijo{{ memberChildrenCount(m) !== 1 ? 's' : '' }}</span>
                  }
                </div>

              } @else {
                <!-- Solo member header -->
                @if (m.photoUrl) {
                  <img [src]="m.photoUrl" [alt]="m.fullName" class="sp-avatar sp-avatar-photo"/>
                } @else {
                  <div class="sp-avatar" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                    {{ m.avatarInitials || initials(m) }}
                  </div>
                }
                <div>
                  <div class="sp-name">{{ m.fullName }}</div>
                  <div class="sp-gen-badge" [style.color]="nodeStroke(m.generation)">
                    {{ getGenMeta(m.generation).label }}
                    @if (m.isAnchor) { <span class="anchor-tag">⚓ Ancla</span> }
                  </div>
                  <div class="sp-status">{{ statusIcon(m.status) }} {{ statusLabel(m.status) }}</div>
                  @if (m.familyMemberId && linkedMember(m.familyMemberId); as reg) {
                    <div class="sp-linked">
                      🔗 <span>{{ reg.fullName }}</span>
                      @if (reg.email) { <span class="sp-linked-email">{{ reg.email }}</span> }
                    </div>
                  }
                </div>
              }
            </div>

            <!-- Member sub-tabs -->
            <div class="sp-tabs">
              <button class="sp-tab" [class.active]="memberTab() === 'capsule'"
                (click)="memberTab.set('capsule')">💎 Cápsula</button>
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

              <!-- CÁPSULA DE VIDA -->
              @if (memberTab() === 'capsule') {
                <div class="capsule-header">
                  <div class="ch-name">{{ m.fullName }}</div>
                  <div class="ch-gen" [style.color]="nodeStroke(m.generation)">
                    {{ getGenMeta(m.generation).label }}
                  </div>
                  <!-- completeness ring summary -->
                  <div class="ch-completeness">
                    <div class="ch-ring" [style.background]="
                      'conic-gradient(' + completenessColor(m) + ' ' + completeness(m) + '%, #1f2937 0)'">
                      <div class="ch-ring-inner">{{ completeness(m) }}%</div>
                    </div>
                    <span class="ch-ring-lbl">Perfil documentado</span>
                  </div>
                </div>

                <!-- Grow the tree -->
                <div class="cap-grow-section">
                  <div class="cap-section-title">🌱 Hacer crecer el árbol</div>
                  <div class="cap-grow-row">
                    <button class="btn-quick child" (click)="quickAdd(m, 'son')">👦 Hijo</button>
                    <button class="btn-quick child" (click)="quickAdd(m, 'daughter')">👧 Hija</button>
                    @if (!hasSpouse(m)) {
                      <button class="btn-quick spouse" (click)="quickAdd(m, 'spouse')">❤️ Cónyuge</button>
                    }
                  </div>
                  @if (memberChildrenCount(m) > 0) {
                    <div class="cap-children-count">
                      🌿 {{ memberChildrenCount(m) }} hijo{{ memberChildrenCount(m) !== 1 ? 's' : '' }} registrados en el árbol
                    </div>
                  }
                </div>

                <!-- Familia inferida -->
                @if (inferredFamily().parents.length || inferredFamily().siblings.length || inferredFamily().children.length || inferredFamily().grandparents.length) {
                  <div class="cap-family-section">
                    <div class="cap-section-title">👨‍👩‍👧‍👦 Familia en el árbol</div>
                    @if (inferredFamily().grandparents.length) {
                      <div class="cap-fam-row">
                        <span class="cap-fam-lbl">Abuelos</span>
                        <span class="cap-fam-names">
                          @for (gp of inferredFamily().grandparents; track gp.id) {
                            <button class="cap-fam-chip" (click)="selectMember(gp, $event)">{{ gp.fullName }}</button>
                          }
                        </span>
                      </div>
                    }
                    @if (inferredFamily().parents.length) {
                      <div class="cap-fam-row">
                        <span class="cap-fam-lbl">Padres</span>
                        <span class="cap-fam-names">
                          @for (p of inferredFamily().parents; track p.id) {
                            <button class="cap-fam-chip" (click)="selectMember(p, $event)">{{ p.fullName }}</button>
                          }
                        </span>
                      </div>
                    }
                    @if (inferredFamily().siblings.length) {
                      <div class="cap-fam-row">
                        <span class="cap-fam-lbl">Hermanos</span>
                        <span class="cap-fam-names">
                          @for (s of inferredFamily().siblings; track s.id) {
                            <button class="cap-fam-chip" (click)="selectMember(s, $event)">{{ s.fullName }}</button>
                          }
                        </span>
                      </div>
                    }
                    @if (inferredFamily().children.length) {
                      <div class="cap-fam-row">
                        <span class="cap-fam-lbl">Hijos</span>
                        <span class="cap-fam-names">
                          @for (c of inferredFamily().children; track c.id) {
                            <button class="cap-fam-chip" (click)="selectMember(c, $event)">{{ c.fullName }}</button>
                          }
                        </span>
                      </div>
                    }
                  </div>
                }

                <!-- Navigation links -->
                <div class="capsule-grid">
                  <button class="cap-link" (click)="navigate('/evaluations/history')">
                    <span class="cap-icon">📊</span>
                    <span class="cap-label">Indicadores ICF</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/plans')">
                    <span class="cap-icon">🎯</span>
                    <span class="cap-label">Plan de mejora</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/logbook')">
                    <span class="cap-icon">📋</span>
                    <span class="cap-label">Bitácora familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/checklist')">
                    <span class="cap-icon">✅</span>
                    <span class="cap-label">Misiones activas</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/documentary-maker')">
                    <span class="cap-icon">🎬</span>
                    <span class="cap-label">Documental familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/legado')">
                    <span class="cap-icon">🏛️</span>
                    <span class="cap-label">Legado del linaje</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/family-dna')">
                    <span class="cap-icon">🧬</span>
                    <span class="cap-label">ADN familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/family-timeline')">
                    <span class="cap-icon">📅</span>
                    <span class="cap-label">Historia familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/family-pulse')">
                    <span class="cap-icon">💓</span>
                    <span class="cap-label">Pulso familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/digital-twin')">
                    <span class="cap-icon">🤖</span>
                    <span class="cap-label">Gemelo digital</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  <button class="cap-link" (click)="navigate('/family-council')">
                    <span class="cap-icon">🏛</span>
                    <span class="cap-label">Consejo familiar</span>
                    <span class="cap-arrow">→</span>
                  </button>
                  @if (m.familyMemberId) {
                    <button class="cap-link cap-link-highlight" (click)="navigate('/my-space')">
                      <span class="cap-icon">👤</span>
                      <span class="cap-label">Mi espacio personal</span>
                      <span class="cap-arrow">→</span>
                    </button>
                  }
                </div>
              }

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

      <!-- Generational controls (solo en genMode) -->
      @if (genMode()) {
        <div class="gen-mode-bar">
          @if (unexpandedNodeCount() > 0) {
            <div class="gmb-hint">
              <span class="gmb-dot">▼</span>
              Toca un nodo para expandir sus hijos · {{ unexpandedNodeCount() }} nodo{{ unexpandedNodeCount() !== 1 ? 's' : '' }} con hijos ocultos
            </div>
            <button class="ges-btn" (click)="expandAllVisible()">Expandir todo</button>
          } @else {
            <div class="gmb-hint gmb-done">✅ Todas las generaciones visibles están expandidas</div>
          }
          @if (expandedParentIds().size > 0) {
            <button class="gcs-btn" (click)="collapseAll()">▲ Colapsar todo</button>
          }
        </div>
      }

      <!-- generation legend (fuera del tree-layout, dentro del @if tree) -->
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
          <div>
            <h3>Línea de Tiempo del Linaje</h3>
            <span class="ct-sub">{{ allEvents().length }} eventos · {{ lineage()?.foundingYear ? 'Desde ' + lineage()!.foundingYear : '' }}</span>
          </div>
          <button class="btn-gold btn-sm" (click)="openAddEvent()">➕ Nuevo evento</button>
        </div>
        <!-- Generation context blocks -->
        @for (genGroup of membersByGeneration(); track genGroup.gen) {
          <div class="gen-context-block"
            [style.border-left-color]="getGenMeta(genGroup.gen).color"
            [class.gcb-empty]="!genInfoFor(genGroup.gen)">
            <div class="gcb-header">
              <span class="gcb-label" [style.color]="getGenMeta(genGroup.gen).color">
                {{ genInfoFor(genGroup.gen)?.title || getGenMeta(genGroup.gen).label }}
              </span>
              <div class="gcb-actions">
                @if (genInfoFor(genGroup.gen)?.periodStart || genInfoFor(genGroup.gen)?.periodEnd) {
                  <span class="gcb-period">
                    {{ genInfoFor(genGroup.gen)?.periodStart }}–{{ genInfoFor(genGroup.gen)?.periodEnd }}
                  </span>
                }
                <button class="gcb-edit-btn"
                  (click)="openGenInfo(genGroup.gen)"
                  [title]="genInfoFor(genGroup.gen) ? 'Editar contexto' : 'Agregar contexto'">
                  {{ genInfoFor(genGroup.gen) ? '✏️' : '➕' }}
                </button>
              </div>
            </div>
            @if (genInfoFor(genGroup.gen)?.summary) {
              <p class="gcb-text">{{ genInfoFor(genGroup.gen)!.summary }}</p>
            }
            @if (genInfoFor(genGroup.gen)?.context) {
              <p class="gcb-text gcb-context">{{ genInfoFor(genGroup.gen)!.context }}</p>
            }
            @if (genInfoFor(genGroup.gen)?.keyChallenge) {
              <div class="gcb-kv"><span>⚡ Desafío:</span> {{ genInfoFor(genGroup.gen)!.keyChallenge }}</div>
            }
            @if (genInfoFor(genGroup.gen)?.keyAchievement) {
              <div class="gcb-kv"><span>🏆 Logro:</span> {{ genInfoFor(genGroup.gen)!.keyAchievement }}</div>
            }
            @if (!genInfoFor(genGroup.gen)) {
              <p class="gcb-empty-hint">Sin contexto aún — haz clic en ➕ para agregar.</p>
            }
          </div>
        }
        @if (allEvents().length === 0) {
          <p class="empty-msg">No hay eventos registrados aún.</p>
        } @else {
          <div class="timeline">
            @for (ev of allEvents(); track ev.event.id) {
              <div class="tl-item" (click)="ev.isDoc ? viewDocumentary(ev.docId!) : openEditEvent(ev)" title="Clic para ver detalle">
                <div class="tl-year">{{ ev.event.isApproximate ? '~' : '' }}{{ ev.event.eventYear || '?' }}</div>
                <div class="tl-dot" [style.background]="ev.memberColor" [style.box-shadow]="'0 0 8px ' + ev.memberColor + '80'"></div>
                <div class="tl-body">
                  <div class="tl-member" [style.color]="ev.memberColor">{{ ev.memberName }}</div>
                  <div class="tl-title">
                    <span class="tl-type-badge" [style.background]="ev.isDoc ? '#7c3aed' : ''" [style.color]="ev.isDoc ? '#fff' : ''">
                      {{ ev.isDoc ? 'DOCUMENTAL' : eventTypeLabel(ev.event.eventType) }}
                    </span>
                    {{ ev.event.title }}
                  </div>
                  @if (ev.event.description) { <p class="tl-desc">{{ ev.event.description }}</p> }
                  @if (ev.isDoc) {
                    <button class="tl-edit-btn doc-btn" (click)="viewDocumentary(ev.docId!); $event.stopPropagation()" title="Ver Documental">🎬 Ver</button>
                  } @else {
                    <button class="tl-edit-btn" (click)="openEditEvent(ev); $event.stopPropagation()" title="Editar">✏️</button>
                  }
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
          @if (true) {
            <div class="gen-narrative-section">
              <div class="gns-title" [style.color]="getGenMeta(genGroup.gen).color">
                {{ getGenMeta(genGroup.gen).label }}
                <span class="gns-count">{{ genGroup.members.length }} miembro{{ genGroup.members.length !== 1 ? 's' : '' }}</span>
              </div>
              @for (m of genGroup.members; track m.id) {
                @if (m.story || m.valores || m.aprendizajes || m.legadoPersonal || m.erroresSuperados || m.tradiciones || m.misionesCumplidas) {
                  <div class="narrative-card">
                    <div class="nc-header">
                      @if (m.photoUrl) {
                        <img [src]="m.photoUrl" [alt]="m.fullName" class="nc-photo"/>
                      } @else {
                        <div class="nc-avatar" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                          {{ m.avatarInitials || initials(m) }}
                        </div>
                      }
                      <div class="nc-meta">
                        <div class="nc-name">{{ m.fullName }}</div>
                        <div class="nc-role">{{ m.roleLabel || '' }}</div>
                        <div class="nc-completeness-wrap">
                          <div class="nc-completeness-bar">
                            <div class="nc-completeness-fill"
                              [style.width.%]="completeness(m)"
                              [style.background]="completenessColor(m)">
                            </div>
                          </div>
                          <span class="nc-completeness-pct" [style.color]="completenessColor(m)">
                            {{ completeness(m) }}%
                          </span>
                        </div>
                      </div>
                      <button class="nc-edit-btn" (click)="openEditMember(m)" title="Editar perfil">✏️</button>
                    </div>
                    @if (m.story)             { <p class="nc-story italic">{{ m.story }}</p> }
                    @if (m.valores)           { <p class="nc-field"><span>💎 Valores:</span> {{ m.valores }}</p> }
                    @if (m.aprendizajes)      { <p class="nc-field"><span>📚 Aprendizajes:</span> {{ m.aprendizajes }}</p> }
                    @if (m.erroresSuperados)  { <p class="nc-field"><span>🔄 Errores superados:</span> {{ m.erroresSuperados }}</p> }
                    @if (m.tradiciones)       { <p class="nc-field"><span>🎎 Tradiciones:</span> {{ m.tradiciones }}</p> }
                    @if (m.misionesCumplidas) { <p class="nc-field"><span>🎯 Misiones:</span> {{ m.misionesCumplidas }}</p> }
                    @if (m.legadoPersonal)    { <p class="nc-field nc-legado"><span>🏛️ Legado:</span> {{ m.legadoPersonal }}</p> }
                  </div>
                } @else {
                  <!-- Miembro sin narrativa — tarjeta vacía que invita a completar -->
                  <div class="narrative-card nc-empty" (click)="openEditMember(m)">
                    <div class="nc-header">
                      @if (m.photoUrl) {
                        <img [src]="m.photoUrl" [alt]="m.fullName" class="nc-photo"/>
                      } @else {
                        <div class="nc-avatar nc-avatar-dim" [style.background]="m.avatarColor || nodeStroke(m.generation)">
                          {{ m.avatarInitials || initials(m) }}
                        </div>
                      }
                      <div class="nc-meta">
                        <div class="nc-name">{{ m.fullName }}</div>
                        <div class="nc-empty-hint">Sin historia aún · haz clic para completar</div>
                      </div>
                    </div>
                  </div>
                }
              }
            </div>
          }
        }
        @if (!(lineage()?.members?.length)) {
          <p class="empty-msg">Agrega miembros al árbol para ver su narrativa.</p>
        }
      </div>
    }

    <!-- ── TAB: LEGADO ─────────────────────────────────── -->
    @if (activeTab() === 'legacy') {
      <div class="content-tab">

        <!-- Banner con tagline real -->
        <div class="legacy-banner">
          <div class="lb-title">
            {{ legadoData()?.constitutionFamilyName || 'Árbol de Evolución y Legado Familiar' }}
          </div>
          <div class="lb-sub">
            {{ legadoData()?.familyTagline || 'Historias que inspiran · Lecciones que trascienden · Decisiones que transforman' }}
          </div>
          <button class="btn-gold lb-cta" (click)="goToLegado()">
            📜 Ver Documento Completo →
          </button>
        </div>

        <!-- Stats -->
        <div class="legacy-stats">
          @for (s of legacyStats(); track s.label) {
            <div class="ls-card">
              <div class="ls-num">{{ s.value }}</div>
              <div class="ls-lbl">{{ s.label }}</div>
            </div>
          }
        </div>

        <!-- Misión & Visión -->
        @if (legadoData()?.familyMission || legadoData()?.familyVision) {
          <div class="mv-row">
            @if (legadoData()?.familyMission) {
              <div class="mv-block">
                <div class="mv-bhead">🎯 Misión Familiar</div>
                <p class="mv-btext">{{ legadoData()!.familyMission }}</p>
              </div>
            }
            @if (legadoData()?.familyVision) {
              <div class="mv-block">
                <div class="mv-bhead">🌅 Visión 2040</div>
                <p class="mv-btext">{{ legadoData()!.familyVision }}</p>
              </div>
            }
          </div>
        }

        <!-- Valores familiares -->
        @if (legadoVals().length > 0) {
          <div class="vals-section">
            <div class="vals-title">💎 Valores Familiares</div>
            <div class="vals-grid">
              @for (v of legadoVals(); track v.id) {
                <div class="val-chip">
                  <span class="val-icon">{{ v.icon }}</span>
                  <span class="val-name">{{ v.name }}</span>
                </div>
              }
            </div>
          </div>
        }

        <!-- Columnas de generación -->
        <div class="lc-sec-title">🌳 Legado Personal por Generación</div>
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

    <!-- ── TAB: ESTADÍSTICAS ──────────────────────────────── -->
    @if (activeTab() === 'stats') {
      <div class="content-tab stats-tab">

        <!-- KPIs principales -->
        <div class="st-kpi-row">
          <div class="st-kpi">
            <div class="st-kpi-num">{{ lineageStats().totalMembers }}</div>
            <div class="st-kpi-lbl">Miembros totales</div>
          </div>
          <div class="st-kpi">
            <div class="st-kpi-num">{{ lineageStats().totalGenerations }}</div>
            <div class="st-kpi-lbl">Generaciones</div>
          </div>
          <div class="st-kpi">
            <div class="st-kpi-num">{{ lineageStats().totalCouples }}</div>
            <div class="st-kpi-lbl">Parejas</div>
          </div>
          <div class="st-kpi">
            <div class="st-kpi-num">{{ lineageStats().avgCompleteness }}%</div>
            <div class="st-kpi-lbl">Completitud media</div>
          </div>
        </div>

        <!-- Estado vital -->
        <div class="st-section">
          <div class="st-section-title">Estado vital</div>
          <div class="st-bar-list">
            <div class="st-bar-item">
              <span class="st-bar-lbl">Vivos</span>
              <div class="st-bar-track">
                <div class="st-bar-fill st-alive"
                  [style.width.%]="lineageStats().totalMembers ? (lineageStats().alive / lineageStats().totalMembers * 100) : 0"></div>
              </div>
              <span class="st-bar-val">{{ lineageStats().alive }}</span>
            </div>
            <div class="st-bar-item">
              <span class="st-bar-lbl">Fallecidos</span>
              <div class="st-bar-track">
                <div class="st-bar-fill st-deceased"
                  [style.width.%]="lineageStats().totalMembers ? (lineageStats().deceased / lineageStats().totalMembers * 100) : 0"></div>
              </div>
              <span class="st-bar-val">{{ lineageStats().deceased }}</span>
            </div>
            <div class="st-bar-item">
              <span class="st-bar-lbl">Sin dato</span>
              <div class="st-bar-track">
                <div class="st-bar-fill st-unknown"
                  [style.width.%]="lineageStats().totalMembers ? (lineageStats().unknown / lineageStats().totalMembers * 100) : 0"></div>
              </div>
              <span class="st-bar-val">{{ lineageStats().unknown }}</span>
            </div>
          </div>
        </div>

        <!-- Distribución por generación -->
        <div class="st-section">
          <div class="st-section-title">Miembros por generación</div>
          <div class="st-gen-bars">
            @for (g of lineageStats().byGeneration; track g.gen) {
              <div class="st-gen-row">
                <span class="st-gen-lbl">{{ getGenMeta(g.gen).label }}</span>
                <div class="st-gen-track">
                  <div class="st-gen-fill"
                    [style.width.%]="lineageStats().maxInGen ? (g.count / lineageStats().maxInGen * 100) : 0"
                    [style.background]="getGenMeta(g.gen).color"></div>
                </div>
                <span class="st-gen-count">{{ g.count }}</span>
              </div>
            }
          </div>
        </div>

        <!-- Completitud de perfiles -->
        <div class="st-section">
          <div class="st-section-title">Completitud de perfiles</div>
          <div class="st-compl-grid">
            @for (band of lineageStats().complBands; track band.label) {
              <div class="st-compl-card" [style.border-color]="band.color">
                <div class="st-compl-num" [style.color]="band.color">{{ band.count }}</div>
                <div class="st-compl-lbl">{{ band.label }}</div>
              </div>
            }
          </div>
        </div>

        <!-- Top miembros mejor documentados -->
        @if (lineageStats().topDocumented.length) {
          <div class="st-section">
            <div class="st-section-title">✨ Perfiles más completos</div>
            <div class="st-top-list">
              @for (item of lineageStats().topDocumented; track item.member.id) {
                <div class="st-top-item" (click)="jumpToMember(item.member)">
                  <div class="st-top-avatar" [style.background]="'#374151'">
                    {{ (item.member.firstName || item.member.fullName || '?')[0] }}
                  </div>
                  <div class="st-top-info">
                    <div class="st-top-name">{{ item.member.fullName }}</div>
                    <div class="st-top-gen">{{ getGenMeta(item.member.generation).label }}</div>
                  </div>
                  <div class="st-top-pct">{{ item.pct }}%</div>
                </div>
              }
            </div>
          </div>
        }

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
            <!-- Vincular con miembro registrado -->
            @if (registeredMembers().length) {
              <div class="fg-row fg-full">
                <label>🔗 Vincular con miembro registrado</label>
                <select [(ngModel)]="form.familyMemberId"
                  (ngModelChange)="onRegisteredMemberSelect($event)">
                  <option [ngValue]="undefined">— Sin vincular —</option>
                  @for (rm of registeredMembers(); track rm.id) {
                    <option [ngValue]="rm.id">
                      {{ rm.fullName }}{{ rm.role ? ' · ' + rm.role : '' }}{{ rm.age ? ' (' + rm.age + ' años)' : '' }}
                    </option>
                  }
                </select>
                @if (form.familyMemberId) {
                  <div class="link-badge">
                    ✅ Vinculado a <strong>{{ linkedMember(form.familyMemberId)?.fullName }}</strong>
                    — nombre y rol auto-rellenados
                  </div>
                }
              </div>
            }
            <div class="fg-row fg-full">
              <label>📷 URL de foto de perfil</label>
              <input [(ngModel)]="form.photoUrl" placeholder="https://... (opcional)"/>
              @if (form.photoUrl) {
                <img [src]="form.photoUrl" alt="preview"
                  style="width:48px;height:48px;border-radius:50%;object-fit:cover;margin-top:6px;border:2px solid #fbbf24"/>
              }
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
        <div class="modal-title">
          📋 {{ genInfoFor(genInfoForm.generationLevel) ? 'Editar' : 'Agregar' }}
          contexto · {{ getGenMeta(genInfoForm.generationLevel).label }}
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
            <label>Resumen del contexto histórico</label>
            <textarea [(ngModel)]="genInfoForm.summary" rows="2"
              placeholder="¿En qué mundo vivió esta generación?"></textarea>
          </div>
          <div class="fg-row fg-full">
            <label>📖 Contexto ampliado</label>
            <textarea [(ngModel)]="genInfoForm.context" rows="2"
              placeholder="Cultura, economía, fe, costumbres de la época…"></textarea>
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

  <!-- ── MODAL: EVENTO ───────────────────────────────── -->
  @if (showEventModal()) {
    <div class="modal-overlay" (click)="closeEventModal()">
      <div class="modal modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-title">
          {{ editingEvent() ? '✏️ Editar evento' : '➕ Nuevo evento' }}
        </div>
        <div class="form-grid" style="grid-template-columns:1fr;">
          <div class="fg-row">
            <label>Miembro</label>
            <select [(ngModel)]="eventForm.memberId" [disabled]="!!editingEvent()">
              <option [value]="undefined" disabled>— Selecciona un miembro —</option>
              @for (m of lineage()?.members ?? []; track m.id) {
                <option [value]="m.id">
                  Gen {{ m.generation >= 0 ? '+' + m.generation : m.generation }} · {{ m.fullName }}
                </option>
              }
            </select>
          </div>
          <div class="fg-row">
            <label>Tipo de evento</label>
            <select [(ngModel)]="eventForm.eventType">
              <option value="milestone">🏁 Hito</option>
              <option value="birth">🐣 Nacimiento</option>
              <option value="death">🕊️ Fallecimiento</option>
              <option value="marriage">💍 Matrimonio</option>
              <option value="migration">✈️ Migración</option>
              <option value="achievement">🏆 Logro</option>
              <option value="trauma">⚡ Trauma / Quiebre</option>
            </select>
          </div>
          <div class="fg-row">
            <label>Título *</label>
            <input [(ngModel)]="eventForm.title" placeholder="Ej: Llegó a la ciudad..." maxlength="200"/>
          </div>
          <div class="fg-row">
            <label>Año</label>
            <input [(ngModel)]="eventForm.eventYear" placeholder="Ej: 1965 ó ~1970"/>
          </div>
          <div class="fg-row">
            <label>Descripción</label>
            <textarea [(ngModel)]="eventForm.description" rows="3"
              placeholder="Detalle del evento..."></textarea>
          </div>
          <div class="fg-row">
            <label class="check-label">
              <input type="checkbox" [(ngModel)]="eventForm.isApproximate"/>
              El año es aproximado
            </label>
          </div>
        </div>
        <div class="modal-actions">
          @if (editingEvent()) {
            <button class="btn-danger" (click)="confirmDeleteEvent()" [disabled]="saving()">
              🗑 Eliminar
            </button>
          }
          <button class="btn-ghost" (click)="closeEventModal()">Cancelar</button>
          <button class="btn-gold" (click)="saveEvent()"
            [disabled]="saving() || !eventForm.title || !eventForm.memberId">
            {{ saving() ? 'Guardando...' : (editingEvent() ? 'Actualizar' : 'Crear evento') }}
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

  <!-- ── DOCUMENTO DE CONSTITUCIÓN FAMILIAR ───────────── -->
  @if (showDocument()) {
    <div class="doc-overlay" (click)="closeDocument()">
      <div class="doc-modal" (click)="$event.stopPropagation()">

        <!-- Toolbar del documento -->
        <div class="doc-toolbar">
          <span class="doc-toolbar-title">📄 Documento de Legado Familiar</span>
          <div class="doc-toolbar-actions">
            <button class="btn-gold btn-sm" (click)="printDocument()">🖨️ Imprimir / PDF</button>
            <button class="btn-ghost btn-sm" (click)="closeDocument()">✕ Cerrar</button>
          </div>
        </div>

        <!-- Contenido imprimible -->
        <div class="doc-content" id="doc-print-area">

          <!-- PORTADA -->
          <div class="doc-cover">
            <div class="doc-cover-seal">IF</div>
            <h1 class="doc-title">{{ lineage()?.title || 'Legado Familiar' }}</h1>
            <div class="doc-subtitle">{{ legadoData()?.familyName || '' }}</div>
            <div class="doc-code">{{ lineage()?.lineageCode }}</div>
            @if (lineage()?.visionStatement) {
              <blockquote class="doc-vision">"{{ lineage()!.visionStatement }}"</blockquote>
            }
            @if (legadoData()?.tagline) {
              <p class="doc-tagline">{{ legadoData()?.tagline }}</p>
            }
            <div class="doc-year">{{ currentYear }}</div>
          </div>

          <div class="doc-divider"></div>

          <!-- MISIÓN Y VISIÓN -->
          @if (legadoData()?.missionStatement || legadoData()?.visionStatement) {
            <section class="doc-section">
              <h2 class="doc-section-title">🎯 Misión y Visión</h2>
              @if (legadoData()?.missionStatement) {
                <div class="doc-mv-block">
                  <div class="doc-mv-label">MISIÓN</div>
                  <p class="doc-mv-text">{{ legadoData()!.missionStatement }}</p>
                </div>
              }
              @if (legadoData()?.visionStatement) {
                <div class="doc-mv-block">
                  <div class="doc-mv-label">VISIÓN</div>
                  <p class="doc-mv-text">{{ legadoData()!.visionStatement }}</p>
                </div>
              }
            </section>
            <div class="doc-divider"></div>
          }

          <!-- VALORES FAMILIARES -->
          @if (legadoVals().length) {
            <section class="doc-section">
              <h2 class="doc-section-title">💎 Valores Familiares</h2>
              <div class="doc-values-grid">
                @for (v of legadoVals(); track v.id) {
                  <div class="doc-value-card">
                    <div class="doc-value-icon">{{ v.icon || '💠' }}</div>
                    <div class="doc-value-name">{{ v.name }}</div>
                    @if (v.description) {
                      <div class="doc-value-desc">{{ v.description }}</div>
                    }
                  </div>
                }
              </div>
            </section>
            <div class="doc-divider"></div>
          }

          <!-- ÁRBOL GENEALÓGICO — resumen por generación -->
          <section class="doc-section">
            <h2 class="doc-section-title">🌳 Árbol Genealógico</h2>
            <p class="doc-tree-stats">
              {{ memberCount() }} miembros · {{ visibleGenCount() }} generaciones ·
              {{ globalCompleteness() }}% documentado
              @if (lineage()?.foundingYear) { · Desde {{ lineage()!.foundingYear }} }
            </p>

            @for (genGroup of membersByGeneration(); track genGroup.gen) {
              <div class="doc-gen-block">
                <div class="doc-gen-header" [style.border-left-color]="getGenMeta(genGroup.gen).color">
                  <span class="doc-gen-label" [style.color]="getGenMeta(genGroup.gen).color">
                    {{ getGenMeta(genGroup.gen).label }}
                  </span>
                  @if (genInfoFor(genGroup.gen)?.periodStart || genInfoFor(genGroup.gen)?.periodEnd) {
                    <span class="doc-gen-period">
                      {{ genInfoFor(genGroup.gen)?.periodStart }} – {{ genInfoFor(genGroup.gen)?.periodEnd }}
                    </span>
                  }
                </div>
                @if (genInfoFor(genGroup.gen)?.summary) {
                  <p class="doc-gen-context">{{ genInfoFor(genGroup.gen)!.summary }}</p>
                }
                <div class="doc-members-row">
                  @for (m of genGroup.members; track m.id) {
                    <div class="doc-member-chip">
                      @if (m.photoUrl) {
                        <img [src]="m.photoUrl" [alt]="m.fullName" class="doc-member-photo"/>
                      } @else {
                        <div class="doc-member-avatar"
                          [style.background]="m.avatarColor || getGenMeta(m.generation).color">
                          {{ m.avatarInitials || initials(m) }}
                        </div>
                      }
                      <div class="doc-member-info">
                        <div class="doc-member-name">{{ m.fullName }}</div>
                        @if (m.birthYear) {
                          <div class="doc-member-years">
                            {{ m.birthYear }}{{ m.deathYear ? ' – ' + m.deathYear : (m.status === 'alive' ? ' – presente' : '') }}
                          </div>
                        }
                        @if (m.roleLabel) {
                          <div class="doc-member-role">{{ m.roleLabel }}</div>
                        }
                      </div>
                    </div>
                  }
                </div>
              </div>
            }
          </section>

          <div class="doc-divider"></div>

          <!-- NARRATIVA DE EVOLUCIÓN -->
          @if (hasMembersWithNarrative()) {
            <section class="doc-section">
              <h2 class="doc-section-title">✍️ Narrativa de Evolución</h2>
              @for (genGroup of membersByGeneration(); track genGroup.gen) {
                @for (m of genGroup.members; track m.id) {
                  @if (m.story || m.valores || m.aprendizajes || m.legadoPersonal || m.erroresSuperados || m.tradiciones || m.misionesCumplidas) {
                    <div class="doc-narrative-entry">
                      <div class="doc-ne-header">
                        @if (m.photoUrl) {
                          <img [src]="m.photoUrl" [alt]="m.fullName" class="doc-ne-photo"/>
                        } @else {
                          <div class="doc-ne-avatar"
                            [style.background]="m.avatarColor || getGenMeta(m.generation).color">
                            {{ m.avatarInitials || initials(m) }}
                          </div>
                        }
                        <div>
                          <div class="doc-ne-name">{{ m.fullName }}</div>
                          <div class="doc-ne-gen" [style.color]="getGenMeta(m.generation).color">
                            {{ getGenMeta(m.generation).label }}
                            @if (m.birthYear) { · {{ m.birthYear }}{{ m.deathYear ? ' – ' + m.deathYear : '' }} }
                          </div>
                        </div>
                      </div>
                      @if (m.story)             { <p class="doc-ne-story"><em>{{ m.story }}</em></p> }
                      @if (m.valores)           { <p class="doc-ne-field"><strong>💎 Valores:</strong> {{ m.valores }}</p> }
                      @if (m.aprendizajes)      { <p class="doc-ne-field"><strong>📚 Aprendizajes:</strong> {{ m.aprendizajes }}</p> }
                      @if (m.erroresSuperados)  { <p class="doc-ne-field"><strong>🔄 Errores superados:</strong> {{ m.erroresSuperados }}</p> }
                      @if (m.tradiciones)       { <p class="doc-ne-field"><strong>🎎 Tradiciones:</strong> {{ m.tradiciones }}</p> }
                      @if (m.misionesCumplidas) { <p class="doc-ne-field"><strong>🎯 Misiones:</strong> {{ m.misionesCumplidas }}</p> }
                      @if (m.legadoPersonal)    { <p class="doc-ne-field doc-ne-legado"><strong>🏛️ Legado:</strong> {{ m.legadoPersonal }}</p> }
                    </div>
                  }
                }
              }
            </section>
            <div class="doc-divider"></div>
          }

          <!-- COMPROMISOS / PRINCIPIOS RECTORES -->
          @if (legadoData()?.guidelines?.length) {
            <section class="doc-section">
              <h2 class="doc-section-title">📜 Principios Rectores</h2>
              <ol class="doc-guidelines">
                @for (g of legadoData()!.guidelines; track $index) {
                  <li>{{ g }}</li>
                }
              </ol>
            </section>
            <div class="doc-divider"></div>
          }

          <!-- PIE DEL DOCUMENTO -->
          <div class="doc-footer">
            <div class="doc-footer-seal">🌳</div>
            <p>Documento generado por <strong>Integrity Family</strong> · {{ currentYear }}</p>
            <p class="doc-footer-code">{{ lineage()?.lineageCode }}</p>
          </div>

        </div><!-- /doc-content -->
      </div><!-- /doc-modal -->
    </div><!-- /doc-overlay -->
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
    /* Barra de completitud global en header */
    .lh-completeness { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
    .lhc-bar-wrap { flex: 1; max-width: 180px; height: 5px; background: #1f2937; border-radius: 99px; overflow: hidden; }
    .lhc-bar  { height: 100%; border-radius: 99px; transition: width .5s ease; }
    .lhc-label { font-size: 11px; color: #9ca3af; white-space: nowrap; }
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
      display: flex; height: calc(100vh - 209px); min-height: 456px; overflow: hidden;
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
    .sp-avatar-photo {
      object-fit: cover; border: 2px solid #fbbf24;
    }
    .sp-name     { font-size: 15px; font-weight: 700; color: #fbbf24; }
    .sp-gen-badge{ font-size: 11px; margin-top: 2px; font-weight: 600; }
    .anchor-tag  { margin-left: 6px; font-size: 10px; color: #d97706; }

    /* Couple header in side panel */
    .sp-couple-wrap {
      display: flex; align-items: center; gap: 8px;
      padding: 4px 0; width: 100%;
    }
    .sp-couple-unit {
      display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1;
    }
    .sp-couple-name { font-size: 11px; font-weight: 600; color: #fde68a; text-align: center; }
    .sp-couple-heart { font-size: 18px; flex-shrink: 0; }
    .sp-avatar-sm { width: 36px !important; height: 36px !important; font-size: 13px !important; }
    .sp-children-count { font-size: 10px; color: #86efac; margin-left: 8px; }

    /* Grow tree section inside capsule */
    .cap-grow-section {
      padding: 10px 12px; border-bottom: 1px solid #1f2937;
    }
    .cap-section-title { font-size: 10px; font-weight: 700; color: #6b7280;
      text-transform: uppercase; letter-spacing: .05em; margin-bottom: 8px; }
    .cap-grow-row { display: flex; gap: 6px; flex-wrap: wrap; }
    .cap-grow-row .btn-quick { flex: 1; min-width: 70px; justify-content: center;
      padding: 7px 8px; font-size: 11px; }
    .cap-children-count {
      font-size: 11px; color: #4ade80; margin-top: 7px;
    }

    .cap-family-section {
      padding: 10px 12px; border-bottom: 1px solid #1f2937;
      display: flex; flex-direction: column; gap: 6px;
    }
    .cap-fam-row {
      display: flex; align-items: flex-start; gap: 8px; min-height: 22px;
    }
    .cap-fam-lbl {
      font-size: 10px; font-weight: 700; color: #6b7280; text-transform: uppercase;
      letter-spacing: .05em; min-width: 60px; padding-top: 3px; flex-shrink: 0;
    }
    .cap-fam-names { display: flex; flex-wrap: wrap; gap: 4px; }
    .cap-fam-chip {
      font-size: 11px; padding: 2px 8px; border-radius: 12px;
      background: #1f2937; border: 1px solid #374151; color: #d1d5db;
      cursor: pointer; transition: all .15s;
    }
    .cap-fam-chip:hover { background: #fbbf2420; border-color: #fbbf24; color: #fbbf24; }

    .sp-status       { font-size: 11px; color: #9ca3af; margin-top: 4px; }
    .sp-linked       { font-size: 11px; color: #6366f1; margin-top: 4px;
      display: flex; flex-direction: column; gap: 1px; }
    .sp-linked span  { font-weight: 600; color: #a5b4fc; }
    .sp-linked-email { font-weight: 400 !important; color: #6b7280 !important; font-size: 10px !important; }
    /* Badge de vínculo en el modal */
    .link-badge { font-size: 11px; color: #6ee7b7; margin-top: 6px;
      background: #064e3b20; border: 1px solid #065f4640; border-radius: 6px; padding: 4px 8px; }
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
    /* ── CÁPSULA DE VIDA ──────────────────────────────── */
    .capsule-header {
      display: flex; flex-direction: column; align-items: center;
      gap: 6px; padding: 16px 12px 12px; border-bottom: 1px solid #1f2937;
      text-align: center;
    }
    .ch-name { font-size: 14px; font-weight: 700; color: #fbbf24; }
    .ch-gen  { font-size: 11px; font-weight: 600; }
    .ch-completeness { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
    .ch-ring {
      width: 44px; height: 44px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
    }
    .ch-ring-inner {
      width: 32px; height: 32px; border-radius: 50%;
      background: #0d1117;
      display: flex; align-items: center; justify-content: center;
      font-size: 9px; font-weight: 700; color: #fbbf24;
    }
    .ch-ring-lbl { font-size: 10px; color: #6b7280; }

    .capsule-grid { display: flex; flex-direction: column; gap: 2px; padding: 8px 8px; }
    .cap-link {
      display: flex; align-items: center; gap: 10px;
      padding: 9px 10px; border-radius: 8px; cursor: pointer;
      background: transparent; border: none; color: #d1d5db;
      font-size: 12px; text-align: left; transition: background .15s;
      width: 100%;
    }
    .cap-link:hover { background: #1f2937; color: #fbbf24; }
    .cap-icon { font-size: 16px; width: 22px; text-align: center; flex-shrink: 0; }
    .cap-label { flex: 1; }
    .cap-arrow { color: #4b5563; font-size: 11px; }
    .cap-link:hover .cap-arrow { color: #fbbf24; }
    .cap-link-highlight { background: #1c1006; color: #fbbf24; }
    .cap-link-highlight:hover { background: #2d1a00; }

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

    /* Quick-add family buttons */
    .sp-quick-add {
      display: flex; flex-direction: column; gap: 6px;
      padding: 10px 14px; border-top: 1px solid #1f2937;
    }
    .btn-quick {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 12px; border-radius: 8px; font-size: 12px; font-weight: 600;
      border: 1px solid; cursor: pointer; transition: all .18s; text-align: left;
    }
    .btn-quick.spouse {
      background: #1c1006; border-color: #f59e0b40; color: #fbbf24;
    }
    .btn-quick.spouse:hover { background: #2d1a00; border-color: #f59e0b; }
    .btn-quick.child {
      background: #0a1a0a; border-color: #22c55e40; color: #86efac;
    }
    .btn-quick.child:hover { background: #0d2a0d; border-color: #22c55e; }

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
    .zb-divider { width: 1px; height: 20px; background: #374151; margin: 0 4px; }
    .zb-gen-mode {
      width: auto !important; padding: 0 10px !important; font-size: 11px !important;
      color: #9ca3af;
    }
    .zb-gen-mode.active {
      background: #1c2a0a !important; border-color: #4ade80 !important; color: #86efac !important;
    }
    .zb-reorg.active {
      background: #1a1a2e !important; border-color: #818cf8 !important; color: #a5b4fc !important;
    }
    .zb-search-wrap {
      position: relative; margin-left: 4px;
    }
    .zb-search {
      height: 28px; padding: 0 10px; border-radius: 6px;
      background: #1f2937; border: 1px solid #374151;
      color: #d1d5db; font-size: 11px; width: 180px;
      outline: none; transition: border-color .15s;
    }
    .zb-search:focus { border-color: #fbbf24; }
    .zb-search-dropdown {
      position: absolute; top: calc(100% + 4px); left: 0; z-index: 200;
      background: #111827; border: 1px solid #374151; border-radius: 8px;
      min-width: 200px; overflow: hidden;
      box-shadow: 0 8px 24px #00000080;
    }
    .zb-search-item {
      display: flex; align-items: center; justify-content: space-between;
      width: 100%; padding: 7px 12px; background: transparent;
      border: none; color: #d1d5db; font-size: 12px; cursor: pointer;
      text-align: left; transition: background .12s;
    }
    .zb-search-item:hover { background: #1f2937; color: #fbbf24; }
    .zb-si-name { font-weight: 600; }
    .zb-si-gen  { font-size: 10px; color: #6b7280; }

    @keyframes pulse-ring {
      0%   { r: 0; opacity: .9; }
      100% { r: 28; opacity: 0; }
    }
    .search-pulse-ring { animation: pulse-ring 1.2s ease-out infinite; }

    /* ── GENERATIONAL MODE BAR ──────────────────────────── */
    .gen-mode-bar {
      display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
      padding: 8px 20px; background: #0a1a0a;
      border-top: 1px solid #1a2e1a; border-bottom: 1px solid #1a2e1a;
    }
    .gmb-hint {
      flex: 1; font-size: 11px; color: #4ade80;
      display: flex; align-items: center; gap: 6px;
    }
    .gmb-hint.gmb-done { color: #6b7280; }
    .gmb-dot { font-size: 10px; animation: pulse-dot 1.5s ease-in-out infinite; }
    @keyframes pulse-dot { 0%,100% { opacity: .4; } 50% { opacity: 1; } }
    .ges-btn {
      padding: 5px 14px; border-radius: 20px; font-size: 11px; font-weight: 700; cursor: pointer;
      background: #0a2a0a; border: 1px solid #22c55e60; color: #4ade80;
      transition: all .2s; white-space: nowrap;
    }
    .ges-btn:hover { background: #0d3a0d; border-color: #22c55e; box-shadow: 0 0 12px #22c55e30; }
    .gcs-btn {
      padding: 4px 14px; border-radius: 20px; font-size: 10px; cursor: pointer;
      background: transparent; border: 1px solid #374151; color: #6b7280;
      transition: all .2s; white-space: nowrap;
    }
    .gcs-btn:hover { border-color: #9ca3af; color: #d1d5db; }

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
    .ct-header   { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 20px; }
    .ct-header h3{ color: #fbbf24; font-size: 18px; margin: 0 0 2px; }
    .ct-sub      { color: #6b7280; font-size: 12px; }
    .empty-msg   { color: #4b5563; font-style: italic; font-size: 13px; }

    /* Generation context block */
    .gen-context-block {
      border-left: 3px solid; padding: 12px 16px; margin-bottom: 16px;
      background: #111827; border-radius: 0 8px 8px 0;
    }
    /* ── Legado tab enhancements ─────────────────────── */
    .lb-cta      { margin-top: 12px; font-size: 12px; padding: 8px 18px; }
    .mv-row      { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; }
    @media(max-width:640px){ .mv-row { grid-template-columns: 1fr; } }
    .mv-block    { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06);
                   border-radius: 12px; padding: 14px; }
    .mv-bhead   { font-size: 12px; font-weight: 700; color: #fbbf24; margin-bottom: 8px; }
    .mv-btext   { font-size: 12px; color: #9ca3af; line-height: 1.6; margin: 0; }
    .vals-section{ margin-bottom: 20px; }
    .vals-title  { font-size: 13px; font-weight: 700; color: #fff; margin-bottom: 10px; }
    .vals-grid   { display: flex; flex-wrap: wrap; gap: 8px; }
    .val-chip    { display: flex; align-items: center; gap: 6px; background: rgba(251,191,36,0.06);
                   border: 1px solid rgba(251,191,36,0.15); border-radius: 20px;
                   padding: 5px 12px; font-size: 12px; color: #e5e7eb; }
    .val-icon    { font-size: 16px; }
    .val-name    { font-weight: 600; }
    .lc-sec-title{ font-size: 13px; font-weight: 700; color: rgba(255,255,255,0.5);
                   margin-bottom: 12px; letter-spacing: 0.05em; text-transform: uppercase; }
    /* ── Gen context blocks ──────────────────────────── */
    .gcb-header  { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
    .gcb-label   { font-weight: 700; font-size: 13px; }
    .gcb-actions { display: flex; align-items: center; gap: 8px; }
    .gcb-period  { font-size: 11px; color: #6b7280; }
    .gcb-edit-btn{ background: none; border: none; cursor: pointer; font-size: 14px; padding: 2px 4px;
                   border-radius: 6px; opacity: 0.5; transition: opacity 0.2s; }
    .gcb-edit-btn:hover { opacity: 1; background: rgba(255,255,255,0.06); }
    .gcb-text    { font-size: 12px; color: #9ca3af; margin: 4px 0; }
    .gcb-context { color: #6b7280; font-style: italic; }
    .gcb-kv      { font-size: 12px; color: #d1d5db; margin-top: 4px; }
    .gcb-kv span { color: #d97706; margin-right: 4px; }
    .gcb-empty   { opacity: 0.5; }
    .gcb-empty-hint { font-size: 11px; color: #4b5563; font-style: italic; margin: 4px 0 0; }

    /* Timeline */
    .timeline  { padding-left: 80px; }
    .tl-item   { display: flex; gap: 16px; align-items: flex-start;
      position: relative; margin-bottom: 20px; cursor: pointer; padding: 6px 8px;
      border-radius: 6px; transition: background .15s; }
    .tl-item:hover { background: #1f2937; }
    .tl-item:hover .tl-edit-btn { opacity: 1; }
    .tl-year   { position: absolute; left: -80px; width: 68px;
      text-align: right; font-size: 12px; font-weight: 600; color: #d97706; padding-top: 3px; }
    .tl-dot    { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; }
    .tl-body   { flex: 1; }
    .tl-member { font-size: 10px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px; }
    .tl-title  { font-size: 13px; font-weight: 600; color: #fde68a; display: flex; align-items: center; gap: 6px; }
    .tl-type-badge { font-size: 14px; }
    .tl-desc   { font-size: 12px; color: #9ca3af; margin-top: 4px; }
    .tl-edit-btn { background: none; border: none; cursor: pointer; font-size: 13px;
      opacity: 0; transition: opacity .15s; padding: 2px 4px; align-self: flex-start; }

    .tl-edit-btn.doc-btn { opacity: 1 !important; font-size: 14px; padding: 4px 10px; border-radius: 8px; background: rgba(124,58,237,0.15); border: 1px solid rgba(124,58,237,0.4); margin-top: 8px; color: #c4b5fd; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; } .tl-edit-btn.doc-btn:hover { background: rgba(124,58,237,0.3); transform: scale(1.05); color: #fff; } /* Narrative */
    .vision-block {
      display: flex; gap: 12px; align-items: flex-start;
      background: #111827; border-left: 3px solid #f59e0b;
      padding: 14px 16px; border-radius: 0 8px 8px 0; margin-bottom: 24px;
    }
    .vision-icon { font-size: 24px; }
    .vision-block p { color: #fde68a; font-style: italic; font-size: 14px; line-height: 1.7; margin: 0; }
    .gen-narrative-section { margin-bottom: 28px; }
    .gns-title { font-size: 13px; font-weight: 700; text-transform: uppercase;
      margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid #1f2937;
      display: flex; justify-content: space-between; align-items: center; }
    .gns-count { font-size: 11px; font-weight: 400; color: #6b7280; text-transform: none; }
    .narrative-card {
      background: #111827; border: 1px solid #1f2937;
      border-radius: 12px; padding: 14px; margin-bottom: 12px;
    }
    .nc-header { display: flex; gap: 10px; align-items: flex-start; margin-bottom: 8px; }
    .nc-photo  { width: 44px; height: 44px; border-radius: 50%; object-fit: cover;
      border: 2px solid #fbbf24; flex-shrink: 0; }
    .nc-meta   { flex: 1; min-width: 0; }
    .nc-edit-btn { background: none; border: none; cursor: pointer; font-size: 14px;
      opacity: .5; transition: opacity .2s; padding: 2px 4px; }
    .nc-edit-btn:hover { opacity: 1; }
    /* Barra de completitud por miembro */
    .nc-completeness-wrap { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
    .nc-completeness-bar  { flex: 1; height: 3px; background: #1f2937; border-radius: 99px; overflow: hidden; max-width: 80px; }
    .nc-completeness-fill { height: 100%; border-radius: 99px; transition: width .4s; }
    .nc-completeness-pct  { font-size: 10px; font-weight: 600; }
    /* Tarjeta vacía */
    .nc-empty   { opacity: .6; cursor: pointer; border-style: dashed; }
    .nc-empty:hover { opacity: 1; border-color: #fbbf24; }
    .nc-avatar-dim { opacity: .7; }
    .nc-empty-hint { font-size: 11px; color: #6b7280; margin-top: 2px; }
    /* Legado personal destacado */
    .nc-legado  { border-top: 1px solid #1f2937; padding-top: 6px; margin-top: 8px; }
    .nc-legado span { color: #fbbf24 !important; }
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

    /* ── TAB ESTADÍSTICAS ───────────────────────────── */
    .stats-tab { padding: 20px 16px; display: flex; flex-direction: column; gap: 24px; }

    .st-kpi-row {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
    }
    .st-kpi {
      background: #111827; border: 1px solid #1f2937; border-radius: 12px;
      padding: 16px 12px; text-align: center;
    }
    .st-kpi-num { font-size: 28px; font-weight: 800; color: #fbbf24; line-height: 1; }
    .st-kpi-lbl { font-size: 11px; color: #6b7280; margin-top: 4px; }

    .st-section { display: flex; flex-direction: column; gap: 10px; }
    .st-section-title {
      font-size: 12px; font-weight: 700; color: #9ca3af;
      text-transform: uppercase; letter-spacing: .06em;
    }

    .st-bar-list  { display: flex; flex-direction: column; gap: 8px; }
    .st-bar-item  { display: flex; align-items: center; gap: 10px; }
    .st-bar-lbl   { font-size: 12px; color: #d1d5db; min-width: 72px; }
    .st-bar-track {
      flex: 1; height: 8px; background: #1f2937; border-radius: 4px; overflow: hidden;
    }
    .st-bar-fill  { height: 100%; border-radius: 4px; transition: width .5s ease; }
    .st-alive     { background: #4ade80; }
    .st-deceased  { background: #6b7280; }
    .st-unknown   { background: #374151; }
    .st-bar-val   { font-size: 12px; color: #9ca3af; min-width: 24px; text-align: right; }

    .st-gen-bars { display: flex; flex-direction: column; gap: 6px; }
    .st-gen-row  { display: flex; align-items: center; gap: 10px; }
    .st-gen-lbl  { font-size: 11px; color: #9ca3af; min-width: 160px; }
    .st-gen-track {
      flex: 1; height: 10px; background: #1f2937; border-radius: 5px; overflow: hidden;
    }
    .st-gen-fill  { height: 100%; border-radius: 5px; transition: width .5s ease; opacity: .85; }
    .st-gen-count { font-size: 12px; color: #d1d5db; min-width: 24px; text-align: right; }

    .st-compl-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
    .st-compl-card {
      background: #0f172a; border: 1px solid; border-radius: 10px;
      padding: 12px 8px; text-align: center;
    }
    .st-compl-num { font-size: 24px; font-weight: 800; line-height: 1; }
    .st-compl-lbl { font-size: 10px; color: #6b7280; margin-top: 4px; line-height: 1.3; }

    .st-top-list { display: flex; flex-direction: column; gap: 6px; }
    .st-top-item {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 12px; background: #111827; border: 1px solid #1f2937;
      border-radius: 10px; cursor: pointer; transition: border-color .15s;
    }
    .st-top-item:hover { border-color: #fbbf24; }
    .st-top-avatar {
      width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; font-weight: 700; color: #fffbeb;
    }
    .st-top-info   { flex: 1; }
    .st-top-name   { font-size: 13px; font-weight: 600; color: #f3f4f6; }
    .st-top-gen    { font-size: 10px; color: #6b7280; margin-top: 2px; }
    .st-top-pct    {
      font-size: 16px; font-weight: 800; color: #4ade80;
    }

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

    /* ── BTN DOCUMENTO ──────────────────────────────── */
    .btn-doc { border-color: #6366f1 !important; color: #a5b4fc !important; }
    .btn-doc:hover { background: #6366f120 !important; }
    .btn-sm { padding: 6px 14px !important; font-size: 12px !important; }

    /* ── DOCUMENTO OVERLAY ──────────────────────────── */
    .doc-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,.85);
      display: flex; align-items: flex-start; justify-content: center;
      z-index: 2000; overflow-y: auto; padding: 24px 16px;
    }
    .doc-modal {
      background: #fff; color: #1a1a2e; border-radius: 12px;
      width: 100%; max-width: 820px; box-shadow: 0 32px 80px rgba(0,0,0,.6);
      overflow: hidden; margin: auto;
    }
    .doc-toolbar {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 20px; background: #1e1b4b; color: #e0e7ff;
      position: sticky; top: 0; z-index: 1;
    }
    .doc-toolbar-title { font-size: 14px; font-weight: 600; }
    .doc-toolbar-actions { display: flex; gap: 8px; }

    /* ── DOCUMENTO CONTENIDO ─────────────────────────── */
    .doc-content { padding: 48px 56px; font-family: 'Georgia', serif; }

    /* PORTADA */
    .doc-cover { text-align: center; padding: 20px 0 32px; }
    .doc-cover-seal {
      width: 64px; height: 64px; border-radius: 50%; background: #1e1b4b;
      color: #fff; font-size: 22px; font-weight: 900; display: inline-flex;
      align-items: center; justify-content: center; margin-bottom: 16px;
      border: 3px solid #4338ca;
    }
    .doc-title   { font-size: 28px; font-weight: 700; color: #1e1b4b; margin: 0 0 6px; }
    .doc-subtitle { font-size: 16px; color: #4338ca; font-weight: 600; margin-bottom: 4px; }
    .doc-code    { font-size: 11px; color: #9ca3af; letter-spacing: .1em; margin-bottom: 16px; }
    .doc-vision  { font-style: italic; color: #374151; border-left: 3px solid #4338ca;
      margin: 16px auto; max-width: 500px; padding: 8px 16px; text-align: left; font-size: 15px; }
    .doc-tagline { font-size: 13px; color: #6b7280; margin-top: 8px; }
    .doc-year    { font-size: 12px; color: #9ca3af; margin-top: 16px; }

    .doc-divider { border: none; border-top: 2px solid #e5e7eb; margin: 28px 0; }

    /* SECCIONES */
    .doc-section { margin-bottom: 12px; }
    .doc-section-title {
      font-size: 16px; font-weight: 700; color: #1e1b4b;
      border-bottom: 2px solid #4338ca; padding-bottom: 6px; margin-bottom: 16px;
      font-family: 'Inter', sans-serif;
    }

    /* MISIÓN / VISIÓN */
    .doc-mv-block { margin-bottom: 16px; }
    .doc-mv-label { font-size: 10px; font-weight: 700; letter-spacing: .12em;
      color: #4338ca; margin-bottom: 4px; font-family: 'Inter', sans-serif; }
    .doc-mv-text  { font-size: 14px; color: #374151; line-height: 1.7; margin: 0; }

    /* VALORES */
    .doc-values-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
    .doc-value-card  { background: #f5f3ff; border: 1px solid #c7d2fe; border-radius: 8px;
      padding: 12px; text-align: center; }
    .doc-value-icon  { font-size: 22px; margin-bottom: 4px; }
    .doc-value-name  { font-size: 13px; font-weight: 700; color: #1e1b4b; }
    .doc-value-desc  { font-size: 11px; color: #6b7280; margin-top: 4px; line-height: 1.5; }

    /* ÁRBOL */
    .doc-tree-stats  { font-size: 12px; color: #6b7280; margin-bottom: 20px;
      font-family: 'Inter', sans-serif; }
    .doc-gen-block   { margin-bottom: 20px; }
    .doc-gen-header  { display: flex; align-items: baseline; gap: 10px;
      border-left: 3px solid; padding-left: 10px; margin-bottom: 6px; }
    .doc-gen-label   { font-size: 13px; font-weight: 700; font-family: 'Inter', sans-serif; }
    .doc-gen-period  { font-size: 11px; color: #9ca3af; }
    .doc-gen-context { font-size: 12px; color: #6b7280; margin: 0 0 10px 13px; font-style: italic; }
    .doc-members-row { display: flex; flex-wrap: wrap; gap: 8px; padding-left: 13px; }
    .doc-member-chip { display: flex; align-items: center; gap: 8px;
      background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 6px 10px; }
    .doc-member-photo { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
    .doc-member-avatar { width: 32px; height: 32px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 700; color: #fff; flex-shrink: 0; }
    .doc-member-name  { font-size: 12px; font-weight: 600; color: #1f2937;
      font-family: 'Inter', sans-serif; }
    .doc-member-years { font-size: 10px; color: #9ca3af; }
    .doc-member-role  { font-size: 10px; color: #6b7280; }

    /* NARRATIVA */
    .doc-narrative-entry { margin-bottom: 20px; padding: 14px 16px;
      border: 1px solid #e5e7eb; border-radius: 8px; break-inside: avoid; }
    .doc-ne-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .doc-ne-photo  { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 2px solid #c7d2fe; }
    .doc-ne-avatar { width: 40px; height: 40px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: 700; color: #fff; flex-shrink: 0; }
    .doc-ne-name   { font-size: 14px; font-weight: 700; color: #1e1b4b;
      font-family: 'Inter', sans-serif; }
    .doc-ne-gen    { font-size: 11px; font-family: 'Inter', sans-serif; }
    .doc-ne-story  { font-size: 13px; color: #374151; line-height: 1.7; margin-bottom: 6px; }
    .doc-ne-field  { font-size: 12px; color: #4b5563; margin-top: 4px; }
    .doc-ne-legado { border-top: 1px solid #e5e7eb; padding-top: 6px; margin-top: 8px; }

    /* PRINCIPIOS */
    .doc-guidelines { padding-left: 20px; }
    .doc-guidelines li { font-size: 13px; color: #374151; margin-bottom: 8px; line-height: 1.6; }

    /* PIE */
    .doc-footer { text-align: center; padding: 20px 0 0; color: #9ca3af; font-size: 12px;
      font-family: 'Inter', sans-serif; }
    .doc-footer-seal { font-size: 24px; margin-bottom: 6px; }
    .doc-footer strong { color: #4338ca; }
    .doc-footer-code  { letter-spacing: .1em; font-size: 10px; margin-top: 2px; }

    /* ── PRINT ──────────────────────────────────────── */
    @media print {
      .doc-overlay, .doc-modal { position: static !important; }
      .doc-toolbar { display: none !important; }
      .doc-content { padding: 20px 32px; }
      .doc-values-grid { grid-template-columns: repeat(3, 1fr); }
    }
  `]
})
export class LineagePageComponent implements OnInit, OnDestroy {
  private svc         = inject(LineageService);
  private familyState = inject(FamilyStateService);
  private http        = inject(HttpClient);
  private apiSvc      = inject(ApiService);
  private router      = inject(Router);

  // ── Legado familiar (cargado cuando se activa el tab) ─────────────────────
  legadoData  = signal<any>(null);
  legadoVals  = signal<any[]>([]);

  // ── Miembros registrados de la familia (para vincular) ────────────────────
  registeredMembers = signal<{ id: number; fullName: string; firstName: string | null;
    email: string | null; role: string | null; age: number | null }[]>([]);

  lineage        = signal<Lineage | null>(null);
  loading        = signal(true);
  error          = signal<string | null>(null);
  noLineage      = signal(false);
  activeTab      = signal<TreeTab>('tree');
  memberTab      = signal<MemberTab>('bio');
  selectedId     = signal<number | null>(null);
  selectedMember = signal<LineageMember | null>(null);
  genFilter         = signal<number | null>(null);
  genMode               = signal(false);
  expandedParentIds     = signal<Set<number>>(new Set());
  ignoreStoredPositions = signal(true);
  memberSearch          = signal('');
  highlightedId     = signal<number | null>(null);

  searchResults = computed<LineageMember[]>(() => {
    const q = this.memberSearch().trim().toLowerCase();
    if (q.length < 2) return [];
    return (this.lineage()?.members ?? [])
      .filter(m => m.fullName.toLowerCase().includes(q))
      .slice(0, 8);
  });

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

  // ── Quick-add (cónyuge / hijo / hija) ───────────────────────────────────
  private quickAddSource: LineageMember | null = null;
  private quickAddType: 'spouse' | 'son' | 'daughter' | null = null;

  hasSpouse(m: LineageMember): boolean {
    return (this.lineage()?.relationships ?? [])
      .some(r => r.isCouple && (r.fromMemberId === m.id || r.toMemberId === m.id));
  }

  quickAdd(source: LineageMember, type: 'spouse' | 'son' | 'daughter') {
    this.quickAddSource = source;
    this.quickAddType   = type;
    this.editingMember.set(null);
    const childGen = (source.generation ?? 0) + 1;
    this.form = {
      generation:     type === 'spouse' ? (source.generation ?? 0) : childGen,
      status:         'alive',
      confidenceLevel: 80,
      roleLabel: type === 'spouse' ? 'Cónyuge'
               : type === 'son'   ? 'Hijo'
               : 'Hija'
    };
    this.formTab = 'bio';
    this.showModal.set(true);
  }

  selectedMemberSpouse = computed<LineageMember | null>(() => {
    const m = this.selectedMember();
    if (!m) return null;
    const rels = this.lineage()?.relationships ?? [];
    const rel  = rels.find(r => r.isCouple && (r.fromMemberId === m.id || r.toMemberId === m.id));
    if (!rel) return null;
    const spouseId = rel.fromMemberId === m.id ? rel.toMemberId : rel.fromMemberId;
    return (this.lineage()?.members ?? []).find(mb => mb.id === spouseId) ?? null;
  });

  memberChildrenCount(m: LineageMember): number {
    const rels     = this.lineage()?.relationships ?? [];
    const spouseId = (this.lineage()?.relationships ?? [])
      .find(r => r.isCouple && (r.fromMemberId === m.id || r.toMemberId === m.id))
      ?.fromMemberId === m.id
        ? rels.find(r => r.isCouple && r.fromMemberId === m.id)?.toMemberId
        : rels.find(r => r.isCouple && r.toMemberId === m.id)?.fromMemberId;
    const parentIds = new Set([m.id, ...(spouseId ? [spouseId] : [])]);
    return rels.filter(r => !r.isCouple && parentIds.has(r.fromMemberId)).length;
  }

  // ── Couple connectors for SVG visual ────────────────────────────────────
  coupleConnectors = computed<{ id: string; x1: number; x2: number; y: number; mx: number }[]>(() => {
    const rels = this.lineage()?.relationships ?? [];
    const mMap = new Map(this.positionedMembers().map(m => [m.id, m]));
    const seen = new Set<number>();
    return rels
      .filter(r => r.isCouple)
      .map(r => {
        if (seen.has(r.id)) return null;
        seen.add(r.id);
        const a = mMap.get(r.fromMemberId);
        const b = mMap.get(r.toMemberId);
        if (!a || !b) return null;
        const x1 = Math.min(a.px, b.px);
        const x2 = Math.max(a.px, b.px);
        // Solo mostrar conector si los cónyuges están realmente adyacentes (< 200px)
        if (x2 - x1 > 200) return null;
        return { id: `cp-${r.id}`, x1, x2, y: a.py, mx: (x1 + x2) / 2 };
      })
      .filter(Boolean) as { id: string; x1: number; x2: number; y: number; mx: number }[];
  });

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

  minGen = computed<number>(() => {
    const gens = (this.lineage()?.members ?? []).map(m => m.generation);
    return gens.length ? Math.min(...gens) : 0;
  });

  filteredMembers = computed<LineageMember[]>(() => {
    const members = this.lineage()?.members ?? [];
    const rels    = this.lineage()?.relationships ?? [];
    const f = this.genFilter();
    if (f !== null) return members.filter(m => m.generation === f);
    if (!this.genMode()) return members;

    // genMode: show root gen + children/spouses of expanded parents
    const expanded = this.expandedParentIds();
    const minGen   = this.minGen();

    // child → parent IDs
    const parentOf = new Map<number, number[]>();
    rels.filter(r => !r.isCouple).forEach(r => {
      if (!parentOf.has(r.toMemberId)) parentOf.set(r.toMemberId, []);
      parentOf.get(r.toMemberId)!.push(r.fromMemberId);
    });
    // couple map
    const coupleOf = new Map<number, number>();
    rels.filter(r => r.isCouple).forEach(r => {
      coupleOf.set(r.fromMemberId, r.toMemberId);
      coupleOf.set(r.toMemberId, r.fromMemberId);
    });

    const visible = new Set<number>();
    members.filter(m => m.generation === minGen).forEach(m => visible.add(m.id));

    members.forEach(m => {
      const parents = parentOf.get(m.id) ?? [];
      if (parents.some(pid => expanded.has(pid))) {
        visible.add(m.id);
        const spouseId = coupleOf.get(m.id);
        if (spouseId) visible.add(spouseId);
      }
    });

    return members.filter(m => visible.has(m.id));
  });

  /** Members with unexpanded children — used for ▼ badge on nodes */
  hasUnexpandedChildren = computed<Set<number>>(() => {
    if (!this.genMode()) return new Set();
    const rels     = this.lineage()?.relationships ?? [];
    const expanded = this.expandedParentIds();
    const visible  = new Set(this.filteredMembers().map(m => m.id));
    const result   = new Set<number>();
    // couple map to also flag spouses
    const coupleOf = new Map<number, number>();
    rels.filter(r => r.isCouple).forEach(r => {
      coupleOf.set(r.fromMemberId, r.toMemberId);
      coupleOf.set(r.toMemberId, r.fromMemberId);
    });
    rels.filter(r => !r.isCouple).forEach(r => {
      if (visible.has(r.fromMemberId) && !expanded.has(r.fromMemberId)) {
        result.add(r.fromMemberId);
        const sp = coupleOf.get(r.fromMemberId);
        if (sp) result.add(sp);
      }
    });
    return result;
  });

  /** Count of hidden children per parent (for badge numbers) */
  hiddenChildrenCount = computed<Map<number, number>>(() => {
    if (!this.genMode()) return new Map();
    const rels     = this.lineage()?.relationships ?? [];
    const expanded = this.expandedParentIds();
    const visible  = new Set(this.filteredMembers().map(m => m.id));
    const counts   = new Map<number, number>();
    rels.filter(r => !r.isCouple).forEach(r => {
      if (visible.has(r.fromMemberId) && !expanded.has(r.fromMemberId)) {
        counts.set(r.fromMemberId, (counts.get(r.fromMemberId) ?? 0) + 1);
      }
    });
    return counts;
  });

  /** Info sobre nodos expandibles restantes en el árbol visible */
  unexpandedNodeCount = computed<number>(() => this.hasUnexpandedChildren().size);

  lineageStats = computed(() => {
    const members = this.lineage()?.members ?? [];
    const rels    = this.lineage()?.relationships ?? [];

    const totalMembers    = members.length;
    const totalCouples    = rels.filter(r => r.isCouple).length;
    const alive           = members.filter(m => m.status === 'alive').length;
    const deceased        = members.filter(m => m.status === 'deceased').length;
    const unknown         = totalMembers - alive - deceased;

    const gens = [...new Set(members.map(m => m.generation))];
    const totalGenerations = gens.length;

    const byGeneration = [...gens].sort((a, b) => a - b)
      .map(gen => ({ gen, count: members.filter(m => m.generation === gen).length }));
    const maxInGen = Math.max(...byGeneration.map(g => g.count), 1);

    const pcts    = members.map(m => this.completeness(m));
    const avgCompleteness = totalMembers
      ? Math.round(pcts.reduce((a, b) => a + b, 0) / totalMembers)
      : 0;

    const complBands = [
      { label: 'Completo (80–100%)',  color: '#4ade80', count: pcts.filter(p => p >= 80).length },
      { label: 'Bueno (50–79%)',      color: '#fbbf24', count: pcts.filter(p => p >= 50 && p < 80).length },
      { label: 'Parcial (20–49%)',    color: '#f97316', count: pcts.filter(p => p >= 20 && p < 50).length },
      { label: 'Mínimo (<20%)',       color: '#6b7280', count: pcts.filter(p => p < 20).length },
    ];

    const topDocumented = members
      .map(m => ({ member: m, pct: this.completeness(m) }))
      .sort((a, b) => b.pct - a.pct)
      .slice(0, 5);

    return { totalMembers, totalCouples, alive, deceased, unknown,
      totalGenerations, byGeneration, maxInGen,
      avgCompleteness, complBands, topDocumented };
  });

  /** Familia inferida para el miembro seleccionado (padres, hermanos, hijos, cónyuge) */
  inferredFamily = computed<{
    parents:      LineageMember[];
    siblings:     LineageMember[];
    children:     LineageMember[];
    grandparents: LineageMember[];
    spouse:       LineageMember | null;
  }>(() => {
    const sel     = this.selectedMember();
    if (!sel) return { parents: [], siblings: [], children: [], grandparents: [], spouse: null };
    const rels    = this.lineage()?.relationships ?? [];
    const allM    = this.lineage()?.members ?? [];
    const mMap    = new Map(allM.map(m => [m.id, m]));

    const coupleOf = new Map<number, number>();
    rels.filter(r => r.isCouple).forEach(r => {
      coupleOf.set(r.fromMemberId, r.toMemberId);
      coupleOf.set(r.toMemberId, r.fromMemberId);
    });

    // Cónyuge
    const spouseId = coupleOf.get(sel.id);
    const spouse   = spouseId ? (mMap.get(spouseId) ?? null) : null;

    // Hijos: nodos cuyo fromMemberId sea sel.id (o su cónyuge) y relación no-couple
    const selfAndSpouse = new Set([sel.id, ...(spouseId ? [spouseId] : [])]);
    const children = rels
      .filter(r => !r.isCouple && selfAndSpouse.has(r.fromMemberId))
      .map(r => mMap.get(r.toMemberId))
      .filter((m): m is LineageMember => !!m && m.id !== sel.id && m.id !== spouseId);

    // Padres: nodos cuyo toMemberId sea sel.id y relación no-couple
    const parents = rels
      .filter(r => !r.isCouple && r.toMemberId === sel.id)
      .map(r => mMap.get(r.fromMemberId))
      .filter((m): m is LineageMember => !!m);
    // Añadir cónyuge de cada padre (el otro padre)
    const parentSet = new Set(parents.map(p => p.id));
    parents.forEach(p => {
      const ps = coupleOf.get(p.id);
      if (ps && !parentSet.has(ps)) {
        const pm = mMap.get(ps);
        if (pm) { parents.push(pm); parentSet.add(ps); }
      }
    });

    // Hermanos: comparten al menos un padre con sel, y no son sel ni cónyuge
    const siblings: LineageMember[] = [];
    if (parents.length > 0) {
      const sibSet = new Set<number>();
      parents.forEach(p => {
        rels.filter(r => !r.isCouple && r.fromMemberId === p.id).forEach(r => {
          if (r.toMemberId !== sel.id && !sibSet.has(r.toMemberId)) {
            sibSet.add(r.toMemberId);
            const sm = mMap.get(r.toMemberId);
            if (sm) siblings.push(sm);
          }
        });
      });
    }

    // Abuelos: padres de los padres
    const grandparents: LineageMember[] = [];
    const gpSet = new Set<number>();
    parents.forEach(p => {
      rels.filter(r => !r.isCouple && r.toMemberId === p.id).forEach(r => {
        if (!gpSet.has(r.fromMemberId)) {
          gpSet.add(r.fromMemberId);
          const gm = mMap.get(r.fromMemberId);
          if (gm) grandparents.push(gm);
        }
      });
    });
    // Añadir cónyuge de cada abuelo
    grandparents.slice().forEach(gp => {
      const gs = coupleOf.get(gp.id);
      if (gs && !gpSet.has(gs)) {
        gpSet.add(gs);
        const gm = mMap.get(gs);
        if (gm) grandparents.push(gm);
      }
    });

    return { parents, siblings, children, grandparents, spouse };
  });

  positionedMembers = computed<(LineageMember & { px: number; py: number })[]>(() => {
    const members   = this.filteredMembers();
    const rels      = this.lineage()?.relationships ?? [];
    const w         = this.svgW();
    const overrides = this.dragOverride();
    const MARGIN    = 60;

    // Honour manual / drag overrides
    const manualPos = new Map<number, { px: number; py: number }>();
    members.forEach(m => {
      const drag = overrides.get(m.id);
      if (drag) { manualPos.set(m.id, drag); return; }
      if (m.positionX != null && m.positionY != null && (m.positionX !== 0 || m.positionY !== 0)) {
        manualPos.set(m.id, { px: m.positionX, py: m.positionY });
      }
    });

    // ── Constants ──────────────────────────────────────────
    const COUPLE_GAP = 56;   // px between spouses inside one unit
    const NODE_W     = 60;   // footprint of a single (non-couple) node
    const UNIT_GAP   = 80;   // spacing between sibling units (included in subtreeW)

    // ── Relationship maps ──────────────────────────────────
    const memberMap = new Map(members.map(m => [m.id, m]));
    const coupleOf  = new Map<number, number>();
    rels.filter(r => r.isCouple).forEach(r => {
      coupleOf.set(r.fromMemberId, r.toMemberId);
      coupleOf.set(r.toMemberId, r.fromMemberId);
    });
    const childrenOf = new Map<number, number[]>();
    rels.filter(r => !r.isCouple).forEach(r => {
      if (!childrenOf.has(r.fromMemberId)) childrenOf.set(r.fromMemberId, []);
      childrenOf.get(r.fromMemberId)!.push(r.toMemberId);
    });

    // ── Build TreeNodes (one node per couple or single) ───
    interface TN {
      id: string;
      primary: LineageMember; spouse?: LineageMember;
      parentId: string | null; childIds: string[];
      subtreeW: number; cx?: number;
    }
    const nodeMap = new Map<string, TN>();
    const genNodes = new Map<number, TN[]>();

    const grouped = new Map<number, LineageMember[]>();
    members.forEach(m => {
      if (!grouped.has(m.generation)) grouped.set(m.generation, []);
      grouped.get(m.generation)!.push(m);
    });
    const gens = [...grouped.keys()].sort((a, b) => a - b);

    // Create nodes
    for (const gen of gens) {
      const processed = new Set<number>();
      const nodes: TN[] = [];
      for (const m of grouped.get(gen)!) {
        if (processed.has(m.id)) continue;
        processed.add(m.id);
        const spId = coupleOf.get(m.id);
        const sp   = spId ? memberMap.get(spId) : undefined;
        let node: TN;
        if (sp && sp.generation === gen && !processed.has(sp.id)) {
          processed.add(sp.id);
          node = { id: `u${m.id}`, primary: m, spouse: sp, parentId: null, childIds: [], subtreeW: 0 };
        } else {
          node = { id: `u${m.id}`, primary: m, parentId: null, childIds: [], subtreeW: 0 };
        }
        nodes.push(node);
        nodeMap.set(node.id, node);
      }
      genNodes.set(gen, nodes);
    }

    // Link parent→child units
    for (const gen of gens) {
      const prevNodes = genNodes.get(gen - 1) ?? [];
      for (const node of genNodes.get(gen)!) {
        const memberIds = [node.primary.id, node.spouse?.id].filter(Boolean) as number[];
        for (const pn of prevNodes) {
          const parentIds = [pn.primary.id, pn.spouse?.id].filter(Boolean) as number[];
          const linked = parentIds.some(pid =>
            (childrenOf.get(pid) ?? []).some(cid => memberIds.includes(cid))
          );
          if (linked) { node.parentId = pn.id; pn.childIds.push(node.id); break; }
        }
      }
    }

    // ── Pass 1: compute subtreeW bottom-up ────────────────
    const ownW = (n: TN) => (n.spouse ? COUPLE_GAP : NODE_W) + UNIT_GAP;
    const computeW = (n: TN): number => {
      if (n.childIds.length === 0) { n.subtreeW = ownW(n); return n.subtreeW; }
      const kids = n.childIds.map(id => nodeMap.get(id)!);
      const kidsW = kids.reduce((s, k) => s + computeW(k), 0);
      n.subtreeW = Math.max(ownW(n), kidsW);
      return n.subtreeW;
    };
    const roots = genNodes.get(gens[0]) ?? [];
    // Orphan nodes in later gens also need their own subtree
    for (const gen of gens) {
      for (const n of genNodes.get(gen)!) {
        if (n.parentId === null && n.subtreeW === 0) computeW(n);
      }
    }
    roots.forEach(computeW);

    // ── Pass 2: assign X positions top-down ───────────────
    const autoPos    = new Map<number, { px: number; py: number }>();
    const setCx = (n: TN, sliceStart: number) => {
      const sliceW = n.subtreeW - UNIT_GAP;
      const cx     = sliceStart + sliceW / 2;
      n.cx = cx;
      const py = this.genY(n.primary.generation);
      if (n.spouse) {
        autoPos.set(n.primary.id, { px: cx - COUPLE_GAP / 2, py });
        autoPos.set(n.spouse.id,  { px: cx + COUPLE_GAP / 2, py });
      } else {
        autoPos.set(n.primary.id, { px: cx, py });
      }
      if (n.childIds.length === 0) return;
      const kids      = n.childIds.map(id => nodeMap.get(id)!);
      const kidsTotal = kids.reduce((s, k) => s + k.subtreeW, 0) - UNIT_GAP;
      let kx = sliceStart + (sliceW - kidsTotal) / 2;
      for (const k of kids) { setCx(k, kx); kx += k.subtreeW; }
    };

    // Position root row — each root gets a proportional slice of the canvas
    const totalRootW = roots.reduce((s, n) => s + n.subtreeW, 0) - UNIT_GAP;
    const availW     = w - MARGIN * 2;
    const scale      = totalRootW > 0 ? Math.min(1, availW / totalRootW) : 1;
    let rx = MARGIN + (availW - totalRootW * scale) / 2;
    for (const root of roots) {
      setCx(root, rx);
      rx += root.subtreeW * scale;
    }

    // Position orphan units: if orphan has a spouse that IS positioned, place adjacent
    // Otherwise center the remaining true orphans
    for (const gen of gens.slice(1)) {
      const orphans = (genNodes.get(gen) ?? []).filter(n => n.parentId === null && n.cx === undefined);
      if (!orphans.length) continue;

      // First pass: place spouse-orphans adjacent to their already-positioned spouse
      const trueOrphans: typeof orphans = [];
      for (const n of orphans) {
        const spouseId = n.spouse?.id ?? coupleOf.get(n.primary.id);
        let spouseNode: typeof orphans[0] | undefined;
        if (spouseId) {
          for (const [, sn] of nodeMap) {
            if ((sn.primary.id === spouseId || sn.spouse?.id === spouseId) && sn.cx !== undefined) {
              spouseNode = sn; break;
            }
          }
        }
        if (spouseNode && spouseNode.cx !== undefined) {
          // Place this orphan's primary member adjacent to the spouse node
          const py = this.genY(n.primary.generation);
          const spPx = autoPos.get(spouseId!)?.px ?? spouseNode.cx!;
          autoPos.set(n.primary.id, { px: spPx + COUPLE_GAP, py });
          n.cx = spPx + COUPLE_GAP / 2;
        } else {
          trueOrphans.push(n);
        }
      }

      // Second pass: center true orphans (no spouse relationship found)
      if (!trueOrphans.length) continue;
      const totalW = trueOrphans.reduce((s, n) => s + n.subtreeW, 0) - UNIT_GAP;
      let ox = (w - totalW) / 2;
      for (const n of trueOrphans) { setCx(n, ox); ox += n.subtreeW; }
    }

    const skipManual = this.ignoreStoredPositions();
    return members.map(m => {
      const manual = skipManual ? undefined : manualPos.get(m.id);
      if (manual) return { ...m, ...manual };
      const auto = autoPos.get(m.id);
      return { ...m, px: auto?.px ?? w / 2, py: auto?.py ?? this.genY(m.generation) };
    });
  });

  branchPaths = computed<{ id: string; d: string; isCouple: boolean; isThin?: boolean }[]>(() => {
    const rels = this.lineage()?.relationships ?? [];
    const mMap = new Map(this.positionedMembers().map(m => [m.id, m]));

    const coupleOf = new Map<number, number>();
    rels.filter(r => r.isCouple).forEach(r => {
      coupleOf.set(r.fromMemberId, r.toMemberId);
      coupleOf.set(r.toMemberId, r.fromMemberId);
    });

    // Group parent→child rels by parent unit (parent + spouse form one source point)
    type Group = { fromX: number; fromY: number; children: { px: number; py: number; relId: number }[] };
    const groups = new Map<string, Group>();

    rels.filter(r => !r.isCouple).forEach(r => {
      const from = mMap.get(r.fromMemberId);
      const to   = mMap.get(r.toMemberId);
      if (!from || !to) return;
      const spId  = coupleOf.get(from.id);
      const sp    = spId ? mMap.get(spId) : undefined;
      const fromX = sp ? (from.px + sp.px) / 2 : from.px;
      // Normalizar clave para que pareja A+B y B+A compartan el mismo grupo
      const a = from.id, b = spId ?? 0;
      const key = b ? `${Math.min(a, b)}:${Math.max(a, b)}` : `${a}:0`;
      if (!groups.has(key)) groups.set(key, { fromX, fromY: from.py, children: [] });
      else groups.get(key)!.fromX = fromX; // actualizar con midpoint más reciente
      // Evitar hijos duplicados (dos padres apuntan al mismo hijo)
      const existing = groups.get(key)!;
      if (!existing.children.some(c => c.px === to.px && c.py === to.py)) {
        existing.children.push({ px: to.px, py: to.py, relId: r.id });
      }
    });

    const paths: { id: string; d: string; isCouple: boolean; isThin?: boolean }[] = [];

    for (const [key, g] of groups) {
      if (!g.children.length) continue;
      const childY = g.children[0].py;
      const midY   = g.fromY + (childY - g.fromY) * 0.5;

      if (g.children.length === 1) {
        // Single child: straight vertical line (no T needed)
        const c = g.children[0];
        paths.push({ id: `sv-${key}`,
          d: `M ${g.fromX} ${g.fromY} L ${c.px} ${c.py}`, isCouple: false });
      } else {
        // T-junction: trunk → horizontal bar → drops
        const xs   = g.children.map(c => c.px);
        const minX = Math.min(...xs);
        const maxX = Math.max(...xs);

        // Vertical trunk: couple midpoint → horizontal bar
        paths.push({ id: `vt-${key}`,
          d: `M ${g.fromX} ${g.fromY} L ${g.fromX} ${midY}`, isCouple: false });

        // Horizontal bar spanning all siblings
        paths.push({ id: `hb-${key}`,
          d: `M ${minX} ${midY} L ${maxX} ${midY}`, isCouple: false, isThin: true });

        // Vertical drop from bar to each child
        g.children.forEach((c, i) => {
          paths.push({ id: `dr-${key}-${i}`,
            d: `M ${c.px} ${midY} L ${c.px} ${c.py}`, isCouple: false });
        });
      }
    }

    return paths;
  });

  allEvents = computed<{ event: any; memberId: number; memberName: string; memberColor: string; isDoc?: boolean; docId?: string }[]>(() => {
    const members = this.lineage()?.members ?? [];
    const memberEvents = members
      .flatMap(m => (m.events ?? []).map(ev => ({
        event: ev,
        memberId: m.id,
        memberName: m.fullName,
        memberColor: getGenMeta(m.generation).color,
        isDoc: false
      })))
      .filter(x => x.event.eventYear);

    const docEvents = this.documentaries().map(d => {
      const year = new Date(d.createdAt).getFullYear().toString();
      return {
        event: { id: 'doc-' + d.id, eventYear: year, title: d.title, description: 'Mini Documental (' + d.scope + ')', eventType: 'milestone' },
        memberId: 0,
        memberName: 'Memoria Familiar',
        memberColor: '#7c3aed',
        isDoc: true,
        docId: d.id
      };
    });

    return [...memberEvents, ...docEvents]
      .sort((a, b) => {
        const parseYear = (y: string) => parseInt(y.replace(/[^0-9-]/g, '')) || 0;
        return parseYear(a.event.eventYear ?? '0') - parseYear(b.event.eventYear ?? '0');
      });
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

  /** Calcula 0–100 de completitud para un miembro */
  completeness(m: LineageMember): number {
    const fields: (string | number | null | undefined | boolean)[] = [
      m.firstName, m.lastName,          // identidad básica (2)
      m.birthYear,                       // año nac (1)
      m.origin,                          // origen (1)
      m.photoUrl,                        // foto (1)
      m.story,                           // historia (1)
      m.valores,                         // valores (1)
      m.aprendizajes,                    // aprendizajes (1)
      m.erroresSuperados,                // errores superados (1)
      m.tradiciones,                     // tradiciones (1)
      m.misionesCumplidas,               // misiones (1)
      m.legadoPersonal,                  // legado personal (1)
    ];
    const filled = fields.filter(f => f !== null && f !== undefined && f !== '').length;
    return Math.round((filled / fields.length) * 100);
  }

  /** Color del anillo de completitud */
  completenessColor(m: LineageMember): string {
    const pct = this.completeness(m);
    if (pct >= 75) return '#22c55e';   // verde
    if (pct >= 40) return '#f59e0b';   // amarillo
    return '#ef4444';                  // rojo
  }

  /** % global del linaje */
  globalCompleteness = computed(() => {
    const members = this.lineage()?.members ?? [];
    if (!members.length) return 0;
    return Math.round(members.reduce((sum, m) => sum + this.completeness(m), 0) / members.length);
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit() {
    this.loadLineage();
    this.loadLegado();
    this.loadRegisteredMembers();
    this.loadDocumentaries();
  }

  loadLegado() {
    const fid = this.familyId;
    if (!fid) return;
    this.http.get<any>(`${this.apiSvc.base}/families/${fid}/legacy`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        if (!res) return;
        this.legadoData.set(res.legacy ?? res);
        this.legadoVals.set(res.values ?? []);
      });
  }

  documentaries = signal<any[]>([]);

  loadDocumentaries() {
    const fid = this.familyId;
    if (!fid) return;

    // Modern
    this.http.get<any>(`/api/documentary-productions/family/${fid}`)
      .pipe(catchError(() => of({ data: [] })))
      .subscribe(res => {
        const current = this.documentaries();
        this.documentaries.set([...current, ...(res.data || [])]);
      });

    // Legacy
    this.http.get<any>(`${this.apiSvc.base}/evidences/family/${fid}`)
      .pipe(catchError(() => of({ data: [] })))
      .subscribe(res => {
        const allLegacy = res.data || [];
        const masters = allLegacy.filter((e: any) =>
          e.evidenceType === 'BITACORA' && e.title?.startsWith('🎬 Mini Documental:')
        );
        const legacyDocs = masters.map((master: any) => ({
          id: master.id.toString(),
          createdAt: master.createdAt,
          title: master.title.replace('🎬 Mini Documental:', '').trim(),
          scope: 'LEGACY'
        }));
        const current = this.documentaries();
        this.documentaries.set([...current, ...legacyDocs]);
      });
  }

  viewDocumentary(docId: string) {
    this.router.navigate(['/evidence/capture'], { queryParams: { doc: docId } });
  }

  goToLegado() { this.router.navigate(['/legado']); }

  loadRegisteredMembers() {
    const fid = this.familyId;
    if (!fid) return;
    this.http.get<any>(`${this.apiSvc.base}/members/family/${fid}`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        if (!res) return;
        const list = Array.isArray(res) ? res : (res.data ?? []);
        this.registeredMembers.set(list);
      });
  }

  /** Devuelve el miembro registrado vinculado a un LineageMember */
  linkedMember(familyMemberId: number | null) {
    if (!familyMemberId) return null;
    return this.registeredMembers().find(m => m.id === familyMemberId) ?? null;
  }

  /** Al seleccionar un miembro registrado, auto-rellena nombre y rol si están vacíos */
  onRegisteredMemberSelect(regMemberId: number | string) {
    const id = typeof regMemberId === 'string' ? parseInt(regMemberId) : regMemberId;
    if (!id) return;
    const reg = this.registeredMembers().find(m => m.id === id);
    if (!reg) return;
    // Solo auto-rellena si el campo está vacío
    if (!this.form['firstName'] && reg.firstName) this.form['firstName'] = reg.firstName;
    if (!this.form['lastName']  && reg.fullName) {
      const parts = reg.fullName.trim().split(' ');
      if (parts.length > 1 && !this.form['firstName']) this.form['firstName'] = parts[0];
      if (!this.form['lastName']) this.form['lastName'] = parts.slice(1).join(' ');
    }
    if (!this.form['roleLabel'] && reg.role) this.form['roleLabel'] = reg.role;
    this.form['familyMemberId'] = id;
  }

  ngOnDestroy() {
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.dragId() != null)        { this.cancelDrag(); return; }
    if (this.showDocument())          { this.closeDocument(); return; }
    if (this.showEventModal())        { this.closeEventModal(); return; }
    if (this.showModal())             { this.closeModal(); return; }
    if (this.showEditLineageModal())  { this.showEditLineageModal.set(false); return; }
    if (this.showGenInfoModal())      { this.showGenInfoModal.set(false); return; }
    if (this.showConnectModal())      { this.showConnectModal.set(false); return; }
    if (this.deleteTarget())          { this.deleteTarget.set(null); return; }
    if (this.selectedMember())        { this.clearSelection(); }
  }

  // ── Gestión de Eventos desde la Línea de Tiempo ──────────────────────────
  showEventModal = signal(false);
  editingEvent   = signal<{ event: any; memberId: number } | null>(null);
  eventForm: {
    memberId?: number;
    eventType: string;
    title: string;
    eventYear?: string;
    description?: string;
    isApproximate: boolean;
  } = { eventType: 'milestone', title: '', isApproximate: false };

  openAddEvent() {
    this.editingEvent.set(null);
    this.eventForm = { eventType: 'milestone', title: '', isApproximate: false };
    this.showEventModal.set(true);
  }

  openEditEvent(ev: { event: any; memberId: number; memberName: string; memberColor: string }) {
    this.editingEvent.set({ event: ev.event, memberId: ev.memberId });
    this.eventForm = {
      memberId:      ev.memberId,
      eventType:     ev.event.eventType ?? 'milestone',
      title:         ev.event.title,
      eventYear:     ev.event.eventYear ?? undefined,
      description:   ev.event.description ?? undefined,
      isApproximate: !!ev.event.isApproximate,
    };
    this.showEventModal.set(true);
  }

  closeEventModal() { this.showEventModal.set(false); this.editingEvent.set(null); }

  saveEvent() {
    if (!this.eventForm.title || !this.eventForm.memberId) return;
    this.saving.set(true);
    const req = {
      title:         this.eventForm.title,
      eventType:     this.eventForm.eventType,
      eventYear:     this.eventForm.eventYear,
      description:   this.eventForm.description,
      isApproximate: this.eventForm.isApproximate,
    };
    const editing = this.editingEvent();
    const obs = editing
      ? this.svc.updateEvent(this.familyId, editing.memberId, editing.event.id, req)
      : this.svc.addEvent(this.familyId, this.eventForm.memberId, req);

    obs.pipe(catchError(() => {
      this.saving.set(false);
      this.showToast('Error al guardar el evento', true);
      return of(null);
    })).subscribe(saved => {
      if (!saved) return;
      this.saving.set(false);
      this.closeEventModal();
      this.showToast(editing ? 'Evento actualizado' : 'Evento creado');
      // Actualización optimista en el signal local
      const lin = this.lineage();
      if (!lin) return;
      this.lineage.set({
        ...lin,
        members: lin.members.map(m => {
          if (m.id !== (editing?.memberId ?? this.eventForm.memberId)) return m;
          const events = m.events ?? [];
          if (editing) {
            return { ...m, events: events.map(e => e.id === saved.id ? saved : e) };
          } else {
            return { ...m, events: [...events, saved] };
          }
        })
      });
    });
  }

  confirmDeleteEvent() {
    const editing = this.editingEvent();
    if (!editing) return;
    this.saving.set(true);
    this.svc.deleteEvent(this.familyId, editing.memberId, editing.event.id)
      .pipe(catchError(() => {
        this.saving.set(false);
        this.showToast('Error al eliminar el evento', true);
        return of(null);
      })).subscribe(() => {
        this.saving.set(false);
        this.closeEventModal();
        this.showToast('Evento eliminado');
        const lin = this.lineage();
        if (!lin) return;
        this.lineage.set({
          ...lin,
          members: lin.members.map(m =>
            m.id !== editing.memberId ? m
              : { ...m, events: (m.events ?? []).filter(e => e.id !== editing.event.id) }
          )
        });
      });
  }

  eventTypeLabel(type: string): string {
    const map: Record<string, string> = {
      milestone: '🏁', birth: '🐣', death: '🕊️',
      marriage: '💍', migration: '✈️', achievement: '🏆', trauma: '⚡'
    };
    return map[type] ?? '📌';
  }

  // ── Documento de Constitución Familiar ────────────────────────────────────
  showDocument = signal(false);
  currentYear  = new Date().getFullYear();

  openDocument()  { this.showDocument.set(true); }
  closeDocument() { this.showDocument.set(false); }

  printDocument() {
    // Imprime solo el área del documento usando estilos de @media print
    const style = document.createElement('style');
    style.id = 'print-override';
    style.textContent = `
      @media print {
        body > * { display: none !important; }
        body > app-root { display: block !important; }
        app-root > * { display: none !important; }
        app-root lin-page, app-root .lin-page,
        .doc-overlay, .doc-modal { display: block !important; position: static !important; }
        .doc-toolbar { display: none !important; }
        .lin-page > *:not(.doc-overlay) { display: none !important; }
      }
    `;
    document.head.appendChild(style);
    window.print();
    setTimeout(() => { style.remove(); }, 1000);
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
    this.memberTab.set('capsule');
    this.addingEvent.set(false);
    this.quickEvent = { title: '' };

    // In genMode: auto-expand children of clicked member
    if (this.genMode() && this.hasUnexpandedChildren().has(m.id)) {
      this.expandedParentIds.update(set => {
        const next = new Set(set);
        next.add(m.id);
        // Also expand spouse so both parents' children are visible
        const rels = this.lineage()?.relationships ?? [];
        const spouseRel = rels.find(r => r.isCouple &&
          (r.fromMemberId === m.id || r.toMemberId === m.id));
        if (spouseRel) {
          next.add(spouseRel.fromMemberId === m.id ? spouseRel.toMemberId : spouseRel.fromMemberId);
        }
        return next;
      });
    }
  }

  navigate(path: string) { this.router.navigate([path]); }

  clearSelection() { this.selectedId.set(null); this.selectedMember.set(null); }

  filterByGen(gen: number) {
    this.genFilter.update(cur => cur === gen ? null : gen);
  }

  toggleGenMode() {
    const next = !this.genMode();
    this.genMode.set(next);
    if (!next) this.expandedParentIds.set(new Set());
  }

  expandAllVisible() {
    const unexpanded = this.hasUnexpandedChildren();
    this.expandedParentIds.update(set => {
      const next = new Set(set);
      unexpanded.forEach(id => next.add(id));
      return next;
    });
  }

  collapseAll() {
    this.expandedParentIds.set(new Set());
  }

  // ── Búsqueda de miembros ──────────────────────────────────────────────────
  onMemberSearch(e: Event) {
    this.memberSearch.set((e.target as HTMLInputElement).value);
    this.highlightedId.set(null);
  }

  clearSearch() {
    this.memberSearch.set('');
    this.highlightedId.set(null);
  }

  jumpToMember(m: LineageMember) {
    this.memberSearch.set('');
    this.highlightedId.set(m.id);
    // Select member and open its capsule
    this.selectedId.set(m.id);
    this.selectedMember.set(m);
    this.memberTab.set('capsule');
    // In genMode, expand up the ancestor chain so the member is visible
    if (this.genMode()) {
      const rels = this.lineage()?.relationships ?? [];
      const coupleOf = new Map<number, number>();
      rels.filter(r => r.isCouple).forEach(r => {
        coupleOf.set(r.fromMemberId, r.toMemberId);
        coupleOf.set(r.toMemberId, r.fromMemberId);
      });
      const parentOf = new Map<number, number[]>();
      rels.filter(r => !r.isCouple).forEach(r => {
        if (!parentOf.has(r.toMemberId)) parentOf.set(r.toMemberId, []);
        parentOf.get(r.toMemberId)!.push(r.fromMemberId);
      });
      // Walk ancestors and expand them all
      const toExpand = new Set<number>(this.expandedParentIds());
      const walk = (id: number) => {
        const parents = parentOf.get(id) ?? [];
        parents.forEach(pid => {
          toExpand.add(pid);
          const sp = coupleOf.get(pid);
          if (sp) toExpand.add(sp);
          walk(pid);
        });
      };
      walk(m.id);
      this.expandedParentIds.set(toExpand);
    }
    // Center SVG view on the found member node
    const pm = this.positionedMembers().find(p => p.id === m.id);
    if (pm) {
      const z   = this.svgZoom();
      const vbW = this.svgW() / z;
      const vbH = this.svgH() / z;
      this.vbOffX.set(pm.px - vbW / 2);
      this.vbOffY.set(pm.py - vbH / 2 - 60);
    }
    // Clear highlight after a moment
    setTimeout(() => this.highlightedId.set(null), 2500);
  }

  jumpToSearchResult() {
    const results = this.searchResults();
    if (results.length === 1) this.jumpToMember(results[0]);
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
          // Auto-link when coming from quickAdd
          const qSrc  = this.quickAddSource;
          const qType = this.quickAddType;
          if (!em && saved.id && qSrc?.id && qType) {
            this.quickAddSource = null;
            this.quickAddType   = null;
            const isCouple    = qType === 'spouse';
            const relType     = isCouple ? 'couple' : 'biological';
            const fromId      = isCouple ? qSrc.id : qSrc.id; // parent is source for children
            const toId        = saved.id;
            this.svc.addRelationship(this.familyId, fromId, toId, relType, isCouple)
              .pipe(catchError(() => of(null)))
              .subscribe(() => {
                this.loadLineage();
                this.closeModal();
                const label = qType === 'spouse' ? 'cónyuge' : qType === 'son' ? 'hijo' : 'hija';
                this.showToast(`✅ ${saved.firstName || 'Miembro'} agregado como ${label}`);
                this.saving.set(false);
              });
            return;
          }
          this.quickAddSource = null;
          this.quickAddType   = null;
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
  /** Retorna el GenerationInfo de un nivel, o null si no existe */
  genInfoFor(level: number): GenerationInfo | null {
    return this.lineage()?.generationInfos?.find(i => i.generationLevel === level) ?? null;
  }

  openGenInfo(level?: number) {
    const genLevel = level ?? (this.lineage()?.anchorGeneration ?? 0);
    const existing = this.genInfoFor(genLevel);
    if (existing) {
      this.genInfoForm = {
        generationLevel: existing.generationLevel,
        generationType:  existing.generationType ?? undefined,
        title:           existing.title ?? undefined,
        summary:         existing.summary ?? undefined,
        context:         existing.context ?? undefined,
        keyChallenge:    existing.keyChallenge ?? undefined,
        keyAchievement:  existing.keyAchievement ?? undefined,
        periodStart:     existing.periodStart ?? undefined,
        periodEnd:       existing.periodEnd ?? undefined,
      };
    } else {
      this.genInfoForm = { generationLevel: genLevel };
    }
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

// force rebuild
