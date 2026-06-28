package com.integrityfamily.capital.service;

import com.integrityfamily.capital.dto.IndicatorResult;
import com.integrityfamily.capital.dto.IndicatorsSnapshot;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyBehavioralEventRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyGratitudeEntryRepository;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import com.integrityfamily.domain.repository.SprintDailyRepository;
import com.integrityfamily.domain.repository.SprintMissionRepository;
import com.integrityfamily.domain.repository.SprintRetrospectiveRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Calcula los 20 indicadores del Sistema de Medición del Fortalecimiento Familiar (SMFF).
 *
 * IND-01, 05, 06, 13-16, 17-20 delegan en servicios ya implementados (ICF, ICaF, resiliencia).
 * IND-02, 03, 04, 07, 08, 09, 10, 11, 12 se calculan directamente aquí desde repositorios.
 *
 * Todos los indicadores se normalizan a 0–100. Cuando no hay datos reales se retorna
 * un fallback (50.0 por defecto) marcado con isEstimated=true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyIndicatorsService {

    // ── Repositorios ──────────────────────────────────────────────────────────
    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final SprintDailyRepository             sprintDailyRepo;
    private final FamilyGratitudeEntryRepository    gratitudeRepo;
    private final FamilyBehavioralEventRepository   behavioralRepo;
    private final FamilyIcafAnswerRepository        icafAnswerRepo;
    private final MemberRepository                  memberRepo;
    private final TaskEvidenceRepository            evidenceRepo;
    private final SprintMissionRepository           missionRepo;
    private final FamilySprintRepository            sprintRepo;
    private final SprintRetrospectiveRepository     retroRepo;
    private final EvaluationRepository              evaluationRepo;
    private final PlanTaskRepository                planTaskRepo;

    private final FamilyCriticalEventRepository criticalEventRepo;

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final int  GRATITUD_OBJETIVO_30D = 8;   // ≥2 expresiones/semana
    private static final int  EVALUACIONES_OBJETIVO_90D = 2;
    private static final long RECOVERY_UMBRAL_OPTIMO  = 60;
    private static final long RECOVERY_UMBRAL_CRITICO = 180;

    // ── API pública ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IndicatorsSnapshot computeAll(Long familyId) {
        log.debug("[SMFF] Calculando 20 indicadores para familia {}", familyId);

        FamilyLongitudinalState state = longitudinalRepo
                .findByFamilyId(familyId).orElse(null);

        List<IndicatorResult> results = new ArrayList<>(20);

        // ── Grupo 1: Cohesión y Vínculo ──────────────────────────────────────
        results.add(ind01_icf(state));
        results.add(ind02_constanciaDailies(familyId));
        results.add(ind03_frecuenciaGratitud(familyId));
        results.add(ind04_reparacionConductual(familyId));

        // ── Grupo 2: Confianza ───────────────────────────────────────────────
        results.add(ind05_confianzaInterpersonal(familyId));
        results.add(ind06_aperturaComunicacional(familyId));
        results.add(ind07_participacionMultimiembro(familyId));

        // ── Grupo 3: Transformación Activa ───────────────────────────────────
        results.add(ind08_completitudPlan(familyId));
        results.add(ind09_densidadEvidencias(familyId));
        results.add(ind10_momentumSprint(familyId));
        results.add(ind11_frecuenciaEvaluacion(familyId));
        results.add(ind12_impactoTareas(familyId));

        // ── Grupo 4: Resiliencia ─────────────────────────────────────────────
        results.add(ind13_tasaResolucion(familyId, state));
        results.add(ind14_velocidadRecuperacion(familyId, state));
        results.add(ind15_controlRecaidas(familyId, state));
        results.add(ind16_cargaCrisisActiva(familyId, state));

        // ── Grupo 5: Longitudinal ────────────────────────────────────────────
        results.add(ind17_deltaIcaf6m(state));
        results.add(ind18_deltaIcaf12m(state));
        results.add(ind19_velocidadMejora(familyId, state));
        results.add(ind20_madurezFamiliar(state));

        // ── Agregado SMFF ─────────────────────────────────────────────────────
        double smff = results.stream()
                .mapToDouble(IndicatorResult::value)
                .average()
                .orElse(0.0);

        long real = results.stream().filter(r -> !r.isEstimated()).count();
        double completePct = round2((double) real / results.size() * 100);

        return new IndicatorsSnapshot(familyId, LocalDateTime.now(),
                round2(smff), results, (int) real, completePct);
    }

    // ── IND-01: ICF ──────────────────────────────────────────────────────────

    private IndicatorResult ind01_icf(FamilyLongitudinalState state) {
        if (state == null || state.getIcfCurrent() == null) {
            return IndicatorResult.estimated("IND-01", "Índice de Cohesión (ICF)", "cohesion", "RESULTADO", 50.0);
        }
        double icf = state.getIcfCurrent();
        return IndicatorResult.real("IND-01", "Índice de Cohesión (ICF)", "cohesion", "RESULTADO",
                round2(icf), round2(icf), "%", 1);
    }

    // ── IND-02: Constancia en Dailies ────────────────────────────────────────

    private IndicatorResult ind02_constanciaDailies(Long familyId) {
        var sprints = sprintRepo.findActiveAndCompletedByFamilyId(familyId);
        if (sprints.isEmpty()) {
            return IndicatorResult.estimated("IND-02", "Constancia en Dailies", "cohesion", "PROCESO", 50.0);
        }

        long miembros = memberRepo.countByFamilyId(familyId);
        if (miembros == 0) miembros = 1;

        long realizados = 0;
        long esperados  = 0;

        for (var sprint : sprints) {
            if (sprint.getStartDate() == null || sprint.getEndDate() == null) continue;
            long dias = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
            esperados += dias * miembros;
            realizados += sprintDailyRepo.countByFamilyIdAndDateRange(
                    familyId, sprint.getStartDate(), sprint.getEndDate());
        }

        if (esperados == 0) {
            return IndicatorResult.estimated("IND-02", "Constancia en Dailies", "cohesion", "PROCESO", 50.0);
        }

        double pct = Math.min((double) realizados / esperados * 100, 100.0);
        return IndicatorResult.real("IND-02", "Constancia en Dailies", "cohesion", "PROCESO",
                round2(pct), round2(pct), "%", realizados);
    }

    // ── IND-03: Frecuencia de Gratitud ───────────────────────────────────────

    private IndicatorResult ind03_frecuenciaGratitud(Long familyId) {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        long entradas = gratitudeRepo.countByFamilyIdSince(familyId, desde);
        double pct = Math.min((double) entradas / GRATITUD_OBJETIVO_30D * 100, 100.0);
        if (entradas == 0) {
            return IndicatorResult.estimated("IND-03", "Frecuencia de Gratitud", "cohesion", "PROCESO", 0.0);
        }
        return IndicatorResult.real("IND-03", "Frecuencia de Gratitud", "cohesion", "PROCESO",
                round2(pct), entradas, "entradas/30d", entradas);
    }

    // ── IND-04: Reparación Conductual ─────────────────────────────────────────

    private IndicatorResult ind04_reparacionConductual(Long familyId) {
        LocalDateTime desde = LocalDateTime.now().minusDays(90);
        long total     = behavioralRepo.countByFamilyIdSince(familyId, desde);
        long reparados = behavioralRepo.countRepairedByFamilyIdSince(familyId, desde);

        if (total == 0) {
            return IndicatorResult.estimated("IND-04", "Reparación Conductual", "cohesion", "RESULTADO", 50.0);
        }
        double pct = (double) reparados / total * 100;
        return IndicatorResult.real("IND-04", "Reparación Conductual", "cohesion", "RESULTADO",
                round2(pct), round2(pct), "%", total);
    }

    // ── IND-05: Confianza Interpersonal ──────────────────────────────────────

    private IndicatorResult ind05_confianzaInterpersonal(Long familyId) {
        long respuestas = icafAnswerRepo.countAnsweredByDomain(familyId, "confianza");
        if (respuestas == 0) {
            return IndicatorResult.estimated("IND-05", "Confianza Interpersonal", "confianza", "ESTADO", 50.0);
        }
        // El IcafDomainResolver ya calcula esto; aquí accedemos directamente al avg
        Double raw = icafAnswerRepo.avgScoreByDomain(familyId, "confianza");
        // avgScoreByDomain devuelve 1–5 normalizado en IcafDomainResolver, pero aquí
        // tenemos el avg crudo 1–5. Normalizamos igual que el engine.
        double score = raw != null ? (raw - 1) / 4.0 * 100 : 50.0;
        return IndicatorResult.real("IND-05", "Confianza Interpersonal", "confianza", "ESTADO",
                round2(score), round2(raw != null ? raw : 3.0), "avg 1-5", respuestas);
    }

    // ── IND-06: Apertura Comunicacional ──────────────────────────────────────

    private IndicatorResult ind06_aperturaComunicacional(Long familyId) {
        var ans = icafAnswerRepo.findByFamilyIdAndQuestionKey(familyId, "ICAF_CONF_007");
        if (ans.isEmpty()) {
            return IndicatorResult.estimated("IND-06", "Apertura Comunicacional", "confianza", "ESTADO", 50.0);
        }
        int score = ans.get().getScore(); // 1–5, ítem NEGATIVO
        double valor = (5.0 - score) / 4.0 * 100;
        return IndicatorResult.real("IND-06", "Apertura Comunicacional", "confianza", "ESTADO",
                round2(valor), score, "escala 1-5 inv.", 1);
    }

    // ── IND-07: Participación Multi-Miembro ──────────────────────────────────

    private IndicatorResult ind07_participacionMultimiembro(Long familyId) {
        long total = memberRepo.countByFamilyId(familyId);
        if (total == 0) {
            return IndicatorResult.estimated("IND-07", "Participación Multi-Miembro", "confianza", "PROCESO", 50.0);
        }

        LocalDateTime desde = LocalDateTime.now().minusDays(30);

        // Miembros activos = unión de 3 fuentes (tomamos el máximo por fuente)
        long conDaily     = sprintDailyRepo.countDistinctMembersWithDailySince(familyId, desde);
        long conEvidencia = evidenceRepo.countDistinctSubmittersSince(familyId, desde);
        long conRespuesta = icafAnswerRepo.countDistinctRespondersSince(familyId, desde);

        // Usamos el máximo de las 3 fuentes como estimación conservadora
        long activos = Math.max(conDaily, Math.max(conEvidencia, conRespuesta));
        // Si ninguna fuente tiene datos, fallback
        if (activos == 0) {
            return IndicatorResult.estimated("IND-07", "Participación Multi-Miembro", "confianza", "PROCESO", 50.0);
        }

        double pct = Math.min((double) activos / total * 100, 100.0);
        return IndicatorResult.real("IND-07", "Participación Multi-Miembro", "confianza", "PROCESO",
                round2(pct), activos, "miembros activos", total);
    }

    // ── IND-08: Completitud del Plan ─────────────────────────────────────────

    private IndicatorResult ind08_completitudPlan(Long familyId) {
        long total    = planTaskRepo.countByFamilyId(familyId);
        long completadas = planTaskRepo.countCompletedByFamilyId(familyId);

        if (total == 0) {
            return IndicatorResult.estimated("IND-08", "Completitud del Plan", "transf", "RESULTADO", 0.0);
        }
        double pct = (double) completadas / total * 100;
        return IndicatorResult.real("IND-08", "Completitud del Plan", "transf", "RESULTADO",
                round2(pct), round2(pct), "%", total);
    }

    // ── IND-09: Densidad de Evidencias Validadas ──────────────────────────────

    private IndicatorResult ind09_densidadEvidencias(Long familyId) {
        long misionesCompletadas = missionRepo.countCompletedByFamilyId(familyId);
        long evidenciasValidadas = evidenceRepo.countValidatedByFamilyId(familyId);

        if (misionesCompletadas == 0) {
            return IndicatorResult.estimated("IND-09", "Densidad de Evidencias", "transf", "PROCESO", 0.0);
        }
        double ratio = Math.min((double) evidenciasValidadas / misionesCompletadas * 100, 100.0);
        return IndicatorResult.real("IND-09", "Densidad de Evidencias", "transf", "PROCESO",
                round2(ratio), round2(ratio), "%", misionesCompletadas);
    }

    // ── IND-10: Momentum de Sprint ────────────────────────────────────────────

    private IndicatorResult ind10_momentumSprint(Long familyId) {
        long totalSprints   = sprintRepo.countByFamilyId(familyId);
        long sprintsConRetro = retroRepo.countByFamilyId(familyId);

        if (totalSprints == 0) {
            return IndicatorResult.estimated("IND-10", "Momentum de Sprint", "transf", "PROCESO", 0.0);
        }
        double pct = (double) sprintsConRetro / totalSprints * 100;
        return IndicatorResult.real("IND-10", "Momentum de Sprint", "transf", "PROCESO",
                round2(pct), round2(pct), "%", totalSprints);
    }

    // ── IND-11: Frecuencia de Evaluación ICF ─────────────────────────────────

    private IndicatorResult ind11_frecuenciaEvaluacion(Long familyId) {
        LocalDateTime desde = LocalDateTime.now().minusDays(90);
        // Contamos evaluaciones completadas en ventana de 90 días
        long completadas = evaluationRepo
                .findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getFinalizedAt() != null
                        && e.getFinalizedAt().isAfter(desde)
                        && com.integrityfamily.domain.EvaluationStatus.COMPLETED.equals(e.getStatus()))
                .count();

        if (completadas == 0) {
            return IndicatorResult.estimated("IND-11", "Frecuencia de Evaluación", "transf", "PROCESO", 0.0);
        }
        double pct = Math.min((double) completadas / EVALUACIONES_OBJETIVO_90D * 100, 100.0);
        return IndicatorResult.real("IND-11", "Frecuencia de Evaluación", "transf", "PROCESO",
                round2(pct), completadas, "eval/90d", completadas);
    }

    // ── IND-12: Impacto de Tareas ─────────────────────────────────────────────

    private IndicatorResult ind12_impactoTareas(Long familyId) {
        Double avg = planTaskRepo.avgImpactoIcfCompletedByFamilyId(familyId);
        if (avg == null) {
            return IndicatorResult.estimated("IND-12", "Impacto de Tareas", "transf", "RESULTADO", 50.0);
        }
        // impacto_icf escala 1–10, normalizar a 0–100
        double valor = avg * 10.0;
        return IndicatorResult.real("IND-12", "Impacto de Tareas", "transf", "RESULTADO",
                round2(valor), round2(avg), "avg 1-10", 1);
    }

    // ── IND-13: Tasa de Resolución de Crisis ─────────────────────────────────

    private IndicatorResult ind13_tasaResolucion(Long familyId, FamilyLongitudinalState state) {
        long total = totalCriticalEvents(familyId);
        if (total == 0) {
            return IndicatorResult.estimated("IND-13", "Tasa de Resolución de Crisis", "resil", "RESULTADO", 50.0);
        }
        long resolved = criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                      + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED");
        double score = (double) resolved / total * 100;
        return IndicatorResult.real("IND-13", "Tasa de Resolución de Crisis", "resil", "RESULTADO",
                round2(score), round2(score), "%", total);
    }

    // ── IND-14: Velocidad de Recuperación ────────────────────────────────────

    private IndicatorResult ind14_velocidadRecuperacion(Long familyId, FamilyLongitudinalState state) {
        if (totalCriticalEvents(familyId) == 0) {
            return IndicatorResult.estimated("IND-14", "Velocidad de Recuperación", "resil", "RESULTADO", 50.0);
        }
        double avgDays = criticalEventRepo.avgDaysToResolutionByFamilyId(familyId);
        double score;
        if (avgDays <= 0)   score = 50.0;
        else if (avgDays <= 60)  score = 100.0;
        else if (avgDays >= 180) score = 0.0;
        else score = (180.0 - avgDays) / 120.0 * 100.0;
        return IndicatorResult.real("IND-14", "Velocidad de Recuperación", "resil", "RESULTADO",
                round2(score), round2(avgDays), "días prom.", 1);
    }

    // ── IND-15: Control de Recaídas ───────────────────────────────────────────

    private IndicatorResult ind15_controlRecaidas(Long familyId, FamilyLongitudinalState state) {
        long total = totalCriticalEvents(familyId);
        if (total == 0) {
            return IndicatorResult.estimated("IND-15", "Control de Recaídas", "resil", "RESULTADO", 100.0);
        }
        long relapses = criticalEventRepo.totalRelapsesByFamilyId(familyId);
        if (relapses == 0) {
            return IndicatorResult.real("IND-15", "Control de Recaídas", "resil", "RESULTADO",
                    100.0, 0, "recaídas", total);
        }
        double ratio = Math.min((double) relapses / total, 1.0);
        double score = (1.0 - ratio) * 100.0;
        return IndicatorResult.real("IND-15", "Control de Recaídas", "resil", "RESULTADO",
                round2(score), relapses, "recaídas", total);
    }

    // ── IND-16: Carga de Crisis Activa ────────────────────────────────────────

    private IndicatorResult ind16_cargaCrisisActiva(Long familyId, FamilyLongitudinalState state) {
        long active = criticalEventRepo.countActiveByFamilyId(familyId);
        double score = switch ((int) Math.min(active, 3)) {
            case 0  -> 100.0;
            case 1  -> 70.0;
            case 2  -> 40.0;
            default -> 0.0;
        };
        boolean estimated = (active == 0 && totalCriticalEvents(familyId) == 0);
        return new IndicatorResult("IND-16", "Carga de Crisis Activa",
                "resil", "ESTADO", score, active, "activas", estimated, active);
    }

    private long totalCriticalEvents(Long familyId) {
        return criticalEventRepo.countByFamilyIdAndStatus(familyId, "DETECTED")
             + criticalEventRepo.countByFamilyIdAndStatus(familyId, "IN_PROGRESS")
             + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
             + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED")
             + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RELAPSED");
    }

    // ── IND-17: Delta ICaF 6 meses ────────────────────────────────────────────

    private IndicatorResult ind17_deltaIcaf6m(FamilyLongitudinalState state) {
        if (state == null || state.getIcafCurrent() == null || state.getIcaf6mAgo() == null) {
            return IndicatorResult.estimated("IND-17", "Delta ICaF 6 meses", "long", "RESULTADO", 50.0);
        }
        double delta = state.getIcafCurrent() - state.getIcaf6mAgo();
        double valor = Math.max(0, Math.min(100, 50 + delta / 2.0));
        return IndicatorResult.real("IND-17", "Delta ICaF 6 meses", "long", "RESULTADO",
                round2(valor), round2(delta), "pts", 2);
    }

    // ── IND-18: Delta ICaF 12 meses ──────────────────────────────────────────

    private IndicatorResult ind18_deltaIcaf12m(FamilyLongitudinalState state) {
        if (state == null || state.getIcafCurrent() == null || state.getIcaf12mAgo() == null) {
            return IndicatorResult.estimated("IND-18", "Delta ICaF 12 meses", "long", "RESULTADO", 50.0);
        }
        double delta = state.getIcafCurrent() - state.getIcaf12mAgo();
        double valor = Math.max(0, Math.min(100, 50 + delta / 2.0));
        return IndicatorResult.real("IND-18", "Delta ICaF 12 meses", "long", "RESULTADO",
                round2(valor), round2(delta), "pts", 2);
    }

    // ── IND-19: Velocidad de Mejora ───────────────────────────────────────────

    private IndicatorResult ind19_velocidadMejora(Long familyId, FamilyLongitudinalState state) {
        if (state == null || state.getIcafCurrent() == null || state.getIcaf6mAgo() == null) {
            return IndicatorResult.estimated("IND-19", "Velocidad de Mejora", "long", "RESULTADO", 50.0);
        }
        double delta6m = state.getIcafCurrent() - state.getIcaf6mAgo();
        double ptsPerMonth = delta6m / 6.0;
        // Normalizar: 0 pt/mes = 50; +1 pt/mes = 55; -1 pt/mes = 45; cap ±10 = 0/100
        double valor = Math.max(0, Math.min(100, 50 + ptsPerMonth * 5));
        return IndicatorResult.real("IND-19", "Velocidad de Mejora", "long", "RESULTADO",
                round2(valor), round2(ptsPerMonth), "pts/mes", 2);
    }

    // ── IND-20: Nivel de Madurez Familiar ────────────────────────────────────

    private IndicatorResult ind20_madurezFamiliar(FamilyLongitudinalState state) {
        if (state == null || state.getIcafMadurez() == null) {
            return IndicatorResult.estimated("IND-20", "Madurez Familiar", "long", "ESTADO", 20.0);
        }
        int nivel = state.getIcafMadurez();
        double valor = (double) nivel / 5.0 * 100;
        return IndicatorResult.real("IND-20", "Madurez Familiar", "long", "ESTADO",
                round2(valor), nivel, "nivel 1-5", 1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
