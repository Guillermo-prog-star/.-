package com.integrityfamily.member.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final com.integrityfamily.domain.repository.UserRepository userRepository;
    private final com.integrityfamily.domain.repository.FamilyRepository familyRepository;
    private final com.integrityfamily.member.service.InvitationService invitationService;

    @PreAuthorize("@familySecurity.checkMember(#id)")
    @PostMapping("/{id}/invite")
    public ApiResponse<Void> inviteMember(@PathVariable Long id) {
        invitationService.sendInvitation(id);
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<List<FamilyMember>> getAll() {
        return ApiResponse.ok(memberService.findAll());
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ApiResponse<List<FamilyMember>> getMyFamilyMembers(org.springframework.security.core.Authentication auth) {
        com.integrityfamily.domain.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (user.getFamily() == null) {
            return ApiResponse.ok(java.util.Collections.emptyList());
        }
        
        return ApiResponse.ok(memberService.findByFamily(user.getFamily().getId()));
    }

    @PostMapping("/mine")
    @Transactional
    public ApiResponse<FamilyMember> createInMyFamily(
            @RequestBody MemberRequest request,
            org.springframework.security.core.Authentication auth) {
        
        log.info("[MEMBER-DEBUG] Registrando: '{}' con rol '{}'", request.fullName(), request.roleType());

        if (request.fullName() == null || request.fullName().trim().isEmpty()) {
            return ApiResponse.error("El nombre es obligatorio (Manual)");
        }

        com.integrityfamily.domain.User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        com.integrityfamily.domain.Family family = null;
        if (request.familyId() != null) {
            family = familyRepository.findById(request.familyId()).orElse(null);
        }
        
        if (family == null) {
            family = user.getFamily();
        }
        
        if (family == null) {
            log.warn("[MEMBER-AUTH] Usuario {} sin vínculo. Intentando recuperación por dueño...", auth.getName());
            family = familyRepository.findByCreatedBy_Email(auth.getName())
                    .orElseThrow(() -> new RuntimeException("No tienes una familia asociada."));
            
            user.setFamily(family);
            userRepository.saveAndFlush(user);
        }

        MemberRequest integratedRequest = new MemberRequest(
                request.fullName(),
                request.roleType(),
                request.age(),
                request.autonomyLevel(),
                request.responsibilityLevel(),
                request.email(),
                request.phone(),
                family.getId()
        );

        return ApiResponse.ok(memberService.createMember(integratedRequest));
    }

    @GetMapping("/family/{familyId}")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ApiResponse<List<FamilyMember>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(memberService.findByFamily(familyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@familySecurity.checkMember(#id)")
    public ApiResponse<FamilyMember> getById(@PathVariable Long id) {
        return ApiResponse.ok(memberService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@familySecurity.checkMember(#id)")
    public ApiResponse<FamilyMember> update(@PathVariable Long id, @RequestBody FamilyMember member) {
        return ApiResponse.ok(memberService.update(id, member));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@familySecurity.checkMember(#id)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ApiResponse.ok(null);
    }
}
