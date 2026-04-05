package com.integrityfamily.member.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.family.domain.Member;
import com.integrityfamily.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MemberController: Gestión de Integrantes del Nodo Familiar.
 * Optimizado para responder siempre en el formato ApiResponse.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite la comunicación con el Frontend de Angular
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ApiResponse<List<Member>> getAll() {
        return ApiResponse.ok(memberService.findAll());
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<Member>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(memberService.findByFamily(familyId));
    }

    /**
     * Crea un integrante vinculado a una familia.
     * Se elimina setFamilyId(Long) para evitar el error de compilación.
     */
    @PostMapping("/family/{familyId}")
    public ApiResponse<Member> createInFamily(@PathVariable Long familyId, @RequestBody Member member) {
        // La lógica de vinculación con la familia se delega al Service para mantener el Controller limpio
        return ApiResponse.ok(memberService.createInFamily(familyId, member));
    }

    @GetMapping("/{id}")
    public ApiResponse<Member> getById(@PathVariable Long id) {
        return ApiResponse.ok(memberService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Member> update(@PathVariable Long id, @RequestBody Member member) {
        return ApiResponse.ok(memberService.update(id, member));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ApiResponse.ok(null);
    }
}