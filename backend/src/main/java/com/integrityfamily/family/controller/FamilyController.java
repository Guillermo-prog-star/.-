package com.integrityfamily.family.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.Family;
import com.integrityfamily.family.dto.FamilyResponse;
import com.integrityfamily.family.service.FamilyService;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ApiResponse<List<FamilyResponse>> getAll() {
        return ApiResponse.ok(familyService.findAll());
    }

    /**
     * GET /api/families/mine — Retorna la familia del usuario autenticado.
     * Devuelve 404 si no tiene familia, para que el cliente muestre el formulario de creación.
     */
    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ApiResponse<FamilyResponse> getMyFamily(Principal principal) {
        if (principal == null) {
            throw new BusinessException("No autenticado", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        return familyService.findByCreatorEmail(principal.getName())
                .map(f -> ApiResponse.ok(f, "Familia recuperada"))
                .orElseThrow(() -> new BusinessException("Sin familia vinculada", "NO_FAMILY", HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@familySecurity.check(#id)")
    public ApiResponse<FamilyResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(familyService.findById(id));
    }

    /**
     * POST /api/families — Crea un nuevo núcleo familiar.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FamilyResponse> createFamily(@RequestBody Family family, Principal principal) {
        if (principal == null) {
            throw new BusinessException("No autenticado", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        FamilyResponse created = familyService.create(family, principal.getName());
        return ApiResponse.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@familySecurity.check(#id)")
    public ApiResponse<FamilyResponse> update(@PathVariable Long id, @RequestBody Family family) {
        return ApiResponse.ok(familyService.update(id, family));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@familySecurity.check(#id)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        familyService.delete(id);
        return ApiResponse.ok(null);
    }
}
