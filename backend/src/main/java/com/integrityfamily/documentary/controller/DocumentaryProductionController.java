package com.integrityfamily.documentary.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.documentary.domain.DocumentaryProduction;
import com.integrityfamily.documentary.domain.DocumentaryScope;
import com.integrityfamily.documentary.dto.DocumentaryProductionDTO;
import com.integrityfamily.documentary.service.DocumentaryProductionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documentary-productions")
@RequiredArgsConstructor
public class DocumentaryProductionController {

    private final DocumentaryProductionService productionService;

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<DocumentaryProductionDTO>> createDraft(@RequestBody CreateDraftRequest request) {
        DocumentaryProduction production = productionService.createDraft(
                request.getFamilyId(),
                request.getTitle(),
                request.getScope(),
                request.getReferenceId()
        );
        return ResponseEntity.ok(ApiResponse.ok(DocumentaryProductionDTO.fromEntity(production), "Borrador creado"));
    }

    @PutMapping("/{id}/curation")
    public ResponseEntity<ApiResponse<DocumentaryProductionDTO>> updateCuration(
            @PathVariable Long id,
            @RequestBody UpdateCurationRequest request) {
        DocumentaryProduction production = productionService.updateCuration(id, request.getEvidenceIds());
        return ResponseEntity.ok(ApiResponse.ok(DocumentaryProductionDTO.fromEntity(production), "Curaduría actualizada"));
    }

    @PostMapping("/{id}/generate-script")
    public ResponseEntity<ApiResponse<DocumentaryProductionDTO>> generateScript(@PathVariable Long id) {
        DocumentaryProduction production = productionService.generateScript(id);
        return ResponseEntity.ok(ApiResponse.ok(DocumentaryProductionDTO.fromEntity(production), "Guion generado"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<DocumentaryProductionDTO>> approveProduction(@PathVariable Long id) {
        DocumentaryProduction production = productionService.approveProduction(id);
        return ResponseEntity.ok(ApiResponse.ok(DocumentaryProductionDTO.fromEntity(production), "Producción aprobada"));
    }

    @GetMapping("/family/{familyId}")
    public ResponseEntity<ApiResponse<List<DocumentaryProductionDTO>>> getProductions(@PathVariable Long familyId) {
        List<DocumentaryProductionDTO> dtos = productionService.getProductionsByFamily(familyId).stream()
                .map(DocumentaryProductionDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentaryProductionDTO>> getProduction(@PathVariable Long id) {
        DocumentaryProduction production = productionService.getProduction(id);
        return ResponseEntity.ok(ApiResponse.ok(DocumentaryProductionDTO.fromEntity(production)));
    }

    @Data
    public static class CreateDraftRequest {
        private Long familyId;
        private String title;
        private DocumentaryScope scope;
        private Long referenceId;
    }

    @Data
    public static class UpdateCurationRequest {
        private List<Long> evidenceIds;
    }
}
