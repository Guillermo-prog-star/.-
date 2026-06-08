package com.integrityfamily.council.controller;

import com.integrityfamily.council.dto.CouncilRequest;
import com.integrityfamily.council.dto.CouncilResponse;
import com.integrityfamily.council.service.FamilyCouncilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}/council")
@RequiredArgsConstructor
public class FamilyCouncilController {

    private final FamilyCouncilService councilService;

    /**
     * Consulta al Consejo Familiar.
     * La IA responde desde la constitución, ADN e historia de la propia familia.
     */
    @PostMapping("/consult")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<CouncilResponse> consult(
            @PathVariable Long familyId,
            @Valid @RequestBody CouncilRequest request) {
        return ResponseEntity.ok(councilService.consult(familyId, request));
    }
}
