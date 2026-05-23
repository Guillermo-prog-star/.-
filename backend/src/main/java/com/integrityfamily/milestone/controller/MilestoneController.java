package com.integrityfamily.milestone.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Milestone;
import com.integrityfamily.milestone.service.MilestoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Gestión y avance automático de la ruta de transformación familiar W1→M36")
public class MilestoneController {

    private final MilestoneService milestoneService;

    @GetMapping
    public List<Milestone> getAll() {
        return milestoneService.findAll();
    }

    @GetMapping("/{id}")
    public Milestone getById(@PathVariable Long id) {
        return milestoneService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Milestone create(@RequestBody Milestone milestone) {
        return milestoneService.create(milestone);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Milestone update(@PathVariable Long id, @RequestBody Milestone milestone) {
        return milestoneService.update(id, milestone);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void delete(@PathVariable Long id) {
        milestoneService.delete(id);
    }

    @GetMapping("/family/{familyId}/current")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public String getCurrentMilestoneLabel(@PathVariable Long familyId) {
        return milestoneService.getCurrentMilestoneLabel(familyId);
    }

    /**
     * Diagnóstico completo de los 3 criterios de avance para esta familia.
     * Útil para que el frontend muestre la barra de progreso hacia el próximo hito.
     */
    @Operation(
        summary = "Diagnóstico de avance de hito",
        description = "Evalúa los 3 criterios (tiempo, ICF y tareas) y devuelve el estado detallado. " +
                      "No realiza cambios en BD.")
    @GetMapping("/family/{familyId}/advancement-status")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<ApiResponse<MilestoneService.AdvancementEvaluation>> getAdvancementStatus(
            @PathVariable Long familyId) {
        MilestoneService.AdvancementEvaluation status = milestoneService.getAdvancementStatus(familyId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * Intenta avanzar la familia al siguiente hito si los 3 criterios están cumplidos.
     */
    @Operation(
        summary = "Avanzar hito",
        description = "Avanza la familia al siguiente hito si tiempo + ICF + tareas lo permiten. " +
                      "Retorna el código del hito resultante (nuevo o el mismo si no aplica).")
    @PostMapping("/family/{familyId}/advance")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<ApiResponse<String>> advanceMilestone(@PathVariable Long familyId) {
        String result = milestoneService.advanceMilestone(familyId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
