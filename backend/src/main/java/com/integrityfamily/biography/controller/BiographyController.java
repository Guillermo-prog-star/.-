package com.integrityfamily.biography.controller;

import com.integrityfamily.biography.dto.BiographyDtos.AnswerRequest;
import com.integrityfamily.biography.dto.BiographyDtos.AnswerResponse;
import com.integrityfamily.biography.dto.BiographyDtos.ChapterResponse;
import com.integrityfamily.biography.service.BiographyService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Diario guiado biográfico. El catálogo (/api/biography/catalog) es
 * contenido global, igual para cualquier familia. Las respuestas cuelgan
 * de /api/families/{familyId}/biography — mismo patrón de seguridad que
 * LineageController/ConsentController.
 */
@RestController
@RequiredArgsConstructor
public class BiographyController {

    private final BiographyService biographyService;
    private final MemberRepository memberRepository;

    @GetMapping("/api/biography/catalog")
    public ApiResponse<List<ChapterResponse>> getCatalog() {
        return ApiResponse.ok(biographyService.getCatalog());
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/api/families/{familyId}/biography/members/{memberId}/answers")
    public ApiResponse<List<AnswerResponse>> getAnswers(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            Principal principal) {
        return ApiResponse.ok(biographyService.getAnswers(familyId, memberId, resolveViewerMemberId(principal)));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/api/families/{familyId}/biography/perspectives/{perceivedEventKey}")
    public ApiResponse<List<AnswerResponse>> getPerspectives(
            @PathVariable Long familyId,
            @PathVariable String perceivedEventKey,
            Principal principal) {
        return ApiResponse.ok(biographyService.getPerspectives(familyId, perceivedEventKey, resolveViewerMemberId(principal)));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PutMapping("/api/families/{familyId}/biography/answers/{questionId}")
    public ApiResponse<AnswerResponse> upsertAnswer(
            @PathVariable Long familyId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequest request,
            Principal principal) {
        Long authorMemberId = resolveAuthorMemberId(principal);
        return ApiResponse.ok(biographyService.upsertAnswer(familyId, questionId, request, authorMemberId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @DeleteMapping("/api/families/{familyId}/biography/answers/{questionId}")
    public ApiResponse<Void> deleteAnswer(
            @PathVariable Long familyId,
            @PathVariable Long questionId,
            Principal principal) {
        Long authorMemberId = resolveAuthorMemberId(principal);
        biographyService.deleteAnswer(familyId, questionId, authorMemberId);
        return ApiResponse.ok(null);
    }

    /** Autor real resuelto del principal autenticado — nunca confiado del cliente (mismo patrón que JournalController, ADR-012). */
    private Long resolveAuthorMemberId(Principal principal) {
        Long memberId = resolveViewerMemberId(principal);
        if (memberId == null) {
            throw new AccessDeniedException("No tienes un perfil de miembro en esta familia");
        }
        return memberId;
    }

    /**
     * Miembro propio del principal, o null si no tiene perfil de miembro. Deliberadamente NO usa
     * SecurityValidator.resolveViewerMemberId: ese devuelve null (bypass total) para admin y creador
     * de familia, y en el diario biográfico lo PRIVATE solo lo ve su autor (ADR-017).
     */
    private Long resolveViewerMemberId(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }
        return memberRepository.findByEmailIgnoreCase(principal.getName())
                .map(FamilyMember::getId)
                .orElse(null);
    }
}
