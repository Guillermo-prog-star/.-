package com.integrityfamily.capital.service;

import com.integrityfamily.capital.service.IcafScoringEngine.IcafDomains;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resuelve el valor (0-100) de cada uno de los 11 dominios del ICaF
 * consultando las fuentes de datos existentes en el sistema.
 *
 * Estado de implementación por sprint:
 *
 *   S2:
 *     cohesion     → FamilyLongitudinalState.icfCurrent           [COMPLETO]
 *     integracion  → estimado: consciousness_level + inactividad   [ESTIMADO]
 *     resiliencia  → estimado: inversión de crisis recientes        [ESTIMADO]
 *     comunicacion → estimado: dim_comunicacion del ICF             [ESTIMADO]
 *     bienestar    → estimado: dim_emociones del ICF                [ESTIMADO — S2]
 *     confianza    → valor por defecto 50.0                         [PENDIENTE — S2]
 *
 *   S3:
 *     confianza         → cuestionario ICAF_CONF_001..007           [COMPLETO]
 *     bienestar         → cuestionario ICAF_BIEN_001..007           [COMPLETO]
 *     (fallback a estimación si la familia no ha respondido aún)
 *
 *   S4 (actual):
 *     resiliencia       → IcafResilienciaEngine (FamilyCriticalEvents)[COMPLETO]
 *     (fallback a estimación longitudinal si sin eventos aún)
 *
 * Los valores estimados son mejores que 0 — evitan sesgar el ICaF
 * inicial hacia abajo hasta que los dominios tengan fuente propia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcafDomainResolver {

    private static final double DEFAULT_DOMAIN_SCORE = 50.0;

    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final FamilyCapitalSnapshotRepository snapshotRepo;
    private final FamilyIcafAnswerRepository icafAnswerRepo;
    private final IcafResilienciaEngine resilienciaEngine;

    public IcafDomains resolve(Long familyId) {
        Optional<FamilyLongitudinalState> stateOpt = longitudinalRepo.findByFamilyId(familyId);

        if (stateOpt.isEmpty()) {
            log.warn("[ICaF-Resolver] Sin estado longitudinal para familia {} — usando defaults", familyId);
            return defaultDomains();
        }

        FamilyLongitudinalState state = stateOpt.get();

        double cohesion      = resolveCohesion(state);
        double comunicacion  = resolveComunicacion(state);
        double resiliencia   = resilienciaEngine.compute(familyId, state);
        double integracion   = resolveIntegracion(state);

        // S3: dominios con cuestionario propio (fallback a estimación si sin respuestas)
        double confianza     = resolveConfianza(familyId, state);
        double bienestar     = resolveBienestar(familyId, state);

        // S5: dominios con cuestionario propio (fallback a DEFAULT si sin respuestas)
        double autonomia      = resolveFromQuestionnaire(familyId, IcafQuestionnaireService.DOMAIN_AUTONOMIA);
        double proposito      = resolveFromQuestionnaire(familyId, IcafQuestionnaireService.DOMAIN_PROPOSITO);
        double emprendimiento = resolveFromQuestionnaire(familyId, IcafQuestionnaireService.DOMAIN_EMPRENDIMIENTO);
        double legado         = resolveFromQuestionnaire(familyId, IcafQuestionnaireService.DOMAIN_LEGADO);
        double madurezScore   = resolveMadurezScore(state);

        log.debug("[ICaF-Resolver] Familia {} | cohesion={} confianza={} bienestar={} resiliencia={} integracion={}",
                familyId,
                String.format("%.1f", cohesion),
                String.format("%.1f", confianza),
                String.format("%.1f", bienestar),
                String.format("%.1f", resiliencia),
                String.format("%.1f", integracion));

        return new IcafDomains(
                cohesion, confianza, resiliencia, comunicacion,
                autonomia, bienestar, proposito, integracion,
                emprendimiento, legado, madurezScore
        );
    }

    // ── Resolvers por dominio ─────────────────────────────────────────────────

    /** Dominio 1: ICF actual (fuente principal del ICaF en S2) */
    private double resolveCohesion(FamilyLongitudinalState state) {
        if (state.getIcfCurrent() != null && state.getIcfCurrent() > 0) {
            return clamp(state.getIcfCurrent());
        }
        return DEFAULT_DOMAIN_SCORE;
    }

    /** Dominio 4: calidad de comunicación — usa dim_comunicacion del ICF */
    private double resolveComunicacion(FamilyLongitudinalState state) {
        if (state.getDimComunicacion() != null && state.getDimComunicacion() > 0) {
            return clamp(state.getDimComunicacion());
        }
        return DEFAULT_DOMAIN_SCORE;
    }

    /**
     * Dominio 2: confianza — cuestionario ICAF_CONF_001..007 (S3).
     * Fallback: estimación desde dim_comunicacion si la familia no ha respondido.
     */
    private double resolveConfianza(Long familyId, FamilyLongitudinalState state) {
        double rawAvg = icafAnswerRepo.avgScoreByDomain(familyId, IcafQuestionnaireService.DOMAIN_CONFIANZA);
        if (rawAvg > 0) {
            // Promedio 1-5 → 0-100 (dirección ya normalizada en saveAnswers)
            // avgScoreByDomain devuelve promedio crudo 1-5; normalizamos aquí
            return clamp((rawAvg - 1.0) / 4.0 * 100.0);
        }
        // Fallback: aproximación desde comunicación (correlacionadas)
        if (state.getDimComunicacion() != null && state.getDimComunicacion() > 0) {
            return clamp(state.getDimComunicacion() * 0.85);
        }
        return DEFAULT_DOMAIN_SCORE;
    }

    /**
     * Dominio 6: bienestar emocional — cuestionario ICAF_BIEN_001..007 (S3).
     * Fallback: estimación desde dim_emociones si la familia no ha respondido.
     */
    private double resolveBienestar(Long familyId, FamilyLongitudinalState state) {
        double rawAvg = icafAnswerRepo.avgScoreByDomain(familyId, IcafQuestionnaireService.DOMAIN_BIENESTAR);
        if (rawAvg > 0) {
            return clamp((rawAvg - 1.0) / 4.0 * 100.0);
        }
        // Fallback: dim_emociones del ICF
        if (state.getDimEmociones() != null && state.getDimEmociones() > 0) {
            return clamp(state.getDimEmociones());
        }
        return DEFAULT_DOMAIN_SCORE;
    }

    /**
     * Dominios con cuestionario propio sin fallback inferencial.
     * Si la familia no ha respondido, retorna DEFAULT_DOMAIN_SCORE (50.0).
     */
    private double resolveFromQuestionnaire(Long familyId, String domain) {
        double rawAvg = icafAnswerRepo.avgScoreByDomain(familyId, domain);
        if (rawAvg > 0) {
            return clamp((rawAvg - 1.0) / 4.0 * 100.0);
        }
        return DEFAULT_DOMAIN_SCORE;
    }

    /**
     * Dominio 8: integración — % de actividad familiar.
     * Estimado desde el nivel de consciencia y la inactividad acumulada.
     */
    private double resolveIntegracion(FamilyLongitudinalState state) {
        int consciousnessLevel = state.getConsciousnessLevel() != null ? state.getConsciousnessLevel() : 3;
        int inactivityDays     = state.getInactivityDays()     != null ? state.getInactivityDays()     : 0;
        // consciousnessLevel 1=Plena (100) … 5=Inconsciente (20)
        double base = switch (consciousnessLevel) {
            case 1 -> 90.0;
            case 2 -> 75.0;
            case 3 -> 60.0;
            case 4 -> 40.0;
            default -> 25.0;
        };
        // Descuento por inactividad: -2 por cada día, máx -30
        double descuento = Math.min(inactivityDays * 2.0, 30.0);
        return clamp(base - descuento);
    }

    /**
     * Dominio 11: madurez normalizada (0-100).
     * Convierte el nivel 1-5 a una escala porcentual.
     */
    private double resolveMadurezScore(FamilyLongitudinalState state) {
        if (state.getIcfCurrent() == null) return DEFAULT_DOMAIN_SCORE;
        int nivel = IcafScoringEngine.computeMadurez(state.getIcfCurrent());
        // 1→20, 2→40, 3→60, 4→80, 5→100
        return nivel * 20.0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private IcafDomains defaultDomains() {
        return new IcafDomains(
                DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE,
                DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE,
                DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE,
                DEFAULT_DOMAIN_SCORE, DEFAULT_DOMAIN_SCORE
        );
    }
}
