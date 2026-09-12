package com.integrityfamily.vitality.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.EvidenceSource;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.HypothesisEvidence;
import com.integrityfamily.domain.repository.HypothesisEvidenceRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.vitality.domain.DailyVitalityLog;
import com.integrityfamily.vitality.repository.DailyVitalityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * RECOVERY_INDEX_HYPOTHESIS (ADR-004/ADR-009) — índice de recuperación 0-100
 * calculado sobre una ventana de días de DailyVitalityLog.
 *
 * El resultado se escribe en hypothesis_evidence, nunca directamente en
 * FamilyLongitudinalState ni como input del ICF (ADR-009, Decisión 2) — la
 * fórmula es ella misma una hipótesis sin validar.
 */
@Service
@RequiredArgsConstructor
public class RecoveryIndexService {

    public static final String RECOVERY_INDEX_HYPOTHESIS = "RECOVERY_INDEX_HYPOTHESIS";
    public static final String RECOVERY_INDEX_HYPOTHESIS_VERSION = "v1";
    private static final String INSTRUMENT = "RECOVERY_INDEX_V1";

    /** Umbrales del Semáforo Biológico (ADR-009, Decisión 3) — vista, no columna. */
    public static final double THRESHOLD_GREEN = 70.0;
    public static final double THRESHOLD_YELLOW = 40.0;

    private final DailyVitalityLogRepository logRepository;
    private final HypothesisEvidenceRepository hypothesisEvidenceRepository;
    private final MemberRepository memberRepository;

    /**
     * Calcula el índice sobre los últimos {@code windowDays} días (incluyendo
     * hoy) y escribe una fila en hypothesis_evidence si hay al menos un
     * componente presente. Retorna {@code null} sin escribir nada cuando la
     * ventana no tiene ningún dato — ausencia de dato no es un índice de 0
     * (mismo principio que el guard de neuroProfile en ADR-007).
     */
    @Transactional
    public Double calculateAndRecord(Long familyId, Long familyMemberId, int windowDays) {
        FamilyMember member = memberRepository.findById(familyMemberId)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado: " + familyMemberId));
        if (member.getFamily() == null || !member.getFamily().getId().equals(familyId)) {
            throw new BusinessException("El miembro no pertenece a esta familia");
        }

        LocalDate asOf = LocalDate.now();
        LocalDate from = asOf.minusDays(windowDays - 1L);
        List<DailyVitalityLog> logs = logRepository.findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(
                familyMemberId, from, asOf);

        Double index = compute(logs);
        if (index == null) return null;

        hypothesisEvidenceRepository.save(HypothesisEvidence.builder()
                .hypothesis(RECOVERY_INDEX_HYPOTHESIS)
                .hypothesisVersion(RECOVERY_INDEX_HYPOTHESIS_VERSION)
                .subjectType("FAMILY_MEMBER")
                .subjectId(familyMemberId)
                .measurementType("RECOVERY_INDEX")
                .measurementValue(index)
                .instrument(INSTRUMENT)
                .instrumentVersion("1")
                .source(EvidenceSource.DERIVED)
                .observedAt(asOf.atStartOfDay())
                .build());

        return index;
    }

    /**
     * Fórmula v1 (ADR-009, Decisión 2 — ponderación fijada aquí, no en el ADR):
     * <ul>
     *   <li>sleepComponent = media de {@code sleepHours/8h*100} (cap 100) y {@code sleepQuality*20}</li>
     *   <li>exerciseComponent = {@code exerciseMinutes/30min*100} (cap 100)</li>
     *   <li>nutritionComponent = {@code nutritionQuality*20}</li>
     *   <li>fatigueComponent (invertido) = {@code (6 - fatigueLevel)*20}</li>
     * </ul>
     * El índice final es la media de los componentes presentes. Si un campo
     * nunca se registró en la ventana, su componente se omite — no se asume
     * un valor neutro. {@code null} si ningún componente tiene dato.
     * Cambiar esta ponderación exige subir a {@code v2} (ADR-004, Decisión 2).
     */
    Double compute(List<DailyVitalityLog> logs) {
        Double avgSleepHours = average(logs, DailyVitalityLog::getSleepHours);
        Double avgSleepQuality = averageInt(logs, DailyVitalityLog::getSleepQuality);
        Double avgExerciseMinutes = averageInt(logs, DailyVitalityLog::getExerciseMinutes);
        Double avgNutritionQuality = averageInt(logs, DailyVitalityLog::getNutritionQuality);
        Double avgFatigueLevel = averageInt(logs, DailyVitalityLog::getFatigueLevel);

        Double sleepHoursScore = avgSleepHours == null ? null : Math.min(100.0, avgSleepHours / 8.0 * 100.0);
        Double sleepQualityScore = avgSleepQuality == null ? null : avgSleepQuality * 20.0;
        Double sleepComponent = mean(sleepHoursScore, sleepQualityScore);

        Double exerciseComponent = avgExerciseMinutes == null ? null : Math.min(100.0, avgExerciseMinutes / 30.0 * 100.0);
        Double nutritionComponent = avgNutritionQuality == null ? null : avgNutritionQuality * 20.0;
        Double fatigueComponent = avgFatigueLevel == null ? null
                : Math.max(0.0, Math.min(100.0, (6.0 - avgFatigueLevel) * 20.0));

        Double result = mean(sleepComponent, exerciseComponent, nutritionComponent, fatigueComponent);
        return result == null ? null : Math.round(result * 10.0) / 10.0;
    }

    public String semaphore(Double index) {
        if (index == null) return null;
        if (index >= THRESHOLD_GREEN) return "GREEN";
        if (index >= THRESHOLD_YELLOW) return "YELLOW";
        return "RED";
    }

    private Double average(List<DailyVitalityLog> logs, Function<DailyVitalityLog, Double> getter) {
        List<Double> values = logs.stream().map(getter).filter(v -> v != null).toList();
        return values.isEmpty() ? null : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double averageInt(List<DailyVitalityLog> logs, Function<DailyVitalityLog, Integer> getter) {
        List<Integer> values = logs.stream().map(getter).filter(v -> v != null).toList();
        return values.isEmpty() ? null : values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private Double mean(Double... values) {
        List<Double> present = java.util.Arrays.stream(values).filter(v -> v != null).toList();
        return present.isEmpty() ? null : present.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
