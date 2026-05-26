package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import com.integrityfamily.scanner.domain.EmotionalOperationalState;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.domain.RuleActivation;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * IF-CIS: Servicio de gestión de registros de inferencia.
 *
 * Garantiza estabilidad epistemológica: misma evidencia → misma inferencia.
 * El evidenceHash previene la creación de duplicados y permite replay histórico.
 *
 * Flujo epistemológico de un InferenceRecord:
 *   INFERRED → (sin nueva evidencia por 7+ días) → STABILIZED
 *              (nueva evidencia contradictoria)    → REVISED → nuevo INFERRED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InferenceRecordService {

    private final InferenceRecordRepository inferenceRecordRepository;
    private final EmotionalStateClassifier  stateClassifier;

    /**
     * Crea un InferenceRecord a partir de una evaluación finalizada y su AlgoResult.
     * Si ya existe un registro con el mismo evidenceHash, no crea duplicado.
     *
     * @param evaluation evaluación ya persistida
     * @param algo       resultado completo del RISK_ALGO_V1
     * @return el InferenceRecord creado, o null si era duplicado
     */
    @Transactional
    public InferenceRecord createFromEvaluation(Evaluation evaluation,
                                                RiskAlgoV1Engine.AlgoResult algo) {
        String evidenceHash = computeEvidenceHash(evaluation, algo);

        if (inferenceRecordRepository.existsByEvidenceHash(evidenceHash)) {
            log.debug("[IF-CIS] Hash {} ya existe — inferencia estable, sin duplicado.", evidenceHash);
            return null;
        }

        EmotionalOperationalState opState =
                stateClassifier.classify(evaluation.getFamily().getId());

        double uncertaintyTotal = algo.uncertainty() != null ? algo.uncertainty().total() : 0.10;

        InferenceRecord record = InferenceRecord.builder()
                .familyId(evaluation.getFamily().getId())
                .evaluationId(evaluation.getId())
                .inferenceKey("ICF_CALC")
                .algoVersion(1)
                .epistemicState("INFERRED")
                .evidenceHash(evidenceHash)
                .icfValue(algo.healthyIndex())
                .riskLevel(algo.riskLevel())
                .criticalDimension(algo.criticalDimension())
                .operationalState(opState.name())
                .simulationSuspected(algo.simulationSuspected())
                .uncertaintyTotal(uncertaintyTotal)
                .createdAt(Instant.now())
                .build();

        InferenceRecord saved = inferenceRecordRepository.save(record);
        log.info("[IF-CIS] InferenceRecord creado: id={} | familia={} | estado={} | incert={}",
                saved.getId(), saved.getFamilyId(), opState, uncertaintyTotal);
        return saved;
    }

    /**
     * IF-REE: Crea un InferenceRecord a partir de una activación de regla EEDSL.
     *
     * A diferencia de {@link #createFromEvaluation}, el inferenceKey es el ruleKey
     * (no "ICF_CALC"), lo que permite distinguir registros generados por reglas
     * de los generados por el cálculo base del ICF.
     *
     * El evidenceHash se calcula con el ruleKey para garantizar que la misma regla
     * no genere un segundo registro en la misma evaluación.
     */
    @Transactional
    public InferenceRecord createFromRule(Evaluation evaluation,
                                          RiskAlgoV1Engine.AlgoResult algo,
                                          RuleActivation activation) {
        String evidenceHash = computeRuleHash(evaluation, activation);

        if (inferenceRecordRepository.existsByEvidenceHash(evidenceHash)) {
            log.debug("[IF-REE] Regla {} ya registrada para evaluación {} — sin duplicado.",
                    activation.ruleKey(), evaluation.getId());
            return null;
        }

        EmotionalOperationalState opState =
                stateClassifier.classify(evaluation.getFamily().getId());

        double uncertaintyTotal = algo.uncertainty() != null ? algo.uncertainty().total() : 0.10;

        // La confianza de la regla modula la incertidumbre: mayor confianza → menor incertidumbre
        double ruleUncertainty = uncertaintyTotal * (1.0 - activation.confidenceBase());

        // Usar el riskOutput de la regla si está definido, sino el nivel calculado por el algo
        String effectiveRisk = (activation.riskOutput() != null && !activation.riskOutput().isBlank())
                ? activation.riskOutput()
                : algo.riskLevel();

        InferenceRecord record = InferenceRecord.builder()
                .familyId(evaluation.getFamily().getId())
                .evaluationId(evaluation.getId())
                .inferenceKey(activation.ruleKey())
                .algoVersion(activation.version())
                .epistemicState("INFERRED")
                .evidenceHash(evidenceHash)
                .icfValue(algo.healthyIndex())
                .riskLevel(effectiveRisk)
                .criticalDimension(algo.criticalDimension())
                .operationalState(opState.name())
                .simulationSuspected(algo.simulationSuspected())
                .uncertaintyTotal(ruleUncertainty)
                .createdAt(Instant.now())
                .build();

        InferenceRecord saved = inferenceRecordRepository.save(record);
        log.info("[IF-REE] InferenceRecord de regla creado: id={} | ruleKey={} | confianza={} | incert={}",
                saved.getId(), activation.ruleKey(), activation.confidenceBase(), ruleUncertainty);
        return saved;
    }

    /**
     * Marca un registro como STABILIZED si no ha sido revisado en el período esperado.
     * Llamar desde un scheduler externo (no incluido aquí para evitar acoplamiento).
     */
    @Transactional
    public void stabilize(Long inferenceRecordId) {
        inferenceRecordRepository.findById(inferenceRecordId).ifPresent(r -> {
            if ("INFERRED".equals(r.getEpistemicState())) {
                r.setEpistemicState("STABILIZED");
                r.setStabilizedAt(Instant.now());
                inferenceRecordRepository.save(r);
                log.info("[IF-CIS] InferenceRecord {} estabilizado.", inferenceRecordId);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Computa el hash determinístico de la evidencia base (ICF_CALC).
     * Componentes: familyId | evaluationId | algoVersion | ICF (2 dec) | riskLevel | critDim
     */
    private String computeEvidenceHash(Evaluation evaluation, RiskAlgoV1Engine.AlgoResult algo) {
        String algoVersion = evaluation.getAlgorithmVersion() != null
                ? evaluation.getAlgorithmVersion() : "RISK_ALGO_V1";
        String raw = evaluation.getFamily().getId()
                + "|" + evaluation.getId()
                + "|" + algoVersion
                + "|" + String.format("%.2f", algo.healthyIndex())
                + "|" + algo.riskLevel()
                + "|" + algo.criticalDimension();
        return sha256Short(raw);
    }

    /**
     * Hash determinístico para activaciones de reglas EEDSL.
     * Componentes: familyId | evaluationId | ruleKey | ruleVersion | signals
     */
    private String computeRuleHash(Evaluation evaluation, RuleActivation activation) {
        List<String> sortedSignals = activation.activatedSignals().stream().sorted().toList();
        String raw = evaluation.getFamily().getId()
                + "|" + evaluation.getId()
                + "|" + activation.ruleKey()
                + "|v" + activation.version()
                + "|" + String.join(",", sortedSignals);
        return sha256Short(raw);
    }

    private String sha256Short(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
