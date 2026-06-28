package com.integrityfamily.scanner.controller;

import com.integrityfamily.ai.service.FamilyNarrativeService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.security.SecurityValidator;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse;
import com.integrityfamily.simulation.service.FamilyScenarioProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/families/{familyId}/radar")
@RequiredArgsConstructor
@Tag(name = "Radar de Señales Sutiles", description = "Detección de microseñales y tendencias familiares")
public class SubtleSignalRadarController {

    private final SubtleSignalRadarService radarService;
    private final FamilyScenarioProjectionService scenarioService;
    private final FamilyNarrativeService narrativeService;
    private final SecurityValidator securityValidator;

    @GetMapping
    @Operation(summary = "Analiza tendencias y microseñales sutiles de la familia",
               description = "Detecta patrones de riesgo emergentes y fortalezas invisibles " +
                             "a partir del historial de evaluaciones ICF y el estado longitudinal.")
    public ResponseEntity<ApiResponse<SubtleSignalRadarResponse>> analyze(
        @PathVariable Long familyId,
        Authentication authentication
    ) {
        securityValidator.validateFamilyOwnership(familyId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(radarService.analyze(familyId)));
    }

    @GetMapping("/scenarios")
    @Operation(summary = "Proyecta tres escenarios familiares condicionales a 12 semanas",
               description = "Escenario A (sin intervención), B (misiones actuales), C (intervención intensiva). " +
                             "Muestra futuros plausibles con rangos de incertidumbre para que " +
                             "la familia comprenda que el resultado depende de sus decisiones.")
    public ResponseEntity<ApiResponse<FamilyScenarioResponse>> scenarios(
        @PathVariable Long familyId,
        Authentication authentication
    ) {
        securityValidator.validateFamilyOwnership(familyId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(scenarioService.project(familyId)));
    }

    @GetMapping("/narrative")
    @Operation(summary = "Genera la narrativa evolutiva de la familia vía IA",
               description = "Orquesta el Radar (Fase 1) y los Escenarios (Fase 2) y pide a Claude " +
                             "que los traduzca a una historia comprensible del tipo " +
                             "'La historia de esta familia está cambiando...'. " +
                             "Texto Markdown listo para mostrar en el dashboard.")
    public ResponseEntity<ApiResponse<FamilyNarrativeService.NarrativeResponse>> narrative(
        @PathVariable Long familyId,
        Authentication authentication
    ) {
        securityValidator.validateFamilyOwnership(familyId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(narrativeService.generate(familyId)));
    }
}
