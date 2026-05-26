package com.integrityfamily.scanner.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.scanner.dto.EmotionalRuleDto;
import com.integrityfamily.scanner.dto.EmotionalRuleRequest;
import com.integrityfamily.scanner.service.EmotionalRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * API de gestión de Reglas EEDSL (IF-EEDSL Admin).
 *
 * Permite a los administradores visualizar, activar/desactivar,
 * crear y actualizar reglas del motor de diagnóstico emocional
 * sin modificar el código fuente.
 *
 * Prefijo: /api/admin/eedsl
 */
@RestController
@RequestMapping("/api/admin/eedsl")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "10. EEDSL Admin", description =
    "Gestión dinámica de reglas del Lenguaje Emocional Ejecutable (IF-EEDSL). " +
    "Solo accesible para administradores.")
public class EmotionalRuleController {

    private final EmotionalRuleService ruleService;

    @Operation(summary = "Listar todas las reglas EEDSL",
               description = "Devuelve todas las reglas, activas e inactivas. Ordenadas por ID.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmotionalRuleDto>>> getAll(
            @RequestParam(required = false) Boolean activeOnly) {

        List<EmotionalRuleDto> rules = Boolean.TRUE.equals(activeOnly)
                ? ruleService.findActive()
                : ruleService.findAll();

        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    @Operation(summary = "Alternar estado activo/inactivo de una regla",
               description = "Cambia el campo 'active' de la regla. " +
                   "Una regla inactiva no se aplica en nuevas evaluaciones.")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<EmotionalRuleDto>> toggle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(ruleService.toggleActive(id)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Crear nueva regla EEDSL",
               description = "Crea una nueva regla. Si el ruleKey ya existe, " +
                   "se incrementa la versión automáticamente.")
    @PostMapping
    public ResponseEntity<ApiResponse<EmotionalRuleDto>> create(
            @RequestBody EmotionalRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ruleService.create(req)));
    }

    @Operation(summary = "Actualizar campos de una regla existente",
               description = "Actualización parcial. Solo se modifican los campos no nulos del payload.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmotionalRuleDto>> update(
            @PathVariable Long id,
            @RequestBody EmotionalRuleRequest req) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(ruleService.update(id, req)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
