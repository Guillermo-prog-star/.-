package com.integrityfamily.scanner.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.scanner.domain.EmotionalOperationalState;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.dto.FamilyAlertDto;
import com.integrityfamily.scanner.dto.InferenceRecordDto;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import com.integrityfamily.scanner.service.EmotionalStateClassifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API REST del Scanner Emocional Formal (IF-Scanner).
 *
 * Expone:
 *   - Historial de inferencias (IF-CIS): trazabilidad epistemológica
 *   - Estado operacional actual (IF-TOS): trayectoria familiar
 *
 * Todos los endpoints son de solo lectura.
 * Los datos son generados automáticamente al finalizar cada evaluación.
 */
@RestController
@RequestMapping("/api/scanner")
@RequiredArgsConstructor
@Tag(name = "9. IF-Scanner Formal", description =
    "Scanner emocional formal de Integrity Family. " +
    "Registros de inferencia (IF-CIS), estados operacionales (IF-TOS) " +
    "e índice de incertidumbre estructural (IF-SUM).")
public class ScannerController {

    private final InferenceRecordRepository inferenceRecordRepository;
    private final FamilyAlertRepository     familyAlertRepository;
    private final EmotionalStateClassifier  stateClassifier;

    @Operation(
        summary = "Historial de inferencias formales",
        description = "Devuelve todos los InferenceRecord de la familia ordenados por fecha " +
            "descendente. Cada registro incluye el hash de evidencia, estado epistemológico, " +
            "estado operacional IF-TOS e índice de incertidumbre IF-SUM."
    )
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/inferences")
    public ResponseEntity<ApiResponse<List<InferenceRecordDto>>> getInferences(
            @PathVariable Long familyId) {

        List<InferenceRecordDto> dtos = inferenceRecordRepository
                .findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(r -> new InferenceRecordDto(
                        r.getId(),
                        r.getEvaluationId(),
                        r.getInferenceKey(),
                        r.getEpistemicState(),
                        r.getOperationalState(),
                        r.getIcfValue(),
                        r.getRiskLevel(),
                        r.getCriticalDimension(),
                        r.getUncertaintyTotal(),
                        r.getSimulationSuspected(),
                        r.getEvidenceHash(),
                        r.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @Operation(
        summary = "Estado operacional actual (IF-TOS)",
        description = "Clasifica el estado operacional emocional actual de la familia " +
            "comparando el historial longitudinal de evaluaciones. " +
            "Estados: EMERGING | STABLE | ESCALATING | CRITICAL | RECOVERING | RESOLVED"
    )
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOperationalState(
            @PathVariable Long familyId) {

        EmotionalOperationalState state = stateClassifier.classify(familyId);
        Map<String, Object> result = Map.of(
                "familyId",        familyId,
                "operationalState", state.name(),
                "label",           stateLabel(state),
                "description",     stateDescription(state)
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(
        summary = "Alertas clínicas activas (IF-ALT)",
        description = "Devuelve las alertas clínicas sin resolver para la familia. " +
            "Tipos: CONSECUTIVE_HIGH_RISK | CRITICAL_STATE_SUSTAINED | " +
            "SIMULATION_REPEAT | RELAPSE_CONFIRMED | MULTI_RULE_ACTIVATION"
    )
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/alerts")
    public ResponseEntity<ApiResponse<List<FamilyAlertDto>>> getAlerts(
            @PathVariable Long familyId,
            @RequestParam(defaultValue = "false") boolean includeResolved) {

        List<FamilyAlert> alerts = includeResolved
                ? familyAlertRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                : familyAlertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(familyId);

        List<FamilyAlertDto> dtos = alerts.stream()
                .map(a -> new FamilyAlertDto(
                        a.getId(), a.getFamilyId(), a.getAlertType(), a.getSeverity(),
                        a.getTitle(), a.getDetail(), a.getInferenceKey(), a.getEvaluationId(),
                        a.isResolved(), a.getResolvedAt(), a.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @Operation(summary = "Marcar alerta como resuelta (IF-ALT)")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @PatchMapping("/family/{familyId}/alerts/{alertId}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveAlert(
            @PathVariable Long familyId,
            @PathVariable Long alertId) {

        familyAlertRepository.findById(alertId).ifPresent(a -> {
            if (a.getFamilyId().equals(familyId) && !a.isResolved()) {
                a.setResolved(true);
                a.setResolvedAt(java.time.Instant.now());
                familyAlertRepository.save(a);
            }
        });
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String stateLabel(EmotionalOperationalState s) {
        return switch (s) {
            case EMERGING   -> "Diagnóstico Inicial";
            case STABLE     -> "Estable";
            case ESCALATING -> "En Deterioro";
            case CRITICAL   -> "Crisis";
            case RECOVERING -> "En Recuperación";
            case RESOLVED   -> "Resuelto";
        };
    }

    private String stateDescription(EmotionalOperationalState s) {
        return switch (s) {
            case EMERGING   -> "Primera evaluación registrada. Línea de base establecida.";
            case STABLE     -> "Sin cambio significativo en el último período evaluado.";
            case ESCALATING -> "Deterioro progresivo detectado en evaluaciones consecutivas.";
            case CRITICAL   -> "Riesgo crítico activo. Se recomienda intervención inmediata.";
            case RECOVERING -> "Mejora observable desde un estado de alto riesgo anterior.";
            case RESOLVED   -> "Estabilización confirmada en nivel de riesgo bajo.";
        };
    }
}
