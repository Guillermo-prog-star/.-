package com.integrityfamily.context.controller;

import com.integrityfamily.context.dto.FamilyContextDto;
import com.integrityfamily.context.service.FamilyContextEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}/context")
@RequiredArgsConstructor
public class FamilyContextController {

    private final FamilyContextEngine contextEngine;

    /** Devuelve el contexto actual (desde caché si tiene < 2h). */
    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyContextDto> get(@PathVariable Long familyId) {
        return ResponseEntity.ok(contextEngine.getContext(familyId));
    }

    /** Fuerza un recómputo inmediato del contexto. */
    @PostMapping("/refresh")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyContextDto> refresh(@PathVariable Long familyId) {
        return ResponseEntity.ok(contextEngine.refresh(familyId));
    }
}
