package com.integrityfamily.family.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

/**
 * FamilyController: Gestión de Nodos Familiares.
 * Centraliza las peticiones del Frontend y estandariza las respuestas.
 * Optimizado para la integración con el Dashboard de Bienestar.
 */
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * Lista todas las familias registradas en el sistema.
     */
    @GetMapping
    public ApiResponse<List<Family>> getAll() {
        return ApiResponse.ok(familyService.findAll());
    }

    /**
     * Obtiene los detalles de una familia específica por su ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<Family> getById(@PathVariable Long id) {
        return ApiResponse.ok(familyService.findById(id));
    }

    /**
     * Crea un nuevo núcleo familiar (Nodo).
     * Sincronizado con FamilyService para incluir el email del creador.
     */
    @PostMapping
    public ApiResponse<Family> createFamily(@RequestBody Family family, Principal principal) {
        // Obtenemos el email del usuario autenticado (William) desde el token/sesión
        String creatorEmail = principal.getName();
        
        // ESTOCADA FINAL: Ahora pasamos ambos argumentos requeridos
        Family created = familyService.create(family, creatorEmail);
        return ApiResponse.ok(created);
    }

    /**
     * Actualiza los datos de un núcleo familiar existente.
     */
    @PutMapping("/{id}")
    public ApiResponse<Family> update(@PathVariable Long id, @RequestBody Family family) {
        return ApiResponse.ok(familyService.update(id, family));
    }

    /**
     * Elimina un registro de familia de la base de datos MySQL.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        familyService.delete(id);
        return ApiResponse.ok(null);
    }
}