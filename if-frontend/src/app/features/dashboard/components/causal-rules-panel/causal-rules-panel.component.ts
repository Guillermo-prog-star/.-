import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CausalInferenceDTO } from '../../../../core/models/dashboard.model';

interface RuleDisplay {
  code: string;
  label: string;
  icon: string;
  color: string;
  explanation: string;
}

/**
 * CausalRulesPanelComponent — Explicabilidad del Motor Inferencial Causal.
 *
 * Muestra qué reglas causales (R1-R7) están activas, por qué se dispararon
 * y qué significan para la familia.
 *
 * Cierra el requisito de "explicabilidad" de la arquitectura epistemológica:
 *   "el sistema debe poder explicar por qué el riesgo es el que es"
 *
 * Solo se muestra cuando hay reglas activas (sistema en estado de alerta).
 * En estado estable (sin reglas), permanece oculto.
 */
@Component({
  selector: 'app-causal-rules-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Solo visible cuando el Motor Inferencial tiene reglas activas -->
    @if (visible) {
    <div class="causal-panel glass-premium rounded-3xl border border-white/5 overflow-hidden animate-in fade-in slide-in-from-bottom-4 duration-700">

      <!-- Header -->
      <div class="p-5 border-b border-white/5" [style.background]="headerBg">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center text-lg flex-shrink-0"
               [style.background]="iconBg">
            ⚙️
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-[10px] font-black uppercase tracking-widest text-white/30 mb-0.5">
              Motor Inferencial Causal · IF-CAUSAL
            </div>
            <div class="text-sm font-bold text-white">
              {{ activeRules.length }} Regla{{ activeRules.length > 1 ? 's' : '' }} Activa{{ activeRules.length > 1 ? 's' : '' }}
              <span class="ml-2 text-[10px] font-normal px-2 py-0.5 rounded-full"
                    [ngClass]="requiresIntervention ? 'bg-red-500/20 text-red-400' : 'bg-amber-500/20 text-amber-400'">
                {{ requiresIntervention ? '🆘 INTERVENCIÓN REQUERIDA' : '⚠️ MONITOREO ACTIVO' }}
              </span>
            </div>
          </div>
          <!-- Indicador pulsante -->
          <div class="w-2.5 h-2.5 rounded-full animate-ping flex-shrink-0"
               [ngClass]="requiresIntervention ? 'bg-red-500' : 'bg-amber-500'">
          </div>
        </div>
      </div>

      <!-- Reglas activas -->
      <div class="p-4 space-y-3">
        @for (rule of activeRules; track rule.code) {
        <div class="flex items-start gap-3 p-3.5 rounded-2xl border transition-all"
             [style.background]="rule.color + '0a'"
             [style.border-color]="rule.color + '30'">
          <div class="text-xl flex-shrink-0 mt-0.5">{{ rule.icon }}</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full"
                    [style.color]="rule.color"
                    [style.background]="rule.color + '18'">
                {{ rule.code }}
              </span>
              <span class="text-xs font-semibold text-white/80">{{ rule.label }}</span>
            </div>
            <p class="text-[11px] text-white/50 leading-relaxed">{{ rule.explanation }}</p>
          </div>
        </div>
        }
      </div>

      <!-- Explicaciones adicionales -->
      @if (explanations.length > 0) {
      <div class="px-4 pb-4">
        <div class="p-3 rounded-xl bg-white/[0.02] border border-white/5">
          <div class="text-[9px] font-black uppercase tracking-widest text-white/20 mb-2">
            Análisis causal detallado
          </div>
          @for (exp of explanations; track exp) {
          <div class="text-[11px] text-white/50 mb-1 flex items-start gap-1.5">
            <span class="text-white/20 flex-shrink-0 mt-0.5">›</span>
            <span>{{ exp }}</span>
          </div>
          }
        </div>
      </div>
      }

      <!-- Dimensión crítica -->
      @if (criticalDimension) {
      <div class="px-4 pb-4">
        <div class="flex items-center gap-2 text-[11px] text-white/40">
          <span class="text-base">🎯</span>
          <span>Dimensión más crítica: <strong class="text-white/70 capitalize">{{ criticalDimension }}</strong></span>
        </div>
      </div>
      }

    </div>
    }

    <!-- Estado estable — sin reglas activas -->
    @if (!visible && showStableState) {
    <div class="flex items-center gap-3 px-4 py-3 rounded-2xl bg-emerald-500/5 border border-emerald-500/10">
      <span class="text-base">✅</span>
      <div>
        <div class="text-[10px] font-bold text-emerald-500 uppercase tracking-wider">Motor Inferencial · Sistema Estable</div>
        <div class="text-[11px] text-white/30">Ninguna regla causal activa · La familia está en equilibrio dinámico</div>
      </div>
    </div>
    }
  `,
  styles: [`.causal-panel { transition: all 0.3s ease; }`]
})
export class CausalRulesPanelComponent implements OnChanges {

  @Input() inference: CausalInferenceDTO | null = null;
  @Input() showStableState = false; // mostrar estado estable cuando no hay reglas

  activeRules: RuleDisplay[] = [];
  explanations: string[] = [];
  criticalDimension = '';
  requiresIntervention = false;
  visible = false;
  headerBg = 'rgba(239,68,68,0.06)';
  iconBg   = 'rgba(239,68,68,0.15)';

  // Metadata de cada regla causal del Motor Inferencial
  private readonly RULE_META: Record<string, Omit<RuleDisplay, 'explanation' | 'code'>> = {
    'R1:JOURNAL_DETERIORATION_PATTERN': {
      label: 'Patrón de Deterioro en Bitácora',
      icon: '📉',
      color: '#f59e0b'
    },
    'R2:ACTIVE_CRISIS': {
      label: 'Crisis Activa (< 48h)',
      icon: '🆘',
      color: '#ef4444'
    },
    'R3:EVOLUTION_MILESTONE': {
      label: 'Hito Evolutivo Alcanzado',
      icon: '🌱',
      color: '#10b981'
    },
    'R4:INACTIVITY_LATENT_RISK': {
      label: 'Riesgo Latente por Inactividad',
      icon: '⏱️',
      color: '#f59e0b'
    },
    'R5:DIM_CRITICAL': {
      label: 'Dimensión en Colapso Crítico',
      icon: '🔴',
      color: '#ef4444'
    },
    'R6:COMMUNICATION_COLLAPSE': {
      label: 'Colapso Comunicacional',
      icon: '💬',
      color: '#f43f5e'
    },
    'R7:LONGITUDINAL_DETERIORATION': {
      label: 'Deterioro Longitudinal Sostenido',
      icon: '📊',
      color: '#f97316'
    }
  };

  ngOnChanges(_: SimpleChanges) {
    const inf = this.inference;
    if (!inf) { this.visible = false; return; }

    const rules = inf.activeRules ?? [];
    const exps  = inf.explanations ?? [];

    this.visible                = rules.length > 0;
    this.criticalDimension      = inf.criticalDimension ?? '';
    this.requiresIntervention   = inf.communicationCollapseActive ||
                                  ['ALTO','CRITICO'].includes(inf.inferredRiskLevel ?? '');

    // Construir display de reglas
    this.activeRules = rules.map((code, i) => {
      const meta = this.RULE_META[code] ?? { label: code, icon: '⚠️', color: '#f59e0b' };
      return { code, ...meta, explanation: exps[i] ?? '' } as RuleDisplay;
    });

    this.explanations = exps.filter((_, i) => i >= rules.length); // exps extras

    // Color del header según severidad
    const hasCritical = rules.some(r => r.includes('CRISIS') || r.includes('CRITICAL') || r.includes('DIM_CRITICAL'));
    this.headerBg = hasCritical ? 'rgba(239,68,68,0.06)' : 'rgba(245,158,11,0.06)';
    this.iconBg   = hasCritical ? 'rgba(239,68,68,0.15)' : 'rgba(245,158,11,0.15)';
  }
}
