package com.integrityfamily.risk.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * SDD: Controlador del Monitor Sentinel (Riesgo).
 * Orquestador de la visibilidad de estados de crisis.
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    /**
     * Obtiene el histÃƒÂ³rico de estados de riesgo.
     * SDD FIX: Sincronizado con la firma unificada del Service.
     */
    @GetMapping("/family/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<RiskSnapshot>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(riskService.findByFamilyId(familyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@familySecurity.checkRiskSnapshot(#id)")
    public ApiResponse<RiskSnapshot> getById(@PathVariable Long id) {
        return ApiResponse.ok(riskService.findById(id));
    }

    /**
     * Persiste un nuevo estado de riesgo (Uso técnico/Sentinel).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RiskSnapshot> create(@RequestBody RiskSnapshot snapshot) {
        return ApiResponse.ok(riskService.save(snapshot));
    }
}


