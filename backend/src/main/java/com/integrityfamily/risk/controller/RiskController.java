package com.integrityfamily.risk.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping
    public ApiResponse<List<RiskSnapshot>> getAll() {
        // SDD: Se asume refactorizaciÃƒÂ³n a nombres estÃƒÂ¡ndar en RiskService
        return ApiResponse.ok(riskService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<RiskSnapshot> getById(@PathVariable Long id) {
        return ApiResponse.ok(riskService.findById(id));
    }

    /**
     * Persiste un nuevo estado de riesgo (Uso tÃƒÂ©cnico/Sentinel).
     */
    @PostMapping
    public ApiResponse<RiskSnapshot> create(@RequestBody RiskSnapshot snapshot) {
        // SDD FIX: Sincronizado con la firma real para evitar "cannot find symbol"
        return ApiResponse.ok(riskService.save(snapshot));
    }
}


