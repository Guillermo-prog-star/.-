package com.integrityfamily.family.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Family;
import com.integrityfamily.family.service.FamilyService;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * SDD: Controlador de Núcleos Familiares.
 * Postura Técnica: Se elimina el bypass de repositorios y se estandariza el contrato ApiResponse.
 */
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping
    public ApiResponse<List<Family>> getAll() {
        return ApiResponse.ok(familyService.findAll());
    }

    /**
     * GET /api/families/mine — Retorna la familia del usuario autenticado.
     */
    @GetMapping("/mine")
    public ApiResponse<Family> getMyFamily(Principal principal) {
        if (principal == null) {
            throw new BusinessException("No autenticado", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        Optional<Family> family = familyService.findByCreatorEmail(principal.getName());
        return ApiResponse.ok(family.orElse(null), family.isPresent() ? "Familia recuperada" : "Sin familia vinculada");
    }

    @GetMapping("/{id}")
    public ApiResponse<Family> getById(@PathVariable Long id) {
        return ApiResponse.ok(familyService.findById(id));
    }

    /**
     * POST /api/families — Crea un nuevo núcleo familiar.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Family> createFamily(@RequestBody Family family, Principal principal) {
        if (principal == null) {
            throw new BusinessException("No autenticado", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        Family created = familyService.create(family, principal.getName());
        return ApiResponse.ok(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Family> update(@PathVariable Long id, @RequestBody Family family) {
        return ApiResponse.ok(familyService.update(id, family));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        familyService.delete(id);
        return ApiResponse.ok(null);
    }
}
