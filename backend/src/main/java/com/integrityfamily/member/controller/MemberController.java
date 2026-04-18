package com.integrityfamily.member.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.family.domain.Member;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
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
     * Crea un integrante usando el DTO MemberRequest que envía el frontend.
     * POST /api/members/family/{familyId}
     */
    @PostMapping("/family/{familyId}")
    public ApiResponse<Member> createInFamily(
            @PathVariable Long familyId,
            @Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.createFromRequest(familyId, request));
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