package com.integrityfamily.member.service;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.family.domain.Member;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final FamilyRepository familyRepository;

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public List<Member> findByFamily(Long familyId) {
        return memberRepository.findByFamilyId(familyId);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Miembro de la familia no encontrado"));
    }

    @Transactional
    public Member create(Member member) {
        return memberRepository.save(member);
    }

    /**
     * Crea un miembro desde el DTO del frontend, mapeando los campos correctamente.
     * - fullName → firstName + lastName (divide en espacio)
     * - roleType → role + relationship
     * - Vincula la familia por ID
     */
    @Transactional
    public Member createFromRequest(Long familyId, MemberRequest req) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada: " + familyId));

        Member member = new Member();
        member.setFullName(req.fullName());
        member.setRoleType(req.roleType());
        member.setAge(req.age() != null ? req.age() : 0);
        member.setAutonomyLevel(req.autonomyLevel() != null ? req.autonomyLevel() : 70);
        member.setResponsibilityLevel(req.responsibilityLevel() != null ? req.responsibilityLevel() : 70);
        member.setActive(true);
        member.setFamily(family);

        return memberRepository.save(member);
    }

    @Transactional
    public Member createInFamily(Long familyId, Member member) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada: " + familyId));
        member.setFamily(family);
        return memberRepository.save(member);
    }

    @Transactional
    public Member update(Long id, Member request) {
        Member existing = findById(id);
        
        // SINCRONIZACIÓN FINAL: Usamos los nombres exactos de Member.java
        existing.setFullName(request.getFullName());
        existing.setRoleType(request.getRoleType());
        existing.setAge(request.getAge());
        existing.setActive(request.isActive());
        
        return memberRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}