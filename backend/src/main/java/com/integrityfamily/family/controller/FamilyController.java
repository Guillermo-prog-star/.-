package com.integrityfamily.family.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.family.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final FamilyRepository familyRepository;

    @GetMapping
    public ApiResponse<List<Family>> getAll() {
        return ApiResponse.ok(familyService.findAll());
    }

    /**
     * GET /api/families/mine — Retorna la familia del usuario autenticado.
     * El frontend lo usa en ngOnInit para detectar si ya existe y rellenar el estado.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> getMyFamily(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }
        Optional<Family> family = familyRepository.findByCreatedBy_Email(principal.getName());
        if (family.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok(family.get()));
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Sin familia"));
    }

    @GetMapping("/{id}")
    public ApiResponse<Family> getById(@PathVariable Long id) {
        return ApiResponse.ok(familyService.findById(id));
    }

    /**
     * POST /api/families — Crea un nuevo núcleo familiar.
     * Si el usuario ya tiene una, retorna 409 con los datos de la existente.
     */
    @PostMapping
    public ResponseEntity<?> createFamily(@RequestBody Family family, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autenticado"));
        }
        String creatorEmail = principal.getName();

        Optional<Family> existing = familyRepository.findByCreatedBy_Email(creatorEmail);
        if (existing.isPresent()) {
            Family f = existing.get();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "success", false,
                        "message", "ya posee",
                        "familyId", f.getId(),
                        "familyName", f.getName() != null ? f.getName() : "",
                        "familyCode", f.getFamilyCode() != null ? f.getFamilyCode() : ""
                    ));
        }

        Family created = familyService.create(family, creatorEmail);
        return ResponseEntity.ok(ApiResponse.ok(created));
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