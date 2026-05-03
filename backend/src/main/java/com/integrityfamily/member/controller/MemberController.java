package com.integrityfamily.member.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDD: Controlador de Miembros sincronizado.
 * Postura Técnica: Se centraliza la creación a través del Record universal.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final com.integrityfamily.domain.repository.UserRepository userRepository;
    private final com.integrityfamily.member.service.InvitationService invitationService;

    @PostMapping("/{id}/invite")
    public ApiResponse<Void> inviteMember(@PathVariable Long id) {
        invitationService.sendInvitation(id);
        return ApiResponse.ok(null);
    }


    @GetMapping
    public ApiResponse<List<FamilyMember>> getAll() {
        return ApiResponse.ok(memberService.findAll());
    }

    /**
     * SDD: Recupera los miembros de la familia del usuario autenticado.
     */
    @GetMapping("/mine")
    public ApiResponse<List<FamilyMember>> getMyFamilyMembers(org.springframework.security.core.Authentication auth) {
        com.integrityfamily.domain.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (user.getFamily() == null) {
            return ApiResponse.ok(java.util.Collections.emptyList());
        }
        
        return ApiResponse.ok(memberService.findByFamily(user.getFamily().getId()));
    }

    /**
     * SDD: Registra un miembro en la familia del usuario autenticado (ADMIN flow).
     */
    @PostMapping("/mine")
    public ApiResponse<FamilyMember> createInMyFamily(
            @Valid @RequestBody MemberRequest request,
            org.springframework.security.core.Authentication auth) {

        com.integrityfamily.domain.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getFamily() == null) {
            throw new RuntimeException("El usuario no tiene una familia asociada");
        }

        MemberRequest integratedRequest = new MemberRequest(
                request.fullName(),
                request.roleType(),
                request.age(),
                request.autonomyLevel(),
                request.responsibilityLevel(),
                request.email(),
                request.phone(),
                user.getFamily().getId()
        );

        return ApiResponse.ok(memberService.createMember(integratedRequest));
    }

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<FamilyMember>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(memberService.findByFamily(familyId));
    }

    @PostMapping("/family/{familyId}")
    public ApiResponse<FamilyMember> createInFamily(
            @PathVariable Long familyId,
            @Valid @RequestBody MemberRequest request) {

        MemberRequest integratedRequest = new MemberRequest(
                request.fullName(),
                request.roleType(),
                request.age(),
                request.autonomyLevel(),
                request.responsibilityLevel(),
                request.email(),
                request.phone(),
                familyId
        );

        return ApiResponse.ok(memberService.createMember(integratedRequest));
    }


    @GetMapping("/{id}")
    public ApiResponse<FamilyMember> getById(@PathVariable Long id) {
        return ApiResponse.ok(memberService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<FamilyMember> update(@PathVariable Long id, @RequestBody FamilyMember member) {
        return ApiResponse.ok(memberService.update(id, member));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ApiResponse.ok(null);
    }
}
